package org.example.gui.devices.edvards.d39730880.util;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class EdwardsDevicesSearch extends JDialog {

    private static final Logger log = Logger.getLogger(EdwardsDevicesSearch.class);

    private static final int[] BAUDRATES = {9600, 19200, 38400};

    private final DefaultTableModel tableModel;
    private final JProgressBar overallProgress;
    private final JLabel progressLabel;
    private final JLabel statusLabel;
    private final JButton stopButton;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<FoundDevice> foundDevices = new CopyOnWriteArrayList<>();
    private volatile SerialPort currentPort;

    private final boolean scanAllPorts;
    private final boolean scanAllSpeeds;
    private final boolean scanAllAddresses;
    private final String selectedPort;
    private final int selectedBaud;

    public EdwardsDevicesSearch(Frame owner,
                                boolean scanAllPorts,
                                boolean scanAllSpeeds,
                                boolean scanAllAddresses,
                                String selectedPort,
                                int selectedBaud) {
        super(owner, "Поиск устройств Edwards TIC (D397)", true);
        this.scanAllPorts = scanAllPorts;
        this.scanAllSpeeds = scanAllSpeeds;
        this.scanAllAddresses = scanAllAddresses;
        this.selectedPort = selectedPort;
        this.selectedBaud = selectedBaud;

        tableModel = new DefaultTableModel(new String[]{"Порт", "Скорость (бод)", "Результат"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable resultTable = new JTable(tableModel);
        resultTable.setRowHeight(22);
        resultTable.getColumnModel().getColumn(2).setPreferredWidth(350);
        resultTable.setBackground(new Color(0, 0, 0));
        resultTable.setForeground(new Color(200, 200, 200));
        resultTable.setGridColor(new Color(60, 60, 60));
        resultTable.setSelectionBackground(new Color(60, 60, 60));
        resultTable.setSelectionForeground(new Color(220, 220, 220));
        resultTable.setShowGrid(true);
        resultTable.setIntercellSpacing(new Dimension(1, 1));

        overallProgress = new JProgressBar(0, 100);
        overallProgress.setStringPainted(true);

        progressLabel = new JLabel(" ");
        statusLabel = new JLabel(" ");
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 11));

        stopButton = new JButton("Остановить");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSearch());

        JPanel progressPanel = new JPanel(new BorderLayout(8, 2));
        progressPanel.add(progressLabel, BorderLayout.NORTH);
        progressPanel.add(overallProgress, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout(8, 2));
        bottomPanel.add(progressPanel, BorderLayout.CENTER);
        bottomPanel.add(stopButton, BorderLayout.EAST);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusLabel, BorderLayout.WEST);
        statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 4, 4, 4));

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.getViewport().setBackground(new Color(0, 0, 0));
        resultTable.getTableHeader().setBackground(new Color(40, 40, 40));
        resultTable.getTableHeader().setForeground(new Color(200, 200, 200));

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        mainPanel.add(statusPanel, BorderLayout.NORTH);
        add(mainPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopSearch();
            }
        });

        setSize(750, 450);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void startSearch() {
        running.set(true);
        stopButton.setEnabled(true);
        tableModel.setRowCount(0);
        foundDevices.clear();

        new SwingWorker<Void, Object[]>() {
            @Override
            protected Void doInBackground() throws Exception {
                performSearch();
                return null;
            }

            @Override
            protected void process(List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    tableModel.addRow(row);
                }
            }

            @Override
            protected void done() {
                stopButton.setEnabled(false);
                running.set(false);
                if (!foundDevices.isEmpty()) {
                    StringBuilder sb = new StringBuilder("<html><b>Найдено устройств: " + foundDevices.size() + "</b><br>");
                    for (FoundDevice fd : foundDevices) {
                        sb.append("Порт ").append(fd.port)
                                .append(", скорость ").append(fd.baud)
                                .append("<br>");
                    }
                    sb.append("</html>");
                    progressLabel.setText(sb.toString());
                } else {
                    progressLabel.setText("Устройств не найдено");
                }
                overallProgress.setValue(overallProgress.getMaximum());
                statusLabel.setText("Поиск завершён");
                closeCurrentPort();
            }
        }.execute();
    }

    private void stopSearch() {
        running.set(false);
        stopButton.setEnabled(false);
        progressLabel.setText("Поиск остановлен");
        statusLabel.setText("Поиск остановлен");
        closeCurrentPort();
    }

    private void closeCurrentPort() {
        SerialPort port = currentPort;
        if (port != null && port.isOpen()) {
            try {
                port.closePort();
            } catch (Exception ignored) {}
        }
        currentPort = null;
    }

    private void performSearch() {
        SerialPort[] allPorts = SerialPort.getCommPorts();
        ArrayList<SerialPort> portsToScan = new ArrayList<>();
        if (scanAllPorts) {
            for (SerialPort p : allPorts) portsToScan.add(p);
        } else {
            for (SerialPort p : allPorts) {
                if (p.getSystemPortName().equals(selectedPort)) {
                    portsToScan.add(p);
                    break;
                }
            }
            if (portsToScan.isEmpty() && selectedPort != null) {
                SerialPort sp = SerialPort.getCommPort(selectedPort);
                portsToScan.add(sp);
            }
        }

        int totalSteps = 0;
        for (SerialPort ignored : portsToScan) {
            int baudCount = scanAllSpeeds ? BAUDRATES.length : 1;
            totalSteps += baudCount;
        }
        overallProgress.setMaximum(totalSteps);
        overallProgress.setValue(0);
        int step = 0;

        byte[] probeCmd = "?V00902\r".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        for (SerialPort sp : portsToScan) {
            if (!running.get()) break;

            String portName = sp.getSystemPortName();
            int[] baudsToTry = scanAllSpeeds ? BAUDRATES : new int[]{selectedBaud};

            for (int baud : baudsToTry) {
                if (!running.get()) break;

                setStatus("Проверка порт=" + portName + " скорость=" + baud);
                log.info("Search: port=" + portName + " baud=" + baud);
                publishRow(portName, baud, "Открытие порта...");

                SerialPort testPort = SerialPort.getCommPort(portName);
                testPort.setBaudRate(baud);
                testPort.setNumDataBits(8);
                testPort.setNumStopBits(1);
                testPort.setParity(SerialPort.NO_PARITY);
                testPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
                testPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 50, 2000);

                if (!testPort.openPort()) {
                    publishRow(portName, baud, "Ошибка: порт не открыт");
                    updateProgress(++step);
                    continue;
                }

                currentPort = testPort;

                synchronized (testPort) {
                    int written = testPort.writeBytes(probeCmd, probeCmd.length);
                    if (written != probeCmd.length) {
                        publishRow(portName, baud, "Ошибка записи");
                        closeCurrentPort();
                        updateProgress(++step);
                        continue;
                    }
                }

                String reply = readResponse(testPort, baud);
                if (reply != null && (reply.contains("=V902") || reply.contains("*V902")
                        || reply.contains("=V904") || reply.contains("*V904"))) {
                    publishRow(portName, baud, "Найдено! " + reply.substring(0, Math.min(60, reply.length())));
                    foundDevices.add(new FoundDevice(portName, baud));
                    log.info("Edwards found: port=" + portName + " baud=" + baud);
                } else {
                    publishRow(portName, baud, "Нет ответа");
                }

                closeCurrentPort();
                updateProgress(++step);
            }
        }
    }

    private String readResponse(SerialPort port, int baud) {
        int totalWaitMs = Math.max(500, 2000);
        long deadline = System.currentTimeMillis() + totalWaitMs;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        while (System.currentTimeMillis() < deadline) {
            int read = port.readBytes(buffer, buffer.length);
            if (read > 0) {
                baos.write(buffer, 0, read);
                byte[] data = baos.toByteArray();
                if (data.length > 0 && data[data.length - 1] == '\r') {
                    break;
                }
            }
        }
        byte[] result = baos.toByteArray();
        if (result.length > 0) {
            String s = new String(result, java.nio.charset.StandardCharsets.US_ASCII).trim();
            return s.isEmpty() ? null : s;
        }
        return null;
    }

    private void publishRow(Object port, Object baud, Object status) {
        SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{port, baud, status})
        );
    }

    private void updateProgress(int value) {
        SwingUtilities.invokeLater(() -> {
            overallProgress.setValue(value);
            overallProgress.setString(value + " / " + overallProgress.getMaximum());
        });
    }

    private void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    public List<FoundDevice> getFoundDevices() {
        return foundDevices;
    }

    public static class FoundDevice {
        public final String port;
        public final int baud;

        FoundDevice(String port, int baud) {
            this.port = port;
            this.baud = baud;
        }
    }
}
