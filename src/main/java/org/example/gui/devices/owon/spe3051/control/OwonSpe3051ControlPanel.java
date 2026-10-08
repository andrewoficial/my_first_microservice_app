package org.example.gui.devices.owon.spe3051.control;

import org.example.device.command.SingleCommand;
import org.example.device.protOwonSpe3051.OwonSpe3051CommandRegistry;
import org.example.gui.utilites.GuiUtilities;
import org.example.services.AnswerValues;
import ru.kantser.gui.buttons.Buttons;
import ru.kantser.gui.buttons.SkewButton;
import ru.kantser.gui.gauges.DefaultGaugeModel;
import ru.kantser.gui.gauges.Gauges;
import ru.kantser.gui.gauges.RoundGauge;
import ru.kantser.gui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.prefs.Preferences;

/**
 * Панель управления реальным прибором <b>OWON SPE3051</b>.
 *
 * <p>Собрана из виджетов библиотеки {@code ru.kantser.gui}. Обмен идёт
 * ASCII/SCPI-протоколом {@code org.example.device.protOwonSpe3051}
 * (115200 8N1, конец строки CR+LF).
 *
 * <p>Чтение (запросы с {@code ?}): {@code *IDN?}, {@code MEAS:VOLT?},
 * {@code MEAS:CURR?}, {@code MEAS:POW?}, {@code OUTPut?}, {@code VOLT?},
 * {@code CURR?}, {@code VOLT:LIM?}, {@code CURR:LIM?}.<br>
 * Запись (ответа не ждём): {@code VOLT}, {@code CURR}, {@code OUTPut},
 * {@code VOLT:LIM}, {@code CURR:LIM}.
 *
 * <p>Спидометры <b>Voltage Setting / Current Setting / OVP / OCP</b> редактируемые:
 * их стрелки вверх/вниз и поле ввода меняют уставку и сразу шлют команду прибору.
 */
