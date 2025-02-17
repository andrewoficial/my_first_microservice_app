/*
Сингл-тон объект, который при ините создает файл и если нужно дописывает в него принимаемые ответы

 */
package org.example.services.loggers;

import org.apache.log4j.Logger;
import org.example.device.SomeDevice;
import org.example.services.DeviceAnswer;
import org.example.utilites.MyUtilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static org.example.device.SomeDevice.log;

public class PoolLogger {
    public static final Logger log = Logger.getLogger(PoolLogger.class);
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

    public static FileWriter fw = null;

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
        try {
            PoolLogger.fw = new FileWriter(logFile, true);
        } catch (IOException e) {
            //throw new RuntimeException(e);
            System.out.println("Ошибка создания FileWriter");
        }
    }

    public static void writeLine(DeviceAnswer answer){
        if(answer == null){
            log.warn("Для логирования передан ответ null");
            return;
        }

        if(answer.getTabNumber() < 0){
            log.warn("Для логирования передан ответ с отрицательным номером");
            return;
        }
        StringBuilder line = new StringBuilder(answer.getAnswerReceivedTime().format(MyUtilities.CUSTOM_FORMATTER));
        line.append("\t");
        line.append(answer.getDeviceType().getClass().toString().replace("class org.example.device.", ""));
        line.append("\t");
        line.append(answer.getAnswerReceivedString());
        line.append("\n");


        if((System.currentTimeMillis() - dateTimeLastWrite ) < 100L ){
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


            if(PoolLogger.fw != null) {
                BufferedWriter bw = new BufferedWriter(PoolLogger.fw);
                try {

                    bw.write(stringBuilder.toString());
                    bw.flush();
                } catch (IOException e) {
                    //throw new RuntimeException(e);
                    System.out.println("Ошибка выполнения  write " + e.getMessage());
                } finally {
                    line = null;
                    bw = null;
                }
            }
        }
        log.info("Завершено логирование в общий файл с ответами для клиента " + answer.getTabNumber());

    }


}
