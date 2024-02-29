package org.example;


public class Main {
    public static ComPort comPorts = new ComPort();

    public static void main(String[] args) {

        MainWindow dialog = new MainWindow();
        dialog.pack();

        dialog.setVisible(true);
        System.exit(0);
    }
}