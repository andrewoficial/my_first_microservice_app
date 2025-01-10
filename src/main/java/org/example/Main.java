package org.example;

import org.example.gui.MainWindow;
import org.example.services.comPool.AnyPoolService;
import org.example.services.comPort.ComPort;
import org.example.utilites.properties.MyProperties;
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
        String confName = context.getEnvironment().getProperty("spring.config.name", "dunno");
        System.out.println("Параметр confName: " + confName);

        SwingUtilities.invokeLater(() -> {
            ComPort comPorts = context.getBean(ComPort.class);
            AnyPoolService anyPoolService = context.getBean(AnyPoolService.class);
            MyProperties myProperties = context.getBean(MyProperties.class);
            MainWindow mainWindow = new MainWindow(myProperties, comPorts, anyPoolService);

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



