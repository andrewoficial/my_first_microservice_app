package org.example;

import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.MyProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import javax.swing.*;


@SpringBootApplication
public class Main {
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false"); // Устанавливаем GUI-режим

        context  =  new SpringApplicationBuilder(Main.class)
                .run(args);


        //Проверка чтения конфига
        Environment env = context.getEnvironment();
        String serverEnabled = env.getProperty("server.enabled", "true");
        String confName = env.getProperty("spring.config.name", "dunno");
        System.out.println("Параметр server.enabled: " + serverEnabled);
        System.out.println("Параметр confName: " + confName);


        SwingUtilities.invokeLater(() -> {
            MyProperties myProperties = context.getBean(MyProperties.class);
            ComPort comPorts = context.getBean(ComPort.class);
            MainWindow mainWindow = new MainWindow(myProperties, comPorts);
            mainWindow.pack();
            mainWindow.setVisible(true);
            //mainWindow.setVisible(true);
        });

    }

    public static void restart(String... newProfiles) {
        if (context != null) {
            context.close();
        }

        SpringApplication app = new SpringApplication(Main.class);
        app.setAdditionalProfiles(newProfiles);
        context = app.run();
    }
}



