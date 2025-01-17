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


import static org.example.utilites.MyUtilities.bytesToHex;
import static org.example.utilites.MyUtilities.createDeviceByProtocol;


public class ComDataCollector implements Runnable{
    private final static Logger log = Logger.getLogger(ComDataCollector.class); // Объект логера
    private final AnyPoolService parentService;//Родительский класс для получения единого для всех потомков объекта синхронизаци логирования.
    private final MyProperties myProperties = MyProperties.getInstance(); //Объект с параметрами для того, что бы определять тип логирования
    @Getter
    private boolean alive = true; // Признак того, что поток жив (после введения Event всегда истина и сбрасывается в shutdown, раньше сбрасывался если нету клиентов у потока)
    @Getter
    private volatile boolean comDataCollectorBusy = false;// Признак того, что текущее соединение по ком-порту занято
    @Setter
    private boolean threadForEvent; //Признак того, что при открытии соединения по ком-порту будет подключен SerialPortDataListener.
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






    public ComDataCollector(ProtocolsList protocol, String prefix, String command, SerialPort comPort, int poolDelay,boolean needLog,boolean threadForEvent,Integer tabNumber,AnyPoolService parentService) {
        super();
        this.parentService = parentService;
        if(protocol == null){
            log.error("Передан параметр protocol null");
            return;
        }

        if(command == null){
            log.error("Передан параметр command null");
            return;
        }

        if(prefix == null){
            log.error("Передан параметр prefix null");
            return;
        }

        if(comPort == null){
            log.error("Передан параметр comPort null");
            return;
        }


        if(tabNumber == null){
            log.error("Передан параметр tabNumber null");
            return;
        }
        this.protocol = protocol;

            try {
                device = createDeviceByProtocol(protocol, comPort);
                log.info("Device in ComDataCollector created " + protocol.getValue());
            }catch (RuntimeException e){
                log.error("Device in ComDataCollector creating ERROR");
                System.out.println(e.getMessage());
            }
            if(device == null){
                log.warn("Device in ComDataCollector still null");
            }



        this.clientsTabNumbers.add(tabNumber);
        this.comPort = comPort;
        this.poolDelay = poolDelay;
        this.threadForEvent = threadForEvent;
        needPool.add(true);
        commandsToSendArray.add("");
        prefixToSendArray.add("");
        needLogArrayList.add(needLog);

        serialPortDataListener = new SerialPortDataListener() {
            @Override
            public int getListeningEvents() {
                return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
            }

            @Override
            public void serialEvent(SerialPortEvent event) {
                StringBuilder builder = new StringBuilder();
                while (comPort.bytesAvailable() > 0) {
                    int size = comPort.bytesAvailable();
                    byte[] buffer = new byte[size];
                    int bytesRead = comPort.readBytes(buffer, size);
                    for (int i = 0; i < bytesRead; i++) {
                        builder.append((char) buffer[i]);
                    }

                    try {
                        Thread.sleep(80);
                    } catch (InterruptedException ex) {
                        log.error("Ошибка при обработке данных слушателя: ", ex);
                    }
                }

                //log.info("Parse external Event ASCII [" + builder.toString().trim() + "]");
                //log.info("Принято по событию (без запроса) [" + bytesToHex(builder.toString().getBytes()) + "]");
                saveReceivedByEvent(builder.toString(), tabNumber);
            }
        };


    }

    public void setupComConnection(SerialPort comPort){
        this.comPort = comPort;
    }

    public int getProtocolForJCombo(){
        return ProtocolsList.getNumber(this.protocol);
    }

    public int getClientsCount() {
        if(clientsTabNumbers == null) return 0;
        return clientsTabNumbers.size();
    }

    public int getComPortForJCombo(){
        if(this.comPort == null) return 0;

        ComPort comPorts = new ComPort();
        ArrayList <SerialPort> ports = comPorts.getAllPorts();
        for (int i = 0; i < ports.size(); i++) {
            SerialPort port = ports.get(i);
            if (port != null && portsAreEqual(port, comPort)) {
                return i;
            }
        }
        log.error("Текущий ком-порт не найден в списке доступных");
        return 0;
    }
    private boolean portsAreEqual(SerialPort port1, SerialPort port2) {
        return port1.getSystemPortName().equalsIgnoreCase(port2.getSystemPortName());
    }
    @Override
    public void run() {
        Thread.currentThread().setName("Thread Pool Tab " + clientsTabNumbers.get(0));
        millisPrev = System.currentTimeMillis() - (poolDelay * 2); // Инициализация таймера

        while ((!Thread.currentThread().isInterrupted()) && alive) {
            handleEventListener();
            if (shouldPoll()) {
                pollCommands();
            } else {
                sleepSafely(Math.min(poolDelay / 5, 100L));
            }
        }
    }

    /**
     * Определение нужно ли добавлять слушатиель
     */
    private void handleEventListener() {
        if (threadForEvent && !comDataCollectorBusy) {
            waitForComDataCollectorBusy(300);
            if (!comDataCollectorBusy) {
                comPort.addDataListener(serialPortDataListener);
            }
        }
    }

