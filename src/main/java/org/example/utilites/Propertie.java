package org.example.utilites;


import java.io.*;
import java.util.Properties;

/**
 * The class responsible for getting the settings from the file
 * (what not to place in the code and on the git-hub)
 *
 * <p>Author: Andrew Kantser</p>
 * <p>Date: 2023-07-01</p>
 *
 */

public class Propertie {
    private String lastComPort;


    public Propertie(){


        System.out.println("Start load configAcces.properties");
        try{
            File f = new File("conf"+"configAcces.properties");
            if(f.exists() && !f.isDirectory()) {
                // do something
            }else {
                new File("conf").mkdirs();
            }
        } catch (Exception e) {
            //throw new RuntimeException(e);
        }
        File myObj = null;
        try {
            myObj = new File("conf/"+"configAcces.properties");
            if (myObj.createNewFile()) {
                //System.out.println("File created: " + myObj.getName());
                System.out.println("File created: " + myObj.getAbsolutePath());
            } else {
                System.out.println("File already exists.");
                System.out.println(myObj.getAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("An error occurred.");
            //e.printStackTrace();
        }



        Properties props = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(myObj.getAbsolutePath());
        } catch (FileNotFoundException e) {
            //throw new RuntimeException(e);
        }
        try {
            props.load(in);
        } catch (IOException e) {
            //throw new RuntimeException(e);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                //throw new RuntimeException(e);
            }
        }

        // Получение значений из файла
        String lastComPortReadet = props.getProperty("lastComPort");
        System.out.println(lastComPortReadet);

        this.lastComPort = lastComPortReadet;
    }

    public String getLastComPort() {
        return lastComPort;
    }


}

