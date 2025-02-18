package org.example.services.loggers;

import org.apache.log4j.Logger;
import org.example.device.SomeDevice;
import org.example.services.DeviceAnswer;
import org.example.utilites.MyUtilities;
import org.example.utilites.ProgramUpdater;
import org.example.utilites.properties.MyProperties;
import org.hibernate.engine.spi.IdentifierValue;

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
    private MyProperties properties = MyProperties.getInstance();
    private static final Logger log = Logger.getLogger(DeviceLogger.class);
    private Long dateTimeLastWrite = System.currentTimeMillis();
    private final ArrayList<String> stringsBuffer = new ArrayList<>();
    private final ArrayList<String> stringsBufferCSV = new ArrayList<>();

    public DeviceLogger(String name) {
        if(properties.isCsvLogState()){
            this.fileNameCSV = this.fileNameCSV + " " + "tab_" + name + ".csv";
            this.logFileCSV = createLogFile("logs", this.fileNameCSV);
        }

        if(properties.isDbgLogState()){
            this.fileName = this.fileName + " " + "tab_" + name + ".txt";
            this.logFile = createLogFile("logs", this.fileName);
        }
    }

    public DeviceLogger(Integer name) {
        if(properties.isCsvLogState()){
            this.fileNameCSV = this.fileNameCSV + " " + "tab_" + name + ".csv";
            this.logFileCSV = createLogFile("logs", this.fileNameCSV);
        }

        if(properties.isDbgLogState()){
            this.fileName = this.fileName + " " + "tab_" + name + ".txt";
            this.logFile = createLogFile("logs", this.fileName);
        }
    }

    private File createLogFile(String directory, String fileName) {
        File logFile = null;
        try {
            logFile = new File(directory + "/" + fileName);
            if (!logFile.exists()) {
                new File(directory).mkdirs();
                if (logFile.createNewFile()) {
                    // Log file creation success if needed
                } else {
                    // Log file already exists
                }
            }
        } catch (IOException e) {
            // Handle exception for file creation
            log.warn("Проблема при создании файла лога" + e.getMessage());
        } catch (Exception e) {
            // Handle other exceptions
            log.warn("Проблема при создании файла лога" + e.getMessage());
        }
        return logFile;
    }

    public void writeLine (DeviceAnswer answer){
        StringBuilder lineCSV = null;
        StringBuilder line = null;

        // Create lineCSV builder if CSV logging is enabled
        if (properties.isCsvLogState()) {
            lineCSV = new StringBuilder();
            lineCSV.append(answer.toStringCSV());
        }

        // Create line builder if TXT logging is enabled
        if (properties.isDbgLogState()) {
            line = new StringBuilder();
            line.append(answer.toStringDBG());
        }

        // Check buffering conditions
        if ((System.currentTimeMillis() - dateTimeLastWrite) < 300L) {
            if (line != null) {
                stringsBuffer.add(line.toString());
            }
            if (lineCSV != null) {
                stringsBufferCSV.add(lineCSV.toString());
            }
        } else {
            dateTimeLastWrite = System.currentTimeMillis();
            if (line != null) {
                stringsBuffer.add(line.toString());
            }
            if (lineCSV != null) {
                stringsBufferCSV.add(lineCSV.toString());
            }

            // Combine buffered lines for writing
            StringBuilder stringBuilder = new StringBuilder();
            for (String s : stringsBuffer) {
                stringBuilder.append(s);
            }

            StringBuilder stringBuilderCSV = new StringBuilder();
            for (String s : stringsBufferCSV) {
                stringBuilderCSV.append(s);
            }

            // Clear buffers after writing
            stringsBuffer.clear();
            stringsBufferCSV.clear();

            // Write to files
            if (properties.isDbgLogState() && logFile != null) {
                writeFile(stringBuilder, logFile);
            }
            if (properties.isCsvLogState() && logFileCSV != null) {
                writeFile(stringBuilderCSV, logFileCSV);
            }
        }
        log.info("Завершено ведение лога согласно настройками для идентефикатора " + answer.getTabNumber());
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
