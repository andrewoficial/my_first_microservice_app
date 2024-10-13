package org.example.services;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.utilites.ProtocolsList;
import org.example.device.*;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

import static org.example.Main.comPorts;
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
    private long poolDelay;

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
                int size = comPort.bytesAvailable();
                byte[] buffer = new byte[size];
                while (comPort.bytesAvailable() > 0) {

                    comPort.readBytes(buffer, size);
                    for (int i = 0; i < size; i++) {
                        builder.append((char) buffer[i]);
                    }

                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                        //throw new RuntimeException(ex);
                    }
                }
                if(log.isInfoEnabled()){
                    log.info("Parse external Event ASCII [" + builder.toString().trim() + "] ");
                    log.info("Parse external Event HEX " + buffer.toString() + " ");
                }
                receiveByEvent(builder.toString(), tabNumber);
                //System.out.println(builder.toString());
            }
        };
        if(comPort != null){
            comPort.addDataListener(serialPortDataListener);
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
        Thread.currentThread().setName("Thread Pool Tab "+currentTab.get(0));
        deviceLoggerArrayList.add(new DeviceLogger(currentTab.get(0).toString()));
        answersCollection.add(new StringBuffer());
        while ((!Thread.currentThread().isInterrupted()) && threadLive) {
            if (System.currentTimeMillis() - millisPrev > poolDelay) {
                millisPrev = System.currentTimeMillis();
                //System.out.println("Millis timer");
                assert device != null;
                //comPort.removeDataListener();
                //System.out.println("Run pool...");

                for (int i = 0; i < textToSend.size(); i++) {
                    boolean wasWaited = false;
                    //Если для внутренней очереди нет номера вкладки ИЛИ флаг опроса FALSE
                    if(getTabNumberByInnerNumber(i) < 0 || (!needPool.get(getTabNumberByInnerNumber(i)))){
                        //System.out.println(needPool.get(getTabNumberByInnerNumber(i)));
                        continue;
                    }else{
                        while (comBusy){
                            wasWaited = true;
                            try {
                                //log.info("Флаг comBusy: " + comBusy + " из цикла опроса НЕ вызываю sendOnce с командой " + textToSend.get(i) + " внутренняя очередь i: " + i);
                                Thread.sleep(20);
                                //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
                            } catch (InterruptedException e) {
                                //throw new RuntimeException(e);
                            }
                        }
                        if(wasWaited){
                            log.info("Завершено ожидание снятия флага comBusy из цикла опроса при вызове sendOnce с командой " + textToSend.get(i) + " внутренняя очередь i: " + i);
                        }
                        log.info("Флаг comBusy: " + comBusy + " из цикла опроса вызываю sendOnce с командой " + textToSend.get(i) + " внутренняя очередь i: " + i);

                        sendOnce(textToSend.get(i), i, true);
                    }


                }
                //comPort.addDataListener(serialPortDataListener);
            }else {
                try {
                    Thread.sleep(Math.min((poolDelay / 5), 100L));
                    //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            }
        }
    }


    public void sendOnce (String arg, int i, boolean internal){
        //comBusy = comBusy;

        if(!internal){
            log.info("Инициирована отправка команды прибору " + arg + " внутренний вызов? " + false + ". Будет выполнена.  Флаг comBusy проигнорирован, его статус " + comBusy);
            //comBusy =  false;
            log.info("Изменен номер вкладки с внутренней очереди на какую-то иную. ");
            log.info("Старый аргумент указателя "  + i + " соответствует вкладке " + findSubDevByTabNumber(i));
            i = findSubDevByTabNumber(i);
            log.info("Новый аргумент указателя "  + i + " соответствует вкладке " + findSubDevByTabNumber(i));
        }
        if (comBusy) {
            log.info("Инициирована отправка команды прибору " + arg + " внутренний вызов? " + internal + ". Будет отвергнута. Флаг comBusy учтен, его статус " + comBusy);
            try {
                Thread.sleep(5);
                //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
            return;
        }else{

            comBusy = true;
            log.info("Инициирована отправка команды прибору " + arg + " внутренний вызов? " + internal + ". Будет выполнена. Флаг comBusy установлен в " + comBusy);
        }

        if(device == null){
            try {
                device = createDeviceByProtocol(protocol, comPort);
            }catch (RuntimeException e){
                System.out.println(e.getMessage());
            }
            if(device == null){
                System.out.println("device obj still null");
                return;
            }
            log.info("Выполнено определение протокола");
        }

        int tabDirection = getTabNumberByInnerNumber(i);
        log.info("Параметр i = " + i + " tabDirection будет задан " + tabDirection);


        DeviceAnswer answer = new DeviceAnswer(LocalDateTime.now(),textToSend.get(i),tabDirection);

        if(device.isBisy()){
            log.info("ОТМЕНЕНА ОТПРАВКА ЗАНЯТОМУ УСТРОЙСТВУ");
            return;
        }
        device.sendData(arg, device.getStrEndian(), comPort, device.isKnownCommand(), 300, device);
        log.info("Инициирован приём ответа ");
        device.receiveData(device);

        //log.info("Инициирована обработка ответа");
        device.parseData();
        answer.setDeviceType(device);
        answer.setAnswerReceivedTime(LocalDateTime.now());
        if (device.hasAnswer()) {
            answer.setAnswerReceivedString(device.getAnswer());
            answer.setAnswerReceivedValues(device.getValues());
        }else{
            answer.setAnswerReceivedString(device.getAnswer());
            answer.setAnswerReceivedValues(null);
        }

        if(device.getTabForAnswer() != null) {
            tabDirection = device.getTabForAnswer();
            log.info("Direction storage changed to: " + tabDirection);
        }

        //log.info("Адрес ответа: " + answer.getTabNumber());
        //System.out.println("Адрес ответа: " + answer.getTabNumber());
        AnswerStorage.addAnswer(answer);

        logSome(answer, i);
        //receiveMsg(i, internal, arg);
        if(!internal){
            comPort.addDataListener(serialPortDataListener);
            log.info("Добавлен слушатель (разовая отправка)");
        }


        try {
            Thread.sleep(20);
            //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        }
        comBusy = false;
    }

    public void receiveByEvent (String msg, int tabN){
        //System.out.println("Receive by event " + msg);
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

        //System.out.println("Event from tab " + tabN + " and inner number " + getInnerNumberByTabNumber(tabN));
        int tabDirection = getTabNumberByInnerNumber(tabN);
        //System.out.println("    Direction storage set to: " + tabDirection);
        if(device.getTabForAnswer() != null) {
            tabDirection = device.getTabForAnswer();
            //System.out.println("    Direction storage changed to: " + tabDirection);
        }

        DeviceAnswer answer = new DeviceAnswer(LocalDateTime.now(),"",tabDirection);
        answer.setDeviceType(device);
        answer.setAnswerReceivedTime(LocalDateTime.now());
        answer.setAnswerReceivedTime(LocalDateTime.now());
        answer.setAnswerReceivedString(device.getAnswer());
        answer.setAnswerReceivedValues(device.getValues());

        AnswerStorage.addAnswer(answer);
        logSome(answer, tabN);
        //System.out.println(tabN);

    }

    public void setNeedPool (int tabNum, boolean bool){
        needPool.put(tabNum, bool);
        if(! bool){
            if(comPort != null){ //Если закрытие вкладки, которая использует один и тот же ком-порт с другой вкладкой
                comPort.addDataListener(serialPortDataListener);
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
        System.out.println("Подустройство не найдено для вкладки номер " + tabNum);
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
        //System.out.println("Попытка закрыть поток опроса, у которого есть дочерние вкладки");
        return false;
    }
}
