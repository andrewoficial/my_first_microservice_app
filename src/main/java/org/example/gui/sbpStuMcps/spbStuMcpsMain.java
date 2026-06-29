package org.example.gui.sbpStuMcps;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.services.comPort.BaudRatesList;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class spbStuMcpsMain {
    private static final String PREFS_NODE = "org/example/gui/sbpStuMcps";
    private static final String LAST_PORT = "lastPort";

    private JPanel mainPanel;
    private JComboBox<String> portComboSelect;
    private JButton refreshBtn;
    private JButton openPortBtn;
    private JButton closePortBtn;
    private JLabel connectionLabel;
    private JButton seqBtn;
    private JPanel channelsPlaceholder;
    private JLabel logLabel;
    private JButton exitBtn;
    private JComboBox portComboSpeed;
    private JPanel comConnectionIndicatorContainer;
    private JPanel comConnectionIndicatorContainerLamp;
    private JPanel deviceConnectionIndicatorContainer;
    private JPanel deviceConnectionIndicatorContainerLamp;
    private JLabel deviceConnectionLabel;
    private JPanel generationStatusPanel;
    private JPanel generationStatusLamp;
    private JPanel connectionPanelContainer;
    private JPanel logLableContainer;
    private JPanel channelsPlaceholderContainer;
    private JLabel generationStatusArea;
    private JButton openLogFileFolder;

    private final AsyncLogger logger;
    private final McpsCommunicationService service;
    private final McpsChannelsPanel channelsPanel;
    private McpsSequencePulseDialog sequenceDialog;

    private final LampIndicator comLamp = new LampIndicator();
    private final LampIndicator deviceLamp = new LampIndicator();
    private final LampIndicator genLamp = new LampIndicator();
    private Consumer<String> deviceDetectionListener;
    private Timer deviceDetectionTimer;

    public spbStuMcpsMain() {
        logger = new AsyncLogger("mcps_communication.log");
        service = new McpsCommunicationService(logger);
        channelsPanel = new McpsChannelsPanel(service, logger);

        channelsPlaceholder.setLayout(new BorderLayout());
        channelsPlaceholder.add(channelsPanel, BorderLayout.CENTER);

        comConnectionIndicatorContainerLamp.setLayout(new BorderLayout());
        comConnectionIndicatorContainerLamp.add(comLamp, BorderLayout.CENTER);
        deviceConnectionIndicatorContainerLamp.setLayout(new BorderLayout());
        deviceConnectionIndicatorContainerLamp.add(deviceLamp, BorderLayout.CENTER);
        generationStatusLamp.setLayout(new BorderLayout());
        generationStatusLamp.add(genLamp, BorderLayout.CENTER);

        for (BaudRatesList rate : BaudRatesList.values()) {
            portComboSpeed.addItem(String.valueOf(rate.getValue()));
        }
        portComboSpeed.setSelectedItem("115200");

        comLamp.setLampColor(Color.RED);
        deviceLamp.setLampColor(Color.RED);
        genLamp.setLampColor(Color.RED);
        connectionLabel.setText("Порт закрыт");

        openPortBtn.addActionListener(e -> openSelectedPort());
        closePortBtn.addActionListener(e -> closePort());
        refreshBtn.addActionListener(e -> refreshPorts());
        seqBtn.addActionListener(e -> showSequenceDialog());

        closePortBtn.setEnabled(false);
        seqBtn.setEnabled(false);

        service.setConnectionStatusListener(status -> SwingUtilities.invokeLater(() -> {
            if (status.equals("DISCONNECTED")) {
                onDisconnected();
            }
        }));

        refreshPorts();
        loadLastPort();
    }

    public JPanel getMainPanel() {
        return mainPanel;
    }

    private void refreshPorts() {
        portComboSelect.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort p : ports) {
            portComboSelect.addItem(p.getSystemPortName() + " — " + p.getDescriptivePortName());
        }
        if (portComboSelect.getItemCount() == 0) {
            portComboSelect.addItem("Нет доступных портов");
        }
    }

    private void loadLastPort() {
        Preferences prefs = Preferences.userRoot().node(PREFS_NODE);
        String last = prefs.get(LAST_PORT, "");
        if (!last.isEmpty()) {
            for (int i = 0; i < portComboSelect.getItemCount(); i++) {
                if (portComboSelect.getItemAt(i).startsWith(last)) {
                    portComboSelect.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private void saveLastPort(String portName) {
        Preferences.userRoot().node(PREFS_NODE).put(LAST_PORT, portName);
    }

    private void openSelectedPort() {
        String selected = (String) portComboSelect.getSelectedItem();
        if (selected == null || selected.contains("Нет")) {
            JOptionPane.showMessageDialog(mainPanel, "Выберите COM порт");
            return;
        }
        String portName = selected.split(" — ")[0].trim();
        comLamp.setLampColor(Color.YELLOW);
        connectionLabel.setText("Открытие порта...");
        boolean ok = service.openPort(portName);
        if (ok) {
            saveLastPort(portName);
            comLamp.setLampColor(Color.GREEN);
            connectionLabel.setText("Порт открыт");
            openPortBtn.setEnabled(false);
            closePortBtn.setEnabled(true);
            seqBtn.setEnabled(true);
            service.readAllOutputs();
            service.readMode();
            startDeviceDetection();
        } else {
            comLamp.setLampColor(Color.RED);
            connectionLabel.setText("Ошибка открытия порта");
        }
    }

    private void closePort() {
        cancelDeviceDetection();
        if (sequenceDialog != null) sequenceDialog.dispose();
        channelsPanel.shutdown();
        service.closePort();
        onDisconnected();
    }

    private void onDisconnected() {
        comLamp.setLampColor(Color.RED);
        connectionLabel.setText("Порт закрыт");
        deviceLamp.setLampColor(Color.RED);
        deviceConnectionLabel.setText("Устройство не найдено");
        genLamp.setLampColor(Color.RED);
        openPortBtn.setEnabled(true);
        closePortBtn.setEnabled(false);
        seqBtn.setEnabled(false);
        if (sequenceDialog != null) sequenceDialog.dispose();
    }

    private void startDeviceDetection() {
        cancelDeviceDetection();
        deviceLamp.setLampColor(Color.YELLOW);
        deviceConnectionLabel.setText("Поиск устройства...");
        deviceDetectionTimer = new Timer(2000, e -> {
            deviceLamp.setLampColor(Color.RED);
            deviceConnectionLabel.setText("Устройство не найдено");
        });
        deviceDetectionTimer.setRepeats(false);
        deviceDetectionTimer.start();
        deviceDetectionListener = response -> {
            cancelDeviceDetection();
            deviceLamp.setLampColor(Color.GREEN);
            deviceConnectionLabel.setText("Устройство найдено");
        };
        service.addResponseListener(deviceDetectionListener);
    }

    private void cancelDeviceDetection() {
        if (deviceDetectionTimer != null) {
            deviceDetectionTimer.stop();
            deviceDetectionTimer = null;
        }
        if (deviceDetectionListener != null) {
            service.removeResponseListener(deviceDetectionListener);
            deviceDetectionListener = null;
        }
    }

    private void showSequenceDialog() {
        Window window = SwingUtilities.getWindowAncestor(mainPanel);
        Frame owner = (window instanceof Frame) ? (Frame) window : null;
        if (sequenceDialog == null || !sequenceDialog.isDisplayable()) {
            sequenceDialog = new McpsSequencePulseDialog(owner, service, logger);
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        logLableContainer = new JPanel();
        logLableContainer.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(logLableContainer, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 35), new Dimension(-1, 35), new Dimension(-1, 35), 2, false));
        logLabel = new JLabel();
        logLabel.setText("Лог: mcps_communication.log");
        logLableContainer.add(logLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openLogFileFolder = new JButton();
        openLogFileFolder.setText("Открыть папку с файлом");
        logLableContainer.add(openLogFileFolder, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        connectionPanelContainer = new JPanel();
        connectionPanelContainer.setLayout(new GridLayoutManager(17, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(connectionPanelContainer, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(270, -1), new Dimension(270, -1), new Dimension(270, -1), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("COM порт:");
        connectionPanelContainer.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSelect = new JComboBox();
        connectionPanelContainer.add(portComboSelect, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(215, -1), new Dimension(215, -1), new Dimension(215, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Скорость, бод");
        connectionPanelContainer.add(label2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        portComboSpeed = new JComboBox();
        connectionPanelContainer.add(portComboSpeed, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(215, -1), new Dimension(215, -1), new Dimension(215, -1), 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Обновить список портов");
        connectionPanelContainer.add(label3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        openPortBtn = new JButton();
        openPortBtn.setText("Открыть порт");
        connectionPanelContainer.add(openPortBtn, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(215, -1), new Dimension(215, -1), new Dimension(215, -1), 0, false));
        closePortBtn = new JButton();
        closePortBtn.setText("Закрыть порт");
        connectionPanelContainer.add(closePortBtn, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(215, -1), new Dimension(215, -1), new Dimension(215, -1), 0, false));
        comConnectionIndicatorContainer = new JPanel();
        comConnectionIndicatorContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        connectionPanelContainer.add(comConnectionIndicatorContainer, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comConnectionIndicatorContainerLamp = new JPanel();
        comConnectionIndicatorContainerLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        comConnectionIndicatorContainer.add(comConnectionIndicatorContainerLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        connectionLabel = new JLabel();
        connectionLabel.setText("Отключено");
        comConnectionIndicatorContainer.add(connectionLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        comConnectionIndicatorContainer.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        deviceConnectionIndicatorContainer = new JPanel();
        deviceConnectionIndicatorContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        connectionPanelContainer.add(deviceConnectionIndicatorContainer, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        deviceConnectionIndicatorContainerLamp = new JPanel();
        deviceConnectionIndicatorContainerLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        deviceConnectionIndicatorContainer.add(deviceConnectionIndicatorContainerLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        deviceConnectionLabel = new JLabel();
        deviceConnectionLabel.setText("Устройство не найдено");
        deviceConnectionIndicatorContainer.add(deviceConnectionLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        deviceConnectionIndicatorContainer.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        seqBtn = new JButton();
        seqBtn.setText("Последовательность импульсов");
        connectionPanelContainer.add(seqBtn, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        generationStatusPanel = new JPanel();
        generationStatusPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        connectionPanelContainer.add(generationStatusPanel, new GridConstraints(16, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(200, 80), new Dimension(200, 80), new Dimension(200, 80), 0, false));
        generationStatusLamp = new JPanel();
        generationStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        generationStatusPanel.add(generationStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        generationStatusArea = new JLabel();
        generationStatusArea.setText("Label");
        generationStatusPanel.add(generationStatusArea, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        connectionPanelContainer.add(spacer3, new GridConstraints(14, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Операции с выбранным портом");
        connectionPanelContainer.add(label4, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Статус выбранного порта");
        connectionPanelContainer.add(label5, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Статус обнаружения устройства");
        connectionPanelContainer.add(label6, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Текущий статус прибора");
        connectionPanelContainer.add(label7, new GridConstraints(15, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        refreshBtn = new JButton();
        refreshBtn.setText("↻");
        connectionPanelContainer.add(refreshBtn, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(215, -1), new Dimension(215, -1), new Dimension(215, -1), 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        channelsPlaceholderContainer = new JPanel();
        channelsPlaceholderContainer.setLayout(new GridLayoutManager(1, 1, new Insets(5, 5, 5, 5), -1, -1));
        scrollPane1.setViewportView(channelsPlaceholderContainer);
        channelsPlaceholder = new JPanel();
        channelsPlaceholderContainer.add(channelsPlaceholder, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

}
