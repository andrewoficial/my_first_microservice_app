package org.example.gui.devices.binder.camera.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.device.ethernet.binder.BinderCommandRegistry;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * TCP-сервер, имитирующий климатическую камеру Binder (порт по умолчанию 10001).
 *
 * <p>Принимает Modbus-подобные кадры запроса, проверяет CRC, изменяет состояние
 * {@link BinderEmulator} (SetT / SetHC) либо возвращает измеренную температуру (GetT),
 * и пишет ответ. Каждый кадр логируется в hex.
 */
@Slf4j
public class BinderServerService {

    public static final int DEFAULT_PORT = 10001;

    private final BinderEmulator emulator;
    private final CopyOnWriteArrayList<Consumer<String>> logListeners = new CopyOnWriteArrayList<>();
    private final CopyOnWriteArrayList<Consumer<Boolean>> connectionListeners = new CopyOnWriteArrayList<>();

    private ServerSocket serverSocket;
    private Thread acceptThread;
    private volatile boolean running = false;

    public BinderServerService(BinderEmulator emulator) {
        this.emulator = emulator;
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
                // Реальная камера держит одно соединение длительное время (запросы идут
                // с паузами > таймаута чтения). Таймаут отключаем: соединение живёт,
                // пока клиент его не закроет.
                socket.setSoTimeout(0);
                Thread h = new Thread(() -> handleClient(socket), "Binder-Emu-Conn");
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

    private void handleClient(Socket socket) {
        try (Socket s = socket) {
            String remote = s.getRemoteSocketAddress().toString();
            fireConnection(true);
            fireLog("Клиент подключён: " + remote);
            InputStream in = s.getInputStream();
            OutputStream out = s.getOutputStream();

            while (running && !s.isClosed()) {
                int first = in.read();
                if (first < 0) {
                    break;
                }
                int func = in.read();
                if (func < 0) {
                    break;
                }

                int total;
                if (func == 0x03) {
                    total = 8;                 // GetT
                } else if (func == 0x06) {
                    total = 8;                 // SetHC
                } else if (func == 0x10) {
                    total = 13;                // SetT
                } else {
                    fireLog("Неизвестная функция 0x" + hexByte(func));
                    break;
                }

                byte[] frame = new byte[total];
                frame[0] = (byte) first;
                frame[1] = (byte) func;
                byte[] body = readN(in, total - 2);
                if (body == null) {
                    break;
                }
                System.arraycopy(body, 0, frame, 2, body.length);

                int lenNoCrc = total - 2;
                if (!BinderCommandRegistry.checkCrc(frame, lenNoCrc)) {
                    fireLog("RX (CRC ERR, " + total + "B): " + toHex(frame));
                    continue;
                }

                int slave = frame[0] & 0xFF;
                fireLog("RX (" + total + "B) slave=" + slave + " fn=0x" + hexByte(func) + ": " + toHex(frame));

                if (func == 0x03) {
                    float measured = (float) emulator.getMeasuredTemperature();
                    out.write(buildGetTempReply(slave, measured));
                    out.flush();
                } else if (func == 0x10) {
                    float t = parseSetTempFloat(frame);
                    emulator.setSetpoint(t);
                    out.write(echo(frame, 6));
                    out.flush();
                    fireLog("   SetT → уставка " + String.format(java.util.Locale.US, "%.2f", t) + " °C");
                } else if (func == 0x06) {
                    boolean on = (frame[5] & 0xFF) == 0x81;
                    emulator.setHumidityControl(on);
                    out.write(echo(frame, 6));
                    out.flush();
                    fireLog("   SetHC → влажность " + (on ? "ВКЛ" : "ВЫКЛ"));
                }
            }
            fireLog("Клиент отключён: " + remote);
        } catch (EOFException e) {
            // клиент закрыл поток
        } catch (IOException e) {
            if (running) {
                log.warn("Binder emu: conn error", e);
            }
        } finally {
            fireConnection(false);
        }
    }

    private byte[] buildGetTempReply(int slave, float temperature) {
        int bits = Float.floatToRawIntBits(temperature);
        byte b0 = (byte) (bits & 0xFF);
        byte b1 = (byte) ((bits >>> 8) & 0xFF);
        byte b2 = (byte) ((bits >>> 16) & 0xFF);
        byte b3 = (byte) ((bits >>> 24) & 0xFF);
        byte[] f = new byte[9];
        f[0] = (byte) slave;
        f[1] = 0x03;
        f[2] = 0x04;
        f[3] = b1;
        f[4] = b0;
        f[5] = b3;
        f[6] = b2;
        int crc = BinderCommandRegistry.crc16Modbus(f, 7);
        f[7] = (byte) (crc & 0xFF);
        f[8] = (byte) ((crc >>> 8) & 0xFF);
        return f;
    }

    private float parseSetTempFloat(byte[] frame) {
        int bits = (frame[8] & 0xFF) | ((frame[7] & 0xFF) << 8)
                | ((frame[10] & 0xFF) << 16) | ((frame[9] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }

    /** Эхо-ответ на write: первые {@code keep} байт запроса + CRC. */
    private byte[] echo(byte[] request, int keep) {
        byte[] f = new byte[keep + 2];
        System.arraycopy(request, 0, f, 0, keep);
        int crc = BinderCommandRegistry.crc16Modbus(f, keep);
        f[keep] = (byte) (crc & 0xFF);
        f[keep + 1] = (byte) ((crc >>> 8) & 0xFF);
        return f;
    }

    private byte[] readN(InputStream in, int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r < 0) {
                throw new EOFException();
            }
            off += r;
        }
        return buf;
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

    private static String hexByte(int v) {
        String h = Integer.toHexString(v & 0xFF).toUpperCase();
        return h.length() < 2 ? "0" + h : h;
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (byte b : data) {
            sb.append(hexByte(b & 0xFF)).append(' ');
        }
        return sb.toString().trim();
    }
}
