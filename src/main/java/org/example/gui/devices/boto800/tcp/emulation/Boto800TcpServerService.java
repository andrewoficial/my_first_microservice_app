package org.example.gui.devices.boto800.tcp.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.devices.emulation.EmulatorCommandLog;
import org.example.gui.devices.emulation.EmulatorTrace;
import org.example.gui.devices.emulation.ModbusFrameGuesser;
import org.example.gui.devices.emulation.ModbusFunctions;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * TCP-сервер-«сниффер» термокамеры <b>B-TH-800 F</b> — протокол
 * {@code BOTO-800 Modbus} (FW {@code v4.2.4}, программа {@code UApp_boxed.exe}),
 * порт по умолчанию {@value #DEFAULT_PORT}.
 *
 * <p><b>Не путать с China Modbus</b> — протоколом камеры B-TH-120 E
 * ({@code 12/100/105, SCALE=100}), у которой Ethernet нет вообще.
 * Разграничение моделей — {@code boto_cameras.md}, карта — {@code boto_800_register_map.md}.
 *
 * <p>Назначение — перехват протокола камеры по Ethernet: прибор сам стучится в эмулятор,
 * а тот подробно журналирует <b>каждый</b> байт и максимально мягко отвечает, чтобы
 * прибор не отвалился и продолжил опрос (нужно для брутфорса карты регистров).
 *
 * <p>Что делает сервер:
 * <ul>
 *   <li>пишет hex + ASCII + смещения каждого пакета в консоль приложения, в терминал
 *       ({@code System.out}), в GUI-лог и в {@code logs/Boto800-TCP_wire_*.log}
 *       — см. {@link EmulatorTrace};</li>
 *   <li>автоматически различает <b>Modbus/TCP</b> (MBAP-заголовок, {@code protocolId = 0x0000})
 *       и «сырой» <b>Modbus/RTU по TCP</b>; ответ строится в том же формате, что и запрос
 *       (transactionId и unitId эхо-ятся);</li>
 *   <li>для незнакомых кодов функций <b>не рвёт соединение</b> (в отличие от прежнего
 *       «Неизвестная функция 0x01» → disconnect), а шлёт штатное исключение Modbus
 *       {@code 0x01 IllegalFunction} — прибор обычно после этого продолжает опрос;</li>
 *   <li>для неопознанного формата ищет границу кадра перебором по CRC-16/Modbus, поэтому
 *       даже «сырой» поток разбирается на кадры;</li>
 *   <li>помнит все адреса, куда прибор писал ({@link Boto800TcpEmulator#writtenRegisters()})
 *       — это и есть результат брутфорса.</li>
 * </ul>
 *
 * <p>Все ответы на чтение берутся из {@link Boto800TcpEmulator}: подтверждённые регистры
 * (12/100/105) отдают реальные значения, остальные — значение «обучения» (0 по умолчанию).
 */
@Slf4j
public class Boto800TcpServerService {

    public static final int DEFAULT_PORT = 8000;

    private static final int READ_CHUNK = 2048;
    private static final int MAX_PENDING = 8192;

    /** Режим ответа сервера. */
    public enum AnswerMode {
        /** Отвечать на все известные функции, на незнакомые — исключение Modbus. */
        FULL("Полный (Modbus)"),
        /** Отвечать только на чтение/запись 0x01..0x10, на остальное — тишина. */
        CORE("Только 0x01..0x10"),
        /** Ничего не отвечать: чистый сниффер, байты только в лог. */
        SILENT("Молча (только лог)");

        private final String title;

        AnswerMode(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    private final Boto800TcpEmulator emulator;
    private final EmulatorTrace trace = new EmulatorTrace("Boto800-TCP");

    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Integer>> clientCountListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger clientCount = new AtomicInteger();
    private final AtomicInteger connectionSeq = new AtomicInteger();

    private volatile EmulatorCommandLog commandLog;
    private volatile AnswerMode answerMode = AnswerMode.FULL;
    private volatile boolean autoEchoUnknown = true;
    private volatile boolean autoLearn = true;
    private volatile int idleTimeoutMs = 0;

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running = false;

    public Boto800TcpServerService(Boto800TcpEmulator emulator) {
        this.emulator = emulator;
        trace.addListener(this::forwardToGui);
    }

    // ─── настройки ────────────────────────────────────────────────────────

    public void setCommandLog(EmulatorCommandLog log) {
        this.commandLog = log;
    }

    public EmulatorTrace trace() {
        return trace;
    }

    public Boto800TcpEmulator emulator() {
        return emulator;
    }

    public void setAnswerMode(AnswerMode mode) {
        this.answerMode = mode;
    }

    public AnswerMode getAnswerMode() {
        return answerMode;
    }

    /** Отвечать ли кодом-исключением на незнакомую функцию (иначе — тишина). */
    public void setAutoEchoUnknown(boolean v) {
        this.autoEchoUnknown = v;
    }

    public boolean isAutoEchoUnknown() {
        return autoEchoUnknown;
    }

    /** «Обучение»: незнакомые адреса отвечают значением по умолчанию, чтобы прибор не зависал. */
    public void setAutoLearn(boolean v) {
        this.autoLearn = v;
        emulator.setLearnEnabled(v);
    }

    public boolean isAutoLearn() {
        return autoLearn;
    }

    /** Таймаут простоя соединения, мс (0 — ждать бесконечно). */
    public void setIdleTimeoutMs(int ms) {
        this.idleTimeoutMs = Math.max(0, ms);
    }

    public int getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public int clientCount() {
        return clientCount.get();
    }

    public void addLogListener(Consumer<String> l) {
        logListeners.add(l);
    }

    public void addConnectionListener(Consumer<Boolean> l) {
        connectionListeners.add(l);
    }

    public void addClientCountListener(Consumer<Integer> l) {
        clientCountListeners.add(l);
    }

    public boolean isRunning() {
        return running;
    }

    // ─── жизненный цикл сервера ───────────────────────────────────────────

    public boolean start(int port) {
        return start(port, null);
    }

    /**
     * Запуск сервера.
     *
     * @param port TCP-порт (0 — любой свободный, фактический доступен через {@link #boundPort()})
     * @param bind локальный адрес или {@code null}/пусто — слушать на всех интерфейсах
     */
    public boolean start(int port, String bind) {
        if (running) {
            trace.event("сервер уже запущен");
            return false;
        }
        String host = (bind == null || bind.isBlank()) ? "*" : bind.trim();
        try {
            ServerSocket ss = new ServerSocket();
            ss.setReuseAddress(true);
            ss.bind(host.equals("*") ? new InetSocketAddress(port) : new InetSocketAddress(host, port), 16);
            serverSocket = ss;
        } catch (IOException e) {
            trace.event("ОШИБКА bind %s:%d — %s", host, port, e);
            log.error("Boto800-TCP emu: bind fail {}:{}", host, port, e);
            return false;
        }
        running = true;
        trace.event("Эмулятор слушает %s:%d · режим=%s · idle=%d мс · автоответ на незнакомое=%s",
                host, boundPort(), answerMode.title(), idleTimeoutMs, autoEchoUnknown ? "да" : "нет");
        trace.event("Подсказка: адрес камеры в сети — запустите на порту %d, затем смотрите лог", boundPort());
        acceptThread = new Thread(this::acceptLoop, "Boto800-TCP-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        return true;
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException ignored) {
        }
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        trace.event("Эмулятор остановлен");
    }

    /** Фактический порт (полезно при старте на порту 0). */
    public int boundPort() {
        ServerSocket ss = serverSocket;
        return ss != null && ss.isBound() ? ss.getLocalPort() : -1;
    }

    // ─── приём соединений ─────────────────────────────────────────────────

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                socket.setSoTimeout(idleTimeoutMs);
                int id = connectionSeq.incrementAndGet();
                Thread h = new Thread(() -> handleClient(socket, id), "Boto800-TCP-Conn#" + id);
                h.setDaemon(true);
                h.start();
            } catch (IOException e) {
                if (running) {
                    trace.event("accept error: %s", e);
                    log.warn("Boto800-TCP emu: accept error", e);
                }
                break;
            }
        }
    }

    private void handleClient(Socket socket, int id) {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        String local = String.valueOf(socket.getLocalSocketAddress());
        trace.event("#%d ПОДКЛЮЧЕНИЕ %s -> %s · ответы=%s · счётчики RX=%d B / TX=%d B",
                id, remote, local, answerMode.title(), trace.rxBytes(), trace.txBytes());
        clientCount.incrementAndGet();
        fireConnection(true);
        fireClientCount();
        try (Socket s = socket) {
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            ByteArrayOutputStream pending = new ByteArrayOutputStream();
            byte[] chunk = new byte[READ_CHUNK];

            while (running && !s.isClosed()) {
                int r;
                try {
                    r = in.read(chunk);
                } catch (SocketTimeoutException te) {
                    trace.event("#%d простой %d мс · соединение держим · нераспознано %d B",
                            id, idleTimeoutMs, pending.size());
                    continue;
                }
                if (r < 0) {
                    break;
                }
                trace.rx(chunk, 0, r, "#" + id + " chunk");
                pending.write(chunk, 0, r);
                if (pending.size() > MAX_PENDING) {
                    trace.event("#%d буфер переполнен (%d B) — сбрасываем", id, pending.size());
                    trace.dump("??", pending.toByteArray(), 0, pending.size());
                    pending.reset();
                    continue;
                }
                drainPending(pending, out, id);
            }
            if (pending.size() > 0) {
                trace.event("#%d в хвосте %d B нераспознанного потока:", id, pending.size());
                trace.dump("??", pending.toByteArray(), 0, pending.size());
            }
        } catch (IOException e) {
            trace.event("#%d ошибка ввода-вывода: %s", id, e);
            if (running) {
                log.warn("Boto800-TCP emu: conn #{} error", id, e);
            }
        } finally {
            clientCount.decrementAndGet();
            trace.event("#%d ОТКЛЮЧЕНИЕ %s · за сессию RX=%d B / TX=%d B · адресов в зеркале: %s",
                    id, remote, trace.rxBytes(), trace.txBytes(), emulator.writtenSummary());
            fireConnection(clientCount.get() > 0);
            fireClientCount();
        }
    }

    // ─── разбор потока на кадры ───────────────────────────────────────────

    private void drainPending(ByteArrayOutputStream pending, OutputStream out, int id) {
        byte[] data = pending.toByteArray();
        int off = 0;
        boolean progress = true;
        while (progress && off < data.length) {
            progress = false;
            int len = frameLength(data, off, data.length - off);
            if (len <= 0 || off + len > data.length) {
                break;
            }
            byte[] frame = Arrays.copyOfRange(data, off, off + len);
            off += len;
            progress = true;
            handleFrame(frame, out, id);
        }
        if (off > 0) {
            byte[] rest = Arrays.copyOfRange(data, off, data.length);
            pending.reset();
            pending.write(rest, 0, rest.length);
        }
    }

    /**
     * Длина очередного кадра в потоке (0 — данных недостаточно или формат не распознан).
     *
     * @see ModbusFrameGuesser
     */
    static int frameLength(byte[] d, int off, int avail) {
        return ModbusFrameGuesser.frameLength(d, off, avail);
    }

    // ─── обработка кадра ──────────────────────────────────────────────────

    private void handleFrame(byte[] frame, OutputStream out, int id) {
        boolean mbap = ModbusFrameGuesser.isMbap(frame);
        int pduOff = mbap ? 7 : 1;                       // после MBAP-заголовка+unitId / после slave
        int pduEnd = mbap ? frame.length : frame.length - 2;
        if (pduEnd <= pduOff) {
            trace.event("#%d слишком короткий кадр (%d B): %s", id, frame.length, EmulatorTrace.hex(frame));
            return;
        }
        int txId = mbap ? ((frame[0] & 0xFF) << 8) | (frame[1] & 0xFF) : 0;
        int unitId = frame[pduOff - 1] & 0xFF;
        byte[] pdu = Arrays.copyOfRange(frame, pduOff, pduEnd);
        int func = pdu[0] & 0xFF;
        String crc = mbap ? "n/a(MBAP)" : (BotoModbusUtil.validateCrc(frame) ? "ok" : "BAD");

        if (mbap) {
            int declared = u16(frame, 4);
            int actual = frame.length - 6;
            if (declared != actual) {
                trace.event("      ВНИМАНИЕ: MBAP LEN=%d, а в кадре unitId+PDU=%d байт — "
                        + "возможно это не Modbus/TCP, трактуем как есть", declared, actual);
            }
        }
        if (!mbap && "BAD".equals(crc)) {
            trace.event("      ВНИМАНИЕ: CRC не сошёлся — отвечаем по разбору, "
                    + "чтобы не терять кадр (смотрите байты в дампе выше)");
        }

        trace.rx(frame, 0, frame.length, String.format(Locale.US,
                "#%d %s unitId=%02X fn=%s crc=%s",
                id, mbap ? "MBAP" : "RTU ", unitId, EmulatorTrace.hexByte(func), crc));
        trace.event("      разбор: %s", ModbusFunctions.describePdu(pdu));

        if (commandLog != null) {
            commandLog.dataRequest(EmulatorTrace.hex(frame));
        }

        byte[] pduReply = buildPdu(func, pdu);
        if (pduReply == null) {
            trace.tx(new byte[0], "#" + id + " ответа нет (режим=" + answerMode.title() + ")");
            return;
        }
        byte[] adu = mbap ? wrapMbap(txId, unitId, pduReply) : rtuFrame(unitId, pduReply);
        try {
            out.write(adu);
            out.flush();
            trace.tx(adu, 0, adu.length, "#" + id + " " + (mbap ? "MBAP" : "RTU ")
                    + "unitId=" + String.format("%02X", unitId)
                    + " fn=" + EmulatorTrace.hexByte(pduReply[0] & 0xFF));
        } catch (IOException e) {
            trace.event("#%d ошибка отправки ответа: %s", id, e);
        }
    }

    private static byte[] wrapMbap(int txId, int unitId, byte[] pdu) {
        int length = 1 + pdu.length;
        byte[] adu = new byte[6 + length];
        adu[0] = (byte) ((txId >> 8) & 0xFF);
        adu[1] = (byte) (txId & 0xFF);
        adu[2] = 0x00;
        adu[3] = 0x00;
        adu[4] = (byte) ((length >> 8) & 0xFF);
        adu[5] = (byte) (length & 0xFF);
        adu[6] = (byte) unitId;
        System.arraycopy(pdu, 0, adu, 7, pdu.length);
        return adu;
    }

    private static byte[] rtuFrame(int unitId, byte[] pdu) {
        byte[] body = new byte[1 + pdu.length];
        body[0] = (byte) unitId;
        System.arraycopy(pdu, 0, body, 1, pdu.length);
        return BotoModbusUtil.appendCrc(body);
    }

    /** Сборка PDU ответа; {@code null} — отвечать нечего. */
    private byte[] buildPdu(int func, byte[] request) {
        if (answerMode == AnswerMode.SILENT) {
            return null;
        }
        int f = func & 0x7F;
        if (answerMode == AnswerMode.CORE && f > 0x10) {
            trace.event("      функция %s вне ядра 0x01..0x10 — молчание",
                    ModbusFunctions.name(func));
            return null;
        }
        switch (f) {
            case 0x01:
            case 0x02:
                return buildBitReadResponse(request);
            case 0x03:
            case 0x04:
                return buildRegisterReadResponse(request);
            case 0x05:
                return buildCoilWriteEcho(request);
            case 0x06:
                return buildRegisterWriteEcho(request);
            case 0x0F:
                return buildCoilsWriteAck(request);
            case 0x10:
                return buildRegistersWriteAck(request);
            case 0x0B: {
                byte[] r = new byte[4];
                r[0] = (byte) 0x0B;
                r[1] = (byte) 0xFF;
                r[2] = (byte) 0xFF;
                r[3] = 0x00;
                return r;
            }
            case 0x0C: {
                byte[] r = new byte[6];
                r[0] = (byte) 0x0C;
                r[1] = 0x00;   // status
                r[2] = 0x01;   // event count
                r[3] = 0x00;
                r[4] = 0x00;
                r[5] = 0x00;
                return r;
            }
            case 0x0D: {
                byte[] r = new byte[3];
                r[0] = (byte) 0x0D;
                r[1] = 0x02;   // длина идентификатора
                r[2] = 0x01;   // slave активен
                return r;
            }
            default:
                break;
        }
        if (ModbusFunctions.isKnown(func)) {
            trace.event("      функция %s известна, но не эмулируется — ответа нет",
                    ModbusFunctions.name(func));
            return null;
        }
        trace.event("      функция %s не распознана — %s",
                EmulatorTrace.hexByte(func),
                autoEchoUnknown ? "шлём исключение IllegalFunction (0x81)" : "молчание (автоответ выключен)");
        if (!autoEchoUnknown) {
            return null;
        }
        return new byte[]{(byte) (func | 0x80), 0x01};
    }

    private byte[] buildRegisterReadResponse(byte[] request) {
        if (request.length < 5) {
            return null;
        }
        int start = u16(request, 1);
        int quantity = clamp(u16(request, 3), 1, 125);
        int byteCount = quantity * 2;
        byte[] r = new byte[2 + byteCount];
        r[0] = request[0];
        r[1] = (byte) byteCount;
        int unknown = 0;
        for (int i = 0; i < quantity; i++) {
            int reg = start + i;
            int value = emulator.readRegister(reg);
            if (!emulator.isKnownRegister(reg)) {
                unknown++;
            }
            r[2 + 2 * i] = (byte) ((value >> 8) & 0xFF);
            r[3 + 2 * i] = (byte) (value & 0xFF);
        }
        trace.event("      чтение %d..%d: %d из %d адресов вне карты BOTO 800%s",
                start, start + quantity - 1, unknown, quantity,
                autoLearn ? " · отдано значение обучения " + emulator.getLearnDefault() : "");
        if (commandLog != null) {
            commandLog.command("чтение рег " + start + ".." + (start + quantity - 1));
        }
        if (quantity > 1) {
            trace.event("      значения: %s", formatRegisters(start, r, 2, quantity));
        }
        return r;
    }

    /** Компактный дамп значений блока: {@code 10=250 11=250 12=1 …}. */
    private static String formatRegisters(int start, byte[] pduReply, int off, int quantity) {
        StringBuilder sb = new StringBuilder(quantity * 8);
        for (int i = 0; i < quantity; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(start + i).append('=');
            if (off + 2 * i + 1 < pduReply.length) {
                sb.append(u16(pduReply, off + 2 * i));
            }
        }
        return sb.toString();
    }

    private byte[] buildBitReadResponse(byte[] request) {
        if (request.length < 5) {
            return null;
        }
        int func = request[0] & 0xFF;
        int start = u16(request, 1);
        int quantity = clamp(u16(request, 3), 1, 2000);
        int byteCount = (quantity + 7) / 8;
        byte[] r = new byte[2 + byteCount];
        r[0] = request[0];
        r[1] = (byte) byteCount;
        for (int i = 0; i < quantity; i++) {
            if ((emulator.readCoil(start + i) & 0xFF00) == 0xFF00) {
                r[2 + i / 8] |= (byte) (1 << (i % 8));
            }
        }
        trace.event("      %s %d..%d: включено %d бит",
                func == 0x01 ? "катушки" : "дискретные входы", start, start + quantity - 1,
                countBits(r, 2, byteCount));
        if (commandLog != null) {
            commandLog.command((func == 0x01 ? "чтение катушек " : "чтение дискретных входов ")
                    + start + ".." + (start + quantity - 1));
        }
        return r;
    }

    private byte[] buildRegisterWriteEcho(byte[] request) {
        if (request.length < 5) {
            return null;
        }
        int reg = u16(request, 1);
        int value = u16(request, 3);
        emulator.writeRegister(reg, value);
        reportWrite(reg, value);
        return Arrays.copyOf(request, 5);
    }

    private byte[] buildCoilWriteEcho(byte[] request) {
        if (request.length < 5) {
            return null;
        }
        int addr = u16(request, 1);
        int value = u16(request, 3);
        emulator.writeCoil(addr, value);
        reportWrite(addr, value);
        return Arrays.copyOf(request, 5);
    }

    private byte[] buildRegistersWriteAck(byte[] request) {
        if (request.length < 6) {
            return null;
        }
        int start = u16(request, 1);
        int qty = u16(request, 3);
        int byteCount = request[5] & 0xFF;
        if (byteCount != 2 * qty || request.length < 6 + byteCount) {
            trace.event("      множественная запись: byteCount=%d != 2×%d — ответа нет",
                    byteCount, qty);
            return new byte[]{request[0], (byte) (request[1] | 0x80), 0x03};
        }
        // PDU 0x10: [0]=fn [1..2]=addr [3..4]=qty [5]=byteCount [6..]=данные
        for (int i = 0; i < qty; i++) {
            emulator.writeRegister(start + i, u16(request, 6 + 2 * i));
        }
        trace.event("      множественная запись %d регистров с адреса %d: %s",
                qty, start, values(start, qty, request));
        if (commandLog != null) {
            commandLog.command("запись рег " + start + ".." + (start + qty - 1));
        }
        return Arrays.copyOf(request, 5);
    }

    /** Значения, реально применённые множественной записью. */
    private String values(int start, int qty, byte[] request) {
        StringBuilder sb = new StringBuilder(qty * 8);
        for (int i = 0; i < qty; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(start + i).append('=').append(u16(request, 6 + 2 * i));
        }
        return sb.toString();
    }

    private byte[] buildCoilsWriteAck(byte[] request) {
        if (request.length < 5) {
            return null;
        }
        int start = u16(request, 1);
        int qty = u16(request, 3);
        if (commandLog != null) {
            commandLog.command("запись катушек " + start + ".." + (start + qty - 1));
        }
        return Arrays.copyOf(request, 5);
    }

    private void reportWrite(int reg, int value) {
        boolean known = emulator.isKnownRegister(reg);
        trace.event("      запись рег %d (0x%04X) = %d%s", reg, reg, value,
                known ? " · подтверждённый регистр" : " · адрес НЕ в карте, добавлен в зеркало");
        if (commandLog != null) {
            commandLog.command("запись рег " + reg + " = " + value);
        }
        if (!known) {
            trace.event("      зеркало адресов: %s", emulator.writtenSummary());
        }
    }

    // ─── служебное ────────────────────────────────────────────────────────

    private static int u16(byte[] d, int off) {
        return ((d[off] & 0xFF) << 8) | (d[off + 1] & 0xFF);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    private static int countBits(byte[] data, int off, int len) {
        int n = 0;
        for (int i = off; i < off + len; i++) {
            n += Integer.bitCount(data[i] & 0xFF);
        }
        return n;
    }

    private void forwardToGui(String line) {
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(line);
            } catch (Exception ignored) {
            }
        }
    }

    private void fireConnection(boolean connected) {
        for (Consumer<Boolean> l : connectionListeners) {
            try {
                l.accept(connected);
            } catch (Exception ignored) {
            }
        }
    }

    private void fireClientCount() {
        int n = clientCount.get();
        for (Consumer<Integer> l : clientCountListeners) {
            try {
                l.accept(n);
            } catch (Exception ignored) {
            }
        }
    }
}
