package org.example.gui.devices.arduino.emulator.mipex;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.extern.slf4j.Slf4j;
import org.example.device.command.SingleCommand;
import org.example.device.protArdMipexEmu.ArdMipexEmuCommandRegistry;
import org.example.services.AnswerValues;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Панель управления ARD_MIPEX_EMU (Arduino Mipex / multi-mode emulator).
 * Протокол: {@code mip_emu.md}.
 */
@Slf4j
public class MipexEmuMain {

    private static final String PREFS_NODE = "org/example/gui/arduino/mipexemu";
    private static final String LAST_PORT = "lastPort";
    private static final String LAST_BAUD = "lastBaud";
    private static final int DEFAULT_BAUD = MipexEmuCommunicationService.DEFAULT_BAUD;

    private static final Color GREEN = new Color(0, 180, 80);
    private static final Color RED = new Color(200, 40, 40);
    private static final Color ORANGE = new Color(180, 120, 0);
    private static final Color GRAY = Color.DARK_GRAY;
    /** Filled header values: white on dark chip (readable on light and dark L&F). */
    private static final Color HEADER_FG = Color.WHITE;
    private static final Color HEADER_BG = new Color(40, 42, 46);
    private static final Color HEADER_EMPTY = new Color(160, 160, 160);

    /**
     * Boot banner (tabs/spaces/* wrappers):
     * <pre>
     * *********************************
     * *	Device: sens-emu	*
     * *	LogDel: 4		*
     * *	SerNum: 08500016	*
     * *	Adres: 255		*
     * *	SW:2.03 12.01.2025 	*
     * *********************************
     * Timer 0 is 0
     * ...
     * </pre>
     */
    private static final Pattern BANNER_DEVICE = Pattern.compile(
            "Device\\s*:\\s*([^\\r\\n\\t*]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNER_LOGDEL = Pattern.compile(
            "LogDel\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNER_SN = Pattern.compile(
            "SerNum\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNER_ADDR = Pattern.compile(
            "Adres\\s*:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNER_SW = Pattern.compile(
            "SW\\s*:\\s*([^\\r\\n\\t*]+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNER_TIMER = Pattern.compile(
            "Timer\\s+(\\d+)\\s+is\\s+(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern BANNER_EDGE = Pattern.compile("^[*\\s]+$");
    private static final Pattern URTS_SHOWED = Pattern.compile(
            "showed\\s*:\\s*([TF])", Pattern.CASE_INSENSITIVE);
    private static final Pattern MODE_DIGITS = Pattern.compile("(\\d{1,4})");
    /** Unsolicited timer cycle line, e.g. {@code Set: 11} */
    private static final Pattern SET_LINE = Pattern.compile(
            "^Set\\s*:\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);

    private static final String[] TIMER_DESC = {
            "0 — служебный",
            "1 — служебный",
            "2 — авто-вывод F (лог A0–A8)",
            "3 — мигание D13 (alive)",
            "4 — не используется",
            "5 — не использовать (конфликт с mipex)",
            "6 — не использовать (UART stress)",
            "7 — фон. измерение напряжений",
            "8 — цикл. изменение показаний",
    };

    private static final String[] FMOD_NAMES = {
            "0 legacy",
            "1 V-meter",
            "2 Mipex II",
            "3 Mipex-14",
            "4 electrochem",
            "5 thermocat",
            "6 photoion",
    };

    private volatile String fwDevice = "";
    private volatile String fwSerial = "";
    private volatile String fwAddress = "";
    private volatile String fwSw = "";
    private volatile String fwLogDel = "";

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
    private JLabel emuDevTypeLblLbl;
    private JLabel emuDevTypeVal;
    private JLabel emuConcLbl;
    private JLabel emuConcVal;
    private JLabel emuConcDoggyLbl;
    private JLabel emuConcDoggyVal;
    private JLabel emuSetLbl;
    private JLabel emuSetVal;

    private final MipexEmuCommunicationService service = new MipexEmuCommunicationService();
    private final ArdMipexEmuCommandRegistry registry = new ArdMipexEmuCommandRegistry();
    private final LampIndicator comLamp = new LampIndicator();

    private final Map<String, JComponent> responseWidgets = new LinkedHashMap<>();
    /**
     * CMOD response label per timer index 0..8
     */
    private final JLabel[] timerStateLabels = new JLabel[9];
    private final JButton[] timerToggleButtons = new JButton[9];
    private final JTextField[] timerPeriodFields = new JTextField[9];
    private final boolean[] timerOn = new boolean[9];

    private volatile String lastSentCommand = "";
    private volatile boolean bannerSeen = false;
    /**
     * Sequential auto-probe after connect: F? → F → @ (no lastSentCommand races).
     */
    private final Deque<String> autoProbeQueue = new ArrayDeque<>();
    private volatile boolean autoProbeActive = false;
    /**
     * Key of the probe command currently awaiting a reply (null if idle).
     */
    private volatile String autoProbeCurrent = null;

    public MipexEmuMain() {
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

        styleHeaderValue(serialNumVal);
        styleHeaderValue(fwVersionVal);
        styleHeaderValue(nAddrrVal);
        styleHeaderValue(emuDevTypeVal);
        styleHeaderValue(emuConcVal);
        styleHeaderValue(emuConcDoggyVal);
        styleHeaderValue(emuSetVal);

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

        // 1) Simulation settings — top
        addSimulationSection();

        // 2) Timers — primary purpose of this panel
        addTimersSection();

        // 3) UART / bridge / debug
        addSection("UART / мост / отладка", new Cmd[]{
                Cmd.of("URTS1", "URTS1 — показ UART1"),
                Cmd.of("URTS2", "URTS2 — показ UART2"),
                Cmd.of("URTS3", "URTS3 — показ UART3"),
                Cmd.of("RSND", "RSND — мост UART1↔2"),
                Cmd.of("URT0", "URT0 — тест → UART0"),
                Cmd.of("URT1", "URT1 — тест → UART1"),
                Cmd.of("URT2", "URT2 — тест → UART2"),
                Cmd.of("URT3", "URT3 — тест → UART3"),
                Cmd.of("PING", "PING — целостность UART"),
                Cmd.multiline("OSLT", "OSLT — нагрузочный тест"),
                Cmd.of("SUCU", "SUCU — unknown cmds"),
                Cmd.of("SKCU", "SKCU — known cmds"),
                Cmd.of("SRKC", "SRKC — answers"),
        });

        // 4) System
        addSection("Система", new Cmd[]{
                Cmd.of("SREV?", "SREV? — версия ПО"),
                Cmd.of("SRAL?", "SRAL? — серийный номер"),
                Cmd.of("%**", "%** — адрес"),
                Cmd.withArg("S085", "S085 — смена S/N (5 цифр)", "0"),
                Cmd.withArg("!", "!xxYY — смена адреса", "203"),
        });

        // 5) Measurements / rest
        addSection("Измерения / прочее", new Cmd[]{
                Cmd.of("F", "F — fixed-width (Mipex)"),
                Cmd.of("F?", "F? — текущий режим имитации"),
                Cmd.of("CCS", "CCS — концентрация (ASCII)"),
                Cmd.of("@", "@ — концентрация (binary)"),
                Cmd.of("LOG", "LOG — авто-отправка F on/off"),
                Cmd.of("CONC?", "CONC? — концентрация"),
                Cmd.multiline("CONST?", "CONST? — параметры"),
                Cmd.of("ID", "ID — ID оптического сенсора"),
                Cmd.of("TERM?", "TERM? — температура"),
                Cmd.of("MMES", "MMES — разовое A4/A5"),
        });

        commandsPlaceholder.add(Box.createVerticalGlue());
        commandsPlaceholder.revalidate();
        commandsPlaceholder.repaint();
    }

    private void addSimulationSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Настройки имитации",
                TitledBorder.LEFT, TitledBorder.TOP));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton readAllBtn = new JButton("Читать всё (шапка: F? → F → @)");
        readAllBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        readAllBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        readAllBtn.setToolTipText("Опросить режим (F?), концентрацию F (C1) и @ — заполнить шапку");
        readAllBtn.addActionListener(e -> {
            if (!service.isConnected()) {
                JOptionPane.showMessageDialog(mainPanel, "Сначала откройте COM-порт");
                return;
            }
            startHeaderProbe("ручной опрос");
        });
        section.add(readAllBtn);
        section.add(Box.createVerticalStrut(6));

