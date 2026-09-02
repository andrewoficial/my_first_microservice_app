package org.example.gui.devices.esp32.kantser.emu.ble;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Панель управления ESP_KANTSER_BLE_EMU (ESP32 Kantser BLE simulator).
 * Протокол: 115200 8N1, CR.
 */
@Slf4j
public class KantserBleMain {

    private static final String PREFS_NODE = "org/example/gui/esp32/kantser/emu/ble";
    private static final String LAST_PORT = "lastPort";
    private static final String LAST_BAUD = "lastBaud";
    private static final int DEFAULT_BAUD = KantserBleCommunicationService.DEFAULT_BAUD;

    private static final Color GREEN = new Color(0, 180, 80);
    private static final Color RED = new Color(200, 40, 40);
    private static final Color ORANGE = new Color(180, 120, 0);
    private static final Color GRAY = Color.DARK_GRAY;
    private static final Color HEADER_FG = Color.WHITE;
    private static final Color HEADER_BG = new Color(40, 42, 46);
    private static final Color HEADER_EMPTY = new Color(160, 160, 160);

    private static final Pattern MAC_PATTERN = Pattern.compile(
            "[0-9A-Fa-f]{2}([:-])[0-9A-Fa-f]{2}(\\1[0-9A-Fa-f]{2}){4}");

    private static final String[] MAC_DEFAULT = {"00:00:00:00:00:00"};

    private volatile String fwDevice = "";
    private volatile String fwVersion = "";

    private JPanel mainPanel;
    private JComboBox<String> portComboSelect;
    private JComboBox<Integer> portComboSpeed;
    private JButton refreshBtn;
    private JButton openPortBtn;
    private JButton closePortBtn;
    private JPanel comLampContainer;
    private JLabel portStatusLabel;
    private JPanel commandsPlaceholder;
    private JTextArea logArea;
    private JLabel statusBar;
    private JCheckBox evenParityCb;
    private JPanel devInfo;
    private JPanel fwInfo;
    private JLabel fwDeviceLbl;
    private JLabel fwDeviceVal;
    private JLabel fwVersionLbl;
    private JLabel fwVersionVal;
    private JLabel bleStatusLbl;
    private JLabel bleStatusVal;
    private JLabel advStateLbl;
    private JLabel advStateVal;
    private JLabel conc1Lbl;
    private JLabel conc1Val;
    private JLabel conc2Lbl;
    private JLabel conc2Val;
    private JLabel conc3Lbl;
    private JLabel conc3Val;
    private JLabel conc4Lbl;
    private JLabel conc4Val;
    private JLabel tempOffsetLbl;
    private JLabel tempOffsetVal;
    private JLabel masterMacLbl;
    private JLabel masterMacVal;
    private JLabel devMacLbl;
    private JLabel devMacVal;
    private JLabel lastCmdLbl;
    private JLabel lastCmdVal;
    private JLabel lastRspLbl;
    private JLabel lastRspVal;
    private JLabel lastAdvLbl;
    private JLabel lastAdvVal;

    private final KantserBleCommunicationService service = new KantserBleCommunicationService();
    private final LampIndicator comLamp = new LampIndicator();

    private final Map<String, JComponent> responseWidgets = new LinkedHashMap<>();

    private volatile String lastSentCommand = "";
    private volatile boolean autoProbeActive = false;
    private volatile String autoProbeCurrent = null;
    private final Deque<String> autoProbeQueue = new ArrayDeque<>();

