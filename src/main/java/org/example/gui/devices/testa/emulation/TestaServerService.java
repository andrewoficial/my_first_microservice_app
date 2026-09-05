package org.example.gui.devices.testa.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.device.ethernet.testa.TestaCommands;

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
                if (isSetTemperatureDatagram(data)) {
                    float t = parseSetTemp(data);
                    emulator.setSetpoint(t);
                    fireLog("   SetT → уставка " + String.format(java.util.Locale.US, "%.2f", t) + " °C");
                    sendStatusTo(clientAddr);
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
                        emulator.getActual(), emulator.getSetpoint(), emulator.getRawStatusBytes());
            }
            DatagramPacket pkt = new DatagramPacket(data, data.length, addr, REPLY_PORT);
            s.send(pkt);
            fireLog("TX (UDP) → " + addr + ":" + REPLY_PORT + ": " + TestaCommands.toHex(data));
        } catch (Exception e) {
            log.warn("Testa emu send status error", e);
        }
    }

    private static boolean isSetTemperatureDatagram(byte[] d) {
        return d != null && d.length == 32
                && (d[0] & 0xFF) == 0x02 && (d[1] & 0xFF) == 0x33
                && (d[2] & 0xFF) == 0x88 && (d[3] & 0xFF) == 0x66;
    }

    private static float parseSetTemp(byte[] d) {
        int bits = (d[12] & 0xFF) | ((d[13] & 0xFF) << 8)
                | ((d[14] & 0xFF) << 16) | ((d[15] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
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
