/*
Сингл-тон объект, который при ините создает файл и если нужно дописывает в него принимаемые ответы

 */
package org.example.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;

public class PoolLogger {
    private static final String fileName = (new SimpleDateFormat("yyyy.MM.dd HH-mm-ss").format(Calendar.getInstance().getTime())) + " SumLog.txt";
    private static File logFile;
    private static Long dateTimeLastWrite = System.currentTimeMillis();
    private static final ArrayList<String> stringsBuffer = new ArrayList<>();
    public static class SingletonHolder {
        public static final PoolLogger HOLDER_INSTANCE = new PoolLogger();
    }

    public static PoolLogger getInstance() {
        return SingletonHolder.HOLDER_INSTANCE;
    }

    private PoolLogger(){
        File logFile = null;
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
                System.out.println("File created: " + logFile.getAbsolutePath());
            } else {
                System.out.println("File already exists.");
                System.out.println(logFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            //e.printStackTrace();
        }
        PoolLogger.logFile = logFile;
    }

    public static void writeLine (DeviceAnswer answer){
        DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        StringBuilder line = new StringBuilder(answer.getAnswerReceivedTime().format(CUSTOM_FORMATTER));
        line.append("\t");
        line.append(answer.getDeviceType().getClass().toString().replace("class org.example.device.", ""));
        line.append("\t");
        line.append(answer.getAnswerReceivedString());
        for (int i = 0; i < answer.getAnswerReceivedValues().getCounter(); i++) {
            line.append("\t");
            line.append(answer.getAnswerReceivedValues().getValues()[i]);
            line.append("\t");
            line.append(answer.getAnswerReceivedValues().getUnits()[i]);
        }
        line.append("\n");


        String formattedString = answer.getAnswerReceivedTime().format(CUSTOM_FORMATTER);
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


}
