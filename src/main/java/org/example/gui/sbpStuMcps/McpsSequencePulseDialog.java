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
    private ScheduledFuture<?> sequenceFuture;

    private final JTextField sequenceField = new JTextField("1,2,3,2,1,2,3", 20);
    private final JTextField durationField = new JTextField("100", 6);
    private final JTextField periodField = new JTextField("300", 6);
    private final JButton startBtn = new JButton("Старт");
    private final JLabel statusLabel = new JLabel("ОСТАНОВЛЕНО", SwingConstants.CENTER);
    private final JButton closeBtn = new JButton("Закрыть");

    private volatile boolean running = false;
    private List<Integer> channelSequence = new ArrayList<>();
    private int currentIndex = 0;
    private Consumer<Boolean> onSequenceStateChange; // колбэк для блокировки главной панели

    public McpsSequencePulseDialog(Frame owner, McpsCommunicationService service, AsyncLogger logger) {
        super(owner, "Последовательная подача импульсов", true);
        this.service = service;
        this.logger = logger;

        setLayout(new BorderLayout(10, 10));
        setSize(420, 220);
        setLocationRelativeTo(owner);

        JPanel main = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        main.add(new JLabel("Последовательность каналов (через запятую):"), gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        main.add(sequenceField, gbc);

        gbc.gridwidth = 1;
        gbc.gridx = 0; gbc.gridy = 2;
        main.add(new JLabel("Длительность импульса (мс):"), gbc);
        gbc.gridx = 1; main.add(durationField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        main.add(new JLabel("Период следования (мс):"), gbc);
        gbc.gridx = 1; main.add(periodField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        startBtn.setPreferredSize(new Dimension(90, 32));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.RED);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 13f));
        statusLabel.setPreferredSize(new Dimension(120, 28));
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

        running = true;
        currentIndex = 0;
        startBtn.setText("Стоп");
        statusLabel.setText("РАБОТАЕТ");
        statusLabel.setBackground(Color.GREEN.darker());

        if (onSequenceStateChange != null) onSequenceStateChange.accept(true);

        // Первая отправка сразу
        sendNextPulse(duration);

        // Периодическая отправка следующего в последовательности
        sequenceFuture = scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            currentIndex = (currentIndex + 1) % channelSequence.size();
            sendNextPulse(duration);
        }, period, period, TimeUnit.MILLISECONDS);

        logger.info("Запущена последовательность: " + channelSequence + " | " + duration + "мс / " + period + "мс");
    }

    private void sendNextPulse(int duration) {
        if (!running || channelSequence.isEmpty()) return;
        int ch = channelSequence.get(currentIndex);
        service.writeOutput(ch, true, duration);
        logger.debug("Seq pulse -> канал " + ch);
    }

    private void stopSequence() {
        running = false;
        if (sequenceFuture != null) {
            sequenceFuture.cancel(true);
            sequenceFuture = null;
        }
        startBtn.setText("Старт");
        statusLabel.setText("ОСТАНОВЛЕНО");
        statusLabel.setBackground(Color.RED);

        // Выключаем последний активный канал
        if (!channelSequence.isEmpty()) {
            int lastCh = channelSequence.get(currentIndex);
            service.writeOutput(lastCh, false, 0);
        }

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
