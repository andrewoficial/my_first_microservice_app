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
    private String gitLogin;
    private String gitPass;
    private String gitAdress;
    private String tgToken;
    private String tgName;

    public Propertie(){
        this.gitPass = null;
        this.gitLogin = null;

        Propertie properties = new Propertie();
        InputStream  inputStream = null;
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

            /*
            ClassLoader classLoader = getClass().getClassLoader();
            InputStream  inputStream2 = classLoader.getResourceAsStream(myObj);
            InputStream inputStream2 = new FileInputStream(myObj.getAbsolutePath());
            if (inputStream2 != null) {

                inputStream = inputStream2;
            } else {
                System.out.println("Resource configAcces.properties not found");
                inputStream = new FileInputStream("src/main/resources/configAcces.properties");
            }
            */






        // Получение значений логина и пароля из файла
        String username = props.getProperty("username");
        String password = props.getProperty("password");
        String addressGit = props.getProperty("addressGit");
        String tgToken = props.getProperty("tgtoken");
        String tgName = props.getProperty("tgname");
        System.out.println(username);
        System.out.println(password.substring(0, 3));
        System.out.println(addressGit);
        System.out.println(tgToken.substring(0,3));
        System.out.println(tgName);


        this.gitLogin = username;
        this.gitPass = password;
        this.gitAdress = addressGit;
        this.tgToken = tgToken;
        this.tgName = tgName;
    }

    public String getTgToken() {
        return tgToken;
    }

    public String getTgName() {
        return tgName;
    }

    public String getGitLogin() {
        return gitLogin;
    }

    public String getGitPass() {
        return gitPass;
    }

    public String getGitAdress() {
        return gitAdress;
    }
}

