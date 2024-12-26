package org.example.services;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.utilites.ProtocolsList;
import org.example.device.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static org.example.Main.comPorts;
import static org.example.utilites.MyUtilities.bytesToHex;
import static org.example.utilites.MyUtilities.createDeviceByProtocol;

public class PoolService implements Runnable{
    private boolean threadLive = true;
    private volatile boolean  comBusy = false;
    private ArrayList <Integer> currentTab = new ArrayList<>();
    private ArrayList <String> textToSend = new ArrayList<>();
    @Getter
    private ArrayList <StringBuffer> answersCollection = new ArrayList<>();
    private ArrayList <DeviceLogger> deviceLoggerArrayList = new ArrayList<>();
    private ArrayList <Boolean> needLogArrayList = new ArrayList<>();

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
    private static final Logger log = Logger.getLogger(PoolService.class);
    public PoolService(ProtocolsList protocol,
                       String textToSendString,
                       SerialPort comPort,
                       int poolDelay,
                       boolean needLog,
                       boolean threadForEvent,
                       int tabNumber) {
        super();
        this.protocol = protocol;
        this.textToSend.add(textToSendString);
        this.needLogArrayList.add(needLog);
        this.currentTab.add(tabNumber);
        this.comPort = comPort;
        this.poolDelay = poolDelay;
        this.threadForEvent = threadForEvent;
        needPool.put(tabNumber, true);
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

                log.info("Parse external Event ASCII [" + builder.toString().trim() + "]");
                log.info("Parse external Event HEX [" + bytesToHex(builder.toString().getBytes()) + "]");
                receiveByEvent(builder.toString(), tabNumber);
            }
        };

        if(device == null){
            try {
                device = createDeviceByProtocol(protocol, comPort);
            }catch (RuntimeException e){
                System.out.println(e.getMessage());
            }
            if(device == null){
                System.out.println("device obj still null");
            }
        }
    }

    public void setupComConnection(SerialPort comPort){
        this.comPort = comPort;
    }
    public int getProtocolForJCombo(){
        return ProtocolsList.getNumber(this.protocol);
    }

    public int getComPortForJCombo(){
        ArrayList <SerialPort> ports = comPorts.getAllPorts();
        for (int i = 0; i < ports.size(); i++) {
            if(ports.get(i) != null && this.comPort != null && ports.get(i).getSystemPortName().equalsIgnoreCase(this.comPort.getSystemPortName())){
                return i;
            }
        }
        System.out.println("Текущий ком-порт не найден в списке доступных");
        return 0;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Thread Pool Tab " + currentTab.get(0));
        deviceLoggerArrayList.add(new DeviceLogger(currentTab.get(0).toString()));
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
                    log.info("Отправка команды: " + textToSend.get(i));
                    sendOnce(textToSend.get(i), i, true);
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
            log.info("Инициирована отправка команды прибору " + arg + " внутренний вызов? " + false + ". Будет выполнена.  Флаг comBusy проигнорирован, его статус " + comBusy);
            log.info("Старый аргумент указателя "  + i + " соответствует вкладке " + findSubDevByTabNumber(i));
            i = findSubDevByTabNumber(i);
            log.info("Новый аргумент указателя "  + i + " соответствует вкладке " + findSubDevByTabNumber(i));
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
        log.info("Параметр i = " + i + " tabDirection будет задан " + tabDirection);

        LocalDateTime startSend = LocalDateTime.now();
        comPort.flushDataListener();
        comPort.removeDataListener();
        sleepSafely(50);

        device.sendData(arg, device.getStrEndian(), comPort, device.isKnownCommand(), 150, device);
        device.receiveData(device);

        comPort.addDataListener(serialPortDataListener);

        device.parseData();
        DeviceAnswer answer = new DeviceAnswer(startSend,textToSend.get(i),tabDirection);
        answer.setDeviceType(device);
        answer.setAnswerReceivedTime(LocalDateTime.now());
        if (device.getValues() != null) {
            answer.setAnswerReceivedString(device.getAnswer());
            answer.setAnswerReceivedValues(device.getValues());
        }else{
            answer.setAnswerReceivedString(device.getAnswer());
            answer.setAnswerReceivedValues(null);
        }
        AnswerStorage.addAnswer(answer);
        logSome(answer, i);
        comBusy = false;
    }

    public void receiveByEvent (String msg, int tabN){
        tabN =  findSubDevByTabNumber(tabN);
        if(device == null){
            device = createDeviceByProtocol(protocol, comPort);
        }

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

        AnswerStorage.addAnswer(answer);
        logSome(answer, tabN);
    }

    public void setNeedPool (int tabNum, boolean bool){
        needPool.put(tabNum, bool);
        if(! bool){
            if(comPort != null){ //Если закрытие вкладки, которая использует один и тот же ком-порт с другой вкладкой
                //comPort.addDataListener(serialPortDataListener);
            }

        }
        if((!bool) && (! isRootTab(tabNum)) ){
            //this.threadLive = false;
        }
        if (threadForEvent){
            threadLive = true;
        }
        if(bool){
            threadLive = true;
        }
        if(! threadLive){
            System.out.println("Поток будет закрыт");
        }
    }
    private int getTabNumberByInnerNumber(int innerNumber){
        for (int i = 0; i < currentTab.size(); i++) {
            if(i == innerNumber){
                return currentTab.get(i);
            }
        }
        return -1;
    }

    private int getInnerNumberByTabNumber(int tabNumber){
        for (int i = 0; i < currentTab.size(); i++) {
            if(currentTab.get(i).equals(tabNumber)){
                return i;
            }
        }
        return -1;
    }
    private void logSome(DeviceAnswer answer, int subDevNum){
        //System.out.println("Делаю общий лог");
        PoolLogger poolLogger = PoolLogger.getInstance();
        PoolLogger.writeLine(answer);
        if(needLogArrayList.get(subDevNum)) {
            //System.out.println("Делаю лог отдельный для прибора");
            //PoolLogger poolLogger = PoolLogger.getInstance();
            //PoolLogger.writeLine(answer);
            deviceLoggerArrayList.get(subDevNum).writeLine(answer);
        }

        if(this.device != null && this.device.getClass().equals(GPS_Test.class) && (! loggersSet.containsKey(answer.getTabNumber()) )){
            //System.out.println("Создан GPS logger");
            loggersSet.put(answer.getTabNumber(), new GPS_Loger("GPS_"+AnswerStorage.getIdentByTab(answer.getTabNumber()),answer.getTabNumber() ));
            //gpsLoger = new GPS_Loger("demo",answer.getTabNumber() );
        }else{
//            System.out.println("Не создан по одной из причин");
//            System.out.print("Не содержится в коллекции? (надо создать) ");
//            System.out.println(! loggersSet.containsKey(answer.getTabNumber()));
//            System.out.print("Выбранный протокол не подходит? ");
//            System.out.println(this.device.getClass().equals(GPS_Test.class));
//            System.out.print("Не выбран протокол? ");
//            System.out.println(this.device != null);
        }
        if(loggersSet.containsKey(answer.getTabNumber())){
            //System.out.println("Run write GPS LOG" + answer);
            loggersSet.get(answer.getTabNumber()).writeLine(answer);
        }



    }


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
    public void setTextToSendString(String cmd, int tabNumber){
        if(cmd == null || cmd.isEmpty()){
            return;
        }
        if(findSubDevByTabNumber(tabNumber) != -1){
            textToSend.set(findSubDevByTabNumber(tabNumber), cmd);
        }
    }

    public String getTextToSensByTab(int tabNum){
        if(findSubDevByTabNumber(tabNum) != -1){
            return textToSend.get(findSubDevByTabNumber(tabNum));
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
            return needLogArrayList.get(findSubDevByTabNumber(tabNum));
        }
        System.out.println("Подустройство не найдено для вкладки номер " + tabNum);
        return false;
    }

    public boolean isNeedPool(int tabNum){
        if(findSubDevByTabNumber(tabNum) != -1){
            //System.out.println("Определение логирования для вкладки номер" + tabNum);
            //System.out.println(needPool.toString());
            //for (Integer i : needPool.keySet()) {
                //System.out.println("Key" + i + " val: " + needPool.get(i));
            //}
            return needPool.get(tabNum);
        }
        log.error("Подустройство не найдено для вкладки номер " + tabNum);
        return false;
    }

    private int findSubDevByTabNumber(int tabNumber){
        for (int i = 0; i < currentTab.size(); i++) {
            if(currentTab.get(i) == tabNumber){
                return i;
            }
        }
        return -1;
    }

    public boolean containTabDev(int tabNum){
        for (Integer integer : currentTab) {
            if (integer == tabNum) {
                return true;
            }
        }
        return false;
    }

    public void addDeviceToService(int tabNumber, String command, boolean needLog, boolean needPoolFlag) {
        currentTab.add(tabNumber);
        textToSend.add(command);
        needLogArrayList.add(needLog);
        deviceLoggerArrayList.add(new DeviceLogger(String.valueOf(tabNumber)));
        answersCollection.add(new StringBuffer());
        needPool.put(tabNumber, needPoolFlag);
    }

    public void removeDeviceToService(int tabNumber){ //Когда вкладка закрывается
        int forRemove = findSubDevByTabNumber(tabNumber);
        if(forRemove > 0){
            currentTab.remove(forRemove);
            textToSend.remove(forRemove);
            needLogArrayList.remove(forRemove);
            deviceLoggerArrayList.remove(forRemove);
            answersCollection.remove(forRemove);
        }
    }

    public boolean isLoggedTab(int tabNum){
        for (int i = 0; i < currentTab.size(); i++) {
            if(currentTab.get(i) == tabNum){
               return needLogArrayList.get(i);
            }
        }
        return false;
    }



    public boolean isRootTab(Integer tabNum){
        for (int i = 0; i < currentTab.size(); i++) {
            if(Objects.equals(currentTab.get(i), tabNum)){
                continue;
            }
            if(needPool.get(currentTab.get(i))){
                return true;
            }
        }
        return false;
    }
}