    public KantserBleMain() {
        initUI();
        registerListeners();
        buildCommandRows();
        refreshPorts();
        loadLastSettings();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void initUI() {
        comLampContainer.setLayout(new BorderLayout());
        comLampContainer.add(comLamp, BorderLayout.CENTER);
        comLamp.setPreferredSize(new Dimension(14, 14));
        comLamp.setLampColor(Color.RED);

        portComboSpeed.removeAllItems();
        for (int b : new int[]{9600, 19200, 38400, 57600, 115200, 230400}) {
            portComboSpeed.addItem(b);
        }
        portComboSpeed.setSelectedItem(DEFAULT_BAUD);

        evenParityCb.setSelected(false);
        closePortBtn.setEnabled(false);
        portStatusLabel.setText("Порт закрыт");
        statusBar.setText("Готово · " + DEFAULT_BAUD + " 8N1");

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setRows(6);

        styleHeaderValue(fwDeviceVal);
        styleHeaderValue(fwVersionVal);
        styleHeaderValue(bleStatusVal);
        styleHeaderValue(advStateVal);
        styleHeaderValue(conc1Val);
        styleHeaderValue(conc2Val);
        styleHeaderValue(conc3Val);
        styleHeaderValue(conc4Val);
        styleHeaderValue(tempOffsetVal);
        styleHeaderValue(masterMacVal);
        styleHeaderValue(devMacVal);
        styleHeaderValue(lastCmdVal);
        styleHeaderValue(lastRspVal);
        styleHeaderValue(lastAdvVal);

        resetDeviceInfo();
    }

    private static void styleHeaderValue(JLabel lbl) {
        if (lbl == null) {
            return;
        }
        lbl.setOpaque(true);
        lbl.setBackground(HEADER_BG);
        lbl.setForeground(HEADER_EMPTY);
        lbl.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
    }

    private static void setHeaderValue(JLabel lbl, String text) {
        if (lbl == null) {
            return;
        }
        lbl.setText(text);
        lbl.setForeground(HEADER_FG);
    }

    private static void clearHeaderValue(JLabel lbl) {
        if (lbl == null) {
            return;
        }
        lbl.setText("- - -");
        lbl.setForeground(HEADER_EMPTY);
    }

    private void resetDeviceInfo() {
        clearHeaderValue(fwDeviceVal);
        clearHeaderValue(fwVersionVal);
        clearHeaderValue(bleStatusVal);
        clearHeaderValue(advStateVal);
        clearHeaderValue(conc1Val);
        clearHeaderValue(conc2Val);
        clearHeaderValue(conc3Val);
        clearHeaderValue(conc4Val);
        clearHeaderValue(tempOffsetVal);
        clearHeaderValue(masterMacVal);
        clearHeaderValue(devMacVal);
        clearHeaderValue(lastCmdVal);
        clearHeaderValue(lastRspVal);
        clearHeaderValue(lastAdvVal);
    }

    private void registerListeners() {
        refreshBtn.addActionListener(e -> refreshPorts());
        openPortBtn.addActionListener(e -> openPort());
        closePortBtn.addActionListener(e -> closePort());
        service.addResponseListener(this::onResponse);
        service.setConnectionStatusListener(status -> SwingUtilities.invokeLater(() -> {
            if (status != null && status.startsWith("DISCONNECTED")) {
                comLamp.setLampColor(Color.RED);
                portStatusLabel.setText("Порт закрыт");
            }
        }));
    }

    private void buildCommandRows() {
        commandsPlaceholder.removeAll();
        commandsPlaceholder.setLayout(new BoxLayout(commandsPlaceholder, BoxLayout.Y_AXIS));
        responseWidgets.clear();

        addTerminalSection();

        addSection("Терминал / информация", new Cmd[]{
                Cmd.multiline("HELP", "HELP — список команд"),
                Cmd.of("GDUI?", "GDUI? — описание устройства"),
                Cmd.of("SREV?", "SREV? — версия симулятора"),
                Cmd.multiline("BLST?", "BLST? — статус BLE"),
                Cmd.of("ADST?", "ADST? — текущий adv-пакет"),
        });

        addSection("Установка параметров (имитация)", new Cmd[]{
                Cmd.withArg("SCH1", "SCH1 — концентрация CH1", "0"),
                Cmd.withArg("SCH2", "SCH2 — концентрация CH2", "0"),
                Cmd.withArg("SCH3", "SCH3 — концентрация CH3", "0"),
                Cmd.withArg("SCH4", "SCH4 — концентрация CH4", "0"),
                Cmd.withArg("STER", "STER — смещение температуры", "0"),
                Cmd.withArg("SMAC", "SMAC — MAC мастера", MAC_DEFAULT[0]),
        });

        addSection("Управление BLE", new Cmd[]{
                Cmd.of("ADVE", "ADVE — реклама BLE ON"),
                Cmd.of("ADVD", "ADVD — реклама BLE OFF"),
                Cmd.of("REBT", "REBT — перезагрузка устройства"),
        });

        addSection("Служебное", new Cmd[]{
                Cmd.of("LRBC", "LRBC — последняя принятая BLE команда"),
                Cmd.of("LSBA", "LSBA — последний отправленный BLE ответ"),
                Cmd.of("LLBA", "LLBA — последняя принятая BLE adv"),
                Cmd.of("CMMD", "CMMD — MAC мастера"),
                Cmd.of("GMAC", "GMAC — MAC устройства"),
        });

        commandsPlaceholder.add(Box.createVerticalGlue());
        commandsPlaceholder.revalidate();
        commandsPlaceholder.repaint();
    }

    private void addSection(String title, Cmd[] cmds) {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (Cmd c : cmds) {
            section.add(makeCommandRow(c));
            section.add(Box.createVerticalStrut(3));
        }
        commandsPlaceholder.add(section);
        commandsPlaceholder.add(Box.createVerticalStrut(6));
    }

    /**
     * Свободный ввод произвольной команды (в т.ч. BLE GET/SET параметров).
     * Ответ накапливается как многострочный блок.
     */
    private void addTerminalSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Произвольная команда / терминал",
                TitledBorder.LEFT, TitledBorder.TOP));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextField input = new JTextField();
        input.setToolTipText("Любая команда: VE?, GDUI, SCH2 44, SMAC AA:BB:CC:DD:EE:FF, …");
        JButton sendBtn = new JButton("Отправить");
        sendBtn.setPreferredSize(new Dimension(120, 28));

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.add(input, BorderLayout.CENTER);
        row.add(sendBtn, BorderLayout.EAST);
        section.add(row);
        section.add(Box.createVerticalStrut(4));

        JTextArea resp = new JTextArea(6, 40);
        resp.setEditable(false);
        resp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        resp.setLineWrap(false);
        resp.setText("—");
        JScrollPane sp = new JScrollPane(resp);
        sp.setPreferredSize(new Dimension(420, 110));
        section.add(sp);

        commandsPlaceholder.add(section);
        commandsPlaceholder.add(Box.createVerticalStrut(6));

        responseWidgets.put("__TERM__", resp);

        sendBtn.addActionListener(e -> sendFreeCommand(input, resp));
        input.addActionListener(e -> sendFreeCommand(input, resp));
    }

    private void sendFreeCommand(JTextField input, JTextArea resp) {
        if (!service.isConnected()) {
            JOptionPane.showMessageDialog(mainPanel, "Сначала откройте COM-порт");
            return;
        }
        String cmd = input.getText().trim();
        if (cmd.isEmpty()) {
            return;
        }
        resp.setText("…");
        resp.setForeground(ORANGE);
        sendCommand(cmd, "__TERM__");
    }

    private JPanel makeCommandRow(Cmd c) {
        boolean tall = c.multiline;
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, tall ? 140 : 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton btn = new JButton(c.label);
        btn.setPreferredSize(new Dimension(260, 28));
        btn.setMinimumSize(new Dimension(200, 28));

        JTextField argField = null;
        if (c.defaultArg != null) {
            argField = new JTextField(c.defaultArg, 8);
            argField.setMaximumSize(new Dimension("SMAC".equals(c.key) ? 160 : 100, 28));
            argField.setToolTipText("SMAC".equals(c.key)
                    ? "MAC мастера, формат AA:BB:CC:DD:EE:FF"
                    : "Числовое значение (концентрация / смещение)");
        }

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(btn);
        if (argField != null) {
            left.add(new JLabel("SMAC".equals(c.key) ? "mac=" : "v="));
            left.add(argField);
        }

        JComponent respWidget;
        if (c.multiline) {
            JTextArea area = new JTextArea(8, 40);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            area.setLineWrap(false);
            area.setText("—");
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(420, 160));
            respWidget = area;
            row.add(left, BorderLayout.NORTH);
            row.add(sp, BorderLayout.CENTER);
        } else {
            JLabel resp = new JLabel("—");
            resp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            resp.setForeground(GRAY);
            respWidget = resp;
            row.add(left, BorderLayout.WEST);
            row.add(resp, BorderLayout.CENTER);
        }

        responseWidgets.put(c.key, respWidget);

        final JTextField argRef = argField;
        btn.addActionListener(e -> onCommandClick(c, argRef, respWidget));

        return row;
    }

    private void onCommandClick(Cmd c, JTextField argRef, JComponent respWidget) {
        if (!service.isConnected()) {
            JOptionPane.showMessageDialog(mainPanel, "Сначала откройте COM-порт");
            return;
        }

        String toSend = c.key;
        if (argRef != null) {
            String raw = argRef.getText().trim();
            if ("SMAC".equals(c.key)) {
                if (!MAC_PATTERN.matcher(raw).find()) {
                    setResponseText(respWidget, "ERR mac", RED);
                    return;
                }
                toSend = "SMAC " + raw;
            } else {
                double v;
                try {
                    v = Double.parseDouble(raw.replace(',', '.'));
                } catch (NumberFormatException ex) {
                    setResponseText(respWidget, "ERR v", RED);
                    return;
                }
                toSend = c.key + " " + formatNumber(v);
            }
        }

        service.setMultilineMode(c.multiline);
        if (respWidget instanceof JTextArea area) {
            area.setText("…");
            area.setForeground(ORANGE);
        } else {
            setResponseText(respWidget, "…", ORANGE);
        }
        // Устройство отвечает просто "OK" на установку — показываем установленное значение сразу
        applyOptimisticHeader(c.key, toSend);
        sendCommand(toSend, c.key);
    }

    /**
     * SCH1..4 / STER / SMAC отвечают "OK" без эха значения — обновляем шапку из того, что отправили.
     */
    private void applyOptimisticHeader(String key, String toSend) {
        switch (key) {
            case "SCH1" -> setHeaderValue(conc1Val, valueFromCommand(toSend));
            case "SCH2" -> setHeaderValue(conc2Val, valueFromCommand(toSend));
            case "SCH3" -> setHeaderValue(conc3Val, valueFromCommand(toSend));
            case "SCH4" -> setHeaderValue(conc4Val, valueFromCommand(toSend));
            case "STER" -> setHeaderValue(tempOffsetVal, valueFromCommand(toSend));
            case "SMAC" -> {
                int sp = toSend.indexOf(' ');
                if (sp > 0) {
                    setHeaderValue(masterMacVal, toSend.substring(sp + 1));
                }
            }
            default -> {
                // no header field for this key
            }
        }
    }

    private static String valueFromCommand(String toSend) {
        int sp = toSend.indexOf(' ');
        return sp > 0 ? toSend.substring(sp + 1) : toSend;
    }

    private static String formatNumber(double v) {
        if (Math.rint(v) == v && Math.abs(v) < 1e15) {
            return String.format(Locale.US, "%.0f", v);
        }
        return String.format(Locale.US, "%s", v);
    }

    private void sendCommand(String cmd, String responseKey) {
        lastSentCommand = responseKey;
        service.setMultilineMode(isMultilineKey(responseKey));
        appendLog("→ " + cmd);
        statusBar.setText("TX: " + cmd);
        service.sendCommand(cmd);
    }

    private static boolean isMultilineKey(String key) {
        return "HELP".equals(key) || "BLST?".equals(key) || "__TERM__".equals(key);
    }

    private void onResponse(String text) {
        SwingUtilities.invokeLater(() -> {
            for (String line : text.split("\n", -1)) {
                if (!line.isBlank()) {
                    appendLog("← " + line);
                }
            }

            // Устройство эхо-печатает команду ("\n> CMD ARGS\n") перед ответом — убираем эхо из обработки
            String clean = stripEcho(text);
            if (clean.isEmpty()) {
                statusBar.setText("RX: " + summarize(text, 60));
                return;
            }

            statusBar.setText("RX: " + summarize(clean, 60));
            String key = lastSentCommand;
            if (key == null || key.isEmpty()) {
                return;
            }

            // Header probe / status keys update header even without a row widget
            updateHeaderFromResponse(key, clean);

            JComponent resp = responseWidgets.get(key);
            if (resp == null) {
                advanceAutoProbeIfNeeded(key);
                return;
            }

            if ("HELP".equals(key) || "BLST?".equals(key) || "__TERM__".equals(key)) {
                if (resp instanceof JTextArea area) {
                    String current = area.getText();
                    if ("…".equals(current.trim()) || current.trim().isEmpty()) {
                        area.setText(clean);
                    } else {
                        area.append("\n" + clean);
                    }
                    area.setCaretPosition(0);
                    area.setForeground(GREEN);
                }
                advanceAutoProbeIfNeeded(key);
                return;
            }

            setResponseText(resp, summarize(clean, 120), GREEN);
            advanceAutoProbeIfNeeded(key);
        });
    }

    /**
     * Удаляет строки-эхо устройства ("\n> CMD ARGS\n") из кадра ответа.
     */
    private static String stripEcho(String text) {
        String[] lines = text.split("\\R");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (line.trim().startsWith(">")) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(line);
        }
        return sb.toString().trim();
    }

    /**
     * Header fields from command responses:
     * GDUI?/SREV?/BLST?/ADST?/CMMD/GMAC/LRBC/LSBA/LLBA/ADVE/ADVD.
     * SCH1..4/STER/SMAC отвечают "OK" — шапку заполняем в {@link #applyOptimisticHeader}.
     */
    private void updateHeaderFromResponse(String key, String text) {
        String t = text.trim();
        switch (key) {
            case "GDUI?" -> {
                fwDevice = t;
                setHeaderValue(fwDeviceVal, t);
            }
            case "SREV?" -> {
                fwVersion = t;
                setHeaderValue(fwVersionVal, t);
            }
            case "BLST?" -> applyBleStatus(t);
            case "ADST?" -> setHeaderValue(lastAdvVal, summarize(t, 32));
            case "CMMD" -> setHeaderValue(masterMacVal, extractMac(t));
            case "GMAC" -> setHeaderValue(devMacVal, extractMac(t));
            case "LRBC" -> setHeaderValue(lastCmdVal, summarize(t, 24));
            case "LSBA" -> setHeaderValue(lastRspVal, summarize(t, 24));
            case "LLBA" -> setHeaderValue(lastAdvVal, summarize(t, 32));
            case "ADVE" -> {
                setHeaderValue(advStateVal, "● ADV ON");
                advStateVal.setForeground(GREEN);
            }
            case "ADVD" -> {
                setHeaderValue(advStateVal, "● ADV OFF");
                advStateVal.setForeground(RED);
            }
            default -> {
                // no header field for this key
            }
        }
    }

    /**
     * BLST? — "BLE Status:\n  Connected: YES\n  Advertising: NO\n  Scanning: YES".
     */
    private void applyBleStatus(String text) {
        boolean connected = yesNoValue(text, "Connected");
        boolean advertising = yesNoValue(text, "Advertising");
        boolean scanning = yesNoValue(text, "Scanning");
        setHeaderValue(bleStatusVal,
                (connected ? "CONN" : "IDLE") + " · adv " + (advertising ? "ON" : "OFF")
                        + " · scan " + (scanning ? "ON" : "OFF"));
        advStateVal.setText(advertising ? "● ADV ON" : "● ADV OFF");
        advStateVal.setForeground(advertising ? GREEN : RED);
    }

    private static boolean yesNoValue(String text, String field) {
        Matcher m = Pattern.compile(field + "\\s*:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE).matcher(text);
        return m.find() && "YES".equalsIgnoreCase(m.group(1));
    }

    private static String extractMac(String text) {
        Matcher m = MAC_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(0);
        }
        return summarize(text, 20);
    }

    private void setResponseText(JComponent widget, String text, Color color) {
        if (widget instanceof JLabel label) {
            label.setText(text);
            label.setForeground(color);
        } else if (widget instanceof JTextArea area) {
            area.setText(text);
            area.setForeground(color);
        }
    }

    private static String summarize(String text, int max) {
        String one = text.replace('\n', ' ').trim();
        return one.length() > max ? one.substring(0, max) + "…" : one;
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private void refreshPorts() {
        portComboSelect.removeAllItems();
        for (SerialPort p : SerialPort.getCommPorts()) {
            portComboSelect.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portComboSelect.getItemCount() == 0) {
            portComboSelect.addItem("Нет доступных портов");
        }
    }

    private void loadLastSettings() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        String lastPort = prefs.get(LAST_PORT, "");
        int lastBaud = prefs.getInt(LAST_BAUD, DEFAULT_BAUD);
        if (!lastPort.isEmpty()) {
            for (int i = 0; i < portComboSelect.getItemCount(); i++) {
                if (portComboSelect.getItemAt(i).startsWith(lastPort)) {
                    portComboSelect.setSelectedIndex(i);
                    break;
                }
            }
        }
        selectBaud(lastBaud);
    }

    private void saveLastSettings(String portName, int baud) {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        prefs.put(LAST_PORT, portName);
        prefs.putInt(LAST_BAUD, baud);
    }

    private int resolveSelectedBaud() {
        Object sel = portComboSpeed.getSelectedItem();
        if (sel instanceof Integer i) {
            return i > 0 ? i : DEFAULT_BAUD;
        }
        if (sel instanceof Number n) {
            int v = n.intValue();
            return v > 0 ? v : DEFAULT_BAUD;
        }
        if (sel != null) {
            try {
                int v = Integer.parseInt(sel.toString().trim());
                return v > 0 ? v : DEFAULT_BAUD;
            } catch (NumberFormatException ignored) {
            }
        }
        return DEFAULT_BAUD;
    }

    private void selectBaud(int baud) {
        for (int i = 0; i < portComboSpeed.getItemCount(); i++) {
            Integer item = portComboSpeed.getItemAt(i);
            if (item != null && item == baud) {
                portComboSpeed.setSelectedIndex(i);
                return;
            }
        }
        portComboSpeed.addItem(baud);
        portComboSpeed.setSelectedItem(baud);
    }

    private void openPort() {
        String selected = (String) portComboSelect.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(mainPanel, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        final int baud = resolveSelectedBaud();
        final int parity = evenParityCb.isSelected() ? SerialPort.EVEN_PARITY : SerialPort.NO_PARITY;

        comLamp.setLampColor(Color.YELLOW);
        portStatusLabel.setText("Открытие @ " + baud + "…");
        openPortBtn.setEnabled(false);
        appendLog("Открытие " + portName + " @ " + baud + (evenParityCb.isSelected() ? " 8E1" : " 8N1"));

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.openPort(portName, baud, parity);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        int applied = service.getActiveBaud();
                        saveLastSettings(portName, baud);
                        comLamp.setLampColor(GREEN);
                        portStatusLabel.setText("Открыт @ " + applied);
                        closePortBtn.setEnabled(true);
                        statusBar.setText("Порт " + portName + " @ " + applied
                                + (evenParityCb.isSelected() ? " 8E1" : " 8N1"));
                        appendLog("Порт открыт, baud=" + applied);
                        scheduleAutoProbe();
                    } else {
                        comLamp.setLampColor(RED);
                        portStatusLabel.setText("Ошибка открытия");
                        openPortBtn.setEnabled(true);
                        appendLog("Ошибка открытия " + portName + " @ " + baud);
                    }
                } catch (Exception ex) {
                    log.error("Open port error (baud={})", baud, ex);
                    openPortBtn.setEnabled(true);
                    appendLog("Ошибка: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void closePort() {
        service.setMultilineMode(false);
        service.closePort();
        autoProbeActive = false;
        autoProbeCurrent = null;
        autoProbeQueue.clear();
        fwDevice = "";
        fwVersion = "";
        comLamp.setLampColor(RED);
        portStatusLabel.setText("Порт закрыт");
        openPortBtn.setEnabled(true);
        closePortBtn.setEnabled(false);
        statusBar.setText("Порт закрыт");
        resetDeviceInfo();
    }

    /**
     * After open: sequential header probe GDUI? → SREV? → BLST? → ADST? → CMMD → GMAC.
     */
    private void scheduleAutoProbe() {
        autoProbeActive = false;
        autoProbeCurrent = null;
        autoProbeQueue.clear();
        new Timer(2000, e -> {
            ((Timer) e.getSource()).stop();
            if (!service.isConnected()) {
                return;
            }
            startHeaderProbe("после открытия порта");
        }).start();
    }

    private void startHeaderProbe(String reason) {
        if (!service.isConnected()) {
            return;
        }
        autoProbeActive = false;
        autoProbeCurrent = null;
        autoProbeQueue.clear();
        appendLog("◆ Читать шапку (" + reason + "): GDUI? → SREV? → BLST? → ADST? → CMMD → GMAC");
        autoProbeQueue.addLast("GDUI?");
        autoProbeQueue.addLast("SREV?");
        autoProbeQueue.addLast("BLST?");
        autoProbeQueue.addLast("ADST?");
        autoProbeQueue.addLast("CMMD");
        autoProbeQueue.addLast("GMAC");
        autoProbeActive = true;
        sendNextAutoProbe();
    }

    private void sendNextAutoProbe() {
        if (!service.isConnected()) {
            autoProbeActive = false;
            autoProbeCurrent = null;
            return;
        }
        if (autoProbeQueue.isEmpty()) {
            autoProbeActive = false;
            autoProbeCurrent = null;
            appendLog("◆ Auto-probe done");
            return;
        }
        String cmd = autoProbeQueue.pollFirst();
        if (cmd == null) {
            autoProbeActive = false;
            autoProbeCurrent = null;
            return;
        }
        autoProbeCurrent = cmd;
        appendLog("◆ Auto-probe: " + cmd);
        sendCommand(cmd, cmd);
        final String expected = cmd;
        new Timer(1500, ev -> {
            ((Timer) ev.getSource()).stop();
            if (autoProbeActive && expected.equals(autoProbeCurrent) && service.isConnected()) {
                appendLog("◆ Auto-probe timeout on " + expected + ", continue");
                autoProbeCurrent = null;
                sendNextAutoProbe();
            }
        }).start();
    }

    private void advanceAutoProbeIfNeeded(String answeredKey) {
        if (!autoProbeActive || answeredKey == null || autoProbeCurrent == null) {
            return;
        }
        if (!answeredKey.equals(autoProbeCurrent)) {
            return;
        }
        autoProbeCurrent = null;
        new Timer(200, e -> {
            ((Timer) e.getSource()).stop();
            if (autoProbeActive && service.isConnected()) {
                sendNextAutoProbe();
            }
        }).start();
    }

    // ─── form scaffolding ────────────────────────────────────────────────

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
        mainPanel.setLayout(new GridLayoutManager(4, 2, new Insets(6, 6, 6, 6), 8, 8));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(10, 1, new Insets(4, 4, 4, 4), -1, 4));
        mainPanel.add(panel1, new GridConstraints(0, 0, 3, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(240, -1), new Dimension(240, -1), new Dimension(260, -1), 0, false));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "COM", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("COM порт:");
        panel1.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSelect = new JComboBox();
        panel1.add(portComboSelect, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(220, -1), new Dimension(220, -1), new Dimension(220, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Скорость:");
        panel1.add(label2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSpeed = new JComboBox();
        panel1.add(portComboSpeed, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(220, -1), new Dimension(220, -1), new Dimension(220, -1), 0, false));
        evenParityCb = new JCheckBox();
        evenParityCb.setSelected(false);
        evenParityCb.setText("Even parity (иначе 8N1)");
        panel1.add(evenParityCb, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshBtn = new JButton();
        refreshBtn.setText("Обновить порты");
        panel1.add(refreshBtn, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(220, -1), new Dimension(220, -1), new Dimension(220, -1), 0, false));
        openPortBtn = new JButton();
        openPortBtn.setText("Открыть порт");
        panel1.add(openPortBtn, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(220, -1), new Dimension(220, -1), new Dimension(220, -1), 0, false));
        closePortBtn = new JButton();
        closePortBtn.setText("Закрыть порт");
        panel1.add(closePortBtn, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(220, -1), new Dimension(220, -1), new Dimension(240, -1), 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        panel1.add(panel2, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comLampContainer = new JPanel();
        comLampContainer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(comLampContainer);
        portStatusLabel = new JLabel();
        portStatusLabel.setText("Порт закрыт");
        panel2.add(portStatusLabel);
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(640, 420), null, 0, false));
        scrollPane1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Команды (ответ справа)", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        commandsPlaceholder = new JPanel();
        commandsPlaceholder.setLayout(new GridLayoutManager(1, 1, new Insets(4, 4, 4, 4), -1, -1));
        scrollPane1.setViewportView(commandsPlaceholder);
        final JScrollPane scrollPane2 = new JScrollPane();
        mainPanel.add(scrollPane2, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(-1, 120), null, 0, false));
        scrollPane2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Лог", TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        logArea = new JTextArea();
        logArea.setEditable(false);
        scrollPane2.setViewportView(logArea);
        statusBar = new JLabel();
        statusBar.setText("Готово");
        mainPanel.add(statusBar, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        devInfo = new JPanel();
        devInfo.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(devInfo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fwInfo = new JPanel();
        fwInfo.setLayout(new GridLayoutManager(7, 4, new Insets(2, 2, 2, 2), 6, 4));
        devInfo.add(fwInfo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fwDeviceLbl = new JLabel();
        fwDeviceLbl.setText("Устройство");
        fwInfo.add(fwDeviceLbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fwDeviceVal = new JLabel();
        fwDeviceVal.setText("- - -");
        fwInfo.add(fwDeviceVal, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fwVersionLbl = new JLabel();
        fwVersionLbl.setText("Версия ПО");
        fwInfo.add(fwVersionLbl, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fwVersionVal = new JLabel();
        fwVersionVal.setText("- - -");
        fwInfo.add(fwVersionVal, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bleStatusLbl = new JLabel();
        bleStatusLbl.setText("BLE статус");
        fwInfo.add(bleStatusLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        bleStatusVal = new JLabel();
        bleStatusVal.setText("- - -");
        fwInfo.add(bleStatusVal, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        advStateLbl = new JLabel();
        advStateLbl.setText("Реклама");
        fwInfo.add(advStateLbl, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        advStateVal = new JLabel();
        advStateVal.setText("- - -");
        fwInfo.add(advStateVal, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc1Lbl = new JLabel();
        conc1Lbl.setText("Конц. CH1");
        fwInfo.add(conc1Lbl, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc1Val = new JLabel();
        conc1Val.setText("- - -");
        fwInfo.add(conc1Val, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc2Lbl = new JLabel();
        conc2Lbl.setText("Конц. CH2");
        fwInfo.add(conc2Lbl, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc2Val = new JLabel();
        conc2Val.setText("- - -");
        fwInfo.add(conc2Val, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc3Lbl = new JLabel();
        conc3Lbl.setText("Конц. CH3");
        fwInfo.add(conc3Lbl, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc3Val = new JLabel();
        conc3Val.setText("- - -");
        fwInfo.add(conc3Val, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc4Lbl = new JLabel();
        conc4Lbl.setText("Конц. CH4");
        fwInfo.add(conc4Lbl, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        conc4Val = new JLabel();
        conc4Val.setText("- - -");
        fwInfo.add(conc4Val, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tempOffsetLbl = new JLabel();
        tempOffsetLbl.setText("Смещение темп.");
        fwInfo.add(tempOffsetLbl, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tempOffsetVal = new JLabel();
        tempOffsetVal.setText("- - -");
        fwInfo.add(tempOffsetVal, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        masterMacLbl = new JLabel();
        masterMacLbl.setText("Master MAC");
        fwInfo.add(masterMacLbl, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        masterMacVal = new JLabel();
        masterMacVal.setText("- - -");
        fwInfo.add(masterMacVal, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        devMacLbl = new JLabel();
        devMacLbl.setText("MAC устройства");
        fwInfo.add(devMacLbl, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        devMacVal = new JLabel();
        devMacVal.setText("- - -");
        fwInfo.add(devMacVal, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastCmdLbl = new JLabel();
        lastCmdLbl.setText("Посл. BLE команда");
        fwInfo.add(lastCmdLbl, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastCmdVal = new JLabel();
        lastCmdVal.setText("- - -");
        fwInfo.add(lastCmdVal, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastRspLbl = new JLabel();
        lastRspLbl.setText("Посл. BLE ответ");
        fwInfo.add(lastRspLbl, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastRspVal = new JLabel();
        lastRspVal.setText("- - -");
        fwInfo.add(lastRspVal, new GridConstraints(6, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastAdvLbl = new JLabel();
        lastAdvLbl.setText("Посл. BLE adv");
        fwInfo.add(lastAdvLbl, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        lastAdvVal = new JLabel();
        lastAdvVal.setText("- - -");
        fwInfo.add(lastAdvVal, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    private static final class Cmd {
        final String key;
        final String label;
        final String defaultArg;
        final boolean multiline;

        private Cmd(String key, String label, String defaultArg, boolean multiline) {
            this.key = key;
            this.label = label;
            this.defaultArg = defaultArg;
            this.multiline = multiline;
        }

        static Cmd of(String key, String label) {
            return new Cmd(key, label, null, false);
        }

        static Cmd withArg(String key, String label, String defaultArg) {
            return new Cmd(key, label, defaultArg, false);
        }

        static Cmd multiline(String key, String label) {
            return new Cmd(key, label, null, true);
        }
    }
}