public class OwonSpe3051ControlPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/owon/spe3051/control";
    private static final String PREFS_KEY_PORT = "lastPort";

    private static final double MAX_VOLTAGE = 24.0;
    private static final double MAX_CURRENT = 5.0;
    private static final double MAX_POWER = MAX_VOLTAGE * MAX_CURRENT;
    private static final double VOLTAGE_STEP = 0.5;
    private static final double CURRENT_STEP = 0.1;

    private static final byte[] CR_LF = {13, 10};

    private static final String CMD_IDN = "*IDN?";
    private static final String CMD_MEAS_VOLT = "MEAS:VOLT?";
    private static final String CMD_MEAS_CURR = "MEAS:CURR?";
    private static final String CMD_MEAS_POW = "MEAS:POW?";
    private static final String CMD_OUTPUT_Q = "OUTPut?";
    private static final String CMD_VOLT_Q = "VOLT?";
    private static final String CMD_CURR_Q = "CURR?";
    private static final String CMD_VOLT_LIM_Q = "VOLT:LIM?";
    private static final String CMD_CURR_LIM_Q = "CURR:LIM?";

    private static final String CMD_SET_VOLT = "VOLT";
    private static final String CMD_SET_CURR = "CURR";
    private static final String CMD_SET_OUTPUT = "OUTPut";
    private static final String CMD_SET_VOLT_LIM = "VOLT:LIM";
    private static final String CMD_SET_CURR_LIM = "CURR:LIM";

    private static final String[] POLL_CMDS = {CMD_MEAS_VOLT, CMD_MEAS_CURR, CMD_MEAS_POW,
            CMD_OUTPUT_Q, CMD_VOLT_Q, CMD_CURR_Q, CMD_VOLT_LIM_Q, CMD_CURR_LIM_Q};

    /** Таймаут ожидания ответа, после которого очередь запросов сбрасывается. */
    private static final long REPLY_TIMEOUT_MS = 1500;
    /** Минимальная пауза между запросами опроса (чтобы не «дудосить» прибор). */
    private static final long MIN_SEND_INTERVAL_MS = 500;
    /** Сколько после записи уставки игнорировать чтение этого параметра (защита от гонки). */
    private static final long SETPOINT_GRACE_MS = 1500;

    private final OwonSpe3051CommunicationService service = new OwonSpe3051CommunicationService();
    private final OwonSpe3051CommandRegistry registry = new OwonSpe3051CommandRegistry();
    private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    /** Очередь ожидаемых ответов (только на запросы с {@code ?}). */
    private final ConcurrentLinkedQueue<String> pending = new ConcurrentLinkedQueue<>();

    /** Время последней записи уставки по имени write-команды. */
    private final ConcurrentHashMap<String, Long> setpointWriteAt = new ConcurrentHashMap<>();

    // Редактируемые спидометры (стрелки/поле → команда прибору).
    private final DefaultGaugeModel voltageSetModel =
            new DefaultGaugeModel("Настройка напряжения", "В", 0, MAX_VOLTAGE, 0);
    private final DefaultGaugeModel currentSetModel =
            new DefaultGaugeModel("Настройка тока", "А", 0, MAX_CURRENT, 0);
    private final DefaultGaugeModel ovpModel =
            new DefaultGaugeModel("Ограничение напряжения", "В", 0, MAX_VOLTAGE, 0);
    private final DefaultGaugeModel ocpModel =
            new DefaultGaugeModel("Ограничение тока", "А", 0, MAX_CURRENT, 0);

    // Измерительные спидометры (только отображение).
    private final DefaultGaugeModel voltageOutModel =
            new DefaultGaugeModel("Выходное напряжение", "В", 0, MAX_VOLTAGE, 0);
    private final DefaultGaugeModel currentOutModel =
            new DefaultGaugeModel("Выходной ток", "А", 0, MAX_CURRENT, 0);
    private final DefaultGaugeModel powerOutModel =
            new DefaultGaugeModel("Мощность", "Вт", 0, MAX_POWER, 0);

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Отключено");
    private final JTextArea logArea = new JTextArea();

    private final JLabel outputLabel = value("ВЫКЛ");
    private final JLabel measuredVLabel = value("—");
    private final JLabel measuredALabel = value("—");
    private final JLabel powerLabel = value("—");
    private final JLabel setVLabel = value("—");
    private final JLabel setALabel = value("—");
    private final JLabel ovpLabel = value("—");
    private final JLabel ocpLabel = value("—");
    private final JLabel idnLabel = value("—");

    private final SkewButton outputButton = Buttons.skewRight("Выход: ВЫКЛ");
    private final JCheckBox pollCheck = new JCheckBox("Автоопрос", true);

    private volatile boolean outputOn = false;
    private volatile long lastReplyAt = System.currentTimeMillis();
    private volatile long lastSendAt = 0;
    /** true, пока значение в модель пишем мы сами (ответ прибора) — не слать команду. */
    private boolean updatingFromDevice = false;
    private int pollIndex = 0;
    private Timer pollTimer;

    public OwonSpe3051ControlPanel() {
        super(new BorderLayout(8, 8));
        setBackground(Theme.BG);
        setOpaque(true);
        buildUi();
        refreshPorts();
        String lastPort = prefs.get(PREFS_KEY_PORT, "");
        if (!lastPort.isEmpty()) {
            portCombo.setSelectedItem(lastPort);
        }

        attachSetpoint(voltageSetModel, CMD_SET_VOLT, setVLabel, "В");
        attachSetpoint(currentSetModel, CMD_SET_CURR, setALabel, "А");
        attachSetpoint(ovpModel, CMD_SET_VOLT_LIM, ovpLabel, "В");
        attachSetpoint(ocpModel, CMD_SET_CURR_LIM, ocpLabel, "А");

        service.addLogListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));
        service.addStatusListener(line -> SwingUtilities.invokeLater(() -> statusLabel.setText(line)));
        service.addLineListener(this::onLine);
        GuiUtilities.darkenInputs(this);
    }

    // ------------------------------------------------------------------- UI

    private void buildUi() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildGauges(), BorderLayout.CENTER);
        add(buildSide(), BorderLayout.EAST);
        add(buildLog(), BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("OWON SPE3051 — Панель управления");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ORANGE_BRIGHT);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(title);
        header.add(Box.createVerticalStrut(8));

        JPanel portRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        portRow.setOpaque(false);
        portRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel portLabel = new JLabel("COM:");
        portLabel.setForeground(Theme.TEXT_DIM);
        portLabel.setFont(Theme.FONT_BODY);
        portRow.add(portLabel);
        portCombo.setPreferredSize(new Dimension(170, 26));
        portRow.add(portCombo);

        SkewButton refresh = Buttons.plain("Обновить");
        SkewButton open = Buttons.skewLeft("Открыть");
        SkewButton close = Buttons.skewRight("Закрыть");
        portRow.add(refresh);
        portRow.add(open);
        portRow.add(close);
        refresh.addActionListener(e -> refreshPorts());
        open.addActionListener(e -> openPort());
        close.addActionListener(e -> closePort());
        portRow.add(Box.createHorizontalStrut(12));

        statusLabel.setForeground(Theme.TEXT);
        statusLabel.setFont(Theme.FONT_BODY);
        portRow.add(statusLabel);

        header.add(portRow);
        return header;
    }

    private JComponent buildGauges() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        JPanel setpoints = new JPanel(new GridLayout(1, 4, 10, 0));
        setpoints.setOpaque(false);
        setpoints.add(editableGauge(voltageSetModel, "Текущая настройка %.2f %s"));
        setpoints.add(editableGauge(currentSetModel, "Текущая настройка %.2f %s"));
        setpoints.add(editableGauge(ovpModel, "Текущая настройка %.2f %s"));
        setpoints.add(editableGauge(ocpModel, "Текущая настройка %.2f %s"));

        JPanel measures = new JPanel(new GridLayout(1, 2, 10, 0));
        measures.setOpaque(false);
        measures.add(readOnlyGauge(voltageOutModel, "Измерено %.2f %s"));
        measures.add(readOnlyGauge(currentOutModel, "Измерено %.2f %s"));

        column.add(setpoints);
        column.add(Box.createVerticalStrut(6));
        column.add(measures);
        return column;
    }

    private RoundGauge editableGauge(DefaultGaugeModel model, String captionFormat) {
        RoundGauge gauge = Gauges.round(model);
        gauge.setCaptionFormat(captionFormat);
        return gauge;
    }

    private RoundGauge readOnlyGauge(DefaultGaugeModel model, String captionFormat) {
        RoundGauge gauge = Gauges.round(model);
        gauge.setCaptionFormat(captionFormat);
        gauge.setValueEditable(false);
        return gauge;
    }

    private JComponent buildSide() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(300, 0));

        side.add(section("Уставки"));
        side.add(stepRow("Напряжение, В",
                () -> bump(voltageSetModel, -VOLTAGE_STEP), () -> bump(voltageSetModel, VOLTAGE_STEP)));
        side.add(stepRow("Ток, А",
                () -> bump(currentSetModel, -CURRENT_STEP), () -> bump(currentSetModel, CURRENT_STEP)));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Защита (OVP / OCP)"));
        side.add(stepRow("OVP, В",
                () -> bump(ovpModel, -VOLTAGE_STEP), () -> bump(ovpModel, VOLTAGE_STEP)));
        side.add(stepRow("OCP, А",
                () -> bump(ocpModel, -CURRENT_STEP), () -> bump(ocpModel, CURRENT_STEP)));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Выход"));
        outputButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        outputButton.addActionListener(e -> toggleOutput());
        side.add(outputButton);
        side.add(infoRow("Состояние", outputLabel));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Измерения"));
        side.add(infoRow("Выход, В", measuredVLabel));
        side.add(infoRow("Выход, А", measuredALabel));
        side.add(infoRow("Мощность, Вт", powerLabel));
        side.add(infoRow("Уставка V", setVLabel));
        side.add(infoRow("Уставка A", setALabel));
        side.add(infoRow("OVP", ovpLabel));
        side.add(infoRow("OCP", ocpLabel));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Прибор"));
        side.add(infoRow("IDN", idnLabel));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Опрос"));
        pollCheck.setOpaque(false);
        pollCheck.setForeground(Theme.TEXT_DIM);
        pollCheck.setFont(Theme.FONT_BODY);
        pollCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
        pollCheck.addActionListener(e -> {
            if (pollCheck.isSelected()) {
                startPolling();
            } else {
                stopPolling();
            }
        });
        side.add(pollCheck);
        SkewButton query = Buttons.plain("Опросить сейчас");
        query.setAlignmentX(Component.LEFT_ALIGNMENT);
        query.addActionListener(e -> requestAll());
        side.add(query);

        return side;
    }

    private JComponent buildLog() {
        logArea.setEditable(false);
        logArea.setFont(Theme.FONT_MONO);
        logArea.setBackground(Theme.PANEL_BG);
        logArea.setForeground(Theme.TEXT_DIM);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(0, 130));
        return scroll;
    }

    private JComponent section(String title) {
        JLabel label = new JLabel(title);
        label.setFont(Theme.FONT_SUBTITLE);
        label.setForeground(Theme.ORANGE);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        wrap.setOpaque(false);
        wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(label);
        return wrap;
    }

    private JComponent stepRow(String title, Runnable minus, Runnable plus) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(title);
        label.setForeground(Theme.TEXT_DIM);
        label.setFont(Theme.FONT_BODY);
        label.setPreferredSize(new Dimension(120, 18));
        row.add(label);

        SkewButton dec = Buttons.skewLeft("-");
        SkewButton inc = Buttons.skewRight("+");
        dec.addActionListener(e -> minus.run());
        inc.addActionListener(e -> plus.run());
        row.add(dec);
        row.add(inc);
        return row;
    }

    private JComponent infoRow(String title, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel label = new JLabel(title);
        label.setForeground(Theme.TEXT_DIM);
        label.setFont(Theme.FONT_SMALL);
        row.add(label, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private static JLabel value(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Theme.TEXT);
        label.setFont(Theme.FONT_BODY);
        return label;
    }

    // ---------------------------------------------------------- setpoints

    /**
     * Подписывает модель на изменение пользователем (стрелки спидометра или поле ввода):
     * обновляет подпись и шлёт команду прибору. Программные обновления (ответы прибора)
     * не шлют команду — за это отвечает флаг {@link #updatingFromDevice}.
     */
    private void attachSetpoint(DefaultGaugeModel model, String command, JLabel label, String unit) {
        model.addChangeListener(m -> {
            double v = round2(m.getValue());
            if (label != null) {
                label.setText(String.format(Locale.US, "%.2f %s", v, unit));
            }
            if (!updatingFromDevice) {
                writeCommand(command, v);
            }
        });
    }

    /** Изменение уставки кнопками +/- панели (пойдёт через слушатель модели). */
    private void bump(DefaultGaugeModel model, double delta) {
        model.setValue(round2(model.getValue() + delta));
    }

    /** Программно выставить значение модели, не отправляя команду прибору. */
    private void setGaugeSilently(DefaultGaugeModel model, double value) {
        if (Double.isNaN(value)) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            updatingFromDevice = true;
            try {
                model.setValue(value);
            } finally {
                updatingFromDevice = false;
            }
        });
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ------------------------------------------------------------ transport

    private void refreshPorts() {
        String prev = (String) portCombo.getSelectedItem();
        portCombo.removeAllItems();
        com.fazecast.jSerialComm.SerialPort[] ports =
                com.fazecast.jSerialComm.SerialPort.getCommPorts();
        for (com.fazecast.jSerialComm.SerialPort port : ports) {
            portCombo.addItem(port.getSystemPortName());
        }
        if (portCombo.getItemCount() == 0) {
            portCombo.addItem("— нет COM-портов —");
        } else if (prev != null) {
            portCombo.setSelectedItem(prev);
        }
    }

    private void openPort() {
        String portName = (String) portCombo.getSelectedItem();
        if (portName == null || portName.startsWith("—")) {
            return;
        }
        if (!service.open(portName)) {
            appendLog("Не удалось открыть " + portName);
            return;
        }
        prefs.put(PREFS_KEY_PORT, portName);
        pending.clear();
        lastReplyAt = System.currentTimeMillis();
        lastSendAt = System.currentTimeMillis();
        service.send(readCommand(CMD_IDN));
        if (pollCheck.isSelected()) {
            startPolling();
        }
    }

    private void closePort() {
        stopPolling();
        service.close();
    }

    private void startPolling() {
        stopPolling();
        pollTimer = new Timer(100, e -> pollTick());
        pollTimer.start();
        pollTick();
    }

    private void stopPolling() {
        if (pollTimer != null) {
            pollTimer.stop();
            pollTimer = null;
        }
    }

    private void pollTick() {
        if (!service.isConnected()) {
            return;
        }
        if (!pending.isEmpty()) {
            if (System.currentTimeMillis() - lastReplyAt > REPLY_TIMEOUT_MS) {
                pending.clear();
                lastReplyAt = System.currentTimeMillis();
            } else {
                return;
            }
        }
        if (System.currentTimeMillis() - lastSendAt < MIN_SEND_INTERVAL_MS) {
            return;
        }
        String cmd = POLL_CMDS[pollIndex % POLL_CMDS.length];
        pollIndex++;
        lastSendAt = System.currentTimeMillis();
        service.send(readCommand(cmd));
    }

    private void requestAll() {
        if (!service.isConnected()) {
            return;
        }
        pending.clear();
        pollIndex = 0;
        lastSendAt = 0;
        appendLog("Перезапуск опроса");
        if (!pollCheck.isSelected()) {
            pollCheck.setSelected(true);
        }
        startPolling();
    }

    // --------------------------------------------------------------- обмен

    /** Формирует команду-запрос (с {@code ?}) и ставит её в очередь ожидания ответа. */
    private byte[] readCommand(String name) {
        SingleCommand command = registry.getCommandList().getCommand(name);
        byte[] body = command != null
                ? command.getBaseBody()
                : name.getBytes(StandardCharsets.US_ASCII);
        pending.add(name);
        return concat(body, CR_LF);
    }

    /** Отправляет команду-установку (без {@code ?}); ответ не ожидается. */
    private void writeCommand(String name, Object value) {
        SingleCommand command = registry.getCommandList().getCommand(name);
        if (command == null) {
            return;
        }
        byte[] body;
        try {
            body = command.build(Collections.singletonMap("value", value));
        } catch (RuntimeException e) {
            appendLog(name + " = " + value + " отклонено: " + e.getMessage());
            return;
        }
        setpointWriteAt.put(name, System.currentTimeMillis());
        lastSendAt = System.currentTimeMillis();
        appendLog(name + " = " + value);
        service.send(concat(body, CR_LF));
    }

    /** Имя write-команды, соответствующей прочитанному параметру (для защиты от гонки). */
    private String writeNameForRead(String readName) {
        switch (readName) {
            case CMD_VOLT_Q:
                return CMD_SET_VOLT;
            case CMD_CURR_Q:
                return CMD_SET_CURR;
            case CMD_VOLT_LIM_Q:
                return CMD_SET_VOLT_LIM;
            case CMD_CURR_LIM_Q:
                return CMD_SET_CURR_LIM;
            case CMD_OUTPUT_Q:
                return CMD_SET_OUTPUT;
            default:
                return null;
        }
    }

    private boolean recentlyWritten(String writeName) {
        Long at = setpointWriteAt.get(writeName);
        return at != null && System.currentTimeMillis() - at < SETPOINT_GRACE_MS;
    }

    private void onLine(String line) {
        lastReplyAt = System.currentTimeMillis();
        String name = pending.poll();
        if (name == null) {
            return;
        }
        String writeName = writeNameForRead(name);
        if (writeName != null && recentlyWritten(writeName)) {
            return;
        }
        switch (name) {
            case CMD_IDN:
                updateIdentity(parse(name, line));
                break;
            case CMD_MEAS_VOLT:
                updateGauge(parse(name, line), measuredVLabel, "%.2f В", voltageOutModel);
                break;
            case CMD_MEAS_CURR:
                updateGauge(parse(name, line), measuredALabel, "%.2f А", currentOutModel);
                break;
            case CMD_MEAS_POW:
                updateGauge(parse(name, line), powerLabel, "%.2f Вт", powerOutModel);
                break;
            case CMD_OUTPUT_Q:
                updateOutput(parse(name, line));
                break;
            case CMD_VOLT_Q:
                setGaugeSilently(voltageSetModel, valueOf(parse(name, line)));
                break;
            case CMD_CURR_Q:
                setGaugeSilently(currentSetModel, valueOf(parse(name, line)));
                break;
            case CMD_VOLT_LIM_Q:
                setGaugeSilently(ovpModel, valueOf(parse(name, line)));
                break;
            case CMD_CURR_LIM_Q:
                setGaugeSilently(ocpModel, valueOf(parse(name, line)));
                break;
            default:
                break;
        }
    }

    private AnswerValues parse(String commandName, String line) {
        SingleCommand command = registry.getCommandList().getCommand(commandName);
        if (command == null) {
            return null;
        }
        try {
            return command.getResult(line.getBytes(StandardCharsets.US_ASCII));
        } catch (Exception e) {
            return null;
        }
    }

    private static double valueOf(AnswerValues values) {
        if (values == null || values.getValues().length == 0) {
            return Double.NaN;
        }
        return values.getValues()[0];
    }

    private void updateGauge(AnswerValues values, JLabel label, String format, DefaultGaugeModel gauge) {
        if (values == null || values.getValues().length == 0) {
            return;
        }
        final double v = values.getValues()[0];
        final String text = String.format(Locale.US, format, v);
        SwingUtilities.invokeLater(() -> {
            label.setText(text);
            gauge.setValue(v);
        });
    }

    private void updateOutput(AnswerValues values) {
        if (values == null || values.getValues().length == 0) {
            return;
        }
        final boolean on = values.getValues()[0] != 0;
        SwingUtilities.invokeLater(() -> applyOutputState(on));
    }

    private void updateIdentity(AnswerValues values) {
        if (values == null) {
            return;
        }
        String[] units = values.getUnits();
        StringBuilder sb = new StringBuilder();
        for (String unit : units) {
            if (unit == null || unit.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(unit);
        }
        final String text = sb.length() == 0 ? "—" : sb.toString();
        SwingUtilities.invokeLater(() -> idnLabel.setText(text));
    }

    // ------------------------------------------------------------- команды

    private void toggleOutput() {
        boolean on = !outputOn;
        applyOutputState(on);
        writeCommand(CMD_SET_OUTPUT, on ? "ON" : "OFF");
    }

    private void applyOutputState(boolean on) {
        outputOn = on;
        outputLabel.setText(on ? "ВКЛ" : "ВЫКЛ");
        outputLabel.setForeground(on ? Theme.LAMP_ON : Theme.TEXT);
        outputButton.setText(on ? "Выход: ВКЛ" : "Выход: ВЫКЛ");
    }

    private static byte[] concat(byte[] body, byte[] tail) {
        byte[] result = new byte[body.length + tail.length];
        System.arraycopy(body, 0, result, 0, body.length);
        System.arraycopy(tail, 0, result, body.length, tail.length);
        return result;
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void shutdown() {
        stopPolling();
        service.shutdown();
    }
}
