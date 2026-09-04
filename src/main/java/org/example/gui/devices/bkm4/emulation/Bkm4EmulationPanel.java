package org.example.gui.devices.bkm4.emulation;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора блока коммутации БКМ-4 (RS-232C, 9600 8N1, ASCII/CR).
 * Управление состоянием (режим, клапан, уставка, генерация) и лог ASCII.
 */
public class Bkm4EmulationPanel extends JPanel {

    private final Bkm4Emulator emulator = new Bkm4Emulator();
    private final Bkm4EmulationService service = new Bkm4EmulationService(new Bkm4Responder(emulator));

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton openBtn = new JButton("Открыть");
    private final JButton closeBtn = new JButton("Закрыть");

    private final JComboBox<String> modeCombo = new JComboBox<>(new String[]{"0 — ручное", "1 — внешнее"});
    private final JComboBox<String> valveCombo = new JComboBox<>(new String[]{"0 — все выкл", "1 — клапан 1", "2 — клапан 2", "3 — клапан 3", "4 — клапан 4"});
    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(500, 0, 3000, 50));
    private final JCheckBox generationCb = new JCheckBox("Генерация потока (ВКЛ)", false);

    private final JLabel currentFlowScreen = screenLabel();
    private final JLabel infoLabel = new JLabel();
    private final JLabel statusLabel = new JLabel("Эмулятор отключён");
    private final JTextArea logArea = new JTextArea();

    private final javax.swing.Timer simTimer;
    private long lastTickNanos = System.nanoTime();

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 400;

    public Bkm4EmulationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "Эмулятор БКМ-4 (RS-232C 9600 8N1)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshPorts());
        openBtn.addActionListener(e -> openPort());
        closeBtn.addActionListener(e -> closePort());
        closeBtn.setEnabled(false);

        modeCombo.addActionListener(e -> emulator.setMode(modeCombo.getSelectedIndex()));
        valveCombo.addActionListener(e -> emulator.setValve(valveCombo.getSelectedIndex()));
        setpointSpinner.addChangeListener(e -> emulator.setSetpointMlMin(num(setpointSpinner)));
        generationCb.addActionListener(e -> emulator.setGeneration(generationCb.isSelected() ? 1 : 0));

        service.addResponseListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));

        simTimer = new javax.swing.Timer(100, e -> advanceSim());
        simTimer.start();
        refreshPorts();
    }

    private void advanceSim() {
        long now = System.nanoTime();
        double dt = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        emulator.advance(Math.min(dt, 0.5));
        double flow = emulator.getCurrentFlowMlMin();
        currentFlowScreen.setText(String.format(Locale.US, "%.1f мл/мин", flow));
        currentFlowScreen.setForeground(emulator.getGeneration() == 1
                ? new Color(0, 140, 0) : new Color(160, 160, 160));
        infoLabel.setText("Режим: " + emulator.getMode()
                + "   ·   Клапан: " + emulator.getValve()
                + "   ·   Уставка: " + String.format(Locale.US, "%.0f", emulator.getSetpointMlMin()) + " мл/мин");
    }

    private void openPort() {
        String selected = (String) portCombo.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(this, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.openPort(portName, Bkm4EmulationService.DEFAULT_BAUD);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        openBtn.setEnabled(false);
                        closeBtn.setEnabled(true);
                        statusLabel.setText("Открыт " + portName + " @ 9600 8N1");
                        appendLog("Порт открыт: " + portName + " @ 9600 8N1");
                    } else {
                        statusLabel.setText("Ошибка открытия порта");
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Ошибка: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void closePort() {
        service.closePort();
        openBtn.setEnabled(true);
        closeBtn.setEnabled(false);
        statusLabel.setText("Эмулятор отключён");
        appendLog("Порт закрыт");
    }

    // ─── UI построение ────────────────────────────────────────────────────

    private JPanel createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(300, 0));

        p.add(label("COM-порт"));
        portCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        portCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, portCombo.getPreferredSize().height));
        p.add(portCombo);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(refreshBtn);
        btns.add(openBtn);
        btns.add(closeBtn);
        p.add(btns);
        p.add(Box.createVerticalStrut(4));

        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(12));
        p.add(sectionLabel("Состояние эмулятора"));
        p.add(label("Режим работы"));
        p.add(fullWidth(modeCombo));
        p.add(Box.createVerticalStrut(6));
        p.add(label("Газовый клапан"));
        p.add(fullWidth(valveCombo));
        p.add(Box.createVerticalStrut(6));
        p.add(label("Уставка расхода, мл/мин (0–3000)"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(6));
        generationCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(generationCb);

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        screens.add(screenBox("ЭКРАН ТЕКУЩИЙ РАСХОД", currentFlowScreen));

        center.add(screens, BorderLayout.NORTH);

        JLabel info = infoLabel;
        info.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        info.setText("Режим: " + emulator.getMode() + "   ·   Клапан: " + emulator.getValve()
                + "   ·   Уставка: " + String.format(Locale.US, "%.0f", emulator.getSetpointMlMin()) + " мл/мин");
        center.add(info, BorderLayout.CENTER);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог (ASCII RX/TX)"));
        logScroll.setPreferredSize(new Dimension(400, 170));
        center.add(logScroll, BorderLayout.SOUTH);
        return center;
    }

    private static JPanel screenBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        value.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    private static JLabel screenLabel() {
        JLabel l = new JLabel("0.0 мл/мин", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(160, 160, 160));
        return l;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel sectionLabel(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setFont(l.getFont().deriveFont(Font.BOLD));
        return l;
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }

    private void appendLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) {
            logLines.remove(0);
        }
        logArea.setText(String.join("\n", logLines));
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    private void refreshPorts() {
        portCombo.removeAllItems();
        for (SerialPort p : SerialPort.getCommPorts()) {
            portCombo.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portCombo.getItemCount() == 0) {
            portCombo.addItem("Нет доступных портов");
        }
    }

    public void shutdown() {
        simTimer.stop();
        service.closePort();
    }
}