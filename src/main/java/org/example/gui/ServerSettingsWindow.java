package org.example.gui;

import org.example.utilites.SpringLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


public class ServerSettingsWindow extends JDialog {
    private JPanel serverParametersPanel;
    private JTextField IN_DbDriver;
    private JPasswordField ecrvEk0pw2UaY6jdKY16R3RGiBrefui1PasswordField;
    private JButton saveButton;
    private JButton BT_StartServer;
    private JButton BT_StopServer;
    private JTextField IN_ServerPort;
    private JTextArea textArea1;
    private JTextField textField1;
    private JTextField textField2;
    private JButton addUserButton;
    private JTextField textField3;

    private final ConfigurableApplicationContext ctx;
    private final SpringApplication app;

    private String springPort;
    private String dbDriver;



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
                dbDriver = IN_DbDriver.getText();
                checkParameters();
                Map<String, Object> parameters = new HashMap<>();
                parameters.put("server.port", springPort);
                //parameters.put("server.sql.init.platform", "---"); //ToDo Does not work :((
                parameters.put("spring.datasource.url", "jdbc:postgresql://floppy.db.elephantsql.com:8888/z123k"); //ToDo Does not work :((
                app.setDefaultProperties(parameters);

                /*
                app.setDefaultProperties(Collections
                        .singletonMap("server.port", springPort)
                        .put("f", "g"));

                 */
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

        if(dbDriver == null){
            dbDriver = "postgres";
        }else if(dbDriver.length() < 2 || dbDriver.length() > 15){
            dbDriver = "postgres";
        }else{
            dbDriver = dbDriver.trim();
        }
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
