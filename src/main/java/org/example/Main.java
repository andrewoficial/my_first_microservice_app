package org.example;

import org.example.gui.MainWindow;
import org.example.services.comPool.AnyPoolService;
import org.example.services.comPort.ComPort;
import org.example.utilites.properties.MyProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import javax.swing.*;
import java.util.Collections;


@SpringBootApplication
public class Main {
    private static ConfigurableApplicationContext context;
    private static MainWindow mainWindow = null;
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
            mainWindow = new MainWindow(myProperties, comPorts, anyPoolService);

            //mainWindow.setVisible(true);
        });

    }

    public static void restart(String newProfile) {
        String savedPort = (mainWindow != null) ? MyProperties.getInstance().getPrt() : "8080";
        System.out.println("Сохранённый порт: " + savedPort);
        if (context != null) {
            context.close();
            System.out.println("Закрыл контекст Spring");
        }
        System.out.println("Проверка контекста isActive: " + context.isActive());
        System.out.println("Будет выбран профиль: " + newProfile);
        System.out.println("Будет выбран порт: " + savedPort);

        context = new SpringApplicationBuilder(Main.class)
                .profiles(newProfile) // Устанавливаем профиль сразу
                .properties(Collections.singletonMap("server.port", savedPort)) // Устанавливаем порт
                .run();

        System.out.println("Spring видит профили после run: "
                + String.join(", ", context.getEnvironment().getActiveProfiles()));
        SpringApplication app = new SpringApplication(Main.class);

        // Переинициализируем сервисы, если окно уже существует
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> {
                MyProperties myProperties = context.getBean(MyProperties.class);
                ComPort comPorts = context.getBean(ComPort.class);
                AnyPoolService anyPoolService = context.getBean(AnyPoolService.class);


                // Передаём новые бины в уже созданное окно
                mainWindow.updateServices(myProperties, comPorts, anyPoolService);
            });
        }
    }
}



