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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import static org.example.device.SomeDevice.log;

public class PoolLogger {
    private static final Logger log = Logger.getLogger(PoolLogger.class);
    private static final long LOG_WRITE_INTERVAL = 100L;

    private static class SingletonHolder {
        static final PoolLogger INSTANCE = new PoolLogger();
    }

    public static PoolLogger getInstance() {
        return SingletonHolder.INSTANCE;
    }

    private final Path logFile;
    private final List<String> buffer = new ArrayList<>();
    private long lastWriteTime = System.currentTimeMillis();

    private PoolLogger() {
        this.logFile = createLogFile();
    }

    private Path createLogFile() {
        String fileName = new SimpleDateFormat("yyyy.MM.dd HH-mm-ss").format(new Date()) + " SumLog.txt";
        Path path = Paths.get("logs", fileName);

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
        if (!validateAnswer(answer)) {
            return;
        }

        String logLine = formatLogLine(answer);
        synchronized (buffer) {
            buffer.add(logLine);
            tryWriteBuffer();
        }
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

    private String formatLogLine(DeviceAnswer answer) {
        return String.format("%s\t%s\t%s%n",
                answer.getAnswerReceivedTime().format(MyUtilities.CUSTOM_FORMATTER),
                answer.getDeviceType().getClass().getSimpleName().replace("Device", ""),
                answer.getAnswerReceivedString());
    }

    private void tryWriteBuffer() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastWriteTime < LOG_WRITE_INTERVAL) {
            return;
        }

        if (logFile == null || buffer.isEmpty()) {
            return;
        }

        try {
            Files.write(logFile, buffer, StandardOpenOption.APPEND);
            //log.info("Successfully wrote " + buffer.size() + " log entries");
            buffer.clear();
            lastWriteTime = currentTime;
        } catch (IOException e) {
            log.error("Failed to write log entries: " + e.getMessage());
        }
    }
}