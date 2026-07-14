package org.example.gui.devices.edvards.d39730880.control;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protEdwardsD397.EdwardsCommunicationService;
import org.example.device.protEdwardsD397.EdwardsD397CommandRegistry;
import org.example.utilites.MyUtilities;
import javax.swing.*;
import java.awt.*;
import java.util.concurrent.*;
import java.util.prefs.Preferences;

@Slf4j
public class EdwardsTicMain {
    private static final String PREFS_NODE = "org/example/gui/edwards/tic";
    private static final String LAST_PORT = "lastPort";
    private static final String LAST_BAUD = "lastBaud";

    private JPanel mainPanel;
    private JComboBox<String> portCombo;
    private JComboBox<Integer> baudCombo;
    private JButton refreshPortsBtn;
    private JButton openBtn;
    private JButton closeBtn;
    private JPanel comStatusLamp;
    private JLabel comStatusLabel;
    private JPanel deviceStatusLamp;
    private JLabel deviceStatusLabel;

    // Статус системы
    private JTextArea systemStatusArea;
    private JButton readSystemStatusBtn;
    private JButton readAllGaugesBtn;
    private JButton turboOnBtn;
    private JButton turboOffBtn;
    private JButton readTempsBtn;
    private JCheckBox autoRefreshCheck;

    private JLabel statusBar;
    private final EdwardsCommunicationService service = new EdwardsCommunicationService();
    private final EdwardsD397CommandRegistry registry = new EdwardsD397CommandRegistry();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private volatile boolean autoRefreshActive = false;

    private final LampIndicator comLamp = new LampIndicator();
    private final LampIndicator deviceLamp = new LampIndicator();

    public EdwardsTicMain() {
        initUI();
        registerListeners();
        refreshPorts();
        loadLastSettings();
    }

