package org.example;

import org.apache.log4j.Logger;
import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.MyProperties;
import org.example.utilites.ProgramUpdater;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.util.jar.Manifest;

@SpringBootApplication
public class Main {
    public static final MyProperties prop = new MyProperties();
    public static ComPort comPorts = new ComPort();
    public static String currentVersion = "1.7.10-Beta";
    public static String programName = "Elephant-Monitor";
    public static String programTitle = programName + " v" + currentVersion;
    public static MainWindow mainWindow;

    public static void main(String[] args) {
        Thread.currentThread().setName("Elephant Monitor");
        Logger log = Logger.getLogger(Main.class);
        log.debug("Запуск программы...");
        log.debug(Thread.currentThread().getName());



        Manifest mf = null;
        log.info("Ищу файл META-INF/MANIFEST.MF для определения версии");
        try {
            mf = new Manifest(Main.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
        } catch (IOException e) {
            log.error("Файл META-INF/MANIFEST.MF не найден в ресурсах.");
        }



        if(mf != null){
            if(mf.getMainAttributes().getValue("Implementation-Title") != null ){
                programName = mf.getMainAttributes().getValue("Implementation-Title");
                log.info("Установил имя заголовка программы" + programName);
            }else {
                log.info("Оставил имя заголовка программы" + programName);
            }
            if(mf.getMainAttributes().getValue("Implementation-Version") != null ){
                currentVersion = mf.getMainAttributes().getValue("Implementation-Version");
                log.info("Установил версию программы" + currentVersion);
            }else {
                log.info("Оставил версию программы" + currentVersion);
            }
            programTitle = programName + " v" + currentVersion;
            log.debug("Установил имя заголовка программы и версию" + programTitle);
        }


        //In new windows




        URL resource = Main.class.getClassLoader().getResource("GUI_Images/Pic.png");
        mainWindow = new MainWindow();
        mainWindow.setName(programTitle);
        mainWindow.setTitle(programTitle);
        if(resource != null){
            ImageIcon pic = new ImageIcon(resource);
            mainWindow.setIconImage(pic.getImage());
            log.debug("Установка картинки");
        }
        mainWindow.pack();
        mainWindow.setVisible(true);
    }


}

