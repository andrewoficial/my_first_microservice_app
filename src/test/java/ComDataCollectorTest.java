import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.apache.logging.log4j.LoggingException;
import org.example.services.connectionPool.ComDataCollector;
import org.example.services.loggers.DeviceLogger;
import org.example.services.rule.com.ComRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;
import org.example.services.DeviceAnswer;
import org.example.services.rule.RuleStorage;
import org.example.device.ProtocolsList;
import org.example.services.connectionPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class ComDataCollectorTest {

    private ComDataCollector collector;
    private SerialPort mockPort;
    private MainLeftPanelStateCollection mockState;
    private AnyPoolService mockParentService;
    private DeviceLogger mockLogger;

    @BeforeEach
    void setUp() throws Exception {
        // Создание моков
        mockPort = mock(SerialPort.class);
        mockState = mock(MainLeftPanelStateCollection.class);
        mockParentService = mock(AnyPoolService.class);
        mockLogger = mock(DeviceLogger.class);

        // Настройка моков
        when(mockState.getBaudRateValue(anyInt())).thenReturn(9600);
        when(mockState.getDataBitsValue(anyInt())).thenReturn(8);
        when(mockState.getStopBitsValue(anyInt())).thenReturn(1);
        when(mockState.getParityBitsValue(anyInt())).thenReturn(0);
        when(mockPort.isOpen()).thenReturn(true);

        // Мокирование статического метода
        try (MockedStatic<AnswerStorage> ignored = mockStatic(AnswerStorage.class)) {
            collector = new ComDataCollector(
                    mockState,
                    ProtocolsList.DEMO_PROTOCOL,
                    "",
                    "TEST",
                    mockPort,
                    1000,
                    true,
                    1,
                    mockParentService
            );
        }

        // Подмена логгера через рефлексию
        Field clientsMapField = ComDataCollector.class.getDeclaredField("clientsMap");
        clientsMapField.setAccessible(true);
        ConcurrentHashMap<Integer, ComDataCollector.ClientData> clientsMap =
                (ConcurrentHashMap<Integer, ComDataCollector.ClientData>) clientsMapField.get(collector);

        ComDataCollector.ClientData clientData = clientsMap.get(1);
        clientData.setDeviceLogger(mockLogger); //Подменяем логгер на мок
        clientData.setNeedPool(false);
    }

    @Test
    void whenLoggerThrowsException_ExceptionIsPropagated() throws Exception {
        // Настройка мока логгера на выброс исключения
        doThrow(new RuntimeException("Mocked exception when log")).when(mockLogger).writeLine(any(DeviceAnswer.class));

        // Создание тестовых данных
        String testData = "Test data";
        boolean isResponse = true;
        long timestamp = System.currentTimeMillis();

        // Вызов приватного метода через рефлексию
        Method method = ComDataCollector.class.getDeclaredMethod(
                "saveReceivedByEvent", String.class, boolean.class, long.class
        );
        method.setAccessible(true);
        long receiveTimestamp = System.currentTimeMillis();
        //method.invoke(collector,"Test", false, startTime);

        // Проверка исключения
        Throwable exception = null;
        try{
            method.invoke(collector, testData, isResponse, receiveTimestamp);
        }catch (InvocationTargetException ex) {
            System.out.println(ex.toString());
            exception = ex.getCause();
        }finally {
            if(exception != null){
                System.out.println("Exception occurred: " + exception.getMessage());
            }
        }

        assertEquals("Mocked exception when log", exception.getMessage());
    }

    @Test
    void whenLoggerThrowsException_threadSurvives() throws Exception {
        // Настройка мока логгера на выброс исключения
        doThrow(new RuntimeException("Mocked exception when log")).when(mockLogger).writeLine(any(DeviceAnswer.class));

        // Создаем шпион для реального объекта collector
        ComDataCollector collectorSpy = spy(collector);

        // Подменяем serialPortDataListener в шпионе
        SerialPortDataListener originalListener = collector.getSerialPortDataListener();
        SerialPortDataListener spyListener = spy(originalListener);
        collectorSpy.setSerialPortDataListener(spyListener);
        collectorSpy.setTextToSendString("", "DemoCMD", 0);
        // Настраиваем поведение COM-порта для имитации данных
        byte[] testData = "Test data".getBytes();
        when(mockPort.bytesAvailable())
                .thenReturn(testData.length)  // Первый вызов - данные есть
                .thenReturn(testData.length)  // Первый вызов - данные есть
                .thenReturn(0);            // Последующие вызовы - данных нет

        when(mockPort.readBytes(any(byte[].class), anyInt()))
                .thenAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    System.arraycopy(testData, 0, buffer, 0, testData.length);
                    return testData.length;
                });

        // Запускаем коллектор в отдельном потоке
        Thread collectorThread = new Thread(collectorSpy);
        collectorThread.start();

        // Ждем запуска потока
        await().atMost(1, TimeUnit.SECONDS).until(collectorSpy::isAlive);

        // Имитируем событие DATA_AVAILABLE
        SerialPortEvent event = mock(SerialPortEvent.class);
        when(event.getEventType()).thenReturn(SerialPort.LISTENING_EVENT_DATA_AVAILABLE);

        // Вызываем обработчик события через шпиона
        spyListener.serialEvent(event);

        // Проверяем что обработчик события был вызван
        verify(spyListener).serialEvent(event);

        // Проверяем что handleDataAvailableEvent был вызван
        verify(collectorSpy).handleDataAvailableEvent();

        // Ждем обработки сообщения
        await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            // Проверяем что исключение было обработано
            verify(mockLogger, times(1)).writeLine(any(DeviceAnswer.class));
        });

        // Проверяем что поток выжил после исключения
        assertTrue(collectorSpy.isAlive(), "Поток должен остаться активным после исключения");
        assertFalse(collectorThread.isInterrupted(), "Поток не должен быть прерван");

        // Проверяем что флаг busy был сброшен
        assertFalse(collectorSpy.getComDataCollectorBusy().get());

        // Проверяем что слушатель был переустановлен
        verify(mockPort, atLeastOnce()).addDataListener(spyListener);

        // Останавливаем поток
        collectorSpy.shutdown();
        collectorThread.join(1000);
        assertFalse(collectorSpy.isAlive(), "Поток должен завершиться после shutdown");
    }

    @Test
    void whenManyMessagesReceived_allAreStoredInAnswerStorage() throws Exception {
        // Настройка COM-порта для имитации быстрых данных
        AnswerStorage.removeAnswersForTab(0);//Очищаем перед тестом
        byte[] testData = "Test data".getBytes();
        when(mockPort.bytesAvailable())
                .thenReturn(testData.length)  // Данные есть
                .thenReturn(testData.length)  // Данные есть
                .thenReturn(0);            // Данных нет

        when(mockPort.readBytes(any(byte[].class), anyInt()))
                .thenAnswer(invocation -> {
                    byte[] buffer = invocation.getArgument(0);
                    System.arraycopy(testData, 0, buffer, 0, testData.length);
                    return testData.length;
                });

        // Очищаем AnswerStorage перед тестом
        clearAnswerStorage();

        // Запускаем коллектор в отдельном потоке
        Thread collectorThread = new Thread(collector);
        collectorThread.start();

        // Ждем запуска потока
        await().atMost(1, TimeUnit.SECONDS).until(collector::isAlive);

        // Генерируем 500 событий DATA_AVAILABLE
        SerialPortEvent event = mock(SerialPortEvent.class);
        when(event.getEventType()).thenReturn(SerialPort.LISTENING_EVENT_DATA_AVAILABLE);

        for (int i = 0; i < 5; i++) {
            System.out.println("Call " + i);
            // Вызываем обработчик события
            collector.getSerialPortDataListener().serialEvent(event);
            when(mockPort.bytesAvailable())
                    .thenReturn(testData.length)  // Данные есть
                    .thenReturn(testData.length)  // Данные есть
                    .thenReturn(0);            // Данных нет
            // Небольшая задержка для имитации реальных условий
            Thread.sleep(1);
        }
        Thread.sleep(20);
        List<DeviceAnswer> answers = AnswerStorage.getAnswersForGraph(1);
        System.out.println(answers.size());
        assertEquals(500, answers.size(), "Должно быть 500 сообщений в хранилище");


        // Останавливаем поток
        collector.shutdown();
        collectorThread.join(1000);
        assertFalse(collector.isAlive(), "Поток должен завершиться после shutdown");
    }

    // Вспомогательные методы
    private void clearAnswerStorage() throws Exception {
        Field answersByTabField = AnswerStorage.class.getDeclaredField("answersByTab");
        answersByTabField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Integer, Queue<DeviceAnswer>> storage =
                (Map<Integer, Queue<DeviceAnswer>>) answersByTabField.get(null);
        storage.clear();
    }

    private int countOccurrences(String source, String substring) {
        int count = 0;
        int lastIndex = 0;

        while (lastIndex != -1) {
            lastIndex = source.indexOf(substring, lastIndex);
            if (lastIndex != -1) {
                count++;
                lastIndex += substring.length();
            }
        }
        return count;
    }

}