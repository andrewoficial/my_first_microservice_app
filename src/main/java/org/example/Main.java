package org.example;


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
import java.util.jar.Attributes;
import java.util.jar.Manifest;


@SpringBootApplication
public class Main {
    public static ComPort comPorts = new ComPort();
    private static final Logger log = Logger.getLogger(Main.class);
    public static void main(String[] args) {
        log.info("Запуск программы...");

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
}