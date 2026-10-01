package org.example.gui.devices.binder.camera.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.device.ethernet.binder.BinderCommandRegistry;
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
 * TCP-сервер, имитирующий климатическую камеру Binder (порт по умолчанию 10001).
 *
 * <p>Принимает Modbus-подобные кадры запроса, проверяет CRC, изменяет состояние
 * {@link BinderEmulator} (SetT / SetHC) либо возвращает измеренную температуру (GetT),
 * и пишет ответ. Каждый кадр логируется в hex + ASCII.
 *
 * <p>Отладка (нужна и для перехвата других приборов по Ethernet — их часто заводят
 * на этот же эмулятор):
 * <ul>
 *   <li>подробная трассировка каждого байта в консоль, терминал, GUI и
 *       {@code logs/Binder-TCP_wire_*.log} — см. {@link EmulatorTrace};</li>
 *   <li>на <b>неизвестную функцию соединение больше не рвётся</b> (раньше было
 *       «Неизвестная функция 0x01» → disconnect): пишется разбор PDU и отправляется
 *       исключение {@code 0x81 0x01 IllegalFunction}, после чего соединение живёт дальше —
 *       так виден весь сеанс обмена прибора;</li>
 *   <li>помимо 0x03/0x06/0x10 поддержаны 0x01/0x02 (чтение катушек/дискретных входов)
 *       и 0x0F — иначе прибор, спрашивающий их, сразу отваливается.</li>
 *   <li>граница кадра ищется эвристически ({@link ModbusFrameGuesser}), так что даже
 *       нестандартный код функции не «съедает» поток.</li>
 * </ul>
 */
@Slf4j
public class BinderServerService {

    public static final int DEFAULT_PORT = 10001;

    private final BinderEmulator emulator;
    private final EmulatorTrace trace = new EmulatorTrace("Binder-TCP");
    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();
    private final AtomicInteger connectionSeq = new AtomicInteger();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running = false;

    private volatile EmulatorCommandLog commandLog;
    private volatile boolean answerUnknownFunction = true;
    private volatile int idleTimeoutMs = 0;

    public BinderServerService(BinderEmulator emulator) {
        this.emulator = emulator;
        trace.addListener(this::forwardToGui);
    }

    /** Подключить лог команд виртуальной камеры (опционально). */
    public void setCommandLog(EmulatorCommandLog log) {
        this.commandLog = log;
    }

    public EmulatorTrace trace() {
        return trace;
    }

    /** Отвечать ли исключением на незнакомый код функции (иначе — тишина). */
    public void setAnswerUnknownFunction(boolean v) {
        this.answerUnknownFunction = v;
    }

    public boolean isAnswerUnknownFunction() {
        return answerUnknownFunction;
    }

    /** Таймаут простоя соединения, мс (0 — не ограничивать). */
    public void setIdleTimeoutMs(int ms) {
        this.idleTimeoutMs = Math.max(0, ms);
    }

    public int getIdleTimeoutMs() {
        return idleTimeoutMs;
    }

    public void addLogListener(Consumer<String> l) {
        logListeners.add(l);
    }

    public void addConnectionListener(Consumer<Boolean> l) {
        connectionListeners.add(l);
    }

