/*
Сингл-тон объект, который при ините создает файл и если нужно дописывает в него принимаемые ответы

 */
package org.example.services.loggers;

import org.apache.log4j.Logger;
import org.example.device.SomeDevice;
import org.example.services.DeviceAnswer;
import org.example.utilites.MyUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import static org.example.device.SomeDevice.log;

public class PoolLogger {
    private static final Logger log = Logger.getLogger(PoolLogger.class);
    private static final long DEFAULT_LOG_WRITE_INTERVAL = 100L;
    private static final int DEFAULT_BUFFER_LIMIT = 50;

    // Зависимости
    private Clock clock;
    private final Path logFile;
    private final long logWriteInterval;
    private final int bufferLimit;
    private final Function<LocalDateTime, String> fileNameGenerator;

    private final ReentrantLock lock = new ReentrantLock();
    private final List<String> buffer = new ArrayList<>();
    private long lastWriteTime;
    private static PoolLogger instance;
    private static int instanceCount = 0;

    public PoolLogger(Clock clock,
                      String baseDirectory,
                      Function<LocalDateTime, String> fileNameGenerator,
                      long logWriteInterval,
                      int bufferLimit) {
        this.clock = clock;
        this.fileNameGenerator = fileNameGenerator;
        this.logWriteInterval = logWriteInterval;
        this.bufferLimit = bufferLimit;
        this.lastWriteTime = clock.millis();
        this.logFile = createLogFile(baseDirectory);
    }

    // Конструктор для продакшена
    public PoolLogger() {
        this(Clock.systemDefaultZone(),
                "logs",
                time -> DateTimeFormatter.ofPattern("yyyy.MM.dd HH-mm-ss").format(time) + " SumLog.txt",
                DEFAULT_LOG_WRITE_INTERVAL,
                DEFAULT_BUFFER_LIMIT);
    }

    public static PoolLogger getInstance() {
        if (instance == null) {
            instance = new PoolLogger();
            instanceCount++;
            if(instanceCount > 1){
                log.warn("Создано больше одного instance для PoolLogger!");
            }
        }
        return instance;
    }

    private Path createLogFile(String baseDirectory) {
        String fileName = fileNameGenerator.apply(LocalDateTime.now(clock));
        Path path = Paths.get(baseDirectory, fileName);

        try {
            Files.createDirectories(path.getParent());
            if (Files.notExists(path)) {
                Files.createFile(path);
                log.info("Created log file: " + path);
            }
            return path;
        } catch (IOException e) {
            log.error("Failed to create log file", e);
            return null;
        }
    }

    public void writeLine(DeviceAnswer answer) {
        if (!validateAnswer(answer)) return;

        String logLine = formatLogLine(answer);

        lock.lock();
        try {
            buffer.add(logLine);
            tryWriteBuffer(false);
        } finally {
            lock.unlock();
        }
    }

    public void flush() {
        tryWriteBuffer(true);
    }

    private boolean validateAnswer(DeviceAnswer answer) {
        if (answer == null) {
            log.warn("Attempted to log null answer");
            return false;
        }

        Integer clientId = answer.getClientId();
        if (clientId == null || clientId < 0) {
            log.error(String.format("Invalid client ID: %s", clientId));
            return false;
        }

        if (answer.getRequestSendTime() == null
                || answer.getAnswerReceivedTime() == null
                || answer.getAnswerReceivedTime().isBefore(answer.getRequestSendTime())) {
            log.error(String.format("Invalid timestamps for client %d", clientId));
            return false;
        }

        if (answer.getAnswerReceivedString() == null
                || answer.getAnswerReceivedString().trim().isEmpty()) {
            log.error(String.format("Отклонено логирование для клиента [%d] - пустая строка ответа", clientId));
            return false;
        }

        return true;
    }


    private void tryWriteBuffer(boolean force) {
        lock.lock();
        try {
            long currentTime = clock.millis();
            if (!force
                    && currentTime - lastWriteTime < logWriteInterval
                    && buffer.size() < bufferLimit) {
                return;
            }

            if (logFile == null || buffer.isEmpty()) {
                return;
            }

            try {
                Files.write(logFile, buffer, StandardOpenOption.APPEND);
                buffer.clear();
                lastWriteTime = currentTime;
            } catch (IOException e) {
                log.error("Failed to write log entries: " + e.getMessage());
            }
        } finally {
            lock.unlock();
        }
    }

    private String formatLogLine(DeviceAnswer answer) {
        return String.format("%s\t%s\t%s%n",
                answer.getAnswerReceivedTime().format(MyUtilities.CUSTOM_FORMATTER),
                answer.getDeviceType().getClass().getSimpleName().replace("Device", ""),
                answer.getAnswerReceivedString());
    }

    public Path getLogFile() {
        return logFile;
    }

    public int getBufferSize() {
        lock.lock();
        try {
            return buffer.size();
        } finally {
            lock.unlock();
        }
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}