package org.example.utilites;


import lombok.Getter;

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
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
    public static String driver = "org.postgresql.Driver";
    public static String url = "jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk";
    public static String pwd = "EcrvEk0pw2UaY6jdKY16R3RGiBrefui1";

    public static String usr = "zhsiszsk";
    public static String prt = "8080";
    @Getter
    private String lastComPort;

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


        System.out.println("Start load configAccess.properties");
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
                System.out.println("File created: " + someFile.getAbsolutePath());
            } else {
                System.out.println("File already exists.");
                System.out.println(someFile.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            //e.printStackTrace();
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

        try{
            this.lastComSpeed = Integer.parseInt(props.getProperty("lastComSpeed"));
            System.out.println("Last com-port" + lastComPort);
        }catch (NumberFormatException exception){
            this.lastComSpeed = 0;
            System.out.println("configAccess.properties contain incorrect value of lastComSpeed");
        }

        try{
            this.lastDataBits = Integer.parseInt(props.getProperty("lastDataBits"));
            System.out.println("Last DataBits" + lastDataBits);
        }catch (NumberFormatException exception){
            this.lastDataBits = 0;
            System.out.println("configAccess.properties contain incorrect value of lastDataBits");
        }

        try{
            this.lastStopBits = Integer.parseInt(props.getProperty("lastStopBits"));
            System.out.println("Last lastStopBits: " + lastStopBits);
        }catch (NumberFormatException exception){
            this.lastStopBits = 0;
            System.out.println("configAccess.properties contain incorrect value of lastStopBits");
        }

        lastParity = props.getProperty("lastParity");
        if(lastParity == null){
            this.lastParity = "dunno";
            System.out.println("configAccess.properties contain incorrect value of lastParity");
        }else{
            this.lastParity = props.getProperty("lastParity");
            System.out.println("Last Parity" + lastParity);
        }

        lastProtocol = props.getProperty("lastProtocol");
        if(lastProtocol == null){
            this.lastProtocol = "dunno";
            System.out.println("configAccess.properties contain incorrect value of lastProtocol");
        }else{
            this.lastProtocol = props.getProperty("lastProtocol");
            System.out.println("Last Protocol " + lastProtocol);
        }
    }

    public void setLastComPort(String comPort){
        this.lastComPort = comPort;
        properties.setProperty("lastComPort", comPort);
        this.updateFile();
    }

    public void setLastDataBits(int dataBits){
        //System.out.println("Will save data bits" + dataBits);
        this.lastDataBits = dataBits;
        properties.setProperty("lastDataBits", String.valueOf(dataBits));
        this.updateFile();
    }

    public void setLastStopBits(int lastStopBits){
        this.lastStopBits = lastStopBits;
        properties.setProperty("lastStopBits", String.valueOf(lastStopBits));
        this.updateFile();
    }
    public void setLastComSpeed(int lastComSpeed){
        this.lastComSpeed = lastComSpeed;
        properties.setProperty("lastComSpeed", String.valueOf(lastComSpeed));
        this.updateFile();
    }

    public void setLastParity(String lastParity){
        this.lastParity = lastParity;
        properties.setProperty("lastParity", lastParity);
        this.updateFile();
    }

    public void setLastProtocol(String lastProtocol){
        this.lastProtocol = lastProtocol;
        properties.setProperty("lastProtocol", lastProtocol);
        this.updateFile();
    }
    private void updateFile(){
        try (OutputStream file = new FileOutputStream(this.settingFile.getAbsoluteFile())){
            this.properties.store(file, null);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
    }


}

