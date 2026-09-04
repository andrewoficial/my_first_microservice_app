package org.example.gui.devices.testa.control;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель управления климатической камерой Testa (UDP).
 * Подключение к host:1300 (приём статуса на :1200), задание уставки (SetT),
 * периодическое чтение текущей температуры из статусных датаграмм.
 */
public class TestaControlPanel {

    private static final Color GREEN = new Color(0, 160, 0);
    private static final Color RED = new Color(200, 40, 40);
    private static final Color GRAY = Color.DARK_GRAY;
    private static final Color ORANGE = new Color(200, 120, 0);

    private final TestaCommunicationService service = new TestaCommunicationService();

    private JPanel mainPanel;
    private JTextField hostField;
    private JSpinner portSpinner;
    private JButton connectBtn;
    private JButton disconnectBtn;
    private JLabel comLamp;
    private JLabel statusLabel;

    private JSpinner targetSpinner;
    private JButton setBtn;
    private JLabel cameraSpLabel = new JLabel("--");

    private final JLabel setpointScreen = screenLabel();
    private final JLabel measuredScreen = screenLabel();
    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    public TestaControlPanel() {
        buildUi();
        service.addTemperatureListener(t -> SwingUtilities.invokeLater(() ->
                measuredScreen.setText(String.format(Locale.US, "%.2f °C", t))));
        service.addSetpointListener(sp -> SwingUtilities.invokeLater(() -> {
            cameraSpLabel.setText(String.format(Locale.US, "%.2f °C", sp));
            setpointScreen.setText(String.format(Locale.US, "%.2f °C", sp));
        }));
        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(line -> SwingUtilities.invokeLater(() -> addLog(line)));
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void buildUi() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel north = new JPanel();
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Соединение и уставка",
                TitledBorder.LEFT, TitledBorder.TOP));

        hostField = new JTextField("127.0.0.1", 10);
        portSpinner = new JSpinner(new SpinnerNumberModel(1300, 1, 65535, 1));
        connectBtn = new JButton("Подключиться");
        disconnectBtn = new JButton("Отключиться");
        disconnectBtn.setEnabled(false);
        comLamp = lamp(GRAY);
        statusLabel = new JLabel("Не подключено");

        JPanel connRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        connRow.add(new JLabel("host:"));
        connRow.add(hostField);
        connRow.add(new JLabel("port (SetT):"));
        connRow.add(portSpinner);
        connRow.add(new JLabel("приём :1200"));
        connRow.add(connectBtn);
        connRow.add(disconnectBtn);
        connRow.add(comLamp);
        connRow.add(statusLabel);
        north.add(connRow);

        targetSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
        setBtn = new JButton("Задать уставку (SetT)");
        JPanel setRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        setRow.add(new JLabel("Уставка, °C:"));
        setRow.add(targetSpinner);
        setRow.add(setBtn);
        setRow.add(new JLabel("уставка из камеры:"));
        setRow.add(cameraSpLabel);
        north.add(setRow);

        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        screens.add(screenBox("ЭКРАН ЗАДАНО", setpointScreen));
        screens.add(screenBox("ЭКРАН ТЕКУЩАЯ", measuredScreen));
        center.add(screens, BorderLayout.NORTH);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Дебаг (UDP TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 200));
        center.add(logScroll, BorderLayout.CENTER);

        mainPanel.add(north, BorderLayout.NORTH);
        mainPanel.add(center, BorderLayout.CENTER);

        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        setBtn.addActionListener(e -> applySetpoint());

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
    }

    private void connect() {
        String host = hostField.getText().trim();
        int port = ((Number) portSpinner.getValue()).intValue();
        connectBtn.setEnabled(false);
        statusLabel.setText("Подключение…");
        comLamp.setForeground(ORANGE);
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.connect(host, port);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        connectBtn.setEnabled(false);
                        disconnectBtn.setEnabled(true);
                        comLamp.setForeground(GREEN);
                        service.setPollingEnabled(true);
                        announceToCamera();
                    } else {
                        connectBtn.setEnabled(true);
                        disconnectBtn.setEnabled(false);
                        comLamp.setForeground(RED);
                    }
                } catch (Exception ex) {
                    connectBtn.setEnabled(true);
                    comLamp.setForeground(RED);
                }
            }
        }.execute();
    }

    private void disconnect() {
        service.setPollingEnabled(false);
        service.disconnect();
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        comLamp.setForeground(GRAY);
        statusLabel.setText("Не подключено");
    }

    /** Отправляет одну уставку при подключении, чтобы камера узнала наш адрес и начала слать статус. */
    private void announceToCamera() {
        double target = ((Number) targetSpinner.getValue()).doubleValue();
        service.setTargetTemperature(target);
    }

    private void applySetpoint() {
        if (!service.isConnected()) {
            statusLabel.setText("Нет соединения — подключитесь к эмулятору/камере");
            return;
        }
        double target = ((Number) targetSpinner.getValue()).doubleValue();
        setpointScreen.setText(String.format(Locale.US, "%.2f °C", target));
        service.setTargetTemperature(target);
    }

    private void addLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) {
            logLines.remove(0);
        }
        logArea.setText(String.join("\n", logLines));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static JLabel lamp(Color c) {
        JLabel l = new JLabel("●");
        l.setForeground(c);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("-- °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(GREEN);
        return l;
    }

    private static JPanel screenBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    /** Standalone-запуск панели управления. */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            TestaControlPanel panel = new TestaControlPanel();
            JFrame frame = new JFrame("Testa — Управление камерой (UDP)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(900, 620);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
