package org.example.gui;

import org.example.utilites.MyProperties;

import javax.swing.*;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class DebugWindow extends JDialog {
    private JPanel mainField;
    private JTextArea textArea1;
    private JLabel MyLable;
    private JTextField textField1;

    private Runtime runtime = Runtime.getRuntime();

    private NumberFormat format = NumberFormat.getInstance();

    private long timerWinUpdate = System.currentTimeMillis();
    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime now = LocalDateTime.now();
    public DebugWindow() {
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainField);
        Thread.currentThread().setName("NARUTO JavaResourceMonitor");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();





    }

    public void startMonitor(){
        System.out.println("Run while");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        //while(!Thread.currentThread().isInterrupted()) {
            //if (System.currentTimeMillis() - timerWinUpdate > 500L) {
                timerWinUpdate = System.currentTimeMillis();
                StringBuilder sb = new StringBuilder();
                long maxMemory = runtime.maxMemory();
                long allocatedMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();

                sb.append("free memory: " + format.format(freeMemory / 1024) + "\n");
                sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
                sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
                sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");
                for (Thread thread : threadSet) {
                    sb.append(thread.getName());
                    sb.append("\n");
                }
                textArea1.setText(sb.toString());
            //} else {
                System.out.println("Start sleep");
                try {
                    Thread.sleep(300L);
                    //System.out.println("Sleep " + (Math.min((millisLimit / 3), 300L)) + " time limit is " + millisLimit);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            //}
        //}
    }
}
