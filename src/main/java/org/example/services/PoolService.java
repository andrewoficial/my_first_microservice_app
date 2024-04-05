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

    private StringBuffer answersCollection = new StringBuffer();
    private ProtocolsList protocol = null;
    private String textToSendString;

    private DeviceLogger deviceLogger;


    private JTextPane receivedText;

    private SerialPort comPort;

    @Getter
    private int poolDelay;
    private SomeDevice device = null;

    private boolean needLog = false;
    private long timerWinUpdate = System.currentTimeMillis();



    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();

    StringBuilder uxAnswer = new StringBuilder("\n");
    public PoolService(ProtocolsList protocol, String textToSendString, JTextPane receivedText, SerialPort comPort, int poolDelay, boolean needLog) {
        super();
        this.protocol = protocol;
        this.textToSendString = textToSendString;
        this.receivedText = receivedText;
        this.comPort = comPort;
        this.poolDelay = poolDelay;
        this.needLog = needLog;

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
        deviceLogger = new DeviceLogger(Thread.currentThread().getName());
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
                device.sendData(textToSendString);
                now = LocalDateTime.now();

                uxAnswer.append(dtf.format(now));
                uxAnswer.append(" ");
                uxAnswer.append(Thread.currentThread().getName());
                uxAnswer.append(" ");
                if (device.hasAnswer()) {
                    uxAnswer.append(device.getAnswer());
                    uxAnswer.append("\n");
                    logSome(uxAnswer.toString());
                    answersCollection.append(uxAnswer.toString());
                    //receivedText.setText("2342342342342342342342342342342342342423");
                    //receivedText.setText(uxAnswer.toString() + receivedText.getText());

                } else {
                    uxAnswer.append("\n");
                    logSome(uxAnswer.toString());
                    answersCollection.append(uxAnswer.toString());
                    //receivedText.setText("2342342342342342342342342342342342342423");
                }
                if((System.currentTimeMillis() - timerWinUpdate ) > 550L ){
                    timerWinUpdate = System.currentTimeMillis();
                    //receivedText.setText(Thread.currentThread().getName() + " " + String.valueOf(Math.random()));
                    receivedText.setText(answersCollection.toString());
                }
                uxAnswer.delete(0, uxAnswer.length());
                //System.out.println();
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

    private void logSome(String str){
        if(needLog) {
            //System.out.println("Do log");
            PoolLogger poolLogger = PoolLogger.getInstance();
            PoolLogger.writeLine(str);
            deviceLogger.writeLine(str);
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

    public void setTextToSendString(String cmd){
        if(cmd == null || cmd.isEmpty()){
            return;
        }
        textToSendString = cmd;
    }

    public void setNeedLog(boolean bool){
        this.needLog = bool;
        System.out.println("Значение записи в файл изменено на: " + bool);
    }

    public boolean isNeedLog(){
        return this.needLog;
    }

}
