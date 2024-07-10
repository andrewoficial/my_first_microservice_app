package org.example;


import org.apache.ignite.IgniteCache;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.cache.query.SqlFieldsQuery;

@SpringBootApplication
public class Main {
    public static ComPort comPorts = new ComPort();
    private static Logger log = null;
    public static void main(String[] args) {

//        IgniteConfiguration cfg = new IgniteConfiguration();
//        cfg.setClientMode(true);
//
//
//        try (Ignite ignite = Ignition.start(cfg)) {
//            // ваша логика работы с кэшем
//            System.out.println("OK");
//        }

//        try (Ignite ignite = Ignition.start(cfg)) {
//            // Конфигурация кэша
//            CacheConfiguration<Long, String> cacheCfg = new CacheConfiguration<>("myCache");
//            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
//
//            // Создание или получение кэша
//            ignite.getOrCreateCache(cacheCfg);
//
//            // Кэш операции
//            ignite.cache("myCache").put(1L, "Hello, Ignite!");
//            String value = (String) ignite.cache("myCache").get(1L);
//
//            System.out.println("Value: " + value);
//        }

        //System.exit(0);

        Thread.currentThread().setName("Elephant Monitor");
        log = Logger.getLogger(Main.class);
        log.info("Запуск программы...");
        log.info(Thread.currentThread().getName());

        String ver = "Dunno....";

        Manifest mf = null;
        log.info("Ищу файл META-INF/MANIFEST.MF для определения версии");
        try {
            mf = new Manifest(Main.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
        } catch (IOException e) {
            log.error("Файл META-INF/MANIFEST.MF не найден в ресурсах.");
        }



        if(mf != null){
            ver = mf.getMainAttributes().getValue("Implementation-Title") + mf.getMainAttributes().getValue("Implementation-Version");
            log.info("Установил имя заголовка программы" + ver);
        }

        URL resource = Main.class.getClassLoader().getResource("GUI_Images/Pic.png");
        MainWindow dialog = new MainWindow();
        dialog.setName(ver);
        dialog.setTitle(ver);
        if(resource != null){
            ImageIcon pic = new ImageIcon(resource);
            dialog.setIconImage(pic.getImage());
            log.info("Установка картинки");
        }
        dialog.pack();
        dialog.setVisible(true);
    }

    private static class RemoteTask implements IgniteRunnable {
        @IgniteInstanceResource
        Ignite ignite;

        @Override public void run() {
            System.out.println(">> Executing the compute task");

            System.out.println(
                    "   Node ID: " + ignite.cluster().localNode().id() + "\n" +
                            "   OS: " + System.getProperty("os.name") +
                            "   JRE: " + System.getProperty("java.runtime.name"));

            IgniteCache<Integer, String> cache = ignite.cache("myCache");

            System.out.println(">> " + cache.get(1) + " " + cache.get(2));
        }
    }

}

