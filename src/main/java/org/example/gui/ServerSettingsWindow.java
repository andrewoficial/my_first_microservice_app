package org.example.gui;

import org.example.utilites.MyProperties;
import org.example.utilites.SpringLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ServerSettingsWindow extends JDialog{
    private String driver;
    private JPanel serverParametersPanel;
    private JTextField IN_DbDriver;
    private JPasswordField IN_Pwd;
    private JButton saveButton;
    private JButton BT_StartServer;
    private JButton BT_StopServer;
    private JTextField IN_ServerPort;
    private JTextArea textArea1;
    private JTextField textField1;
    private JTextField textField2;
    private JButton addUserButton;
    private JTextField textField3;
    private JTextField IN_Login;
    private JTextField IN_Url;

    private final ConfigurableApplicationContext ctx;
    private final SpringApplication app;

    private String springPort;


    private String dbUrl;
    private String username;
    private String password;




    public ServerSettingsWindow() {
        this.app = SpringLoader.app;
        this.ctx = SpringLoader.ctx;
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(serverParametersPanel);


        BT_StartServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_StartServer");
                springPort = IN_ServerPort.getText();
                driver = IN_DbDriver.getText();
                username = IN_Login.getText();
                StringBuilder pwdInput = new StringBuilder();
                for (char c : IN_Pwd.getPassword()) {
                    pwdInput.append(c);
                }
                password = pwdInput.toString();
                dbUrl = IN_Url.getText();

                checkParameters();

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("server.port", springPort);
                app.setDefaultProperties(parameters);
                SpringLoader.ctx = app.run();

            }
        });

        BT_StopServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_StopServer");
                SpringLoader.ctx.close();
            }
        });
    }

    private void checkParameters(){
        if(springPort == null){
            springPort = "8080";
        } else if (! this.stringIsNumeric(springPort)) {
            springPort = "8080";
        }else if(springPort.length() > 5 || springPort.length() < 2){
            springPort = "8080";
        }

        if(driver == null){
            driver = "org.postgresql.Driver";
        }else if(driver.length() < 2 || driver.length() > 15){
            driver = "org.postgresql.Driver";
        }else{
            driver = driver.trim();
        }

        if(dbUrl == null){
            dbUrl = "jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk";
        }else if(dbUrl.length() < 2 || dbUrl.length() > 15){
            dbUrl = "jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk";
        }else{
            dbUrl = dbUrl.trim();
        }

        if(password == null){
            password = "EcrvEk0pw2UaY6jdKY16R3RGiBrefui1";
        }else if(password.length() < 2 || password.length() > 155){
            password = "EcrvEk0pw2UaY6jdKY16R3RGiBrefui1";
        }else{
            password = password.trim();
        }

        if(username == null){
            username = "zhsiszsk";
        }else if(username.length() < 2 || username.length() > 55){
            username = "zhsiszsk";
        }else{
            username = username.trim();
        }


        MyProperties.driver = this.driver;
        MyProperties.usr = this.username;
        MyProperties.pwd = this.password;
        MyProperties.url = this.dbUrl;
        MyProperties.prt = this.springPort;


    }

    private boolean stringIsNumeric(String str){
        if (str == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
