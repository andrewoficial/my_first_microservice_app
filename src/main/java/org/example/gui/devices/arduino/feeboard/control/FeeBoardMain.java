package org.example.gui.devices.arduino.feeboard.control;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.extern.slf4j.Slf4j;
import org.example.device.command.SingleCommand;
import org.example.device.protArdFeeBrdMeter.ArdFeeBrdMeterCommandRegistry;
import org.example.services.AnswerValues;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Панель управления ARD_FEE_BRD_METER (CCM Fee Board).
 * Кнопки команд + ответ рядом; блок «Состояние» из F/MMESU.
 */
@Slf4j
public class FeeBoardMain {

    private static final String PREFS_NODE = "org/example/gui/arduino/feeboard";
    private static final String LAST_PORT = "lastPort";
    private static final String LAST_BAUD = "lastBaud";
    private static final int DEFAULT_BAUD = FeeBoardCommunicationService.DEFAULT_BAUD;

    private static final Color GREEN = new Color(0, 160, 0);
    private static final Color RED = new Color(200, 40, 40);
    private static final Color BLUE = new Color(0, 90, 160);
    private static final Color ORANGE = new Color(180, 120, 0);
    private static final Color GRAY = Color.DARK_GRAY;

    private static final Pattern FPWR_PATTERN = Pattern.compile("feePWR:\\s*(ON|OFF)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MMESU_FPWR = Pattern.compile("\\b(ON|OFF)\\b", Pattern.CASE_INSENSITIVE);

    /**
     * Per-line banner patterns — each line arrives separately from the service.
     * Example lines: "*\tDevice: CurMeter\t*", "*\tS/N: 08500010\t\t*"
     */
    private static final Pattern BANNER_DEVICE = Pattern.compile("\\*\\s*Device:\\s*(.+?)\\s*\\*");
    private static final Pattern BANNER_SN = Pattern.compile("\\*\\s*S/N:\\s*(\\d+)");
    private static final Pattern BANNER_ADDR = Pattern.compile("\\*\\s*Adres:\\s*(\\d+)");
    private static final Pattern BANNER_URT = Pattern.compile("\\*\\s*URT:\\s*(\\d+)");
    private static final Pattern BANNER_SW = Pattern.compile("\\*\\s*SW:(.+?)\\s*\\*");
    private static final Pattern BANNER_EDGE = Pattern.compile("^\\*{3,}$");

    // Firmware info parsed from boot banner
    private volatile String fwDevice = "";
    private volatile String fwSerial = "";
    private volatile String fwAddress = "";
    private volatile String fwUrt = "";
    private volatile String fwSw = "";

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
    private JLabel serialNumLbl;
    private JLabel serialNumVal;
    private JLabel fwVersionLbl;
    private JLabel fwVersionVal;
    private JLabel nAddrrLbl;
    private JLabel nAddrrVal;
    private JLabel urtSpeedLbl;
    private JLabel urtSpeedVal;
    private JPanel hwInfo;
    private JLabel outStateLableVal;
    private JLabel outStateLableLbl;
    private JPanel outStateLamp;
    private JLabel outPoverVoltLbl;
    private JLabel outPoverVoltVal;
    private JLabel termLbl;
    private JLabel termVal;
    private JLabel consurptionMaLbl;
    private JLabel consurptionMaVal;

    // HW state lamp (inside outStateLamp from GUI designer)
    private final LampIndicator statePowerLamp = new LampIndicator();

    private final FeeBoardCommunicationService service = new FeeBoardCommunicationService();
    private final ArdFeeBrdMeterCommandRegistry registry = new ArdFeeBrdMeterCommandRegistry();
    private final LampIndicator comLamp = new LampIndicator();

    /**
     * command key → response widget (JLabel or JTextArea)
     */
    private final Map<String, JComponent> responseWidgets = new LinkedHashMap<>();
    private final Map<String, LampIndicator> responseLamps = new LinkedHashMap<>();

    private volatile String lastSentCommand = "";
    private volatile Integer urtmodPendingBaud = null;
    private volatile boolean bannerSeen = false;

    public FeeBoardMain() {
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

        // Wire LampIndicator into outStateLamp from GUI designer
        outStateLamp.setLayout(new BorderLayout());
        outStateLamp.add(statePowerLamp, BorderLayout.CENTER);
        statePowerLamp.setPreferredSize(new Dimension(14, 14));

        portComboSpeed.removeAllItems();
        for (int b : new int[]{9600, 19200, 38400, 57600, 115200}) {
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

        resetDeviceInfo();
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
        responseLamps.clear();

        addSection("Измерения", new Cmd[]{
                Cmd.of("F", "F — fixed-width"),
                Cmd.of("MMESU", "MMESU — быстрые показания"),
                Cmd.of("LOGO", "LOGO — лог 1 раз"),
                Cmd.of("LOG", "LOG — авто-лог on/off"),
                Cmd.of("CONC?", "CONC? — FEE/CUR/TER/HUD/PRS/PWR"),
        });

        addSection("Питание / мост", new Cmd[]{
                Cmd.of("FPWR?", "FPWR? — статус питания"),
                Cmd.of("FPWR", "FPWR — toggle питание"),
                Cmd.of("SENSON", "SENSON — мост ON"),
                Cmd.of("SENSOFF", "SENSOFF — мост OFF"),
        });

        addSection("Система", new Cmd[]{
                Cmd.of("SREV?", "SREV? — версия ПО"),
                Cmd.of("SRAL?", "SRAL? — серийный номер"),
                Cmd.of("%**", "%** — адрес"),
                Cmd.multiline("GCOEF", "GCOEF — коэффициенты"),
                Cmd.multiline("REBOOT", "REBOOT"),
        });

        addSection("Усреднение / UART", new Cmd[]{
                Cmd.withArg("SLAS", "SLAS — LogAproxStep_K1", "10"),
                Cmd.withArg("SDAS", "SDAS — LogAproxStep_K2", "10"),
                Cmd.withArg("URTMOD", "URTMOD — UART baud", "9600"),
        });

        addSection("Калибровка (entry)", new Cmd[]{
                Cmd.of("SPOLY0", "SPOLY0 — poly CH0"),
                Cmd.of("SPOLY1", "SPOLY1 — poly CH1"),
                Cmd.of("SVOLT0", "SVOLT0 — нули АЦП"),
                Cmd.of("STRGLV", "STRGLV — trigger"),
                Cmd.of("SV2AMP", "SV2AMP — V→A"),
                Cmd.of("SCABD", "SCABD — дата калибровки"),
        });

        commandsPlaceholder.add(Box.createVerticalGlue());
        commandsPlaceholder.revalidate();
        commandsPlaceholder.repaint();
    }

    private void resetDeviceInfo() {
        statePowerLamp.setLampColor(Color.GRAY);
        outStateLableVal.setText("Выход отключён");
        outStateLableVal.setForeground(GRAY);
        outPoverVoltVal.setText("- - -");
        termVal.setText("- - -");
        consurptionMaVal.setText("- - -");
        serialNumVal.setText("- - -");
        fwVersionVal.setText("- - -");
        nAddrrVal.setText("- - -");
        urtSpeedVal.setText("- - -");
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
            argField = new JTextField(c.defaultArg, 6);
            argField.setMaximumSize(new Dimension(80, 28));
            argField.setToolTipText(c.key.equals("URTMOD")
                    ? "Скорость UART после Ok (например 9600)"
                    : "Аргумент (для SLAS/SDAS — число точек)");
        }

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(btn);
        if (argField != null) {
            left.add(new JLabel(c.key.equals("URTMOD") ? "baud=" : "n="));
            left.add(argField);
        }

        JComponent respWidget;
        if (c.multiline) {
            JTextArea area = new JTextArea(6, 40);
            area.setEditable(false);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
            area.setLineWrap(false);
            area.setText("—");
            JScrollPane sp = new JScrollPane(area);
            sp.setPreferredSize(new Dimension(420, 120));
            respWidget = area;
            row.add(left, BorderLayout.NORTH);
            row.add(sp, BorderLayout.CENTER);
        } else if (c.key.startsWith("FPWR")) {
            JPanel respRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            LampIndicator lamp = new LampIndicator();
            lamp.setLampColor(Color.GRAY);
            JPanel lampHost = new JPanel(new BorderLayout());
            lampHost.setPreferredSize(new Dimension(14, 14));
            lampHost.add(lamp, BorderLayout.CENTER);
            JLabel resp = new JLabel("—");
            resp.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            resp.setForeground(GRAY);
            respRow.add(lampHost);
            respRow.add(resp);
            responseLamps.put(c.key, lamp);
            respWidget = resp;
            row.add(left, BorderLayout.WEST);
            row.add(respRow, BorderLayout.CENTER);
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

        if ("URTMOD".equals(c.key)) {
            int baud;
            try {
                baud = Integer.parseInt(argRef.getText().trim());
                if (baud < 300) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                setResponseText(respWidget, "ERR baud", RED);
                return;
            }
            urtmodPendingBaud = baud;
            service.setMultilineMode(false);
            setResponseText(respWidget, "… URTMOD", ORANGE);
            sendCommand("URTMOD", "URTMOD");
            return;
        }

        urtmodPendingBaud = null;
        String toSend = c.key;
        if (argRef != null) {
            String raw = argRef.getText().trim();
            try {
                int n = Integer.parseInt(raw);
                toSend = String.format("%s %04d", c.key, Math.max(0, Math.min(9999, n)));
            } catch (NumberFormatException ex) {
                setResponseText(respWidget, "ERR arg", RED);
                return;
            }
        }

        service.setMultilineMode(c.multiline);
        if (respWidget instanceof JTextArea area) {
            area.setText("…");
            area.setForeground(ORANGE);
        } else {
            setResponseText(respWidget, "…", ORANGE);
        }
        sendCommand(toSend, c.key);
    }

    private void sendCommand(String cmd, String responseKey) {
        lastSentCommand = responseKey;
        appendLog("→ " + cmd);
        statusBar.setText("TX: " + cmd);
        service.sendCommand(cmd);
    }

    private void onResponse(String text) {
        SwingUtilities.invokeLater(() -> {
            // multi-line: log each visual line
            for (String line : text.split("\n", -1)) {
                if (!line.isBlank()) {
                    appendLog("← " + line);
                }
            }

            // ── Boot banner detection (unsolicited or after REBOOT) ────
            if (tryApplyBootBanner(text)) {
                statusBar.setText("Устройство: " + fwDevice + "  SN:" + fwSerial
                        + "  SW:" + fwSw + "  URT:" + fwUrt);
                return;
            }

            statusBar.setText("RX: " + summarize(text, 60));

            String key = lastSentCommand;

            // URTMOD two-step: Ok → send baud digits (device usually silent after value)
            if ("URTMOD".equals(key) && urtmodPendingBaud != null) {
                handleUrtmodStep(text);
                return;
            }

            JComponent resp = responseWidgets.get(key);
            if (resp == null) {
                return;
            }

            if ("GCOEF".equals(key) || "REBOOT".equals(key)) {
                if (resp instanceof JTextArea area) {
                    String current = area.getText();
                    if ("…".equals(current.trim()) || current.trim().isEmpty()) {
                        area.setText(text);
                    } else {
                        area.append("\n" + text);
                    }
                    area.setCaretPosition(0);
                    area.setForeground(GREEN);
                }
                return;
            }

            if (key.startsWith("FPWR")) {
                applyFpwrResponse(key, text);
                return;
            }

            if ("SRAL?".equals(key) || "SREV?".equals(key) || "%**".equals(key)) {
                String clean = text.trim();
                setResponseText(resp, clean, GREEN);
                updateFirmwareFromCommand(key, clean);
                return;
            }

            if ("F".equals(key) || "MMESU".equals(key) || "LOGO".equals(key) || "LOG".equals(key) || "CONC?".equals(key)) {
                String parseHint = tryParsePretty(key, text);
                setResponseText(resp, parseHint != null ? parseHint : summarize(text, 100), GREEN);
                updateStateFromMeasurement(key, text);
                return;
            }

            String parseHint = tryParsePretty(key, text);
            if (parseHint != null) {
                setResponseText(resp, parseHint, GREEN);
            } else {
                setResponseText(resp, summarize(text, 100), GREEN);
            }
        });
    }

    private void handleUrtmodStep(String text) {
        Integer baud = urtmodPendingBaud;
        JComponent resp = responseWidgets.get("URTMOD");
        if (baud == null) {
            return;
        }
        String t = text.trim();
        if (t.equalsIgnoreCase("Ok") || t.equalsIgnoreCase("OK")) {
            appendLog("→ " + baud + "  (URTMOD value)");
            statusBar.setText("TX: " + baud + " (URTMOD baud)");
            service.sendCommand(String.valueOf(baud));
            // board typically does not echo the value
            urtmodPendingBaud = null;
            setResponseText(resp, "● скорость установлена: " + baud, GREEN);
            // optional: after URTMOD the device UART may switch — remind user
            statusBar.setText("URTMOD: baud " + baud + " отправлен (переключите COM baud при необходимости)");
        } else {
            setResponseText(resp, "ожидал Ok, получил: " + summarize(t, 40), RED);
            urtmodPendingBaud = null;
        }
    }

    /**
     * Try to parse a single boot banner line from received text.
     * Each line arrives separately, e.g. "*\tS/N: 08500010\t\t*"
     * Returns true if a banner line was matched.
     */
    private boolean tryApplyBootBanner(String text) {
        String t = text.trim();

        // Skip pure separator lines ***
        if (BANNER_EDGE.matcher(t).matches()) {
            return false;
        }

        boolean matched = false;
        Matcher m;

        m = BANNER_DEVICE.matcher(t);
        if (m.find()) {
            fwDevice = m.group(1).trim();
            matched = true;
        }

        m = BANNER_SN.matcher(t);
        if (m.find()) {
            fwSerial = m.group(1).trim();
            serialNumVal.setText(fwSerial);
            matched = true;
        }

        m = BANNER_ADDR.matcher(t);
        if (m.find()) {
            fwAddress = m.group(1).trim();
            nAddrrVal.setText(fwAddress);
            matched = true;
        }

        m = BANNER_URT.matcher(t);
        if (m.find()) {
            fwUrt = m.group(1).trim();
            urtSpeedVal.setText(fwUrt);
            matched = true;
        }

        m = BANNER_SW.matcher(t);
        if (m.find()) {
            fwSw = m.group(1).trim();
            fwVersionVal.setText(fwSw);
            matched = true;
        }

        if (matched) {
            bannerSeen = true;
            appendLog("◆ Banner: " + t);
        }
        return matched;
    }

    /**
     * When user clicks SREV? or SRAL?, update firmware info labels from the response.
     */
    private void updateFirmwareFromCommand(String key, String clean) {
        if ("SREV?".equals(key) && !clean.isEmpty()) {
            fwSw = clean;
            fwVersionVal.setText(clean);
        } else if ("SRAL?".equals(key) && !clean.isEmpty()) {
            fwSerial = clean.replaceAll("[^0-9]", "");
            serialNumVal.setText(fwSerial);
        }
    }

    private void applyFpwrResponse(String key, String text) {
        Matcher m = FPWR_PATTERN.matcher(text);
        boolean on;
        if (m.find()) {
            on = "ON".equalsIgnoreCase(m.group(1));
        } else if (text.toUpperCase(Locale.ROOT).contains("ON")) {
            on = true;
        } else if (text.toUpperCase(Locale.ROOT).contains("OFF")) {
            on = false;
        } else {
            setResponseText(responseWidgets.get(key), summarize(text, 60), BLUE);
            return;
        }
        String label = on ? "● ON" : "● OFF";
        Color color = on ? GREEN : RED;
        setResponseText(responseWidgets.get(key), label, color);
        LampIndicator lamp = responseLamps.get(key);
        if (lamp != null) {
            lamp.setLampColor(on ? GREEN : RED);
        }
        // update hwInfo panel
        statePowerLamp.setLampColor(on ? GREEN : RED);
        outStateLableVal.setText(on ? "Выход включён" : "Выход отключён");
        outStateLableVal.setForeground(on ? GREEN : RED);
    }

    private void updateStateFromMeasurement(String key, String text) {
        if ("F".equals(key)) {
            AnswerValues av = parseF(text);
            if (av != null && av.getValues().length >= 11) {
                double term = av.getValues()[0];      // TermBM / 100 = °C
                double vpwr = av.getValues()[3];      // thre_V / 100 = V(pwr)
                double currRes = av.getValues()[8];   // CurrResF / 1000 = A → display mA
                termVal.setText(String.format(Locale.US, "%.2f °C", term));
                outPoverVoltVal.setText(String.format(Locale.US, "%.3f V", vpwr));
                consurptionMaVal.setText(String.format(Locale.US, "%.3f mA", currRes * 1000.0));
                return;
            }
        }

        if ("MMESU".equals(key) || "LOGO".equals(key) || "LOG".equals(key)) {
            parseMmesuIntoState(text, key);
            return;
        }

        if ("CONC?".equals(key)) {
            Matcher m = Pattern.compile(
                    "FEE:([\\d.+-]+)\\s+CUR:([\\d.+-]+)\\s+TER:([\\d.+-]+)\\s+HUD:([\\d.+-]+)\\s+PRS:([\\d.+-]+)\\s+PWR:([\\d.+-]+)",
                    Pattern.CASE_INSENSITIVE).matcher(text);
            if (m.find()) {
                consurptionMaVal.setText(m.group(2) + " mA");
                termVal.setText(m.group(3) + " °C");
                outPoverVoltVal.setText(m.group(6) + " V");
            }
        }
    }

    private void parseMmesuIntoState(String text, String key) {
        String t = text.trim();
        if (t.regionMatches(true, 0, "OK ", 0, 3)) {
            t = t.substring(3).trim();
        }

        // Extract V(pwr) — the V value closest before FPWR ON/OFF
        Matcher vpwr = Pattern.compile("([\\d.+-]+)\\s*V\\b.*?\\b(ON|OFF)\\b", Pattern.CASE_INSENSITIVE).matcher(t);
        if (vpwr.find()) {
            outPoverVoltVal.setText(vpwr.group(1) + " V");
        } else {
            Matcher vAll = Pattern.compile("([\\d.+-]+)\\s*V\\b", Pattern.CASE_INSENSITIVE).matcher(t);
            String lastV = null;
            int vCount = 0;
            while (vAll.find()) {
                lastV = vAll.group(1);
                vCount++;
                if (vCount == 3) break;
            }
            if (lastV != null) {
                outPoverVoltVal.setText(lastV + " V");
            }
        }

        Matcher fpwr = MMESU_FPWR.matcher(t);
        String lastFpwr = null;
        while (fpwr.find()) {
            lastFpwr = fpwr.group(1);
        }
        if (lastFpwr != null) {
            boolean on = "ON".equalsIgnoreCase(lastFpwr);
            statePowerLamp.setLampColor(on ? GREEN : RED);
            outStateLableVal.setText(on ? "Выход включён" : "Выход отключён");
            outStateLableVal.setForeground(on ? GREEN : RED);
        }

        Matcher temp = Pattern.compile("([\\d.+-]+)\\s*deg\\s*C", Pattern.CASE_INSENSITIVE).matcher(t);
        if (temp.find()) {
            termVal.setText(temp.group(1) + " °C");
        }

        Matcher ma = Pattern.compile("([\\d.+-]+)\\s*mA", Pattern.CASE_INSENSITIVE).matcher(t);
        String lastMa = null;
        while (ma.find()) {
            lastMa = ma.group(1);
        }
        if (lastMa != null) {
            consurptionMaVal.setText(lastMa + " mA");
        }
    }

    private AnswerValues parseF(String text) {
        try {
            SingleCommand cmd = registry.getCommandList().getCommand("F");
            if (cmd == null) return null;
            // F response may be raw bytes; try as ASCII (tabs preserved better from service single-line)
            return cmd.getResult(text.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Pretty parse for row label — avoids scientific notation for integers/SN.
     */
    private String tryParsePretty(String cmdKey, String text) {
        try {
            SingleCommand cmd = registry.getCommandList().getCommand(cmdKey);
            if (cmd == null) {
                return null;
            }
            AnswerValues av = cmd.getResult(text.getBytes(StandardCharsets.US_ASCII));
            if (av == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < av.getValues().length; i++) {
                if (i > 0) sb.append(" | ");
                double v = av.getValues()[i];
                String unit = av.getUnits()[i] != null ? av.getUnits()[i] : "";
                // SN / large integer-like → plain long
                if (unit.contains("SN") || (Math.abs(v) >= 1_000_000 && Math.rint(v) == v)) {
                    sb.append(String.format(Locale.US, "%.0f", v));
                } else if (Math.rint(v) == v && Math.abs(v) < 1e9) {
                    sb.append(String.format(Locale.US, "%.0f", v));
                } else {
                    sb.append(String.format(Locale.US, "%.4f", v));
                }
                sb.append(unit);
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
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
        urtmodPendingBaud = null;
        bannerSeen = false;
        fwDevice = "";
        fwSerial = "";
        fwAddress = "";
        fwUrt = "";
        fwSw = "";
        comLamp.setLampColor(RED);
        portStatusLabel.setText("Порт закрыт");
        openPortBtn.setEnabled(true);
        closePortBtn.setEnabled(false);
        statusBar.setText("Порт закрыт");
        resetDeviceInfo();
    }

    /**
     * After port open, wait briefly for boot banner, then auto-query:
     * - banner seen → F + MMESU (fill measurements)
     * - no banner  → SREV? + SRAL? (fill firmware info)
     */
    private void scheduleAutoProbe() {
        bannerSeen = false;
        new Timer(2000, e -> {
            ((Timer) e.getSource()).stop();
            if (!service.isConnected()) return;
            if (bannerSeen) {
                appendLog("◆ Banner detected → auto F + MMESU");
                sendCommand("F", "F");
                new Timer(500, e2 -> {
                    ((Timer) e2.getSource()).stop();
                    if (service.isConnected()) sendCommand("MMESU", "MMESU");
                }).start();
            } else {
                appendLog("◆ No banner → auto SREV? + SRAL?");
                sendCommand("SREV?", "SREV?");
                new Timer(300, e2 -> {
                    ((Timer) e2.getSource()).stop();
                    if (service.isConnected()) sendCommand("SRAL?", "SRAL?");
                }).start();
                new Timer(600, e2 -> {
                    ((Timer) e2.getSource()).stop();
                    if (service.isConnected()) sendCommand("MMESU", "MMESU");
                }).start();
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
        devInfo.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(devInfo, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        fwInfo = new JPanel();
        fwInfo.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        devInfo.add(fwInfo, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        serialNumLbl = new JLabel();
        serialNumLbl.setText("Серийный номер");
        fwInfo.add(serialNumLbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serialNumVal = new JLabel();
        serialNumVal.setText("- - -");
        fwInfo.add(serialNumVal, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fwVersionLbl = new JLabel();
        fwVersionLbl.setText("Версия прошивки");
        fwInfo.add(fwVersionLbl, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fwVersionVal = new JLabel();
        fwVersionVal.setText("- - -");
        fwInfo.add(fwVersionVal, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nAddrrLbl = new JLabel();
        nAddrrLbl.setText("Сетевой адрес");
        fwInfo.add(nAddrrLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        nAddrrVal = new JLabel();
        nAddrrVal.setText("- - -");
        fwInfo.add(nAddrrVal, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urtSpeedLbl = new JLabel();
        urtSpeedLbl.setText("Скорость UART на 10-ти пиновом выходе");
        fwInfo.add(urtSpeedLbl, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        urtSpeedVal = new JLabel();
        urtSpeedVal.setText("- - -");
        fwInfo.add(urtSpeedVal, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        hwInfo = new JPanel();
        hwInfo.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        devInfo.add(hwInfo, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        outStateLableLbl = new JLabel();
        outStateLableLbl.setText("Состояние выхода");
        hwInfo.add(outStateLableLbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        hwInfo.add(panel3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outStateLamp = new JPanel();
        outStateLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(outStateLamp);
        outStateLableVal = new JLabel();
        outStateLableVal.setText("Выход отключён");
        panel3.add(outStateLableVal);
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        hwInfo.add(panel4, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        outPoverVoltLbl = new JLabel();
        outPoverVoltLbl.setText("Питание, В");
        panel4.add(outPoverVoltLbl, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outPoverVoltVal = new JLabel();
        outPoverVoltVal.setText("- - -");
        panel4.add(outPoverVoltVal, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        termLbl = new JLabel();
        termLbl.setText("Температура");
        panel4.add(termLbl, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        termVal = new JLabel();
        termVal.setText("- - -");
        panel4.add(termVal, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        consurptionMaLbl = new JLabel();
        consurptionMaLbl.setText("Потребление, мА");
        panel4.add(consurptionMaLbl, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        consurptionMaVal = new JLabel();
        consurptionMaVal.setText("- - -");
        panel4.add(consurptionMaVal, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
