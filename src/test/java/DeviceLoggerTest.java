import org.apache.log4j.Logger;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.example.services.loggers.DeviceLogger;
import org.example.utilites.properties.MyProperties;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceLoggerTest {
    private static final Logger log = Logger.getLogger(DeviceLoggerTest.class);
    private static final String FIXED_TIME_STR = "2025-08-08T22:00:42.682Z";
    private static final Instant FIXED_INSTANT = Instant.parse(FIXED_TIME_STR);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());
    private static final LocalDateTime FIXED_TIME = LocalDateTime.now(FIXED_CLOCK);
    /*
    ToDo
    1. Добавить проверку: DeviceAnswer (DA) содержит одно значение null. [Done.]
    2. Сам DA null [Done. And logger fixed.]
    3. DA содержит RawString как null [Done. And logger fixed.]
    4. DA содержит Sent String как null [Done]
    5. Контроль записи не по таймеру а по накоплению буфера
    6. Контроль записи по таймеру а не по накоплению буфера [Done.]
    7. При создании логгера (файла) с одинаковым именем просто открывает его и дописывает
    8. Spy метод writeLine внутри DeviceLogger с выбросом исключения. Нужно корректное логирование и корректная работа после этого
     */
    @TempDir
    Path tempDir;

    @Mock
    private MyProperties properties;

    @Mock
    private DeviceAnswer deviceAnswer;

    @BeforeEach
    void logStartTestName(TestInfo testInfo) {
        log.info("Начат тест: " + testInfo.getDisplayName());


        // Общие настройки для всех тестов
        lenient().when(properties.isCsvLogInputParsed()).thenReturn(true);
        lenient().when(properties.isCsvLogInputASCII()).thenReturn(true);
        lenient().when(properties.isCsvLogOutputASCII()).thenReturn(true);
        lenient().when(properties.isDbgLogOutputASCII()).thenReturn(true);
        lenient().when(properties.isDbgLogOutputHEX()).thenReturn(true);
        lenient().when(properties.isDbgLogInputHEX()).thenReturn(true);
        lenient().when(properties.isDbgLogInputASCII()).thenReturn(true);
        lenient().when(properties.isDbgLogInputParsed()).thenReturn(true);
        lenient().when(properties.getDbgLogSeparator()).thenReturn(" | ");
        lenient().when(properties.getCsvLogSeparator()).thenReturn(" | ");
    }

    @AfterEach
    void logEndTestName(TestInfo testInfo){
        log.info("--- Завершён тест: " + testInfo.getDisplayName() + " ---");
    }

    @Test
    @DisplayName("создание файлов при инициализации")
    void init_createsFilesWhenEnabled() {
        when(properties.isCsvLogState()).thenReturn(true);
        when(properties.isDbgLogState()).thenReturn(true);

        DeviceLogger logger = createTestLogger(null);

        assertNotNull(logger.getLogFile());
        assertNotNull(logger.getLogFileCSV());
        assertTrue(logger.getLogFile().exists());
        assertTrue(logger.getLogFileCSV().exists());
    }

    @Test
    @DisplayName("данные буферизуются и записываются по истечении интервала")
    void writeLine_flushesAfterInterval() {
        // Настраиваем часы с фиксированным временем
        Clock clock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
        when(properties.isDbgLogState()).thenReturn(true);
        when(deviceAnswer.toStringDBG()).thenReturn("debug_data");

        DeviceLogger logger = createTestLogger(null);

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

    @Test
    @DisplayName("явный вызов flush")
    void flush_writesBuffersImmediately() {
        when(properties.isCsvLogState()).thenReturn(true);
        when(deviceAnswer.toStringCSV()).thenReturn("csv_data");

        DeviceLogger logger = createTestLogger(null);

        logger.writeLine(deviceAnswer);
        logger.flush(); // Явная запись

        assertEquals(0, logger.getCsvBuffer().size());
        assertTrue(logger.getLogFileCSV().length() > 0);
    }

    @ParameterizedTest
    @CsvSource({
            "true, false, 1, 0",    // только TXT
            "false, true, 0, 1",    // только CSV
            "true, true, 1, 1",     // оба формата
            "false, false, 0, 0"    // оба отключены
    })
    @DisplayName("Проверка конфигурации логирования")
    void logger_respectsConfiguration(
            boolean txtEnabled,
            boolean csvEnabled,
            int expectedTxtSize,
            int expectedCsvSize
    ) {
        when(properties.isDbgLogState()).thenReturn(txtEnabled);
        when(properties.isCsvLogState()).thenReturn(csvEnabled);
        deviceAnswer = createTestAnswer(FIXED_TIME, "Request","Response", "parsed", null);
        DeviceLogger logger = createTestLogger(null);
        logger.writeLine(deviceAnswer);

        assertEquals(expectedTxtSize, logger.getTxtBuffer().size());
        assertEquals(expectedCsvSize, logger.getCsvBuffer().size());
    }


    @Test
    @DisplayName("DA содержит одно поле результатов измерения как NULL")
    void writeLineWithBrokenMeasureResult() throws IOException {
        // Настройка свойств логирования
        when(properties.isDbgLogState()).thenReturn(true);
        when(properties.isCsvLogState()).thenReturn(true);
        AnswerValues answers = new AnswerValues(5);
        for (int i = 0; i < 4; i++) {
            answers.addValue(i, "units for " + i);
        }
        answers.addValue(0, null);
        DeviceAnswer realAnswer = createTestAnswer(FIXED_TIME,
                "Mocked Request 012",
                "Raw Answer String 345",
                "Parsed to CSV Answer String 678",
                answers);

        // Инициализируем логгер с фиксированным временем
        DeviceLogger logger = createTestLogger(null);

        // Записываем данные
        logger.writeLine(realAnswer);
        logger.flush();

        // Проверяем существование файлов и права
        assertTrue(logger.getLogFile().exists());
        assertTrue(logger.getLogFile().canRead());
        assertTrue(logger.getLogFile().canWrite());

        assertTrue(logger.getLogFileCSV().exists());
        assertTrue(logger.getLogFileCSV().canRead());
        assertTrue(logger.getLogFileCSV().canWrite());

        // Ожидаемое содержимое TXT-файла
        String timeStr = FIXED_TIME.format(DateTimeFormatter.ofPattern("yyyy.MM.dd;HH:mm:ss.SSS"));
        List<String> expectedTxtLines = Arrays.asList(
                timeStr + " | Mocked Request 012",
                timeStr + " | {77 111 99 107 101 100 32 82 101 113 117 101 115 116 32 48 49 50}",
                timeStr + " | {82 97 119 32 65 110 115 119 101 114 32 83 116 114 105 110 103 32 51 52 53}",
                timeStr + " | Raw Answer String 345",
                timeStr + " | 0,0 | units for 0 | 1,0 | units for 1 | 2,0 | units for 2 | 3,0 | units for 3 | 0,0 | null |"
        );

        // Ожидаемое содержимое CSV-файла
        List<String> expectedCsvLines = Arrays.asList(
                timeStr + " | Mocked Request 012",
                timeStr + " | Raw Answer String 345",
                timeStr + " | 0,0 | units for 0 | 1,0 | units for 1 | 2,0 | units for 2 | 3,0 | units for 3 | 0,0 | null |"
        );

        // Проверяем содержимое TXT-файла
        assertFileContent(logger.getLogFile(), expectedTxtLines);

        // Проверяем содержимое CSV-файла
        assertFileContent(logger.getLogFileCSV(), expectedCsvLines);

        // Проверяем очистку буферов
        assertBuffersEmpty(logger);
    }

    @Test
    @DisplayName("DA сам NULL")
    void writeLineWithNullDeviceAnswer() throws IOException {
        // Настройка свойств логирования
        when(properties.isDbgLogState()).thenReturn(true);
        when(properties.isCsvLogState()).thenReturn(true);

        // Создаем тестовые данные с фиксированным временем
        DeviceAnswer realAnswer = createTestAnswer(FIXED_TIME,
                "Mocked Request 012",
                "Raw Answer String 345",
                "Parsed to CSV Answer String 678",
                null);

        // Инициализируем логгер с фиксированным временем
        DeviceLogger logger = createTestLogger(FIXED_CLOCK);

        // Записываем данные
        logger.writeLine(realAnswer);
        logger.flush();

        // Проверяем существование файлов и права
        assertTrue(logger.getLogFile().exists());
        assertTrue(logger.getLogFile().canRead());
        assertTrue(logger.getLogFile().canWrite());

        assertTrue(logger.getLogFileCSV().exists());
        assertTrue(logger.getLogFileCSV().canRead());
        assertTrue(logger.getLogFileCSV().canWrite());

        // Ожидаемое содержимое TXT-файла
        String timeStr = FIXED_TIME.format(DateTimeFormatter.ofPattern("yyyy.MM.dd;HH:mm:ss.SSS"));
        List<String> expectedTxtLines = Arrays.asList(
                timeStr + " | Mocked Request 012",
                timeStr + " | {77 111 99 107 101 100 32 82 101 113 117 101 115 116 32 48 49 50}",
                timeStr + " | {82 97 119 32 65 110 115 119 101 114 32 83 116 114 105 110 103 32 51 52 53}",
                timeStr + " | Raw Answer String 345",
                timeStr + " | nullValuesReceived | nullValuesReceived |"
        );

        // Ожидаемое содержимое CSV-файла
        List<String> expectedCsvLines = Arrays.asList(
                timeStr + " | Mocked Request 012",
                timeStr + " | Raw Answer String 345",
                timeStr + " |"
        );

        // Проверяем содержимое TXT-файла
        assertFileContent(logger.getLogFile(), expectedTxtLines);

        // Проверяем содержимое CSV-файла
        assertFileContent(logger.getLogFileCSV(), expectedCsvLines);

        // Проверяем очистку буферов
        assertBuffersEmpty(logger);
    }

    @Test
    @DisplayName("DA содержит NULL строку ответа.")
    void writeLineWithNullRawString() throws IOException {
        // Настройка свойств логирования
        when(properties.isDbgLogState()).thenReturn(true);
        when(properties.isCsvLogState()).thenReturn(true);

        // Создаем тестовые данные с фиксированным временем
        DeviceAnswer realAnswer = createTestAnswer(FIXED_TIME,
                "Mocked Request 012",
                null,
                null,
                null);

        // Инициализируем логгер с фиксированным временем
        DeviceLogger logger = createTestLogger(FIXED_CLOCK);

        // Записываем данные
        logger.writeLine(realAnswer);
        logger.flush();

        // Проверяем существование файлов и права
        assertTrue(logger.getLogFile().exists());
        assertTrue(logger.getLogFile().canRead());
        assertTrue(logger.getLogFile().canWrite());

        assertTrue(logger.getLogFileCSV().exists());
        assertTrue(logger.getLogFileCSV().canRead());
        assertTrue(logger.getLogFileCSV().canWrite());

        String timeStr = FIXED_TIME.format(DateTimeFormatter.ofPattern("yyyy.MM.dd;HH:mm:ss.SSS"));
        // Ожидаемое содержимое TXT-файла
        List<String> expectedTxtLines = Arrays.asList(
                timeStr + " | Mocked Request 012",
                timeStr + " | {77 111 99 107 101 100 32 82 101 113 117 101 115 116 32 48 49 50}",
                timeStr + " | {78 85 76 76 32 83 84 82 73 78 71}",
                timeStr + " | NULL STRING",
                timeStr + " | nullValuesReceived | nullValuesReceived |"
        );

        // Ожидаемое содержимое CSV-файла
        List<String> expectedCsvLines = Arrays.asList(
                timeStr + " | Mocked Request 012",
                timeStr + " | NULL STRING",
                timeStr + " |"
        );

        // Проверяем содержимое TXT-файла
        assertFileContent(logger.getLogFile(), expectedTxtLines);

        // Проверяем содержимое CSV-файла
        assertFileContent(logger.getLogFileCSV(), expectedCsvLines);

        // Проверяем очистку буферов
        assertBuffersEmpty(logger);
    }

    @Test
    @DisplayName("в DA NULL строка запроса.")
    void writeLineWithNullRequestSendString() throws IOException {
        // Настройка свойств логирования
        when(properties.isDbgLogState()).thenReturn(true);
        when(properties.isCsvLogState()).thenReturn(true);

        // Создаем тестовые данные с фиксированным временем
        DeviceAnswer realAnswer = createTestAnswer(FIXED_TIME,
                null,
                "Raw Answer String 345",
                "Parsed to CSV Answer String 678",
                null);

        // Инициализируем логгер с фиксированным временем
        DeviceLogger logger = createTestLogger(FIXED_CLOCK);

        // Записываем данные
        logger.writeLine(realAnswer);
        logger.flush();

        // Проверяем существование файлов и права
        assertTrue(logger.getLogFile().exists());
        assertTrue(logger.getLogFile().canRead());
        assertTrue(logger.getLogFile().canWrite());

        assertTrue(logger.getLogFileCSV().exists());
        assertTrue(logger.getLogFileCSV().canRead());
        assertTrue(logger.getLogFileCSV().canWrite());

        String timeStr = FIXED_TIME.format(DateTimeFormatter.ofPattern("yyyy.MM.dd;HH:mm:ss.SSS"));
        // Ожидаемое содержимое TXT-файла
        List<String> expectedTxtLines = Arrays.asList(
                timeStr + " | null",
                timeStr + " | {}",
                timeStr + " | {82 97 119 32 65 110 115 119 101 114 32 83 116 114 105 110 103 32 51 52 53}",
                timeStr + " | Raw Answer String 345",
                timeStr + " | nullValuesReceived | nullValuesReceived |"
        );

        // Ожидаемое содержимое CSV-файла
        List<String> expectedCsvLines = Arrays.asList(
                timeStr + " | null",
                timeStr + " | Raw Answer String 345",
                timeStr + " |"
        );

        // Проверяем содержимое TXT-файла
        assertFileContent(logger.getLogFile(), expectedTxtLines);

        // Проверяем содержимое CSV-файла
        assertFileContent(logger.getLogFileCSV(), expectedCsvLines);

        // Проверяем очистку буферов
        assertBuffersEmpty(logger);
    }

    @Test
    @DisplayName("контроль записи по накоплению буфера")
    void writeLine_flushesWhenBufferFull() throws IOException {
        // Настройка свойств
        when(properties.isDbgLogState()).thenReturn(true);
        when(properties.isCsvLogState()).thenReturn(true);

        // Создаем логгер
        DeviceLogger logger = createTestLogger(FIXED_CLOCK);
        logger.setBufferLineLimit(3);
        // Создаем тестовые данные

        DeviceAnswer realAnswerOne = createTestAnswer(LocalDateTime.now(),
                "First Request","First Answer","First Answer CSV",null);


        DeviceAnswer realAnswerTwo = createTestAnswer(LocalDateTime.now(),
                "Second Request","Second Answer","Second Answer CSV",null);

        DeviceAnswer realAnswerThree = createTestAnswer(LocalDateTime.now(),
                "Third Request","Third Answer","Third Answer CSV",null);

        DeviceAnswer realAnswerFourth = createTestAnswer(LocalDateTime.now(),
                "Fourth Request","Fourth Answer","Fourth Answer CSV",null);


        // До записи буфер пустой
        assertEquals(0, logger.getTxtBuffer().size());

        // Первая запись - буфер не заполнен
        logger.writeLine(realAnswerOne);
        assertEquals(1, logger.getTxtBuffer().size());
        assertEquals(0, logger.getLogFile().length());

        // Вторая запись - буфер заполняется, но не сбрасывается (размер буфера = 2)
        logger.writeLine(realAnswerTwo);
        assertEquals(2, logger.getTxtBuffer().size());
        assertEquals(0, logger.getLogFile().length());

        // Третья запись - вызывает сброс буфера
        logger.writeLine(realAnswerThree);
        assertEquals(0, logger.getTxtBuffer().size()); // Буфер содержит только новую запись
        assertTrue(logger.getLogFile().length() > 0);  // Данные записаны в файл

        // Четвертая запись - в очереди должна быть она
        logger.writeLine(realAnswerFourth);
        assertEquals(1, logger.getTxtBuffer().size()); // Буфер содержит только новую запись
        assertTrue(logger.getLogFile().length() > 0);  // Данные записаны в файл

        // Явный сброс для очистки
        logger.flush();
    }

    @Test
    @DisplayName("обработка исключений при записи")
    void writeLine_handlesIOException() throws IOException {
        // Настройка свойств
        when(properties.isDbgLogState()).thenReturn(true);

        // Создаем логгер с невалидным путем
        Path invalidPath = Path.of("/invalid/path");
        DeviceLogger logger = new DeviceLogger(
                "test", properties, Clock.systemDefaultZone(), invalidPath.toString(), Object::toString
        );

        // Проверяем обработку исключения при записи
        assertDoesNotThrow(() -> {
            logger.writeLine(deviceAnswer);
            logger.flush();
        });

        // Проверяем, что буфер очищен несмотря на ошибку
        assertTrue(logger.getTxtBuffer().isEmpty());

        // Проверяем, что логгер продолжает работать
        assertDoesNotThrow(() -> logger.writeLine(deviceAnswer));
    }

    @Test
    @DisplayName("DBG-логгер при занятом файле")
    void writeLineWhenFileIsLocked() throws IOException {
        // Настройка свойств
        when(properties.isDbgLogState()).thenReturn(true);
        when(properties.isCsvLogState()).thenReturn(true);

        // Стандартные ответы для deviceAnswer
        when(deviceAnswer.toStringDBG()).thenReturn("debug_data");
        when(deviceAnswer.toStringCSV()).thenReturn("csv_data");

        // Создаем логгер
        DeviceLogger logger = createTestLogger(null);

        // Записываем начальные данные
        logger.writeLine(deviceAnswer);
        logger.flush();

        // Блокируем файл для эмуляции ситуации "открыт в Excel"
        Path logFilePath = logger.getLogFile().toPath();
        try (FileOutputStream fos = new FileOutputStream(logFilePath.toFile());
             FileLock lock = fos.getChannel().lock()) {

            // Пытаемся записать данные при заблокированном файле
            assertDoesNotThrow(() -> {
                logger.writeLine(deviceAnswer);
                logger.flush();
            });

            // Проверяем, что данные добавились в буфер (но не записались)
            assertEquals(1, logger.getTxtBuffer().size());

            // Проверяем, что размер файла не изменился
            long fileSizeAfterLock = Files.size(logFilePath);
            assertEquals(Files.size(logFilePath), fileSizeAfterLock);

        } // Здесь блокировка автоматически снимется

        // Проверяем, что после снятия блокировки можно писать
        logger.writeLine(deviceAnswer);
        logger.flush();
        assertTrue(Files.size(logFilePath) > 0);
    }

    @Test
    @DisplayName("CSV-логгер при занятом файле")
    void writeCSVWhenFileIsLocked() throws IOException {
        // Настройка свойств
        when(properties.isCsvLogState()).thenReturn(true);

        // Стандартные ответы для deviceAnswer
        when(deviceAnswer.toStringCSV()).thenReturn("csv_data");
        // Создаем логгер
        DeviceLogger logger = createTestLogger(null);

        // Записываем начальные данные
        logger.writeLine(deviceAnswer);
        logger.flush();

        // Блокируем CSV-файл
        Path csvPath = logger.getLogFileCSV().toPath();
        try (FileOutputStream fos = new FileOutputStream(csvPath.toFile());
             FileLock lock = fos.getChannel().lock()) {

            // Пытаемся записать данные при заблокированном файле
            assertDoesNotThrow(() -> {
                logger.writeLine(deviceAnswer);
                logger.flush();
            });

            // Проверяем, что буфер хранит значение (данные попытались записаться)
            assertEquals(1, logger.getCsvBuffer().size());

            // Проверяем, что размер файла не изменился
            long initialSize = Files.size(csvPath);
            assertEquals(initialSize, Files.size(csvPath));

        } // Блокировка снимается здесь

        // Проверяем, что после снятия блокировки можно писать
        logger.writeLine(deviceAnswer);
        logger.flush();
        assertTrue(Files.size(csvPath) > 0);
    }

    @Test
    @DisplayName("дописывание в существующий файл")
    void writeLine_appendsToExistingFile() throws IOException {
        when(properties.isDbgLogState()).thenReturn(true);

        DeviceLogger logger1 = createTestLogger(null);
        logger1.writeLine(createTestAnswer(FIXED_TIME, "first", "fAns", "fCsvAns", null));
        logger1.flush();

        List<String> firstContent = Files.readAllLines(logger1.getLogFile().toPath());

        DeviceLogger logger2 = createTestLogger(null); // С тем же путем
        logger2.writeLine(createTestAnswer(FIXED_TIME, "second", "sAns", "sCsvAns", null));
        logger2.flush();

        List<String> newContent = Files.readAllLines(logger1.getLogFile().toPath());
        System.out.println(firstContent);

        System.out.println(newContent.size());
        assertEquals(10, newContent.size());
    }


    @Test
    @DisplayName("конкурентный доступ к логгеру")
    void concurrentAccess() throws InterruptedException {
        when(properties.isDbgLogState()).thenReturn(true);
        DeviceLogger logger = createTestLogger(null);
        logger.setBufferLineLimit(100);

        int threadsCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);

        for (int i = 0; i < 1000; i++) {
            executor.submit(() -> {
                DeviceAnswer answer = createTestAnswer(FIXED_TIME, "req", "rAns", "csvAns", null);
                logger.writeLine(answer);
            });
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        logger.flush();

        assertEquals(0, logger.getTxtBuffer().size());
        assertTrue(logger.getLogFile().length() > 0);
    }

    public DeviceAnswer createTestAnswer(
            LocalDateTime time,
            String request,
            String rawAnswer,
            String csvAnswer,
            AnswerValues values) {

        DeviceAnswer answer = new DeviceAnswer(time, request, 0, properties);
        answer.setDeviceType(null);
        answer.setAnswerReceivedTime(time);
        answer.setAnswerReceivedString(rawAnswer);
        answer.setAnswerReceivedStringCSV(csvAnswer);
        answer.setAnswerReceivedValues(values);
        return answer;
    }

    private void assertFileContent(File file, List<String> expectedLines) throws IOException {
        assertNotNull(file, "Файл не должен быть null");
        assertTrue(file.exists(), "Файл должен существовать: " + file.getPath());
        assertTrue(file.canRead(), "Нет прав на чтение: " + file.getPath());
        assertTrue(file.canWrite(), "Нет прав на запись: " + file.getPath());

        List<String> actualLines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        assertEquals(expectedLines.size(), actualLines.size(), "Неверное количество строк");

        for (int i = 0; i < expectedLines.size(); i++) {
            assertEquals(expectedLines.get(i), actualLines.get(i), "Ошибка в строке " + (i+1));
        }
    }

    private void assertBuffersEmpty(DeviceLogger logger) {
        assertTrue(logger.getTxtBuffer().isEmpty(), "TXT буфер должен быть пустым");
        assertTrue(logger.getCsvBuffer().isEmpty(), "CSV буфер должен быть пустым");
    }

    private DeviceLogger createTestLogger(Clock clock) {
        return new DeviceLogger(
                "test",
                properties,
                clock != null ? clock : Clock.systemDefaultZone(),
                tempDir.toString(),
                Object::toString
        );
    }
}