    private void initUI() {
        mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // === TOP: COM controls ===
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        topPanel.setBorder(BorderFactory.createTitledBorder("COM порт"));

        portCombo = new JComboBox<>();
        portCombo.setPreferredSize(new Dimension(280, 28));
        baudCombo = new JComboBox<>(new Integer[]{9600, 19200, 38400});
        baudCombo.setSelectedItem(9600);

        refreshPortsBtn = new JButton("Обновить порты");
        openBtn = new JButton("Открыть");
        closeBtn = new JButton("Закрыть");
        closeBtn.setEnabled(false);

        comStatusLamp = new JPanel();
        deviceStatusLamp = new JPanel();
        comStatusLamp.setPreferredSize(new Dimension(18, 18));
        deviceStatusLamp.setPreferredSize(new Dimension(18, 18));
        comStatusLamp.add(comLamp);
        deviceStatusLamp.add(deviceLamp);

        topPanel.add(new JLabel("Порт:"));
        topPanel.add(portCombo);
        topPanel.add(new JLabel("Скорость:"));
        topPanel.add(baudCombo);
        topPanel.add(refreshPortsBtn);
        topPanel.add(openBtn);
        topPanel.add(closeBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(comStatusLamp);
        topPanel.add(comStatusLabel = new JLabel("Порт закрыт"));
        topPanel.add(Box.createHorizontalStrut(15));
        topPanel.add(deviceStatusLamp);
        topPanel.add(deviceStatusLabel = new JLabel("Устройство не подключено"));

        mainPanel.add(topPanel, BorderLayout.NORTH);

        // === CENTER: Основные кнопки и статус ===
        JPanel centerPanel = new JPanel(new GridLayout(1, 2, 10, 10));

        // Левая колонка — Системный статус
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.setBorder(BorderFactory.createTitledBorder("Системный статус (902)"));
        systemStatusArea = new JTextArea(12, 45);
        systemStatusArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        systemStatusArea.setEditable(false);
        left.add(new JScrollPane(systemStatusArea), BorderLayout.CENTER);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        readSystemStatusBtn = new JButton("Прочитать статус");
        readAllGaugesBtn = new JButton("Все датчики (?V940)");
        leftButtons.add(readSystemStatusBtn);
        leftButtons.add(readAllGaugesBtn);
        left.add(leftButtons, BorderLayout.SOUTH);

        // Правая колонка — Управление
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setBorder(BorderFactory.createTitledBorder("Управление"));

        turboOnBtn = new JButton("Включить турбонасос (IC904 1)");
        turboOffBtn = new JButton("Выключить турбонасос (IC904 0)");
        readTempsBtn = new JButton("Прочитать температуры");

        turboOnBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        turboOffBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        readTempsBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        right.add(turboOnBtn);
        right.add(Box.createVerticalStrut(6));
        right.add(turboOffBtn);
        right.add(Box.createVerticalStrut(6));
        right.add(readTempsBtn);
        right.add(Box.createVerticalStrut(20));

        autoRefreshCheck = new JCheckBox("Автообновление статуса (каждые 2 сек)");
        right.add(autoRefreshCheck);

        centerPanel.add(left);
        centerPanel.add(right);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // === BOTTOM: Status bar ===
        statusBar = new JLabel("Готов к работе");
        statusBar.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        // Начальные цвета ламп
        comLamp.setLampColor(Color.RED);
        deviceLamp.setLampColor(Color.RED);
    }

    private void registerListeners() {
        refreshPortsBtn.addActionListener(e -> refreshPorts());
        openBtn.addActionListener(e -> openPort());
        closeBtn.addActionListener(e -> closePort());

        readSystemStatusBtn.addActionListener(e -> readSystemStatus());
        readAllGaugesBtn.addActionListener(e -> readAllGauges());
        turboOnBtn.addActionListener(e -> sendTurboCommand(true));
        turboOffBtn.addActionListener(e -> sendTurboCommand(false));
        readTempsBtn.addActionListener(e -> readTemperatures());

        autoRefreshCheck.addActionListener(e -> toggleAutoRefresh());

        service.addResponseListener(this::handleResponse);
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

    private void loadLastSettings() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        String lastPort = prefs.get(LAST_PORT, "");
        int lastBaud = prefs.getInt(LAST_BAUD, 9600);
        if (!lastPort.isEmpty()) {
            for (int i = 0; i < portCombo.getItemCount(); i++) {
                if (portCombo.getItemAt(i).startsWith(lastPort)) {
                    portCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
        baudCombo.setSelectedItem(lastBaud);
    }

    private void saveLastSettings(String portName, int baud) {
        Preferences.userRoot().node(PREFS_NODE).put(LAST_PORT, portName);
        Preferences.userRoot().node(PREFS_NODE).putInt(LAST_BAUD, baud);
    }

    private void openPort() {
        String selected = (String) portCombo.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(mainPanel, "Выберите COM-порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        int baud = (int) baudCombo.getSelectedItem();

        comLamp.setLampColor(Color.YELLOW);
        comStatusLabel.setText("Открытие порта...");
        openBtn.setEnabled(false);

        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                return service.openPort(portName, baud);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        saveLastSettings(portName, baud);
                        comLamp.setLampColor(Color.GREEN);
                        comStatusLabel.setText("Порт открыт");
                        closeBtn.setEnabled(true);
                        readSystemStatusBtn.setEnabled(true);
                        readAllGaugesBtn.setEnabled(true);
                        turboOnBtn.setEnabled(true);
                        turboOffBtn.setEnabled(true);
                        readTempsBtn.setEnabled(true);
                        autoRefreshCheck.setEnabled(true);
                        deviceLamp.setLampColor(Color.YELLOW);
                        deviceStatusLabel.setText("Устройство подключено (ожидание ответа)");
                        statusBar.setText("Порт открыт. Можно отправлять команды.");

                        readSystemStatus();
                    } else {
                        comLamp.setLampColor(Color.RED);
                        comStatusLabel.setText("Ошибка открытия порта");
                        openBtn.setEnabled(true);
                    }
                } catch (Exception ex) {
                    log.error("Open port error", ex);
                }
            }
        }.execute();
    }

    private void closePort() {
        stopAutoRefresh();
        service.closePort();
        comLamp.setLampColor(Color.RED);
        comStatusLabel.setText("Порт закрыт");
        deviceLamp.setLampColor(Color.RED);
        deviceStatusLabel.setText("Устройство не подключено");
        openBtn.setEnabled(true);
        closeBtn.setEnabled(false);
        readSystemStatusBtn.setEnabled(false);
        readAllGaugesBtn.setEnabled(false);
        turboOnBtn.setEnabled(false);
        turboOffBtn.setEnabled(false);
        readTempsBtn.setEnabled(false);
        autoRefreshCheck.setEnabled(false);
        systemStatusArea.setText("");
        statusBar.setText("Порт закрыт");
    }

    // ==================== Отправка команд ====================

    private void readSystemStatus() {
        byte[] cmd = "?V00902".getBytes();
        service.sendRequest(cmd);
    }

    private void readAllGauges() {
        byte[] cmd = "?V00940".getBytes();
        service.sendRequest(cmd);
    }

    private void sendTurboCommand(boolean on) {
        String cmd = on ? "IC904 1" : "IC904 0";
        service.sendCommand(cmd);
        statusBar.setText("Отправлена команда: " + cmd);
    }

    private void readTemperatures() {
        service.sendCommand("?V00919");
        try { Thread.sleep(80); } catch (InterruptedException ignored) {}
        service.sendCommand("?V00920");
    }

    private void toggleAutoRefresh() {
        if (autoRefreshCheck.isSelected()) {
            startAutoRefresh();
        } else {
            stopAutoRefresh();
        }
    }

    private void startAutoRefresh() {
        if (autoRefreshActive) return;
        autoRefreshActive = true;
        scheduler.scheduleAtFixedRate(() -> {
            if (autoRefreshActive) {
                readSystemStatus();
            }
        }, 0, 2000, TimeUnit.MILLISECONDS);
    }

    private void stopAutoRefresh() {
        autoRefreshActive = false;
    }

    // ==================== Обработка ответов ====================

    private void handleResponse(byte[] response) {
        String text = new String(response).trim();
        log.info("Edwards RX: " + MyUtilities.bytesToHexString(response) + " → " + text);

        SwingUtilities.invokeLater(() -> {
            if (text.startsWith("=V902") || text.startsWith("*V902")) {
                systemStatusArea.setText("=== СИСТЕМНЫЙ СТАТУС (902) ===\n" + text);
                deviceLamp.setLampColor(Color.GREEN);
                deviceStatusLabel.setText("Устройство отвечает");
            } 
            else if (text.startsWith("=V940") || text.startsWith("*V940")) {
                systemStatusArea.append("\n\n=== ВСЕ ДАТЧИКИ (940) ===\n" + text);
            } 
            else if (text.startsWith("=V919") || text.startsWith("=V920")) {
                systemStatusArea.append("\n" + text);
            } 
            else if (text.startsWith("*C904")) {
                statusBar.setText("Команда турбонасоса выполнена: " + text);
            } 
            else {
                systemStatusArea.append("\n" + text);
            }
        });
    }

    // ==================== Вспомогательный класс лампы ====================

    private static class LampIndicator extends JPanel {
        private Color color = Color.GRAY;

        public LampIndicator() {
            setPreferredSize(new Dimension(18, 18));
            setOpaque(false);
        }

        public void setLampColor(Color c) {
            this.color = c;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight()) - 2;
            g2.setColor(color);
            g2.fillOval(1, 1, size, size);
            g2.setColor(Color.DARK_GRAY);
            g2.drawOval(1, 1, size, size);
            g2.dispose();
        }
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    public void shutdown() {
        stopAutoRefresh();
        service.shutdown();
    }
}