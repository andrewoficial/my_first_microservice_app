package org.example.gui.devices.erstevak;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Поиск сетевых адресов Erstevak MTP4D (протокол Thyracont RS485 V1, ASCII).
 * <p>
 * Перебирает адреса 1..247, отправляя {@code <NNN>M^<CR>} и проверяя корректный ответ
 * {@code <NNN>M<6 цифр><checksum><CR>} (адрес, команда, checksum). У прибора одно значение —
 * давление, оно и показывается в таблице.
 */
@Slf4j
public class ErstevakSearchDialog extends JDialog {

    private static final int MIN_ADDRESS = 1;
    private static final int MAX_ADDRESS = 247;
    private static final int ADDRESS_DIGITS = 3;
    private static final String QUERY_COMMAND = "M^";
    private static final char RESPONSE_COMMAND = 'M';

    private final String portName;
    private final int baud;
    private final int dataBits;
    private final int stopBits;
    private final int parity;

    private final DefaultTableModel tableModel;
    private final JProgressBar overallProgress;
    private final JLabel progressLabel;
    private final JLabel statusLabel;
    private final JButton stopButton;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<FoundDevice> foundDevices = new CopyOnWriteArrayList<>();
    private volatile SerialPort currentPort;

    public ErstevakSearchDialog(Frame owner, String portName, int baud,
                                int dataBits, int stopBits, int parity) {
        super(owner, "Поиск сетевых адресов Erstevak MTP4D", true);
        this.portName = portName;
        this.baud = baud;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;

        tableModel = new DefaultTableModel(
                new String[]{"Порт", "Скорость (бод)", "Адрес", "Давление, mbar", "Результат"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable resultTable = new JTable(tableModel);
        resultTable.setRowHeight(22);
        resultTable.getColumnModel().getColumn(4).setPreferredWidth(220);
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

        setSize(780, 450);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void startSearch() {
        running.set(true);
        stopButton.setEnabled(true);
        tableModel.setRowCount(0);
        foundDevices.clear();

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                performSearch();
                return null;
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
                                .append(", адрес ").append(fd.formattedAddress())
                                .append("<br>");
                    }
                    sb.append("</html>");
                    progressLabel.setText(sb.toString());
                } else {
                    progressLabel.setText("Устройств не найдено");
                }
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
            } catch (Exception ignored) {
            }
        }
        currentPort = null;
    }

    private void performSearch() {
        overallProgress.setMaximum(MAX_ADDRESS - MIN_ADDRESS + 1);
        overallProgress.setValue(0);

        SerialPort sp = SerialPort.getCommPort(portName);
        sp.setBaudRate(baud);
        sp.setNumDataBits(dataBits);
        sp.setNumStopBits(stopBits);
        sp.setParity(parity);
        sp.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 80, 300);

        if (!sp.openPort()) {
            String msg = "Ошибка: порт " + portName + " не открыт (занят другой программой/потоком?)";
            publishRow(portName, baud, "-", "-", msg);
            setStatus(msg);
            updateProgress(overallProgress.getMaximum());
            return;
        }
        currentPort = sp;

        int step = 0;
        for (int addr = MIN_ADDRESS; addr <= MAX_ADDRESS; addr++) {
            if (!running.get()) {
                break;
            }
            setStatus("Проверка порт=" + portName + " скорость=" + baud + " адрес=" + formatted(addr));

            byte[] request = buildProbe(addr);
            int written;
            synchronized (sp) {
                written = sp.writeBytes(request, request.length);
            }
            if (written != request.length) {
                updateProgress(++step);
                continue;
            }

            byte[] response = readResponse(sp);
            if (isMatch(response, addr)) {
                Double pressure = parsePressure(response);
                String value = pressure != null ? String.format("%.4g", pressure) : "?";
                publishRow(portName, baud, formatted(addr), value, "Найдено!");
                foundDevices.add(new FoundDevice(portName, baud, addr, pressure));
                log.info("Erstevak found: port={} baud={} addr={} value={}", portName, baud, addr, value);
                setStatus("Найдено устройство: порт=" + portName + " адрес=" + formatted(addr) + " (" + value + " mbar)");
            }
            updateProgress(++step);
        }

        closeCurrentPort();
    }

    private byte[] buildProbe(int address) {
        String request = formatted(address) + QUERY_COMMAND + "\r";
        return request.getBytes(StandardCharsets.US_ASCII);
    }

    /**
     * Проверяет ответ: {@code <NNN>M<6 цифр><checksum>} с совпадающим адресом и checksum.
     */
    private boolean isMatch(byte[] response, int address) {
        if (response == null || response.length == 0) {
            return false;
        }
        String s = new String(response, StandardCharsets.US_ASCII);
        int end = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\r' || c == '\n') {
                end = i;
                break;
            }
        }
        int payloadEnd = ADDRESS_DIGITS + 1 + 6; // адрес + команда + 6 цифр
        if (end < payloadEnd + 1) {
            return false;
        }
        if (!s.startsWith(formatted(address))) {
            return false;
        }
        if (s.charAt(ADDRESS_DIGITS) != RESPONSE_COMMAND) {
            return false;
        }
        String digits = s.substring(ADDRESS_DIGITS + 1, payloadEnd);
        if (!digits.matches("\\d{6}")) {
            return false;
        }
        int checksum = s.charAt(payloadEnd);
        return checksum == calcChecksum(s, 0, payloadEnd);
    }

    private Double parsePressure(byte[] response) {
        String s = new String(response, StandardCharsets.US_ASCII);
        int cmdPos = s.indexOf(RESPONSE_COMMAND);
        if (cmdPos < 0 || s.length() < cmdPos + 7) {
            return null;
        }
        try {
            int mantissa = Integer.parseInt(s.substring(cmdPos + 1, cmdPos + 6));
            int degree = Integer.parseInt(s.substring(cmdPos + 6, cmdPos + 7));
            return mantissa * Math.pow(10, degree) / 10000.0;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** checksum протокола Thyracont/Erstevak: sum(bytes) % 64 + 64. */
    private static int calcChecksum(String s, int offset, int length) {
        int sum = 0;
        for (int i = offset; i < offset + length; i++) {
            sum += s.charAt(i) & 0xFF;
        }
        return sum % 64 + 64;
    }

    private byte[] readResponse(SerialPort port) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[256];
        int idleStreak = 0;
        // Ответ короткий (~12 байт) и приходит быстро; ждём до ~200 мс или двух пауз.
        while (idleStreak < 2) {
            int read = port.readBytes(buffer, buffer.length);
            if (read > 0) {
                baos.write(buffer, 0, read);
                idleStreak = 0;
            } else {
                idleStreak++;
            }
        }
        byte[] result = baos.toByteArray();
        return result.length > 0 ? result : null;
    }

    private static String formatted(int address) {
        return String.format("%0" + ADDRESS_DIGITS + "d", address);
    }

    private void publishRow(Object port, Object baud, Object addr, Object value, Object status) {
        SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{port, baud, addr, value, status}));
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
        public final int address;
        public final Double value;

        FoundDevice(String port, int baud, int address, Double value) {
            this.port = port;
            this.baud = baud;
            this.address = address;
            this.value = value;
        }

        public String formattedAddress() {
            return String.format("%03d", address);
        }
    }
}
