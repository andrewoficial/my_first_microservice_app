package org.example.gui.devices.fnirsi.dps150.emulation;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.devices.emulation.EmulatorCommandLog;
import org.example.gui.devices.fnirsi.dps150.control.FnirsiDps150CommunicationService;
import org.example.gui.utilites.GuiUtilities;
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
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.prefs.Preferences;

/**
 * Панель-эмулятор прибора <b>FNIRSI DPS150</b>.
 *
 * <p>Без реального прибора: открывает COM-порт и отвечает на бинарные
 * запросы {@code F1 ...} кадрами {@code F0 A1 ...} по тому же протоколу
 * (см. {@link FnirsiDps150Emulator}). Элементы панели позволяют вручную
 * задавать уставки, выход и яркость эмулируемого источника.
 */
@Slf4j
public class FnirsiDps150EmulationPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/fnirsi/dps150/emulation";
    private static final String PREFS_KEY_PORT = "lastEmuPort";

    private static final byte START_SEND = (byte) 0xF1;
    private static final int BAUD = FnirsiDps150CommunicationService.DEFAULT_BAUD;
    private static final int READ_TIMEOUT_MS = 20;
    private static final long FRAME_TIMEOUT_MS = 60;

    private static final double VOLTAGE_STEP = 0.5;
    private static final double CURRENT_STEP = 0.1;

    private final FnirsiDps150Emulator emulator = new FnirsiDps150Emulator();
    private final EmulatorCommandLog commandLog = new EmulatorCommandLog("FNIRSI DPS150");
    private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final DefaultGaugeModel voutSetModel =
            new DefaultGaugeModel("Voltage Setting", "V", 0, 30, 0);
    private final DefaultGaugeModel ioutSetModel =
            new DefaultGaugeModel("Current Setting", "A", 0, 5, 0);
    private final DefaultGaugeModel voutOutModel =
            new DefaultGaugeModel("Output Voltage", "V", 0, 30, 0);
    private final DefaultGaugeModel ioutOutModel =
            new DefaultGaugeModel("Output Current", "A", 0, 5, 0);

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Остановлено");
    private final JTextArea logArea = new JTextArea();
    private final JLabel outputLabel = new JLabel("ВЫКЛ");
    private final JLabel brightnessLabel = new JLabel("10");
    private final SkewButton outputButton = Buttons.skewRight("Выход: ВЫКЛ");

    private final Timer uiTimer;

    public FnirsiDps150EmulationPanel() {
        super(new BorderLayout(8, 8));
        setBackground(Theme.BG);
        setOpaque(true);
        buildUi();
        refreshPorts();
        String last = prefs.get(PREFS_KEY_PORT, "");
        if (!last.isEmpty()) {
            portCombo.setSelectedItem(last);
        }

        uiTimer = new Timer(120, e -> refreshModel());
        uiTimer.start();
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

        JLabel title = new JLabel("FNIRSI DPS150 — Эмулятор прибора");
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

        JPanel cmdRow = EmulatorCommandLog.createControls(commandLog);
        cmdRow.setOpaque(false);
        cmdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(cmdRow);
        return header;
    }

    private JComponent buildGauges() {
        JPanel row = new JPanel(new GridLayout(1, 4, 10, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        RoundGauge voutSet = Gauges.round(voutSetModel);
        RoundGauge ioutSet = Gauges.round(ioutSetModel);
        RoundGauge voutOut = Gauges.round(voutOutModel);
        RoundGauge ioutOut = Gauges.round(ioutOutModel);
        row.add(voutSet);
        row.add(ioutSet);
        row.add(voutOut);
        row.add(ioutOut);
        return row;
    }

    private JComponent buildSide() {
        JPanel side = new JPanel();
        side.setOpaque(false);
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setPreferredSize(new Dimension(260, 0));

        side.add(section("Уставки"));
        side.add(stepRow("Напряжение, В",
                () -> emulator.setVoutSet(emulator.getVoutSet() - (float) VOLTAGE_STEP),
                () -> emulator.setVoutSet(emulator.getVoutSet() + (float) VOLTAGE_STEP)));
        side.add(stepRow("Ток, А",
                () -> emulator.setIoutSet(emulator.getIoutSet() - (float) CURRENT_STEP),
                () -> emulator.setIoutSet(emulator.getIoutSet() + (float) CURRENT_STEP)));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Выход"));
        outputButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        outputButton.addActionListener(e -> {
            emulator.setOutput(emulator.getOutput() == 1 ? 0 : 1);
            commandLog.command(emulator.getOutput() == 1 ? "выход ВКЛ" : "выход ВЫКЛ");
        });
        side.add(outputButton);
        side.add(Box.createVerticalStrut(10));

        side.add(section("Яркость (0..14)"));
        side.add(stepRow("Яркость",
                () -> emulator.setBrightness(emulator.getBrightness() - 1),
                () -> emulator.setBrightness(emulator.getBrightness() + 1)));
        side.add(Box.createVerticalStrut(10));

        side.add(section("Состояние"));
        side.add(infoRow("Выход", outputLabel));
        side.add(infoRow("Яркость", brightnessLabel));

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

    private void refreshModel() {
        voutSetModel.setValue(emulator.getVoutSet());
        ioutSetModel.setValue(emulator.getIoutSet());
        voutOutModel.setValue(emulator.getMeasuredVout());
        ioutOutModel.setValue(emulator.getMeasuredIout());
        outputLabel.setText(emulator.getOutput() == 1 ? "ВКЛ" : "ВЫКЛ");
        outputLabel.setForeground(emulator.getOutput() == 1 ? Theme.LAMP_ON : Theme.TEXT);
        outputButton.setText(emulator.getOutput() == 1 ? "Выход: ВКЛ" : "Выход: ВЫКЛ");
        brightnessLabel.setText(String.valueOf(emulator.getBrightness()));
    }

    // ------------------------------------------------------------ transport

    private void refreshPorts() {
        String prev = (String) portCombo.getSelectedItem();
        portCombo.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort p : ports) {
            portCombo.addItem(p.getSystemPortName());
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
        if (port != null && port.isOpen()) {
            closePort();
        }
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(BAUD);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);
        if (!port.openPort()) {
            appendLog("Не удалось открыть " + portName);
            port = null;
            return;
        }
        prefs.put(PREFS_KEY_PORT, portName);
        running = true;
        startReader();
        statusLabel.setText("Эмуляция: " + portName + " @ " + BAUD + " 8N1");
        appendLog("Порт открыт: " + portName + " @ " + BAUD + " 8N1");
    }

    private void closePort() {
        running = false;
        readerThread = null;
        if (port != null && port.isOpen()) {
            port.closePort();
        }
        port = null;
        synchronized (buffer) {
            buffer.reset();
        }
        statusLabel.setText("Остановлено");
    }

    public void shutdown() {
        closePort();
        uiTimer.stop();
    }

    private void startReader() {
        readerThread = new Thread(() -> {
            byte[] single = new byte[1];
            long lastByteAt = 0;
            boolean collecting = false;
            while (running && port != null && port.isOpen()) {
                try {
                    int r = port.readBytes(single, 1);
                    long now = System.currentTimeMillis();
                    if (r > 0) {
                        if (!collecting) {
                            collecting = true;
                            synchronized (buffer) {
                                buffer.reset();
                            }
                        }
                        synchronized (buffer) {
                            buffer.write(single[0]);
                        }
                        lastByteAt = now;
                    } else if (collecting && (now - lastByteAt > FRAME_TIMEOUT_MS)) {
                        processBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) {
                        log.error("FnirsiDps150 emu read error", e);
                    }
                }
            }
        }, "FnirsiDps150Emu-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
            buffer.reset();
        }
        int off = 0;
        while (off + 4 <= data.length) {
            if (data[off] != START_SEND) {
                off++;
                continue;
            }
            int len = data[off + 3] & 0xFF;
            int frameLen = 4 + len + 1;
            if (off + frameLen > data.length) {
                break;
            }
            byte[] frame = Arrays.copyOfRange(data, off, off + frameLen);
            appendLog("RX << " + FnirsiDps150CommunicationService.hex(frame));
            byte[] response = emulator.handle(frame);
            if (response != null) {
                port.writeBytes(response, response.length);
                appendLog("TX >> " + FnirsiDps150CommunicationService.hex(response));
            } else {
                appendLog("TX >> (нет ответа)");
            }
            off += frameLen;
        }
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}
