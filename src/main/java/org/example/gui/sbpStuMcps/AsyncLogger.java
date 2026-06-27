package org.example.gui.sbpStuMcps;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Неблокирующий асинхронный логгер для быстрого обмена с устройством.
 * Не использует synchronized логирование в hot-path потока порта/таймеров.
 */
public final class AsyncLogger {

    private static final int QUEUE_CAPACITY = 2048;
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final Thread writerThread;
    private final BufferedWriter writer;

    public AsyncLogger(String logFilePath) {
        BufferedWriter tempWriter;
        try {
            tempWriter = new BufferedWriter(new FileWriter(logFilePath, true));
        } catch (IOException e) {
            System.err.println("Failed to open log file: " + logFilePath + ", falling back to console");
            tempWriter = null;
        }
        this.writer = tempWriter;

        this.writerThread = new Thread(this::writeLoop, "McpsAsyncLogger");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    public void log(String level, String message) {
        String entry = String.format("%s [%s] %s%n",
                LocalDateTime.now().format(TS_FORMAT), level, message);
        if (!queue.offer(entry)) {
            queue.poll(); // drop oldest
            queue.offer(entry);
        }
    }

    public void info(String msg) { log("INFO", msg); }
    public void warn(String msg) { log("WARN", msg); }
    public void error(String msg) { log("ERROR", msg); }
    public void debug(String msg) { log("DEBUG", msg); }

    private void writeLoop() {
        while (running.get() || !queue.isEmpty()) {
            try {
                String entry = queue.poll(200, TimeUnit.MILLISECONDS);
                if (entry != null && writer != null) {
                    writer.write(entry);
                    writer.flush();
                } else if (entry != null) {
                    System.out.print(entry);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (IOException e) {
                System.err.println("Log write error: " + e.getMessage());
            }
        }
        try { if (writer != null) writer.close(); } catch (IOException ignored) {}
    }

    public void shutdown() {
        running.set(false);
        try { writerThread.join(1000); } catch (InterruptedException ignored) {}
    }
}
