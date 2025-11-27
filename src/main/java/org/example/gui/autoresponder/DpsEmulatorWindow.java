package org.example.gui.autoresponder;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.gui.autoresponder.DumpGenerator.safetyGetValue;

public class DpsEmulatorWindow extends JFrame {
    public static final Logger log = Logger.getLogger(DpsEmulatorWindow.class);
    private static final int RESPONSE_TIMEOUT_MS = 250; // Таймаут для накопления пакета

    private JPanel mainPanel;
    private JPanel leftPanel; // Управление портом и лог
    private JPanel rightPanel; // Поля ввода значений
    private JComboBox<String> jcbComPort;
    private JComboBox<Integer> jcbBaudRate;
    private JButton jbOpenPort;
    private JButton jbClosePort;
    JTextArea jtaLog;
    private JButton jbSaveConfig;
    private JButton jbSimulateAll;
    private JButton jbSimulateGraph;

    // Поля ввода для эмуляции
    JTextField jtfModel;
    JTextField jtfSwVersion;
    JTextField jtfHwVersion;
    JTextField jtfVoltageSet;
    JTextField jtfCurrentSet;
    JTextField jtfTemperature;
    JTextField jtfStatus;
    JTextField jtfVin;
    JTextField jtfUpperLimitVoltage;
    JTextField jtfUpperLimitCurrent;
    JTextField jtfMeasCurrentOut;
    JTextField jtfMeasVoltOut;
    JTextField jtfMeasWattOut;
    JTextField jtfBrightness;

    // Визуальные индикаторы
    private JPanel wakeUpIndicator;
    private JPanel dunnoModeIndicator;
    private JPanel powerOutputIndicator;

    // Состояния
    boolean isWakeUp = false;
    boolean isPowerOutputOpen = false;

    SerialPort comPort;
    private Thread listenerThread;
    private AtomicBoolean running = new AtomicBoolean(false);

    public DpsEmulatorWindow() {
        setTitle("DPS Emulator");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        mainPanel = new JPanel(new GridLayout(1, 2));
        leftPanel = new JPanel(new BorderLayout());
        rightPanel = new JPanel(new GridLayout(0, 2));

        JPanel portControls = new JPanel(new GridLayout(4, 1));
        jcbComPort = new JComboBox<>();
        updateComPortList();
        jcbBaudRate = new JComboBox<>(new Integer[]{4800, 9600, 19200, 38400, 57600, 115200});
        jcbBaudRate.setSelectedItem(115200); // Из протокола
        jbOpenPort = new JButton("Open Port");
        jbClosePort = new JButton("Close Port");
        jbClosePort.setEnabled(false);

        portControls.add(jcbComPort);
        portControls.add(jcbBaudRate);
        portControls.add(jbOpenPort);
        portControls.add(jbClosePort);

        jtaLog = new JTextArea();
        jtaLog.setEditable(false);
        JScrollPane logScroll = new JScrollPane(jtaLog);

        leftPanel.add(portControls, BorderLayout.NORTH);
        leftPanel.add(logScroll, BorderLayout.CENTER);

        rightPanel.add(new JLabel("Model:"));
        jtfModel = new JTextField("DPS-150");
        rightPanel.add(jtfModel);

        rightPanel.add(new JLabel("SW Version:"));
        jtfSwVersion = new JTextField("V1.1");
        rightPanel.add(jtfSwVersion);

        rightPanel.add(new JLabel("HW Version:"));
        jtfHwVersion = new JTextField("V1.0");
        rightPanel.add(jtfHwVersion);

        //Нигде не отображается
        rightPanel.add(new JLabel("Selected Out (V):"));
        jtfVoltageSet = new JTextField("4.0");
        rightPanel.add(jtfVoltageSet);

        //Нигде не отображается
        rightPanel.add(new JLabel("Selected Out (I):"));
        jtfCurrentSet = new JTextField("1.234");
        rightPanel.add(jtfCurrentSet);

        rightPanel.add(new JLabel("Temperature (°C):"));
        jtfTemperature = new JTextField("24.0");
        rightPanel.add(jtfTemperature);

        rightPanel.add(new JLabel("Status (0/1):"));
        jtfStatus = new JTextField("1");
        rightPanel.add(jtfStatus);

        rightPanel.add(new JLabel("Meas In(V):")); //PowerSupply
        jtfVin = new JTextField("5.25");
        rightPanel.add(jtfVin);

        rightPanel.add(new JLabel("Upper Limit (V):"));
        jtfUpperLimitVoltage = new JTextField("4.55");
        rightPanel.add(jtfUpperLimitVoltage);

        rightPanel.add(new JLabel("Upper Limit (A):"));
        jtfUpperLimitCurrent = new JTextField("1.55");
        rightPanel.add(jtfUpperLimitCurrent);


        rightPanel.add(new JLabel("Meas Out (I):"));
        jtfMeasCurrentOut = new JTextField("1.110");
        rightPanel.add(jtfMeasCurrentOut);

        rightPanel.add(new JLabel("Meas Out (V):"));
        jtfMeasVoltOut = new JTextField("2.220");
        rightPanel.add(jtfMeasVoltOut);

        rightPanel.add(new JLabel("Meas Out (W):"));
        jtfMeasWattOut = new JTextField("3.330");
        rightPanel.add(jtfMeasWattOut);

        rightPanel.add(new JLabel("Brightness:"));
        jtfBrightness = new JTextField("10");
        rightPanel.add(jtfBrightness);

        // Визуальные индикаторы
        wakeUpIndicator = new JPanel();
        wakeUpIndicator.setPreferredSize(new Dimension(20, 20));
        wakeUpIndicator.setBackground(Color.BLACK);

        dunnoModeIndicator = new JPanel();
        dunnoModeIndicator.setPreferredSize(new Dimension(20, 20));
        dunnoModeIndicator.setBackground(Color.BLACK);

        powerOutputIndicator = new JPanel();
        powerOutputIndicator.setPreferredSize(new Dimension(20, 20));
        powerOutputIndicator.setBackground(Color.BLACK);

        rightPanel.add(new JLabel("WakeUp Indicator:"));
        rightPanel.add(wakeUpIndicator);

        rightPanel.add(new JLabel("DunnoMode Indicator:"));
        rightPanel.add(dunnoModeIndicator);

        rightPanel.add(new JLabel("PowerOutput Indicator:"));
        rightPanel.add(powerOutputIndicator);

        //Отсуствует поля:
        //Яркость
        //F1 B1 D6 01 0B E2 (set 11)
        //Voltage
        //F1 B1 C1 04 D7 A3 40 40 BF (set 3.01 V)
        //Ampere
        //F1 B1 C2 04 9C C4 80 3F E5 (set 1.006 A)



        jbSaveConfig = new JButton("Save Config");
        rightPanel.add(jbSaveConfig);

        jbSimulateAll = new JButton("Simulate All Responses");
        rightPanel.add(jbSimulateAll);

        jbSimulateGraph = new JButton("Simulate Graph");
        rightPanel.add(jbSimulateGraph);

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);
        setContentPane(mainPanel);

