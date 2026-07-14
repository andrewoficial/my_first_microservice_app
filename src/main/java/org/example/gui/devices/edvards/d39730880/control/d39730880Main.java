package org.example.gui.devices.edvards.d39730880.control;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protEdwardsD397.EdwardsCommunicationService;
import org.example.device.protEdwardsD397.EdwardsD397CommandRegistry;
import org.example.gui.devices.edvards.d39730880.util.EdwardsDevicesSearch;
import org.example.utilites.MyUtilities;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class d39730880Main {
    private static final String PREFS_NODE = "org/example/gui/edwards/tic";
    private static final String LAST_PORT = "lastPort";
    private static final String LAST_BAUD = "lastBaud";

    private JPanel mainPanel;
    private JLabel pvValueLabel;
    private JLabel unitLabel;
    private JLabel decimalLabel;
    private JPanel oscilloscopePanel;
    private JPanel comControl;
    private JComboBox<String> portComboSelect;
    private JComboBox<Integer> portComboSpeed;
    private JButton refreshBtn;
    private JButton openPortBtn;
    private JButton closePortBtn;
    private JPanel comLampContainer;
    private JPanel statusPanel;
    private JPanel portStatusLamp;
    private JLabel portStatusLabel;
    private JPanel deviceLampContainer;
    private JPanel deviceLampSubContainer;
    private JPanel deviceStatusLamp;
    private JLabel deviceStatusLabel;
    private JButton readPvBtn;
    private JCheckBox autoRefreshCheck;
    private JLabel statusBar;
    private JPanel devSearch;
    private JCheckBox addressUnknown_CB;
    private JCheckBox portUnknown_CB;
    private JCheckBox speedUnknown_CB;
    private JButton runSearch_BT;
    private JTextArea noteForUser;
    private JPanel turboPumpJpanel;
    private JPanel gaugesJpanel;
    private JPanel backingPumpJpanel;
    private JPanel temperatureAndOtherJpane;
    private JPanel dataIndication;
    private JLabel turboStatusL;
    private JButton turboOnBtn;
    private JButton turboOffBtn;
    private JButton turboSlaveBtn;
    private JButton turboStartDelayBtn;
    private JLabel turboSpeedL;
    private JLabel turboPowerL;
    private JButton turboStandbyBtn;
    private JButton turboNormBtn;
    private JLabel turboCycleL;
    private JButton gauge1OnBtn;
    private JButton gauge1OffBtn;
    private JButton gauge1ZeroBtn;
    private JButton gauge1CalBtn;
    private JButton gauge1DegasBtn;
    private JLabel gauge1ValueL;
    private JLabel gauge2ValueL;
    private JLabel gauge3ValueL;
    private JButton backingOnBtn;
    private JButton backingOffBtn;
    private JButton relay1OnBtn;
    private JButton relay1OffBtn;
    private JLabel backingStatusL;
    private JLabel relay1StatusL;
    private JButton readAllGaugesBtn;
    private JButton readAllBtn;
    private JLabel psTempL;
    private JLabel internalTempL;

    private final EdwardsCommunicationService service = new EdwardsCommunicationService();
    private final EdwardsD397CommandRegistry registry = new EdwardsD397CommandRegistry();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean autoRefreshActive = false;

    private final LampIndicator comLamp = new LampIndicator();
    private final LampIndicator deviceLamp = new LampIndicator();

    private JTextArea systemStatusArea;

    public d39730880Main() {
        initUI();
        registerListeners();
        refreshPorts();
        loadLastSettings();
    }

    private void initUI() {
        portStatusLamp.setLayout(new BorderLayout());
        portStatusLamp.add(comLamp, BorderLayout.CENTER);

        deviceStatusLamp.setLayout(new BorderLayout());
        deviceStatusLamp.add(deviceLamp, BorderLayout.CENTER);

        systemStatusArea = new JTextArea(8, 40);
        systemStatusArea.setEditable(false);
        systemStatusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        oscilloscopePanel.setLayout(new BorderLayout());
        oscilloscopePanel.setBorder(BorderFactory.createTitledBorder("Системный статус"));
        oscilloscopePanel.add(new JScrollPane(systemStatusArea), BorderLayout.CENTER);

        noteForUser.setText("Поиск прибора Edwards TIC (D397).\nПеред началом поиска необходимо\nзакрыть все порты.");

        // fix form text values with stray quotes
        turboSpeedL.setText("Speed: -- %");
        turboPowerL.setText("Power: -- W");
        turboCycleL.setText("Наработка: -- h");
        gauge1ValueL.setText("G1: --");
        gauge2ValueL.setText("G2: --");
        gauge3ValueL.setText("G3: --");
        backingStatusL.setText("Backing: --");
        relay1StatusL.setText("Relay1: --");
        psTempL.setText("PS Temp: -- °C");
        internalTempL.setText("Internal: -- °C");
        turboOnBtn.setText("Вкл турбонасос");
        turboOffBtn.setText("Выкл турбонасос");
        turboSlaveBtn.setText("Slave setup");
        turboStartDelayBtn.setText("Старт. задержка");
        turboStandbyBtn.setText("Standby вкл");
        turboNormBtn.setText("Normal");
        gauge1OnBtn.setText("G1 Вкл");
        gauge1OffBtn.setText("G1 Выкл");
        gauge1ZeroBtn.setText("G1 Zero");
        gauge1CalBtn.setText("G1 Cal");
        gauge1DegasBtn.setText("G1 Degas");
        backingOnBtn.setText("Вкл");
        backingOffBtn.setText("Выкл");
        relay1OnBtn.setText("Relay1 Вкл");
        relay1OffBtn.setText("Relay1 Выкл");
        readAllGaugesBtn.setText("Все датчики");
        readAllBtn.setText("Читать всё");

        portComboSpeed.removeAllItems();
        portComboSpeed.addItem(9600);
        portComboSpeed.addItem(19200);
        portComboSpeed.addItem(38400);
        portComboSpeed.setSelectedItem(9600);

        closePortBtn.setEnabled(false);
        runSearch_BT.setEnabled(false);

        comLamp.setPreferredSize(new Dimension(16, 16));
        deviceLamp.setPreferredSize(new Dimension(16, 16));
        comLamp.setLampColor(Color.RED);
        deviceLamp.setLampColor(Color.RED);
        portStatusLabel.setText("Порт закрыт");
        deviceStatusLabel.setText("Устройство не подключено");
    }

    private void registerListeners() {
        refreshBtn.addActionListener(e -> refreshPorts());
        openPortBtn.addActionListener(e -> openPort());
        closePortBtn.addActionListener(e -> closePort());

        runSearch_BT.addActionListener(e -> startDeviceSearch());

        // Turbo pump
        turboOnBtn.addActionListener(e -> sendAndLog("IC904 1"));
        turboOffBtn.addActionListener(e -> sendAndLog("IC904 0"));
        turboSlaveBtn.addActionListener(e -> sendAndLog("?S00904 4"));
        turboStartDelayBtn.addActionListener(e -> sendAndLog("?S00904 21"));
        turboStandbyBtn.addActionListener(e -> sendAndLog("IC908 1"));
        turboNormBtn.addActionListener(e -> sendAndLog("?V00907"));

        // Gauges
        gauge1OnBtn.addActionListener(e -> sendAndLog("IC913 1"));
        gauge1OffBtn.addActionListener(e -> sendAndLog("IC913 0"));
        gauge1ZeroBtn.addActionListener(e -> sendAndLog("IC913 3"));
        gauge1CalBtn.addActionListener(e -> sendAndLog("IC913 4"));
        gauge1DegasBtn.addActionListener(e -> sendAndLog("IC913 5"));

        // Backing & Relay
        backingOnBtn.addActionListener(e -> sendAndLog("IC910 1"));
        backingOffBtn.addActionListener(e -> sendAndLog("IC910 0"));
        relay1OnBtn.addActionListener(e -> sendAndLog("IC916 1"));
        relay1OffBtn.addActionListener(e -> sendAndLog("IC916 0"));

        // Other
        readAllGaugesBtn.addActionListener(e -> sendAndLog("?V00940"));
        readAllBtn.addActionListener(e -> readAll());

        service.addResponseListener(this::handleResponse);
    }

    private void refreshPorts() {
        portComboSelect.removeAllItems();
        for (SerialPort p : SerialPort.getCommPorts()) {
            portComboSelect.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portComboSelect.getItemCount() == 0) {
            portComboSelect.addItem("Нет доступных портов");
        }
        runSearch_BT.setEnabled(portComboSelect.getItemCount() > 0
                && !portComboSelect.getItemAt(0).contains("Нет"));
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
            @Override
            protected Boolean doInBackground() {
                return service.openPort(portName, baud);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        saveLastSettings(portName, baud);
                        comLamp.setLampColor(Color.GREEN);
                        portStatusLabel.setText("Порт открыт");
                        closePortBtn.setEnabled(true);
                        deviceLamp.setLampColor(Color.YELLOW);
                        deviceStatusLabel.setText("Устройство подключено (ожидание ответа)");
                        statusBar.setText("Порт открыт. Можно отправлять команды.");

                        readSystemStatus();
                    } else {
                        comLamp.setLampColor(Color.RED);
                        portStatusLabel.setText("Ошибка открытия порта");
                        openPortBtn.setEnabled(true);
                    }
                } catch (Exception ex) {
                    log.error("Open port error", ex);
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
        deviceStatusLabel.setText("Устройство не подключено");
        openPortBtn.setEnabled(true);
        closePortBtn.setEnabled(false);
        systemStatusArea.setText("");
        statusBar.setText("Порт закрыт");
    }

    private void readSystemStatus() {
        sendAndLog("?V00902");
    }

    private void sendAndLog(String cmd) {
        service.sendCommand(cmd);
        statusBar.setText("→ " + cmd);
        log.info("TX: " + cmd);
    }

    private void readAll() {
        sendAndLog("?V00902");
        sleep(80);
        sendAndLog("?V00904");
        sleep(80);
        sendAndLog("?V00905");
        sleep(80);
        sendAndLog("?V00906");
        sleep(80);
        sendAndLog("?V00907");
        sleep(80);
        sendAndLog("?V00908");
        sleep(80);
        sendAndLog("?V00909");
        sleep(80);
        sendAndLog("?V00910");
        sleep(80);
        sendAndLog("?V00913");
        sleep(80);
        sendAndLog("?V00914");
        sleep(80);
        sendAndLog("?V00915");
        sleep(80);
        sendAndLog("?V00916");
        sleep(80);
        sendAndLog("?V00919");
        sleep(80);
        sendAndLog("?V00920");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {
        }
    }

    private void toggleAutoRefresh() {

    }

    private void startAutoRefresh() {
        if (autoRefreshActive) return;
        autoRefreshActive = true;
        scheduler.scheduleAtFixedRate(() -> {
            if (autoRefreshActive) {
                readSystemStatus();
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
    }

    private void stopAutoRefresh() {
        autoRefreshActive = false;
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

        EdwardsDevicesSearch searchDialog = new EdwardsDevicesSearch(
                (Frame) SwingUtilities.getWindowAncestor(mainPanel),
                scanPorts, scanSpeeds, scanAddrs,
                selectedPortName, baud
        );
        searchDialog.startSearch();
        searchDialog.setVisible(true);

        List<EdwardsDevicesSearch.FoundDevice> found = searchDialog.getFoundDevices();
        if (!found.isEmpty()) {
            EdwardsDevicesSearch.FoundDevice first = found.get(0);
            for (int i = 0; i < portComboSelect.getItemCount(); i++) {
                String item = portComboSelect.getItemAt(i);
                if (item.startsWith(first.port)) {
                    portComboSelect.setSelectedIndex(i);
                    break;
                }
            }
            portComboSpeed.setSelectedItem(first.baud);
            statusBar.setText("Найдено устройство: порт " + first.port
                    + ", скорость " + first.baud);
            log.info("Device search completed, found device: " + first.port
                    + " @" + first.baud);
        } else {
            statusBar.setText("Поиск завершён — устройств не найдено");
        }
    }

    private void handleResponse(byte[] response) {
        String text = new String(response).trim();
        log.info("Edwards RX: " + MyUtilities.bytesToHexString(response) + " -> " + text);

        SwingUtilities.invokeLater(() -> {
            int cmdId = extractCommandId(text);
            String data = extractData(text);
            String[] parts = data.split(";");



            switch (cmdId) {
                case 902:
                    systemStatusArea.setText("=== СИСТЕМНЫЙ СТАТУС (902) ===\n" + text);
                    deviceLamp.setLampColor(Color.GREEN);
                    deviceStatusLabel.setText("Устройство отвечает");
                    break;
                case 904:
                    turboStatusL.setText("Turbo: state=" + safe(parts, 0) + " alert=" + safe(parts, 1));
                    systemStatusArea.append("\n?V904 → " + data);
                    break;
                case 905:
                    turboSpeedL.setText("Speed: " + safe(parts, 0) + " %");
                    break;
                case 906:
                    turboPowerL.setText("Power: " + safe(parts, 0) + " W");
                    break;
                case 907:
                    turboNormBtn.setText("Normal: " + safe(parts, 0));
                    break;
                case 908:
                    turboStatusL.setText("Turbo: standby=" + safe(parts, 0));
                    break;
                case 909:
                    turboCycleL.setText("Наработка: " + safe(parts, 0) + " h");
                    break;
                case 910:
                    backingStatusL.setText("Backing: state=" + safe(parts, 0) + " alert=" + safe(parts, 1));
                    break;
                case 913:
                    gauge1ValueL.setText("G1: " + safe(parts, 0) + " (" + safe(parts, 1) + ")");
                    break;
                case 914:
                    gauge2ValueL.setText("G2: " + safe(parts, 0) + " (" + safe(parts, 1) + ")");
                    break;
                case 915:
                    gauge3ValueL.setText("G3: " + safe(parts, 0) + " (" + safe(parts, 1) + ")");
                    break;
                case 916:
                    relay1StatusL.setText("Relay1: state=" + safe(parts, 0) + " alert=" + safe(parts, 1));
                    break;
                case 919:
                    psTempL.setText("PS Temp: " + safe(parts, 0) + " °C");
                    break;
                case 920:
                    internalTempL.setText("Internal: " + safe(parts, 0) + " °C");
                    break;
                case 940:
                    systemStatusArea.append("\n\n=== ВСЕ ДАТЧИКИ (940) ===\n" + text);
                    break;
                default:
                    systemStatusArea.append("\n" + text);
                    break;
            }

            if (text.startsWith("*C") || text.startsWith("=C")) {
                statusBar.setText("✓ Команда выполнена: " + text);
            }
        });
    }

    private int extractCommandId(String text) {
        Matcher m = Pattern.compile("[=#*][CVS]\\s*(\\d{3,5})").matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception ignored) {
            }
        }
        return 0;
    }

    private String extractData(String text) {
        return text.replaceFirst("^[=#*][CVS]\\s*\\d+\\s*", "").trim();
    }

    private String safe(String[] parts, int idx) {
        if (idx < parts.length && !parts[idx].isEmpty()) return parts[idx];
        return "?";
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void shutdown() {
        stopAutoRefresh();
        service.shutdown();
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 3, new Insets(5, 5, 5, 5), -1, -1));
        panel1.add(mainPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        dataIndication = new JPanel();
        dataIndication.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(dataIndication, new GridConstraints(0, 1, 2, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(300, -1), new Dimension(300, -1), 0, false));
        turboPumpJpanel = new JPanel();
        turboPumpJpanel.setLayout(new GridLayoutManager(10, 1, new Insets(0, 0, 0, 0), -1, -1));
        dataIndication.add(turboPumpJpanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        turboStatusL = new JLabel();
        turboStatusL.setText("Turbo: --");
        turboPumpJpanel.add(turboStatusL, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboOnBtn = new JButton();
        turboOnBtn.setText("Вкл турбонасос\" → IC904 1");
        turboPumpJpanel.add(turboOnBtn, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboOffBtn = new JButton();
        turboOffBtn.setText("Выкл турбонасос\" → IC904 0");
        turboPumpJpanel.add(turboOffBtn, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboSlaveBtn = new JButton();
        turboSlaveBtn.setText("\"Slave setup\" → ?S00904 4");
        turboPumpJpanel.add(turboSlaveBtn, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboStartDelayBtn = new JButton();
        turboStartDelayBtn.setText("\"Старт. задержка\" → ?S00904 21");
        turboPumpJpanel.add(turboStartDelayBtn, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboSpeedL = new JLabel();
        turboSpeedL.setText("\"Speed: -- %\"");
        turboPumpJpanel.add(turboSpeedL, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboPowerL = new JLabel();
        turboPowerL.setText("\"Power: -- W\"");
        turboPumpJpanel.add(turboPowerL, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboStandbyBtn = new JButton();
        turboStandbyBtn.setText("\"Standby вкл\" → IC908 1");
        turboPumpJpanel.add(turboStandbyBtn, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboNormBtn = new JButton();
        turboNormBtn.setText("\"Normal\" → ?V00907");
        turboPumpJpanel.add(turboNormBtn, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        turboCycleL = new JLabel();
        turboCycleL.setText("\"Наработка: -- h\"");
        turboPumpJpanel.add(turboCycleL, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gaugesJpanel = new JPanel();
        gaugesJpanel.setLayout(new GridLayoutManager(8, 1, new Insets(0, 0, 0, 0), -1, -1));
        dataIndication.add(gaugesJpanel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        gauge1ValueL = new JLabel();
        gauge1ValueL.setText("\"G1: --\"");
        gaugesJpanel.add(gauge1ValueL, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge2ValueL = new JLabel();
        gauge2ValueL.setText("\"G2: --\"");
        gaugesJpanel.add(gauge2ValueL, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge3ValueL = new JLabel();
        gauge3ValueL.setText("\"G3: --\"");
        gaugesJpanel.add(gauge3ValueL, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge1OnBtn = new JButton();
        gauge1OnBtn.setText("\"G1 Вкл\"");
        gaugesJpanel.add(gauge1OnBtn, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge1OffBtn = new JButton();
        gauge1OffBtn.setText("\"G1 Выкл\"");
        gaugesJpanel.add(gauge1OffBtn, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge1ZeroBtn = new JButton();
        gauge1ZeroBtn.setText("\"G1 Zero\" → IC913 3");
        gaugesJpanel.add(gauge1ZeroBtn, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge1CalBtn = new JButton();
        gauge1CalBtn.setText("\"G1 Cal\" → IC913 4");
        gaugesJpanel.add(gauge1CalBtn, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        gauge1DegasBtn = new JButton();
        gauge1DegasBtn.setText("\"G1 Degas\" → IC913 5");
        gaugesJpanel.add(gauge1DegasBtn, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        backingPumpJpanel = new JPanel();
        backingPumpJpanel.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        dataIndication.add(backingPumpJpanel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        backingStatusL = new JLabel();
        backingStatusL.setText("\"Backing: --\"");
        backingPumpJpanel.add(backingStatusL, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        backingOnBtn = new JButton();
        backingOnBtn.setText("\"Вкл\" → IC910 1");
        backingPumpJpanel.add(backingOnBtn, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        backingOffBtn = new JButton();
        backingOffBtn.setText("\"Выкл\" → IC910 0");
        backingPumpJpanel.add(backingOffBtn, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        relay1StatusL = new JLabel();
        relay1StatusL.setText("\"Relay1: --\"");
        backingPumpJpanel.add(relay1StatusL, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        relay1OnBtn = new JButton();
        relay1OnBtn.setText("\"Relay1 Вкл\" → IC916 1");
        backingPumpJpanel.add(relay1OnBtn, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        relay1OffBtn = new JButton();
        relay1OffBtn.setText("\"Relay1 Выкл\" → IC916 0");
        backingPumpJpanel.add(relay1OffBtn, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        temperatureAndOtherJpane = new JPanel();
        temperatureAndOtherJpane.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        dataIndication.add(temperatureAndOtherJpane, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        psTempL = new JLabel();
        psTempL.setText("\"PS Temp: -- °C\"");
        temperatureAndOtherJpane.add(psTempL, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        internalTempL = new JLabel();
        internalTempL.setText("\"Internal: -- °C\"");
        temperatureAndOtherJpane.add(internalTempL, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        readAllGaugesBtn = new JButton();
        readAllGaugesBtn.setText("\"Все датчики\" → ?V00940");
        temperatureAndOtherJpane.add(readAllGaugesBtn, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        readAllBtn = new JButton();
        readAllBtn.setText("\"Читать всё\" (все ?V подряд)");
        temperatureAndOtherJpane.add(readAllBtn, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        oscilloscopePanel = new JPanel();
        dataIndication.add(oscilloscopePanel, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        comControl = new JPanel();
        comControl.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(comControl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(12, 1, new Insets(0, 0, 0, 0), -1, -1));
        comControl.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("COM порт:");
        panel2.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSelect = new JComboBox();
        panel2.add(portComboSelect, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Скорость, бод:");
        panel2.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSpeed = new JComboBox();
        panel2.add(portComboSpeed, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        refreshBtn = new JButton();
        refreshBtn.setText("Обновить список портов");
        panel2.add(refreshBtn, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        openPortBtn = new JButton();
        openPortBtn.setText("Открыть порт");
        panel2.add(openPortBtn, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        closePortBtn = new JButton();
        closePortBtn.setText("Закрыть порт");
        panel2.add(closePortBtn, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(400, -1), new Dimension(400, -1), new Dimension(400, -1), 0, false));
        comLampContainer = new JPanel();
        comLampContainer.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(comLampContainer, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        panel2.add(deviceLampContainer, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        panel2.add(spacer2, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        statusBar = new JLabel();
        statusBar.setText("Готов");
        panel2.add(statusBar, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        devSearch = new JPanel();
        devSearch.setLayout(new GridLayoutManager(5, 2, new Insets(5, 5, 5, 5), -1, -1));
        panel2.add(devSearch, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(300, -1), new Dimension(300, -1), 0, false));
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

    private static class LampIndicator extends JPanel {
        private Color color = Color.GRAY;

        public LampIndicator() {
            setPreferredSize(new Dimension(16, 16));
            setOpaque(false);
        }

        public void setLampColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 2;
            g2.setColor(color);
            g2.fillOval(1, 1, size, size);
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(1, 1, size, size);
            g2.dispose();
        }
    }
}
