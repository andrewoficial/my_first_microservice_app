package org.example.services.loggers;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.services.DeviceAnswer;
import org.example.utilites.MyUtilities;
import org.example.utilites.properties.MyProperties;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;

public class DeviceLogger {
    private static final Logger log = Logger.getLogger(DeviceLogger.class);
    private static final long LOG_WRITE_INTERVAL = 300L;

    // Теперь получаем зависимости через конструктор
    private final MyProperties properties;
    private final Clock clock;
    private final String baseDirectory;
    private final Function<Object, String> fileNameGenerator;

    // Геттеры для тестирования
    @Getter
    private final File logFile;
    @Getter
    private final File logFileCSV;

    private final ReentrantLock lock = new ReentrantLock();
    @Getter
    private long lastWriteTime;
    private final List<String> txtBuffer = new ArrayList<>();
    private final List<String> csvBuffer = new ArrayList<>();

    /**
     * Основной конструктор с инъекцией зависимостей.
     * Позволяет подменять все компоненты при тестировании.
     *
     * @param name Имя устройства/логгера
     * @param properties Источник настроек (не синглтон)
     * @param clock Источник времени (для тестирования временных интервалов)
     * @param baseDirectory Базовая директория для логов
     * @param fileNameGenerator Стратегия генерации имен файлов
     */
    public DeviceLogger(
            Object name,
            MyProperties properties,
            Clock clock,
            String baseDirectory,
            Function<Object, String> fileNameGenerator
    ) {
        this.properties = properties;
        this.clock = clock;
        this.baseDirectory = baseDirectory;
        this.fileNameGenerator = fileNameGenerator;
        this.lastWriteTime = clock.millis();

        this.logFileCSV = properties.isCsvLogState()
                ? createLogFile(buildFileName("csv", name))
                : null;

        this.logFile = properties.isDbgLogState()
                ? createLogFile(buildFileName("txt", name))
                : null;
    }

    /**
     * Упрощенный конструктор для production-использования.
     * Использует стандартные значения:
     * - Директория: "logs"
     * - Часы: системные
     * - Генератор имен: по умолчанию
     */
    public DeviceLogger(Object name, MyProperties properties) {
        this(
                name,
                properties,
                Clock.systemDefaultZone(),
                "logs",
                // Стандартный генератор имен файлов
                deviceName -> String.format("%s tab_%s",
                        LocalDateTime.now().format(MyUtilities.CUSTOM_FORMATTER_FILES),
                        sanitize(deviceName.toString()))
        );
    }

    // Генерация имени файла с учетом расширения
    private String buildFileName(String extension, Object name) {
        return fileNameGenerator.apply(name) + "." + extension;
    }

    private static String sanitize(String input) {
        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private File createLogFile(String fileName) {
        try {
            Path path = Paths.get(baseDirectory, fileName);
            Files.createDirectories(path.getParent());

            if (Files.notExists(path)) {
                Files.createFile(path);
                log.info("Создан новый лог-файл: " + path);
            }
            return path.toFile();

        } catch (IOException e) {
            log.error("Ошибка создания файла " + fileName, e);
            return null;
        }
    }

    public void writeLine(DeviceAnswer answer) {
        lock.lock();
        log.info("Run writeLine");
        try {
            // Подготовка данных
            String txtLine = null;
            String csvLine = null;

            if (properties.isDbgLogState()) {
                txtLine = answer.toStringDBG();
                log.info("Create DBG sting");
            }
            if (properties.isCsvLogState()) {
                csvLine = answer.toStringCSV();
                log.info("Create CSV sting");
            }

            // Добавление в буферы
            if (txtLine != null)            {
                txtBuffer.add(txtLine);
            }else{
                log.info("txtLine is null");
            }
            if (csvLine != null){
                csvBuffer.add(csvLine);
            }else{
                log.info("csvLine is null");
            }

            // Проверка необходимости записи
            long currentTime = clock.millis();
            if (currentTime - lastWriteTime >= LOG_WRITE_INTERVAL) {
                //log.info("Need flush");
                flush();
                lastWriteTime = currentTime;
            }else{
                log.info("No need flush, current time is " + currentTime + " last write time is " + lastWriteTime
                + ",difference " +(currentTime - lastWriteTime) + " and interval " + LOG_WRITE_INTERVAL);
            }
        } finally {
            lock.unlock();
        }
    }

    public void flush() {
        lock.lock();
        try {
            writeBuffer(txtBuffer, logFile, properties.isDbgLogState());
            writeBuffer(csvBuffer, logFileCSV, properties.isCsvLogState());
            log.info("Done writing");
        } finally {
            lock.unlock();
        }
    }

    private void writeBuffer(List<String> buffer, File file, boolean isEnabled) {
        if(!isEnabled){
            log.info("For current file logging is disabled");
            return;
        }

        if(buffer.isEmpty()){
            log.info("For " + file.getAbsolutePath() + " is empty");
            return;
        }

        if(file == null || !file.exists()){
            log.info("For FILE is null or does not exist");
            return;
        }


        try {
            Files.write(
                    file.toPath(),
                    buffer,
                    StandardOpenOption.APPEND,
                    StandardOpenOption.CREATE
            );
            buffer.clear();
        } catch (IOException e) {
            log.error("Ошибка записи в лог: " + e.getMessage());
        }
    }

    public List<String> getTxtBuffer() {
        return new ArrayList<>(txtBuffer);
    }

    public List<String> getCsvBuffer() {
        return new ArrayList<>(csvBuffer);
    }

    public void setLastWriteTime(long millis) {
        this.lastWriteTime = millis;
    }
}