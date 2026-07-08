package org.example.gui.devices.qidian.qdl80a.control;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.device.protQdl80a.Qdl80aCommandRegistry;
import org.example.device.protQdl80a.Qdl80aCommunicationService;
import org.example.gui.devices.qidian.qdl80a.util.DevicesSearch;
import org.example.utilites.MyUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.StyleContext;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.prefs.Preferences;

public class Qdl80aMain {
    private static final Logger log = Logger.getLogger(Qdl80aMain.class);
    private static final String PREFS_NODE = "org/example/gui/qdl80a";
    private static final String LAST_PORT = "lastPort";
    private static final String LAST_BAUD = "lastBaud";

    private JPanel mainPanel;
    private JComboBox<String> portComboSelect;
    private JComboBox<Integer> portComboSpeed;
    private JButton refreshBtn;
    private JButton openPortBtn;
    private JButton closePortBtn;
    private JPanel comLampContainer;
    private JPanel deviceLampContainer;
    private JLabel pvValueLabel;
    private JLabel unitLabel;
    private JLabel decimalLabel;
    private JTextField addressField;
    private JComboBox<Integer> baudCombo;
    private JComboBox<String> unitCombo;
    private JComboBox<Integer> decimalCombo;
    private JTextField offsetField;
    private JButton readParamsBtn;
    private JButton writeParamsBtn;
    private JButton saveBtn;
    private JButton restoreBtn;
    private JButton readPvBtn;
    private JCheckBox autoRefreshCheck;
    private JLabel statusBar;
    private JPanel oscilloscopePanel;
    private JPanel comControl;
    private JPanel devSearch;
    private JCheckBox addressUnknown_CB;
    private JCheckBox portUnknown_CB;
    private JCheckBox speedUnknown_CB;
    private JButton runSearch_BT;
    private JPanel statusPanel;
    private JPanel portStatusLamp;
    private JLabel portStatusLabel;
    private JPanel deviceStatusLamp;
    private JLabel deviceStatusLabel;
    private JPanel deviceLampSubContainer;
    private JTextArea noteForUser;
    private JPanel sequenceStatusLamp;

    //private final LampIndicator seqLamp = new LampIndicator();
    private final LampIndicator comLamp = new LampIndicator();
    private final LampIndicator deviceLamp = new LampIndicator();
    private final Qdl80aCommunicationService service = new Qdl80aCommunicationService();
    private final Qdl80aCommandRegistry registry = new Qdl80aCommandRegistry();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private volatile boolean autoRefreshActive = false;
    private final java.util.Queue<byte[]> requestQueue = new ConcurrentLinkedQueue<>();
    private volatile long lastPvValue = 0;
    private volatile int currentAddress = 1;

    // Списки для лога (если нужен)
    private final List<String> logLines = new CopyOnWriteArrayList<>();

    public Qdl80aMain() {
        initUI();
        registerListeners();
        refreshPorts();
        loadLastSettings();

        // Стартуем таймер для автообновления (пока выключен)
        // Будет включён при открытии порта и отметке чекбокса
    }

