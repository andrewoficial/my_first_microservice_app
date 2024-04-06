/*
Процесс (не демон), запускающийся, когда пользователь начинает опрос прибора
ToDo
Логирование последнего значения не только в файл но и в б.д
 */
package org.example.services;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import lombok.Setter;
import org.example.utilites.ProtocolsList;
import org.example.device.*;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static org.example.Main.comPorts;

public class PoolService implements Runnable{
    private ArrayList <Integer> currentTab = new ArrayList<>();
    private ArrayList <String> textToSend = new ArrayList<>();
    private ArrayList <StringBuffer> answersCollection = new ArrayList<>();
    private ArrayList <DeviceLogger> deviceLoggerArrayList = new ArrayList<>();
    private ArrayList <Boolean> needLogArrayList = new ArrayList<>();
    private ArrayList <JTextPane> receivedTextArrayList = new ArrayList<>();
    private ArrayList <StringBuilder> uxAnswerArrayList = new ArrayList<>();

    private ArrayList <String> deviceNames = new ArrayList<>();

    private ProtocolsList protocol = null;
    private SerialPort comPort;
    @Getter
    private int poolDelay;
    private SomeDevice device = null;
    private long timerWinUpdate = System.currentTimeMillis();
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();


    public PoolService(ProtocolsList protocol,
                       String textToSendString,
                       JTextPane receivedText,
                       SerialPort comPort,
                       int poolDelay,
                       boolean needLog,
                       int tabNumber) {
        super();
        this.protocol = protocol;
        this.textToSend.add(textToSendString);
        this.receivedTextArrayList.add(receivedText);
        this.needLogArrayList.add(needLog);
        this.currentTab.add(tabNumber);
        this.comPort = comPort;
        this.poolDelay = poolDelay;
    }

    public int getProtocolForJCombo(){
        return ProtocolsList.getNumber(this.protocol);
    }

    public int getComPortForJCombo(){
        ArrayList <SerialPort> ports = comPorts.getAllPorts();
        for (int i = 0; i < ports.size(); i++) {
            if(ports.get(i) != null && ports.get(i).getSystemPortName().equalsIgnoreCase(this.comPort.getSystemPortName())){
                return i;
            }
        }
        System.out.println("Текущий ком-порт не найден в списке доступных");
        return 0;
    }



    @Override
    public void run() {
        long millisLimit = poolDelay;
        long millisPrev = System.currentTimeMillis() - millisLimit - millisLimit;
        long millisDela = 0L;
        deviceNames.add(Thread.currentThread().getName());
        deviceLoggerArrayList.add(new DeviceLogger(deviceNames.get(0)));
        uxAnswerArrayList.add(new StringBuilder());
        answersCollection.add(new StringBuffer());
        while (!Thread.currentThread().isInterrupted()) {
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
                    }
                }


                assert device != null;
                for (int i = 0; i < textToSend.size(); i++) {
                    device.sendData(textToSend.get(i));
                    now = LocalDateTime.now();

                    uxAnswerArrayList.get(i).append(dtf.format(now));
                    uxAnswerArrayList.get(i).append(" ");
                    uxAnswerArrayList.get(i).append(Thread.currentThread().getName());
                    uxAnswerArrayList.get(i).append(" ");
                    if (device.hasAnswer()) {
                        uxAnswerArrayList.get(i).append(device.getAnswer());
                        uxAnswerArrayList.get(i).append("\n");
                        logSome(uxAnswerArrayList.get(i).toString(), i);
                        answersCollection.get(i).append(uxAnswerArrayList.get(i).toString());
                        //receivedText.setText("2342342342342342342342342342342342342423");
                        //receivedText.setText(uxAnswer.toString() + receivedText.getText());

                    } else {
                        uxAnswerArrayList.get(i).append("\n");
                        logSome(uxAnswerArrayList.get(i).toString(), i);
                        answersCollection.get(i).append(uxAnswerArrayList.get(i).toString());
                        //receivedText.setText("2342342342342342342342342342342342342423");
                    }
                    if((System.currentTimeMillis() - timerWinUpdate ) > 100L ){
                        timerWinUpdate = System.currentTimeMillis();
                        //receivedText.setText(Thread.currentThread().getName() + " " + String.valueOf(Math.random()));
                        receivedTextArrayList.get(i).setText(answersCollection.get(i).toString());
                    }
                    System.out.println("Now"+i);
                    System.out.println("Command" + textToSend.get(i));
                    System.out.println("Answer" + uxAnswerArrayList.get(i));
                    uxAnswerArrayList.get(i).delete(0, uxAnswerArrayList.get(i).length());

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

    private void logSome(String str, int subDevNum){
        if(needLogArrayList.get(subDevNum)) {
            //System.out.println("Do log");
            PoolLogger poolLogger = PoolLogger.getInstance();
            PoolLogger.writeLine(str);
            deviceLoggerArrayList.get(subDevNum).writeLine(str);
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

    public void addDeviceToService(int tabNumber, String command, JTextPane textPane, boolean needLog, String devName) {
        currentTab.add(tabNumber);
        textToSend.add(command);
        receivedTextArrayList.add(textPane);
        needLogArrayList.add(needLog);
        deviceLoggerArrayList.add(new DeviceLogger(Thread.currentThread().getName()));
        uxAnswerArrayList.add(new StringBuilder());
        answersCollection.add(new StringBuffer());
        deviceNames.add(devName);
    }

    public void removeDeviceToService(int tabNumber){
        int forRemove = findSubDevByTabNumber(tabNumber);
        if(forRemove > 0){
            currentTab.remove(forRemove);
            textToSend.remove(forRemove);
            receivedTextArrayList.remove(forRemove);
            needLogArrayList.remove(forRemove);
            deviceLoggerArrayList.remove(forRemove);
            uxAnswerArrayList.remove(forRemove);
            answersCollection.remove(forRemove);
            deviceNames.remove(forRemove);
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
}
