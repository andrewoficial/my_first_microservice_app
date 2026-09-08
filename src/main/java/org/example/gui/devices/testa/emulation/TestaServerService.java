package org.example.gui.devices.testa.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.device.ethernet.testa.TestaCommands;
import org.example.gui.devices.emulation.EmulatorCommandLog;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * UDP-сервер, имитирующий климатическую камеру Testa.
 *
 * <p>Слушает команду SetT на порту 1300 (порт камеры). Получив от клиента 32-байтную
 * датаграмму {@code 02 33 88 66}, применяет уставку. Статус ({@code 11 22 00 00}, 40 байт)
 * шлётся периодически (≈1 Гц) на порт 1200 клиента. Как и реальная камера, эмулятор шлёт
 * статус «сам» (широковещательно / на loopback), не дожидаясь первого SetT, — поэтому
 * слушатель порта 1200 получает температуру сразу, ещё до установки уставки.
 */
@Slf4j
public class TestaServerService {

    public static final int DEFAULT_LISTEN_PORT = TestaCommands.DEFAULT_REMOTE_PORT; // 1300
    public static final int REPLY_PORT = TestaCommands.LOCAL_RECEIVE_PORT;           // 1200
    private static final long STATUS_PERIOD_MS = 1000;

    private final TestaEmulator emulator;
    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> runningListeners = new CopyOnWriteArrayList<>();

    private volatile EmulatorCommandLog commandLog;

    private static final String BROADCAST_ADDR = "255.255.255.255";

    private volatile DatagramSocket socket;
    private volatile InetAddress clientAddr;
    private volatile boolean running = false;
    private Thread receiveThread;
    private Thread pushThread;
    private volatile InetAddress broadcastAddr;
    private volatile InetAddress loopbackAddr;

    public TestaServerService(TestaEmulator emulator) {
        this.emulator = emulator;
    }

    /** Подключить лог команд виртуальной камеры (опционально). */
    public void setCommandLog(EmulatorCommandLog log) { this.commandLog = log; }

    public void addLogListener(Consumer<String> l) { logListeners.add(l); }
    public void addRunningListener(Consumer<Boolean> l) { runningListeners.add(l); }

    public boolean start(int listenPort) {
        if (running) {
            return false;
        }
        try {
            DatagramSocket s = new DatagramSocket(listenPort);
            s.setBroadcast(true);
            socket = s;
        } catch (SocketException e) {
            log.error("Testa emu: bind fail on {}", listenPort, e);
            fireLog("Не удалось открыть порт " + listenPort + ": " + e.getMessage());
            return false;
        }
        try {
            // Адреса для «самостоятельной» рассылки статуса (без предварительного SetT):
            // реальная камера шлёт статус на широковещательный адрес подсети, а для
            // локального теста — на loopback.
            broadcastAddr = InetAddress.getByName(BROADCAST_ADDR);
            loopbackAddr = InetAddress.getLoopbackAddress();
        } catch (UnknownHostException e) {
            log.error("Testa emu: resolve fail", e);
            fireLog("Не удалось разрешить адрес рассылки: " + e.getMessage());
            broadcastAddr = null;
        }
        running = true;
        fireLog("Эмулятор Testa слушает UDP " + listenPort + " (статус → порт " + REPLY_PORT + ")");
        receiveThread = new Thread(this::receiveLoop, "Testa-Emu-Recv");
        receiveThread.setDaemon(true);
        receiveThread.start();
        pushThread = new Thread(this::pushLoop, "Testa-Emu-Push");
        pushThread.setDaemon(true);
        pushThread.start();
        notifyRunning(true);
        return true;
    }

    public void stop() {
        running = false;
        DatagramSocket s = socket;
        if (s != null) {
            s.close();
        }
        socket = null;
        clientAddr = null;
        if (receiveThread != null) {
            receiveThread.interrupt();
        }
        if (pushThread != null) {
            pushThread.interrupt();
        }
        fireLog("Эмулятор Testa остановлен");
        notifyRunning(false);
    }

    public boolean isRunning() { return running; }

