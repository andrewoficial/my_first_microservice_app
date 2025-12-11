package org.example;

import org.example.gui.MainLeftPanelStateCollection;
import org.example.gui.MainWindow;
import org.example.services.connectionPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import javax.swing.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;


@SpringBootApplication
public class Main {
    private static ConfigurableApplicationContext context;
    private static MainWindow mainWindow = null;
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false"); // Устанавливаем GUI-режим

        context  =  new SpringApplicationBuilder(Main.class)
                .run(args);

        //Проверка чтения конфига
        //String confName = context.getEnvironment().getProperty("spring.config.name", "dunno");
        //System.out.println("Параметр confName: " + confName);
        SwingUtilities.invokeLater(() -> {
            AnyPoolService anyPoolService = context.getBean(AnyPoolService.class);
            MyProperties myProperties = context.getBean(MyProperties.class);
            MainLeftPanelStateCollection leftPanelStateCollection = context.getBean(MainLeftPanelStateCollection.class);
            mainWindow = new MainWindow(myProperties, anyPoolService, leftPanelStateCollection);
        });

    }

    public static void restart(String newProfile) {
        String savedPort = (mainWindow != null) ? MyProperties.getInstance().getPrt() : "8080";
        System.out.println("Сохранённый порт: " + savedPort);
        // 1. Сохраняем бины ДО закрытия старого контекста
        final MyProperties myProperties;
        final AnyPoolService anyPoolService;
        final MainLeftPanelStateCollection leftPanelStateCollection;

        if (context != null) {
            myProperties = context.getBean(MyProperties.class);
            anyPoolService = context.getBean(AnyPoolService.class);
            leftPanelStateCollection = context.getBean(MainLeftPanelStateCollection.class);
            context.close();
            System.out.println("Закрыл контекст Spring");
        } else {
            myProperties = null;
            anyPoolService = null;
            leftPanelStateCollection = null;
        }

        // 2. Создаём новый контекст с предварительной настройкой
        ConfigurableApplicationContext newContext = new SpringApplicationBuilder(Main.class)
                .profiles(newProfile)
                .properties(Collections.singletonMap("server.port", savedPort))
                .initializers(initializer -> {
                    if (initializer instanceof GenericApplicationContext genericContext) {
                        // 3. Используем BeanFactoryPostProcessor для безопасной модификации
                        genericContext.addBeanFactoryPostProcessor(beanFactory -> {
                            // Удаляем существующие определения
                            Stream.of(
                                    MyProperties.class,
                                    AnyPoolService.class,
                                    MainLeftPanelStateCollection.class
                            ).forEach(clazz -> {
                                String[] beanNames = beanFactory.getBeanNamesForType(clazz);
                                Arrays.stream(beanNames).forEach(beanFactory::destroyBean);
                            });

                            // 4. Регистрируем сохранённые экземпляры
                            if (myProperties != null) {
                                beanFactory.registerSingleton(
                                        "myProperties",
                                        myProperties
                                );
                            }
                            if (anyPoolService != null) {
                                beanFactory.registerSingleton(
                                        "anyPoolService",
                                        anyPoolService
                                );
                            }
                            if (leftPanelStateCollection != null) {
                                beanFactory.registerSingleton(
                                        "mainLeftPanelStateCollection",
                                        leftPanelStateCollection
                                );
                            }
                        });
                    }
                })
                .run();

        // 5. Обновляем ссылку на контекст только после успешного создания
        context = newContext;

        System.out.println("Spring видит профили после run: "
                + String.join(", ", context.getEnvironment().getActiveProfiles()));

        // 6. Обновляем сервисы в UI
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> {
                mainWindow.updateServices(
                        context.getBean(MyProperties.class),
                        context.getBean(AnyPoolService.class),
                        context.getBean(MainLeftPanelStateCollection.class)
                );
            });
        }
    }

    // Вспомогательный метод для удаления bean definitions (JDK17+ compatible)
    private static void removeBeanDefinitions(
            GenericApplicationContext context,
            List<Class<?>> beanClasses
    ) {
        beanClasses.forEach(clazz -> {
            String[] beanNames = context.getBeanNamesForType(clazz);
            for (String beanName : beanNames) {
                if (context.containsBeanDefinition(beanName)) {
                    context.removeBeanDefinition(beanName);
                    System.out.println("Removed bean definition: " + beanName);
                }
            }
        });
    }
}



