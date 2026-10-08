package org.example.gui.devices.owon.spe3051.debug;

import org.example.gui.devices.owon.spe3051.control.OwonSpe3051CommunicationService;
import org.example.gui.utilites.GuiUtilities;
import ru.kantser.gui.buttons.Buttons;
import ru.kantser.gui.buttons.SkewButton;
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
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.nio.charset.StandardCharsets;
import java.util.prefs.Preferences;

/**
 * Панель отладки <b>OWON SPE3051</b> — «сырой» ASCII-терминал.
 *
 * <p>Позволяет вручную отправлять любые команды (SCPI) и видеть ответные строки
 * прибора в ASCII и hex. Использует тот же транспорт
 * {@link OwonSpe3051CommunicationService} (115200 8N1, CR+LF), что и панель управления.
 */
public class OwonSpe3051DebugPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/owon/spe3051/debug";
    private static final String PREFS_KEY_PORT = "lastDebugPort";

    private static final byte[] CR = {13, 10};

    private static final String[] QUICK_COMMANDS = {
            "*IDN?",
            "MEAS:VOLT?",
            "MEAS:CURR?",
            "MEAS:POW?",
            "OUTPut?",
            "OUTPut ON",
            "OUTPut OFF",
            "VOLT?",
            "CURR?",
            "VOLT:LIM?",
            "CURR:LIM?",
    };

    private final OwonSpe3051CommunicationService service = new OwonSpe3051CommunicationService();
    private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Отключено");
    private final JTextArea logArea = new JTextArea();
    private final JTextField commandField = new JTextField();
    private final JCheckBox appendCrCheck = new JCheckBox("Добавлять CR", true);

    public OwonSpe3051DebugPanel() {
        super(new BorderLayout(8, 8));
        setBackground(Theme.BG);
        setOpaque(true);
        buildUi();
        refreshPorts();
        String lastPort = prefs.get(PREFS_KEY_PORT, "");
        if (!lastPort.isEmpty()) {
            portCombo.setSelectedItem(lastPort);
        }

        service.addStatusListener(line -> SwingUtilities.invokeLater(() -> statusLabel.setText(line)));
        service.addLineListener(this::onLine);
        GuiUtilities.darkenInputs(this);
    }

    // ------------------------------------------------------------------- UI

    private void buildUi() {
        add(buildHeader(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildLog(), BorderLayout.SOUTH);
    }

    private JComponent buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("OWON SPE3051 — Панель отладки");
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

    private JComponent buildCenter() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        center.add(section("Быстрые команды"));
        JPanel quickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        quickRow.setOpaque(false);
        quickRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String cmd : QUICK_COMMANDS) {
            SkewButton button = Buttons.plain(cmd);
            button.addActionListener(e -> sendCommand(cmd));
            quickRow.add(button);
        }
        center.add(quickRow);

        center.add(Box.createVerticalStrut(10));
        center.add(section("Команда"));

        JPanel inputRow = new JPanel(new BorderLayout(6, 0));
        inputRow.setOpaque(false);
        inputRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        inputRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        commandField.setFont(Theme.FONT_MONO);
        commandField.addActionListener(e -> sendCommand(commandField.getText()));
        inputRow.add(commandField, BorderLayout.CENTER);

        JPanel inputButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        inputButtons.setOpaque(false);
        SkewButton send = Buttons.skewLeft("Отправить");
        send.addActionListener(e -> sendCommand(commandField.getText()));
        inputButtons.add(send);
        inputRow.add(inputButtons, BorderLayout.EAST);

        center.add(inputRow);

        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        optRow.setOpaque(false);
        optRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        appendCrCheck.setOpaque(false);
        appendCrCheck.setForeground(Theme.TEXT_DIM);
        appendCrCheck.setFont(Theme.FONT_BODY);
        optRow.add(appendCrCheck);

        SkewButton clear = Buttons.plain("Очистить лог");
        clear.addActionListener(e -> logArea.setText(""));
        optRow.add(clear);
        center.add(optRow);

        return center;
    }

    private JComponent buildLog() {
        logArea.setEditable(false);
        logArea.setFont(Theme.FONT_MONO);
        logArea.setBackground(Theme.PANEL_BG);
        logArea.setForeground(Theme.TEXT_DIM);
        JScrollPane scroll = new JScrollPane(logArea);
        scroll.setPreferredSize(new Dimension(0, 420));
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
    }

    private void closePort() {
        service.close();
    }

    // --------------------------------------------------------------- обмен

    private void sendCommand(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        byte[] body = text.getBytes(StandardCharsets.US_ASCII);
        byte[] payload = appendCrCheck.isSelected() ? concat(body, CR) : body;
        appendLog("TX >> " + escaped(body) + "   [" + OwonSpe3051CommunicationService.hex(payload) + "]");
        service.send(payload);
        commandField.setText("");
    }

    private void onLine(String line) {
        byte[] bytes = line.getBytes(StandardCharsets.US_ASCII);
        appendLog("RX << " + escaped(bytes) + "   [" + OwonSpe3051CommunicationService.hex(bytes) + "]");
    }

    private static String escaped(byte[] bytes) {
        return new String(bytes, StandardCharsets.US_ASCII);
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
        service.shutdown();
    }
}
