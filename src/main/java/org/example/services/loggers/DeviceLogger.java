package org.example.services.loggers;

import org.example.services.DeviceAnswer;
import org.example.utilites.MyUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class DeviceLogger {
    private String fileName = (java.time.LocalDateTime.now().format(MyUtilities.CUSTOM_FORMATTER_FILES));
    private String fileNameCSV = (java.time.LocalDateTime.now().format(MyUtilities.CUSTOM_FORMATTER_FILES));
    private File logFile;
    private File logFileCSV;
    private Long dateTimeLastWrite = System.currentTimeMillis();
    private final ArrayList<String> stringsBuffer = new ArrayList<>();
    private final ArrayList<String> stringsBufferCSV = new ArrayList<>();

    public DeviceLogger(String name){
        this.fileName = this.fileName + " " + "tab_" + name + ".txt";
        this.fileNameCSV = this.fileNameCSV + " " + "tab_" + name + ".csv";
        File logFile = null;
        File logFileCSV = null;
        try{
            logFile = new File("logs"+fileName);
            logFileCSV = new File("logs"+fileNameCSV);

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

        try{
            logFileCSV = new File("logs/"+fileNameCSV);
        }catch (Exception e){
            //Dunno...
        }
        this.logFile = logFile;
        this.logFileCSV = logFileCSV;
    }

    public void writeLine (DeviceAnswer answer){
        StringBuilder lineCSV = new StringBuilder();
        StringBuilder line = new StringBuilder();

        line.append(answer.getRequestSendTime().format(MyUtilities.CUSTOM_FORMATTER));
        line.append("\t");
        line.append(answer.getRequestSendString());
        line.append("\n");
        line.append(answer.getAnswerReceivedTime().format(MyUtilities.CUSTOM_FORMATTER));
        line.append("\t");
        line.append(answer.getAnswerReceivedString());
        line.append("\n");

        lineCSV.append(answer.toStringCSV());



        if((System.currentTimeMillis() - dateTimeLastWrite ) < 300L ){
            stringsBuffer.add(line.toString());
            stringsBufferCSV.add(lineCSV.toString());
            //System.out.println("Log buffered");
        }else {
            //ToDo Do in another thread
            dateTimeLastWrite = System.currentTimeMillis();
            stringsBuffer.add(line.toString());
            stringsBufferCSV.add(lineCSV.toString());
            StringBuilder stringBuilder = new StringBuilder();
            StringBuilder stringBuilderCSV = new StringBuilder();
            for (String s : stringsBuffer) {
                stringBuilder.append(s);
            }
            for (String s : stringsBufferCSV) {
                stringBuilderCSV.append(s);
            }
            stringsBuffer.clear();
            stringsBufferCSV.clear();
            writeFile(stringBuilder, logFile);
            writeFile(stringBuilderCSV, logFileCSV);

        }
    }

    private void writeFile(StringBuilder sbToWrite, File file){
        FileWriter fw = null;
        try {
            fw = new FileWriter(file, true);
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println("Ошибка создания FileWriter" + file.getName());
        }
        assert fw != null;
        BufferedWriter bw = new BufferedWriter(fw);
        try {
            bw.write(sbToWrite.toString());

            bw.close();
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println("Ошибка выполнения  write ");
        }
    }
}
