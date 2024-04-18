package org.example;


import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.MyProperties;
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

    public static void main(String[] args) {
        String ver = "Dunno....";

        Manifest mf = null;

        try {
            mf = new Manifest(Main.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF"));
        } catch (IOException e) {
            System.out.println("File not found");
            //throw new RuntimeException(e);
        }



        if(mf != null){
            ver = mf.getMainAttributes().getValue("Implementation-Title") + mf.getMainAttributes().getValue("Implementation-Version");
            //System.out.println(mf.getMainAttributes().getValue("Implementation-Version"));
            //ver = mf.
/*
            ver = mf.getMainAttributes()
                    .get(Attributes.Name.IMPLEMENTATION_VERSION)
                    .getClass()
                    .toString();

 */


        }

        URL resource = Main.class.getClassLoader().getResource("GUI_Images/Pic.png");
        MainWindow dialog = new MainWindow();
        dialog.setName(ver);
        dialog.setTitle(ver);
        if(resource != null){
            ImageIcon pic = new ImageIcon(resource);
            dialog.setIconImage(pic.getImage());
        }
        dialog.pack();
        dialog.setVisible(true);


    }
}