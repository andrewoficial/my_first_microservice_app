package org.example.gui.devices.binder.camera.control;

import lombok.extern.slf4j.Slf4j;
import org.example.device.ethernet.binder.BinderCommandRegistry;
import org.example.device.ethernet.binder.BinderModbus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * TCP-клиент для Binder (по умолчанию {@code host:10001}). Инкапсулирует соединение,
 * периодический опрос температуры и влажностную авто-логику (как {@code BinderChamber}):
 * при {@code temp > -6} влажность включается, при {@code temp < -12} — выключается,
 * в диапазоне {@code -12..-6} — без изменений.
 *
 * <p>Вся работа с сокетом выполняется на единственном фоновом потоке (не на EDT).
 * Результаты доставляются слушателям.
 */
@Slf4j
public class BinderCommunicationService {

    public static final int DEFAULT_PORT = 10001;
    public static final int CONNECT_TIMEOUT_MS = 3000;
    public static final int READ_TIMEOUT_MS = 2000;
    private static final int ATTEMPTS = 3;
    private static final long DEFAULT_POLL_MS = 1500;

    private final CopyOnWriteArrayList<Consumer<Float>> tempListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> humidityListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<String>> statusListeners = new CopyOnWriteArrayList<>();

    private volatile Socket socket;
    private volatile BinderModbus modbus;
    private volatile String host;
    private volatile int port = DEFAULT_PORT;
    private volatile int slaveId = 1;

    /** Кэш влажности: null — состояние неизвестно, команду следует отправить. */
    private volatile Boolean humidityControlOn = null;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "BinderClient");
                t.setDaemon(true);
                return t;
            });

    private volatile boolean polling = false;
    private java.util.concurrent.ScheduledFuture<?> pollFuture;

    // ─── соединение ───────────────────────────────────────────────────────

    /** Синхронное подключение (вызывать НЕ из EDT). */
    public synchronized boolean connect(String host, int port, int slaveId) {
        closeSocket();
        try {
            Socket s = new Socket();
            s.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            s.setTcpNoDelay(true);
            s.setSoTimeout(READ_TIMEOUT_MS);
            this.socket = s;
            this.host = host;
            this.port = port;
            this.slaveId = slaveId;
            this.modbus = new BinderModbus(s, slaveId);
            this.humidityControlOn = null;
            fireStatus("Подключено к " + host + ":" + port + " (slave " + slaveId + ")");
            fireLog("TCP подключено к " + host + ":" + port);
            return true;
        } catch (IOException e) {
            log.warn("Binder connect fail {}:{}", host, port, e);
            closeSocket();
            fireStatus("Ошибка подключения: " + e.getMessage());
            return false;
        }
    }

    public synchronized void disconnect() {
        closeSocket();
        fireStatus("Отключено");
    }

    private void closeSocket() {
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        socket = null;
        modbus = null;
        humidityControlOn = null;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    // ─── опрос ────────────────────────────────────────────────────────────

    public synchronized void setPollingEnabled(boolean enable) {
        if (enable == polling) {
            return;
        }
        polling = enable;
        if (enable) {
            pollFuture = executor.scheduleWithFixedDelay(this::pollOnce, 0, DEFAULT_POLL_MS, TimeUnit.MILLISECONDS);
        } else if (pollFuture != null) {
            pollFuture.cancel(false);
            pollFuture = null;
        }
    }

    private void pollOnce() {
        if (!isConnected()) {
            return;
        }
        doGetTemperature();
    }

    // ─── операции ─────────────────────────────────────────────────────────

    /** Асинхронно задать уставку температуры, °C. */
    public void setTargetTemperature(float temperature) {
        executor.execute(() -> {
            BinderModbus m = modbus;
            if (m == null || !isConnected()) {
                fireLog("SetT отменено: нет соединения");
                return;
            }
            fireLog("TX [SetT] " + toHex(BinderCommandRegistry.buildSetTemperatureFrame(slaveId, temperature)));
            for (int i = 0; i < ATTEMPTS; i++) {
                try {
                    m.setTemperature(temperature);
                    fireLog("SetT принято (уставка " + formatTemp(temperature) + " °C)");
                    fireStatus("Уставка: " + formatTemp(temperature) + " °C");
                    return;
                } catch (IOException e) {
                    fireLog("SetT попытка " + (i + 1) + " не удалась: " + e.getMessage());
                    sleepRetry();
                }
            }
            fireStatus("Ошибка SetT после " + ATTEMPTS + " попыток");
        });
    }

    private void doGetTemperature() {
        BinderModbus m = modbus;
        if (m == null || !isConnected()) {
            return;
        }
        fireLog("TX [GetT] " + toHex(BinderCommandRegistry.buildGetTemperatureFrame(slaveId)));
        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                float t = m.readTemperature();
                fireLog("RX [GetT] " + formatTemp(t) + " °C");
                notifyTemperature(t);
                applyHumidityRule(t);
                return;
            } catch (IOException e) {
                fireLog("GetT попытка " + (i + 1) + " не удалась: " + e.getMessage());
                sleepRetry();
            }
        }
        fireStatus("Ошибка GetT после " + ATTEMPTS + " попыток");
    }

    private void applyHumidityRule(float temp) {
        boolean need;
        if (temp > -6.0f) {
            need = true;
        } else if (temp < -12.0f) {
            need = false;
        } else {
            return;
        }
        Boolean cached = humidityControlOn;
        if (cached != null && cached == need) {
            return;
        }
        BinderModbus m = modbus;
        if (m == null) {
            return;
        }
        fireLog("TX [SetHC] влажность " + (need ? "ВКЛ" : "ВЫКЛ") + " (t=" + formatTemp(temp) + " °C)");
        for (int i = 0; i < ATTEMPTS; i++) {
            try {
                m.setHumidity(need);
                humidityControlOn = need;
                notifyHumidity(need);
                return;
            } catch (IOException e) {
                fireLog("SetHC попытка " + (i + 1) + " не удалась: " + e.getMessage());
                sleepRetry();
            }
        }
    }

    private void sleepRetry() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    // ─── слушатели ────────────────────────────────────────────────────────

    public void addTemperatureListener(Consumer<Float> l) { tempListeners.add(l); }
    public void addHumidityListener(Consumer<Boolean> l) { humidityListeners.add(l); }
    public void addLogListener(Consumer<String> l) { logListeners.add(l); }
    public void addStatusListener(Consumer<String> l) { statusListeners.add(l); }

    public void shutdown() {
        polling = false;
        executor.shutdownNow();
        closeSocket();
    }
    private void notifyTemperature(float t) {
        for (Consumer<Float> l : tempListeners) {
            try {
                l.accept(t);
            } catch (Exception ignored) {
            }
        }
    }

    private void notifyHumidity(boolean on) {
        for (Consumer<Boolean> l : humidityListeners) {
            try {
                l.accept(on);
            } catch (Exception ignored) {
            }
        }
    }

    private void fireLog(String line) {
        log.info("Binder client: {}", line);
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

    private static String formatTemp(float t) {
        return String.format(java.util.Locale.US, "%.2f", t);
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            String h = Integer.toHexString(b & 0xFF).toUpperCase();
            sb.append(h.length() < 2 ? "0" + h : h).append(' ');
        }
        return sb.toString().trim();
    }
}
