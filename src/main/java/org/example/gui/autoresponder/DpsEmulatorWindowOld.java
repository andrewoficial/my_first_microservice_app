package org.example.gui.autoresponder;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class DpsEmulatorWindowOld extends JFrame {
    private static final Logger log = Logger.getLogger(DpsEmulatorWindowOld.class);
    private static final int RESPONSE_TIMEOUT_MS = 250; // Таймаут для накопления пакета
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final String CONFIG_FILE = "./config/dps_emulator_config.properties";

    private JPanel mainPanel;
    private JPanel leftPanel; // Управление портом и лог
    private JPanel rightPanel; // Поля ввода значений
    private JComboBox<String> jcbComPort;
    private JComboBox<Integer> jcbBaudRate;
    private JButton jbOpenPort;
    private JButton jbClosePort;
    private JTextArea jtaLog;
    private JButton jbSaveConfig;
    private JButton jbSimulateAll;
    private JButton jbSimulateGraph;

    // Поля ввода для эмуляции
    private JTextField jtfModel;
    private JTextField jtfSwVersion;
    private JTextField jtfHwVersion;
    private JTextField jtfVoltageSet;
    private JTextField jtfCurrentSet;
    private JTextField jtfTemperature;
    private JTextField jtfStatus;
    private JTextField jtfVin;
    private JTextField jtfUpperLimitVoltage;
    private JTextField jtfUpperLimitCurrent;
    private JTextField jtfMeasCurrentOut;
    private JTextField jtfMeasVoltOut;
    private JTextField jtfMeasWattOut;
    private JTextField jtfBrightness;

    private SerialPort comPort;
    private Thread listenerThread;
    private AtomicBoolean running = new AtomicBoolean(false);

    public DpsEmulatorWindowOld() {
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
        jbSaveConfig.addActionListener(this::saveConfig);
        jbSimulateAll.addActionListener(e -> simulateAll());
        jbSimulateGraph.addActionListener(e -> simulateForGraph());




        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                loadConfig();
            }

            @Override
            public void windowClosing(WindowEvent e) {
                saveConfig(null);
                closePort(null);
            }
        });
    }
/*
    //F0 A1 C3 0C 7B 14 0E 40 7B 14 8E 3F B8 1E 55 40 73 F0 A1 C0 04 00 00 A8 40 AC F0 A1 E2 04 9A 99 91 40 EA F0 A1 E3 04 66 66 C6 3F B8 F0 A1 C4 04 00 00 C8 41 D1
    //20:58:35.237: Simulated graph sent.
    private void simulateForGraph(){
        try {
            List<byte[]> packets = new ArrayList<>();

            packets.add(buildResponse((byte)0xA1, (byte)0xC3,
                    ByteBuffer.allocate(12)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putFloat(Float.parseFloat(jtfMeasVoltOut.getText()))
                            .putFloat(Float.parseFloat(jtfMeasCurrentOut.getText()))
                            .putFloat(Float.parseFloat(jtfMeasWattOut.getText()))
                            .array()
            ));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xC0,
                    floatToBytes(Float.parseFloat(jtfVin.getText()))));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xE2,
                    floatToBytes(Float.parseFloat(jtfUpperLimitVoltage.getText()))));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xE3,
                    floatToBytes(Float.parseFloat(jtfUpperLimitCurrent.getText()))));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xC4,
                    floatToBytes(Float.parseFloat(jtfTemperature.getText()))));

            // Правильно вычисляем общий размер всех пакетов
            int totalSize = 0;
            for (int i = 0; i < packets.size(); i++) {
                totalSize += packets.get(i).length;
            }

            // Создаем массив для объединенных данных
            byte[] data = new byte[totalSize];

            // Копируем данные из каждого пакета в общий массив
            int currentPosition = 0; // Текущая позиция в целевом массиве
            for (int i = 0; i < packets.size(); i++) {
                byte[] packet = packets.get(i);
                // Копируем содержимое пакета в массив data
                System.arraycopy(packet, 0, data, currentPosition, packet.length);
                // Увеличиваем позицию для следующего пакета
                currentPosition += packet.length;
            }

            comPort.writeBytes(data, data.length);
            logSent(data);
            Thread.sleep(30);
            // отправляем последовательно
            for (byte[] p : packets) {
//                comPort.writeBytes(p, p.length);
//                logSent(p);
//                Thread.sleep(30);
            }

            logMessage("Simulated graph sent.");

        } catch (Exception ex) {
            log.error("simulateForGraph() error", ex);
        }
    }
 */

