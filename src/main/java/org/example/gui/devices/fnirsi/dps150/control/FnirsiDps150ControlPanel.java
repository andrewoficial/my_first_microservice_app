package org.example.gui.devices.fnirsi.dps150.control;

import org.example.device.command.SingleCommand;
import org.example.device.protFnirsiDps150.FnirsiDps150CommandRegistry;
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.prefs.Preferences;

/**
 * Панель управления реальным прибором <b>FNIRSI DPS150</b>.
 *
 * <p>Собрана из виджетов библиотеки {@code ru.kantser.gui}. Обмен идёт
 * бинарным протоколом {@code org.example.device.protFnirsiDps150}:
 * чтение — {@code getVin / getPower / getTemp / getLimitVout / getLimitCurrent /
 * getOutput / getBrightness}, запись — {@code setVout / setIout / setOutput /
 * setBrightness}. Диапазоны: напряжение {@code 0..30.0 В}, ток {@code 0..5.0 А},
 * яркость {@code 0..14}.
 */
public class FnirsiDps150ControlPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/fnirsi/dps150/control";
    private static final String PREFS_KEY_PORT = "lastPort";

    private static final double MAX_VOLTAGE = 30.0;
    private static final double MAX_CURRENT = 5.0;
    private static final double VOLTAGE_STEP = 0.5;
    private static final double CURRENT_STEP = 0.1;

    private static final byte TYPE_POWER = FnirsiDps150CommandRegistry.TYPE_POWER;
    private static final byte TYPE_VIN = FnirsiDps150CommandRegistry.TYPE_DEV_VIN;
    private static final byte TYPE_VOUT_LIMIT = FnirsiDps150CommandRegistry.TYPE_VOUT_LIMIT;
    private static final byte TYPE_IOUT_LIMIT = FnirsiDps150CommandRegistry.TYPE_IOUT_LIMIT;
    private static final byte TYPE_TEMP = FnirsiDps150CommandRegistry.TYPE_TEMP;
    private static final byte TYPE_OUTPUT = FnirsiDps150CommandRegistry.TYPE_DEV_OUTPUT;
    private static final byte TYPE_BRIGHTNESS = FnirsiDps150CommandRegistry.TYPE_BRIGHTNESS;

    private static final byte[] POLL_TYPES = {TYPE_POWER, TYPE_VIN, TYPE_VOUT_LIMIT,
            TYPE_IOUT_LIMIT, TYPE_TEMP, TYPE_OUTPUT, TYPE_BRIGHTNESS};

    private final FnirsiDps150CommunicationService service = new FnirsiDps150CommunicationService();
    private final FnirsiDps150CommandRegistry registry = new FnirsiDps150CommandRegistry();
    private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    private final DefaultGaugeModel voltageSetModel =
            new DefaultGaugeModel("Voltage Setting", "V", 0, MAX_VOLTAGE, 0);
    private final DefaultGaugeModel currentSetModel =
            new DefaultGaugeModel("Current Setting", "A", 0, MAX_CURRENT, 0);
    private final DefaultGaugeModel voltageOutModel =
            new DefaultGaugeModel("Output Voltage", "V", 0, MAX_VOLTAGE, 0);
    private final DefaultGaugeModel currentOutModel =
            new DefaultGaugeModel("Output Current", "A", 0, MAX_CURRENT, 0);

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Отключено");
    private final JTextArea logArea = new JTextArea();

    private final JLabel vinLabel = value("—");
    private final JLabel powerLabel = value("—");
    private final JLabel tempLabel = value("—");
    private final JLabel outputLabel = value("ВЫКЛ");
    private final JLabel brightnessLabel = value("—");
    private final JLabel limitVLabel = value("—");
    private final JLabel limitALabel = value("—");
    private final JLabel modelLabel = value("—");
    private final JLabel swLabel = value("—");
    private final JLabel hwLabel = value("—");

    private final SkewButton outputButton = Buttons.skewRight("Выход: ВЫКЛ");

    private volatile boolean outputOn = false;
    private int brightness = 10;
    private int pollIndex = 0;
    private boolean identityRequested = false;
    private Timer pollTimer;

    public FnirsiDps150ControlPanel() {
        super(new BorderLayout(8, 8));
        setBackground(Theme.BG);
        setOpaque(true);
        buildUi();
        refreshPorts();
        String lastPort = prefs.get(PREFS_KEY_PORT, "");
        if (!lastPort.isEmpty()) {
            portCombo.setSelectedItem(lastPort);
        }

        service.addLogListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));
        service.addStatusListener(line -> SwingUtilities.invokeLater(() -> statusLabel.setText(line)));
        service.addResponseListener(this::onResponse);
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

        JLabel title = new JLabel("FNIRSI DPS150 — Панель управления");
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

        portRow.add(Buttons.plain("Обновить"));
        SkewButton open = Buttons.skewLeft("Открыть");
        SkewButton close = Buttons.skewRight("Закрыть");
        portRow.add(open);
        portRow.add(close);
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
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        RoundGauge voltageSet = Gauges.round(voltageSetModel);
        RoundGauge currentSet = Gauges.round(currentSetModel);
        RoundGauge voltageOut = Gauges.round(voltageOutModel);
        RoundGauge currentOut = Gauges.round(currentOutModel);
        row.add(voltageSet);
        row.add(currentSet);
        row.add(voltageOut);
        row.add(currentOut);
        return row;
    }

    private JComponent buildSide() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(300, 0));

        side.add(section("Уставки"));
        side.add(stepRow("Напряжение, В",
                () -> adjustVoltage(-VOLTAGE_STEP), () -> adjustVoltage(VOLTAGE_STEP)));
        side.add(stepRow("Ток, А",
                () -> adjustCurrent(-CURRENT_STEP), () -> adjustCurrent(CURRENT_STEP)));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Выход"));
        outputButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        outputButton.addActionListener(e -> toggleOutput());
        side.add(outputButton);
        side.add(Box.createVerticalStrut(10));

        side.add(section("Яркость (0..14)"));
        side.add(stepRow("Яркость",
                () -> adjustBrightness(-1), () -> adjustBrightness(1)));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Измерения"));
        side.add(infoRow("Вход, В", vinLabel));
        side.add(infoRow("Мощность, Вт", powerLabel));
        side.add(infoRow("Температура, °C", tempLabel));
        side.add(infoRow("Состояние", outputLabel));
        side.add(infoRow("Яркость", brightnessLabel));
        side.add(infoRow("Лимит V", limitVLabel));
        side.add(infoRow("Лимит A", limitALabel));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Прибор"));
        side.add(infoRow("Модель", modelLabel));
        side.add(infoRow("ПО", swLabel));
        side.add(infoRow("Аппаратная", hwLabel));

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
        identityRequested = false;
        startPolling();
    }

    private void closePort() {
        stopPolling();
        service.close();
    }

    private void startPolling() {
        stopPolling();
        pollTimer = new Timer(250, e -> pollTick());
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
        if (!identityRequested) {
            identityRequested = true;
            service.send(registry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_DEV_MODEL));
            service.send(registry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_SW_VERSION));
            service.send(registry.buildReadCommand(FnirsiDps150CommandRegistry.TYPE_HW_VERSION));
            return;
        }
        byte type = POLL_TYPES[pollIndex % POLL_TYPES.length];
        pollIndex++;
        service.send(registry.buildReadCommand(type));
    }

    // --------------------------------------------------------------- обмен

    private void onResponse(byte[] frame) {
        if (frame.length < 5 || frame[1] != (byte) 0xA1) {
            return;
        }
        byte type = frame[2];
        switch (type) {
            case TYPE_POWER:
                updatePower(parse("getPower", frame));
                break;
            case TYPE_VIN:
                updateSingle(parse("getVin", frame), vinLabel, "%.2f В");
                break;
            case TYPE_VOUT_LIMIT:
                updateSingle(parse("getLimitVout", frame), limitVLabel, "%.2f В");
                break;
            case TYPE_IOUT_LIMIT:
                updateSingle(parse("getLimitCurrent", frame), limitALabel, "%.2f А");
                break;
            case TYPE_TEMP:
                updateSingle(parse("getTemp", frame), tempLabel, "%.1f °C");
                break;
            case TYPE_OUTPUT:
                updateOutput(parse("getOutput", frame));
                break;
            case TYPE_BRIGHTNESS:
                updateBrightness(parse("getBrightness", frame));
                break;
            case FnirsiDps150CommandRegistry.TYPE_DEV_MODEL:
                updateText(parse("getModel", frame), modelLabel);
                break;
            case FnirsiDps150CommandRegistry.TYPE_SW_VERSION:
                updateText(parse("getSwVersion", frame), swLabel);
                break;
            case FnirsiDps150CommandRegistry.TYPE_HW_VERSION:
                updateText(parse("getHwVersion", frame), hwLabel);
                break;
            default:
                break;
        }
    }

    private AnswerValues parse(String commandName, byte[] frame) {
        SingleCommand command = registry.getCommandList().getCommand(commandName);
        if (command == null) {
            return null;
        }
        try {
            return command.getResult(frame);
        } catch (Exception e) {
            return null;
        }
    }

    private void updatePower(AnswerValues values) {
        if (values == null) {
            return;
        }
        final double volts = values.getValues()[0];
        final double amps = values.getValues()[1];
        final double watts = values.getValues()[2];
        SwingUtilities.invokeLater(() -> {
            voltageOutModel.setValue(volts);
            currentOutModel.setValue(amps);
            powerLabel.setText(String.format("%.2f Вт", watts));
        });
    }

    private void updateSingle(AnswerValues values, JLabel label, String format) {
        if (values == null) {
            return;
        }
        final String text = String.format(format, values.getValues()[0]);
        SwingUtilities.invokeLater(() -> label.setText(text));
    }

    private void updateOutput(AnswerValues values) {
        if (values == null) {
            return;
        }
        final boolean on = values.getValues()[0] != 0;
        SwingUtilities.invokeLater(() -> applyOutputState(on));
    }

    private void updateBrightness(AnswerValues values) {
        if (values == null) {
            return;
        }
        final int level = (int) Math.round(values.getValues()[0]);
        SwingUtilities.invokeLater(() -> {
            brightness = level;
            brightnessLabel.setText(String.valueOf(level));
        });
    }

    private void updateText(AnswerValues values, JLabel label) {
        if (values == null || values.getUnits().length == 0) {
            return;
        }
        final String text = values.getUnits()[0];
        SwingUtilities.invokeLater(() -> label.setText(text));
    }

    // ------------------------------------------------------------- команды

    private void adjustVoltage(double delta) {
        double next = voltageSetModel.getValue() + delta;
        next = Math.max(0, Math.min(MAX_VOLTAGE, next));
        voltageSetModel.setValue(next);
        sendFloat(FnirsiDps150CommandRegistry.TYPE_DEV_VOUT, (float) next, "setVout");
    }

    private void adjustCurrent(double delta) {
        double next = currentSetModel.getValue() + delta;
        next = Math.max(0, Math.min(MAX_CURRENT, next));
        currentSetModel.setValue(next);
        sendFloat(FnirsiDps150CommandRegistry.TYPE_DEV_IOUT, (float) next, "setIout");
    }

    private void toggleOutput() {
        applyOutputState(!outputOn);
        sendByte(FnirsiDps150CommandRegistry.TYPE_DEV_OUTPUT, (byte) (outputOn ? 1 : 0), "setOutput");
    }

    private void adjustBrightness(int delta) {
        brightness = Math.max(0, Math.min(14, brightness + delta));
        brightnessLabel.setText(String.valueOf(brightness));
        sendByte(FnirsiDps150CommandRegistry.TYPE_BRIGHTNESS, (byte) brightness, "setBrightness");
    }

    private void applyOutputState(boolean on) {
        outputOn = on;
        outputLabel.setText(on ? "ВКЛ" : "ВЫКЛ");
        outputLabel.setForeground(on ? Theme.LAMP_ON : Theme.TEXT);
        outputButton.setText(on ? "Выход: ВКЛ" : "Выход: ВЫКЛ");
    }

    private void sendFloat(byte type, float value, String commandName) {
        byte[] data = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
        appendLog(String.format("%s = %.2f", commandName, value));
        service.send(registry.buildWriteCommand(type, data));
    }

    private void sendByte(byte type, byte value, String commandName) {
        appendLog(String.format("%s = %d", commandName, value & 0xFF));
        service.send(registry.buildWriteCommand(type, new byte[]{value}));
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