    public boolean start(int port) {
        if (running) {
            return false;
        }
        try {
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(port));
        } catch (IOException e) {
            log.error("Binder emu: bind fail on {}", port, e);
            fireLog("Не удалось открыть порт " + port + ": " + e.getMessage());
            return false;
        }
        running = true;
        fireLog("Эмулятор слушает порт " + port);
        trace.event("Эмулятор слушает порт %d · ответ на незнакомую функцию=%s · idle=%d мс",
                port, answerUnknownFunction ? "да" : "нет", idleTimeoutMs);
        acceptThread = new Thread(this::acceptLoop, "Binder-Emu-Accept");
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
        fireLog("Эмулятор остановлен");
    }

    public boolean isRunning() {
        return running;
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                socket.setTcpNoDelay(true);
                socket.setKeepAlive(true);
                // Реальная камера держит одно соединение длительное время (запросы идут
                // с паузами > таймаута чтения). По умолчанию таймаут отключён: соединение
                // живёт, пока клиент его не закроет.
                socket.setSoTimeout(idleTimeoutMs);
                int id = connectionSeq.incrementAndGet();
                Thread h = new Thread(() -> handleClient(socket, id), "Binder-Emu-Conn#" + id);
                h.setDaemon(true);
                h.start();
            } catch (IOException e) {
                if (running) {
                    log.warn("Binder emu: accept error", e);
                }
                break;
            }
        }
    }

    private void handleClient(Socket socket, int id) {
        String remote = String.valueOf(socket.getRemoteSocketAddress());
        trace.event("#%d ПОДКЛЮЧЕНИЕ %s -> %s · счётчики RX=%d B / TX=%d B",
                id, remote, socket.getLocalSocketAddress(), trace.rxBytes(), trace.txBytes());
        try (Socket s = socket) {
            fireConnection(true);
            fireLog("Клиент подключён: " + remote);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();
            ByteArrayOutputStream pending = new ByteArrayOutputStream();
            byte[] chunk = new byte[1024];

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
                drainPending(pending, out, id);
            }
            if (pending.size() > 0) {
                trace.event("#%d в хвосте %d B нераспознанного потока:", id, pending.size());
                trace.dump("??", pending.toByteArray(), 0, pending.size());
            }
            fireLog("Клиент отключён: " + remote);
        } catch (IOException e) {
            trace.event("#%d ошибка ввода-вывода: %s", id, e);
            if (running) {
                log.warn("Binder emu: conn #{} error", id, e);
            }
        } finally {
            trace.event("#%d ОТКЛЮЧЕНИЕ %s · за сессию RX=%d B / TX=%d B",
                    id, remote, trace.rxBytes(), trace.txBytes());
            fireConnection(false);
        }
    }

    private void drainPending(ByteArrayOutputStream pending, OutputStream out, int id) {
        byte[] data = pending.toByteArray();
        int off = 0;
        boolean progress = true;
        while (progress && off < data.length) {
            progress = false;
            int len = ModbusFrameGuesser.frameLength(data, off, data.length - off);
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

    private void handleFrame(byte[] frame, OutputStream out, int id) {
        boolean mbap = ModbusFrameGuesser.isMbap(frame);
        int pduOff = mbap ? 7 : 1;
        int pduEnd = mbap ? frame.length : frame.length - 2;
        if (pduEnd <= pduOff) {
            trace.event("#%d слишком короткий кадр (%d B): %s", id, frame.length, EmulatorTrace.hex(frame));
            return;
        }
        int txId = mbap ? ((frame[0] & 0xFF) << 8) | (frame[1] & 0xFF) : 0;
        int slave = frame[pduOff - 1] & 0xFF;
        byte[] pdu = Arrays.copyOfRange(frame, pduOff, pduEnd);
        int func = pdu[0] & 0xFF;
        String crc = mbap ? "n/a(MBAP)"
                : (BinderCommandRegistry.checkCrc(frame, frame.length - 2) ? "ok" : "BAD");

        trace.rx(frame, 0, frame.length, String.format(Locale.US,
                "#%d %s slave=%02X fn=%s [%s] crc=%s",
                id, mbap ? "MBAP" : "RTU ", slave, EmulatorTrace.hexByte(func),
                ModbusFunctions.name(func), crc));
        trace.event("      разбор: %s", ModbusFunctions.describePdu(pdu));

        byte[] pduReply = buildPdu(func, pdu);
        if (pduReply == null) {
            trace.tx(new byte[0], "#" + id + " ответ не отправлен");
            return;
        }
        byte[] adu = mbap ? wrapMbap(txId, slave, pduReply) : appendCrcWithSlave(slave, pduReply);
        try {
            out.write(adu);
            out.flush();
            trace.tx(adu, 0, adu.length, "#" + id + " slave=" + String.format("%02X", slave)
                    + " fn=" + EmulatorTrace.hexByte(pduReply[0] & 0xFF));
        } catch (IOException e) {
            trace.event("#%d ошибка отправки ответа: %s", id, e);
        }
    }

    /** Сборка PDU ответа; {@code null} — отвечать нечего. */
    private byte[] buildPdu(int func, byte[] request) {
        switch (func & 0x7F) {
            case 0x01:
            case 0x02:
                return buildBitRead(request);
            case 0x03: {
                float measured = (float) emulator.getMeasuredTemperature();
                if (commandLog != null) {
                    commandLog.dataRequest(EmulatorTrace.hex(request));
                }
                return buildGetTempReply(request[0], measured);
            }
            case 0x06: {
                int reg = u16(request, 1);
                boolean on = (request[4] & 0xFF) == 0x81 || (u16(request, 3) == 1);
                emulator.setHumidityControl(on);
                trace.event("      SetHC → влажность %s (рег 0x%04X)", on ? "ВКЛ" : "ВЫКЛ", reg);
                if (commandLog != null) {
                    commandLog.command(on ? "включение поддержки влажности" : "отключение поддержки влажности");
                }
                return Arrays.copyOf(request, 5);
            }
            case 0x0F:
                return Arrays.copyOf(request, Math.min(5, request.length));
            case 0x10: {
                float t = parseSetTempFloat(request);
                emulator.setSetpoint(t);
                trace.event("      SetT → уставка %s °C", String.format(Locale.US, "%.2f", t));
                if (commandLog != null) {
                    commandLog.command("установка температуры: " + String.format(Locale.US, "%.2f", t) + " °C");
                }
                byte[] ack = new byte[5];
                System.arraycopy(request, 0, ack, 0, Math.min(5, request.length));
                return ack;
            }
            default:
                break;
        }
        if (ModbusFunctions.isKnown(func)) {
            trace.event("      функция %s известна, но не эмулируется — ответа нет", ModbusFunctions.name(func));
            return null;
        }
        trace.event("      функция %s не распознана — %s (в Binder её нет, соединение НЕ рвём)",
                EmulatorTrace.hexByte(func),
                answerUnknownFunction ? "шлём исключение IllegalFunction" : "молчание");
        if (!answerUnknownFunction) {
            return null;
        }
        return new byte[]{(byte) (func | 0x80), 0x01};
    }

    private byte[] buildBitRead(byte[] request) {
        if (request.length < 5) {
            return null;
        }
        int func = request[0] & 0xFF;
        int start = u16(request, 1);
        int qty = Math.max(1, Math.min(2000, u16(request, 3)));
        int byteCount = (qty + 7) / 8;
        byte[] r = new byte[2 + byteCount];
        r[0] = request[0];
        r[1] = (byte) byteCount;
        // Камера Binder: бит 0 = готовность к работе. Остальные — «управление влажностью».
        for (int i = 0; i < qty; i++) {
            boolean on = i == 0 ? true : emulator.isHumidityControlOn();
            if (on) {
                r[2 + i / 8] |= (byte) (1 << (i % 8));
            }
        }
        trace.event("      %s %d..%d → %s", func == 0x01 ? "катушки" : "дискретные входы",
                start, start + qty - 1, EmulatorTrace.hex(r, 2, byteCount));
        return r;
    }

    private byte[] buildGetTempReply(int func, float temperature) {
        int bits = Float.floatToRawIntBits(temperature);
        byte b0 = (byte) (bits & 0xFF);
        byte b1 = (byte) ((bits >>> 8) & 0xFF);
        byte b2 = (byte) ((bits >>> 16) & 0xFF);
        byte b3 = (byte) ((bits >>> 24) & 0xFF);
        byte[] f = new byte[6];
        f[0] = (byte) func;
        f[1] = 0x04;
        f[2] = b1;
        f[3] = b0;
        f[4] = b3;
        f[5] = b2;
        return f;
    }

    private float parseSetTempFloat(byte[] pdu) {
        if (pdu.length < 11) {
            return 0f;
        }
        int bits = (pdu[6] & 0xFF) | ((pdu[5] & 0xFF) << 8)
                | ((pdu[8] & 0xFF) << 16) | ((pdu[7] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }

    private static byte[] wrapMbap(int txId, int slave, byte[] pdu) {
        int length = 1 + pdu.length;
        byte[] adu = new byte[6 + length];
        adu[0] = (byte) ((txId >> 8) & 0xFF);
        adu[1] = (byte) (txId & 0xFF);
        adu[2] = 0x00;
        adu[3] = 0x00;
        adu[4] = (byte) ((length >> 8) & 0xFF);
        adu[5] = (byte) (length & 0xFF);
        adu[6] = (byte) slave;
        System.arraycopy(pdu, 0, adu, 7, pdu.length);
        return adu;
    }

    /** RTU ADU: {@code slave + PDU + CRC-16/Modbus}. */
    private static byte[] appendCrcWithSlave(int slave, byte[] pdu) {
        byte[] body = new byte[1 + pdu.length];
        body[0] = (byte) slave;
        System.arraycopy(pdu, 0, body, 1, pdu.length);
        int crc = BinderCommandRegistry.crc16Modbus(body, body.length);
        byte[] adu = Arrays.copyOf(body, body.length + 2);
        adu[body.length] = (byte) (crc & 0xFF);
        adu[body.length + 1] = (byte) ((crc >>> 8) & 0xFF);
        return adu;
    }

    private static int u16(byte[] d, int off) {
        return ((d[off] & 0xFF) << 8) | (d[off + 1] & 0xFF);
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

    private void fireLog(String line) {
        log.info("Binder emu: {}", line);
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(line);
            } catch (Exception ignored) {
            }
        }
    }
}
