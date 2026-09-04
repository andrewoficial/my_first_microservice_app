package org.example.gui.devices.testa.control;

import lombok.extern.slf4j.Slf4j;
import org.example.device.ethernet.testa.TestaCommands;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * UDP-клиент управления камерой Testa.
 * <p>Локальный приём статуса на порту 1200; команда SetT шлётся на {@code host:1300}.
 * Камера сама периодически шлёт статус на порт 1200 (GetT не запрашивается отдельно).
 */
@Slf4j
public class TestaCommunicationService {

    public static final int DEFAULT_REMOTE_PORT = TestaCommands.DEFAULT_REMOTE_PORT; // 1300
    public static final int LOCAL_RECEIVE_PORT = TestaCommands.LOCAL_RECEIVE_PORT;   // 1200
    private static final int RECV_TIMEOUT_MS = 500;
    private static final long POLL_MS = 1000;

    private final CopyOnWriteArrayList<Consumer<Double>> tempListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Double>> setpointListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    private volatile DatagramSocket socket;
    private volatile InetAddress remoteAddr;
    private volatile int remotePort = DEFAULT_REMOTE_PORT;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TestaClient");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean polling = false;

    public synchronized boolean connect(String host) {
        return connect(host, DEFAULT_REMOTE_PORT);
    }

    public synchronized boolean connect(String host, int port) {
        close();
        try {
            DatagramSocket s = new DatagramSocket(LOCAL_RECEIVE_PORT);
            s.setSoTimeout(RECV_TIMEOUT_MS);
            this.socket = s;
            this.remoteAddr = InetAddress.getByName(host);
            this.remotePort = port;
            fireStatus("Готово: приём на UDP :" + LOCAL_RECEIVE_PORT + ", камера " + host + ":" + port);
            fireLog("UDP слушаем " + LOCAL_RECEIVE_PORT + ", шлём на " + host + ":" + port);
            return true;
        } catch (Exception e) {
            log.warn("Testa connect fail {}:{}", host, port, e);
            close();
            fireStatus("Ошибка: " + e.getMessage());
            return false;
        }
    }

    public synchronized void disconnect() {
        close();
        fireStatus("Отключено");
    }

    private void close() {
        if (socket != null) {
            socket.close();
        }
        socket = null;
        remoteAddr = null;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    // ─── операции ─────────────────────────────────────────────────────────

    public void setTargetTemperature(double temp) {
        executor.execute(() -> {
            if (!isConnected()) {
                fireLog("SetT отменено: нет соединения");
                return;
            }
            byte[] frame = TestaCommands.buildSetTemperatureFrame((float) temp);
            send(frame);
            fireLog("TX [SetT] " + TestaCommands.toHex(frame));
        });
    }

    private void send(byte[] data) {
        try {
            DatagramPacket pkt = new DatagramPacket(data, data.length, remoteAddr, remotePort);
            socket.send(pkt);
        } catch (Exception e) {
            log.warn("Testa send error", e);
            fireStatus("Ошибка отправки: " + e.getMessage());
        }
    }

    public synchronized void setPollingEnabled(boolean enable) {
        if (enable == polling) {
            return;
        }
        polling = enable;
        if (enable) {
            executor.scheduleWithFixedDelay(this::pollOnce, 0, POLL_MS, TimeUnit.MILLISECONDS);
        }
    }

    private void pollOnce() {
        if (!isConnected()) {
            return;
        }
        try {
            byte[] buf = new byte[64];
            DatagramPacket pkt = new DatagramPacket(buf, buf.length);
            socket.receive(pkt);
            if (pkt.getLength() == 0) {
                return;
            }
            byte[] data = new byte[pkt.getLength()];
            System.arraycopy(pkt.getData(), pkt.getOffset(), data, 0, pkt.getLength());
            if (TestaCommands.isTemperatureDatagram(data)) {
                double actual = TestaCommands.parseActual(data);
                double set = TestaCommands.parseSetpoint(data);
                fireLog("RX [GetT] " + TestaCommands.toHex(data)
                        + "  (t=" + String.format(java.util.Locale.US, "%.2f", actual)
                        + ", sp=" + String.format(java.util.Locale.US, "%.2f", set) + ")");
                notifyTemp(actual);
                notifySetpoint(set);
            }
        } catch (SocketTimeoutException ignored) {
            // нет данных в этом окне — нормально
        } catch (Exception e) {
            log.warn("Testa recv error", e);
        }
    }

    // ─── слушатели ────────────────────────────────────────────────────────

    public void addTemperatureListener(Consumer<Double> l) { tempListeners.add(l); }
    public void addSetpointListener(Consumer<Double> l) { setpointListeners.add(l); }
    public void addLogListener(Consumer<String> l) { logListeners.add(l); }
    public void addStatusListener(Consumer<String> l) { statusListeners.add(l); }

    public void shutdown() {
        polling = false;
        executor.shutdownNow();
        close();
    }

    private void notifyTemp(double v) {
        for (Consumer<Double> l : tempListeners) {
            try {
                l.accept(v);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifySetpoint(double v) {
        for (Consumer<Double> l : setpointListeners) {
            try {
                l.accept(v);
            } catch (Exception ignored) {
            }
        }
    }

    private void fireLog(String line) {
        log.info("Testa client: {}", line);
        for (Consumer<String> l : logListeners) {
            try {
                l.accept(line);
            } catch (Exception ignored) {
            }
        }
    }

    private void fireStatus(String line) {
        for (Consumer<String> l : statusListeners) {
            try {
                l.accept(line);
            } catch (Exception ignored) {
            }
        }
    }
}
