package org.example.gui.devices.testa.emulation;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Отладочный UDP-сниффер на базе NIO {@link Selector}: открывает сразу большой диапазон
 * локальных UDP-портов (вплоть до почти всех 1..65535) и мультиплексирует приём в небольшом
 * числе потоков. Логирует любую пришедшую дейтаграмму: локальный порт, отправитель, hex.
 * Нужен, чтобы выяснить, на какой порт штатная программа шлёт команды.
 */
public class TestaUdpSniffer {

    /** Сколько каналов обслуживает один поток-селектор (чтобы не плодить тысячи потоков). */
    private static final int CHANNELS_PER_THREAD = 3000;
    private static final int BUF_SIZE = 65536;

    private final int startPort;
    private final int count;
    private final Consumer<String> log;
    private volatile boolean running = false;
    private final List<Thread> threads = new ArrayList<>();
    private final List<Selector> selectors = new ArrayList<>();
    private final List<DatagramChannel> channels = new ArrayList<>();

    public TestaUdpSniffer(int startPort, int count, Consumer<String> log) {
        this.startPort = Math.max(1, startPort);
        this.count = count;
        this.log = log;
    }

    /** Открывает сокеты начиная с {@code startPort}, распределяя их по селекторам. */
    public synchronized boolean start() {
        if (running) {
            return false;
        }
        running = true;
        List<DatagramChannel> chunk = new ArrayList<>();
        Selector sel = null;
        int opened = 0;
        int lastEnd = startPort + count - 1;
        if (lastEnd > 65535) {
            lastEnd = 65535;
        }
        for (int port = startPort; port <= lastEnd; port++) {
            try {
                if (sel == null) {
                    sel = Selector.open();
                }
                DatagramChannel ch = DatagramChannel.open();
                ch.configureBlocking(false);
                ch.bind(new InetSocketAddress(port));
                ch.register(sel, SelectionKey.OP_READ);
                chunk.add(ch);
                channels.add(ch);
                opened++;
                if (chunk.size() >= CHANNELS_PER_THREAD) {
                    final Selector s = sel;
                    Thread t = new Thread(() -> selectLoop(s), "Testa-Sniff");
                    t.setDaemon(true);
                    t.start();
                    threads.add(t);
                    selectors.add(sel);
                    chunk = new ArrayList<>();
                    sel = null;
                }
            } catch (Exception e) {
                // порт занят или исчерпаны ресурсы ОС — пропускаем
            }
        }
        if (sel != null && !chunk.isEmpty()) {
            final Selector s = sel;
            Thread t = new Thread(() -> selectLoop(s), "Testa-Sniff");
            t.setDaemon(true);
            t.start();
            threads.add(t);
            selectors.add(sel);
        }
        emit("Сниффер UDP: пытаюсь слушать " + startPort + ".." + lastEnd
                + " (" + opened + " портов открыто, " + selectors.size() + " потоков)");
        return opened > 0;
    }

    private void selectLoop(Selector sel) {
        ByteBuffer buf = ByteBuffer.allocate(BUF_SIZE);
        while (running) {
            try {
                if (sel.select(500) == 0) {
                    continue;
                }
                Set<SelectionKey> keys = sel.selectedKeys();
                java.util.Iterator<SelectionKey> it = keys.iterator();
                while (it.hasNext()) {
                    SelectionKey key = it.next();
                    it.remove();
                    if (!key.isValid() || !key.isReadable()) {
                        continue;
                    }
                    DatagramChannel ch = (DatagramChannel) key.channel();
                    int port = ((InetSocketAddress) ch.getLocalAddress()).getPort();
                    buf.clear();
                    SocketAddress src;
                    try {
                        src = ch.receive(buf);
                    } catch (Exception e) {
                        continue;
                    }
                    if (src == null) {
                        continue;
                    }
                    buf.flip();
                    int len = buf.remaining();
                    byte[] data = new byte[len];
                    buf.get(data);
                    emit("SNIFF ← ПОРТ " + port + " от " + src
                            + " (" + len + " б): " + toHex(data));
                }
            } catch (Exception e) {
                if (running) {
                    break;
                }
            }
        }
        try {
            sel.close();
        } catch (Exception ignored) {
        }
    }

    public synchronized void stop() {
        running = false;
        for (DatagramChannel ch : channels) {
            try {
                ch.close();
            } catch (Exception ignored) {
            }
        }
        for (Selector s : selectors) {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        }
        for (Thread t : threads) {
            t.interrupt();
        }
        channels.clear();
        selectors.clear();
        threads.clear();
        emit("Сниффер UDP остановлен");
    }

    public synchronized boolean isRunning() {
        return running;
    }

    private void emit(String line) {
        if (log != null) {
            try {
                log.accept(line);
            } catch (Exception ignored) {
            }
        }
    }

    private static String toHex(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (byte b : data) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