    private void initUI() {
        // Настраиваем индикаторы — FlowLayout не растягивает, preferredSize фиксирует 16×16
        portStatusLamp.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        comLamp.setPreferredSize(new Dimension(16, 16));
        comLamp.setMinimumSize(new Dimension(16, 16));
        comLamp.setMaximumSize(new Dimension(16, 16));
        portStatusLamp.add(comLamp);
        deviceStatusLamp.setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));
        deviceLamp.setPreferredSize(new Dimension(16, 16));
        deviceLamp.setMinimumSize(new Dimension(16, 16));
        deviceLamp.setMaximumSize(new Dimension(16, 16));
        deviceStatusLamp.add(deviceLamp);

        comLamp.setLampColor(Color.RED);
        deviceLamp.setLampColor(Color.RED);
        portStatusLabel.setText("Порт закрыт");
        deviceStatusLabel.setText("Устройство не обнаружено");


        // Заполняем списки скоростей
        Integer[] baudValues = {1200, 2400, 4800, 9600, 19200, 38400};
        for (int b : baudValues) {
            portComboSpeed.addItem(b);
            baudCombo.addItem(b);
        }
        portComboSpeed.setSelectedItem(9600);
        baudCombo.setSelectedItem(9600);

        // Единицы измерения (коды согласно документации)
        String[] units = {"МПа", "кПа", "Па", "бар", "мбар", "кгс/см²", "PSI", "мH₂O",
                "ммH₂O", "дюйм H₂O", "фут H₂O", "ммHg", "дюйм Hg", "атм", "Торр",
                "м", "см", "мм", "кг", "°C", "pH", "°F", "пусто"};
        for (String u : units) unitCombo.addItem(u);

        // Десятичные знаки 0..4
        for (int i = 0; i <= 4; i++) decimalCombo.addItem(i);
        decimalCombo.setSelectedItem(1);

        // PV – большой шрифт
        pvValueLabel.setFont(new Font("Monospaced", Font.BOLD, 48));
        pvValueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        pvValueLabel.setText("----");

        // График (осциллограф)
        oscilloscopePanel = new OscilloscopePanel(2000); // окно 2 сек
        // По умолчанию он добавлен в панель oscilloscopePlaceholder (см. форму)
    }

    private void registerListeners() {
        refreshBtn.addActionListener(e -> refreshPorts());
        openPortBtn.addActionListener(e -> openPort());
        closePortBtn.addActionListener(e -> closePort());
        readPvBtn.addActionListener(e -> readPv());
        readParamsBtn.addActionListener(e -> readAllParams());
        writeParamsBtn.addActionListener(e -> writeAllParams());
        saveBtn.addActionListener(e -> saveParameters());
        restoreBtn.addActionListener(e -> restoreFactory());
        autoRefreshCheck.addActionListener(e -> toggleAutoRefresh());
        runSearch_BT.addActionListener(e -> startDeviceSearch());

        // Слушатель ответов от устройства
        service.addResponseListener(this::handleResponse);
    }

    private void refreshPorts() {
        portComboSelect.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort p : ports) {
            portComboSelect.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portComboSelect.getItemCount() == 0) {
            portComboSelect.addItem("Нет доступных портов");
        }
    }

    private void loadLastSettings() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        String lastPort = prefs.get(LAST_PORT, "");
        int lastBaud = prefs.getInt(LAST_BAUD, 9600);
        if (!lastPort.isEmpty()) {
            for (int i = 0; i < portComboSelect.getItemCount(); i++) {
                if (portComboSelect.getItemAt(i).startsWith(lastPort)) {
                    portComboSelect.setSelectedIndex(i);
                    break;
                }
            }
        }
        portComboSpeed.setSelectedItem(lastBaud);
        baudCombo.setSelectedItem(lastBaud);
    }

    private void saveLastSettings(String portName, int baud) {
        Preferences.userRoot().node(PREFS_NODE).put(LAST_PORT, portName);
        Preferences.userRoot().node(PREFS_NODE).putInt(LAST_BAUD, baud);
    }

    private void openPort() {
        String selected = (String) portComboSelect.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(mainPanel, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        int baud = (int) portComboSpeed.getSelectedItem();

        comLamp.setLampColor(Color.YELLOW);
        portStatusLabel.setText("Открытие порта...");
        openPortBtn.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            private boolean opened;

            @Override
            protected Boolean doInBackground() throws Exception {
                opened = service.openPort(portName, baud);
                if (opened) {
                    byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0000, (short) 1);
                    sendRequest(req);

                }
                return opened;
            }

            @Override
            protected void done() {
                if (opened) {
                    saveLastSettings(portName, baud);
                    comLamp.setLampColor(Color.GREEN);
                    portStatusLabel.setText("Порт открыт");
                    closePortBtn.setEnabled(true);
                    readPvBtn.setEnabled(true);
                    readParamsBtn.setEnabled(true);
                    writeParamsBtn.setEnabled(true);
                    saveBtn.setEnabled(true);
                    restoreBtn.setEnabled(true);
                    autoRefreshCheck.setEnabled(true);
                    deviceLamp.setLampColor(Color.YELLOW);
                    deviceStatusLabel.setText("Поиск устройства...");
                } else {
                    comLamp.setLampColor(Color.RED);
                    portStatusLabel.setText("Ошибка открытия порта");
                    openPortBtn.setEnabled(true);
                }
            }
        }.execute();
    }

    private void closePort() {
        stopAutoRefresh();
        service.closePort();
        comLamp.setLampColor(Color.RED);
        portStatusLabel.setText("Порт закрыт");
        deviceLamp.setLampColor(Color.RED);
        deviceStatusLabel.setText("Устройство не обнаружено");
        openPortBtn.setEnabled(true);
        closePortBtn.setEnabled(false);
        readPvBtn.setEnabled(false);
        readParamsBtn.setEnabled(false);
        writeParamsBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        restoreBtn.setEnabled(false);
        autoRefreshCheck.setEnabled(false);
        pvValueLabel.setText("----");
        unitLabel.setText("ед.");
        decimalLabel.setText("0");
    }

    // ---------- Отправка команд ----------

    private void sendRequest(byte[] request) {
        requestQueue.add(request);
        service.sendRequest(request);
    }

    // Чтение PV (регистр 0x0004)
    private void readPv() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0004, (short) 1);
        sendRequest(req);
    }

    // Чтение адреса (регистр 0x0000)
    private void readAddress() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0000, (short) 1);
        sendRequest(req);
    }

    // Чтение всех параметров — одним запросом (0x0000-0x000C)
    private void readAllParams() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0000, (short) 13);
        sendRequest(req);
    }

    private void readBaudRate() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0001, (short) 1);
        sendRequest(req);
    }

    private void readUnit() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0002, (short) 1);
        sendRequest(req);
    }

    private void readDecimalPoints() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x0003, (short) 1);
        sendRequest(req);
    }

    private void readOffset() {
        byte[] req = registry.buildReadRequest((byte) currentAddress, (byte) 0x03, (short) 0x000C, (short) 1);
        sendRequest(req);
    }

    // Запись всех параметров
    private void writeAllParams() {
        try {
            int addr = Integer.parseInt(addressField.getText().trim());
            if (addr < 1 || addr > 247) {
                JOptionPane.showMessageDialog(mainPanel, "Адрес должен быть 1..247");
                return;
            }
            int baud = (int) baudCombo.getSelectedItem();
            int baudCode = getBaudCode(baud);
            int unitIdx = unitCombo.getSelectedIndex();
            int decimal = (int) decimalCombo.getSelectedItem();
            int offset = Integer.parseInt(offsetField.getText().trim());
            if (offset < -32768 || offset > 32767) {
                JOptionPane.showMessageDialog(mainPanel, "Смещение вне диапазона (-32768..32767)");
                return;
            }

            // Последовательная запись
            writeRegister(0x0000, addr);
            currentAddress = addr;
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            writeRegister(0x0001, baudCode);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            writeRegister(0x0002, unitIdx);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            writeRegister(0x0003, decimal);
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
            writeRegister(0x000C, offset);

            JOptionPane.showMessageDialog(mainPanel, "Параметры записаны (требуется сохранение)");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(mainPanel, "Некорректный ввод числовых значений");
        }
    }

    private void writeRegister(int regAddr, int value) {
        byte[] req = registry.buildWriteSingleRegisterRequest((byte) currentAddress, (short) regAddr, (short) value);
        sendRequest(req);
    }

    private void saveParameters() {
        byte[] req = registry.buildWriteSingleRegisterRequest((byte) currentAddress, (short) 0x000F, (short) 0);
        sendRequest(req);
        JOptionPane.showMessageDialog(mainPanel, "Команда сохранения отправлена");
    }

    private void restoreFactory() {
        byte[] req = registry.buildWriteSingleRegisterRequest((byte) currentAddress, (short) 0x0010, (short) 1);
        sendRequest(req);
        JOptionPane.showMessageDialog(mainPanel, "Команда сброса отправлена. После сброса устройство может изменить адрес/скорость, переподключитесь.");
    }

    private int getBaudCode(int baud) {
        switch (baud) {
            case 1200:
                return 0;
            case 2400:
                return 1;
            case 4800:
                return 2;
            case 9600:
                return 3;
            case 19200:
                return 4;
            case 38400:
                return 5;
            default:
                return 3;
        }
    }

    // ---------- Обработка ответов ----------

    private void handleResponse(byte[] response) {
        log.info("Ответ: " + MyUtilities.bytesToHexString(response));

        // Проверяем CRC – если не прошла, метод сам залогирует ошибку
        if (!Qdl80aCommandRegistry.validateCRC(response)) {
            return;
        }

        // Определяем функцию
        int func = response[1] & 0xFF;
        if ((func & 0x80) != 0) {
            // Ошибка
            int errCode = response[2] & 0xFF;
            SwingUtilities.invokeLater(() -> {
                statusBar.setText("Ошибка Modbus: код " + errCode);
            });
            return;
        }

        // Парсим в зависимости от ожидаемого запроса
        if (func == 0x03 || func == 0x04) {
            byte[] sent = requestQueue.poll();
            if (sent == null) {
                log.warn("Ответ без ожидающего запроса: " + MyUtilities.bytesToHexString(response));
                return;
            }
            int dataLen = response[2] & 0xFF;
            int regCount = dataLen / 2;
            int reqAddr = ((sent[2] & 0xFF) << 8) | (sent[3] & 0xFF);
            int reqQuantity = ((sent[4] & 0xFF) << 8) | (sent[5] & 0xFF);
            if (reqQuantity == regCount) {
                for (int i = 0; i < regCount; i++) {
                    int addr = reqAddr + i;
                    int hi = response[3 + i * 2] & 0xFF;
                    int lo = response[3 + i * 2 + 1] & 0xFF;
                    short val = (short) ((hi << 8) | lo);
                    updateUiForRegister(addr, val);
                }
            }
        } else if (func == 0x06) {
            // Ответ на запись одного регистра
            int regAddr = ((response[2] & 0xFF) << 8) | (response[3] & 0xFF);
            int value = ((response[4] & 0xFF) << 8) | (response[5] & 0xFF);
            SwingUtilities.invokeLater(() -> {
                if (regAddr == 0x0000) {
                    currentAddress = value;
                    addressField.setText(String.valueOf(value));
                } else if (regAddr == 0x0001) {
                    int baud = getBaudFromCode(value);
                    baudCombo.setSelectedItem(baud);
                } else if (regAddr == 0x0002) {
                    if (value < unitCombo.getItemCount()) {
                        unitCombo.setSelectedIndex(value);
                        unitLabel.setText("Ед.: " + unitCombo.getItemAt(value));
                    }
                } else if (regAddr == 0x0003) {
                    if (value >= 0 && value <= 4) {
                        decimalCombo.setSelectedItem(value);
                        decimalLabel.setText("Дес.: " + value);
                    }
                } else if (regAddr == 0x000C) {
                    offsetField.setText(String.valueOf(value));
                }
                statusBar.setText("Запись успешна (регистр 0x" + String.format("%04X", regAddr) + " = " + value + ")");
            });
        }
    }

    private void updateUiForRegister(int regAddr, short value) {
        SwingUtilities.invokeLater(() -> {
            switch (regAddr) {
                case 0x0000: // адрес
                    int addr = value & 0xFFFF;
                    currentAddress = addr;  // сохраняем адрес
                    addressField.setText(String.valueOf(addr));
                    deviceLamp.setLampColor(Color.GREEN);
                    deviceStatusLabel.setText("Устройство найдено (адрес " + addr + ")");
                    break;
                case 0x0001: // скорость
                    int baudCode = value & 0xFFFF;
                    int baud = getBaudFromCode(baudCode);
                    baudCombo.setSelectedItem(baud);
                    break;
                case 0x0002: // единицы
                    int unitIdx = value & 0xFFFF;
                    if (unitIdx < unitCombo.getItemCount()) {
                        unitCombo.setSelectedIndex(unitIdx);
                        unitLabel.setText("Ед.: " + unitCombo.getItemAt(unitIdx));
                    }
                    break;
                case 0x0003: // десятичные
                    int dp = value & 0xFFFF;
                    if (dp >= 0 && dp <= 4) {
                        decimalCombo.setSelectedItem(dp);
                        decimalLabel.setText("Дес.: " + dp);
                    }
                    break;
                case 0x0004: // PV
                    lastPvValue = value & 0xFFFF;
                    int dpCurrent = (int) decimalCombo.getSelectedItem();
                    double realVal = lastPvValue / Math.pow(10, dpCurrent);
                    pvValueLabel.setText(String.format("%.2f", realVal));
                    break;
                case 0x000C: // смещение нуля
                    offsetField.setText(String.valueOf(value));
                    break;
                default:
                    break;
            }
        });
    }

    private int getBaudFromCode(int code) {
        switch (code) {
            case 0:
                return 1200;
            case 1:
                return 2400;
            case 2:
                return 4800;
            case 3:
                return 9600;
            case 4:
                return 19200;
            case 5:
                return 38400;
            default:
                return 9600;
        }
    }

    // ---------- Автообновление ----------

    private void toggleAutoRefresh() {
        if (autoRefreshCheck.isSelected()) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshActive) return;
        autoRefreshActive = true;
        scheduler.scheduleAtFixedRate(() -> {
            if (autoRefreshActive) {
                readAllParams();
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);
    }

    private void stopAutoRefresh() {
        autoRefreshActive = false;
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pvValueLabel = new JLabel();
        Font pvValueLabelFont = this.$$$getFont$$$(null, Font.BOLD, 48, pvValueLabel.getFont());
        if (pvValueLabelFont != null) pvValueLabel.setFont(pvValueLabelFont);
        pvValueLabel.setHorizontalAlignment(0);
        pvValueLabel.setText("----");
        panel1.add(pvValueLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        unitLabel = new JLabel();
        Font unitLabelFont = this.$$$getFont$$$(null, -1, 16, unitLabel.getFont());
        if (unitLabelFont != null) unitLabel.setFont(unitLabelFont);
        unitLabel.setText("Ед.: кПа");
        panel2.add(unitLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decimalLabel = new JLabel();
        Font decimalLabelFont = this.$$$getFont$$$(null, -1, 16, decimalLabel.getFont());
        if (decimalLabelFont != null) decimalLabel.setFont(decimalLabelFont);
        decimalLabel.setText("Дес.: 1");
        panel2.add(decimalLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        oscilloscopePanel = new JPanel();
        panel1.add(oscilloscopePanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        comControl = new JPanel();
        comControl.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(comControl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(18, 1, new Insets(0, 0, 0, 0), -1, -1));
        comControl.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("COM порт:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSelect = new JComboBox();
        panel3.add(portComboSelect, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Скорость, бод:");
        panel3.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSpeed = new JComboBox();
        panel3.add(portComboSpeed, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        refreshBtn = new JButton();
        refreshBtn.setText("Обновить список портов");
        panel3.add(refreshBtn, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        openPortBtn = new JButton();
        openPortBtn.setText("Открыть порт");
        panel3.add(openPortBtn, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        closePortBtn = new JButton();
        closePortBtn.setText("Закрыть порт");
        panel3.add(closePortBtn, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        comLampContainer = new JPanel();
        comLampContainer.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(comLampContainer, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        comLampContainer.add(statusPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(320, -1), new Dimension(320, -1), new Dimension(320, -1), 0, false));
        portStatusLamp = new JPanel();
        portStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        statusPanel.add(portStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(16, 16), new Dimension(16, 16), new Dimension(16, 16), 0, false));
        portStatusLabel = new JLabel();
        portStatusLabel.setText("Порт закрыт");
        statusPanel.add(portStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        comLampContainer.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        deviceLampContainer = new JPanel();
        deviceLampContainer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(deviceLampContainer, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deviceLampSubContainer = new JPanel();
        deviceLampSubContainer.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        deviceLampContainer.add(deviceLampSubContainer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(320, -1), new Dimension(320, -1), new Dimension(320, -1), 0, false));
        deviceStatusLamp = new JPanel();
        deviceStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        deviceLampSubContainer.add(deviceStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(16, 16), new Dimension(16, 16), new Dimension(16, 16), 0, false));
        deviceStatusLabel = new JLabel();
        deviceStatusLabel.setText("Устройство подключено");
        deviceLampSubContainer.add(deviceStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        Font label3Font = this.$$$getFont$$$(null, Font.BOLD, -1, label3.getFont());
        if (label3Font != null) label3.setFont(label3Font);
        label3.setText("Параметры устройства");
        panel3.add(label3, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(4, 2, new Insets(5, 0, 0, 0), 5, 2));
        panel3.add(panel4, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Адрес:");
        panel4.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        addressField = new JTextField();
        addressField.setText("1");
        panel4.add(addressField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Скорость:");
        panel4.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        baudCombo = new JComboBox();
        panel4.add(baudCombo, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Единицы:");
        panel4.add(label6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        unitCombo = new JComboBox();
        panel4.add(unitCombo, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Десятичных:");
        panel4.add(label7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        decimalCombo = new JComboBox();
        panel4.add(decimalCombo, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 5, 2));
        panel3.add(panel5, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("Смещение нуля:");
        panel5.add(label8, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        offsetField = new JTextField();
        offsetField.setText("0");
        panel5.add(offsetField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(5, 0, 0, 0), 5, 2));
        panel3.add(panel6, new GridConstraints(14, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        readParamsBtn = new JButton();
        readParamsBtn.setText("Читать");
        panel6.add(readParamsBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        writeParamsBtn = new JButton();
        writeParamsBtn.setText("Записать");
        panel6.add(writeParamsBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        saveBtn = new JButton();
        saveBtn.setText("Сохранить");
        panel6.add(saveBtn, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), 5, 2));
        panel3.add(panel7, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        restoreBtn = new JButton();
        restoreBtn.setText("Сброс к заводским");
        panel7.add(restoreBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        readPvBtn = new JButton();
        readPvBtn.setText("Обновить PV");
        panel7.add(readPvBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        autoRefreshCheck = new JCheckBox();
        autoRefreshCheck.setText("Автообновление PV");
        panel3.add(autoRefreshCheck, new GridConstraints(16, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusBar = new JLabel();
        statusBar.setText("Готов");
        panel3.add(statusBar, new GridConstraints(17, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        devSearch = new JPanel();
        devSearch.setLayout(new GridLayoutManager(5, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel3.add(devSearch, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(300, -1), new Dimension(300, -1), 0, false));
        devSearch.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        addressUnknown_CB = new JCheckBox();
        addressUnknown_CB.setText("Адрес неизвестен");
        devSearch.add(addressUnknown_CB, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        devSearch.add(spacer3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        portUnknown_CB = new JCheckBox();
        portUnknown_CB.setText("Порт неизвестен");
        devSearch.add(portUnknown_CB, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        speedUnknown_CB = new JCheckBox();
        speedUnknown_CB.setText("Скорость неизвестна");
        devSearch.add(speedUnknown_CB, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        runSearch_BT = new JButton();
        runSearch_BT.setText("Поиск устройства");
        devSearch.add(runSearch_BT, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        noteForUser = new JTextArea();
        noteForUser.setEditable(false);
        noteForUser.setEnabled(false);
        noteForUser.setFocusable(false);
        noteForUser.setLineWrap(true);
        noteForUser.setText("Поиск прибора. Перед началом поиска необходимо закрыть все порты. Отключить лишннее оборудование от компьютера. Это влияет на качество и скорость поиска.");
        noteForUser.setWrapStyleWord(true);
        devSearch.add(noteForUser, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, 1, new Dimension(200, -1), new Dimension(200, 50), new Dimension(200, -1), 0, false));
    }

    /**
     * @noinspection ALL
     */
    private Font $$$getFont$$$(String fontName, int style, int size, Font currentFont) {
        if (currentFont == null) return null;
        String resultName;
        if (fontName == null) {
            resultName = currentFont.getName();
        } else {
            Font testFont = new Font(fontName, Font.PLAIN, 10);
            if (testFont.canDisplay('a') && testFont.canDisplay('1')) {
                resultName = fontName;
            } else {
                resultName = currentFont.getName();
            }
        }
        Font font = new Font(resultName, style >= 0 ? style : currentFont.getStyle(), size >= 0 ? size : currentFont.getSize());
        boolean isMac = System.getProperty("os.name", "").toLowerCase(Locale.ENGLISH).startsWith("mac");
        Font fontWithFallback = isMac ? new Font(font.getFamily(), font.getStyle(), font.getSize()) : new StyleContext().getFont(font.getFamily(), font.getStyle(), font.getSize());
        return fontWithFallback instanceof FontUIResource ? fontWithFallback : new FontUIResource(fontWithFallback);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    // ---------- Вспомогательные классы ----------

    private static class LampIndicator extends JPanel {
        private Color lampColor = Color.GRAY;

        public LampIndicator() {
            setOpaque(false);
            setPreferredSize(new Dimension(16, 16));
            setMinimumSize(new Dimension(16, 16));
            setMaximumSize(new Dimension(16, 16));
        }

        public void setLampColor(Color color) {
            this.lampColor = color;
            repaint();
        }

        public Color getLampColor() {
            return lampColor;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            g2.setColor(lampColor);
            g2.fillOval(x + 1, y + 1, size - 2, size - 2);
            g2.dispose();
        }
    }

    // Осциллограф (упрощённая версия)
    private static class OscilloscopePanel extends JPanel {
        private final int windowMs;
        private final List<DataPoint> data = new CopyOnWriteArrayList<>();
        private double minVal = -1000, maxVal = 1000;

        OscilloscopePanel(int windowMs) {
            this.windowMs = windowMs;
            setPreferredSize(new Dimension(600, 200));
            setBackground(new Color(16, 16, 24));
        }

        synchronized void addDataPoint(double value) {
            data.add(new DataPoint(System.currentTimeMillis(), value));
            long cutoff = System.currentTimeMillis() - windowMs - 500;
            data.removeIf(dp -> dp.timestamp < cutoff);
            if (!data.isEmpty()) {
                double min = data.stream().mapToDouble(dp -> dp.value).min().orElse(-1000);
                double max = data.stream().mapToDouble(dp -> dp.value).max().orElse(1000);
                double range = max - min;
                if (range < 1) range = 1;
                minVal = min - range * 0.1;
                maxVal = max + range * 0.1;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();
            long now = System.currentTimeMillis();
            long start = now - windowMs;

            // Сетка
            g2.setColor(new Color(36, 36, 48));
            for (int i = 0; i < 5; i++) {
                int y = h * i / 5;
                g2.drawLine(0, y, w, y);
            }
            for (int i = 0; i < 10; i++) {
                int x = w * i / 10;
                g2.drawLine(x, 0, x, h);
            }

            if (data.isEmpty()) {
                g2.setColor(Color.GRAY);
                g2.drawString("Нет данных", 10, 20);
                return;
            }

            g2.setColor(new Color(0, 200, 100));
            g2.setStroke(new BasicStroke(2));

            double range = maxVal - minVal;
            if (range == 0) range = 1;

            DataPoint prev = null;
            for (DataPoint dp : data) {
                if (dp.timestamp < start) continue;
                int x = (int) ((dp.timestamp - start) * w / (double) windowMs);
                if (x > w) break;
                int y = (int) (h - (dp.value - minVal) / range * (h - 20) - 10);
                y = Math.min(h - 5, Math.max(5, y));
                if (prev != null) {
                    int prevX = (int) ((prev.timestamp - start) * w / (double) windowMs);
                    int prevY = (int) (h - (prev.value - minVal) / range * (h - 20) - 10);
                    prevY = Math.min(h - 5, Math.max(5, prevY));
                    g2.drawLine(prevX, prevY, x, y);
                }
                prev = dp;
            }
        }

        private static class DataPoint {
            final long timestamp;
            final double value;

            DataPoint(long ts, double val) {
                timestamp = ts;
                value = val;
            }
        }
    }

    // ---------- Методы для GUI Designer ----------

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void startDeviceSearch() {
        boolean scanPorts = portUnknown_CB.isSelected();
        boolean scanSpeeds = speedUnknown_CB.isSelected();
        boolean scanAddrs = addressUnknown_CB.isSelected();

        String selectedPortName = null;
        Object portItem = portComboSelect.getSelectedItem();
        if (portItem != null) {
            String val = portItem.toString();
            selectedPortName = val.contains(" — ") ? val.split(" — ")[0].trim() : val;
        }
        int baud = portComboSpeed.getSelectedItem() instanceof Integer
                ? (int) portComboSpeed.getSelectedItem() : 9600;

        DevicesSearch searchDialog = new DevicesSearch(
                (Frame) SwingUtilities.getWindowAncestor(mainPanel),
                scanPorts, scanSpeeds, scanAddrs,
                selectedPortName, baud
        );
        searchDialog.startSearch();
        searchDialog.setVisible(true);

        List<DevicesSearch.FoundDevice> found = searchDialog.getFoundDevices();
        if (!found.isEmpty()) {
            DevicesSearch.FoundDevice first = found.get(0);
            for (int i = 0; i < portComboSelect.getItemCount(); i++) {
                String item = portComboSelect.getItemAt(i);
                if (item.startsWith(first.port)) {
                    portComboSelect.setSelectedIndex(i);
                    break;
                }
            }
            portComboSpeed.setSelectedItem(first.baud);

            currentAddress = first.address;
            addressField.setText(String.valueOf(first.address));
            statusBar.setText("Найдено устройство: порт " + first.port
                    + ", скорость " + first.baud + ", адрес " + first.address);
            log.info("Device search completed, found device: " + first.port
                    + " @" + first.baud + " addr=" + first.address);
        } else {
            statusBar.setText("Поиск завершён — устройств не найдено");
        }
    }

    public void shutdown() {
        stopAutoRefresh();
        service.shutdown();
    }


}