
import org.apache.log4j.Logger;
import org.example.device.ProtocolsList;
import org.example.device.SomeDevice;
import org.example.services.DeviceAnswer;
import org.example.services.loggers.PoolLogger;
import org.example.utilites.MyUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PoolLoggerTest {
    private static final Logger log = Logger.getLogger(PoolLoggerTest.class);
    private static final String FIXED_TIME_STR = "2025-08-08T22:00:42.682Z";
    private static final Instant FIXED_INSTANT = Instant.parse(FIXED_TIME_STR);
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());
    private static final LocalDateTime FIXED_TIME = LocalDateTime.now(FIXED_CLOCK);

    @TempDir
    Path tempDir;

    private PoolLogger logger;
    private DeviceAnswer deviceAnswer;
    private Clock testClock;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        log.info("Starting test: " + testInfo.getDisplayName());
        
        testClock = Clock.fixed(FIXED_INSTANT, ZoneId.systemDefault());
        
        // Reset singleton instance before each test
        resetPoolLoggerSingleton();
        
        // Create logger with test parameters
        logger = new PoolLogger(
            testClock,
            tempDir.toString(),
            time -> "test-pool.log",
            100L, // write interval
            5     // buffer limit
        );
        
        // Create test DeviceAnswer
        deviceAnswer = mock(DeviceAnswer.class);
        when(deviceAnswer.getClientId()).thenReturn(1);
        when(deviceAnswer.getRequestSendTime()).thenReturn(FIXED_TIME.minusSeconds(1));
        when(deviceAnswer.getAnswerReceivedTime()).thenReturn(FIXED_TIME);
        when(deviceAnswer.getAnswerReceivedString()).thenReturn("test response");
        SomeDevice testDevice = MyUtilities.createDeviceByProtocol(ProtocolsList.DEMO_PROTOCOL);
        when(deviceAnswer.getDeviceType()).thenReturn(testDevice);
    }

    @AfterEach
    void tearDown(TestInfo testInfo) {
        log.info("Finished test: " + testInfo.getDisplayName());
    }

    private void resetPoolLoggerSingleton() {
        try {
            var field = PoolLogger.class.getDeclaredField("instance");
            field.setAccessible(true);
            field.set(null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to reset singleton", e);
        }
    }

    @Test
    @DisplayName("инициализация создает файл лога")
    void init_createsLogFile() throws IOException {
        assertNotNull(logger.getLogFile());
        assertTrue(Files.exists(logger.getLogFile()));
        assertTrue(Files.isRegularFile(logger.getLogFile()));

        logger.writeLine(deviceAnswer);
        assertTrue(Files.exists(logger.getLogFile()));
        assertFalse(Files.size(logger.getLogFile()) > 0);
        logger.flush();
        assertTrue(Files.size(logger.getLogFile()) > 0);

    }

    @Test
    @DisplayName("буферизация данных перед записью")
    void writeLine_buffersData() {
        logger.writeLine(deviceAnswer);
        assertEquals(1, getInternalBufferSize());
        assertEquals(0, getFileLineCount());
    }

    @Test
    @DisplayName("сброс буфера по истечении интервала")
    void writeLine_flushesAfterInterval() {
        logger.writeLine(deviceAnswer);
        assertEquals(1, getInternalBufferSize());
        
        // Advance time beyond write interval
        testClock = Clock.offset(testClock, Duration.ofMillis(150));
        logger.setClock(testClock);
        
        logger.writeLine(deviceAnswer);
        assertEquals(0, getInternalBufferSize());
        assertTrue(getFileLineCount() > 0);
    }

    @Test
    @DisplayName("сброс буфера при достижении лимита")
    void writeLine_flushesWhenBufferFull() {
        for (int i = 0; i < 4; i++) {
            logger.writeLine(deviceAnswer);
        }
        assertEquals(4, getInternalBufferSize());
        
        logger.writeLine(deviceAnswer); // 5th line triggers flush
        assertEquals(0, getInternalBufferSize());
        assertEquals(5, getFileLineCount());
    }

    @Test
    @DisplayName("явный вызов flush записывает данные")
    void flush_writesDataImmediately() {
        logger.writeLine(deviceAnswer);
        logger.writeLine(deviceAnswer);
        assertEquals(2, getInternalBufferSize());
        
        logger.flush();
        assertEquals(0, getInternalBufferSize());
        assertEquals(2, getFileLineCount());
    }

    @Test
    @DisplayName("невалидный DeviceAnswer не записывается")
    void writeLine_rejectsInvalidAnswer() {
        // Null answer
        DeviceAnswer nullAnswer = null;
        logger.writeLine(nullAnswer);
        assertEquals(0, getInternalBufferSize());
        
        // Invalid client ID
        DeviceAnswer invalidClient = mock(DeviceAnswer.class);
        when(invalidClient.getClientId()).thenReturn(-1);
        logger.writeLine(invalidClient);
        assertEquals(0, getInternalBufferSize());
        
        // Empty response
        DeviceAnswer emptyResponse = mock(DeviceAnswer.class);
        when(emptyResponse.getClientId()).thenReturn(1);
        when(emptyResponse.getRequestSendTime()).thenReturn(FIXED_TIME);
        when(emptyResponse.getAnswerReceivedTime()).thenReturn(FIXED_TIME.plusSeconds(1));
        when(emptyResponse.getAnswerReceivedString()).thenReturn("");
        logger.writeLine(emptyResponse);
        assertEquals(0, getInternalBufferSize());
        
        // Invalid timestamps
        DeviceAnswer invalidTimestamps = mock(DeviceAnswer.class);
        when(invalidTimestamps.getClientId()).thenReturn(1);
        when(invalidTimestamps.getRequestSendTime()).thenReturn(FIXED_TIME);
        when(invalidTimestamps.getAnswerReceivedTime()).thenReturn(FIXED_TIME.minusSeconds(1));
        //when(invalidTimestamps.getAnswerReceivedString()).thenReturn("test");
        logger.writeLine(invalidTimestamps);
        assertEquals(0, getInternalBufferSize());

        logger.writeLine(deviceAnswer);
        assertEquals(1, getInternalBufferSize());
    }

    @Test
    @DisplayName("корректное форматирование строки лога")
    void formatLogLine_correctFormatting() throws IOException {
        logger.writeLine(deviceAnswer);
        logger.flush();
        
        List<String> lines = Files.readAllLines(logger.getLogFile(), StandardCharsets.UTF_8);
        assertEquals(2, lines.size()); //Потому что логер добавляет строки
        
        String expected = String.format("%s\t%s\t%s%n",
            FIXED_TIME.format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS")),
            "DEMO_PROTOCOL",
            "test response");
        
        assertEquals(expected.trim(), lines.get(0).trim());
    }

    @Test
    @DisplayName("обработка ошибок при записи в файл")
    void writeLine_handlesIOException() throws IOException {
        // Make file read-only to cause write error
        Path file = logger.getLogFile();
        file.toFile().setReadOnly();
        
        logger.writeLine(deviceAnswer);
        logger.flush();
        
        // Should not throw exception
        assertTrue(logger.getLogFile().toFile().exists());
        assertEquals(1, getInternalBufferSize());
    }

    @Test
    @DisplayName("работа с заблокированным файлом")
    void writeLine_handlesLockedFile() throws IOException {
        try (FileLock lock = java.nio.channels.FileChannel
                .open(logger.getLogFile(), java.nio.file.StandardOpenOption.WRITE)
                .lock()) {
            
            logger.writeLine(deviceAnswer);
            logger.flush();
            
            // Data should remain in buffer
            assertEquals(1, getInternalBufferSize());
        }
        
        // After releasing lock
        logger.flush();
        assertEquals(0, getInternalBufferSize());
        assertEquals(1, getFileLineCount());
    }

    @Test
    @DisplayName("конкурентный доступ из нескольких потоков")
    void concurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int writesPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                for (int j = 0; j < writesPerThread; j++) {
                    logger.writeLine(deviceAnswer);
                }
            });
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        
        logger.flush();
        
        assertEquals(threadCount * writesPerThread, getFileLineCount());
    }

    // Helper methods
    private int getInternalBufferSize() {
        try {
            var field = PoolLogger.class.getDeclaredField("buffer");
            field.setAccessible(true);
            return ((List<?>) field.get(logger)).size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to access buffer", e);
        }
    }
    
    private int getFileLineCount() {
        try {
            if (!Files.exists(logger.getLogFile())) return 0;
            return Files.readAllLines(logger.getLogFile()).size() / 2;//Логгер добавляет лишнюю строку
        } catch (IOException e) {
            return 0;
        }
    }
}