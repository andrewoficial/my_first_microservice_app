package org.example.gui.devices.testa.emulation;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора климатической камеры Testa (UDP-сервер на порту 1300).
 * Модель (температура к уставке) тикает таймером; статусные датаграммы рассылаются сервером.
 */
public class TestaEmulationPanel extends JPanel {

    private static final Color GREEN = new Color(0, 160, 0);
    private static final Color GRAY = Color.DARK_GRAY;

    private final TestaEmulator emulator = new TestaEmulator();
    private final TestaServerService service = new TestaServerService(emulator);

    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(1300, 1, 65535, 1));
    private final JButton startBtn = new JButton("Запустить");
    private final JButton stopBtn = new JButton("Остановить");
    private final JLabel statusLabel = new JLabel("Эмулятор остановлен");

    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
    private final JSpinner rampSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 60.0, 0.5));
    private final JLabel tempScreen = screenLabel();
    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    private final javax.swing.Timer simTimer;
    private long lastNanos = System.nanoTime();

    public TestaEmulationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Эмулятор Testa (UDP :1300, статус → :1200)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(buildLeft(), BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);

        stopBtn.setEnabled(false);
        setpointSpinner.addChangeListener(e -> emulator.setSetpoint(((Number) setpointSpinner.getValue()).doubleValue()));
        rampSpinner.addChangeListener(e -> emulator.setRampRateDegPerMin(((Number) rampSpinner.getValue()).doubleValue()));
        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());

        emulator.setSetpoint(((Number) setpointSpinner.getValue()).doubleValue());
        emulator.setRampRateDegPerMin(((Number) rampSpinner.getValue()).doubleValue());

        service.addLogListener(line -> SwingUtilities.invokeLater(() -> addLog(line)));
        service.addRunningListener(r -> SwingUtilities.invokeLater(() ->
                statusLabel.setText(r ? "Эмулятор работает (UDP :" + portSpinner.getValue() + ")" : "Эмулятор остановлен")));

        simTimer = new javax.swing.Timer(100, e -> advanceSim());
        simTimer.start();
    }

    private void start() {
        int port = ((Number) portSpinner.getValue()).intValue();
        startBtn.setEnabled(false);
        if (service.start(port)) {
            stopBtn.setEnabled(true);
        } else {
            startBtn.setEnabled(true);
        }
    }

    private void stop() {
        service.stop();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private void advanceSim() {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        emulator.advance(Math.min(dt, 0.5));
        tempScreen.setText(String.format(Locale.US, "%.2f °C", emulator.getActual()));
    }

    private JPanel buildLeft() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(260, 0));

        p.add(label("Порт приёма SetT"));
        p.add(fullWidth(portSpinner));
        p.add(Box.createVerticalStrut(4));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(startBtn);
        btns.add(stopBtn);
        p.add(btns);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(12));
        p.add(label("Уставка, °C"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("Скорость выхода, °C/мин"));
        p.add(fullWidth(rampSpinner));

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel screens = new JPanel(new GridLayout(1, 1, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "ТЕКУЩАЯ ТЕМПЕРАТУРА"),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(tempScreen, BorderLayout.CENTER);
        screens.add(box);
        center.add(screens, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог UDP (TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 180));
        center.add(logScroll, BorderLayout.CENTER);
        return center;
    }

    private void addLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) {
            logLines.remove(0);
        }
        logArea.setText(String.join("\n", logLines));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    public void shutdown() {
        simTimer.stop();
        service.stop();
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("-- °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(GREEN);
        return l;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }
}
