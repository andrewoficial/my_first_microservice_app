package org.example;


import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.MyProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;


@SpringBootApplication
public class Main {
    public static ComPort comPorts = new ComPort();

    public static void main(String[] args) {
        String ver = "Dunno....";
        /*
        Manifest mf = null;
        try {
            mf = new Manifest(Main.class.getResourceAsStream("/META-INF/manifest.mf"));
        } catch (IOException e) {
            //throw new RuntimeException(e);
        }
        if(mf != null){
            ver = mf.getMainAttributes()
                    .get(Attributes.Name.IMPLEMENTATION_VERSION)
                    .getClass()
                    .toString();
        }
         */

        MainWindow dialog = new MainWindow();
        dialog.setModal(false);
        dialog.setName(ver);
        dialog.setTitle(ver);
        dialog.pack();
        //dialog.setDefaultCloseOperation(3);
        dialog.setVisible(true);

        if(! dialog.isDisplayable()){
            System.exit(0);
        }

    }
}