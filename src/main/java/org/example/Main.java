package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.gui.main.MainWindow;
import org.example.gui.ServerSettingsWindow;
import org.example.services.AnswerStorage;
import org.example.services.ConnectionSettingsService;
import org.example.services.PollingService;
import org.example.services.PortLifecycleService;
import org.example.services.TabService;
import org.example.services.connectionPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.GenericApplicationContext;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Properties;


/**
 * Component scan is intentionally narrow: {@code device} (~88) and most of {@code gui} (~180)
 * have no Spring stereotypes — scanning them only wastes startup time (ASM over .class files).
 * The two GUI beans live under {@code gui} and are registered via {@link Import}.
 */
@SpringBootApplication(scanBasePackages = {
        "org.example.services",
        "org.example.utilites.properties",
        "org.example.web"
})
@Import({
        MainLeftPanelStateCollection.class,
        ServerSettingsWindow.class
})
@Slf4j
public class Main {
    private static ConfigurableApplicationContext context;
    private static MainWindow mainWindow = null;
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false"); // Устанавливаем GUI-режим

        String startupProfile = readStartupProfile();
        log.info("Профиль запуска: {}", startupProfile);
        log.info("Component scan: org.example.services, utilites.properties, web (+ Import: MainLeftPanelStateCollection, ServerSettingsWindow)");

        // gui-only excludes (JPA/Hikari/Security/Web) are applied by GuiOnlyEnvironmentPostProcessor
        // — Spring Boot 4 package names + early Environment hook. Do not use Boot 2/3 class names
        // or builder.properties(String...) without "key=value" form; both silently fail.
        context = new SpringApplicationBuilder(Main.class)
                .profiles(startupProfile)
                .run(args);


//        try {
//            UIManager.setLookAndFeel(new FlatLightLaf());
//        } catch (UnsupportedLookAndFeelException e) {
//            log.info("Error while set FlatLightLaf");
//        }
        SwingUtilities.invokeLater(() -> {
            AnyPoolService anyPoolService = context.getBean(AnyPoolService.class);
            MyProperties myProperties = context.getBean(MyProperties.class);
            MainLeftPanelStateCollection leftPanelStateCollection = context.getBean(MainLeftPanelStateCollection.class);
            ConnectionSettingsService connectionSettingsService = context.getBean(ConnectionSettingsService.class);
            PortLifecycleService portLifecycleService = context.getBean(PortLifecycleService.class);
            PollingService pollingService = context.getBean(PollingService.class);
            TabService tabService = context.getBean(TabService.class);
            AnswerStorage answerStorage = context.getBean(AnswerStorage.class);
            mainWindow = new MainWindow(myProperties, anyPoolService, leftPanelStateCollection, connectionSettingsService, portLifecycleService, pollingService, tabService, answerStorage);
        });

    }

    public static void restart(String newProfile) {
        String savedPort = (mainWindow != null && context != null && context.isActive())
                ? context.getBean(MyProperties.class).getPrt() : "8080";
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
                        // 3. BFPP: снимаем auto-definitions (@Import/@Component/@Bean/@Service),
                        // затем регистрируем те же экземпляры (иначе два бина одного типа).
                        // destroyBean здесь не подходит — definition остаётся, @Import создаёт второй бин.
                        genericContext.addBeanFactoryPostProcessor(beanFactory -> {
                            // By type + known names (@Import uses FQCN; @Service/@Bean use short names)
                            removeBeanDefinitionsOfType(beanFactory, MyProperties.class);
                            removeBeanDefinitionsOfType(beanFactory, AnyPoolService.class);
                            removeBeanDefinitionsOfType(beanFactory, MainLeftPanelStateCollection.class);
                            removeBeanByNames(beanFactory,
                                    "myProperties",
                                    "anyPoolService",
                                    "mainLeftPanelStateCollection",
                                    MyProperties.class.getName(),
                                    AnyPoolService.class.getName(),
                                    MainLeftPanelStateCollection.class.getName());

                            if (myProperties != null) {
                                beanFactory.registerSingleton("myProperties", myProperties);
                            }
                            if (anyPoolService != null) {
                                beanFactory.registerSingleton("anyPoolService", anyPoolService);
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

        if (myProperties != null) {
            MyProperties.restoreInstance(myProperties);
        }

        System.out.println("Spring видит профили после run: "
                + String.join(", ", context.getEnvironment().getActiveProfiles()));

        // 6. Обновляем сервисы в UI
        if (mainWindow != null) {
            SwingUtilities.invokeLater(() -> mainWindow.updateServices(
                    context.getBean(MyProperties.class),
                    context.getBean(AnyPoolService.class),
                    context.getBean(MainLeftPanelStateCollection.class)
            ));
        }
    }

    private static String readStartupProfile() {
        String defaultProfile = "gui-only";
        try {
            Path configFile = Paths.get("config/configAccess.properties");
            if (Files.exists(configFile)) {
                Properties props = new Properties();
                try (FileInputStream in = new FileInputStream(configFile.toFile())) {
                    props.load(in);
                }
                String profile = props.getProperty("startupProfile", defaultProfile);
                if (profile != null && !profile.isBlank()) {
                    return profile.trim();
                }
            }
        } catch (IOException e) {
            log.warn("Не удалось прочитать configAccess.properties: {}", e.getMessage());
        }
        return defaultProfile;
    }

    /**
     * Removes bean definitions (and any early singletons) of the given type so a preserved
     * instance can be {@link ConfigurableListableBeanFactory#registerSingleton(String, Object)
     * registerSingleton}'d without {@code NoUniqueBeanDefinitionException}.
     * <p>
     * Needed because {@code @Import(MainLeftPanelStateCollection.class)} registers a definition
     * under the FQCN while restart used to register a second singleton under a short name.
     */
    private static void removeBeanDefinitionsOfType(ConfigurableListableBeanFactory beanFactory, Class<?> type) {
        String[] names = beanFactory.getBeanNamesForType(type, true, false);
        removeBeanByNames(beanFactory, names);
    }

    private static void removeBeanByNames(ConfigurableListableBeanFactory beanFactory, String... names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            if (beanFactory instanceof BeanDefinitionRegistry registry && registry.containsBeanDefinition(name)) {
                registry.removeBeanDefinition(name);
                log.info("restart: removed bean definition '{}'", name);
            }
            if (beanFactory instanceof DefaultSingletonBeanRegistry singletonRegistry
                    && singletonRegistry.containsSingleton(name)) {
                singletonRegistry.destroySingleton(name);
                log.info("restart: destroyed early singleton '{}'", name);
            }
        }
    }

}



