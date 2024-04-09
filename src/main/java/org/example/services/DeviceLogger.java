package org.example.services;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class DeviceLogger {
    private String fileName = (new SimpleDateFormat("yyyy.MM.dd HH.mm.ss.SS").format(Calendar.getInstance().getTime()));
    private File logFile;
    private Long dateTimeLastWrite = System.currentTimeMillis();
    private final ArrayList<String> stringsBuffer = new ArrayList<>();

    public DeviceLogger(String name){
        this.fileName = this.fileName + " " + name + ".txt";
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
    }

    public void writeLine (String line){
        if((System.currentTimeMillis() - dateTimeLastWrite ) < 300L ){
            stringsBuffer.add(line);
            //System.out.println("Log buffered");
        }else {
            dateTimeLastWrite = System.currentTimeMillis();
            stringsBuffer.add(line);
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : stringsBuffer) {
                stringBuilder.append(s);
            }
            stringsBuffer.clear();
            line = stringBuilder.toString();
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
                bw.write(line);

                bw.close();
            } catch (IOException e) {
                //throw new RuntimeException(e);
                System.out.println("Ошибка выполнения  write ");
            }
        }
    }
}
