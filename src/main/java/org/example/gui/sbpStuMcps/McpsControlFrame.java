package org.example.gui.sbpStuMcps;

import com.fazecast.jSerialComm.SerialPort;
import javax.swing.*;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Автономное окно управления SPB_STU_MCPS (как org.example.gui.curve).
 * 
 * Можно запускать standalone для отладки или встраивать в основное приложение
 * при выборе протокола SPB_STU_MCPS и открытии порта.
 */
public class McpsControlFrame extends JFrame {

    private static final String PREFS_NODE = "org/example/gui/sbpStuMcps";
    private static final String LAST_PORT = "lastPort";

    private final AsyncLogger logger;
    private final McpsCommunicationService service;
    private final McpsChannelsPanel channelsPanel;
    private final JButton openPortBtn = new JButton("Открыть порт");
    private final JButton closePortBtn = new JButton("Закрыть порт");
    private final JButton seqBtn = new JButton("Последовательность импульсов...");
    private final JLabel connectionLabel = new JLabel("Отключено", SwingConstants.CENTER);
    private final JComboBox<String> portCombo = new JComboBox<>();

    private McpsSequencePulseDialog sequenceDialog;

    public McpsControlFrame() {
        super("SPB_STU_MCPS — Управление каналами");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(980, 520);
        setLocationRelativeTo(null);

        logger = new AsyncLogger("mcps_communication.log");
        service = new McpsCommunicationService(logger);

        channelsPanel = new McpsChannelsPanel(service, logger);

        initComponents();
        refreshPorts();
        loadLastPort();

        service.setConnectionStatusListener(status -> SwingUtilities.invokeLater(() -> {
            if (status.startsWith("CONNECTED")) {
                connectionLabel.setText("Подключено: " + status.substring(10));
                connectionLabel.setForeground(Color.GREEN.darker());
                openPortBtn.setEnabled(false);
                closePortBtn.setEnabled(true);
                seqBtn.setEnabled(true);
            } else {
                connectionLabel.setText("Отключено");
                connectionLabel.setForeground(Color.RED);
                openPortBtn.setEnabled(true);
                closePortBtn.setEnabled(false);
                seqBtn.setEnabled(false);
                if (sequenceDialog != null) sequenceDialog.dispose();
            }
        }));
    }

    private void initComponents() {
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.add(new JLabel("COM порт:"));
        portCombo.setPreferredSize(new Dimension(140, 28));
        top.add(portCombo);
        JButton refreshBtn = new JButton("↻");
        refreshBtn.addActionListener(e -> refreshPorts());
        top.add(refreshBtn);
        top.add(openPortBtn);
        top.add(closePortBtn);
        top.add(Box.createHorizontalStrut(20));
        top.add(connectionLabel);
        top.add(Box.createHorizontalGlue());
        top.add(seqBtn);

        openPortBtn.addActionListener(e -> openSelectedPort());
        closePortBtn.addActionListener(e -> closePort());
        seqBtn.addActionListener(e -> showSequenceDialog());
        closePortBtn.setEnabled(false);
        seqBtn.setEnabled(false);

        JPanel center = new JPanel(new BorderLayout());
        center.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        center.add(channelsPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton exitBtn = new JButton("Выход");
        exitBtn.addActionListener(e -> {
            shutdown();
            System.exit(0);
        });
        bottom.add(new JLabel("Лог: mcps_communication.log   |   "));
        bottom.add(exitBtn);

        add(top, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);
        add(bottom, BorderLayout.SOUTH);
    }

    private void refreshPorts() {
        portCombo.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort p : ports) {
            portCombo.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portCombo.getItemCount() == 0) {
            portCombo.addItem("Нет доступных портов");
        }
    }

    private void loadLastPort() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        String last = prefs.get(LAST_PORT, "");
        if (!last.isEmpty()) {
            for (int i = 0; i < portCombo.getItemCount(); i++) {
                if (portCombo.getItemAt(i).startsWith(last)) {
                    portCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void saveLastPort(String portName) {
        Preferences.userRoot().node(PREFS_NODE).put(LAST_PORT, portName);
    }

    private void openSelectedPort() {
        String selected = (String) portCombo.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(this, "Выберите COM порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();

        boolean ok = service.openPort(portName);
        if (ok) {
            saveLastPort(portName);
            // Автоматически запрашиваем текущее состояние
            service.readAllOutputs();
            service.readMode();
        } else {
            JOptionPane.showMessageDialog(this, "Не удалось открыть порт " + portName);
        }
    }

    private void closePort() {
        if (sequenceDialog != null) sequenceDialog.dispose();
        channelsPanel.shutdown();
        service.closePort();
    }

    private void showSequenceDialog() {
        if (sequenceDialog == null || !sequenceDialog.isDisplayable()) {
            sequenceDialog = new McpsSequencePulseDialog(this, service, logger);
            // Блокируем/разблокируем каналы при старте/стопе последовательности
            sequenceDialog.setOnSequenceStateChange(active -> {
                channelsPanel.setGlobalPulseActive(active);
                logger.info("Sequence active = " + active);
            });
        }
        sequenceDialog.setVisible(true);
    }

    public void shutdown() {
        if (sequenceDialog != null) sequenceDialog.dispose();
        channelsPanel.shutdown();
        service.shutdown();
        logger.shutdown();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new McpsControlFrame().setVisible(true);
        });
    }
}
