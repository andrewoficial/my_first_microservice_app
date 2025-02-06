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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static org.example.utilites.MyUtilities.createDeviceByProtocol;

public class ComDataCollector implements Runnable{
    private final static Logger log = Logger.getLogger(ComDataCollector.class); // Объект логера
    private final AnyPoolService parentService;//Родительский класс для получения единого для всех потомков объекта синхронизаци логирования.
    private final MyProperties myProperties = MyProperties.getInstance(); //Объект с параметрами для того, что бы определять тип логирования
    @Getter
    private boolean alive = true; // Признак того, что поток жив (после введения Event всегда истина и сбрасывается в shutdown, раньше сбрасывался если нету клиентов у потока)
    @Getter
    private boolean comDataCollectorBusy = false;// Признак того, что текущее соединение по ком-порту занято
    private ProtocolsList protocol = null;//Enum с протоколами, что бы определять что за устройство является клиентом соединения по ком-порту, задается через гуи
    private SomeDevice device = null;//Объект устройства, содержащий особенности обработки команд. Задается на основе выбранного протоколаё
    @Getter
    private SerialPort comPort;//Объект, обслуживающий соединение по ком-порту. Библиотека fazecast.
    @Setter
    private SerialPortDataListener serialPortDataListener;//Объект EventListener, обрабатывающий входящие без запроса сообщения. Библиотека fazecast.
    @Getter
    private long poolDelay = 2000;//Задает переодичность отправки запросов в мс.
    private long millisPrev = System.currentTimeMillis() - (poolDelay * 100);//Обслуживание таймера в миллисекундах (основная переменная poolDelay).

    /**
     *Массив номеров клиентов (вкладок)
     */
    private final ArrayList <Integer> clientsTabNumbers = new ArrayList<>();//
    private final ArrayList <Boolean> needLogArrayList = new ArrayList<>();//Массив флагов логирования
    private final ArrayList <Boolean> needPool = new ArrayList<>(); //Массив флагов, опроса
    private final ArrayList <String> prefixToSendArray = new ArrayList<>();//Массив префиксов для команд [PREF_command]
    private final ArrayList <String> commandsToSendArray = new ArrayList<>();//Массив команд [pref_COMMAND] (в будущем префикс и номер вкладки станет настоящим id устройства)
    private final ArrayList <DeviceLogger> deviceLoggerArrayList = new ArrayList<>();//Массив логеров
    private boolean haveListener = false;

    private volatile boolean responseRequested = false;
    private volatile long requestTimestamp = 0;
    private volatile int requestId = 0;
    private volatile int innerNumber = 0;
    private volatile LocalDateTime requestTime = LocalDateTime.now();
    private final long RESPONSE_TIMEOUT_MS = 5000;  // Таймаут ожидания ответа
    private volatile String lastCommand = "";