        // Listeners
        jbOpenPort.addActionListener(this::openPort);
        jbClosePort.addActionListener(this::closePort);
        jbSaveConfig.addActionListener(e -> ConfigManager.saveConfig(this));
        jbSimulateAll.addActionListener(e -> Simulator.simulateAll(this));
        jbSimulateGraph.addActionListener(e -> Simulator.simulateForGraph(this));




        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                ConfigManager.loadConfig(DpsEmulatorWindow.this);
            }

            @Override
            public void windowClosing(WindowEvent e) {
                ConfigManager.saveConfig(DpsEmulatorWindow.this);
                closePort(null);
            }
        });
    }

    void updateIndicators() {
        wakeUpIndicator.setBackground(isWakeUp ? Color.GREEN : Color.BLACK);
        powerOutputIndicator.setBackground(isPowerOutputOpen ? Color.GREEN : Color.BLACK);
        try {
            boolean dunnoOn = Integer.parseInt(safetyGetValue(jtfStatus)) == 1;
            dunnoModeIndicator.setBackground(dunnoOn ? Color.LIGHT_GRAY : Color.BLACK);
        } catch (NumberFormatException e) {
            dunnoModeIndicator.setBackground(Color.RED);
        }
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
        comPort.setParity(SerialPort.NO_PARITY);
        comPort.setNumDataBits(8);
        comPort.setNumStopBits(1);
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 60, 60);

        if (comPort.openPort()) {
            LogUtil.logMessage(jtaLog, "Port opened: " + portName);
            jbOpenPort.setEnabled(false);
            jbClosePort.setEnabled(true);
            running.set(true);
            startListener();
        } else {
            LogUtil.logMessage(jtaLog, "Failed to open port");
        }
    }

    private void closePort(ActionEvent e) {
        running.set(false);
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            LogUtil.logMessage(jtaLog, "Port closed");
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
            long lastReadTime = System.currentTimeMillis();
            while (running.get()) {
                try {

                    Thread.sleep(RESPONSE_TIMEOUT_MS);

                    int available = comPort.bytesAvailable();
                    if (available > 0) {
                        byte[] temp = new byte[available];
                        int read = comPort.readBytes(temp, available);
                        if (read > 0) {
                            buffer.write(temp, 0, read);
                            LogUtil.logReceived(jtaLog, temp);
                            lastReadTime = System.currentTimeMillis();
                        }
                    } else if (buffer.size() > 0 && (System.currentTimeMillis() - lastReadTime > RESPONSE_TIMEOUT_MS)) {
                        // Process accumulated packet
                        byte[] packet = buffer.toByteArray();
                        processPacket(packet);
                        buffer.reset();
                    }
                    Thread.sleep(10);
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
        // Разбор пакета на команды и генерация ответов на основе текущих значений
        int index = 0;
        while (index < packet.length) {
            if (packet[index] != (byte) 0xF1) {
                index++;
                continue;
            }
            if (index + 5 >= packet.length) break; // Min header + cs
            byte cmd = packet[index + 1];
            byte type = packet[index + 2];
            byte len = packet[index + 3];
            if (index + 4 + len + 1 > packet.length) break;
            byte[] data = Arrays.copyOfRange(packet, index + 4, index + 4 + len);
            // byte cs = packet[index + 4 + len]; // Можно проверить

            byte[] response = generateResponse(cmd, type, data);
            if (response != null) {
                comPort.writeBytes(response, response.length);
                LogUtil.logSent(jtaLog, response);
            }

            index += 4 + len + 1;
        }
    }

    private byte[] generateResponse(byte cmd, byte type, byte[] data) {
        // Генерация ответа на основе type и текущих значений из полей
        if (cmd == (byte) 0xA1) { // Get
            byte[] respData;
            switch (type) {
                case (byte) 0xE1: // Status
                    respData = new byte[]{(byte) Integer.parseInt(safetyGetValue(jtfStatus))};
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xDE: // Model
                    respData = jtfModel.getText().getBytes();
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xE0: // SW Version
                    respData = jtfSwVersion.getText().getBytes();
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xDF: // HW Version
                    respData = jtfHwVersion.getText().getBytes();
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xFF: // Dump (custom, with values)
                    try {
                        respData = DumpGenerator.generateDump(this);
                    } catch (IOException e) {
                        log.error("Error generating dump", e);
                        throw new RuntimeException(e);
                    }
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xC4: // Temp (example, add if needed)
                    float temp = Float.parseFloat(safetyGetValue(jtfTemperature));
                    respData = ByteUtils.floatToBytes(temp);
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xC0: // Vin
                    float vin = Float.parseFloat(safetyGetValue(jtfVin));
                    return buildResponse((byte) 0xA1, type, ByteUtils.floatToBytes(vin));

                case (byte) 0xE2: // Vout measured
                    float vout = Float.parseFloat(safetyGetValue(jtfUpperLimitVoltage));
                    return buildResponse((byte) 0xA1, type, ByteUtils.floatToBytes(vout));

                case (byte) 0xE3: // Iout measured
                    float iout = Float.parseFloat(safetyGetValue(jtfUpperLimitCurrent));
                    return buildResponse((byte) 0xA1, type, ByteUtils.floatToBytes(iout));
                case (byte) 0xD6: // A1 D6 — получение яркости
                    int brightness = Integer.parseInt(safetyGetValue(jtfBrightness));
                    respData = new byte[]{(byte) brightness};
                    return buildResponse((byte) 0xA1, type, respData);

                case (byte) 0xDB: // A1 DB — состояние выхода (boolean)
                    respData = new byte[]{ (byte) (isPowerOutputOpen ? 0x01 : 0x00) };
                    return buildResponse((byte) 0xA1, type, respData);

                case (byte) 0xC3: // A1 C3 — выходные мощности 3 float LE
                    respData = ByteBuffer.allocate(12)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putFloat(Float.parseFloat(safetyGetValue(jtfMeasVoltOut)))
                            .putFloat(Float.parseFloat(safetyGetValue(jtfMeasCurrentOut)))
                            .putFloat(Float.parseFloat(safetyGetValue(jtfMeasWattOut)))
                            .array();
                    return buildResponse((byte)0xA1, type, respData);
                default:
                    return null;
            }
        } else if (cmd == (byte) 0xB1 || cmd == (byte) 0xC1) { // Set or special — echo or no response
            if (cmd == (byte) 0xB1) {
                switch (type) {
                    case (byte) 0xC1: // set voltage
                        float volt = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                        SwingUtilities.invokeLater(() -> jtfVoltageSet.setText(String.format("%.3f", volt)));
                        return null;
                    case (byte) 0xC2: // set current
                        float curr = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                        SwingUtilities.invokeLater(() -> jtfCurrentSet.setText(String.format("%.3f", curr)));
                        return null;
                    case (byte) 0xD6: // brightness
                        int bright = data[0] & 0xFF;
                        SwingUtilities.invokeLater(() -> jtfBrightness.setText(String.valueOf(bright)));
                        return null;
                    case (byte) 0xDB: // PowerOutput
                        isPowerOutputOpen = (data[0] == 0x01);
                        SwingUtilities.invokeLater(this::updateIndicators);
                        return null;
                    default:
                        return null;
                }
            } else if (cmd == (byte) 0xC1) {
                if (type == (byte) 0x00) { // WakeUp
                    isWakeUp = (data[0] == 0x01);
                    SwingUtilities.invokeLater(this::updateIndicators);
                    return null;
                }
            }
            return null; // Or echo for confirmation
        }
        return null;
    }

    byte[] buildResponse(byte respCmd, byte type, byte[] data) {
        byte len = (byte) data.length;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write((byte) 0xF0); // Start recv
        baos.write(respCmd);
        baos.write(type);
        baos.write(len);
        try {
            baos.write(data);
        } catch (IOException ignored) {}
        byte[] frame = baos.toByteArray();
        byte cs = ByteUtils.calculateCs(frame);
        return ByteUtils.concatenate(frame, new byte[]{cs});
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DpsEmulatorWindow().setVisible(true));
    }
}