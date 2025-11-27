package org.example.gui.autoresponder;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoResponderWindow extends JFrame {
    private static final Logger log = Logger.getLogger(AutoResponderWindow.class);
    private static final int RESPONSE_TIMEOUT_MS = 500; // Таймаут для накопления пакета
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    private JPanel mainPanel;
    private JPanel leftPanel; // Управление портом и лог
    private JPanel rightPanel; // Паттерны и ответы
    private JComboBox<String> jcbComPort;
    private JComboBox<Integer> jcbBaudRate;
    private JButton jbOpenPort;
    private JButton jbClosePort;
    private JTextArea jtaLog;
    private JButton jbAddPair;
    private JButton jbRemoveLastPair; // Или можно сделать per-pair remove
    private JScrollPane scrollPatterns;

    private SerialPort comPort;
    private Thread listenerThread;
    private AtomicBoolean running = new AtomicBoolean(false);
    private List<PatternResponsePair> patternPairs = new ArrayList<>();

    public AutoResponderWindow() {
        setTitle("Auto Responder");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel(new GridLayout(1, 2));
        leftPanel = new JPanel(new BorderLayout());
        rightPanel = new JPanel(new BorderLayout());

        // Left: Port controls
        JPanel portControls = new JPanel(new GridLayout(4, 1));
        jcbComPort = new JComboBox<>();
        updateComPortList();
        jcbBaudRate = new JComboBox<>(new Integer[]{4800, 9600, 19200, 38400, 57600, 115200});
        jcbBaudRate.setSelectedItem(19200); // Default, adjust as needed
        jbOpenPort = new JButton("Open Port");
        jbClosePort = new JButton("Close Port");
        jbClosePort.setEnabled(false);

        portControls.add(jcbComPort);
        portControls.add(jcbBaudRate);
        portControls.add(jbOpenPort);
        portControls.add(jbClosePort);

        // Log
        jtaLog = new JTextArea();
        jtaLog.setEditable(false);
        JScrollPane logScroll = new JScrollPane(jtaLog);

        leftPanel.add(portControls, BorderLayout.NORTH);
        leftPanel.add(logScroll, BorderLayout.CENTER);

        // Right: Patterns
        jbAddPair = new JButton("Add Pair");
        jbRemoveLastPair = new JButton("Remove Last Pair");
        JPanel buttonsPanel = new JPanel(new FlowLayout());
        buttonsPanel.add(jbAddPair);
        buttonsPanel.add(jbRemoveLastPair);

        JPanel patternsContainer = new JPanel();
        patternsContainer.setLayout(new BoxLayout(patternsContainer, BoxLayout.Y_AXIS));
        scrollPatterns = new JScrollPane(patternsContainer);

        rightPanel.add(buttonsPanel, BorderLayout.NORTH);
        rightPanel.add(scrollPatterns, BorderLayout.CENTER);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);
        setContentPane(mainPanel);

        // Listeners
        jbOpenPort.addActionListener(this::openPort);
        jbClosePort.addActionListener(this::closePort);
        jbAddPair.addActionListener(this::addPair);
        jbRemoveLastPair.addActionListener(this::removeLastPair);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closePort(null);
            }
        });

        loadPairs();
    }

    private void updateComPortList() {
        jcbComPort.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort port : ports) {
            jcbComPort.addItem(port.getSystemPortName() + " (" + port.getPortDescription() + ")");
        }
    }

    private void openPort(ActionEvent e) {
        String selectedPort = (String) jcbComPort.getSelectedItem();
        if (selectedPort == null) return;

        String portName = selectedPort.split(" ")[0]; // Extract port name
        comPort = SerialPort.getCommPort(portName);
        comPort.setBaudRate((Integer) jcbBaudRate.getSelectedItem());
        comPort.setParity(SerialPort.NO_PARITY); // Adjust if needed, from example it's variable but default no
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(1);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 60, 60);

        if (comPort.openPort()) {
            logMessage("Port opened: " + portName);
            jbOpenPort.setEnabled(false);
            jbClosePort.setEnabled(true);
            running.set(true);
            startListener();
        } else {
            logMessage("Failed to open port");
        }
    }

    private void closePort(ActionEvent e) {
        running.set(false);
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            logMessage("Port closed");
        }
        jbOpenPort.setEnabled(true);
        jbClosePort.setEnabled(false);
        if (listenerThread != null) {
            listenerThread.interrupt();
        }
    }

    private void startListener() {
        listenerThread = new Thread(() -> {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            while (running.get()) {
                try {
                    try {
                        Thread.sleep(RESPONSE_TIMEOUT_MS);
                    } catch (InterruptedException ex) {
                        log.warn("Listener interrupted");
                    }
                    int available = comPort.bytesAvailable();
                    if (available > 0) {
                        byte[] temp = new byte[available];
                        int read = comPort.readBytes(temp, available);
                        if (read > 0) {
                            buffer.write(temp, 0, read);
                            logReceived(Arrays.copyOf(temp, read)); // Log chunk
                        }
                    } else {
                        if (buffer.size() > 0) {
                            // Timeout reached, process packet
                            byte[] packet = buffer.toByteArray();
                            processPacket(packet);
                            buffer.reset();
                        }
                        Thread.sleep(10); // Small sleep to avoid busy loop
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (Exception ex) {
                    log.error("Listener error", ex);
                }
            }
        });
        listenerThread.start();
    }

    private void processPacket(byte[] packet) {
        String hexPacket = bytesToHex(packet).replace(" ", "").toUpperCase(); // Normalize
        for (PatternResponsePair pair : patternPairs) {
            String patternHex = pair.pattern.getText().replace(" ", "").toUpperCase();
            if (hexPacket.contains(patternHex)) {
                try {
                    byte[] response = hexStringToBytes(pair.response.getText());
                    comPort.writeBytes(response, response.length);
                    logSent(response);
                } catch (Exception e) {
                    logMessage("Error sending response: " + e.getMessage());
                }
                return;
            }
        }
        //logMessage("No match for packet: " + bytesToHex(packet));
    }

    private void logReceived(byte[] data) {
        String timestamp = TIME_FORMAT.format(new Date());
        String hex = bytesToHex(data);
        SwingUtilities.invokeLater(() -> jtaLog.append(timestamp + " < <" + hex + "\n"));
    }

    private void logSent(byte[] data) {
        String timestamp = TIME_FORMAT.format(new Date());
        String hex = bytesToHex(data);
        SwingUtilities.invokeLater(() -> jtaLog.append(timestamp + " > >" + hex + "\n"));
    }

    private void logMessage(String msg) {
        String timestamp = TIME_FORMAT.format(new Date());
        SwingUtilities.invokeLater(() -> jtaLog.append(timestamp + ": " + msg + "\n"));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private static byte[] hexStringToBytes(String hex) {
        hex = hex.replace(" ", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private void addPair(ActionEvent e) {
        PatternResponsePair newPair = new PatternResponsePair();
        patternPairs.add(newPair);
        ((JPanel) scrollPatterns.getViewport().getView()).add(newPair.panel);
        scrollPatterns.revalidate();
        scrollPatterns.repaint();
        savePairs();
    }

    private void removeLastPair(ActionEvent e) {
        if (!patternPairs.isEmpty()) {
            PatternResponsePair last = patternPairs.remove(patternPairs.size() - 1);
            ((JPanel) scrollPatterns.getViewport().getView()).remove(last.panel);
            scrollPatterns.revalidate();
            scrollPatterns.repaint();
            savePairs();
        }
    }

    private void loadPairs() {
        List<String[]> stored = PairStorage.load();
        for (String[] p : stored) {
            PatternResponsePair pair = new PatternResponsePair();
            pair.pattern.setText(p[0]);
            pair.response.setText(p[1]);
            patternPairs.add(pair);
            ((JPanel) scrollPatterns.getViewport().getView()).add(pair.panel);
        }
        scrollPatterns.revalidate();
        scrollPatterns.repaint();
    }

    private void savePairs() {
        List<String[]> toSave = new ArrayList<>();
        for (PatternResponsePair p : patternPairs) {
            toSave.add(new String[]{p.pattern.getText(), p.response.getText()});
        }
        PairStorage.save(toSave);
    }

    private class PatternResponsePair {
        JPanel panel;
        JTextField pattern;
        JTextField response;

        PatternResponsePair() {
            panel = new JPanel(new GridLayout(1, 2));
            pattern = new JTextField("Pattern HEX");
            response = new JTextField("Response HEX");
            panel.add(pattern);
            panel.add(response);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AutoResponderWindow().setVisible(true));
    }
}

class PairStorage {
    private static final Logger log = Logger.getLogger(PairStorage.class);
    private static final String FILE_PATH = "./config/autoresponder_pairs.txt";

    public static List<String[]> load() {
        List<String[]> pairs = new ArrayList<>();
        File file = new File(FILE_PATH);
        if (!file.exists()) return pairs;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                String[] parts = line.split("\\|", 2);
                if (parts.length == 2) {
                    pairs.add(parts);
                }
            }
        } catch (IOException e) {
            log.error("Error loading pairs", e);
        }
        return pairs;
    }

    public static void save(List<String[]> pairs) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (String[] pair : pairs) {
                bw.write(pair[0] + "|" + pair[1]);
                bw.newLine();
            }
        } catch (IOException e) {
            log.error("Error saving pairs", e);
        }
    }
}