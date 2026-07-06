package org.example.gui.sbpStuMcps;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class channelSettings {
    private JPanel rootPanel;
    private JButton toggleBtn;
    private JTextField durationField;
    private JTextField periodField;
    private JButton pulseBtn;
    private JPanel pulseStatusLamp;
    private JLabel pulseStatusLabel;
    private JPanel outputStatusContainer;
    private JPanel outputStatusLamp;
    private JLabel outputStatusLabel;
    private JLabel channelName;
    private JPanel chanStatusContainer;
    private JPanel chanStatusLamp;
    private JLabel sendCommand;
    private JLabel readValue;
    private JPanel chanDataTransferLogContainer;
    private JPanel сhannelSubContainerTwo;
    private JPanel chanControllerContainer;
    private JLabel chanelName;
    private JLabel sentValueHeader;

    private final int channel;
    private final McpsCommunicationService service;
    private final AsyncLogger logger;
    private final ChannelPulseCoordinator coordinator;

    private final LampIndicator outputLamp = new LampIndicator();
    private final LampIndicator pulseLamp = new LampIndicator();
    private final LampIndicator chanLamp = new LampIndicator();

    private Border durationFieldDefaultBorder;
    private Border periodFieldDefaultBorder;

    private int lastDuration;
    private int lastPeriod;

    private volatile boolean isConstantOn = false;
    private volatile boolean actualOn = false;
    private volatile boolean isPulsing = false;
    private Timer pulseTimer;
    private Timer pulseOffTimer;
    private int pulseDuration;
    private int pulseSeq;

    private static final Color GREEN = new Color(0, 200, 0);
    private static final Color YELLOW = new Color(255, 200, 0);
    private static final Color RED = Color.RED;

    public channelSettings(int channel, McpsCommunicationService service, AsyncLogger logger, ChannelPulseCoordinator coordinator) {
        this.channel = channel;
        this.service = service;
        this.logger = logger;
        this.coordinator = coordinator;

        channelName.setText("Канал " + channel);
        chanelName.setText("Канал #" + channel);
        sentValueHeader.setText("Отправленное значение");
        durationFieldDefaultBorder = durationField.getBorder();
        periodFieldDefaultBorder = periodField.getBorder();
        lastDuration = Integer.parseInt(durationField.getText().trim());
        lastPeriod = Integer.parseInt(periodField.getText().trim());
        rootPanel.setBorder(new LineBorder(Color.GRAY, 1));

        outputStatusLamp.setLayout(new BorderLayout());
        outputStatusLamp.add(outputLamp, BorderLayout.CENTER);
        pulseStatusLamp.setLayout(new BorderLayout());
        pulseStatusLamp.add(pulseLamp, BorderLayout.CENTER);
        chanStatusLamp.setLayout(new BorderLayout());
        chanStatusLamp.add(chanLamp, BorderLayout.CENTER);

        outputLamp.setLampColor(RED);
        pulseLamp.setLampColor(RED);
        chanLamp.setLampColor(RED);

        toggleBtn.addActionListener(e -> toggleConstant());
        pulseBtn.addActionListener(e -> togglePulse());
        durationField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                validatePeriod(durationField);
            }
        });
        durationField.addActionListener(e -> validatePeriod(durationField));
        periodField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                validatePeriod(periodField);
            }
        });
        periodField.addActionListener(e -> validatePeriod(periodField));

        сhannelSubContainerTwo.setFocusable(true);
        сhannelSubContainerTwo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Component c = SwingUtilities.getDeepestComponentAt(сhannelSubContainerTwo, e.getX(), e.getY());
                if (c == null || (!(c instanceof JTextField) && !(c instanceof AbstractButton))) {
                    сhannelSubContainerTwo.requestFocusInWindow();
                }
            }
        });

        updateUiState();
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void validatePeriod(JComponent changedField) {
        durationField.setBorder(durationFieldDefaultBorder);
        periodField.setBorder(periodFieldDefaultBorder);

        boolean valid = true;
        int dur = 0, per = 0;
        try {
            dur = Integer.parseInt(durationField.getText().trim());
        } catch (NumberFormatException ex) {
            durationField.setBorder(BorderFactory.createLineBorder(Color.RED));
            valid = false;
        }
        try {
            per = Integer.parseInt(periodField.getText().trim());
        } catch (NumberFormatException ex) {
            periodField.setBorder(BorderFactory.createLineBorder(Color.RED));
            valid = false;
        }

        if (valid && per < dur) {
            changedField.setBorder(BorderFactory.createLineBorder(Color.RED));
            valid = false;
            if (changedField == periodField) {
                JOptionPane.showMessageDialog(rootPanel,
                        "Вы задали период " + per + ", а был период " + lastPeriod
                                + ". Период не может быть меньше длительности (" + dur + ").",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(rootPanel,
                        "Вы задали длительность " + dur + ", а была длительность " + lastDuration
                                + ". Длительность не может быть больше периода (" + per + ").",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
            }
        }

        if (valid) {
            lastDuration = dur;
            lastPeriod = per;
        }

        pulseBtn.setEnabled(valid && !isPulsing && !coordinator.isAnyPulseActive());
    }

    private void toggleConstant() {
        if (coordinator.isAnyPulseActive() && !isPulsing) {
            JOptionPane.showMessageDialog(rootPanel,
                    "Сначала остановите импульсный режим на активном канале");
            return;
        }
        isConstantOn = !isConstantOn;
        service.writeOutput(channel, isConstantOn, 0);
        sendCommand.setText(String.format("@WR%02d %s", channel, isConstantOn ? "1,0" : "0"));
        updateUiState();
        Timer readTimer = new Timer(150, e -> service.readOutput(channel));
        readTimer.setRepeats(false);
        readTimer.start();
    }

    private void togglePulse() {
        if (isPulsing) {
            stopPulse();
        } else {
            startPulse();
        }
    }

    private void firePulseOn(int duration) {
        pulseSeq++;
        String cmd = String.format("@WR%02d 1,%d", channel, duration);
        sendCommand.setText(cmd);
        sentValueHeader.setText("Отправленное значение #" + pulseSeq);
        chanLamp.setLampColor(GREEN);
        try {
            service.writeOutput(channel, true, duration);
        } catch (Exception ex) {
            logger.warn("Ошибка отправки импульса: " + ex.getMessage());
        }
        schedulePulseOff(duration);
    }

    private void schedulePulseOff(int duration) {
        if (pulseOffTimer != null) {
            pulseOffTimer.stop();
            pulseOffTimer = null;
        }
        pulseOffTimer = new Timer(duration, e -> {
            if (isPulsing) {
                chanLamp.setLampColor(YELLOW);
            }
            pulseOffTimer = null;
        });
        pulseOffTimer.setRepeats(false);
        pulseOffTimer.start();
    }

    private void startPulse() {
        if (coordinator.isAnyPulseActive()) {
            JOptionPane.showMessageDialog(rootPanel,
                    "Уже работает импульсный режим на другом канале/последовательности");
            return;
        }
        int duration, period;
        try {
            duration = Integer.parseInt(durationField.getText().trim());
            period = Integer.parseInt(periodField.getText().trim());
            if (duration < 1 || duration > 65535 || period < duration || period > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(rootPanel,
                    "Длительность 1..65535, Период >= длительности");
            return;
        }

        coordinator.onPulseStateChanged(true);
        isPulsing = true;
        pulseDuration = duration;
        pulseSeq = 0;
        coordinator.setAllControlsEnabled(false);
        setControlsEnabled(true);

        firePulseOn(duration);
        updateUiState();

        pulseTimer = new Timer(period, e -> {
            if (!isPulsing) return;
            firePulseOn(pulseDuration);
        });
        pulseTimer.setInitialDelay(period);
        pulseTimer.start();

        logger.info("Канал " + channel + " запущен в импульсном режиме: " + duration + "мс / " + period + "мс");
    }

    void stopPulse() {
        if (pulseTimer != null) {
            pulseTimer.stop();
            pulseTimer = null;
        }
        if (pulseOffTimer != null) {
            pulseOffTimer.stop();
            pulseOffTimer = null;
        }
        isPulsing = false;
        coordinator.onPulseStateChanged(false);
        coordinator.setAllControlsEnabled(true);

        service.writeOutput(channel, false, 0);
        sendCommand.setText(String.format("@WR%02d 0", channel));
        chanLamp.setLampColor(RED);
        updateUiState();
        logger.info("Канал " + channel + " остановлен");
    }

    void updateActualState(boolean on) {
        actualOn = on;
        readValue.setText(String.format("@RA%02d %d", channel, on ? 1 : 0));
        SwingUtilities.invokeLater(this::updateUiState);
    }

    private void updateUiState() {
        if (isConstantOn) {
            toggleBtn.setText("Выкл");
            outputLamp.setLampColor(GREEN);
            outputStatusLabel.setText("Выход включен");
        } else {
            toggleBtn.setText("Вкл");
            outputLamp.setLampColor(RED);
            outputStatusLabel.setText("Выход отключен");
        }

        if (isPulsing) {
            pulseBtn.setText("Стоп");
            pulseLamp.setLampColor(GREEN);
            pulseStatusLabel.setText("Импульсный режим");
        } else {
            pulseBtn.setText("Старт");
            pulseLamp.setLampColor(RED);
            pulseStatusLabel.setText("Импульсный режим выключен");
        }

        if (isPulsing) {
            // chanLamp is managed by firePulseOn/schedulePulseOff during pulse mode
        } else if (actualOn) {
            chanLamp.setLampColor(GREEN);
        } else if (isConstantOn) {
            chanLamp.setLampColor(YELLOW);
        } else {
            chanLamp.setLampColor(RED);
        }

        boolean fieldsEnabled = !isPulsing && !coordinator.isAnyPulseActive();
        durationField.setEnabled(fieldsEnabled);
        periodField.setEnabled(fieldsEnabled);
    }

    void setControlsEnabled(boolean enabled) {
        toggleBtn.setEnabled(enabled);
        pulseBtn.setEnabled(enabled);
        if (!enabled && isPulsing) {
        } else {
            durationField.setEnabled(enabled);
            periodField.setEnabled(enabled);
        }
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
        rootPanel = new JPanel();
        rootPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), 0, 0));
        final JScrollPane scrollPane1 = new JScrollPane();
        rootPanel.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        сhannelSubContainerTwo = new JPanel();
        сhannelSubContainerTwo.setLayout(new GridLayoutManager(3, 1, new Insets(3, 3, 3, 3), -1, -1));
        scrollPane1.setViewportView(сhannelSubContainerTwo);
        chanStatusContainer = new JPanel();
        chanStatusContainer.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainerTwo.add(chanStatusContainer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(320, -1), new Dimension(320, -1), new Dimension(320, -1), 0, false));
        chanStatusLamp = new JPanel();
        chanStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        chanStatusContainer.add(chanStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        channelName = new JLabel();
        channelName.setText("Канал");
        chanStatusContainer.add(channelName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        chanStatusContainer.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        chanStatusContainer.add(panel1, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel1.add(spacer2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        chanControllerContainer = new JPanel();
        chanControllerContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainerTwo.add(chanControllerContainer, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(320, -1), new Dimension(320, -1), new Dimension(320, -1), 0, false));
        chanelName = new JLabel();
        chanelName.setText("Номер");
        chanControllerContainer.add(chanelName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(70, -1), new Dimension(70, -1), new Dimension(70, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        chanControllerContainer.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        chanControllerContainer.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        toggleBtn = new JButton();
        toggleBtn.setText("Вкл");
        panel2.add(toggleBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(70, 28), new Dimension(70, 28), new Dimension(70, 28), 0, false));
        outputStatusContainer = new JPanel();
        outputStatusContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(outputStatusContainer, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputStatusLamp = new JPanel();
        outputStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        outputStatusContainer.add(outputStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        outputStatusLabel = new JLabel();
        outputStatusLabel.setText("Выход отключен");
        outputStatusContainer.add(outputStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        outputStatusContainer.add(spacer5, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainerTwo.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(320, -1), new Dimension(320, -1), new Dimension(320, -1), 0, false));
        chanDataTransferLogContainer = new JPanel();
        chanDataTransferLogContainer.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(chanDataTransferLogContainer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        sentValueHeader = new JLabel();
        sentValueHeader.setText("Отпраленное значение");
        chanDataTransferLogContainer.add(sentValueHeader, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendCommand = new JLabel();
        sendCommand.setText(" ");
        chanDataTransferLogContainer.add(sendCommand, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        chanDataTransferLogContainer.add(panel4, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Прочитанное значение");
        panel4.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer6 = new Spacer();
        panel4.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        readValue = new JLabel();
        readValue.setText(" ");
        panel4.add(readValue, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        chanDataTransferLogContainer.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel5.add(spacer7, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, new Dimension(300, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Импульсный режим:");
        panel6.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        panel6.add(spacer8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel7, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Длит. (мс):");
        panel7.add(label3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer9 = new Spacer();
        panel7.add(spacer9, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        durationField = new JTextField();
        durationField.setText("100");
        panel7.add(durationField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(60, -1), new Dimension(60, -1), new Dimension(60, -1), 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Период (мс):");
        panel7.add(label4, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        periodField = new JTextField();
        periodField.setText("500");
        panel7.add(periodField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(60, -1), new Dimension(60, -1), new Dimension(60, -1), 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel8, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pulseBtn = new JButton();
        pulseBtn.setText("Старт");
        panel8.add(pulseBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(70, 28), new Dimension(70, 28), new Dimension(70, 28), 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.add(panel9, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(220, -1), new Dimension(220, -1), new Dimension(220, -1), 0, false));
        pulseStatusLamp = new JPanel();
        pulseStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel9.add(pulseStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        pulseStatusLabel = new JLabel();
        pulseStatusLabel.setText("Импульсный режим выключен");
        panel9.add(pulseStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
