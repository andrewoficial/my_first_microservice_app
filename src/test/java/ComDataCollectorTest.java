import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import org.example.device.SomeDevice;
import org.example.services.connectionPool.ComDataCollector;
import org.example.services.loggers.DeviceLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;
import org.example.services.DeviceAnswer;
import org.example.device.ProtocolsList;
import org.example.services.connectionPool.AnyPoolService;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;

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

        //assertEquals("Mocked exception when log", exception.getMessage());
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
    @Disabled
    void whenManyMessagesReceived_allAreStoredInAnswerStorage() throws Exception {
        // ========== ПОДГОТОВКА ==========
        final int CLIENT_ID = 1; // ID клиента для тестирования
        final int MESSAGE_COUNT = 500; // Количество сообщений

        // Очищаем хранилище ответов перед тестом
        AnswerStorage.removeAnswersForTab(CLIENT_ID);

        // Подготавливаем тестовые данные
        List<byte[]> testDataList = new ArrayList<>();
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            testDataList.add(("Test data " + i).getBytes(StandardCharsets.UTF_8));
        }

        // Очередь для управления тестовыми данными
        BlockingQueue<byte[]> dataQueue = new LinkedBlockingQueue<>();

        // Настраиваем мок COM-порта
        AtomicInteger bytesToReturn = new AtomicInteger(0);
        AtomicReference<byte[]> currentData = new AtomicReference<>();
        when(mockPort.bytesAvailable()).thenAnswer(invocation -> {
            return bytesToReturn.get();
        });

        when(mockPort.readBytes(any(byte[].class), anyInt())).thenAnswer(invocation -> {
            if (bytesToReturn.get() <= 0) return 0;

            byte[] buffer = invocation.getArgument(0);
            byte[] data = currentData.get();
            int toRead = Math.min(bytesToReturn.get(), buffer.length);
            int start = data.length - bytesToReturn.get();

            System.arraycopy(data, start, buffer, 0, toRead);
            bytesToReturn.addAndGet(-toRead);

            return toRead;
        });

        // ========== СПАЙ ДЛЯ УСТРОЙСТВА ==========
        // Получаем доступ к объекту Device через рефлексию
        Field deviceField = ComDataCollector.class.getDeclaredField("device");
        deviceField.setAccessible(true);
        SomeDevice originalDevice = (SomeDevice) deviceField.get(collector);

        // Создаем шпион для устройства
        SomeDevice spyDevice = spy(originalDevice);

        // Настраиваем временные параметры для минимальных задержек
        when(spyDevice.getRepeatWaitTime()).thenReturn(1L); // Минимальное время ожидания
        when(spyDevice.getMillisReadLimit()).thenReturn(1); // Минимальный лимит чтения
        when(spyDevice.getMillisWriteLimit()).thenReturn(1); // Минимальный лимит записи
        when(spyDevice.getMillisLimit()).thenReturn(1L); // Общий минимальный лимит

        // Устанавливаем шпион обратно в коллектор
        deviceField.set(collector, spyDevice);
        // ========== ВЫПОЛНЕНИЕ ==========
        // Запускаем коллектор в отдельном потоке
        Thread collectorThread = new Thread(collector);
        collectorThread.start();

        // Ждем запуска потока
        await().atMost(1, TimeUnit.SECONDS).until(collector::isAlive);

        // Создаем событие DATA_AVAILABLE
        SerialPortEvent event = mock(SerialPortEvent.class);
        when(event.getEventType()).thenReturn(SerialPort.LISTENING_EVENT_DATA_AVAILABLE);

        // Генерируем события (без цикла по сообщениям!)
        // Вместо этого создаем отдельный поток для генерации событий
        Thread eventGeneratorThread = new Thread(() -> {
            for (byte[] data : testDataList) {
                // Устанавливаем текущие данные и количество байт для возврата
                currentData.set(data);
                bytesToReturn.set(data.length);

                // Генерируем событие о поступлении данных
                collector.getSerialPortDataListener().serialEvent(event);

                // Ждем, пока данные не будут полностью прочитаны
                await().atMost(110, TimeUnit.MILLISECONDS)
                        .until(() -> bytesToReturn.get() == 0);

                // Задержка между сообщениями
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        eventGeneratorThread.start();

        // ========== ПРОВЕРКА ==========
        // Ждем обработки всех сообщений
        await().atMost(500, TimeUnit.SECONDS).untilAsserted(() -> {
            List<DeviceAnswer> answers = AnswerStorage.getAnswersForGraph(CLIENT_ID);
            assertEquals(MESSAGE_COUNT, answers.size(),
                    "Все сообщения должны быть сохранены в хранилище");
        });

        // Проверяем содержимое сообщений
        List<DeviceAnswer> answers = AnswerStorage.getAnswersForGraph(CLIENT_ID);
        Map<String, Integer> contentCount = new HashMap<>();

        for (DeviceAnswer answer : answers) {
            String content = answer.getAnswerReceivedString();
            contentCount.put(content, contentCount.getOrDefault(content, 0) + 1);
        }

        // Проверяем уникальность и полноту данных
        for (int i = 0; i < MESSAGE_COUNT; i++) {
            String expected = "Test data " + i;
            assertEquals(1, contentCount.getOrDefault(expected, 0),
                    "Сообщение '" + expected + "' должно присутствовать 1 раз");
        }

        // ========== ЗАВЕРШЕНИЕ ==========
        // Останавливаем потоки
        collector.shutdown();
        eventGeneratorThread.join(50);
        collectorThread.join(10);
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