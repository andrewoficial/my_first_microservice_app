package org.example.gui.devices.bkm4.control;

import com.fazecast.jSerialComm.SerialPort;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель управления блоком коммутации БКМ-4 (RS-232C, 9600 8N1, ASCII/CR).
 * Протокол: {@code bkm4.md}.
 */
public class Bkm4Main {

    private final Bkm4CommunicationService service = new Bkm4CommunicationService();
    private final JPanel mainPanel = new JPanel(new BorderLayout(8, 8));

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JButton refreshBtn = new JButton("Обновить");
    private final JButton connectBtn = new JButton("Подключить");
    private final JButton disconnectBtn = new JButton("Отключить");
    private final JLabel statusLabel = new JLabel("Отключено");

    private final JComboBox<String> modeCombo = new JComboBox<>(new String[]{"0 — ручное", "1 — внешнее"});
    private final JComboBox<String> valveCombo = new JComboBox<>(new String[]{"0 — все выкл", "1 — клапан 1", "2 — клапан 2", "3 — клапан 3", "4 — клапан 4"});
    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(500, 0, 3000, 50));
    private final JComboBox<String> generationCombo = new JComboBox<>(new String[]{"0 — выкл", "1 — вкл"});

    private final JLabel flowScreen = screenLabel();
    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 400;

    public Bkm4Main() {
        mainPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "БКМ-4 — блок коммутации (RS-232C)",
                TitledBorder.LEFT, TitledBorder.TOP));
        mainPanel.add(createLeftPanel(), BorderLayout.WEST);
        mainPanel.add(createRightPanel(), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> refreshPorts());
        connectBtn.addActionListener(e -> connect());
        disconnectBtn.addActionListener(e -> disconnect());
        disconnectBtn.setEnabled(false);

        service.addStatusListener(s -> SwingUtilities.invokeLater(() -> statusLabel.setText(s)));
        service.addLogListener(l -> SwingUtilities.invokeLater(() -> appendLog(l)));
        service.addFlowListener(v -> SwingUtilities.invokeLater(() ->
                flowScreen.setText(String.format(Locale.US, "%.1f мл/мин", v))));

        refreshPorts();

        GuiUtilities.darkenInputs(mainPanel);
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void connect() {
        String selected = (String) portCombo.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(mainPanel, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.open(portName, Bkm4CommunicationService.DEFAULT_BAUD);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        connectBtn.setEnabled(false);
                        disconnectBtn.setEnabled(true);
                        statusLabel.setText("Подключено " + portName);
                        service.setPollingEnabled(true);
                    }
                } catch (Exception ex) {
                    statusLabel.setText("Ошибка: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void disconnect() {
        service.setPollingEnabled(false);
        service.close();
        connectBtn.setEnabled(true);
        disconnectBtn.setEnabled(false);
        statusLabel.setText("Отключено");
    }

    private boolean connectedGuarded() {
        if (!service.isConnected()) {
            JOptionPane.showMessageDialog(mainPanel, "Сначала подключитесь к COM-порту");
            return false;
        }
        return true;
    }

    private void send(String cmd) {
        if (connectedGuarded()) {
            service.sendCommand(cmd);
        }
    }

    // ─── UI построение ────────────────────────────────────────────────────

    private JPanel createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(320, 0));

        p.add(label("COM-порт (9600 8N1)"));
        portCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        portCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, portCombo.getPreferredSize().height));
        p.add(portCombo);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(refreshBtn);
        btns.add(connectBtn);
        btns.add(disconnectBtn);
        p.add(btns);
        p.add(Box.createVerticalStrut(2));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(14));
        p.add(sectionLabel("Управление и опрос"));

        p.add(label("Режим работы"));
        p.add(commandRow(modeCombo, "Установить", () -> "&A" + modeCombo.getSelectedIndex(),
                "Запросить", "&A?"));
        p.add(Box.createVerticalStrut(6));

        p.add(label("Газовый клапан"));
        p.add(commandRow(valveCombo, "Установить", () -> "&V" + valveCombo.getSelectedIndex(),
                "Запросить", "&V?"));
        p.add(Box.createVerticalStrut(6));

        p.add(label("Уставка расхода, мл/мин (0–3000)"));
        p.add(commandRow(setpointSpinner, "Установить", () ->
                        "&S" + ((Number) setpointSpinner.getValue()).intValue(),
                "Запросить", "&S?"));
        p.add(Box.createVerticalStrut(6));

        p.add(label("Генерация потока"));
        p.add(commandRow(generationCombo, "Установить", () ->
                        "&G" + generationCombo.getSelectedIndex(),
                "Запросить", "&G?"));
        p.add(Box.createVerticalStrut(6));

        p.add(fullWidth(makeQueryFlowBtn()));
        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel commandRow(JComboBox<String> combo, String applyTitle, java.util.function.Supplier<String> setCmd,
                              String queryTitle, String queryCmd) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(combo);
        JButton setBtn = new JButton(applyTitle);
        setBtn.addActionListener(e -> send(setCmd.get()));
        row.add(setBtn);
        JButton qBtn = new JButton(queryTitle);
        qBtn.addActionListener(e -> send(queryCmd));
        row.add(qBtn);
        return row;
    }

    private JPanel commandRow(JSpinner spinner, String applyTitle, java.util.function.Supplier<String> setCmd,
                              String queryTitle, String queryCmd) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(spinner);
        JButton setBtn = new JButton(applyTitle);
        setBtn.addActionListener(e -> send(setCmd.get()));
        row.add(setBtn);
        JButton qBtn = new JButton(queryTitle);
        qBtn.addActionListener(e -> send(queryCmd));
        row.add(qBtn);
        return row;
    }

    private JButton makeQueryFlowBtn() {
        JButton btn = new JButton("Опрос фактического расхода (&F?)");
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, btn.getPreferredSize().height));
        btn.addActionListener(e -> send("&F?"));
        return btn;
    }

    private JPanel createRightPanel() {
        JPanel right = new JPanel(new BorderLayout(8, 8));

        JPanel u = new JPanel(new GridLayout(1, 1, 12, 0));
        u.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        u.add(screenBox("ЭКРАН ФАКТИЧЕСКИЙ РАСХОД", flowScreen));
        right.add(u, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог (ASCII RX/TX)"));
        right.add(logScroll, BorderLayout.CENTER);
        return right;
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
        JLabel l = new JLabel("-- мл/мин", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(0, 140, 0));
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
        service.shutdown();
    }
}