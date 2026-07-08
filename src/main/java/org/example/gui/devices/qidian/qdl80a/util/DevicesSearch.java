package org.example.gui.devices.qidian.qdl80a.util;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import org.example.device.protQdl80a.Qdl80aCommandRegistry;

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

public class DevicesSearch extends JDialog {

    private static final Logger log = Logger.getLogger(DevicesSearch.class);

    private static final int[] BAUDRATES = {1200, 2400, 4800, 9600, 19200, 38400};
    private static final int MIN_ADDRESS = 1;
    private static final int MAX_ADDRESS = 247;

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

    public DevicesSearch(Frame owner,
                         boolean scanAllPorts,
                         boolean scanAllSpeeds,
                         boolean scanAllAddresses,
                         String selectedPort,
                         int selectedBaud) {
        super(owner, "Поиск устройств QDL80A", true);
        this.scanAllPorts = scanAllPorts;
        this.scanAllSpeeds = scanAllSpeeds;
        this.scanAllAddresses = scanAllAddresses;
        this.selectedPort = selectedPort;
        this.selectedBaud = selectedBaud;

        tableModel = new DefaultTableModel(new String[]{"Порт", "Скорость (бод)", "Адрес", "Результат"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        JTable resultTable = new JTable(tableModel);
        resultTable.setRowHeight(22);
        resultTable.getColumnModel().getColumn(3).setPreferredWidth(300);
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
                                .append(", адрес ").append(fd.address)
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
                log.info("Port closed after search");
            } catch (Exception ignored) {}
        }
        currentPort = null;
    }

