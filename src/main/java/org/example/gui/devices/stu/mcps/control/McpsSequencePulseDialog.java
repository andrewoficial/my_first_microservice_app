package org.example.gui.devices.stu.mcps.control;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.gui.devices.stu.mcps.AsyncLogger;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class McpsSequencePulseDialog extends JDialog {
    private JPanel rootPanel;
    private JTextField sequenceField;
    private JTextField durationField;
    private JTextField periodField;
    private JButton startBtn;
    private JButton closeBtn;
    private JPanel sequenceStatusLamp;
    private JLabel sequenceStatusLabel;
    private JPanel statusPanel;
    private JCheckBox repeatSequence;

    private final McpsCommunicationService service;
    private final AsyncLogger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private Future<?> cycleFuture;

    private volatile boolean running = false;
    private int sequenceGen = 0;
    private List<Integer> channelSequence = new ArrayList<>();
    private Consumer<Boolean> onSequenceStateChange;

    private final LampIndicator seqLamp = new LampIndicator();

    private Border sequenceFieldDefaultBorder;
    private Border durationFieldDefaultBorder;
    private Border periodFieldDefaultBorder;

    private int lastDuration;
    private int lastPeriod;
    private volatile int minCommandIntervalMs = 37;

    public McpsSequencePulseDialog(Frame owner, McpsCommunicationService service, AsyncLogger logger, int minCommandIntervalMs) {
        super(owner, "Последовательная подача импульсов", true);
        this.service = service;
        this.logger = logger;
        this.minCommandIntervalMs = minCommandIntervalMs;

        setContentPane(rootPanel);
        setResizable(true);
        setLocationRelativeTo(owner);

        sequenceFieldDefaultBorder = sequenceField.getBorder();
        durationFieldDefaultBorder = durationField.getBorder();
        periodFieldDefaultBorder = periodField.getBorder();
        lastDuration = Integer.parseInt(durationField.getText().trim());
        lastPeriod = Integer.parseInt(periodField.getText().trim());

        sequenceStatusLamp.setLayout(new BorderLayout());
        sequenceStatusLamp.add(seqLamp, BorderLayout.CENTER);
        seqLamp.setLampColor(Color.RED);

        repeatSequence.setSelected(true);

        startBtn.addActionListener(e -> toggleSequence());
        closeBtn.addActionListener(e -> {
            if (running) stopSequence();
            dispose();
        });

        durationField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                validateInputs(durationField);
            }
        });
        durationField.addActionListener(e -> validateInputs(durationField));
        periodField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                validateInputs(periodField);
            }
        });
        periodField.addActionListener(e -> validateInputs(periodField));
        sequenceField.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                validateInputs(sequenceField);
            }
        });
        sequenceField.addActionListener(e -> validateInputs(sequenceField));

        rootPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Component c = SwingUtilities.getDeepestComponentAt(rootPanel, e.getX(), e.getY());
                if (c == null || (!(c instanceof JTextField) && !(c instanceof AbstractButton))) {
                    rootPanel.requestFocusInWindow();
                }
            }
        });

        validateInputs(sequenceField);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        pack();
    }

    public void setOnSequenceStateChange(Consumer<Boolean> callback) {
        this.onSequenceStateChange = callback;
    }

    public void setMinCommandIntervalMs(int ms) {
        this.minCommandIntervalMs = ms;
    }

    private void validateInputs(JComponent changedField) {
        sequenceField.setBorder(sequenceFieldDefaultBorder);
        durationField.setBorder(durationFieldDefaultBorder);
        periodField.setBorder(periodFieldDefaultBorder);

        boolean valid = true;
        int dur = 0, per = 0;

        // Validate sequence
        String seqText = sequenceField.getText().trim();
        if (seqText.isEmpty()) {
            sequenceField.setBorder(BorderFactory.createLineBorder(Color.RED));
            valid = false;
        } else {
            String[] parts = seqText.split("[,;\\s]+");
            boolean seqOk = true;
            for (String p : parts) {
                try {
                    int ch = Integer.parseInt(p.trim());
                    if (ch < 1 || ch > 15) {
                        seqOk = false;
                        break;
                    }
                } catch (NumberFormatException e) {
                    seqOk = false;
                    break;
                }
            }
            if (!seqOk || parts.length == 0) {
                sequenceField.setBorder(BorderFactory.createLineBorder(Color.RED));
                valid = false;
            }
        }

        try {
            dur = Integer.parseInt(durationField.getText().trim());
            if (dur < 1 || dur > 65535) {
                durationField.setBorder(BorderFactory.createLineBorder(Color.RED));
                valid = false;
            }
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

        if (valid && per < minCommandIntervalMs * 2) {
            periodField.setBorder(BorderFactory.createLineBorder(Color.RED));
            valid = false;
            JOptionPane.showMessageDialog(rootPanel,
                    "Период не может быть меньше " + (minCommandIntervalMs * 2)
                            + " мс (мин. интервал между командами × 2).",
                    "Ошибка", JOptionPane.WARNING_MESSAGE);
        }

        if (valid) {
            lastDuration = dur;
            lastPeriod = per;
        }

        if (!running) {
            startBtn.setEnabled(valid);
        }
    }

    private void toggleSequence() {
        if (running) {
            stopSequence();
        } else {
            startSequence();
        }
    }

    private void startSequence() {
        channelSequence.clear();
        String[] parts = sequenceField.getText().trim().split("[,;\\s]+");
        for (String p : parts) {
            try {
                int ch = Integer.parseInt(p.trim());
                if (ch >= 1 && ch <= 15) channelSequence.add(ch);
            } catch (NumberFormatException ignored) {
            }
        }
        if (channelSequence.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите хотя бы один канал, например: 1,2,3");
            return;
        }

        int duration, period;
        try {
            duration = Integer.parseInt(durationField.getText().trim());
            period = Integer.parseInt(periodField.getText().trim());
            if (duration < 1 || duration > 65535 || period < duration) {
                throw new NumberFormatException();
            }
            if (period < minCommandIntervalMs * 2) {
                JOptionPane.showMessageDialog(this,
                        "Период не может быть меньше " + (minCommandIntervalMs * 2)
                                + " мс (мин. интервал между командами × 2).",
                        "Ошибка", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Длительность 1..65535 мс, Период >= длительности");
            return;
        }

        if (cycleFuture != null) {
            cycleFuture.cancel(true);
            cycleFuture = null;
        }

        sequenceGen++;
        int gen = sequenceGen;
        running = true;
        startBtn.setText("Стоп");
        startBtn.setEnabled(true);
        sequenceStatusLabel.setText("Последовательность воспроизводиться");
        seqLamp.setLampColor(Color.GREEN);

        if (onSequenceStateChange != null) onSequenceStateChange.accept(true);

        cycleFuture = scheduler.submit(() -> runSequenceLoop(gen, duration, period));

        logger.info("Запущена последовательность: " + channelSequence + " | " + duration + "мс / " + period + "мс");
    }

    /**
     * Последовательное воспроизведение: на каждый канал отправляется команда
     * записи и ожидается ответ прибора (OK) прежде, чем перейти к следующей
     * команде. Между импульсами выдерживается заданный период. Цикл повторяется,
     * только если включён чек-бокс "Повторять последовательность".
     */
    private void runSequenceLoop(int gen, int duration, int period) {
        try {
            do {
                for (int i = 0; i < channelSequence.size(); i++) {
                    if (!running || gen != sequenceGen) return;

                    int channel = channelSequence.get(i);
                    long slotStart = System.currentTimeMillis();

                    int ackTimeout = Math.max(1000, period);
                    boolean acked = service.writeOutputSync(channel, true, duration, ackTimeout);
                    if (!acked) {
                        logger.warn("Канал " + channel + ": не получен ответ на команду записи (таймаут)");
                    }

                    long wait = period - (System.currentTimeMillis() - slotStart);
                    if (wait > 0) Thread.sleep(wait);
                }
            } while (running && gen == sequenceGen && repeatSequence.isSelected());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            SwingUtilities.invokeLater(() -> {
                if (running && gen == sequenceGen) stopSequence();
            });
        }
    }

    private void stopSequence() {
        running = false;
        if (cycleFuture != null) {
            cycleFuture.cancel(true);
            cycleFuture = null;
        }
        startBtn.setText("Старт");
        validateInputs(sequenceField);
        sequenceStatusLabel.setText("Последовательность не воспроизводиться");
        seqLamp.setLampColor(Color.RED);

        if (onSequenceStateChange != null) onSequenceStateChange.accept(false);
        logger.info("Последовательность остановлена");
    }

    @Override
    public void dispose() {
        if (running) stopSequence();
        scheduler.shutdownNow();
        super.dispose();
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
        rootPanel.setLayout(new GridLayoutManager(6, 5, new Insets(10, 10, 10, 10), 5, 5));
        final JLabel label1 = new JLabel();
        label1.setText("Последовательность каналов (через запятую):");
        rootPanel.add(label1, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sequenceField = new JTextField();
        sequenceField.setText("1,2,3,2,1,2,3");
        rootPanel.add(sequenceField, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Длительность импульса (мс):");
        rootPanel.add(label2, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        durationField = new JTextField();
        durationField.setText("100");
        rootPanel.add(durationField, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(80, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootPanel.add(spacer1, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Период следования (мс):");
        rootPanel.add(label3, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        periodField = new JTextField();
        periodField.setText("300");
        rootPanel.add(periodField, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(80, -1), null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootPanel.add(spacer2, new GridConstraints(3, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        rootPanel.add(spacer3, new GridConstraints(4, 1, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        startBtn = new JButton();
        startBtn.setText("Старт");
        rootPanel.add(startBtn, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(100, 34), null, 0, false));
        closeBtn = new JButton();
        closeBtn.setText("Закрыть");
        rootPanel.add(closeBtn, new GridConstraints(5, 4, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        statusPanel = new JPanel();
        statusPanel.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        rootPanel.add(statusPanel, new GridConstraints(5, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_VERTICAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(320, -1), new Dimension(320, -1), new Dimension(320, -1), 0, false));
        sequenceStatusLamp = new JPanel();
        sequenceStatusLamp.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        statusPanel.add(sequenceStatusLamp, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(10, 10), new Dimension(10, 10), new Dimension(10, 10), 0, false));
        sequenceStatusLabel = new JLabel();
        sequenceStatusLabel.setText("Последовательность не воспроизводиться");
        statusPanel.add(sequenceStatusLabel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        repeatSequence = new JCheckBox();
        repeatSequence.setText("Повторять последовательность");
        rootPanel.add(repeatSequence, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootPanel;
    }

}
