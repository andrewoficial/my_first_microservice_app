package org.example;


import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.Propertie;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

public class Main {
    public static ComPort comPorts = new ComPort();

    public static void main(String[] args) {

        Propertie prop = new Propertie();
        MainWindow dialog = new MainWindow();


        dialog.pack();

        dialog.setVisible(true);

        System.exit(0);
    }
}