    /**
     * Проверяет, нужно ли выполнять следующий опрос
     * @return true - если время ожидания следующего опроса истекло
     */
    private boolean shouldPoll() {
        return System.currentTimeMillis() - millisPrev > poolDelay;
    }



    /**
     * Начинает перебор внутренней очереди клиентов.
     * Ожидая завершения отправки предыдущей команды
     */
    private void pollCommands() {
        millisPrev = System.currentTimeMillis();
        for (int i = 0; i < commandsToSendArray.size(); i++) {
            if (shouldSkipCommand(i)) {
                continue;
            }
            waitForComDataCollectorBusy(500);
            if (!comDataCollectorBusy) {
                sendOnce(prefixToSendArray.get(i), commandsToSendArray.get(i), i, true);
            }
        }
    }

    /**
     * Проверяет нужно ли проводить отправку команды
     * @param i - Внутренний номер клиента в потоке
     * @return true - если в листе needPool содержиться маркер true
     */
    private boolean shouldSkipCommand(int i) {
//        //Debug
//        if(i < 0)   log.info("i < 0");
//        if(needPool.size() <= i)    log.info("needPool.size() <= i");
//        if(! needPool.get(i))   log.info("! needPool.get(i)");

        return i < 0 || needPool.size() < i || (! needPool.get(i));
    }

    /**
     * Ожидает завершения отправки предыдущей команды
     *
     * @param totalLimit - Максимальное количесвто циклов ожидания по 20мс. Лучше задавать кратное 20.
     */
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


    public void sendOnce (String pref, String arg, int i, boolean internal){
        if(!internal){
            i = findSubDevByTabNumber(i);
        }
        //Проверка, если команда уже выполняется (занят ком-порт)
        waitForComDataCollectorBusy(40);

        if (comDataCollectorBusy) {
            log.info("Прервал отправку. Слишком долгое ожидание освобождения порта");
            return;
        } else {
            synchronized (this) {
                comDataCollectorBusy = true;
            }
        }

        if(arg == null || arg.isEmpty()){
            log.info("Прервал отправку. Нет текста команды");
            synchronized (this) {
                comDataCollectorBusy = false;
            }
            return;
        }
        if(pref == null){
            pref = "";
        }
        //comPort.flushDataListener();
        comPort.removeDataListener();
        //comPort.flushIOBuffers();
        int tabDirection = getTabNumberByInnerNumber(i);
        //log.info("Параметр i = " + i + " tabDirection будет задан " + tabDirection);

        LocalDateTime startSend = LocalDateTime.now();
        //log.info("Для отправки textToSend = " + arg + " prefToSend = " + pref);

        try {
            device.sendAndReceiveData(pref + arg, device);
        } catch (Exception e) {
            log.info("Во время отправки произошло исключение" + e.getMessage());
            //throw new RuntimeException(e);
        }finally {
            synchronized (this) {
                comDataCollectorBusy = false;
            }
            comPort.addDataListener(serialPortDataListener);
            device.parseData();
        }



        DeviceAnswer answer = new DeviceAnswer(startSend,arg,tabDirection);
        answer.setDeviceType(device);
        answer.setAnswerReceivedTime(LocalDateTime.now());
        answer.setAnswerReceivedString(device.getAnswer());
        answer.setAnswerReceivedValues(device.getValues());

        saveAndLogSome(answer, i);

    }

    public void saveReceivedByEvent(String msg, int tabN){
        tabN =  findSubDevByTabNumber(tabN);



        byte [] received = new byte[msg.length()];
        char [] receivedChar = msg.toCharArray();
        for (int i = 0; i < received.length; i++) {
            received [i] = (byte) receivedChar[i];
        }
        device.setCmdToSend(null);
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
        threadForEvent = false;
        needPool.clear();
        alive = false;
        if(comPort != null){
            if(comPort.isOpen()){
                comPort.removeDataListener();
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

    //Получает номер вкладки на которой надо обновить команду, сопаставляет со списком
    //однотипных устройств и обновляет
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
        log.info("Поток содержит клиентов до удаления" + clientsTabNumbers.size());
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
        log.info("Поток содержит клиентов после удаления" + clientsTabNumbers.size());
        for (Integer clientsTabNumber : clientsTabNumbers) {
            log.info(clientsTabNumber);
        }
        log.info(clientsTabNumbers.toString());
    }

    public boolean isEmpty(){
        return clientsTabNumbers.isEmpty();
    }


    public boolean isRootTab(Integer tabNum) {
        // Перебираем массив номеров вкладок
        for (Integer clientNumber : clientsTabNumbers) {
            // Если номер не совпадает и вкладка требует опроса, возвращаем true
            if (!Objects.equals(clientNumber, tabNum) && needPool.get(getInnerNumberByTabNumber(clientNumber))) {
                return true;
            }
        }
        return false;
    }
}
