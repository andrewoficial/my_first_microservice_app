package org.example;

import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.MyProperties;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import javax.swing.*;


@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false"); // Устанавливаем GUI-режим

        ConfigurableApplicationContext ctx =  new SpringApplicationBuilder(Main.class)
                .run(args);


        //Проверка чтения конфига
        Environment env = ctx.getEnvironment();
        String serverEnabled = env.getProperty("server.enabled", "true");
        String confName = env.getProperty("spring.config.name", "dunno");
        System.out.println("Параметр server.enabled: " + serverEnabled);
        System.out.println("Параметр confName: " + confName);


        SwingUtilities.invokeLater(() -> {
            MyProperties myProperties = ctx.getBean(MyProperties.class);
            ComPort comPorts = ctx.getBean(ComPort.class);
            MainWindow mainWindow = new MainWindow(myProperties, comPorts);
            mainWindow.pack();
            mainWindow.setVisible(true);
            //mainWindow.setVisible(true);
        });

    }
}



