package org.example.services.comPool;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.comPort.ComPort;
import org.example.services.DeviceAnswer;
import org.example.services.loggers.DeviceLogger;
import org.example.services.loggers.PoolLogger;
import org.example.device.ProtocolsList;
import org.example.device.*;
import org.example.utilites.properties.MyProperties;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.example.utilites.MyUtilities.createDeviceByProtocol;

public class ComDataCollector implements Runnable{
    private final static Logger log = Logger.getLogger(ComDataCollector.class); // Объект логера
    private final AnyPoolService parentService;//Родительский класс для получения единого для всех потомков объекта синхронизаци логирования.
    private final MyProperties myProperties = MyProperties.getInstance(); //Объект с параметрами для того, что бы определять тип логирования
    @Getter
    private volatile boolean alive = true; // Признак того, что поток жив (после введения Event всегда истина и сбрасывается в shutdown, раньше сбрасывался если нету клиентов у потока)
    @Getter
    private final AtomicInteger currentDirection = new AtomicInteger();
    @Getter
    private final AtomicBoolean comDataCollectorBusy = new AtomicBoolean(false);// Признак того, что текущее соединение по ком-порту занято
    private SomeDevice device = null;//Объект устройства, содержащий особенности обработки команд. Задается на основе выбранного протоколаё
    @Getter
    private SerialPort comPort;//Объект, обслуживающий соединение по ком-порту. Библиотека fazecast.
    @Setter
    private SerialPortDataListener serialPortDataListener;//Объект EventListener, обрабатывающий входящие без запроса сообщения. Библиотека fazecast.
    @Getter
    private long poolDelay = 2000;//Задает переодичность отправки запросов в мс.
    private long millisPrev = System.currentTimeMillis() - (poolDelay * 100);//Обслуживание таймера в миллисекундах (основная переменная poolDelay).
    private final ConcurrentHashMap<Integer, ClientData> clientsMap = new ConcurrentHashMap<>();


    private volatile boolean responseRequested = false;
    private volatile long requestTimestamp = 0;
    private final long RESPONSE_TIMEOUT_MS = 5000;  // Таймаут ожидания ответа
    private volatile String lastCommand = "";

    public ComDataCollector(ProtocolsList protocol, String prefix, String command, SerialPort comPort, int poolDelay,boolean needLog,Integer tabNumber,AnyPoolService parentService) {
        super();
        this.parentService = parentService;
        if(protocol == null || command == null || prefix == null || comPort == null || tabNumber  == null){
            log.error("Один из параметров конструктора передан как null");
            return;
        }

        try {
            device = createDeviceByProtocol(protocol, comPort);
            if (device == null) {
                throw new IllegalArgumentException("Не удалось создать устройство");
            }
        }catch (RuntimeException e){
            log.error("Device in ComDataCollector creating ERROR" + e.getMessage());
        }

        this.comPort = comPort;
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, device.getMillisReadLimit(), device.getMillisWriteLimit());
        comPort.openPort();
        this.poolDelay = poolDelay;
        clientsMap.put(tabNumber, new ClientData(tabNumber, needLog, true, "", "", null));

