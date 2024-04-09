package org.example.gui;

import javax.swing.*;

import java.lang.management.ManagementFactory;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import com.sun.management.OperatingSystemMXBean;

public class DebugWindow extends JDialog implements Rendeble{
    private int countRender = 0;
    private JPanel mainField;
    private JTextArea textArea1;
    private JProgressBar PB_Memory;
    private JProgressBar PB_Cpu;
    private JLabel LB_Cpu;
    private JLabel LB_Memory;
    private JLabel MyLable;
    private JTextField textField1;

    private Runtime runtime = Runtime.getRuntime();

    private NumberFormat format = NumberFormat.getInstance();

    private long timerWinUpdate = System.currentTimeMillis();
    private DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private LocalDateTime now = LocalDateTime.now();

    private long maxMemory = runtime.maxMemory();
    private long allocatedMemory = runtime.totalMemory();
    private long freeMemory = runtime.freeMemory();

    private double startSystemAverage;

    private Set<Thread> threadSet;

    public DebugWindow() {
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainField);
        Thread.currentThread().setName("NARUTO JavaResourceMonitor");
        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();





    }

    public void startMonitor(){
                updateData();
    }

    private void updateData(){
        threadSet = Thread.getAllStackTraces().keySet();
        maxMemory = runtime.maxMemory();
        allocatedMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        if (osBean != null) {
            startSystemAverage = osBean.getProcessCpuLoad() * 100;
        }
    }

    public void renderData(){
        updateData();
        StringBuilder sb = new StringBuilder();
        sb.append("free memory: " + format.format(freeMemory / 1024) + "\n");
        sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "\n");
        sb.append("max memory: " + format.format(maxMemory / 1024) + "\n");
        sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "\n");
        PB_Memory.setMaximum(Math.toIntExact(maxMemory / 1024L));
        PB_Memory.setMinimum(0);
        PB_Memory.setValue(Math.toIntExact(allocatedMemory / 1024L));

        PB_Memory.setMinimum(0);
        PB_Memory.setMinimum(100);
        PB_Cpu.setValue((int) startSystemAverage);
        System.out.println(startSystemAverage);
        for (Thread thread : threadSet) {
            sb.append("\t");
            sb.append(thread.getName());
            sb.append("\n");
        }
        textArea1.setText(sb.toString());
        countRender++;
        if(countRender > 20){
            System.gc(); //Runtime.getRuntime().gc();
        }

    }
}
