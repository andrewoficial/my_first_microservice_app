package org.example.gui.sbpStuMcps;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Отдельное окно для последовательной подачи импульсов по нескольким каналам.
 * 
 * Требования:
 * - Поле ввода последовательности каналов (1,2,3,2,1,2,3)
 * - Общая длительность и период
 * - Старт/Стоп с индикатором
 * - Блокировка управления остальными каналами пока работает
 */
public class McpsSequencePulseDialog extends JDialog {

    private final McpsCommunicationService service;
    private final AsyncLogger logger;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> cycleFuture;

    private final JTextField sequenceField = new JTextField("1,2,3,2,1,2,3", 20);
    private final JTextField durationField = new JTextField("100", 6);
    private final JTextField periodField = new JTextField("300", 6);
    private final JButton startBtn = new JButton("Старт");
    private final JLabel statusLabel = new JLabel("ОСТАНОВЛЕНО", SwingConstants.CENTER);
    private final JButton closeBtn = new JButton("Закрыть");

    private volatile boolean running = false;
    private int sequenceGen = 0;
    private List<Integer> channelSequence = new ArrayList<>();
    private Consumer<Boolean> onSequenceStateChange;

    public McpsSequencePulseDialog(Frame owner, McpsCommunicationService service, AsyncLogger logger) {
        super(owner, "Последовательная подача импульсов", true);
        this.service = service;
        this.logger = logger;

        setLayout(new BorderLayout(10, 10));
        setSize(520, 240);
        setResizable(true);
        setLocationRelativeTo(owner);

        JPanel main = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Строка 0: метка последовательности
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.weightx = 1;
        main.add(new JLabel("Последовательность каналов (через запятую):"), gbc);

        // Строка 1: поле последовательности
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3; gbc.weightx = 1;
        main.add(sequenceField, gbc);

        // Строка 2: длительность + поле
        gbc.gridwidth = 1; gbc.weightx = 0;
        gbc.gridx = 0; gbc.gridy = 2;
        main.add(new JLabel("Длительность импульса (мс):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        durationField.setPreferredSize(new Dimension(80, 26));
        main.add(durationField, gbc);

        // Строка 3: период + поле
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0;
        main.add(new JLabel("Период следования (мс):"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.3;
        periodField.setPreferredSize(new Dimension(80, 26));
        main.add(periodField, gbc);

        // Растяжимый заполнитель в конце
        gbc.gridx = 2; gbc.gridy = 2; gbc.gridheight = 2; gbc.weightx = 1;
        main.add(new JPanel(), gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        startBtn.setPreferredSize(new Dimension(100, 34));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.RED);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 14f));
        statusLabel.setPreferredSize(new Dimension(130, 30));
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        btnPanel.add(startBtn);
        btnPanel.add(statusLabel);
        btnPanel.add(closeBtn);

        add(main, BorderLayout.CENTER);
        add(btnPanel, BorderLayout.SOUTH);

        startBtn.addActionListener(e -> toggleSequence());
        closeBtn.addActionListener(e -> {
            if (running) stopSequence();
            dispose();
        });

        // Валидация
        periodField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateInputs(); }
        });
        durationField.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateInputs(); }
        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void setOnSequenceStateChange(Consumer<Boolean> callback) {
        this.onSequenceStateChange = callback;
    }

    private void validateInputs() {
        try {
            int dur = Integer.parseInt(durationField.getText().trim());
            int per = Integer.parseInt(periodField.getText().trim());
            if (per < dur) {
                periodField.setBackground(new Color(255, 220, 220));
            } else {
                periodField.setBackground(Color.WHITE);
            }
        } catch (Exception ex) {
            periodField.setBackground(new Color(255, 220, 220));
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
        // Парсим последовательность
        channelSequence.clear();
        String[] parts = sequenceField.getText().trim().split("[,;\\s]+");
        for (String p : parts) {
            try {
                int ch = Integer.parseInt(p.trim());
                if (ch >= 1 && ch <= 15) channelSequence.add(ch);
            } catch (NumberFormatException ignored) {}
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
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Длительность 1..65535 мс, Период >= длительности");
            return;
        }

        // Отменяем предыдущий цикл (если был перезапуск)
        if (cycleFuture != null) {
            cycleFuture.cancel(false);
            cycleFuture = null;
        }

        sequenceGen++;
        int gen = sequenceGen;
        running = true;
        startBtn.setText("Стоп");
        statusLabel.setText("РАБОТАЕТ");
        statusLabel.setBackground(Color.GREEN.darker());

        if (onSequenceStateChange != null) onSequenceStateChange.accept(true);

        scheduleCycle(gen, duration, period, System.currentTimeMillis());

        logger.info("Запущена последовательность: " + channelSequence + " | " + duration + "мс / " + period + "мс");
    }

    /**
     * Планирует один полный цикл последовательности.
     * Каждый импульс имеет своё смещение от начала цикла: ch@0, ch@period, ch@2*period, ...
     * После завершения цикла планируется следующий рекурсивно.
     */
    private void scheduleCycle(int gen, int duration, int period, long cycleStart) {
        if (!running || gen != sequenceGen) return;

        long now = System.currentTimeMillis();
        long base = Math.max(now, cycleStart);
        int cycleCount = channelSequence.size();
        int cycleDuration = cycleCount * period;

        for (int i = 0; i < cycleCount; i++) {
            int ch = channelSequence.get(i);
            long fireAt = base + (long) i * period;
            long delay = fireAt - System.currentTimeMillis();
            if (delay < 0) delay = 0;

            final int channel = ch;
            scheduler.schedule(() -> {
                if (running && gen == sequenceGen) {
                    service.writeOutput(channel, true, duration);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        long nextCycleStart = base + cycleDuration;
        long nextDelay = nextCycleStart - System.currentTimeMillis();
        if (nextDelay < 0) nextDelay = 0;

        cycleFuture = scheduler.schedule(() -> {
            scheduleCycle(gen, duration, period, nextCycleStart);
        }, nextDelay, TimeUnit.MILLISECONDS);
    }

    private void stopSequence() {
        running = false;
        if (cycleFuture != null) {
            cycleFuture.cancel(false);
            cycleFuture = null;
        }
        startBtn.setText("Старт");
        statusLabel.setText("ОСТАНОВЛЕНО");
        statusLabel.setBackground(Color.RED);

        if (onSequenceStateChange != null) onSequenceStateChange.accept(false);
        logger.info("Последовательность остановлена");
    }

    @Override
    public void dispose() {
        if (running) stopSequence();
        scheduler.shutdownNow();
        super.dispose();
    }
}
