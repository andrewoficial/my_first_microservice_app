package org.example;


import org.example.gui.MainWindow;
import org.example.services.ComPort;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
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