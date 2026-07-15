package org.example.gui.system.resources;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.sun.management.OperatingSystemMXBean;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.Rendeble;

@Slf4j
public class DebugWindow extends JFrame implements Rendeble {
    private static DebugWindow instance;

    private int countRender = 0;
    private JPanel mainField;
    private JPanel mainContainer;
    private JLabel memoryLabel;
    private JProgressBar memoryProgress;
    private JLabel cpuLabel;
    private JProgressBar cpuProgress;
    private JScrollPane scrollPaneForTable;
    private JPanel containerForTable;
    private JTable threadsInfoTable;

    private JTextArea systemInfoTextArea;

    private DefaultTableModel threadTableModel;
    private String systemInfoSnapshot = "";
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
        super();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        add(mainField);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                instance = null;
                log.info("Debug window closed, instance cleared");
            }
        });

        threadTableModel = new DefaultTableModel(new String[]{"Thread", "State"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        threadsInfoTable.setModel(threadTableModel);
        threadsInfoTable.setBackground(Color.DARK_GRAY);
        threadsInfoTable.setForeground(Color.WHITE);
        threadsInfoTable.setGridColor(Color.DARK_GRAY);
        threadsInfoTable.getTableHeader().setBackground(Color.DARK_GRAY);
        threadsInfoTable.getTableHeader().setForeground(Color.WHITE);

        String userDir = System.getProperty("user.dir");
        String jarPath = "N/A";
        try {
            Object loc = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (loc != null) jarPath = loc.toString();
        } catch (Exception ignored) {
        }

        String logsSize = "N/A";
        File logsDir = new File(userDir, "logs");
        if (logsDir.isDirectory()) {
            logsSize = formatSize(dirSize(logsDir));
        }

        File appDrive = new File(userDir);
        long freeBytes = appDrive.getFreeSpace();

        String screenInfo = "N/A";
        try {
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            int w = gd.getDisplayMode().getWidth();
            int h = gd.getDisplayMode().getHeight();
            double scaleX = gd.getDefaultConfiguration().getDefaultTransform().getScaleX();
            int dpi = (int) Math.round(96 * scaleX);
            int scalePercent = (int) Math.round(scaleX * 100);
            screenInfo = w + "x" + h + "  DPI=" + dpi + "  Scale=" + scalePercent + "%";
        } catch (Exception ignored) {
        }

        systemInfoTextArea.setText("");
        systemInfoTextArea.append("User:        " + System.getProperty("user.name"));
        systemInfoTextArea.append("\nOS:          " + System.getProperty("os.name") + " " + System.getProperty("os.arch"));
        systemInfoTextArea.append("\nJava:        " + System.getProperty("java.version"));
        systemInfoTextArea.append("\nJRE:         " + System.getProperty("java.home"));
        systemInfoTextArea.append("\nProcessors:  " + Runtime.getRuntime().availableProcessors());
        systemInfoTextArea.append("\nScreen:      " + screenInfo);
        systemInfoTextArea.append("\nUser dir:    " + userDir);
        systemInfoTextArea.append("\nJAR:         " + jarPath);
        systemInfoTextArea.append("\nFree disk:   " + formatSize(freeBytes));
        systemInfoTextArea.append("\nLogs size:   " + logsSize);

        systemInfoSnapshot = systemInfoTextArea.getText();

        log.info("Открыто окно с информацией о системе");
    }

    public static DebugWindow getInstance() {
        if (instance == null || !instance.isDisplayable()) {
            instance = new DebugWindow();
        }
        return instance;
    }

    private static long dirSize(File dir) {
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    size += f.length();
                } else if (f.isDirectory()) {
                    size += dirSize(f);
                }
            }
        }
        return size;
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format("%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format("%.1f MB", mb);
        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
    }


    public void startMonitor() {
        updateData();
    }

    private void updateData() {
        log.trace("Обновление данных в окне с информацией о системе");
        threadSet = Thread.getAllStackTraces().keySet();
        maxMemory = runtime.maxMemory();
        allocatedMemory = runtime.totalMemory();
        freeMemory = runtime.freeMemory();
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        if (osBean != null) {
            startSystemAverage = osBean.getProcessCpuLoad() * 100;
        }
    }

    public void renderData() {
        updateData();
        StringBuilder sb = new StringBuilder();

        long usedMemory = allocatedMemory - freeMemory;
        sb.append("RAM Used:     ").append(format.format(usedMemory / 1024)).append(" KB\n");
        sb.append("RAM Allocated:").append(format.format(allocatedMemory / 1024)).append(" KB\n");
        sb.append("RAM Max:      ").append(format.format(maxMemory / 1024)).append(" KB\n");
        sb.append("RAM Free:     ").append(format.format(freeMemory / 1024)).append(" KB\n");

        sb.append("CPU: ").append(String.format("%.1f", startSystemAverage)).append("%\n");

        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        long sec = uptimeMs / 1000;
        sb.append("Uptime: ").append(sec / 3600).append("h ")
                .append((sec % 3600) / 60).append("m ")
                .append(sec % 60).append("s\n");

        ThreadMXBean tBean = ManagementFactory.getThreadMXBean();
        sb.append("Threads: ").append(tBean.getThreadCount())
                .append(" (peak: ").append(tBean.getPeakThreadCount())
                .append(", daemon: ").append(tBean.getDaemonThreadCount()).append(")\n");

        sb.append("GC pools: ").append(ManagementFactory.getGarbageCollectorMXBeans().size()).append("\n");

        systemInfoTextArea.setText(systemInfoSnapshot + "\n" + sb.toString());

        memoryProgress.setMaximum(Math.toIntExact(maxMemory / 1024L));
        memoryProgress.setMinimum(0);
        memoryProgress.setValue(Math.toIntExact(allocatedMemory / 1024L));
        memoryLabel.setText(String.format("Память: %d / %d KB", allocatedMemory / 1024, maxMemory / 1024));

        cpuProgress.setMinimum(0);
        cpuProgress.setMaximum(100);
        cpuProgress.setValue((int) Math.max(0, Math.min(100, startSystemAverage)));
        cpuLabel.setText(String.format("Процессор: %.1f%%", startSystemAverage));

        threadTableModel.setRowCount(0);
        for (Thread thread : threadSet) {
            threadTableModel.addRow(new Object[]{thread.getName(), thread.getState().name()});
        }

        countRender++;
        if (countRender > 20) {
            System.gc();
        }
    }

    @Override
    public boolean isEnable() {
        return this.isShowing();
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainField = new JPanel();
        mainField.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainField.setOpaque(true);
        mainField.setPreferredSize(new Dimension(450, 450));
        mainContainer = new JPanel();
        mainContainer.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainField.add(mainContainer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(450, 450), new Dimension(450, 450), new Dimension(500, -1), 0, false));
        memoryProgress = new JProgressBar();
        mainContainer.add(memoryProgress, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cpuProgress = new JProgressBar();
        mainContainer.add(cpuProgress, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        cpuLabel = new JLabel();
        cpuLabel.setText("Процессор");
        mainContainer.add(cpuLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        memoryLabel = new JLabel();
        memoryLabel.setText("Память");
        mainContainer.add(memoryLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        scrollPaneForTable = new JScrollPane();
        mainContainer.add(scrollPaneForTable, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        containerForTable = new JPanel();
        containerForTable.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        scrollPaneForTable.setViewportView(containerForTable);
        systemInfoTextArea = new JTextArea();
        containerForTable.add(systemInfoTextArea, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        threadsInfoTable = new JTable();
        containerForTable.add(threadsInfoTable, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainField;
    }

}
