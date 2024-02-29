package org.example.services;

import org.example.ProtocolsList;
import org.example.device.*;

import javax.swing.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.example.Main.comPorts;

public class PoolService implements Runnable{
    private ProtocolsList protocol;
    private String textToSendString;

    private JCheckBox CB_Pool;
    private JTextPane receivedText;

    public PoolService(ProtocolsList protocol, String textToSendString, JTextPane receivedText,JCheckBox CB_Pool) {
        super();
        this.protocol = protocol;
        this.textToSendString = textToSendString;
        this.receivedText = receivedText;
        this.CB_Pool = CB_Pool;
    }

    @Override
    public void run() {
        long millisLimit = 1500L;
        long millisPrev = System.currentTimeMillis();
        long millisDela = 0L;
        while (!Thread.currentThread().isInterrupted()) {
            if (System.currentTimeMillis() - millisPrev > millisLimit) {
                millisPrev = System.currentTimeMillis();
                SomeDevice device = null;
                switch (protocol) {
                    case ProtocolsList.IGM10ASCII -> {
                        device = new IGM_10(comPorts.activePort);
                    }
                    case ProtocolsList.ARD_BAD_VOLTMETER -> {
                        device = new ARD_BAD_VLT(comPorts.activePort);
                    }
                    case ProtocolsList.ARD_BAD_FEE_BRD -> {
                        device = new ARD_BAD_FEE_BRD(comPorts.activePort);
                    }
                    case ProtocolsList.ARD_FEE_BRD_METER -> {
                        device = new ARD_FEE_BRD_METER(comPorts.activePort);
                    }
                }

                assert device != null;
                device.sendData(textToSendString);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime now = LocalDateTime.now();
                //System.out.println(dtf.format(now)); //2016/11/16 12:08:43

                if (device.hasAnswer()) {
                    String uxAnswer = receivedText.getText() + "\n" + dtf.format(now) + " " + device.getAnswer();
                    receivedText.setText(uxAnswer);

                    logSome(uxAnswer);
                } else {
                    String uxAnswer = receivedText.getText() + "\n" + dtf.format(now) + " " + device.getAnswer();
                    receivedText.setText(uxAnswer);
                    logSome(uxAnswer);
                }
                System.out.println();
            }

        }
    }

    private void logSome(String str){
        PoolLogger poolLogger = PoolLogger.getInstance();
        PoolLogger.writeLine(str);
    }
}
