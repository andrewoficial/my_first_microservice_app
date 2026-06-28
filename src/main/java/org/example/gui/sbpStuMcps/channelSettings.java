package org.example.gui.sbpStuMcps;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class channelSettings {
    private JPanel rootPanel;
    private JButton toggleBtn;
    private JTextField durationField;
    private JTextField periodField;
    private JButton pulseBtn;
    private JPanel pulseStatusContainer;
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
    private JPanel сhannelSubContainer;
    private JPanel chanControllerContainer;
    private JPanel chanDataTransferLogContainer;
    private JPanel pulseModeControllerContainer;
    private JButton button1;

    private final int channel;
    private final McpsCommunicationService service;
    private final AsyncLogger logger;
    private final ChannelPulseCoordinator coordinator;

    private final LampIndicator outputLamp = new LampIndicator();
    private final LampIndicator pulseLamp = new LampIndicator();
    private final LampIndicator chanLamp = new LampIndicator();

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
                validatePeriod();
            }
        });
        periodField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                validatePeriod();
            }
        });
        updateUiState();
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void validatePeriod() {
        try {
            int dur = Integer.parseInt(durationField.getText().trim());
            int per = Integer.parseInt(periodField.getText().trim());
            if (per < dur) {
                periodField.setBackground(new Color(255, 200, 200));
                JOptionPane.showMessageDialog(rootPanel,
                        "Период должен быть >= длительности импульса",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                periodField.setText(String.valueOf(Math.max(dur, 100)));
            } else {
                periodField.setBackground(Color.WHITE);
            }
        } catch (NumberFormatException ex) {
            periodField.setBackground(new Color(255, 200, 200));
        }
    }

    private void toggleConstant() {
        if (coordinator.isAnyPulseActive() && !isPulsing) {
            JOptionPane.showMessageDialog(rootPanel,
                    "Сначала остановите импульсный режим на активном канале");
            return;
        }
        isConstantOn = !isConstantOn;
        service.writeOutput(channel, isConstantOn, 0);
        sendCommand.setText(String.format("@WR%02d %d", channel, isConstantOn ? 1 : 0));
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
        sendCommand.setText(cmd + " #" + pulseSeq);
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

        if (actualOn || isConstantOn) {
            chanLamp.setLampColor(GREEN);
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
        rootPanel.setLayout(new GridLayoutManager(1, 7, new Insets(4, 4, 4, 4), 2, 2));
        сhannelSubContainer = new JPanel();
        сhannelSubContainer.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(сhannelSubContainer, new GridConstraints(0, 0, 1, 7, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(350, 350), new Dimension(350, 350), new Dimension(350, 350), 0, false));
        chanStatusContainer = new JPanel();
        chanStatusContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainer.add(chanStatusContainer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(250, -1), new Dimension(250, -1), new Dimension(250, -1), 0, false));
        chanStatusLamp = new JPanel();
        chanStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        chanStatusContainer.add(chanStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        channelName = new JLabel();
        channelName.setText("Канал");
        chanStatusContainer.add(channelName, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        chanStatusContainer.add(spacer1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        chanControllerContainer = new JPanel();
        chanControllerContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainer.add(chanControllerContainer, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(250, -1), new Dimension(250, -1), new Dimension(250, -1), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Выход");
        chanControllerContainer.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, -1), new Dimension(50, -1), new Dimension(50, -1), 0, false));
        outputStatusContainer = new JPanel();
        outputStatusContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        chanControllerContainer.add(outputStatusContainer, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        outputStatusLamp = new JPanel();
        outputStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        outputStatusContainer.add(outputStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        outputStatusLabel = new JLabel();
        outputStatusLabel.setText("Выход отключен");
        outputStatusContainer.add(outputStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        outputStatusContainer.add(spacer2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        toggleBtn = new JButton();
        toggleBtn.setText("Вкл");
        chanControllerContainer.add(toggleBtn, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(70, 28), new Dimension(70, 28), new Dimension(70, 28), 0, false));
        chanDataTransferLogContainer = new JPanel();
        chanDataTransferLogContainer.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainer.add(chanDataTransferLogContainer, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(250, -1), new Dimension(250, -1), new Dimension(250, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Отпраленное значение");
        chanDataTransferLogContainer.add(label2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sendCommand = new JLabel();
        sendCommand.setText("Label");
        chanDataTransferLogContainer.add(sendCommand, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Прочитанное значение");
        chanDataTransferLogContainer.add(label3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        readValue = new JLabel();
        readValue.setText("null");
        chanDataTransferLogContainer.add(readValue, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pulseModeControllerContainer = new JPanel();
        pulseModeControllerContainer.setLayout(new GridLayoutManager(3, 5, new Insets(0, 0, 0, 0), -1, -1));
        сhannelSubContainer.add(pulseModeControllerContainer, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Импульсный режим:");
        pulseModeControllerContainer.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        pulseModeControllerContainer.add(spacer3, new GridConstraints(0, 1, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Длит. (мс):");
        pulseModeControllerContainer.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        durationField = new JTextField();
        durationField.setText("100");
        pulseModeControllerContainer.add(durationField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(60, -1), new Dimension(60, -1), new Dimension(60, -1), 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Период (мс):");
        pulseModeControllerContainer.add(label6, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        periodField = new JTextField();
        periodField.setText("500");
        pulseModeControllerContainer.add(periodField, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(60, -1), new Dimension(60, -1), new Dimension(60, -1), 0, false));
        pulseBtn = new JButton();
        pulseBtn.setText("Старт");
        pulseModeControllerContainer.add(pulseBtn, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(70, 28), new Dimension(70, 28), new Dimension(70, 28), 0, false));
        pulseStatusContainer = new JPanel();
        pulseStatusContainer.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        pulseModeControllerContainer.add(pulseStatusContainer, new GridConstraints(2, 1, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pulseStatusLamp = new JPanel();
        pulseStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        pulseStatusContainer.add(pulseStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        pulseStatusLabel = new JLabel();
        pulseStatusLabel.setText("Импульсный режим выключен");
        pulseStatusContainer.add(pulseStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        pulseStatusContainer.add(spacer4, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer5 = new Spacer();
        pulseModeControllerContainer.add(spacer5, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
