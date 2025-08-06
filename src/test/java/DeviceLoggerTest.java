import org.example.services.DeviceAnswer;
import org.example.services.loggers.DeviceLogger;
import org.example.utilites.properties.MyProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceLoggerTest {

    /*
    ToDo
    1. Добавить проверку: DeviceAnswer содержит одно значение null.
    2. Сам DA null
    3. DA одно из полей единиц измерения null
    4. DA контроль записи не по таймеру а по накоплению буфера
    5. DA контроль записи по таймеру а не по накоплению буфера
    5. При создании логера (файла) с одинаковым именем просто открывает его и дописывает
     */
    @TempDir
    Path tempDir;

    @Mock
    private MyProperties properties;

    @Mock
    private DeviceAnswer deviceAnswer;

    // Тест: создание файлов при инициализации
    @Test
    void init_createsFilesWhenEnabled() {
        when(properties.isCsvLogState()).thenReturn(true);
        when(properties.isDbgLogState()).thenReturn(true);

        DeviceLogger logger = new DeviceLogger(
                "testDevice",
                properties,
                Clock.systemDefaultZone(),
                tempDir.toString(),
                name -> name + "_log"
        );

        assertNotNull(logger.getLogFile());
        assertNotNull(logger.getLogFileCSV());
        assertTrue(logger.getLogFile().exists());
        assertTrue(logger.getLogFileCSV().exists());
    }

    // Тест: данные буферизуются и записываются по истечении интервала
    @Test
    void writeLine_flushesAfterInterval() {
        // Настраиваем часы с фиксированным временем
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        when(properties.isDbgLogState()).thenReturn(true);
        when(deviceAnswer.toStringDBG()).thenReturn("debug_data");

        DeviceLogger logger = new DeviceLogger(
                "test", properties, clock, tempDir.toString(), name -> name.toString()
        );

        // Подменяем clock для контроля времени
        Clock laterClock = Clock.offset(clock, java.time.Duration.ofMillis(400));
        logger.setLastWriteTime(clock.millis()); // Начальное время

        logger.writeLine(deviceAnswer);

        // Данные в буфере, но не записаны
        assertEquals(1, logger.getTxtBuffer().size());
        assertTrue(logger.getLogFile().length() == 0);

        // Эмулируем сдвиг времени
        logger.setLastWriteTime(laterClock.millis() - 900); // Прошло 350 мс
        logger.writeLine(deviceAnswer);

        // Должен сработать flush
        assertEquals(0, logger.getTxtBuffer().size());
        assertTrue(logger.getLogFile().length() > 0);
    }

    // Тест: явный вызов flush
    @Test
    void flush_writesBuffersImmediately() {
        when(properties.isCsvLogState()).thenReturn(true);
        when(deviceAnswer.toStringCSV()).thenReturn("csv_data");

        DeviceLogger logger = new DeviceLogger(
                "test", properties, Clock.systemDefaultZone(), tempDir.toString(), Object::toString
        );

        logger.writeLine(deviceAnswer);
        logger.flush(); // Явная запись

        assertEquals(0, logger.getCsvBuffer().size());
        assertTrue(logger.getLogFileCSV().length() > 0);
    }

    // Тест: отключенные логи не пишутся
    @Test
    void writeLine_skipsWhenDisabled() {
        when(properties.isDbgLogState()).thenReturn(false);
        when(properties.isCsvLogState()).thenReturn(false);

        DeviceLogger logger = new DeviceLogger(
                "test", properties, Clock.systemDefaultZone(), tempDir.toString(), Object::toString
        );

        logger.writeLine(deviceAnswer);
        logger.flush();

        assertTrue(logger.getTxtBuffer().isEmpty());
        assertTrue(logger.getCsvBuffer().isEmpty());
        assertNull(logger.getLogFile());
        assertNull(logger.getLogFileCSV());
    }
}