    private void performSearch() {
        Qdl80aCommandRegistry registry = new Qdl80aCommandRegistry();

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

        for (SerialPort sp : portsToScan) {
            if (!running.get()) break;

            String portName = sp.getSystemPortName();
            int[] baudsToTry = scanAllSpeeds ? BAUDRATES : new int[]{selectedBaud};

            for (int baud : baudsToTry) {
                if (!running.get()) break;

                setStatus("Проверка порт=" + portName + " скорость=" + baud);
                log.info("Search: port=" + portName + " baud=" + baud + " opening port...");
                publishRow(portName, baud, "-", "Открытие порта...");

                SerialPort testPort = SerialPort.getCommPort(portName);
                testPort.setBaudRate(baud);
                testPort.setNumDataBits(8);
                testPort.setNumStopBits(1);
                testPort.setParity(SerialPort.NO_PARITY);
                testPort.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
                testPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 50, 2000);

                if (!testPort.openPort()) {
                    log.info("Search: port=" + portName + " baud=" + baud + " FAILED to open");
                    publishRow(portName, baud, "-", "Ошибка: порт не открыт");
                    updateProgress(++step);
                    continue;
                }

                log.info("Search: port=" + portName + " baud=" + baud + " port opened");
                currentPort = testPort;

                // Широковещательный запрос (адрес 0)
                log.info("Пробую широковещательный запрос");
                String bcResult = tryBroadcast(registry, testPort, portName, baud);
                log.info("Завершил попытку широковещательного запроса");
                // Перебор адресов (всегда, независимо от результата широковещания)
                int[] addrsToTry;
                if (scanAllAddresses) {
                    log.info("Перебор адресов запрошен из GUI");
                    addrsToTry = new int[MAX_ADDRESS - MIN_ADDRESS + 1];
                    for (int i = 0; i < addrsToTry.length; i++) {
                        addrsToTry[i] = MIN_ADDRESS + i;
                    }
                } else {
                    log.info("Перебор адресов не требуется согласно GUI");
                    addrsToTry = new int[]{1};
                }

                int foundCount = 0;
                for (int addr : addrsToTry) {
                    if (!running.get()) break;

                    byte[] request = registry.buildReadRequest((byte) addr, (byte) 0x03, (short) 0x0000, (short) 1);
                    setStatus("Проверка порт=" + portName + " скорость=" + baud + " адрес=" + addr);
//                    log.info("Search: port=" + portName + " baud=" + baud
//                            + " addr=" + addr + " cmd=" + bytesToHex(request));

                    synchronized (testPort) {
                        int written = testPort.writeBytes(request, request.length);
                        if (written != request.length) {
                            log.info("Search: port=" + portName + " baud=" + baud
                                    + " addr=" + addr + " write failed (wrote " + written + "/" + request.length + ")");
                            continue;
                        }
                    }

                    byte[] response = readResponse(testPort, baud);
                    if (response != null && response.length >= 5) {
                        if (Qdl80aCommandRegistry.validateCRC(response)) {
                            if ((response[1] & 0x80) == 0 && (response[1] == 0x03 || response[1] == 0x04)) {
                                int deviceAddr = response[0] & 0xFF;
                                int regValue = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
                                String detail = "Адрес: " + deviceAddr + " (reg0=" + regValue + ")";
                                publishRow(portName, baud, addr, "Найдено! " + detail);
                                foundDevices.add(new FoundDevice(portName, baud, deviceAddr));
                                foundCount++;
                                setStatus("Найдено устройство: порт=" + portName
                                        + " скорость=" + baud + " адрес=" + deviceAddr);
                                log.info("QDL80A found: port=" + portName + " baud=" + baud + " addr=" + deviceAddr);
                            }
                        }
                    }
                }

                if (foundCount == 0) {
                    String range = scanAllAddresses ? "1-247" : String.valueOf(addrsToTry[0]);
                    publishRow(portName, baud, range, "Устройств не найдено");
                    log.info("Search: port=" + portName + " baud=" + baud + " range=" + range + " no devices");
                }

                closeCurrentPort();
                updateProgress(++step);
            }
        }
    }

    private String tryBroadcast(Qdl80aCommandRegistry registry, SerialPort port,
                                String portName, int baud) {
        byte[] request = registry.buildReadRequest((byte) 0, (byte) 0x03, (short) 0x0000, (short) 1);
        setStatus("Широковещательный запрос порт=" + portName + " скорость=" + baud);
        log.info("Search: broadcast port=" + portName + " baud=" + baud + " cmd=" + bytesToHex(request));
        publishRow(portName, baud, "BC", "Широковещательный запрос...");

        synchronized (port) {
            int written = port.writeBytes(request, request.length);
            if (written != request.length) {
                log.info("Search: broadcast port=" + portName + " baud=" + baud
                        + " write failed (wrote " + written + "/" + request.length + ")");
                publishRow(portName, baud, "BC", "Ошибка записи");
                return "Ошибка записи";
            }
        }

        byte[] response = readResponse(port, baud);
        log.info("Чтение ответа");
        if (response != null && response.length >= 5) {
            log.info("В буфере порта найдены данные");
            if (Qdl80aCommandRegistry.validateCRC(response)) {
                log.info("Контрольная сумма корректная");
                if ((response[1] & 0x80) == 0 && (response[1] == 0x03 || response[1] == 0x04)) {
                    int deviceAddr = response[0] & 0xFF;
                    int regValue = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
                    String detail = "Ответ от адреса " + deviceAddr + " (reg0=" + regValue + ")";
                    publishRow(portName, baud, "BC", detail);
                    log.info("Search: broadcast response port=" + portName + " baud=" + baud + " addr=" + deviceAddr);
                    return detail;
                }else{
                    log.info("Ошибка проверки маркеров в ответе");
                }
            }else{
                log.info("Контрольная сумма неверна");
            }
        }else{
            log.info("В буфере порта нет данных");
        }
        publishRow(portName, baud, "BC", "Нет ответа");
        log.info("Search: broadcast no response port=" + portName + " baud=" + baud);
        return "Нет ответа";
    }

    private byte[] readResponse(SerialPort port, int baud) {
        int totalWaitMs = Math.max(300, baud <= 9600 ? (9600 / baud) * 300 : 200);
        long deadline = System.currentTimeMillis() + totalWaitMs;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int idleStreak = 0;
        while (System.currentTimeMillis() < deadline) {
            int read = port.readBytes(buffer, buffer.length);
            if (read > 0) {
                baos.write(buffer, 0, read);
                idleStreak = 0;
            } else {
                if (++idleStreak >= 2) {
                    break;
                }
            }
        }
        byte[] result = baos.toByteArray();
        return result.length > 0 ? result : null;
    }

    private void publishRow(Object port, Object baud, Object addr, Object status) {
        SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{port, baud, addr, status})
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

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public List<FoundDevice> getFoundDevices() {
        return foundDevices;
    }

    public static class FoundDevice {
        public final String port;
        public final int baud;
        public final int address;

        FoundDevice(String port, int baud, int address) {
            this.port = port;
            this.baud = baud;
            this.address = address;
        }
    }
}