        serialPortDataListener = new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            final Map<Integer, String> eventTypes = new HashMap<>();
            {
                eventTypes.put(SerialPort.LISTENING_EVENT_PORT_DISCONNECTED, "Найдено событие для слушателя порта LISTENING_EVENT_PORT_DISCONNECTED");
                eventTypes.put(SerialPort.LISTENING_EVENT_BREAK_INTERRUPT, "Найдено событие для слушателя порта LISTENING_EVENT_BREAK_INTERRUPT");
                eventTypes.put(SerialPort.LISTENING_EVENT_CARRIER_DETECT, "Найдено событие для слушателя порта LISTENING_EVENT_CARRIER_DETECT");
                eventTypes.put(SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR, "Найдено событие для слушателя порта LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_FRAMING_ERROR, "Найдено событие для слушателя порта LISTENING_EVENT_FRAMING_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_PARITY_ERROR, "Найдено событие для слушателя порта LISTENING_EVENT_PARITY_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR, "Найдено событие для слушателя порта LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_TIMED_OUT, "Найдено событие для слушателя порта LISTENING_EVENT_TIMED_OUT");
                eventTypes.put(SerialPort.LISTENING_EVENT_RING_INDICATOR, "Найдено событие для слушателя порта LISTENING_EVENT_RING_INDICATOR");
                eventTypes.put(SerialPort.LISTENING_EVENT_DATA_AVAILABLE, "Найдено событие для слушателя порта LISTENING_EVENT_DATA_AVAILABLE");
                eventTypes.put(SerialPort.LISTENING_EVENT_DATA_RECEIVED, "Найдено событие для слушателя порта LISTENING_EVENT_DATA_RECEIVED");
            }
            @Override
            public void serialEvent(SerialPortEvent event) {
                String eventName = eventTypes.getOrDefault(event.getEventType(), "UNKNOWN_EVENT");
                log.warn("Найдено событие для слушателя порта " + eventName + " " + comPort.getSystemPortName());
                if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    comPort.closePort();
                } else if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE || event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                    handleDataAvailableEvent(currentDirection.get());
                }
            }
        };
    }

    private void handleDataAvailableEvent(int currentClientId) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[1024];
        long delay = device == null ? 150L : device.getMillisReadLimit();
        int sizeLimit = device == null ? 150 : device.getExpectedBytes();
        try {
            while (comPort.bytesAvailable() > 0) {
                int bytesRead = comPort.readBytes(chunk, Math.min(chunk.length, comPort.bytesAvailable()));
                buffer.write(chunk, buffer.size(), bytesRead);
                if(buffer.size() >= sizeLimit){
                    return;
                }
                sleepSafely(delay);
            }
            String receivedData = buffer.toString("UTF-8"); // Учитывайте кодировку устройства
            if (responseRequested && (System.currentTimeMillis() - requestTimestamp) < RESPONSE_TIMEOUT_MS) {
                log.info("Получен и обработан ожидаемый ответ");
                saveReceivedByEvent(receivedData, currentClientId, true, lastCommand);
                responseRequested = false;
                log.info("Завершена обработка ожидаемых данных (ответа)");
            } else {
                log.info("Получены неожиданные данные: " + receivedData);
                saveReceivedByEvent(receivedData, currentClientId, false, null);
                log.info("Завершена обработка неожиданных данных");
            }
        } catch (UnsupportedEncodingException exept) {
            log.error("Ошибка чтения данных: " + exept.getMessage());
        }finally {
            comDataCollectorBusy.set(false);
            log.info(" Завершил получение данных");
            //Thread.currentThread().interrupt();
        }
    }

    public int getClientsCount() {
        return clientsMap.size();
    }

    public int getComPortForJCombo(){
        if(this.comPort == null) return 0;
        ComPort comPorts = new ComPort();
        return  comPorts.getComNumber(this.comPort);
    }

    @Override
    public void run() {
        alive = true;
        Thread.currentThread().setName("Thread Pool Tab " + comPort.getSystemPortName());
        log.info("Запущен поток опроса " + Thread.currentThread().getName() + " для вкладки порта " + comPort.getSystemPortName());
        millisPrev = System.currentTimeMillis() - (poolDelay * 2); // Инициализация таймера
        int limit = 2;
        int counter = 0;
        while ((!Thread.currentThread().isInterrupted()) && alive) {
            if ( ! clientsMap.isEmpty() && shouldPollBecauseTimer()) {
                millisPrev = System.currentTimeMillis();
                pollCommands();
                if(counter < limit) {
                    if(comPort.bytesAvailable() > 0){
                        handleDataAvailableEvent(0);
                        comPort.flushIOBuffers();
                    }
                    log.info("Слушатель будет добавлен еще " + (limit - counter) + " раз");
                    counter++;
                    comPort.addDataListener(serialPortDataListener);
                }
                flopBusyFlag();
            } else {
                sleepSafely(Math.min(poolDelay / 5, 100L));
            }
        }
    }
    private boolean shouldPollBecauseTimer() {
        return System.currentTimeMillis() - millisPrev > poolDelay;
    }

    private void flopBusyFlag(){
        if((System.currentTimeMillis() - requestTimestamp) < RESPONSE_TIMEOUT_MS){
            responseRequested = false;
            comDataCollectorBusy.set(false);
        }
    }
    private void pollCommands() {
        millisPrev = System.currentTimeMillis();
        for (ClientData client : clientsMap.values()) {
            if(client.needPool){
                log.info("Во внутренней очереди устройств отправляю для устройства tabNumber " + client.tabNumber);
                sendOnce(client.prefix, client.command, client.tabNumber, true);
            }
        }
    }

    private void waitForComDataCollectorBusy(int totalLimit) {
        if(totalLimit < 20) totalLimit = 20;
        int i = totalLimit / 20;
        while (comDataCollectorBusy.get() && i < totalLimit) {
            sleepSafely(20);
            i++;
        }
    }
    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            log.warn("Произошла ошибка при попытке сна" + e.getMessage());
        }
    }
    public void sendOnce(String pref, String arg,int tabNumber, boolean internal) {
        log.info(" Начал отправку " + Thread.currentThread().getName() + " для вкладки " + tabNumber);

        if( ! internal){
            if(! containTabDev(tabNumber)){
                log.error(" Попытка отправки с внешнего вызова sendOnce для клиента, которого нет в потоке");
                return;
            }
        }
        currentDirection.set(tabNumber);
        waitForComDataCollectorBusy(40);

        if (comDataCollectorBusy.get()) {
            log.info("Прервал отправку. Слишком долгое ожидание освобождения порта") ;
            return;
        }

        if (arg == null || arg.isEmpty()) {
            log.info("Прервал отправку. Нет текста команды");
            comDataCollectorBusy.set(false);
            return;
        }

        if (pref == null) {
            pref = "";
        }

        if (!comPort.isOpen()) {
            log.warn("Порт закрыт");
            boolean isReopened = false;
            try{
                isReopened = comPort.openPort();
            }catch (Exception e){
                log.warn("Исключение при попытке переоткрытия порта" + e.getMessage());
            }
            if( ! isReopened) {
                comDataCollectorBusy.set(false);
                return;
            }
        }
        responseRequested = true;
        requestTimestamp = System.currentTimeMillis();
        String command = new StringBuilder(pref).append(arg).toString();
        lastCommand = command;
        device.sendData(lastCommand, device.getStrEndian(), comPort, true, 0, device);
        log.info("Команда отправлена: " + pref + arg);
    }

    public void saveReceivedByEvent(String msg, int tabN, boolean responseRequested, String lastCommand) {
        byte [] received = new byte[msg.length()];
        char [] receivedChar = msg.toCharArray();
        for (int i = 0; i < received.length; i++) {
            received [i] = (byte) receivedChar[i];
        }
        if (device == null) {
            log.error("Устройство не инициализировано при попытке saveReceivedByEvent");
            return;
        }
        if(responseRequested) {
            device.setCmdToSend(lastCommand);
        }else{
            device.setCmdToSend(null);
        }

        device.setLastAnswer(received);
        device.parseData();

        int tabDirection = tabN;
        if(device.getTabForAnswer() != null) {
            tabDirection = device.getTabForAnswer();
        }

        DeviceAnswer answer = new DeviceAnswer(LocalDateTime.now(),"",tabDirection);
        answer.setDeviceType(device);
        answer.setAnswerReceivedTime(LocalDateTime.now());
        answer.setAnswerReceivedString(device.getAnswer());
        answer.setAnswerReceivedValues(device.getValues());
        saveAndLogSome(answer, tabN);
    }

    public void setNeedPool (int tabNum, boolean needStatePool){
        if(containTabDev(tabNum)) {
            clientsMap.get(tabNum).needPool = needStatePool;
            log.info("Значение флага пороса для клиента " + tabNum + " изменено на " + needStatePool);
        }else{
            log.warn("Значение флага пороса для клиента " + tabNum + " невозможно изменить клиента нет в этом потоке ");
        }
    }

    public void shutdown(){
        log.info("Поток опроса " + Thread.currentThread().getName() + " будет закрыт");
        clientsMap.clear();
        alive = false;
        if(comPort != null){
            if(comPort.isOpen()){
                comPort.removeDataListener();
                comPort.flushIOBuffers();
                comPort.closePort();
            }
        }
    }


    private void saveAndLogSome(DeviceAnswer answer, int tabNumber){
        if(answer == null){
            log.warn("Попытка сохранить null ответ");
            return;
        }

        if(answer.getTabNumber() < 0){
            log.warn("Попытка сохранить ответ с отрицательным номером вкладки");
            return;
        }
        AnswerStorage.addAnswer(answer);//Answer Storage
        PoolLogger.writeLine(answer); //Sum log
        DeviceLogger deviceLogger;
        if(clientsMap.get(tabNumber).needLog) {
            deviceLogger = clientsMap.get(tabNumber).logger;
            if(deviceLogger == null){
                log.info("Логгер не был инициализирован ранее");
                clientsMap.get(tabNumber).logger = new DeviceLogger(clientsMap.get(tabNumber).tabNumber);
                deviceLogger = clientsMap.get(tabNumber).logger;
            }
            assert myProperties != null;
            if(myProperties.getNeedSyncSavingAnswer()){
                parentService.getAnswerSaverLogger().doLog(answer, tabNumber, PoolLogger.getInstance(), deviceLogger);
            }else{
                if(deviceLogger != null){
                    deviceLogger.writeLine(answer);
                }
            }
        }
    }

    public void setPoolDelay(int poolDelay) {
        this.poolDelay = poolDelay;
        log.info("Новое значение задержки опроса " + poolDelay);
    }
    public void setTextToSendString(String prf, String cmd, int tabNumber){
        if(cmd == null || cmd.isEmpty()){
            log.warn("Пустая команда");
            return;
        }
        if(prf == null ){
            log.info("Пустой префикс");
            prf = "";
        }

        if(containTabDev(tabNumber)){
            clientsMap.get(tabNumber).prefix = prf;
            clientsMap.get(tabNumber).command = cmd;
            log.info("Значения команды и префикса обновлены в потоке опроса для вкладки устройства " + tabNumber);
        }else{
            log.info("Клиент с вкладкой " + tabNumber + " не найден");
        }
    }
    public String getTextToSensByTab(int tabNum){
        if(containTabDev(tabNum)){
            return clientsMap.get(tabNum).prefix + clientsMap.get(tabNum).command;
        }
        return  null;
    }
    public void setNeedLog(boolean bool, int tabNum){
        if(containTabDev(tabNum)){
            clientsMap.get(tabNum).needLog = bool;
        }
        System.out.println("Значение записи в файл изменено на: " + bool + " для подустройства на вкладке " + tabNum);
    }
    public boolean isNeedLog(int tabNum){
        if(containTabDev(tabNum)){
            return clientsMap.get(tabNum).needLog;
        }else{
            log.warn("Подустройство не найдено для вкладки номер при поиске флоага логирования " + tabNum);
        }
        return false;
    }
    public boolean isNeedPool(int tabNum){
        if(containTabDev(tabNum)){
            return clientsMap.get(tabNum).needPool;
        }else{
            log.warn("Подустройство не найдено для вкладки номер при поиске флага опроса " + tabNum);
        }
        return false;
    }
    public boolean containTabDev(int tabNumber){
        return clientsMap.containsKey(tabNumber);
    }
    public void addDeviceToService(int tabNumber, String prf, String cmd, boolean needLog, boolean needPoolFlag) {
        DeviceLogger deviceLogger = null;
        if(needLog){
            deviceLogger = new DeviceLogger(tabNumber);
        }
        clientsMap.put(tabNumber, new ClientData(tabNumber, needLog, needPoolFlag, prf, cmd, deviceLogger));
    }
    public void removeDeviceFromComDataCollector(int tabNumber){ //Когда вкладка закрывается
        log.info("Удаление вкладки из потока " + tabNumber  + Thread.currentThread().getName());
        if(containTabDev(tabNumber))
            clientsMap.remove(tabNumber);
    }
    public boolean isEmpty(){
        return clientsMap.isEmpty();
    }
    public boolean isRootTab(Integer tabNum) {
        for (ClientData client : clientsMap.values()) {
            if (!Objects.equals(client.tabNumber, tabNum) && client.needPool) {
                return true;
            }
        }
        return false;
    }

    private static class ClientData {
        int tabNumber;
        boolean needLog;
        boolean needPool;
        String prefix;
        String command;
        DeviceLogger logger;

        public ClientData(int tabNumber, boolean needLog, boolean needPool, String prefix, String command, DeviceLogger logger) {
            this.tabNumber = tabNumber;
            this.needLog = needLog;
            this.needPool = needPool;
            this.prefix = prefix;
            this.command = command;
            this.logger = logger;
        }
    }
}

