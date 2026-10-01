package org.example.gui.devices.emulation;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Трассировка wire-обмена для эмуляторов приборов (нужна при брутфорсе протокола).
 *
 * <p>Каждая строка обмена уходит одновременно:
 * <ul>
 *   <li>в logback (консоль приложения + {@code logs/*.log}) — всегда, если {@link #setLogEnabled(boolean)};</li>
 *   <li>в {@code System.out} (терминал, где запущено приложение) — если {@link #setTerminalEcho(boolean)};</li>
 *   <li>в отдельный файл {@code logs/&lt;tag&gt;_wire_&lt;yyyyMMdd&gt;.log} — если {@link #setFileEnabled(boolean)};</li>
 *   <li>подписчикам GUI (текстовое поле лога панели эмуляции).</li>
 * </ul>
 *
 * <p>Формат строки (максимально подробный, читается «глазами» без IDE):
 * <pre>
 * [Boto120-TCP][  12:03:41.552][RX][  8 B][slave=01 fn=0x03 ReadHoldingRegs] 01 03 00 0C 00 01 44 09 | .. .............|
 * </pre>
 * Для длинных пакетов дополнительно печатается многострочный hex-dump с ASCII-колонкой
 * и смещениями ({@link #dump}).
 *
 * <p>Счётчики {@link #rxBytes()} / {@link #txBytes()} показывают объём обмена —
 * полезно отличить «молчащий» прибор от «залипающего» в цикле.
 */
@Slf4j
public class EmulatorTrace {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    private static final int DUMP_LINE = 16;

    private final String tag;
    private final CopyOnWriteArrayList<Consumer<String>> listeners = new CopyOnWriteArrayList<>();
    private final AtomicLong rxBytes = new AtomicLong();
    private final AtomicLong txBytes = new AtomicLong();
    private final AtomicLong rxPackets = new AtomicLong();
    private final AtomicLong txPackets = new AtomicLong();

    private volatile boolean terminalEcho = true;
    private volatile boolean logEnabled = true;
    private volatile boolean fileEnabled = true;
    private volatile BufferedWriter fileWriter;
    private volatile boolean fileErrorReported;

    public EmulatorTrace(String tag) {
        this.tag = tag;
    }

    // ─── настройки ────────────────────────────────────────────────────────

    /** Дублировать строки в {@code System.out} (терминал). По умолчанию — включено. */
    public EmulatorTrace setTerminalEcho(boolean v) { this.terminalEcho = v; return this; }

    /** Писать строки через logback (консоль приложения + logs/EM-LogFile.log). */
    public EmulatorTrace setLogEnabled(boolean v) { this.logEnabled = v; return this; }

    /** Писать строки в отдельный wire-файл {@code logs/<tag>_wire_<дата>.log}. */
    public EmulatorTrace setFileEnabled(boolean v) {
        this.fileEnabled = v;
        if (!v) {
            closeFile();
        }
        return this;
    }

    public EmulatorTrace addListener(Consumer<String> l) {
        listeners.add(l);
        return this;
    }

    public boolean isTerminalEcho() { return terminalEcho; }

    public boolean isLogEnabled() { return logEnabled; }

    public boolean isFileEnabled() { return fileEnabled; }

    public String tag() { return tag; }

    public long rxBytes() { return rxBytes.get(); }

    public long txBytes() { return txBytes.get(); }

    public long rxPackets() { return rxPackets.get(); }

    public long txPackets() { return txPackets.get(); }

    public void resetCounters() {
        rxBytes.set(0);
        txBytes.set(0);
        rxPackets.set(0);
        txPackets.set(0);
    }

    /** Подпись файла wire-лога (пусто, если файл выключен). */
    public String wireFileName() {
        if (!fileEnabled) return "";
        return Paths.get("logs", tag + "_wire_" + LocalDateTime.now().format(FILE_TS) + ".log").toString();
    }

    // ─── точки трассировки ────────────────────────────────────────────────

    /** Событие без бинарных данных (соединение, ошибка, разбор кадра). */
    public void event(String text) {
        emit("[event] " + text);
    }

    /** Событие с форматированием ({@link String#format}). */
    public void event(String fmt, Object... args) {
        emit("[event] " + String.format(fmt, args));
    }

    /** Принятые байты от прибора. */
    public void rx(byte[] data) { rx(data, 0, data == null ? 0 : data.length, null); }

    public void rx(byte[] data, int off, int len) { rx(data, off, len, null); }

    /** Принятые байты с дополнительным пояснением кадра. */
    public void rx(byte[] data, int off, int len, String note) {
        if (data == null || len <= 0) {
            return;
        }
        rxPackets.incrementAndGet();
        rxBytes.addAndGet(len);
        emit("[RX ] " + header(len, note) + " " + hex(data, off, len) + " | " + ascii(data, off, len) + " |");
        dumpIfNeeded(data, off, len, "RX");
    }

    /** Отправленные байты прибору. */
    public void tx(byte[] data) { tx(data, 0, data == null ? 0 : data.length, null); }

    public void tx(byte[] data, String note) {
        tx(data, 0, data == null ? 0 : data.length, note);
    }

    public void tx(byte[] data, int off, int len, String note) {
        if (data == null || len <= 0) {
            emit("[TX ] (пустой ответ) " + (note == null ? "" : note));
            return;
        }
        txPackets.incrementAndGet();
        txBytes.addAndGet(len);
        emit("[TX ] " + header(len, note) + " " + hex(data, off, len) + " | " + ascii(data, off, len) + " |");
        dumpIfNeeded(data, off, len, "TX");
    }

    /**
     * Многострочный hex-dump (смещение + hex + ASCII). Пишется только при пакетах
     * длиннее одной строки — чтобы не засорять вывод.
     */
    public void dump(String direction, byte[] data, int off, int len) {
        if (data == null || len <= 0) {
            return;
        }
        for (int pos = 0; pos < len; pos += DUMP_LINE) {
            int n = Math.min(DUMP_LINE, len - pos);
            StringBuilder sb = new StringBuilder(96);
            sb.append(String.format("      %s %04X  ", direction, pos));
            for (int i = 0; i < n; i++) {
                int b = data[off + pos + i] & 0xFF;
                sb.append(HEX[b >>> 4]).append(HEX[b & 0x0F]).append(' ');
            }
            sb.append(' ').append(ascii(data, off + pos, n));
            emit(sb.toString());
        }
    }

    // ─── статика: представление байтов ────────────────────────────────────

    /** Hex-строка с пробелами: {@code "01 03 00 0C 00 01 44 09"}. */
    public static String hex(byte[] data) {
        return data == null ? "" : hex(data, 0, data.length);
    }

    public static String hex(byte[] data, int off, int len) {
        if (data == null || len <= 0) {
            return "";
        }
        int end = Math.min(off + len, data.length);
        StringBuilder sb = new StringBuilder(Math.max(16, (end - off) * 3));
        for (int i = off; i < end; i++) {
            int b = data[i] & 0xFF;
            sb.append(HEX[b >>> 4]).append(HEX[b & 0x0F]).append(' ');
        }
        return sb.toString().trim();
    }

    /** ASCII-строка: печатаемые символы как есть, остальное — точка. */
    public static String ascii(byte[] data, int off, int len) {
        if (data == null || len <= 0) {
            return "";
        }
        int end = Math.min(off + len, data.length);
        StringBuilder sb = new StringBuilder(end - off);
        for (int i = off; i < end; i++) {
            int b = data[i] & 0xFF;
            sb.append(b >= 0x20 && b < 0x7F ? (char) b : '.');
        }
        return sb.toString();
    }

    /** Читаемый hex вида {@code 0x03} / {@code 0x8A}. */
    public static String hexByte(int v) {
        int b = v & 0xFF;
        return "0x" + HEX[b >>> 4] + HEX[b & 0x0F];
    }

    // ─── внутреннее ───────────────────────────────────────────────────────

    private String header(int len, String note) {
        StringBuilder sb = new StringBuilder(48);
        sb.append(String.format("[%6d B]", len));
        if (note != null && !note.isEmpty()) {
            sb.append('[').append(note).append(']');
        }
        return sb.toString();
    }

    private void dumpIfNeeded(byte[] data, int off, int len, String direction) {
        if (len > DUMP_LINE) {
            dump(direction, data, off, len);
        }
    }

    private void emit(String line) {
        String full = "[" + tag + "][" + LocalDateTime.now().format(TS) + "]" + line;
        if (logEnabled) {
            log.info(full);
        }
        if (terminalEcho) {
            System.out.println(full);
        }
        writeFile(full);
        for (Consumer<String> l : listeners) {
            try {
                l.accept(full);
            } catch (Exception ignored) {
                // трассировка не должна ломать эмулятор
            }
        }
    }

    private void writeFile(String line) {
        if (!fileEnabled) {
            return;
        }
        BufferedWriter w = fileWriter;
        if (w == null) {
            w = openFile();
        }
        if (w == null) {
            return;
        }
        try {
            w.write(line);
            w.newLine();
            w.flush();
        } catch (IOException e) {
            closeFile();
            if (!fileErrorReported) {
                fileErrorReported = true;
                log.warn("[{}] wire-файл недоступен: {}", tag, e.getMessage());
            }
        }
    }

    private synchronized BufferedWriter openFile() {
        if (!fileEnabled || fileWriter != null) {
            return fileWriter;
        }
        try {
            Path p = Paths.get(wireFileName());
            Path parent = p.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            fileWriter = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            log.info("[{}] wire-лог: {}", tag, p);
        } catch (IOException e) {
            if (!fileErrorReported) {
                fileErrorReported = true;
                log.warn("[{}] не удалось открыть wire-файл: {}", tag, e.getMessage());
            }
            fileWriter = null;
        }
        return fileWriter;
    }

    private synchronized void closeFile() {
        BufferedWriter w = fileWriter;
        fileWriter = null;
        if (w == null) {
            return;
        }
        try {
            w.flush();
            w.close();
        } catch (IOException ignored) {
        }
    }

    /** Освободить ресурсы (закрыть wire-файл). Слушатели GUI снимает вызывающий. */
    public void close() {
        closeFile();
    }
}
