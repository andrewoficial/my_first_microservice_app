package org.example.services.loggers;

import org.apache.log4j.Logger;
import org.example.services.DeviceAnswer;
import org.example.utilites.MyUtilities;
import org.example.utilites.properties.MyProperties;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DeviceLogger {
    private static final Logger log = Logger.getLogger(DeviceLogger.class);
    private static final long LOG_WRITE_INTERVAL = 300L;
    private final File logFile;
    private final File logFileCSV;
    private final MyProperties properties = MyProperties.getInstance();

    private Long dateTimeLastWrite = System.currentTimeMillis();
    private final ArrayList<String> stringsBuffer = new ArrayList<>();
    private final ArrayList<String> stringsBufferCSV = new ArrayList<>();

    public DeviceLogger(Object name) {
        this.logFileCSV = properties.isCsvLogState()
                ? createLogFile("logs", buildFileName("tab", "csv", name))
                : null;

        this.logFile = properties.isDbgLogState()
                ? createLogFile("logs", buildFileName("tab", "txt", name))
                : null;
    }

    private String buildFileName(String prefix, String extension, Object name) {
        return String.format("%s tab_%s.%s",
                LocalDateTime.now().format(MyUtilities.CUSTOM_FORMATTER_FILES),
                name.toString(),
                extension);
    }

    private File createLogFile(String directory, String fileName) {
        try {
            Path path = Paths.get(directory, fileName);
            Files.createDirectories(path.getParent());

            if(Files.notExists(path)) {
                Files.createFile(path);
                log.info("Создан новый лог-файл: " + path);
            }
            return path.toFile();

        } catch (IOException e) {
            log.warn("Фатальная ошибка при создании файла " + fileName, e);
            return null;
        }
    }

    public void writeLine (DeviceAnswer answer){
        StringBuilder lineCSV = null;
        StringBuilder line = null;

        // Create lineCSV builder if CSV logging is enabled
        if (properties.isCsvLogState()) {
            lineCSV = new StringBuilder();
            lineCSV.append(answer.toStringCSV());
        }

        // Create line builder if TXT logging is enabled
        if (properties.isDbgLogState()) {
            line = new StringBuilder();
            line.append(answer.toStringDBG());
        }

        if ((System.currentTimeMillis() - dateTimeLastWrite) < LOG_WRITE_INTERVAL) {
            addToBuffer(line, stringsBuffer);
            addToBuffer(lineCSV, stringsBufferCSV);
        } else {
            dateTimeLastWrite = System.currentTimeMillis();
            addToBuffer(line, stringsBuffer);
            addToBuffer(lineCSV, stringsBufferCSV);

            writeBufferedData(stringsBuffer, logFile, properties.isDbgLogState());
            writeBufferedData(stringsBufferCSV, logFileCSV, properties.isCsvLogState());

            //log.info("Завершено ведение лога...");
        }
    }



    private void addToBuffer(StringBuilder source, List<String> target) {
        if(source != null && !source.isEmpty()) {
            target.add(source.toString());
        }
    }

    private void writeBufferedData(List<String> buffer, File file, boolean isEnabled) {
        if(!isEnabled || buffer.isEmpty()) return;

        try {
            Files.write(file.toPath(), buffer,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE);
            buffer.clear();
        } catch (IOException e) {
            log.error( "Ошибка записи буфера в файл" + e.getMessage());
        }
    }
}