        Cmd[] cmds = {
                Cmd.withArg("FMOD", "FMOD — режим (2=Mipex II)", "2"),
                Cmd.withArg("SDAC", "SDAC — ЦАП / база (0–500)", "100"),
                Cmd.withArg("SMCV", "SMCV — предел имитации", "44"),
                Cmd.withArg("KALB", "KALB — принуд. C0/C1", "20"),
                Cmd.withArg("SSTAT", "SSTAT — статус в F", "31"),
                Cmd.withArg("TERM", "TERM — уст. температуры", "2500"),
                Cmd.withArg("SAPR", "SAPR — усреднение ×100", "1"),
                Cmd.of("GMCV", "GMCV — текущий MCV"),
        };
        for (Cmd c : cmds) {
            section.add(makeCommandRow(c));
            section.add(Box.createVerticalStrut(3));
        }
        commandsPlaceholder.add(section);
        commandsPlaceholder.add(Box.createVerticalStrut(6));
    }

    private void addTimersSection() {
        JPanel section = new JPanel();
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Таймеры задач (CMOD / TMOD)",
                TitledBorder.LEFT, TitledBorder.TOP));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel hint = new JLabel(
                "<html><b>Вкл/Выкл</b> = CMOD (переключение задачи). "
                        + "<b>Уст. период</b> = TMOD <i>xxYY</i> (xx = сек 01–99, YY = № задачи). "
                        + "Состояние: баннер <code>Timer N is 0/1</code>.</html>");
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setBorder(BorderFactory.createEmptyBorder(2, 4, 6, 4));
        section.add(hint);

        for (int t = 0; t <= 8; t++) {
            section.add(makeTimerRow(t));
            section.add(Box.createVerticalStrut(3));
        }

        commandsPlaceholder.add(section);
        commandsPlaceholder.add(Box.createVerticalStrut(6));
    }

    private JPanel makeTimerRow(int timer) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        String desc = TIMER_DESC[Math.min(timer, TIMER_DESC.length - 1)];

        JButton toggleBtn = new JButton("Вкл/Выкл");
        toggleBtn.setPreferredSize(new Dimension(90, 28));
        toggleBtn.setToolTipText("CMOD " + String.format(Locale.US, "%04d", timer)
                + " — включить/выключить: " + desc);
        timerToggleButtons[timer] = toggleBtn;

        JLabel numLbl = new JLabel(String.format(Locale.US, "T%d", timer));
        numLbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        numLbl.setPreferredSize(new Dimension(28, 28));

        JLabel descLbl = new JLabel(desc);
        descLbl.setPreferredSize(new Dimension(260, 28));
        descLbl.setToolTipText(desc);

        JLabel stateLbl = new JLabel("—");
        stateLbl.setFont(new Font(Font.MONOSPACED, Font.BOLD, 12));
        stateLbl.setForeground(GRAY);
        stateLbl.setPreferredSize(new Dimension(64, 28));
        timerStateLabels[timer] = stateLbl;
        responseWidgets.put("CMOD_" + timer, stateLbl);

        JTextField periodField = new JTextField("05", 3);
        periodField.setMaximumSize(new Dimension(40, 28));
        periodField.setToolTipText("Период в секундах (1–99) → TMOD xx" + String.format(Locale.US, "%02d", timer));
        timerPeriodFields[timer] = periodField;

        JButton periodBtn = new JButton("Уст. период");
        periodBtn.setPreferredSize(new Dimension(110, 28));
        periodBtn.setToolTipText("TMOD xxYY — xx=сек, YY=задача " + timer
                + ". Пример: 05 + T" + timer + " → TMOD "
                + String.format(Locale.US, "%02d%02d", 5, timer));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(toggleBtn);
        left.add(numLbl);
        left.add(descLbl);
        left.add(stateLbl);
        left.add(new JLabel("сек:"));
        left.add(periodField);
        left.add(periodBtn);

        row.add(left, BorderLayout.WEST);

        final int t = timer;
        toggleBtn.addActionListener(e -> {
            if (!service.isConnected()) {
                JOptionPane.showMessageDialog(mainPanel, "Сначала откройте COM-порт");
                return;
            }
            String cmd = String.format(Locale.US, "CMOD %04d", t);
            stateLbl.setText("…");
            stateLbl.setForeground(ORANGE);
            sendCommand(cmd, "CMOD_" + t);
        });

        periodBtn.addActionListener(e -> {
            if (!service.isConnected()) {
                JOptionPane.showMessageDialog(mainPanel, "Сначала откройте COM-порт");
                return;
            }
            int sec;
            try {
                sec = Integer.parseInt(periodField.getText().trim());
                if (sec < 1 || sec > 99) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException ex) {
                stateLbl.setText("ERR сек");
                stateLbl.setForeground(RED);
                return;
            }
            // TMOD xxYY: xx = seconds (01–99), YY = task number
            String cmd = String.format(Locale.US, "TMOD %02d%02d", sec, t);
            stateLbl.setText("… " + sec + "s");
            stateLbl.setForeground(ORANGE);
            sendCommand(cmd, "TMOD_" + t);
            responseWidgets.put("TMOD_" + t, stateLbl);
        });

        return row;
    }

    private void resetDeviceInfo() {
        clearHeaderValue(serialNumVal);
        clearHeaderValue(fwVersionVal);
        clearHeaderValue(nAddrrVal);
        clearHeaderValue(emuDevTypeVal);
        clearHeaderValue(emuConcVal);
        clearHeaderValue(emuConcDoggyVal);
        clearHeaderValue(emuSetVal);
        for (int i = 0; i < timerStateLabels.length; i++) {
            timerOn[i] = false;
            if (timerStateLabels[i] != null) {
                timerStateLabels[i].setText("—");
                timerStateLabels[i].setForeground(GRAY);
            }
            if (timerToggleButtons[i] != null) {
                timerToggleButtons[i].setText("Вкл/Выкл");
            }
        }
    }

    private static void clearHeaderValue(JLabel lbl) {
        if (lbl == null) {
            return;
        }
        lbl.setText("- - -");
        lbl.setForeground(HEADER_EMPTY);
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
            argField.setToolTipText(argTooltip(c.key));
        }

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(btn);
        if (argField != null) {
            left.add(new JLabel(argPrefix(c.key)));
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

    private static String argPrefix(String key) {
        return switch (key) {
            case "S085" -> "sn=";
            case "!" -> "xxYY=";
            case "TMOD" -> "xxYY=";
            default -> "n=";
        };
    }

    private static String argTooltip(String key) {
        return switch (key) {
            case "FMOD" -> "0=legacy, 1=V-meter, 2=Mipex II, 3=Mipex-14, 4–6=другие";
            case "TMOD" -> "xx = сек (01–99), YY = номер задачи (напр. 0305)";
            case "S085" -> "5 цифр серийного номера (без префикса S085)";
            case "!" -> "4 цифры: xx=текущий адрес, YY=новый";
            case "SMCV" -> "1–9998, предел имитации";
            case "SDAC" -> "0–500, ЦАП / база имитации";
            default -> "Числовой аргумент (до 4 цифр)";
        };
    }

    private void onCommandClick(Cmd c, JTextField argRef, JComponent respWidget) {
        if (!service.isConnected()) {
            JOptionPane.showMessageDialog(mainPanel, "Сначала откройте COM-порт");
            return;
        }

        String toSend = c.key;
        if (argRef != null) {
            String raw = argRef.getText().trim();
            try {
                int n = Integer.parseInt(raw);
                if ("S085".equals(c.key)) {
                    toSend = String.format(Locale.US, "S085%05d", Math.max(0, Math.min(99999, n)));
                } else if ("!".equals(c.key)) {
                    toSend = String.format(Locale.US, "!%04d", Math.max(0, Math.min(9999, n)));
                } else {
                    toSend = String.format(Locale.US, "%s %04d", c.key, Math.max(0, Math.min(9999, n)));
                }
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
            for (String line : text.split("\n", -1)) {
                if (!line.isBlank()) {
                    // binary @ often looks empty/garbled — log hex-friendly preview
                    if (looksBinary(line)) {
                        appendLog("← " + toHexPreview(line));
                    } else {
                        appendLog("← " + line);
                    }
                }
            }

            // Unsolicited timer cycle: "Set: 11" → header (do not treat as command answer)
            if (tryApplySetMessage(text)) {
                statusBar.setText("Set: " + emuSetVal.getText());
                return;
            }

            // Banner may arrive line-by-line or as one multi-line block
            if (tryApplyBootBanner(text)) {
                statusBar.setText("Устройство: " + nullToDash(fwDevice)
                        + "  SN:" + nullToDash(fwSerial)
                        + "  addr:" + nullToDash(fwAddress)
                        + "  SW:" + nullToDash(fwSw)
                        + "  LogDel:" + nullToDash(fwLogDel));
                // pure banner frames must not be treated as command answers
                if (isBannerOnly(text)) {
                    return;
                }
            }

            statusBar.setText("RX: " + summarize(text, 60));
            String key = lastSentCommand;
            if (key == null || key.isEmpty()) {
                return;
            }

            // Timer CMOD / TMOD
            if (key.startsWith("CMOD_") || key.startsWith("TMOD_")) {
                handleTimerResponse(key, text);
                advanceAutoProbeIfNeeded(key);
                return;
            }

            JComponent resp = responseWidgets.get(key);
            if (resp == null) {
                // probe keys F? / F / @ always update header even without row widget
                updateStateFromMeasurement(key, text);
                advanceAutoProbeIfNeeded(key);
                return;
            }

            if ("OSLT".equals(key) || "CONST?".equals(key)) {
                if (resp instanceof JTextArea area) {
                    String current = area.getText();
                    if ("…".equals(current.trim()) || current.trim().isEmpty()) {
                        area.setText(text);
                    } else {
                        area.append("\n" + text);
                    }
                    area.setCaretPosition(0);
                    area.setForeground(GREEN);
                } else {
                    setResponseText(resp, summarize(text, 100), GREEN);
                }
                advanceAutoProbeIfNeeded(key);
                return;
            }

            if ("SRAL?".equals(key) || "SREV?".equals(key) || "%**".equals(key)) {
                String clean = text.trim();
                setResponseText(resp, clean, GREEN);
                updateFirmwareFromCommand(key, clean);
                advanceAutoProbeIfNeeded(key);
                return;
            }

            if (key.startsWith("URTS")) {
                applyUrtsResponse(key, text, resp);
                advanceAutoProbeIfNeeded(key);
                return;
            }

            if ("@".equals(key)) {
                applyAtResponse(text, resp);
                advanceAutoProbeIfNeeded(key);
                return;
            }

            if ("F".equals(key) || "CCS".equals(key) || "F?".equals(key)
                    || "CONC?".equals(key) || "TERM?".equals(key)
                    || "LOG".equals(key) || "GMCV".equals(key) || "FMOD".equals(key)) {
                String parseHint = tryParsePretty(key, text);
                setResponseText(resp, parseHint != null ? parseHint : summarize(text, 100), GREEN);
                updateStateFromMeasurement(key, text);
                advanceAutoProbeIfNeeded(key);
                return;
            }

            String parseHint = tryParsePretty(key, text);
            if (parseHint != null) {
                setResponseText(resp, parseHint, GREEN);
            } else {
                setResponseText(resp, summarize(text, 100), GREEN);
            }
            advanceAutoProbeIfNeeded(key);
        });
    }

    private static String nullToDash(String s) {
        return s == null || s.isEmpty() ? "—" : s;
    }

    /**
     * True if every non-empty line looks like boot banner content.
     */
    private boolean isBannerOnly(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean any = false;
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            any = true;
            if (!isBannerLine(t)) {
                return false;
            }
        }
        return any;
    }

    private boolean isBannerLine(String t) {
        if (BANNER_EDGE.matcher(t).matches()) {
            return true;
        }
        return BANNER_DEVICE.matcher(t).find()
                || BANNER_LOGDEL.matcher(t).find()
                || BANNER_SN.matcher(t).find()
                || BANNER_ADDR.matcher(t).find()
                || BANNER_SW.matcher(t).find()
                || BANNER_TIMER.matcher(t).find();
    }

    private void handleTimerResponse(String key, String text) {
        int timer;
        try {
            timer = Integer.parseInt(key.substring(key.indexOf('_') + 1));
        } catch (Exception e) {
            return;
        }
        JLabel lbl = timerStateLabels[Math.min(timer, timerStateLabels.length - 1)];
        if (lbl == null) {
            return;
        }
        if (key.startsWith("TMOD_")) {
            lbl.setText("период OK");
            lbl.setForeground(GREEN);
            // restore ON/OFF after brief feedback
            new Timer(900, e -> {
                ((Timer) e.getSource()).stop();
                applyTimerStateLabel(timer);
            }).start();
            return;
        }
        // CMOD toggles — banner is source of truth; flip local guess if no explicit state
        String t = text.trim().toUpperCase(Locale.ROOT);
        if (t.contains("ON") || t.endsWith(" 1") || t.equals("1") || t.contains("ENABLE")) {
            timerOn[timer] = true;
        } else if (t.contains("OFF") || t.endsWith(" 0") || t.equals("0") || t.contains("DISABLE")) {
            timerOn[timer] = false;
        } else {
            // toggle locally when board just ACKs
            timerOn[timer] = !timerOn[timer];
        }
        applyTimerStateLabel(timer);
    }

    private void applyTimerStateLabel(int timer) {
        if (timer < 0 || timer >= timerStateLabels.length || timerStateLabels[timer] == null) {
            return;
        }
        JLabel lbl = timerStateLabels[timer];
        if (timerOn[timer]) {
            lbl.setText("● ON");
            lbl.setForeground(GREEN);
        } else {
            lbl.setText("● OFF");
            lbl.setForeground(RED);
        }
        JButton btn = timerToggleButtons[timer];
        if (btn != null) {
            btn.setText(timerOn[timer] ? "Выключить" : "Включить");
        }
    }

    private void applyUrtsResponse(String key, String text, JComponent resp) {
        Matcher m = URTS_SHOWED.matcher(text);
        if (m.find()) {
            boolean on = "T".equalsIgnoreCase(m.group(1));
            setResponseText(resp, on ? "● ON (show)" : "● OFF (hide)", on ? GREEN : RED);
        } else {
            setResponseText(resp, summarize(text, 80), GREEN);
        }
    }

    private void applyAtResponse(String text, JComponent resp) {
        byte[] raw = text.getBytes(StandardCharsets.ISO_8859_1);
        AnswerValues av = null;
        try {
            SingleCommand cmd = registry.getCommandList().getCommand("@");
            if (cmd != null) {
                av = cmd.getResult(raw);
            }
        } catch (Exception ignored) {
            // fall through
        }
        if (av != null && av.getValues().length > 0) {
            long conc = Math.round(av.getValues()[0]);
            String s = String.valueOf(conc);
            setResponseText(resp, s + " C1", GREEN);
            setHeaderValue(emuConcDoggyVal, s);
        } else {
            setResponseText(resp, toHexPreview(text), ORANGE);
        }
    }

    /**
     * Parse boot banner (Device / SerNum / Adres / SW / Timer N is X).
     * Handles one line or a whole multi-line block; applies <b>all</b> Timer lines.
     */
    private boolean tryApplyBootBanner(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        boolean matched = false;
        // Split on CR/LF so a single flush with several banner lines still works
        String[] lines = text.split("\\R");
        for (String rawLine : lines) {
            if (applyBannerLine(rawLine)) {
                matched = true;
            }
        }
        // Also scan whole block once for fields (covers odd separators)
        if (!matched) {
            matched = applyBannerFields(text);
        } else {
            // catch any timers missed when lines were weirdly joined
            applyAllTimers(text);
        }
        if (matched) {
            bannerSeen = true;
        }
        return matched;
    }

    private boolean applyBannerLine(String rawLine) {
        String t = rawLine == null ? "" : rawLine.trim();
        if (t.isEmpty() || BANNER_EDGE.matcher(t).matches()) {
            return false;
        }
        return applyBannerFields(t);
    }

    private boolean applyBannerFields(String t) {
        boolean matched = false;
        Matcher m;

        m = BANNER_DEVICE.matcher(t);
        if (m.find()) {
            fwDevice = m.group(1).trim();
            matched = true;
        }

        m = BANNER_LOGDEL.matcher(t);
        if (m.find()) {
            fwLogDel = m.group(1).trim();
            matched = true;
        }

        m = BANNER_SN.matcher(t);
        if (m.find()) {
            fwSerial = m.group(1).trim();
            setHeaderValue(serialNumVal, fwSerial);
            matched = true;
        }

        m = BANNER_ADDR.matcher(t);
        if (m.find()) {
            fwAddress = m.group(1).trim();
            setHeaderValue(nAddrrVal, fwAddress);
            matched = true;
        }

        m = BANNER_SW.matcher(t);
        if (m.find()) {
            fwSw = m.group(1).trim();
            setHeaderValue(fwVersionVal, fwSw);
            matched = true;
        }

        if (applyAllTimers(t)) {
            matched = true;
        }
        return matched;
    }

    private boolean applyAllTimers(String t) {
        Matcher m = BANNER_TIMER.matcher(t);
        boolean any = false;
        while (m.find()) {
            try {
                int num = Integer.parseInt(m.group(1));
                int on = Integer.parseInt(m.group(2));
                if (num >= 0 && num < timerOn.length) {
                    timerOn[num] = on != 0;
                    applyTimerStateLabel(num);
                    any = true;
                }
            } catch (NumberFormatException ignored) {
                // ignore bad timer line
            }
        }
        return any;
    }

    private void updateFirmwareFromCommand(String key, String clean) {
        if ("SREV?".equals(key) && !clean.isEmpty()) {
            fwSw = clean;
            setHeaderValue(fwVersionVal, clean);
        } else if ("SRAL?".equals(key) && !clean.isEmpty()) {
            fwSerial = clean.replaceAll("[^0-9]", "");
            setHeaderValue(serialNumVal, fwSerial);
        } else if ("%**".equals(key) && !clean.isEmpty()) {
            setHeaderValue(nAddrrVal, clean.replaceAll("[^0-9]", ""));
        }
    }

    private void updateStateFromMeasurement(String key, String text) {
        if ("F".equals(key) || "CCS".equals(key)) {
            AnswerValues av = parseCommand(key.equals("CCS") ? "CCS" : "F", text);
            if (av == null && "CCS".equals(key)) {
                av = parseCommand("F", text);
            }
            // Soft C1 extract when CRC/layout check fails (common on raw emulator dumps)
            if (av == null) {
                Long softC1 = softExtractC1(text);
                if (softC1 != null) {
                    setHeaderValue(emuConcVal, String.valueOf(softC1));
                    return;
                }
            }
            if (av != null && av.getValues().length > 0) {
                // Full F/CCS: C1 at index 8; short CCS: single value
                double c1 = av.getValues().length >= 11
                        ? av.getValues()[8]
                        : av.getValues()[av.getValues().length >= 9 ? 8 : 0];
                setHeaderValue(emuConcVal, String.format(Locale.US, "%.0f", c1));
                if (av.getValues().length >= 11 && av.getValues()[10] > 0) {
                    setHeaderValue(serialNumVal, String.format(Locale.US, "%.0f", av.getValues()[10]));
                }
            }
            return;
        }

        if ("F?".equals(key) || "FMOD".equals(key)) {
            // Board may answer with a name ("MIPEX\tII") or a numeric code (0002)
            String label = formatSimulationMode(text);
            if (label != null && !label.isEmpty()) {
                setHeaderValue(emuDevTypeVal, label);
            }
            return;
        }

        if ("@".equals(key)) {
            applyAtResponse(text, responseWidgets.get("@"));
        }
    }

    /**
     * Unsolicited cycle from timer 8: {@code Set: 11} → header Set + C1 fields.
     * Must not steal normal command answers (only pure Set frames).
     */
    private boolean tryApplySetMessage(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String last = null;
        for (String line : text.split("\\R")) {
            String t = line.trim();
            if (t.isEmpty()) {
                continue;
            }
            Matcher m = SET_LINE.matcher(t);
            if (!m.matches()) {
                return false;
            }
            last = m.group(1);
        }
        if (last == null) {
            return false;
        }
        setHeaderValue(emuSetVal, last);
        // Timer cycle drives the simulated concentration — mirror into C1 fields
        setHeaderValue(emuConcVal, last);
        setHeaderValue(emuConcDoggyVal, last);
        return true;
    }

    /**
     * F? / FMOD display: {@code MIPEX\tII} → {@code MIPEX II}, or {@code 0002} → {@code 2 Mipex II}.
     */
    private static String formatSimulationMode(String raw) {
        if (raw == null) {
            return null;
        }
        // normalize tabs/newlines → single spaces
        String text = raw.replace('\t', ' ')
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replaceAll(" +", " ")
                .trim();
        if (text.isEmpty()) {
            return null;
        }

        String compact = text.toUpperCase(Locale.ROOT).replace(" ", "").replace("-", "");
        // Named modes from firmware (F? → e.g. "MIPEX\tII")
        if (compact.contains("MIPEX14") || compact.contains("MIPEXIII") || compact.equals("MIPEX3")) {
            return FMOD_NAMES[3];
        }
        if (compact.contains("MIPEXII") || compact.contains("MIPEX2")) {
            return FMOD_NAMES[2];
        }
        if (compact.contains("MIPEX")) {
            return text; // unknown Mipex variant — show board string as-is
        }

        // Pure / leading numeric mode code
        Matcher mm = MODE_DIGITS.matcher(text);
        if (mm.find()) {
            try {
                int mode = Integer.parseInt(mm.group(1));
                if (mode >= 0 && mode < FMOD_NAMES.length) {
                    return FMOD_NAMES[mode];
                }
                return String.valueOf(mode);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return text;
    }

    private AnswerValues parseCommand(String cmdKey, String text) {
        try {
            SingleCommand cmd = registry.getCommandList().getCommand(cmdKey);
            if (cmd == null) {
                return null;
            }
            return cmd.getResult(text.getBytes(StandardCharsets.ISO_8859_1));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Mipex F layout: C1 is 5 ASCII digits at offset 49 (after optional SO 0x0E at [0]).
     * Used when full CRC-checked parse fails.
     */
    private static Long softExtractC1(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        byte[] raw = text.getBytes(StandardCharsets.ISO_8859_1);
        // Prefer fixed F offsets 49..53 (frame may start with SO 0x0E; offsets are absolute)
        if (raw.length >= 54) {
            Long v = asciiDigits(raw, 49, 5);
            if (v != null) {
                return v;
            }
        }
        // Tab/space separated: take field that looks like C1 (often 9th numeric token)
        Matcher m = Pattern.compile("(\\d{4,5})").matcher(text);
        String last = null;
        int count = 0;
        while (m.find()) {
            last = m.group(1);
            count++;
            if (count == 9) {
                return Long.parseLong(last);
            }
        }
        return last != null ? Long.parseLong(last) : null;
    }

    private static Long asciiDigits(byte[] raw, int off, int len) {
        if (raw == null || off < 0 || off + len > raw.length) {
            return null;
        }
        long v = 0;
        for (int i = 0; i < len; i++) {
            byte b = raw[off + i];
            if (b < '0' || b > '9') {
                return null;
            }
            v = v * 10 + (b - '0');
        }
        return v;
    }

    private String tryParsePretty(String cmdKey, String text) {
        try {
            SingleCommand cmd = registry.getCommandList().getCommand(cmdKey);
            if (cmd == null) {
                return null;
            }
            AnswerValues av = cmd.getResult(text.getBytes(StandardCharsets.ISO_8859_1));
            if (av == null) {
                return null;
            }
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < av.getValues().length; i++) {
                if (i > 0) {
                    sb.append(" | ");
                }
                double v = av.getValues()[i];
                String unit = av.getUnits()[i] != null ? av.getUnits()[i] : "";
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

    private static boolean looksBinary(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        int nonPrint = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < 0x09 || (c > 0x0D && c < 0x20) || c > 0x7E) {
                nonPrint++;
            }
        }
        return nonPrint > 0 && nonPrint >= s.length() / 2;
    }

    private static String toHexPreview(String s) {
        byte[] b = s.getBytes(StandardCharsets.ISO_8859_1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(b.length, 16); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", b[i] & 0xFF));
        }
        if (b.length > 16) {
            sb.append("…");
        }
        return sb.toString();
    }

    private static String summarize(String text, int max) {
        if (looksBinary(text)) {
            return toHexPreview(text);
        }
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
        bannerSeen = false;
        autoProbeActive = false;
        autoProbeCurrent = null;
        autoProbeQueue.clear();
        fwDevice = "";
        fwSerial = "";
        fwAddress = "";
        fwSw = "";
        fwLogDel = "";
        comLamp.setLampColor(RED);
        portStatusLabel.setText("Порт закрыт");
        openPortBtn.setEnabled(true);
        closePortBtn.setEnabled(false);
        statusBar.setText("Порт закрыт");
        resetDeviceInfo();
    }

    /**
     * After open: wait for boot banner, then header probe F? → F → @.
     */
    private void scheduleAutoProbe() {
        bannerSeen = false;
        autoProbeActive = false;
        autoProbeCurrent = null;
        autoProbeQueue.clear();
        new Timer(2000, e -> {
            ((Timer) e.getSource()).stop();
            if (!service.isConnected()) {
                return;
            }
            startHeaderProbe(bannerSeen ? "после баннера" : "баннер не пришёл");
        }).start();
    }

    /**
     * Sequential F? → F → @ to fill header (mode, C1, @).
     * Used after connect and by «Читать всё».
     */
    private void startHeaderProbe(String reason) {
        if (!service.isConnected()) {
            return;
        }
        autoProbeActive = false;
        autoProbeCurrent = null;
        autoProbeQueue.clear();
        appendLog("◆ Читать шапку (" + reason + "): F? → F → @");
        autoProbeQueue.addLast("F?");
        autoProbeQueue.addLast("F");
        autoProbeQueue.addLast("@");
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
        // Safety timeout if device does not answer
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
        // Only the outstanding probe reply advances the chain
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

    // ─── form scaffolding (matches MipexEmuMain.form) ─────────────────────

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
        fwInfo.setLayout(new GridLayoutManager(4, 4, new Insets(0, 0, 0, 0), -1, -1));
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
        emuDevTypeLblLbl = new JLabel();
        emuDevTypeLblLbl.setText("Режим эмуляции");
        fwInfo.add(emuDevTypeLblLbl, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuDevTypeVal = new JLabel();
        emuDevTypeVal.setText("- - -");
        fwInfo.add(emuDevTypeVal, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuConcLbl = new JLabel();
        emuConcLbl.setText("Имитируемая концентрация (C1)");
        fwInfo.add(emuConcLbl, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuConcVal = new JLabel();
        emuConcVal.setText("- - -");
        fwInfo.add(emuConcVal, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuConcDoggyLbl = new JLabel();
        emuConcDoggyLbl.setText("Имитируемая концентрация (@)");
        fwInfo.add(emuConcDoggyLbl, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuConcDoggyVal = new JLabel();
        emuConcDoggyVal.setText("- - -");
        fwInfo.add(emuConcDoggyVal, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuSetLbl = new JLabel();
        emuSetLbl.setText("Set (цикл таймера)");
        fwInfo.add(emuSetLbl, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        emuSetVal = new JLabel();
        emuSetVal.setText("- - -");
        fwInfo.add(emuSetVal, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
