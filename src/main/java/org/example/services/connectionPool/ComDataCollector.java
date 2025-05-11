package org.example.services.connectionPool;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;
import org.example.services.DeviceAnswer;
import org.example.services.loggers.DeviceLogger;
import org.example.services.loggers.PoolLogger;
import org.example.device.ProtocolsList;
import org.example.device.*;
import org.example.utilites.properties.MyProperties;

import java.io.ByteArrayOutputStream;
import java.net.ConnectException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
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
    private volatile DeviceAnswer deviceAnswer;


    private volatile boolean responseRequested = false;
    private volatile long requestTimestamp = 0;
    private final long RESPONSE_TIMEOUT_MS = 3000;  // Таймаут ожидания ответа
    private MainLeftPanelStateCollection collection;
    private int clientId;

    public ComDataCollector(MainLeftPanelStateCollection state, ProtocolsList protocol, String prefix, String command, SerialPort comPort, int poolDelay, boolean needLog, Integer clientId, AnyPoolService parentService) throws ConnectException {
        super();
        this.parentService = parentService;
        if(protocol == null || command == null || prefix == null || comPort == null || clientId  == null || state == null){
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
        this.clientId = clientId;
        log.warn("getBaudRateValue " + state.getBaudRateValue(clientId) + "getDataBitsValue " +  state.getDataBitsValue(clientId)+ "getStopBitsValue " + state.getStopBitsValue(clientId)+ "getParityBitsValue " + state.getParityBitsValue(clientId));
        comPort.setComPortParameters(state.getBaudRateValue(clientId), state.getDataBitsValue(clientId), state.getStopBitsValue(clientId), state.getParityBitsValue(clientId));
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, device.getMillisReadLimit(), device.getMillisWriteLimit());
        comPort.openPort(device.getMillisReadLimit());
        if ( ! comPort.isOpen()) {
            throw new ConnectException("Порт: " + comPort.getSystemPortName() + ". Код ошибки: " + comPort.getLastErrorCode() + ". Системное положение порта:" + comPort.getPortLocation() + ". VendorID:" + comPort.getVendorID());
        }
        this.clientId = clientId;
        this.collection = state;
        this.poolDelay = poolDelay;
        clientsMap.put(clientId, new ClientData(clientId, needLog, true, "", "", null));

        serialPortDataListener = new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }

            final Map<Integer, String> eventTypes = new HashMap<>();
            {
                eventTypes.put(SerialPort.LISTENING_EVENT_PORT_DISCONNECTED, "LISTENING_EVENT_PORT_DISCONNECTED");
                eventTypes.put(SerialPort.LISTENING_EVENT_BREAK_INTERRUPT, "LISTENING_EVENT_BREAK_INTERRUPT");
                eventTypes.put(SerialPort.LISTENING_EVENT_CARRIER_DETECT, "LISTENING_EVENT_CARRIER_DETECT");
                eventTypes.put(SerialPort.LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR, "LISTENING_EVENT_FIRMWARE_OVERRUN_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_FRAMING_ERROR, "LISTENING_EVENT_FRAMING_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_PARITY_ERROR, "LISTENING_EVENT_PARITY_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR, "LISTENING_EVENT_SOFTWARE_OVERRUN_ERROR");
                eventTypes.put(SerialPort.LISTENING_EVENT_TIMED_OUT, "LISTENING_EVENT_TIMED_OUT");
                eventTypes.put(SerialPort.LISTENING_EVENT_RING_INDICATOR, "LISTENING_EVENT_RING_INDICATOR");
                eventTypes.put(SerialPort.LISTENING_EVENT_DATA_AVAILABLE, "LISTENING_EVENT_DATA_AVAILABLE");
                eventTypes.put(SerialPort.LISTENING_EVENT_DATA_RECEIVED, "LISTENING_EVENT_DATA_RECEIVED");
            }
            @Override
            public void serialEvent(SerialPortEvent event) {
                String eventName = eventTypes.getOrDefault(event.getEventType(), "UNKNOWN_EVENT");
                log.warn("Найдено событие для слушателя порта " + eventName + " " + comPort.getSystemPortName());
                if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    comPort.closePort();
                } else if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE || event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                    handleDataAvailableEvent();
                }
            }
        };
    }

    private void handleDataAvailableEvent() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[2048];
        long delay = device == null ? 150L : device.getMillisReadLimit();
        //int sizeLimit = device == null ? 150 : device.getExpectedBytes();
        int sizeLimit = 2048;//Увеличил тут размер

        try {
            int dataAvailable = comPort.bytesAvailable();

            while (dataAvailable > 0) {
                int bytesRead = comPort.readBytes(chunk, Math.min(chunk.length, dataAvailable));
                //buffer.write(chunk, buffer.size(), bytesRead);
                buffer.write(chunk, 0, bytesRead);
                if(buffer.size() >= sizeLimit){//Вызывало падение
                    return;
                }
                sleepSafely(delay);
                dataAvailable = comPort.bytesAvailable();
            }

            //String receivedData = buffer.toString("UTF-8"); // Учитывайте кодировку устройства
            String receivedData = buffer.toString(); // Учитывайте кодировку устройства
            if (responseRequested && (System.currentTimeMillis() - requestTimestamp) < RESPONSE_TIMEOUT_MS) {
                log.info("Получен ожидаемый ответ");
                saveReceivedByEvent(receivedData, true);
                responseRequested = false;
                log.info("Завершена обработка ожидаемых данных (ответа)");
            } else {
                log.info("Получены неожиданные данные: " + receivedData);
                saveReceivedByEvent(receivedData, false);
                log.info("Завершена обработка неожиданных данных. responseRequested " + responseRequested + " requestTimestamp " + requestTimestamp);
            }
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
                        handleDataAvailableEvent();
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
        if(((System.currentTimeMillis() - requestTimestamp) > RESPONSE_TIMEOUT_MS) || ((System.currentTimeMillis() - requestTimestamp) < 0)){
            responseRequested = false;
            comDataCollectorBusy.set(false);
            comPort.addDataListener(serialPortDataListener);
        }
    }
    private void pollCommands() {
        millisPrev = System.currentTimeMillis();
        for (ClientData client : clientsMap.values()) {
            if(client.needPool){
                log.info("Во внутренней очереди устройств отправляю для устройства clientId " + client.clientId + " командой " + client.command);

                sendOnce(client.prefix, client.command, client.clientId, true);
                comPort.addDataListener(serialPortDataListener);
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
    public void sendOnce(String pref, String cmd,int clientId, boolean internal) {
        log.info(" Начал отправку " + Thread.currentThread().getName() + " для клиента " + clientId);

        if( ! internal){
            if(! containClientId(clientId)){
                log.error(" Попытка отправки с внешнего вызова sendOnce для клиента, которого нет в потоке");
                return;
            }
        }
        currentDirection.set(clientId);

        if(internal){
            waitForComDataCollectorBusy(40);
        }else {
            waitForComDataCollectorBusy(60);
        }


        if (comDataCollectorBusy.get()) {
            log.info("Прервал отправку. Слишком долгое ожидание освобождения порта") ;
            return;
        }

        if (cmd == null || cmd.isEmpty()) {
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

        String textToSend = pref+cmd;
        device.setCmdToSend(pref+cmd);
        byte[] buffer = new byte[textToSend.length() + device.getStrEndian().length];
        for (int i = 0; i < textToSend.length(); i++) {
            buffer[i] = (byte) textToSend.charAt(i);
        }
        System.arraycopy(device.getStrEndian(), 0, buffer, textToSend.length() , device.getStrEndian().length);
        comPort.writeBytes(buffer, buffer.length);



        log.info("Команда отправлена: " + collection.getPrefix(clientId)+collection.getCommand(clientId));
        deviceAnswer = new DeviceAnswer(LocalDateTime.now(),collection.getCommand(clientId),clientId);
        log.info("Заготовка ответа создана ");
    }

    public void saveReceivedByEvent(String msg, boolean responseRequested) {
        if(msg == null || msg.isEmpty()){
            log.warn("Пустое сообщение при попытке saveReceivedByEvent");
            return;
        }
        byte [] received = new byte[msg.length()];
        char [] receivedChar = msg.toCharArray();
        for (int i = 0; i < received.length; i++) {
            received [i] = (byte) receivedChar[i];
        }
        log.info("Конвертация в массив завершена");
        if (device == null) {
            log.error("Устройство не инициализировано при попытке saveReceivedByEvent");
            return;
        }
        if(deviceAnswer == null){
            deviceAnswer = new DeviceAnswer(LocalDateTime.now(), "", currentDirection.get());
        }

        if(responseRequested) {
            try{
                device.setCmdToSend(collection.getCommand(clientId));
            }catch (IndexOutOfBoundsException exception){
                log.warn("Исключение " + exception.getMessage());
                device.setCmdToSend(null);
            }
        }else{
            device.setCmdToSend(null);
            deviceAnswer.setRequestSendTime(LocalDateTime.now());
        }


        int tabDirection = clientId;
        device.setLastAnswer(received);
        deviceAnswer.changeTabNum(tabDirection);
        deviceAnswer.setDeviceType(device);
        deviceAnswer.setAnswerReceivedTime(LocalDateTime.now());
        String answer = null;
        try{
            device.parseData();
            answer = device.getAnswer();
        }catch (Exception e){
            log.warn("Исключение во время разбора ответа внутри класса прибора" + e.getMessage());
            answer = "Исключение во время разбора ответа внутри класса прибора";
        }






        if(answer != null){
            deviceAnswer.setAnswerReceivedString(answer);
        }else{
            deviceAnswer.setAnswerReceivedString("Ошибка при разборе ответа");
        }

        if(device.getValues() != null){
            if(device.getValues().getDirection() > 0)
                tabDirection = device.getValues().getDirection();

            deviceAnswer.setAnswerReceivedValues(device.getValues());
        }else{
            deviceAnswer.setAnswerReceivedValues(new AnswerValues(0));
        }

        saveAndLogSome(deviceAnswer, tabDirection);
    }

    public void setNeedPool (int clientId, boolean needStatePool){
        if(containClientId(clientId)) {
            clientsMap.get(clientId).needPool = needStatePool;
            log.info("Значение флага пороса для клиента " + clientId + " изменено на " + needStatePool);
        }else{
            log.warn("Значение флага пороса для клиента " + clientId + " невозможно изменить клиента нет в этом потоке ");
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


    private void saveAndLogSome(DeviceAnswer answer, int clientId){
        if(answer == null){
            log.warn("Попытка сохранить null ответ");
            return;
        }

        if(answer.getClientId() < 0){
            log.warn("Попытка сохранить ответ с отрицательным номером вкладки");
            return;
        }

        AnswerStorage.addAnswer(answer);//Answer Storage
        PoolLogger.getInstance().writeLine(answer); //Sum log

        DeviceLogger deviceLogger;
        if(clientsMap.get(clientId).needLog) {
            deviceLogger = clientsMap.get(clientId).logger;
            if(deviceLogger == null){
                log.info("Логгер не был инициализирован ранее");
                clientsMap.get(clientId).logger = new DeviceLogger(clientsMap.get(clientId).clientId);
                deviceLogger = clientsMap.get(clientId).logger;
            }
            assert myProperties != null;
            if(myProperties.getNeedSyncSavingAnswer()){
                parentService.getAnswerSaverLogger().doLog(answer, clientId, PoolLogger.getInstance(), deviceLogger);
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
    public void setTextToSendString(String prf, String cmd, int clientId){
        if(cmd == null || cmd.isEmpty()){
            log.warn("Пустая команда");
            return;
        }
        if(prf == null ){
            log.info("Пустой префикс");
            prf = "";
        }

        if(containClientId(clientId)){
            clientsMap.get(clientId).prefix = prf;
            clientsMap.get(clientId).command = cmd;
            log.info("Значения команды и префикса обновлены в потоке опроса для вкладки устройства " + clientId);
        }else{
            log.info("Клиент с вкладкой " + clientId + " не найден");
        }
    }
    public String getTextToSensByTab(int clientId){
        if(containClientId(clientId)){
            return clientsMap.get(clientId).prefix + clientsMap.get(clientId).command;
        }
        return  null;
    }
    public void setNeedLog(boolean bool, int clientId){
        if(containClientId(clientId)){
            clientsMap.get(clientId).needLog = bool;
        }
        System.out.println("Значение записи в файл изменено на: " + bool + " для клиента " + clientId);
    }
    public boolean isNeedLog(int clientId){
        if(containClientId(clientId)){
            return clientsMap.get(clientId).needLog;
        }else{
            log.warn("Ошибка в поиске флага логирования для клиента " + clientId);
        }
        return false;
    }
    public boolean isNeedPool(int clientId){
        if(containClientId(clientId)){
            return clientsMap.get(clientId).needPool;
        }else{
            log.warn("Ошибка в поиске флага опроса для клиента " + clientId);
        }
        return false;
    }
    public boolean containClientId(int clientId){
        return clientsMap.containsKey(clientId);
    }
    public void addDeviceToService(int clientId, String prf, String cmd, boolean needLog, boolean needPoolFlag) {
        log.info("Добавление клиента в поток " + clientId  + Thread.currentThread().getName());
        DeviceLogger deviceLogger = null;
        if(needLog){
            deviceLogger = new DeviceLogger(clientId);
        }
        clientsMap.put(clientId, new ClientData(clientId, needLog, needPoolFlag, prf, cmd, deviceLogger));
    }
    public void removeDeviceFromComDataCollector(int clientId){ //Когда вкладка закрывается
        log.info("Удаление клиента из потока " + clientId  + Thread.currentThread().getName());
        if(containClientId(clientId))
            clientsMap.remove(clientId);
    }
    public boolean isEmpty(){
        return clientsMap.isEmpty();
    }

    public String closePort(int clientId) throws ConnectException {
        if(this.comPort == null){
            throw new ConnectException("Нет объекта порта в потоке опроса");
        }
        String forReturn;
        if(isRootThread(clientId)){
            if (this.comPort.isOpen()) {
                comPort.flushDataListener();
                comPort.removeDataListener();
                comPort.flushIOBuffers();
                comPort.closePort();
                forReturn = "Порт " + comPort.getSystemPortName() + " закрыт ";
            }else{
                forReturn = "Порт " + comPort.getSystemPortName() + " уже был закрыт ранее ";
            }
        }else{
            throw new ConnectException("Порт " + comPort.getSystemPortName() + " занят другими вкладками. В управленеии отказано");
        }
        return forReturn;
    }
    public String reopenPort(int clientId, MainLeftPanelStateCollection stateCollection){
        if(this.comPort == null){
            return "Нет объекта порта в потоке опроса";
        }
        String forReturn;
        if(isRootThread(clientId)){
            if (this.comPort.isOpen()) {
                comPort.flushDataListener();
                comPort.removeDataListener();
                comPort.flushIOBuffers();
                this.comPort.closePort();
                comPort.setComPortParameters(stateCollection.getBaudRateValue(clientId), stateCollection.getDataBits(clientId), stateCollection.getStopBits(clientId), stateCollection.getParityBitsValue(clientId), false);
                comPort.openPort();
                forReturn = "Порт " + comPort.getSystemPortName() + " переоткрыт ";
            } else {
                comPort.setComPortParameters(stateCollection.getBaudRateValue(clientId), stateCollection.getDataBits(clientId), stateCollection.getStopBits(clientId), stateCollection.getParityBitsValue(clientId), false);
                comPort.openPort();
                forReturn = "Порт " + comPort.getSystemPortName() + " открыт ";
            }
            if (comPort.isOpen()) {
                forReturn += "успешно";
            } else {
                forReturn += "с ошибкой " + comPort.getSystemPortName() + "! Код ошибки: " + comPort.getLastErrorCode();
            }
            return forReturn;
        }else{
            return "Порт " + comPort.getSystemPortName() + " занят другими вкладками. В управленеии отказано";
        }
    }
    public boolean isRootThread(Integer clientId) {
        for (ClientData client : clientsMap.values()) {
            if (!Objects.equals(client.clientId, clientId) && client.needPool) { //Если найден другой клиент с активным опросом
                log.warn("Найден " + client.clientId + " и " + clientId + " при этом у найденого " + client.needLog);
                return false;
            }
        }
        return true;
    }

    private static class ClientData {
        int clientId;
        boolean needLog;
        boolean needPool;
        String prefix;
        String command;
        DeviceLogger logger;

        public ClientData(int clientId, boolean needLog, boolean needPool, String prefix, String command, DeviceLogger logger) {
            this.clientId = clientId;
            this.needLog = needLog;
            this.needPool = needPool;
            this.prefix = prefix;
            this.command = command;
            this.logger = logger;
        }
    }
}

