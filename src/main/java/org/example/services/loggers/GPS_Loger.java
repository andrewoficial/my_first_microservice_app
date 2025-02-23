package org.example.services.loggers;

import org.example.services.AnswerStorage;
import org.example.services.DeviceAnswer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class GPS_Loger {
    private String fileName = (new SimpleDateFormat("yyyy.MM.dd HH-mm-ss").format(Calendar.getInstance().getTime()));
    private File logFile;
    private Long dateTimeLastWrite = System.currentTimeMillis();
    private final ArrayList<String> stringsBuffer = new ArrayList<>();
    private int dev_ident = 0;
    private StringBuilder line = new StringBuilder();
    public GPS_Loger(String name,int  dev_ident){
        this.fileName = "" + name + ".js";
        File logFile = null;
        this.dev_ident = dev_ident;
        try{
            logFile = new File("logs"+fileName);
            if(logFile.exists() && !logFile.isDirectory()) {
                // do something
            }else {
                new File("logs").mkdirs();
            }
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }

        try {
            logFile = new File("logs/"+fileName);
            if (logFile.createNewFile()) {
                //System.out.println("File created: " + myObj.getName());
                //System.out.println("File created: " + logFile.getAbsolutePath());
            } else {
                //System.out.println("File already exists.");
                //System.out.println(logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            //System.out.println("An error occurred.");
            //e.printStackTrace();
        }
        this.logFile = logFile;


        //First string
        StringBuilder line = new StringBuilder();
        //line.append("var addressPoints = [\n");

        if((System.currentTimeMillis() - dateTimeLastWrite ) < 300L ){
            stringsBuffer.add(line.toString());
            //System.out.println("Log buffered");
        }else {
            dateTimeLastWrite = System.currentTimeMillis();
            stringsBuffer.add(line.toString());
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : stringsBuffer) {
                stringBuilder.append(s);
            }
            stringsBuffer.clear();
            FileWriter fw = null;
            try {
                fw = new FileWriter(logFile, true);
            } catch (IOException e) {
                //throw new RuntimeException(e);
                System.out.println("Ошибка создания FileWriter");
            }
            assert fw != null;
            BufferedWriter bw = new BufferedWriter(fw);
            try {
                bw.write(stringBuilder.toString());

                bw.close();
            } catch (IOException e) {
                //throw new RuntimeException(e);
                System.out.println("Ошибка выполнения  write ");
            }
        }
    }

    public void writeLine (DeviceAnswer answer){
        line.setLength(0);
        if(answer != null && answer.getAnswerReceivedValues() != null
                && answer.getAnswerReceivedValues().getValues().length > 5){
            line.append("var data_"+ AnswerStorage.getIdentByTab(dev_ident)+" = [\n");
            for (int i = 0; i < AnswerStorage.getAnswersForGraph(dev_ident).size(); i++) {
                DeviceAnswer deviceAnswer = AnswerStorage.getAnswersForGraph(dev_ident).get(i);
                if(deviceAnswer != null && deviceAnswer.getAnswerReceivedValues() != null
                        && deviceAnswer.getAnswerReceivedValues().getValues().length > 5
                        && deviceAnswer.getClientId() == dev_ident){
                    line.append("[");
                    line.append(deviceAnswer.getAnswerReceivedValues().getValues()[1]);
                    line.append(", ");
                    line.append(deviceAnswer.getAnswerReceivedValues().getValues()[0]);
                    line.append(", ");
                    line.append("\"1\"");
                    if(i == AnswerStorage.getAnswersForGraph(dev_ident).size() - 1){
                        line.append("]");
                    }else{
                        line.append("],");
                    }
                    line.append("\n");
                }
            }
            line.append("];\n");

        }else {
            return;
        }


        if((System.currentTimeMillis() - dateTimeLastWrite ) < 300L ){
            stringsBuffer.add(line.toString());
            //System.out.println("Log buffered");
        }else {
            dateTimeLastWrite = System.currentTimeMillis();
            stringsBuffer.add(line.toString());
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : stringsBuffer) {
                stringBuilder.append(s);
            }
            stringsBuffer.clear();
            FileWriter fw = null;
            try {
                fw = new FileWriter(logFile, false);
            } catch (IOException e) {
                //throw new RuntimeException(e);
                System.out.println("Ошибка создания FileWriter");
            }
            assert fw != null;
            PrintWriter pw = new PrintWriter(fw);

            pw.print(stringBuilder.toString());
            pw.close();

        }


    }
}