    public ComDataCollector(ProtocolsList protocol, String prefix, String command, SerialPort comPort, int poolDelay,boolean needLog,Integer tabNumber,AnyPoolService parentService) {
        super();
        this.parentService = parentService;
        if(protocol == null || command == null || prefix == null || comPort == null || tabNumber  == null){
            log.error("Один из параметров конструктора передан как null");
            return;
        }

        this.protocol = protocol;
        try {
            device = createDeviceByProtocol(protocol, comPort);
        }catch (RuntimeException e){
            log.error("Device in ComDataCollector creating ERROR" + e.getMessage());
        }


        this.comPort = comPort;
        this.poolDelay = poolDelay;
        this.needPool.add(true);
        this.clientsTabNumbers.add(tabNumber);
        commandsToSendArray.add("");
        prefixToSendArray.add("");
        needLogArrayList.add(needLog);


        serialPortDataListener = new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE | SerialPort.LISTENING_EVENT_PORT_DISCONNECTED;
            }
            @Override
            public void serialEvent(SerialPortEvent event) {
                if (event.getEventType() == SerialPort.LISTENING_EVENT_PORT_DISCONNECTED) {
                    log.info("Найдено LISTENING_EVENT_PORT_DISCONNECTED");
                    comPort.closePort();
                } else if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_AVAILABLE) {
                    log.info("Найдено LISTENING_EVENT_DATA_AVAILABLE");
                    StringBuilder builder = new StringBuilder();
                    while (comPort.bytesAvailable() > 0) {
                        byte[] buffer = new byte[comPort.bytesAvailable()];
                        int bytesRead = comPort.readBytes(buffer, buffer.length);
                        for (int i = 0; i < bytesRead; i++) {
                            builder.append((char) buffer[i]);
                        }
                        sleepSafely(80);
                    }
                    String receivedData = builder.toString();

                    if (responseRequested && (System.currentTimeMillis() - requestTimestamp) < RESPONSE_TIMEOUT_MS) {
                        saveReceivedByEvent(receivedData, tabNumber, true, lastCommand);
                        log.info("Получен и обработан ожидаемый ответ");
                        comPort.flushDataListener();
                        responseRequested = false;  // Сбрасываем флаг
                    } else {
                        saveReceivedByEvent(receivedData, tabNumber, false, null);
                        log.info("Получены неожиданные данные: " + receivedData);
                        comPort.flushDataListener();
                    }
                    comDataCollectorBusy = false;
                }else if (event.getEventType() == SerialPort.LISTENING_EVENT_DATA_RECEIVED) {
                    log.info("Найдено LISTENING_EVENT_DATA_RECEIVED");
                    readSerialQueue(tabNumber);
                }
            }
        };
    }

    private boolean checkDataInQueue(){

        return comPort.bytesAvailable() > 0;
    }
    private void readSerialQueue(int clientId){
        StringBuilder builder = new StringBuilder();

        byte[] buffer = new byte[comPort.bytesAvailable()];
        int bytesRead = comPort.readBytes(buffer, buffer.length);
        for (int i = 0; i < bytesRead; i++) {
            builder.append((char) buffer[i]);
        }
        String receivedData = builder.toString();

        if (responseRequested && (System.currentTimeMillis() - requestTimestamp) < RESPONSE_TIMEOUT_MS) {
            saveReceivedByEvent(receivedData, clientId, true, lastCommand);
            log.info("Получен и обработан ожидаемый ответ");
            comPort.flushDataListener();
            responseRequested = false;  // Сбрасываем флаг
        } else {
            saveReceivedByEvent(receivedData, clientId, false, null);
            log.info("Получены неожиданные данные: " + receivedData);
            comPort.flushDataListener();
        }
        comDataCollectorBusy = false;
    }

    public int getClientsCount() {
        if(clientsTabNumbers == null) return 0;
        return clientsTabNumbers.size();
    }

    public int getComPortForJCombo(){
        if(this.comPort == null) return 0;
        ComPort comPorts = new ComPort();
        return  comPorts.getComNumber(this.comPort);
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread Pool Tab " + clientsTabNumbers.get(0));
        millisPrev = System.currentTimeMillis() - (poolDelay * 2); // Инициализация таймера
        int limit = 3;
        int counter = 0;
        poolDelay = 1500;
        while ((!Thread.currentThread().isInterrupted()) && alive) {
            if (shouldPoll()) {
                millisPrev = System.currentTimeMillis();
                //log.info("Поток опроса запущен");
                pollCommands();
                if(counter < limit) {
                    if(checkDataInQueue()){
                        readSerialQueue(0);
                    }
                    log.info("Слушатель будет добавлен еще " + (limit - counter) + " раз");
                    counter++;
                    comPort.addDataListener(serialPortDataListener);
                }

            } else {
                sleepSafely(Math.min(poolDelay / 5, 100L));
                //sleepSafely(3000);
            }
        }
    }
    private boolean shouldPoll() {
        return System.currentTimeMillis() - millisPrev > poolDelay;
    }
    private void pollCommands() {
        millisPrev = System.currentTimeMillis();
        for (int i = 0; i < commandsToSendArray.size(); i++) {
            if (shouldSkipCommand(i)) {
                continue;
            }
            waitForComDataCollectorBusy(500);
            if (!comDataCollectorBusy) {
                log.info("Во внутренней очереди устройств отправляю для устройства " + i);
                sendOnce(prefixToSendArray.get(i), commandsToSendArray.get(i), i, true);
            }
        }
    }
    private boolean shouldSkipCommand(int i) {
        return i < 0 || needPool.size() < i || (! needPool.get(i));
    }
    private void waitForComDataCollectorBusy(int totalLimit) {
        if(totalLimit < 20) totalLimit = 20;
        int i = totalLimit / 20;
        while (comDataCollectorBusy && i < totalLimit) {
            sleepSafely(20);
            i++;
        }
    }
    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    public void sendOnce(String pref, String arg, int i, boolean internal) {
        if (!internal) {
            i = findSubDevByTabNumber(i);
        }
        waitForComDataCollectorBusy(40);

        if (comDataCollectorBusy) {
            log.info("Прервал отправку. Слишком долгое ожидание освобождения порта");
            return;
        } else {
            log.info("Завершил ожидание освобождения устройства и занял его.");
            comDataCollectorBusy = true;
        }

        if (arg == null || arg.isEmpty()) {
            log.info("Прервал отправку. Нет текста команды");
            comDataCollectorBusy = false;
            return;
        }

        if (pref == null) {
            pref = "";
        }

        if (!comPort.isOpen()) {
            log.info("Прервал отправку. Порт закрыт");
            comDataCollectorBusy = false;
            return;
        }

        responseRequested = true;
        requestTimestamp = System.currentTimeMillis();
        requestId = getTabNumberByInnerNumber(i);
        requestTime = LocalDateTime.now();
        innerNumber = i;
        lastCommand = pref + arg;

        device.sendData(pref + arg, device.getStrEndian(), comPort, true, 0, device);
        log.info("Команда отправлена: " + pref + arg);
    }
    public void saveReceivedByEvent(String msg, int tabN, boolean responseRequested, String lastCommand) {
        tabN =  findSubDevByTabNumber(tabN);
        byte [] received = new byte[msg.length()];
        char [] receivedChar = msg.toCharArray();
        for (int i = 0; i < received.length; i++) {
            received [i] = (byte) receivedChar[i];
        }
        if(responseRequested) {
            device.setCmdToSend(lastCommand);
        }else{
            device.setCmdToSend(null);
        }

        device.setLastAnswer(received);
        device.parseData();

        int tabDirection = getTabNumberByInnerNumber(tabN);
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
        addMissingObjects(getInnerNumberByTabNumber(tabNum));
        needPool.set(getInnerNumberByTabNumber(tabNum), needStatePool);
        log.info("Значение флага пороса для клиента " + tabNum + " изменено на " + needStatePool + " (Внутреннрий номер " + getInnerNumberByTabNumber(tabNum) + ")");
    }

    public void shutdown(){
        log.info("Поток опроса " + Thread.currentThread().getName() + " будет закрыт");
        needPool.clear();
        alive = false;
        if(comPort != null){
            if(comPort.isOpen()){
                comPort.removeDataListener();
                log.info("Выполнено removeDataListener перед закрытием потока" + Thread.currentThread().getName());
                haveListener = false;
                comPort.flushDataListener();
                comPort.flushIOBuffers();
                comPort.closePort();
            }
        }
        Thread.currentThread().interrupt();
    }

    private int getTabNumberByInnerNumber(int innerNumber){
        for (int i = 0; i < clientsTabNumbers.size(); i++) {
            if(i == innerNumber){
                return clientsTabNumbers.get(i);
            }
        }
        return -1;
    }

    private int getInnerNumberByTabNumber(int tabNumber){
        for (int i = 0; i < clientsTabNumbers.size(); i++) {
            if(clientsTabNumbers.get(i).equals(tabNumber)){
                return i;
            }
        }
        return -1;
    }
    private void saveAndLogSome(DeviceAnswer answer, int subDevNum){
        addMissingObjects(subDevNum);
        AnswerStorage.addAnswer(answer);//Answer Storage
        PoolLogger.writeLine(answer); //Sum log
        DeviceLogger deviceLogger= null;
        if(needLogArrayList.get(subDevNum)) {
            if(deviceLoggerArrayList.size() > subDevNum){ //Безопасно проверяем наличие логера в массиве
                deviceLogger = deviceLoggerArrayList.get(subDevNum);
            }
            if(deviceLogger == null){
                if(! (clientsTabNumbers.size() > subDevNum)) {
                    log.error("Попытка сохранить лог в несуществующий индекс клиента (вкладки) " + subDevNum);
                    return;
                }
                if(deviceLoggerArrayList.size() <= subDevNum) {
                    log.warn("Попытка сохранить лог при несуществующем индексе клиента (вкладки) в массиве логеров" + subDevNum);
                    return;
                }

                if(deviceLoggerArrayList.get(subDevNum) != null){
                    log.info("Логгер не был инициализирован ранее");
                }
                deviceLoggerArrayList.set(subDevNum, new DeviceLogger(clientsTabNumbers.get(subDevNum).toString()));
                deviceLogger = deviceLoggerArrayList.get(subDevNum);
            }

            if(myProperties.getNeedSyncSavingAnswer()){
                parentService.getAnswerSaverLogger().doLog(answer, getTabNumberByInnerNumber(subDevNum), PoolLogger.getInstance(), deviceLogger);
            }else{
                if(deviceLogger != null){
                    deviceLogger.writeLine(answer);
                }
            }
        }
    }

    private void addMissingObjects(int subDevNum){
        ensureSize(deviceLoggerArrayList, subDevNum, null);
        ensureSize(needLogArrayList, subDevNum, false);
        ensureSize(commandsToSendArray, subDevNum, null);
        ensureSize(prefixToSendArray, subDevNum, null);
        log.info("Завершено добавление недостающих объектов для клиента в потоке опроса");
    }
    private <T> void ensureSize(List<T> list, int requiredSize, T defaultValue) {
        while (list.size() <= requiredSize) {
            list.add(defaultValue);
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
        int innerNumber = findSubDevByTabNumber(tabNumber);
        if(innerNumber != -1){
            addMissingObjects(innerNumber);
            prefixToSendArray.set(innerNumber, prf);
            commandsToSendArray.set(innerNumber, cmd);
            log.info("Значения команды и префикса обновлены для подустройства номер " + innerNumber + " в потоке опроса для вкладки устройства " + tabNumber);
            log.info("Теперь команда " + cmd + " и префикс " + prf);
        }else{
            log.info("Клиент с вкладкой " + tabNumber + " не найден");
        }
    }
    public String getTextToSensByTab(int tabNum){
        int innerNumber = findSubDevByTabNumber(tabNum);
        if(innerNumber != -1){
            return prefixToSendArray.get(innerNumber) + commandsToSendArray.get(innerNumber);
        }
        return  null;
    }
    public void setNeedLog(boolean bool, int tabNum){
        int innerNumber = findSubDevByTabNumber(tabNum);
        if(innerNumber != -1){
            addMissingObjects(innerNumber);
            needLogArrayList.set(innerNumber, bool);
        }
        System.out.println("Значение записи в файл изменено на: " + bool + " для подустройства на вкладке " + tabNum + " в потоке опроса это устройство номер " + findSubDevByTabNumber(tabNum));
    }
    public boolean isNeedLog(int tabNum){
        int innerNumber = findSubDevByTabNumber(tabNum);//Поиск внутреннего для потока номера клиента по его id (номеру вкладки)
        if(innerNumber != -1){//Такой клиент содержится в этом потоке?
            addMissingObjects(innerNumber);
            return needLogArrayList.get(innerNumber);
        }
        log.warn("Подустройство не найдено для вкладки номер при поиске флоага логирования " + tabNum);
        return false;
    }
    public boolean isNeedPool(int tabNum){
        int innerNumber = findSubDevByTabNumber(tabNum);
        if(innerNumber != -1){//Такой клиент содержится в этом потоке
            addMissingObjects(innerNumber);
            return needPool.get(innerNumber);
        }
        log.warn("Подустройство не найдено для вкладки номер при поиске флага опроса " + tabNum);
        return false;
    }
    private int findSubDevByTabNumber(int tabNumber){
        return clientsTabNumbers.indexOf(tabNumber);
    }
    public boolean containTabDev(int tabNumber){
        return clientsTabNumbers.contains(tabNumber);
    }
    public void addDeviceToService(int tabNumber, String prf, String cmd, boolean needLog, boolean needPoolFlag) {
        clientsTabNumbers.add(tabNumber);
        prefixToSendArray.add(prf);
        commandsToSendArray.add(cmd);
        needLogArrayList.add(needLog);
        if(needLog){
            deviceLoggerArrayList.add(new DeviceLogger(String.valueOf(tabNumber)));
        }else{
            deviceLoggerArrayList.add(null);
        }
        needPool.add(needPoolFlag);
    }
    public void removeDeviceFromComDataCollector(int tabNumber){ //Когда вкладка закрывается
        int innerNumber = findSubDevByTabNumber(tabNumber);
        log.info("Удаление вкладки из потока " + tabNumber  + Thread.currentThread().getName());
        for (int i = 0; i < clientsTabNumbers.size(); i++) {
            if (clientsTabNumbers.get(i) == tabNumber) {
                clientsTabNumbers.remove(i);
                i--; // Уменьшаем индекс, чтобы учесть сдвиг
            }
        }
        if(innerNumber > 0){
            commandsToSendArray.remove(innerNumber);
            prefixToSendArray.remove(innerNumber);
            needLogArrayList.remove(innerNumber);
            deviceLoggerArrayList.remove(innerNumber);
        }
    }
    public boolean isEmpty(){
        return clientsTabNumbers.isEmpty();
    }
    public boolean isRootTab(Integer tabNum) {
        for (Integer clientNumber : clientsTabNumbers) {
            if (!Objects.equals(clientNumber, tabNum) && needPool.get(getInnerNumberByTabNumber(clientNumber))) {
                return true;
            }
        }
        return false;
    }
}
