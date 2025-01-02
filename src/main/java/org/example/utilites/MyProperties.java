package org.example.utilites;


import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;
import org.example.services.ComPort;

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * The class responsible for getting the settings from the file
 * (what not to place in the code and on the git-hub)
 *
 * <p>Author: Andrew Kantser</p>
 * <p>Date: 2023-07-01</p>
 *
 */

public class MyProperties {
    private static Logger log = null;

    @Setter
    @Getter
    private String drv = "";

    @Setter
    @Getter
    private String url = "";

    @Setter
    @Getter
    private String pwd = "";

    @Setter
    @Getter
    private String usr = "";

    @Setter
    @Getter
    private String prt = "";

    private static String tabNumbersIdents = new String();
    private static String idents = new String();

    @Getter
    private String [] ports = new String[2];

    private String logLevel;

    @Getter
    private int tabCounter;

    @Getter
    private String [] commands = new String[2];

    @Getter
    private String [] prefixes = new String[2];

    @Getter
    private MainLeftPanelStateCollection leftPanelStateCollection = new MainLeftPanelStateCollection();

    private File settingFile = null;

    private final java.util.Properties properties;

    private final java.util.Properties propertiesIdentAssociation;

    public MyProperties() {
        //Thread.currentThread().setName("MyProperties");
        log = Logger.getLogger(MyProperties.class);
        log.debug("Загружаю файл configAccess.properties");
        try {
            Path configDir = Paths.get("config");
            Files.createDirectories(configDir);
            Path configFile = configDir.resolve("configAccess.properties");
            if (Files.exists(configFile)) {
                log.debug("Файл с настройками найден" + configFile.toAbsolutePath());
            } else {
                Files.createFile(configFile);
                log.warn("Создан новый файл с настройками" + configFile.toAbsolutePath());
            }
            this.settingFile = configFile.toFile();
        } catch (IOException e) {
            log.warn("Ошибка при работе с файлом настроек " + e.getMessage());
        }
        if(settingFile == null){
            //нужно прекратить выполнение программы если файл не был создан
            log.error("Ошибка при работе с файлом настроек ");
            System.err.println("Ошибка при создании файла настроек. Программа будет остановлена.");
            System.exit(1);
        }


        java.util.Properties props = new java.util.Properties();
        propertiesIdentAssociation = new java.util.Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(this.settingFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            System.out.println(e.getMessage());
        }

        /*
            try {
                propertiesIdentAssociation.load(in);
            }catch (IOException e) {
                System.out.println(e.getMessage());
            }
         */
        try {
            props.load(in);

        } catch (IOException e) {
            System.out.println(e.getMessage());
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
        this.logLevel = props.getProperty("logLevel");
        if (this.logLevel == null) {
            log.info("Уровень логирования сброшен на значение по умолчанию");
            this.logLevel = "WARN";
            this.updateFile();
        }
        Logger root = Logger.getRootLogger();
        Enumeration allLoggers = root.getLoggerRepository().getCurrentCategories();
        root.setLevel(Level.toLevel(this.logLevel));
        while (allLoggers.hasMoreElements()) {
            Category tmpLogger = (Category) allLoggers.nextElement();
            tmpLogger.setLevel(Level.toLevel(this.logLevel));
        }


        try {
            if (props.getProperty("usr") == null) {
                this.usr = "";
                log.warn("USR IS NULL");
            } else {
                this.usr = props.getProperty("usr");
                log.debug("USR найден успешно");

            }
        } catch (NumberFormatException exception) {
            log.debug("configAccess.properties содержит недопустимое значение параметра usr");
        }

        try {
            if (props.getProperty("pwd") == null) {
                this.pwd = "";
            } else {
                this.pwd = props.getProperty("pwd");
            }
        } catch (NumberFormatException exception) {
            log.info("configAccess.properties содержит недопустимое значение параметра pwd");
        }

        try {
            if (props.getProperty("prt") == null) {
                this.prt = "";
            } else {
                this.prt = props.getProperty("prt");
            }
        } catch (NumberFormatException exception) {
            log.info("configAccess.properties содержит недопустимое значение параметра prt");
        }

        try {
            if (props.getProperty("url") == null) {
                this.url = "";
            } else {
                this.url = props.getProperty("url");
            }
        } catch (NumberFormatException exception) {
            log.info("configAccess.properties содержит недопустимое значение параметра url");
        }

        try {
            if (props.getProperty("drv") == null) {
                this.drv = "";
            } else {
                this.drv = props.getProperty("drv");
            }
        } catch (NumberFormatException exception) {
            log.info("configAccess.properties содержит недопустимое значение параметра drv");
        }

        try {
            if (props.getProperty("commands") == null) {
                this.commands[0] = "";
            } else {
                this.commands = props.getProperty("commands").split(", ");
            }
        } catch (NumberFormatException exception) {
            log.info("configAccess.properties содержит недопустимое значение параметра commands");
        }

        try {
            if (props.getProperty("prefixes") == null) {
                this.prefixes[0] = "";
            } else {
                this.prefixes = props.getProperty("prefixes").split(", ");
            }
            log.debug("Last prefixes: " + Arrays.toString(prefixes));
        } catch (NumberFormatException exception) {
            log.info("configAccess.properties содержит недопустимое значение параметра lastPrefixes");
        }


        this.idents = properties.getProperty("idents");
        this.tabNumbersIdents = properties.getProperty("tabNumbersIdents");
        if(strSeemsLikeArray(idents) && strSeemsLikeArray(tabNumbersIdents)){
            String [] identsArr = idents.split(", ");
            String [] tabNumbersIdentsArr = tabNumbersIdents.split(", ");
            if(tabNumbersIdentsArr != null && identsArr != null){
                if(identsArr.length != tabNumbersIdentsArr.length){
                    log.warn("Разное количество аргументов identsArr и tabNumbersIdentsArr");
                }else{
                    boolean allOk = true;
                    for (String s : tabNumbersIdentsArr) {
                        try {
                            Integer.parseInt(s);
                        }catch (NumberFormatException e){
                            System.out.println(e.getMessage());
                            log.warn(e.getMessage());
                            allOk = false;
                        }
                    }
                    if(allOk){
                        for (int i = 0; i < identsArr.length; i++) {
                            AnswerStorage.registerDeviceTabPair(identsArr[i],
                                    Integer.parseInt(tabNumbersIdentsArr[i]));
                        }
                    }

                }
            }
        }


        try {
            this.tabCounter = Integer.parseInt(props.getProperty("tabCounter"));
            log.debug("Last TabCounter: " + tabCounter);
        } catch (NumberFormatException exception) {
            this.tabCounter = 1;
            log.info("configAccess.properties содержит недопустимое значение параметра tabCounter");
        }



        String[] baudRateCodeArray = new String[1];
        if (props.getProperty("baudRateCode") != null) {
            baudRateCodeArray = props.getProperty("baudRateCode").split(", ");
        }

        String[] stopBitsCodeArray = new String[1];
        if (props.getProperty("stopBitsCode") != null) {
            stopBitsCodeArray = props.getProperty("stopBitsCode").split(", ");
        }

        String[] protocolCodeArray = new String[1];
        if (props.getProperty("protocolCode") != null) {
            protocolCodeArray = props.getProperty("protocolCode").split(", ");
        }

        String[] dataBitsCodeArray = new String[1];
        if (props.getProperty("dataBitsCode") != null) {
            dataBitsCodeArray = props.getProperty("dataBitsCode").split(", ");
        }

        String[] parityBitCodeArray = new String[1];
        if (props.getProperty("parityBitCode") != null) {
            parityBitCodeArray = props.getProperty("parityBitCode").split(", ");
        }

        int maxElementCoutn = Math.max(baudRateCodeArray.length, stopBitsCodeArray.length);
        maxElementCoutn = Math.max(maxElementCoutn, protocolCodeArray.length);
        maxElementCoutn = Math.max(maxElementCoutn, dataBitsCodeArray.length);
        maxElementCoutn = Math.max(maxElementCoutn, parityBitCodeArray.length);
        int needElementCount = Math.min(maxElementCoutn, tabCounter);
        leftPanelStateCollection.getAllAsList().clear();
        while (leftPanelStateCollection.getAllAsList().size() < needElementCount){
            this.leftPanelStateCollection.addEntry();
        }
            for (int i = 0; i < needElementCount; i++) {
                if (baudRateCodeArray.length > i && baudRateCodeArray[i] != null && !baudRateCodeArray[i].isEmpty()) {
                    try {
                        leftPanelStateCollection.setBaudRate(i, Integer.parseInt(baudRateCodeArray[i]));
                    } catch (NumberFormatException exception) {
                        log.warn("Один из параметров BaudRate не был конвертирован корректно");
                    }
                }
            }

            for (int i = 0; i < needElementCount; i++) {
                if (stopBitsCodeArray.length > i && stopBitsCodeArray[i] != null && !stopBitsCodeArray[i].isEmpty()) {
                    try {
                        leftPanelStateCollection.setStopBits(i, Integer.parseInt(stopBitsCodeArray[i]));
                    } catch (NumberFormatException exception) {
                        log.warn("Один из параметров stopBitsCode не был конвертирован корректно");
                    }
                }
            }

            for (int i = 0; i < needElementCount; i++) {
                if (protocolCodeArray.length > i && protocolCodeArray[i] != null && !protocolCodeArray[i].isEmpty()) {
                    try {
                        leftPanelStateCollection.setProtocol(i, Integer.parseInt(protocolCodeArray[i]));
                    } catch (NumberFormatException exception) {
                        log.warn("Один из параметров protocolCodeArray не был конвертирован корректно");
                    }
                }
            }

            for (int i = 0; i < needElementCount; i++) {
                if (dataBitsCodeArray.length > i && dataBitsCodeArray[i] != null && !dataBitsCodeArray[i].isEmpty()) {
                    try {
                        leftPanelStateCollection.setDataBits(i, Integer.parseInt(dataBitsCodeArray[i]));
                    } catch (NumberFormatException exception) {
                        log.warn("Один из параметров dataBitsCodeArray не был конвертирован корректно");
                    }
                }
            }

            for (int i = 0; i < needElementCount; i++) {
                if (parityBitCodeArray.length > i && parityBitCodeArray[i] != null && !parityBitCodeArray[i].isEmpty()) {
                    try {
                        leftPanelStateCollection.setParityBits(i, Integer.parseInt(parityBitCodeArray[i]));
                    } catch (NumberFormatException exception) {
                        log.warn("Один из параметров parityBitCodeArray не был конвертирован корректно");
                    }
                }
            }



        if(ports != null){
            log.debug("Last ComPorts: " + Arrays.toString(ports));
            if(props.getProperty("ports") == null){
                this.ports [0] = "dunno";
            }else{
                this.ports = props.getProperty("ports").split(", ");
            }

        }else{
            log.info("configAccess.properties contain incorrect value of ports");
            this.ports [0] = "dunno";
        }
    }

    public void updateServerConf(){
        properties.setProperty("usr", usr);
        properties.setProperty("pwd", pwd);
        properties.setProperty("prt", prt);
        properties.setProperty("url", url);
        properties.setProperty("drv", drv);
    }
    public void setLastCommands(ArrayList <String> commands){
        int i = 0;
        StringBuilder sb = new StringBuilder();
        this.commands = new String[commands.size()];
        for (String command : commands) {
            this.commands [i] = command;
            sb.append(command);
            sb.append(", ");
            i++;
        }
        properties.setProperty("commands", sb.toString());
        this.updateFile();
        log.info("Обновлено значение последних команд: " + sb);
    }

    public void setLastPrefixes(ArrayList <String> prefixesInp){
        int i = 0;
        StringBuilder sb = new StringBuilder();
        this.prefixes = new String[prefixesInp.size()];
        for (String prefix : prefixesInp) {
            this.prefixes [i] = prefix;
            sb.append(prefix);
            sb.append(", ");
            i++;
        }
        properties.setProperty("prefixes", sb.toString());
        this.updateFile();
        log.info("Обновлено значение последних префиксов: " + sb);
    }

    public void setIdentAndTabBounding(HashMap<String, Integer> pairs){
        StringBuilder sbTabs = new StringBuilder();
        StringBuilder sbIdents = new StringBuilder();
        for (Map.Entry<String, Integer> stringIntegerEntry : pairs.entrySet()) {
            sbTabs.append(stringIntegerEntry.getValue()).append(", ");
            sbIdents.append(stringIntegerEntry.getKey()).append(", ");
        }
        properties.setProperty("tabNumbersIdents", sbTabs.toString());
        properties.setProperty("idents", sbIdents.toString());
        this.updateFile();
        log.debug("Обновлено значение ассоциаций идентификаторов и вкладок... ");
    }
    public void setLastLeftPanel(MainLeftPanelStateCollection leftPanStateInp){
        if(leftPanStateInp == null){
            throw new IllegalArgumentException("Переданный объект состояния левой панели не может быть null");
        }

        if(leftPanStateInp.getAllAsList().isEmpty()){
            throw new IllegalArgumentException("Переданный объект состояния левой панели должен содержать описание хотя бы одной вкладки");
        }

        this.leftPanelStateCollection = leftPanStateInp;
        //Что бы было удобно читать файл с настройками
        StringBuilder sbProtocol = new StringBuilder();
        StringBuilder sbProtocolCode = new StringBuilder();
        StringBuilder sbBaudRate = new StringBuilder();
        StringBuilder sbBaudRateCode = new StringBuilder();
        StringBuilder sbStopBits = new StringBuilder();
        StringBuilder sbStopBitsCode = new StringBuilder();
        StringBuilder sbDataBits = new StringBuilder();
        StringBuilder sbDataBitsCode = new StringBuilder();
        StringBuilder sbParityBit = new StringBuilder();
        StringBuilder sbParityBitCode = new StringBuilder();
        for (MainLeftPanelState mainLeftPanelState : leftPanStateInp.getAllAsList()) {
            sbProtocolCode.append(mainLeftPanelState.getProtocol());
            sbProtocolCode.append(", ");
            sbProtocol.append(ProtocolsList.getLikeArray(mainLeftPanelState.getProtocol()));
            sbProtocol.append(", ");

            sbBaudRateCode.append(mainLeftPanelState.getBaudRate());
            sbBaudRateCode.append(", ");
            sbBaudRate.append(BaudRatesList.getNameLikeArray(mainLeftPanelState.getBaudRate()));
            sbBaudRate.append(", ");

            sbStopBitsCode.append(mainLeftPanelState.getStopBits());
            sbStopBitsCode.append(", ");
            sbStopBits.append(StopBitsList.getNameLikeArray(mainLeftPanelState.getStopBits()));
            sbStopBits.append(", ");

            sbDataBitsCode.append(mainLeftPanelState.getDataBits());
            sbDataBitsCode.append(", ");
            sbDataBits.append(DataBitsList.getNameLikeArray(mainLeftPanelState.getDataBits()));
            sbDataBits.append(", ");

            sbParityBitCode.append(mainLeftPanelState.getParityBit());
            sbParityBitCode.append(", ");
            sbParityBit.append(ParityList.getNameLikeArray(mainLeftPanelState.getParityBit()));
            sbParityBit.append(", ");
        }
        properties.setProperty("protocol", sbProtocol.toString());
        properties.setProperty("protocolCode", sbProtocolCode.toString());
        properties.setProperty("baudRate", sbBaudRate.toString());
        properties.setProperty("baudRateCode", sbBaudRateCode.toString());
        properties.setProperty("stopBits", sbStopBits.toString());
        properties.setProperty("stopBitsCode", sbStopBitsCode.toString());
        properties.setProperty("dataBits", sbDataBits.toString());
        properties.setProperty("dataBitsCode", sbDataBitsCode.toString());
        properties.setProperty("parityBit", sbParityBit.toString());
        properties.setProperty("parityBitCode", sbParityBitCode.toString());
        this.updateFile();
        log.debug("Обновлено значение класса левой вклдаки... ");
    }

    public void setPortForTab(String portName, int tabNumber){
        //getCurrentComName()
        if(ports == null){
            ports = new String[2];
        }

        if(portName == null || portName.isEmpty()){
            log.warn("В метод сохранения ком-портов передано null название");
            return;
        }

        if(tabNumber < 0 || tabNumber > 999){
            log.warn("В метод сохранения ком-портов передано недопустимое значение номера вкладки");
            return;
        }

        if(ports.length < tabNumber){
            String[] newPorts = new String[tabNumber + 1];
            System.arraycopy(ports, 0, newPorts, 0, tabNumber);
            ports = newPorts;
            log.info("Произведено выравнивание длинны сохраняемого массива к количеству вкладок");
        }

        for (int i = 0; i < ports.length; i++) {
            if(ports[i] == null || ports[i].isEmpty()){
                ports[i] = "notOpen";
            }
        }

        ports[tabNumber] = portName;
        log.info("Для вкладки " + tabNumber + ". Обновлен ком порт на " + portName);

        StringBuilder stringBuilder = new StringBuilder();
        for (String port : ports) {
            stringBuilder.append(port);
            stringBuilder.append(", ");
        }
        properties.setProperty("ports", stringBuilder.toString());
        this.updateFile();
        log.debug("Обновлено значение последних портов по названиям: " + stringBuilder);
    }

    public void setLogLevel(org.apache.log4j.Level level){
        this.logLevel = String.valueOf(level);
        properties.setProperty("logLevel", String.valueOf(level));
        this.updateFile();
        log.debug("Обновлено значение последнего logLevel: " + logLevel);
    }

    public void setTabCounter(int counter){
        this.tabCounter = counter;
        properties.setProperty("tabCounter", String.valueOf(counter));
        this.updateFile();
        log.debug("Обновлено значение последнего tabCounter: " + logLevel);
    }


    public org.apache.log4j.Level getLogLevel(){
        log.error("Возвращено значение уровня логирования  " + logLevel);
        return Level.toLevel(this.logLevel);
    }

    private boolean strSeemsLikeArray(String str){
        if(str == null)
            return false;

        if(str.isEmpty())
            return false;

        if(!str.contains(", "))
            return false;

        return true;
    }


    public void updateFile(){
        try (OutputStream file = new FileOutputStream(this.settingFile.getAbsoluteFile())){
            //разбить на несколько файлов, тогда появятся разделы
            //properties.remove("idents");
            //properties.remove("tabCounter");
            this.properties.store(file, "General Settings");
            //this.propertiesIdentAssociation.store(file, "Association of identification signs and tabs (for redirection)");
        } catch (IOException e) {
            //throw new RuntimeException(e);
            log.error("Ошибка обновления файла настроек " + e.getMessage());

        }
    }



}

