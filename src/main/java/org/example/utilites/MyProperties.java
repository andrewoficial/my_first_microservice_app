package org.example.utilites;


import lombok.Getter;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.example.Main;

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

/**
 * The class responsible for getting the settings from the file
 * (what not to place in the code and on the git-hub)
 *
 * <p>Author: Andrew Kantser</p>
 * <p>Date: 2023-07-01</p>
 *
 */

public class MyProperties {
    private static final Logger log = Logger.getLogger(MyProperties.class);
    public static String driver = "org.postgresql.Driver";
    public static String url = "jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk";
    public static String pwd = "EcrvEk0pw2UaY6jdKY16R3RGiBrefui1";

    public static String usr = "zhsiszsk";
    public static String prt = "8080";
    @Getter
    private String lastComPort;

    @Getter
    private String logLevel;

    @Getter
    private int lastComSpeed;

    @Getter
    private int lastDataBits;

    @Getter
    private int lastStopBits;

    @Getter
    private String lastParity;

    @Getter
    private String lastProtocol;


    private final File settingFile;

    private java.util.Properties properties;

    public MyProperties(){


        log.info("Start load configAccess.properties");
        try{
            File f = new File("config"+"configAccess.properties");
            if(f.exists() && !f.isDirectory()) {
                // do something
            }else {
                new File("config").mkdirs();
            }
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
        File someFile = null;
        try {
            someFile = new File("config/"+"configAccess.properties");
            if (someFile.createNewFile()) {
                //System.out.println("File created: " + myObj.getName());
                log.warn("Создан новый файл с настройками" + someFile.getAbsolutePath());
            } else {
                log.info("Файл с настройками найден" + someFile.getAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Ошибка при работе с файлом настроек " + e.getMessage());
        }
        this.settingFile = someFile;


        java.util.Properties props = new java.util.Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(this.settingFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            //throw new RuntimeException(e);
        }
        try {
            props.load(in);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        } finally {
            try {
                assert in != null;
                in.close();
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }

        this.properties = props;

        // Получение значений из файла
        this.lastComPort = props.getProperty("lastComPort");
        this.logLevel = props.getProperty("logLevel");
        if(this.logLevel == null){
            log.info("Уровень логирования сброшен на значение по умолчанию");
            this.logLevel = "WARN";
            this.updateFile();
        }
        Logger root = Logger.getRootLogger();
        Enumeration allLoggers = root.getLoggerRepository().getCurrentCategories();
        root.setLevel(Level.toLevel(this.logLevel));
        while (allLoggers.hasMoreElements()){
            Category tmpLogger = (Category) allLoggers.nextElement();
            tmpLogger .setLevel(Level.toLevel(this.logLevel));
        }

        try{
            this.lastComSpeed = Integer.parseInt(props.getProperty("lastComSpeed"));
            log.info("Last ComSpeed: " + lastComSpeed);
        }catch (NumberFormatException exception){
            this.lastComSpeed = 0;
            log.info("configAccess.properties contain incorrect value of lastComSpeed");
        }

        try{
            this.lastDataBits = Integer.parseInt(props.getProperty("lastDataBits"));
            log.info("Last DataBits: " + lastDataBits);
        }catch (NumberFormatException exception){
            this.lastDataBits = 0;
            log.info("configAccess.properties contain incorrect value of lastDataBits");
        }

        try{
            this.lastStopBits = Integer.parseInt(props.getProperty("lastStopBits"));
            log.info("Last StopBits: " + lastStopBits);
        }catch (NumberFormatException exception){
            this.lastStopBits = 0;
            log.info("configAccess.properties contain incorrect value of lastStopBits");
        }

        lastParity = props.getProperty("lastParity");
        if(lastParity == null){
            this.lastParity = "dunno";
            log.info("configAccess.properties contain incorrect value of lastParity");
        }else{
            this.lastParity = props.getProperty("lastParity");
            log.info("Last Parity: " + lastParity);
        }

        lastProtocol = props.getProperty("lastProtocol");
        if(lastProtocol == null){
            this.lastProtocol = "dunno";
            log.info("configAccess.properties contain incorrect value of lastProtocol");
        }else{
            this.lastProtocol = props.getProperty("lastProtocol");
            log.info("Last Protocol: " + lastProtocol);
        }
    }

    public void setLastComPort(String comPort){
        this.lastComPort = comPort;
        properties.setProperty("lastComPort", comPort);
        this.updateFile();
        log.info("Обновлено значение последнего ком-порта: " + comPort);
    }

    public void setLastDataBits(int dataBits){
        //System.out.println("Will save data bits" + dataBits);
        this.lastDataBits = dataBits;
        properties.setProperty("lastDataBits", String.valueOf(dataBits));
        this.updateFile();
        log.info("Обновлено значение последнего dataBits: " + String.valueOf(dataBits));
    }

    public void setLastStopBits(int lastStopBits){
        this.lastStopBits = lastStopBits;
        properties.setProperty("lastStopBits", String.valueOf(lastStopBits));
        this.updateFile();
        log.info("Обновлено значение последнего StopBits: " + String.valueOf(lastStopBits));
    }
    public void setLastComSpeed(int lastComSpeed){
        this.lastComSpeed = lastComSpeed;
        properties.setProperty("lastComSpeed", String.valueOf(lastComSpeed));
        this.updateFile();
        log.info("Обновлено значение последнего ComSpeed: " + String.valueOf(lastComSpeed));
    }

    public void setLastParity(String lastParity){
        this.lastParity = lastParity;
        properties.setProperty("lastParity", lastParity);
        this.updateFile();
        log.info("Обновлено значение последнего Parity: " + String.valueOf(lastParity));

    }

    public void setLastProtocol(String lastProtocol){
        this.lastProtocol = lastProtocol;
        properties.setProperty("lastProtocol", lastProtocol);
        this.updateFile();
        log.info("Обновлено значение последнего Protocol: " + String.valueOf(lastProtocol));

    }

    public void setLogLevel(org.apache.log4j.Level level){
        this.logLevel = String.valueOf(level);
        properties.setProperty("logLevel", String.valueOf(level));
        this.updateFile();
        log.info("Обновлено значение последнего logLevel: " + String.valueOf(logLevel));

    }

    public org.apache.log4j.Level getLogLevel(){
        log.error("Возвращено значение уровня логирования  " + logLevel);
        return Level.toLevel(this.logLevel);
    }
    private void updateFile(){
        try (OutputStream file = new FileOutputStream(this.settingFile.getAbsoluteFile())){
            this.properties.store(file, null);
        } catch (IOException e) {
            //throw new RuntimeException(e);
            log.error("Ошибка обновления файла настроек " + e.getMessage());

        }
    }



}