    private void receiveLoop() {
        byte[] buf = new byte[64];
        while (running) {
            DatagramSocket s = socket;
            if (s == null) {
                break;
            }
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                s.receive(pkt);
                byte[] data = new byte[pkt.getLength()];
                System.arraycopy(pkt.getData(), pkt.getOffset(), data, 0, pkt.getLength());
                clientAddr = pkt.getAddress();
                fireLog("RX (UDP) ← " + pkt.getSocketAddress() + ": " + TestaCommands.toHex(data));
                if (commandLog != null) commandLog.dataRequest("UDP " + TestaCommands.toHex(data));
                if (isSetTemperatureDatagram(data)) {
                    float t = parseSetTemp(data);
                    emulator.setSetpoint(t);
                    emulator.setRunning(true);
                    float rh = parseFloat(data, 28);
                    if (Float.isFinite(rh) && rh >= 0 && rh <= 100) {
                        emulator.setHumiditySetpoint(rh);
                    }
                    fireLog("   SetT → уставка " + String.format(java.util.Locale.US, "%.2f", t)
                            + " °C, RH " + String.format(java.util.Locale.US, "%.1f", rh) + "%");
                    if (commandLog != null) commandLog.command("старт, установка температуры: "
                            + String.format(java.util.Locale.US, "%.2f", t) + " °C");
                    sendAck((byte) 0x02);
                    sendStatusTo(clientAddr);
                } else if (isButtonCommand(data)) {
                    int op = data[4] & 0xFF;
                    fireLog("   Команда(кнопка) op=0x" + String.format("%02X", op)
                            + " param=" + parseButtonParam(data));
                    sendAck((byte) 0x01);
                    if (op == 0x64) {
                        emulator.setRunning(false);
                        fireLog("   Стоп → состояние «остановлен»");
                        if (commandLog != null) commandLog.command("стоп");
                    } else if (op == 0x66) {
                        boolean on = parseButtonParam(data) != 0;
                        emulator.setLight(on);
                        fireLog("   Подсветка → " + (on ? "вкл" : "выкл"));
                        if (commandLog != null) commandLog.command(on ? "подсветка вкл" : "подсветка выкл");
                    }
                } else if (isProgramDatagram(data)) {
                    emulator.setRunning(true);
                    fireLog("   Программа (заголовок 77 55 33 88), длина " + data.length + " (игнор)");
                    if (commandLog != null) commandLog.command("старт (программа)");
                    sendAck((byte) 0x02);
                } else {
                    fireLog("   Неизвестная датаграмма (игнор)");
                }
            } catch (Exception e) {
                if (running) {
                    log.warn("Testa emu recv error", e);
                }
                break;
            }
        }
    }

    private void pushLoop() {
        while (running) {
            try {
                Thread.sleep(STATUS_PERIOD_MS);
            } catch (InterruptedException e) {
                break;
            }
            if (!running) {
                break;
            }
            // Если клиент себя проявил (слал SetT) — шлём юникастом ему, иначе
            // «объявляемся» широковещательно/на loopback, чтобы слушатель порта 1200
            // получал статус даже без первого SetT.
            InetAddress target = clientAddr;
            if (target != null) {
                sendStatusTo(target);
            } else {
                if (broadcastAddr != null) {
                    sendStatusTo(broadcastAddr);
                }
                if (loopbackAddr != null) {
                    sendStatusTo(loopbackAddr);
                }
            }
        }
    }

    /** Шлёт датаграмму статуса по указанному адресу на порт 1200. */
    private void sendStatusTo(InetAddress addr) {
        DatagramSocket s = socket;
        if (s == null || addr == null || s.isClosed()) {
            return;
        }
        try {
            byte[] data;
            if (emulator.isManualFrameEnabled()) {
                data = emulator.getManualFrame();
            } else {
                data = TestaCommands.buildStatusDatagram(
                        emulator.getActual(), emulator.getSetpoint(),
                        emulator.getHumidityCurrent(), emulator.getHumiditySet(),
                        emulator.getRawStatusBytes());
            }
            DatagramPacket pkt = new DatagramPacket(data, data.length, addr, REPLY_PORT);
            s.send(pkt);
            fireLog("TX (UDP) → " + addr + ":" + REPLY_PORT + ": " + TestaCommands.toHex(data));
        } catch (Exception e) {
            log.warn("Testa emu send status error", e);
        }
    }

    /**
     * Отправляет подтверждение команды (72 34 CC) на :1200 клиента. CC = код действия в очереди
     * штатки: 2 — SetT/программа, 1 — кнопка. Без ACK штатка повторяет команду (~600 мс, 6 попыток).
     */
    private void sendAck(byte code) {
        DatagramSocket s = socket;
        if (s == null || s.isClosed() || clientAddr == null) {
            return;
        }
        try {
            byte[] data = {0x72, 0x34, code};
            DatagramPacket pkt = new DatagramPacket(data, data.length, clientAddr, REPLY_PORT);
            s.send(pkt);
            fireLog("TX ACK → " + clientAddr + ":" + REPLY_PORT + ": " + TestaCommands.toHex(data));
        } catch (Exception e) {
            log.warn("Testa emu send ack error", e);
        }
    }

    private static boolean isSetTemperatureDatagram(byte[] d) {
        return d != null && d.length == 32
                && (d[0] & 0xFF) == 0x02 && (d[1] & 0xFF) == 0x33
                && (d[2] & 0xFF) == 0x88 && (d[3] & 0xFF) == 0x66;
    }

    /** Кадр «программа» (второй режим уставки), заголовок 77 55 33 88. */
    private static boolean isProgramDatagram(byte[] d) {
        return d != null && d.length >= 32
                && (d[0] & 0xFF) == 0x77 && (d[1] & 0xFF) == 0x55
                && (d[2] & 0xFF) == 0x33 && (d[3] & 0xFF) == 0x88;
    }

    /** Кнопочный кадр 23 45 34 21 (12 байт): opcode в [4], param uint32 LE в [8..11]. */
    private static boolean isButtonCommand(byte[] d) {
        return d != null && d.length >= 12
                && (d[0] & 0xFF) == 0x23 && (d[1] & 0xFF) == 0x45
                && (d[2] & 0xFF) == 0x34 && (d[3] & 0xFF) == 0x21;
    }

    private static long parseButtonParam(byte[] d) {
        long p = 0;
        for (int i = 8; i < 12; i++) {
            p |= (long) (d[i] & 0xFF) << (8 * (i - 8));
        }
        return p;
    }

    private static float parseFloat(byte[] d, int off) {
        int bits = (d[off] & 0xFF) | ((d[off + 1] & 0xFF) << 8)
                | ((d[off + 2] & 0xFF) << 16) | ((d[off + 3] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }

    private static float parseSetTemp(byte[] d) {
        return parseFloat(d, 12);
    }

    private void fireLog(String line) {
        log.info("Testa emu: {}", line);
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(line);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyRunning(boolean on) {
        for (Consumer<Boolean> l : runningListeners) {
            try {
                l.accept(on);
            } catch (Exception ignored) {
            }
        }
    }
}
