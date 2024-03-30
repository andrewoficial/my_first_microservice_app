package org.example;


import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.example.utilites.MyProperties;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class Main {
    public static ComPort comPorts = new ComPort();

    public static void main(String[] args) {


        MainWindow dialog = new MainWindow();

        dialog.pack();

        dialog.setVisible(true);

        System.exit(0);
    }
}