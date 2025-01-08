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
import org.example.services.loggers.GPS_Loger;
import org.example.services.loggers.PoolLogger;
import org.example.device.ProtocolsList;
import org.example.device.*;
import org.example.utilites.MyProperties;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;


import static org.example.utilites.MyUtilities.bytesToHex;
import static org.example.utilites.MyUtilities.createDeviceByProtocol;


public class ComDataCollector implements Runnable{
    private final MyProperties myProperties = new MyProperties();
    private boolean threadLive = true;

    private final AnyPoolService parentService;

    @Getter
    private volatile boolean  comBusy = false;


    /* Массив номеров клиентов */
    private final ArrayList <Integer> clientsTabNumbers = new ArrayList<>();
    private ArrayList <Boolean> needLogArrayList = new ArrayList<>();

    private final ArrayList <String> prefToSend = new ArrayList<>();
    private final ArrayList <String> textToSend = new ArrayList<>();
    @Getter
    private ArrayList <StringBuffer> answersCollection = new ArrayList<>();
    private ArrayList <DeviceLogger> deviceLoggerArrayList = new ArrayList<>();


    private HashMap <Integer, Boolean> needPool = new HashMap<>(); //TabNumber -- PoolFlag
    private HashMap <Integer, GPS_Loger> loggersSet = new HashMap<>(); // TabNum -- LoggersSet

    private ProtocolsList protocol = null;
    @Getter
    private SerialPort comPort;
    @Getter
    private long poolDelay = 2000;

    @Setter
    private SerialPortDataListener serialPortDataListener;
    @Setter
    private boolean threadForEvent;
    private SomeDevice device = null;
    private GPS_Loger gpsLoger = null;

    //private final long millisLimit = poolDelay;
    private long millisPrev = System.currentTimeMillis() - (poolDelay * 100);
    private static final Logger log = Logger.getLogger(ComDataCollector.class);
    public ComDataCollector(ProtocolsList protocol,
                            String prefix,
                            String command,
                            SerialPort comPort,
                            int poolDelay,
                            boolean needLog,
                            boolean threadForEvent,
                            Integer tabNumber,
                            AnyPoolService parentService) {
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
                log.info("Device in ComDataCollector created!! " + protocol.getValue());
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
        needPool.put(tabNumber, true);
        textToSend.add("");
        prefToSend.add("");


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
                //log.info("Parse external Event HEX [" + bytesToHex(builder.toString().getBytes()) + "]");
                receiveByEvent(builder.toString(), tabNumber);
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
        if(clientsTabNumbers == null){
            return 0;
        }

        return clientsTabNumbers.size();
    }

    public int getComPortForJCombo(){
        ComPort comPorts = new ComPort();
        ArrayList <SerialPort> ports = comPorts.getAllPorts();
        for (int i = 0; i < ports.size(); i++) {
            if(ports.get(i) != null && this.comPort != null && ports.get(i).getSystemPortName().equalsIgnoreCase(this.comPort.getSystemPortName())){
                comPorts = null;
                return i;
            }
        }
        System.out.println("Текущий ком-порт не найден в списке доступных");
        comPorts = null;
        return 0;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread Pool Tab " + clientsTabNumbers.get(0));
        deviceLoggerArrayList.add(new DeviceLogger(clientsTabNumbers.get(0).toString()));
        answersCollection.add(new StringBuffer());

        while ((!Thread.currentThread().isInterrupted()) && threadLive) {
            comPort.addDataListener(serialPortDataListener);
            if (System.currentTimeMillis() - millisPrev > poolDelay) {
                millisPrev = System.currentTimeMillis();
                for (int i = 0; i < textToSend.size(); i++) {
                    if (shouldSkipCommand(i)) {
                        continue;
                    }
                    waitForComBusy();
                    //log.info("Отправка префикса: " + prefToSend.get(i) + " и команды: " + textToSend.get(i));
                    sendOnce(prefToSend.get(i) + textToSend.get(i), i, true);
                }
            } else {
                sleepSafely(Math.min((poolDelay / 5), 100L));
            }
        }

    }

    private boolean shouldSkipCommand(int i) {
        return getTabNumberByInnerNumber(i) < 0 || !needPool.get(getTabNumberByInnerNumber(i));
    }