//    private void simulateForGraph() {
//        try {
//            String receivedHex = "f0 a1 e1 01 01 e3 f0 a1 de 07 44 50 53 2d 31 35 30 8f f0 a1 e0 04 56 31 2e 31 ca f0 a1 df 04 56 31 2e 30 c8 f0 a1 ff 8b 6f f9 a7 40 00 00 80 40 b6 f3 9d 3f 00 00 00 00 00 00 00 00 00 00 00 00 4d a8 c0 41 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 f0 41 33 33 a3 40 00 00 16 43 00 00 a0 42 00 00 a0 40 0a 0a 01 00 00 00 00 00 00 00 00 00 00 01 01 09 93 a1 40 33 33 a3 40 00 00 f0 41 33 33 a3 40 00 00 16 43 00 00 c6 42 00 00 f0 41 4c f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 01 00 a8 40 ad f0 a1 e2 04 9b 99 a1 40 fb f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 b8 bd be 41 3c f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 e1 f2 a7 40 7e f0 a1 e2 04 7b 8c a1 40 ce f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 4d a8 c0 41 be f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 a4 01 a8 40 51 f0 a1 e2 04 3e 9b a1 40 a0 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 c2 fe bf 41 88 f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 fc ff a7 40 a6 f0 a1 e2 04 96 99 a1 40 f6 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 a0 9c bf 41 04 f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 b3 ea a7 40 48 f0 a1 e2 04 4d 84 a1 40 98 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 ee d4 c0 41 8b f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 5c fe a7 40 05 f0 a1 e2 04 f6 97 a1 40 54 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 6b 12 bf 41 45 f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 e3 f2 a7 40 80 f0 a1 e2 04 7d 8c a1 40 d0 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 9b 84 c0 41 e8 f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 13 fb a7 40 b9 f0 a1 e2 04 ad 94 a1 40 08 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 65 b7 bf 41 e4 f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 52 ec a7 40 e9 f0 a1 e2 04 ec 85 a1 40 38 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 72 5c c0 41 97 f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 0f e9 a7 40 a3 f0 a1 e2 04 a9 82 a1 40 f2 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 c8 ac c0 41 3d f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 86 f4 a7 40 25 f0 a1 e2 04 20 8e a1 40 75 f0 a1 e3 04 33 33 a3 40 30 f0 a1 c4 04 dd 16 bf 41 bb f0 a1 c3 0c 00 00 00 00 00 00 00 00 00 00 00 00 cf f0 a1 c0 04 2c f6 a7 40 cd";
//            byte[] allData = hexStringToBytes(receivedHex);
//
//            int index = 0;
//            while (index < allData.length) {
//                if (allData[index] != (byte) 0xF0) {
//                    index++;
//                    continue;
//                }
//                if (index + 4 >= allData.length) break;
//                byte len = allData[index + 3];
//                int packetLength = 4 + (len & 0xFF) + 1; // header (4 bytes) + data + cs
//                if (index + packetLength > allData.length) break;
//                byte[] packet = Arrays.copyOfRange(allData, index, index + packetLength);
//                comPort.writeBytes(packet, packet.length);
//                logSent(packet);
//                Thread.sleep(30); // Задержка между пакетами для имитации реального обмена
//                index += packetLength;
//            }
//
//            logMessage("Simulated graph sent.");
//
//        } catch (Exception ex) {
//            log.error("simulateForGraph() error", ex);
//        }
//    }

    private void simulateForGraph() {
        new Thread(() -> {
            try {
                AtomicBoolean simulating = new AtomicBoolean(true);
                // To stop, perhaps add a button, but for now, simulate 100 points
                int numPoints = 100;
                float baseVoltOut = Float.parseFloat(jtfMeasVoltOut.getText());
                float baseCurrentOut = Float.parseFloat(jtfMeasCurrentOut.getText());
                float baseWattOut = Float.parseFloat(jtfMeasWattOut.getText());
                float vin = Float.parseFloat(jtfVin.getText());
                float upperLimitVoltage = Float.parseFloat(jtfUpperLimitVoltage.getText());
                float upperLimitCurrent = Float.parseFloat(jtfUpperLimitCurrent.getText());
                float temperature = Float.parseFloat(jtfTemperature.getText());

                for (int i = 0; i < numPoints && simulating.get(); i++) {
                    // Варьируем значения для имитации изменения данных
                    float voltOut = baseVoltOut + (float) Math.sin(i * 0.1) * 0.5f;
                    float currentOut = baseCurrentOut + (float) Math.cos(i * 0.1) * 0.2f;
                    float wattOut = voltOut * currentOut; // Вычисляем мощность

                    // c3: Meas Out (V, I, W)
                    byte[] c3Data = ByteBuffer.allocate(12)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putFloat(voltOut)
                            .putFloat(currentOut)
                            .putFloat(wattOut)
                            .array();
                    byte[] packet = buildResponse((byte) 0xA1, (byte) 0xC3, c3Data);
                    comPort.writeBytes(packet, packet.length);
                    logSent(packet);

                    // c0: Vin (с небольшим случайным вариациями)
                    packet = buildResponse((byte) 0xA1, (byte) 0xC0,
                            floatToBytes(vin + (float) Math.random() * 0.1f - 0.05f));
                    comPort.writeBytes(packet, packet.length);
                    logSent(packet);

                    // e2: Upper Limit Voltage (с небольшим вариациями)
                    packet = buildResponse((byte) 0xA1, (byte) 0xE2,
                            floatToBytes(upperLimitVoltage + (float) Math.random() * 0.1f - 0.05f));
                    comPort.writeBytes(packet, packet.length);
                    logSent(packet);

                    // e3: Upper Limit Current (фиксировано)
                    packet = buildResponse((byte) 0xA1, (byte) 0xE3,
                            floatToBytes(upperLimitCurrent));
                    comPort.writeBytes(packet, packet.length);
                    logSent(packet);

                    // c4: Temperature (с небольшим случайным вариациями)
                    packet = buildResponse((byte) 0xA1, (byte) 0xC4,
                            floatToBytes(temperature + (float) Math.random() * 2f - 1f));
                    comPort.writeBytes(packet, packet.length);
                    logSent(packet);

                    Thread.sleep(500); // Задержка 500 мс между наборами пакетов для обновления графика
                }

                logMessage("Simulated graph sent.");

            } catch (Exception ex) {
                log.error("simulateForGraph() error", ex);
            }
        }).start();
    }
    private void simulateAll() {
        try {
            List<byte[]> packets = new ArrayList<>();

            packets.add(buildResponse((byte) 0xA1, (byte) 0xE1,
                    new byte[]{(byte) Integer.parseInt(jtfStatus.getText())}));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xDE,
                    jtfModel.getText().getBytes()));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xE0,
                    jtfSwVersion.getText().getBytes()));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xDF,
                    jtfHwVersion.getText().getBytes()));



            //F1 B1 C2 04 6F 12 03 3B 85
            packets.add(buildResponse((byte) 0xB1, (byte) 0xC2,
                    floatToBytes(Float.parseFloat(jtfCurrentSet.getText()))));

            packets.add(buildResponse((byte) 0xB1, (byte) 0xC1,
                    floatToBytes(Float.parseFloat(jtfVoltageSet.getText()))));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xE2,
                    floatToBytes(Float.parseFloat(jtfUpperLimitVoltage.getText()))));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xE3,
                    floatToBytes(Float.parseFloat(jtfUpperLimitCurrent.getText()))));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xFF,
                    generateDump()));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xC0,
                    floatToBytes(Float.parseFloat(jtfVin.getText()))));

            packets.add(buildResponse((byte)0xA1, (byte)0xC3,
                    ByteBuffer.allocate(12)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putFloat(Float.parseFloat(jtfMeasVoltOut.getText()))
                            .putFloat(Float.parseFloat(jtfMeasCurrentOut.getText()))
                            .putFloat(Float.parseFloat(jtfMeasWattOut.getText()))
                            .array()
            ));

            packets.add(buildResponse((byte) 0xA1, (byte) 0xC4,
                    floatToBytes(Float.parseFloat(jtfTemperature.getText()))));


            // отправляем последовательно
            for (byte[] p : packets) {
                comPort.writeBytes(p, p.length);
                logSent(p);
                Thread.sleep(30);
            }

            logMessage("Simulated responses sent.");

        } catch (Exception ex) {
            log.error("simulateAll() error", ex);
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
                            logReceived(temp);
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
                logSent(response);
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
                    respData = new byte[]{(byte) Integer.parseInt(jtfStatus.getText())};
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
                        respData = generateDump();
                    } catch (IOException e) {
                        log.error("Error generating dump", e);
                        throw new RuntimeException(e);
                    }
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xC4: // Temp (example, add if needed)
                    float temp = Float.parseFloat(jtfTemperature.getText());
                    respData = floatToBytes(temp);
                    return buildResponse((byte) 0xA1, type, respData);
                case (byte) 0xC0: // Vin
                    float vin = Float.parseFloat(jtfVin.getText());
                    return buildResponse((byte) 0xA1, type, floatToBytes(vin));

                case (byte) 0xE2: // Vout measured
                    float vout = Float.parseFloat(jtfUpperLimitVoltage.getText());
                    return buildResponse((byte) 0xA1, type, floatToBytes(vout));

                case (byte) 0xE3: // Iout measured
                    float iout = Float.parseFloat(jtfUpperLimitCurrent.getText());
                    return buildResponse((byte) 0xA1, type, floatToBytes(iout));
                // Add more types as needed (e.g., 0xE2 vout meas, etc.)
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
                    default:
                        return null;
                }
            }
            return null; // Or echo for confirmation
        }
        return null;
    }

    private byte[] buildResponse(byte respCmd, byte type, byte[] data) {
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
        byte cs = calculateCs(frame);
        return concatenate(frame, new byte[]{cs});
    }

    private byte calculateCs(byte[] data) {
        int sum = 0;
        for (int i = 2; i < data.length; i++) {
            sum += (data[i] & 0xFF);
        }
        return (byte) (sum % 256);
    }

    private byte[] concatenate(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    private byte[] floatToBytes(float value) {
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
        return bb.array();
    }

    private byte[] generateDump() throws IOException {
        // Генерация дампа на основе значений (адаптировать структуру из лога)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Пример: vin (fake), vout_set, iout_set, zeros, temp, limits...
        baos.write(floatToBytes(Float.parseFloat(jtfVin.getText())));
        baos.write(floatToBytes(Float.parseFloat(jtfVoltageSet.getText())));
        baos.write(floatToBytes(Float.parseFloat(jtfCurrentSet.getText())));
        // Add zeros and other fixed/floats from log
        byte[] zeros = new byte[12]; // Adjust
        baos.write(zeros);
        baos.write(floatToBytes(Float.parseFloat(jtfTemperature.getText())));
        // Add more from log structure
        // Total len should be 0x8B (139)
        byte[] fixedPart = hexStringToBytes("00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00"); // From log, adjust
        baos.write(fixedPart);
        return baos.toByteArray();
    }

    private byte[] hexStringToBytes(String hex) {
        hex = hex.replace(" ", "");
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        return bytes;
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

    private void saveConfig(ActionEvent e) {
        Properties prop = new Properties();
        prop.setProperty("model", jtfModel.getText());
        prop.setProperty("swVersion", jtfSwVersion.getText());
        prop.setProperty("hwVersion", jtfHwVersion.getText());
        prop.setProperty("voltageSet", jtfVoltageSet.getText());
        prop.setProperty("currentSet", jtfCurrentSet.getText());
        prop.setProperty("temperature", jtfTemperature.getText());
        prop.setProperty("status", jtfStatus.getText());
        prop.setProperty("brightness", jtfBrightness.getText());

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            prop.store(output, null);
            logMessage("Config saved");
        } catch (IOException io) {
            log.error("Error saving config", io);
        }
    }

    private void loadConfig() {
        Properties prop = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                prop.load(input);
                jtfModel.setText(prop.getProperty("model", "DPS-150"));
                jtfSwVersion.setText(prop.getProperty("swVersion", "V1.1"));
                jtfHwVersion.setText(prop.getProperty("hwVersion", "V1.0"));
                jtfVoltageSet.setText(prop.getProperty("voltageSet", "4.0"));
                jtfCurrentSet.setText(prop.getProperty("currentSet", "1.234"));
                jtfTemperature.setText(prop.getProperty("temperature", "24.0"));
                jtfStatus.setText(prop.getProperty("status", "1"));
                jtfBrightness.setText(prop.getProperty("brightness", "10"));
                logMessage("Config loaded");
            } catch (IOException ex) {
                log.error("Error loading config", ex);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new DpsEmulatorWindowOld().setVisible(true));
    }
}