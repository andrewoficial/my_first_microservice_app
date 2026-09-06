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
    private final CopyOnWriteArrayList<Consumer<Double>> humidityActualListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Double>> humiditySetpointListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> lightListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Integer>> alarmsListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Long>> relaysListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    private volatile DatagramSocket socket;
    private volatile InetAddress remoteAddr;
    private volatile int remotePort = DEFAULT_REMOTE_PORT;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "TestaClientCmd");
                t.setDaemon(true);
                return t;
            });

    private volatile Thread receiveThread;

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
            startReceiveThread();
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

    /** Фоновый поток непрерывно читает статус и обрабатывает каждый пакет сразу (без задержки очереди). */
    private void startReceiveThread() {
        Thread t = new Thread(this::receiveLoop, "TestaClientRecv");
        t.setDaemon(true);
        receiveThread = t;
        t.start();
    }

    private void receiveLoop() {
        byte[] buf = new byte[64];
        while (isConnected()) {
            DatagramSocket s = socket;
            if (s == null || s.isClosed()) {
                break;
            }
            try {
                DatagramPacket pkt = new DatagramPacket(buf, buf.length);
                s.receive(pkt);
                if (pkt.getLength() == 0) {
                    continue;
                }
                byte[] data = new byte[pkt.getLength()];
                System.arraycopy(pkt.getData(), pkt.getOffset(), data, 0, pkt.getLength());
                handleStatus(data);
            } catch (SocketTimeoutException ignored) {
                // нет данных в этом окне — нормально
            } catch (Exception e) {
                if (isConnected()) {
                    log.warn("Testa recv error", e);
                }
                break;
            }
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
        if (receiveThread != null) {
            receiveThread.interrupt();
            receiveThread = null;
        }
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

    /** Запуск статического поддержания (кадр 02 33 88 66 с темп/скорость/влажность). */
    public void startTest(double temp, double rampDegPerMin, double humiditySet, boolean humidityEnabled) {
        executor.execute(() -> {
            if (!isConnected()) {
                fireLog("Запуск отменён: нет соединения");
                return;
            }
            byte[] frame = TestaCommands.buildStartFrame(temp, rampDegPerMin, humiditySet, humidityEnabled);
            send(frame);
            fireLog("TX [Start] " + TestaCommands.toHex(frame));
        });
    }

    public void stopTest() {
        sendButton(TestaCommands.OP_STOP, 1, "Stop");
    }

    public void setLight(boolean on) {
        sendButton(TestaCommands.OP_LIGHT, on ? 1 : 0, on ? "Light ON" : "Light OFF");
    }

    /** Кнопочная команда 23 45 34 21. */
    private void sendButton(int opcode, int param, String name) {
        executor.execute(() -> {
            if (!isConnected()) {
                fireLog(name + " отменён: нет соединения");
                return;
            }
            byte[] frame = TestaCommands.buildButtonCommand(opcode, param);
            send(frame);
            fireLog("TX [" + name + " op=0x" + String.format("%02X", opcode) + "] "
                    + TestaCommands.toHex(frame));
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

    /** Совместимость: приём теперь всегда активен в фоновом потоке, настройка не нужна. */
    public void setPollingEnabled(boolean enable) {
        // no-op: статус читается непрерывно в receiveLoop
    }

    private void handleStatus(byte[] data) {
        if (!TestaCommands.isTemperatureDatagram(data)) {
            return;
        }
        double actual = TestaCommands.parseActual(data);
        double set = TestaCommands.parseSetpoint(data);
        double humCur = TestaCommands.parseHumidityActual(data);
        double humSet = TestaCommands.parseHumiditySetpoint(data);
        boolean light = TestaCommands.parseLight(data);
        int alarms = TestaCommands.parseAlarms(data);
        long relays = TestaCommands.parseFlags(data);
        fireLog("RX [GetT] " + TestaCommands.toHex(data)
                + "  (t=" + String.format(java.util.Locale.US, "%.2f", actual)
                + ", sp=" + String.format(java.util.Locale.US, "%.2f", set)
                + ", RH=" + String.format(java.util.Locale.US, "%.1f", humCur)
                + "/" + String.format(java.util.Locale.US, "%.1f", humSet)
                + "%, light=" + light + ", alarms=0x" + String.format("%02X", alarms) + ")");
        notifyTemp(actual);
        notifySetpoint(set);
        notifyHumidityActual(humCur);
        notifyHumiditySetpoint(humSet);
        notifyLight(light);
        notifyAlarms(alarms);
        notifyRelays(relays);
    }

    // ─── слушатели ────────────────────────────────────────────────────────

    public void addTemperatureListener(Consumer<Double> l) { tempListeners.add(l); }
    public void addSetpointListener(Consumer<Double> l) { setpointListeners.add(l); }
    public void addHumidityActualListener(Consumer<Double> l) { humidityActualListeners.add(l); }
    public void addHumiditySetpointListener(Consumer<Double> l) { humiditySetpointListeners.add(l); }
    public void addLightListener(Consumer<Boolean> l) { lightListeners.add(l); }
    public void addAlarmsListener(Consumer<Integer> l) { alarmsListeners.add(l); }
    public void addRelaysListener(Consumer<Long> l) { relaysListeners.add(l); }
    public void addLogListener(Consumer<String> l) { logListeners.add(l); }
    public void addStatusListener(Consumer<String> l) { statusListeners.add(l); }

    public void shutdown() {
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

    private void notifyHumidityActual(double v) {
        for (Consumer<Double> l : humidityActualListeners) {
            try {
                l.accept(v);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyHumiditySetpoint(double v) {
        for (Consumer<Double> l : humiditySetpointListeners) {
            try {
                l.accept(v);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyLight(boolean v) {
        for (Consumer<Boolean> l : lightListeners) {
            try {
                l.accept(v);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyAlarms(int v) {
        for (Consumer<Integer> l : alarmsListeners) {
            try {
                l.accept(v);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyRelays(long v) {
        for (Consumer<Long> l : relaysListeners) {
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