    private void waitForComBusy() {
        while (comBusy) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sleepSafely(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }


    public void sendOnce (String arg, int i, boolean internal){
        if(!internal){
            //log.info("Инициирована отправка команды прибору " + arg + " внутренний вызов? " + false + ". Будет выполнена.  Флаг comBusy проигнорирован, его статус " + comBusy);
            //log.info("Старый аргумент указателя "  + i + " соответствует вкладке " + findSubDevByTabNumber(i));
            i = findSubDevByTabNumber(i);
            //log.info("Новый аргумент указателя "  + i + " соответствует вкладке " + findSubDevByTabNumber(i));
        }
        //Проверка, если команда уже выполняется (занят ком-порт)
        if (comBusy) {
            return;
        }else{
            comBusy = true;
        }

        //Првоерка, что команда уже выполняется (занят прибором)
        if(device.isBusy()){
            log.info("ОТМЕНЕНА ОТПРАВКА ЗАНЯТОМУ УСТРОЙСТВУ");
            return;
        }

        int tabDirection = getTabNumberByInnerNumber(i);
        //log.info("Параметр i = " + i + " tabDirection будет задан " + tabDirection);

        LocalDateTime startSend = LocalDateTime.now();
        comPort.flushDataListener();
        comPort.removeDataListener();
        sleepSafely(50);
        if(textToSend.get(i) == null){
            textToSend.add(arg);//Что бы не была нулем при начале опроса по галке
        }

        if(prefToSend.get(i) == null){
            prefToSend.add("");
        }
        device.sendData(arg, device.getStrEndian(), comPort, device.isKnownCommand(), 150, device);
        device.receiveData(device);

        comPort.addDataListener(serialPortDataListener);

        device.parseData();

        DeviceAnswer answer = new DeviceAnswer(startSend,prefToSend.get(i) + textToSend.get(i),tabDirection);
        answer.setDeviceType(device);
        answer.setAnswerReceivedTime(LocalDateTime.now());
        answer.setRequestSendString(arg);


        if (device.getValues() != null) {
            answer.setAnswerReceivedString(device.getAnswer());
            answer.setAnswerReceivedValues(device.getValues());
        }else{
            answer.setAnswerReceivedString(device.getAnswer());
            answer.setAnswerReceivedValues(null);
        }


        if(myProperties.getNeedSyncSavingAnswer()){
            parentService.getAnswerSaverLogger().addAnswer(answer, tabDirection);
        }else{
            AnswerStorage.addAnswer(answer);
            logSome(answer, i);
        }

        comBusy = false;
    }

    public void receiveByEvent (String msg, int tabN){
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
        answer.setAnswerReceivedTime(LocalDateTime.now());
        answer.setAnswerReceivedString(device.getAnswer());
        answer.setAnswerReceivedValues(device.getValues());

        if(myProperties.getNeedSyncSavingAnswer()) {
            AnswerStorage.addAnswer(answer);
            logSome(answer, tabN);
        }else{
            logSome(answer, tabN);
        }
    }

    public void setNeedPool (int tabNum, boolean needStatePool){
        needPool.put(tabNum, needStatePool);
        if(! needStatePool){
            if(comPort != null){ //Если закрытие вкладки, которая использует один и тот же ком-порт с другой вкладкой
                //comPort.addDataListener(serialPortDataListener);
            }

        }
        if((!needStatePool) && (! isRootTab(tabNum)) ){
            //this.threadLive = false;
        }
        if (threadForEvent){
            threadLive = true;
        }
        if(needStatePool){
            threadLive = true;
        }
        if(! threadLive){
            System.out.println("Поток будет закрыт");
        }
    }

    public void shutdown(){
        log.info("Поток опроса " + Thread.currentThread().getName() + " будет закрыт");
        threadForEvent = false;
        needPool.clear();
        threadLive = false;
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
    private void logSome(DeviceAnswer answer, int subDevNum){

        PoolLogger.writeLine(answer);
        DeviceLogger deviceLogger= null;
        while (needLogArrayList.size() <= subDevNum) {
            needLogArrayList.add(false);
        }
        if(needLogArrayList.get(subDevNum)) {

            while (deviceLoggerArrayList.size() <= subDevNum) {
                deviceLoggerArrayList.add(new DeviceLogger("lol"));
            }
            deviceLogger = deviceLoggerArrayList.get(subDevNum);

        }
        log.info("Будет предложено согласованное логирование");
        parentService.getAnswerSaverLogger().doLog(answer, subDevNum, PoolLogger.getInstance(), deviceLogger);

    }

    private void logSomeClearly(DeviceAnswer answer, int tab){}


    public void setPoolDelay(String poolDelay) {
        int newPoolDelay = 2000;
        try {
            newPoolDelay = Integer.parseInt(poolDelay);
        }catch (Exception e){
            System.out.println("Неверное значение параметра задержки опроса");
        }
        System.out.println("change poolDelay to " + poolDelay);
        this.poolDelay = newPoolDelay;
    }

    //Получает номер вкладки на которой надо обновить команду, сопаставляет со списком
    //однотипных устройств и обновляет
    public void setTextToSendString(String prf, String cmd, int tabNumber){
        if(cmd == null || cmd.isEmpty()){
            return;
        }
        if(prf == null ){
            prf = "";
        }
        if(findSubDevByTabNumber(tabNumber) != -1){
            while (textToSend.size() <= findSubDevByTabNumber(tabNumber)){
                textToSend.add("");
            }
            while (prefToSend.size() <= findSubDevByTabNumber(tabNumber)){
                prefToSend.add("");
            }
            prefToSend.set(findSubDevByTabNumber(tabNumber), prf);
            textToSend.set(findSubDevByTabNumber(tabNumber), cmd);
        }
    }

    public String getTextToSensByTab(int tabNum){
        if(findSubDevByTabNumber(tabNum) != -1){
            return prefToSend.get(findSubDevByTabNumber(tabNum)) + textToSend.get(findSubDevByTabNumber(tabNum));
        }
        return  null;
    }

    public void setNeedLog(boolean bool, int tabNum){
        if(findSubDevByTabNumber(tabNum) != -1){
            needLogArrayList.set(findSubDevByTabNumber(tabNum), bool);
        }
        System.out.println("Значение записи в файл изменено на: " + bool + " для подустройства на вкладке " + tabNum + " в потоке опроса это устройство номер " + findSubDevByTabNumber(tabNum));
    }

    public boolean isNeedLog(int tabNum){
        if(findSubDevByTabNumber(tabNum) != -1){
            //  ArrayList<Boolean> - хранит только значение
            while(needLogArrayList.size() <= findSubDevByTabNumber(tabNum)){ //Костыль
                needLogArrayList.add(false);
            }
            return needLogArrayList.get(findSubDevByTabNumber(tabNum));
        }
        System.out.println("Подустройство не найдено для вкладки номер " + tabNum);
        return false;
    }

    public boolean isNeedPool(int tabNum){
        if(findSubDevByTabNumber(tabNum) != -1){
            // HashMap<Integer, Boolean> - хранит сразу пару - клиент/значение
            while(needPool.size() <= findSubDevByTabNumber(tabNum)){
                needPool.get(tabNum);
            }
            return needPool.get(tabNum);
        }
        log.error("Подустройство не найдено для вкладки номер " + tabNum);
        return false;
    }

    private int findSubDevByTabNumber(int tabNumber){
        for (int i = 0; i < clientsTabNumbers.size(); i++) {
            if(clientsTabNumbers.get(i) == tabNumber){
                return i;
            }
        }
        return -1;
    }

    public boolean containTabDev(int tabNum){
        for (Integer integer : clientsTabNumbers) {
            if (integer == tabNum) {
                return true;
            }
        }
        return false;
    }

    public void addDeviceToService(int tabNumber, String prf, String cmd, boolean needLog, boolean needPoolFlag) {
        clientsTabNumbers.add(tabNumber);
        prefToSend.add(prf);
        textToSend.add(cmd);
        needLogArrayList.add(needLog);
        deviceLoggerArrayList.add(new DeviceLogger(String.valueOf(tabNumber)));
        answersCollection.add(new StringBuffer());
        needPool.put(tabNumber, needPoolFlag);
    }

    public void removeDeviceFromComDataCollector(int tabNumber){ //Когда вкладка закрывается
        int forRemove = findSubDevByTabNumber(tabNumber);
        log.info("Удаление вкладки из потока " + tabNumber  + Thread.currentThread().getName());
        log.info("Поток содержит клиентов до удаления" + clientsTabNumbers.size());
        for (int i = 0; i < clientsTabNumbers.size(); i++) {
            if (clientsTabNumbers.get(i) == tabNumber) {
                clientsTabNumbers.remove(i);
                i--; // Уменьшаем индекс, чтобы учесть сдвиг
            }
        }
        if(forRemove > 0){
            textToSend.remove(forRemove);
            prefToSend.remove(forRemove);
            needLogArrayList.remove(forRemove);
            deviceLoggerArrayList.remove(forRemove);
            answersCollection.remove(forRemove);
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



    public boolean isLoggedTab(int tabNum){
        for (int i = 0; i < clientsTabNumbers.size(); i++) {
            if(clientsTabNumbers.get(i) == tabNum){
               return needLogArrayList.get(i);
            }
        }
        return false;
    }




    public boolean isRootTab(Integer tabNum) {
        // Перебираем массив номеров вкладок
        for (Integer integer : clientsTabNumbers) {
            // Если номер не совпадает и вкладка требует опроса, возвращаем true
            if (!Objects.equals(integer, tabNum) && needPool.get(integer)) {
                return true;
            }
        }
        return false;
    }
}
