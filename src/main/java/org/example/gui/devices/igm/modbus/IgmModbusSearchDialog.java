package org.example.gui.devices.igm.modbus;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Поиск сетевых адресов IGM (Modbus RTU) на выбранном порту/скорости.
 * Перебирает адреса 1..247, отправляя read holding register (getVersion),
 * и проверяет корректный Modbus-ответ (адрес, функция, CRC).
 */
@Slf4j
public class IgmModbusSearchDialog extends JDialog {

    private static final int MIN_ADDRESS = 1;
    private static final int MAX_ADDRESS = 247;
    private static final int PROBE_REGISTER = 0x0010; // reg 16 (getVersion)
    private static final int PROBE_COUNT = 1;

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

    public IgmModbusSearchDialog(Frame owner, String portName, int baud,
                                 int dataBits, int stopBits, int parity) {
        super(owner, "Поиск сетевых адресов IGM (Modbus)", true);
        this.portName = portName;
        this.baud = baud;
        this.dataBits = dataBits;
        this.stopBits = stopBits;
        this.parity = parity;

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
                                .append(", адрес ").append(fd.address)
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
        sp.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING | SerialPort.TIMEOUT_WRITE_BLOCKING, 60, 500);

        if (!sp.openPort()) {
            String msg = "Ошибка: порт " + portName + " не открыт (занят другой программой/потоком?)";
            publishRow(portName, baud, "-", msg);
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
            setStatus("Проверка порт=" + portName + " скорость=" + baud + " адрес=" + addr);

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
                publishRow(portName, baud, addr, "Найдено!");
                foundDevices.add(new FoundDevice(portName, baud, addr));
                log.info("IGM found: port={} baud={} addr={}", portName, baud, addr);
                setStatus("Найдено устройство: порт=" + portName + " скорость=" + baud + " адрес=" + addr);
            }
            updateProgress(++step);
        }

        closeCurrentPort();
    }

    private byte[] buildProbe(int address) {
        byte[] frame = new byte[8];
        frame[0] = (byte) address;
        frame[1] = 0x03;
        frame[2] = (byte) ((PROBE_REGISTER >> 8) & 0xFF);
        frame[3] = (byte) (PROBE_REGISTER & 0xFF);
        frame[4] = (byte) ((PROBE_COUNT >> 8) & 0xFF);
        frame[5] = (byte) (PROBE_COUNT & 0xFF);
        int crc = modbusCrc(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    private boolean isMatch(byte[] response, int address) {
        if (response == null || response.length < 5) {
            return false;
        }
        if ((response[0] & 0xFF) != address) {
            return false;
        }
        int func = response[1] & 0xFF;
        if (func != 0x03 && func != 0x04) {
            return false; // исключение (>=0x80) тоже отсекаем: адрес не тот
        }
        int received = (response[response.length - 2] & 0xFF) | ((response[response.length - 1] & 0xFF) << 8);
        int calculated = modbusCrc(response, 0, response.length - 2);
        return received == calculated;
    }

    private byte[] readResponse(SerialPort port) {
        int waitMs = 300;
        long deadline = System.currentTimeMillis() + waitMs;
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

    private static int modbusCrc(byte[] data, int offset, int length) {
        int crc = 0xFFFF;
        for (int i = offset; i < offset + length; i++) {
            crc ^= (data[i] & 0xFF);
            for (int j = 0; j < 8; j++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    private void publishRow(Object port, Object baud, Object addr, Object status) {
        SwingUtilities.invokeLater(() ->
                tableModel.addRow(new Object[]{port, baud, addr, status}));
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

        FoundDevice(String port, int baud, int address) {
            this.port = port;
            this.baud = baud;
            this.address = address;
        }
    }
}
