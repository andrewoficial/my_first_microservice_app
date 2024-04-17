/*
Процесс (не демон), запускающийся, когда пользователь начинает опрос прибора
ToDo
Логирование последнего значения не только в файл но и в б.д
 */
package org.example.services;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.Setter;
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

public class PoolService implements Runnable{
    private boolean threadLive = true;
    private ArrayList <Integer> currentTab = new ArrayList<>();
    private ArrayList <String> textToSend = new ArrayList<>();
    @Getter
    private ArrayList <StringBuffer> answersCollection = new ArrayList<>();
    private ArrayList <DeviceLogger> deviceLoggerArrayList = new ArrayList<>();
    private ArrayList <Boolean> needLogArrayList = new ArrayList<>();

    private HashMap <Integer, Boolean> needPool = new HashMap<>(); //TabNumber -- PoolFlag

    private ProtocolsList protocol = null;
    @Getter
    private SerialPort comPort;
    @Getter
    private int poolDelay;
    private SomeDevice device = null;

    LocalDateTime now = LocalDateTime.now();


    public PoolService(ProtocolsList protocol,
                       String textToSendString,
                       SerialPort comPort,
                       int poolDelay,
                       boolean needLog,
                       int tabNumber) {
        super();
        this.protocol = protocol;
        this.textToSend.add(textToSendString);
        this.needLogArrayList.add(needLog);
        this.currentTab.add(tabNumber);
        this.comPort = comPort;
        this.poolDelay = poolDelay;
        needPool.put(tabNumber, true);
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
        //System.out.println(Thread.currentThread().getName());
        long millisLimit = poolDelay;
        long millisPrev = System.currentTimeMillis() - millisLimit - millisLimit;
        deviceLoggerArrayList.add(new DeviceLogger(currentTab.get(0).toString()));
        answersCollection.add(new StringBuffer());
        while ((!Thread.currentThread().isInterrupted()) && threadLive) {
            if (System.currentTimeMillis() - millisPrev > millisLimit) {
                millisPrev = System.currentTimeMillis();
                if(device == null){
                    switch (protocol) {
                        case IGM10ASCII -> {
                            device = new IGM_10(comPort);
                        }
                        case ARD_BAD_VOLTMETER -> {
                            device = new ARD_BAD_VLT(comPort);
                        }
                        case ARD_BAD_FEE_BRD -> {
                            device = new ARD_BAD_FEE_BRD(comPort);
                        }
                        case ARD_FEE_BRD_METER -> {
                            device = new ARD_FEE_BRD_METER(comPort);
                        }
                        case ERSTEVAK_MTP4D -> {
                            device = new ERSTEVAK_MTP4D(comPort);
                        }
                        case EDWARDS_D397_00_000 -> {
                            device = new EDWARDS_D397_00_000(comPort);
                        }
                    }
                }


                assert device != null;
                for (int i = 0; i < textToSend.size(); i++) {
                    //Если для внутренней очереди нет номера вкладки ИЛИ флаг опроса FALSE
                    if(getTabNumberByInnerNumber(i) < 0 || (!needPool.get(getTabNumberByInnerNumber(i)))){
                        System.out.println(needPool.get(getTabNumberByInnerNumber(i)));
                        continue;
                    }
                    DeviceAnswer answer = new DeviceAnswer(
                            LocalDateTime.now(),
                            textToSend.get(i),
                            getTabNumberByInnerNumber(i));
                    device.sendData(textToSend.get(i));
                    now = LocalDateTime.now();
                    answer.setDeviceType(device);
                    answer.setAnswerReceivedTime(LocalDateTime.now());
                    if (device.hasAnswer()) {
                        answer.setAnswerReceivedString(device.getAnswer());
                        answer.setAnswerReceivedValues(device.getValues());
                    }
                    AnswerStorage.addAnswer(answer);
                    logSome(answer, i);
                }
            }else {
                try {
                    Thread.sleep(Math.min((millisLimit / 3), 300L));
                    //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            }

        }
    }

    public void setNeedPool (int tabNum, boolean bool){
        needPool.put(tabNum, bool);
        if((!bool) && (! isRootTab(tabNum))){
            System.out.println("Поток будет закрыт");
            this.threadLive = false;
        }
        if(bool){
            threadLive = true;
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

        if(needLogArrayList.get(subDevNum)) {
            //System.out.println("Do log");
            PoolLogger poolLogger = PoolLogger.getInstance();
            PoolLogger.writeLine(answer);
            deviceLoggerArrayList.get(subDevNum).writeLine(answer);
        }


    }


    public void setPoolDelay(String poolDelay) {
        int newPoolDelay = 2000;
        try {
            newPoolDelay = Integer.parseInt(poolDelay);
        }catch (Exception e){
            System.out.println("Неверное значение параметра задержки опроса");
        }
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

    public void addDeviceToService(int tabNumber, String command, boolean needLog) {
        currentTab.add(tabNumber);
        textToSend.add(command);
        needLogArrayList.add(needLog);
        deviceLoggerArrayList.add(new DeviceLogger(String.valueOf(tabNumber)));
        answersCollection.add(new StringBuffer());
        needPool.put(tabNumber, true);
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
