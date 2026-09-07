package org.example.gui.devices.testa.emulation;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import org.example.device.ethernet.testa.TestaCommands;
import org.example.gui.utilites.GuiUtilities;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора климатической камеры Testa (UDP-сервер на порту 1300).
 * Модель (температура к уставке) тикает таймером; статусные датаграммы рассылаются сервером.
 */
public class TestaEmulationPanel extends JPanel {

    private static final Color GREEN = new Color(0, 160, 0);
    private static final Color BLUE = new Color(60, 150, 255);
    private static final Color GRAY = Color.DARK_GRAY;

    private final TestaEmulator emulator = new TestaEmulator();
    private final TestaServerService service = new TestaServerService(emulator);

    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(1300, 1, 65535, 1));
    private final JButton startBtn = new JButton("Запустить");
    private final JButton stopBtn = new JButton("Остановить");
    private final JLabel statusLabel = new JLabel("Эмулятор остановлен");

    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 120.0, 0.5));
    private final JSpinner rampSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 60.0, 0.5));
    private final JTextField rawBytesField = new JTextField(40);
    private final JButton applyRawBtn = new JButton("Применить");
    private final JLabel rawError = new JLabel(" ");
    private final JLabel tempScreen = screenLabel();
    private final JLabel humBig = bigLabel(BLUE);
    private final JLabel timeLabel = infoLabel();
    private final JCheckBox logAuto = new JCheckBox("автопрокрутка лога", true);
    private final JLabel modeScreen = infoLabel();
    private final JLabel setpointScreen = infoLabel();
    private final JLabel humScreen = infoLabel();

    private static final int[] KNOWN_OFFSETS = {
            8, 14, 16, 26, 28, 32, 34, 36, 38
    };
    private static final String[] KNOWN_LABELS = {
            "Управление температурой, %",
            "Влажность заданная, %RH",
            "Управление влажностью, %",
            "Темп. парогенератора, °C",
            "Датчик 22 — целевое значение",
            "Датчик 404 — текущее",
            "Датчик 23 — текущее",
            "Датчик 404 — целевое",
            "Датчик 23 целевое, атм"
    };
    private static final double[] KNOWN_DEFAULTS = {
            0.0, 77.0, 77.0, 77.0, 77.0, 0.0, 77.0, 77.0, 0.0
    };

    private static final String[] RELAY404_BITS = {
            "Вентиляция",
            "Подогрев двери",
            "Подогрев стекла",
            "Работа со влагой",
            "Соленоид 404 малый",
            "Компрессор 404",
            "Соленоид 404 основной",
            "Соленоид 404 перепуск"
    };
    private static final String[] RELAY23_BITS = {
            "Осушитель",
            "Соленоид 404 на ТО",
            "Компрессор 23",
            "Соленоид 23 основной",
            "Соленоид 23 перепуск",
            "Подогрев 23",
            "(бит 6)",
            "Подсветка камеры"
    };
    private static final String[] ALARM_BITS = {
            "Мало воды в парогенераторе",
            "(бит 1)",
            "(бит 2)",
            "(бит 3)",
            "(бит 4)",
            "(бит 5)",
            "(бит 6)",
            "(бит 7)"
    };

    /** Байты 8..39, не занятые известными short-полями (остаются для ручной отладки). */
    private static final int[] UNKNOWN_OFFSETS;
    static {
        java.util.Set<Integer> known = new java.util.HashSet<>();
        for (int o : KNOWN_OFFSETS) {
            known.add(o);
            known.add(o + 1);
        }
        known.add(20);
        known.add(21);
        known.add(22);
        java.util.List<Integer> unk = new ArrayList<>();
        for (int o = 8; o <= 39; o++) {
            if (!known.contains(o)) {
                unk.add(o);
            }
        }
        UNKNOWN_OFFSETS = unk.stream().mapToInt(Integer::intValue).toArray();
    }

    private final byte[] workRaw = new byte[32];
    private final byte[] manualFrame = new byte[40];
    private final List<JSpinner> knownSpinners = new ArrayList<>();
    private final List<JCheckBox[]> relayGroups = new ArrayList<>();
    private static final int[] ALL_OFFSETS;
    private static final int[] FRAME_OFFSETS;
    static {
        ALL_OFFSETS = new int[32];
        for (int i = 0; i < ALL_OFFSETS.length; i++) {
            ALL_OFFSETS[i] = 8 + i;
        }
        FRAME_OFFSETS = new int[40];
        for (int i = 0; i < FRAME_OFFSETS.length; i++) {
            FRAME_OFFSETS[i] = i;
        }
    }

    private final JCheckBox manualCheck = new JCheckBox("Ручной кадр: слать весь кадр (0..39) как есть, без перезаписи");
    private final JSpinner stateWord = new JSpinner(new SpinnerNumberModel(0, -32768, 65535, 1));
    private final JComboBox<Integer> debugOff = new JComboBox<>(intArrayToBox(UNKNOWN_OFFSETS));
    private final JCheckBox debugAll = new JCheckBox("показывать все адреса (вкл. известные)");
    private final JCheckBox[] debugBits = new JCheckBox[8];
    private final JSpinner debugValue = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
    private final JLabel debugHex = new JLabel(" ");
    private final JButton debugZeros = new JButton("все биты=0 и задать");
    private final JButton debugOnes = new JButton("все биты=1 и задать");
    private final JButton debugPrev = new JButton("< предыдущий адрес");
    private final JButton debugNext = new JButton("следующий адрес >");
    private boolean syncingDebug = false;

    private final JSpinner sniffStart = new JSpinner(new SpinnerNumberModel(1, 1, 65535, 1));
    private final JSpinner sniffCount = new JSpinner(new SpinnerNumberModel(3000, 1, 65535, 100));
    private final JButton sniffStartBtn = new JButton("Слушать");
    private final JButton sniffStopBtn = new JButton("Стоп");
    private final JLabel sniffStatus = new JLabel("Сниффер остановлен");
    private TestaUdpSniffer sniffer;
    private final JTextArea logArea = new JTextArea();
    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private static final int MAX_LOG = 300;

    private final javax.swing.Timer simTimer;
    private long lastNanos = System.nanoTime();

    public TestaEmulationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Эмулятор Testa (UDP :1300, статус → :1200)",
                TitledBorder.LEFT, TitledBorder.TOP));

        JScrollPane leftScroll = new JScrollPane(buildLeft());
        leftScroll.setBorder(BorderFactory.createEmptyBorder());
        leftScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        add(leftScroll, BorderLayout.WEST);
        add(buildCenter(), BorderLayout.CENTER);

        stopBtn.setEnabled(false);
        setpointSpinner.addChangeListener(e -> emulator.setSetpoint(((Number) setpointSpinner.getValue()).doubleValue()));
        rampSpinner.addChangeListener(e -> emulator.setRampRateDegPerMin(((Number) rampSpinner.getValue()).doubleValue()));
        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());

        rawBytesField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        rawBytesField.setText(defaultRawBytesHex());
        emulator.setRawStatusBytes(parseHex(defaultRawBytesHex()));
        rawBytesField.addActionListener(e -> applyRawBytes());
        applyRawBtn.addActionListener(e -> applyRawBytes());
        applyKnownFields();
        loadDebugByte();

        sniffStopBtn.setEnabled(false);
        sniffStartBtn.addActionListener(e -> startSniff());
        sniffStopBtn.addActionListener(e -> sniffStop());

        emulator.setSetpoint(((Number) setpointSpinner.getValue()).doubleValue());
        emulator.setRampRateDegPerMin(((Number) rampSpinner.getValue()).doubleValue());

        service.addLogListener(line -> SwingUtilities.invokeLater(() -> addLog(line)));
        service.addRunningListener(r -> SwingUtilities.invokeLater(() ->
                statusLabel.setText(r ? "Эмулятор работает (UDP :" + portSpinner.getValue() + ")" : "Эмулятор остановлен")));

        simTimer = new javax.swing.Timer(100, e -> advanceSim());
        simTimer.start();

        GuiUtilities.darkenInputs(this);
    }

    private void start() {
        int port = ((Number) portSpinner.getValue()).intValue();
        startBtn.setEnabled(false);
        if (service.start(port)) {
            stopBtn.setEnabled(true);
        } else {
            startBtn.setEnabled(true);
        }
    }

    private void stop() {
        service.stop();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
    }

    private void advanceSim() {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        emulator.advance(Math.min(dt, 0.5));
        tempScreen.setText(String.format(Locale.US, "%.2f °C", emulator.getActual()));
        modeScreen.setText("Режим: " + (emulator.isRunning() ? "Работа" : "Остановлен"));
        setpointScreen.setText(String.format(Locale.US, "Уставка: %.2f °C", emulator.getSetpoint()));
        humScreen.setText(String.format(Locale.US, "Влажность: %.1f%% / уставка %.1f%%",
                emulator.getHumidityCurrent(), emulator.getHumiditySet()));
        humBig.setText(String.format(Locale.US, "%.1f %%", emulator.getHumidityCurrent()));
        timeLabel.setText("Время: " + java.time.LocalTime.now().withNano(0).toString());
    }

    private JPanel buildLeft() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        p.setPreferredSize(new Dimension(660, 0));

        p.add(label("Порт приёма SetT"));
        p.add(fullWidth(portSpinner));
        p.add(Box.createVerticalStrut(4));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.setAlignmentX(Component.LEFT_ALIGNMENT);
        btns.add(startBtn);
        btns.add(stopBtn);
        p.add(btns);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);

        p.add(Box.createVerticalStrut(12));
        p.add(label("Уставка, °C"));
        p.add(fullWidth(setpointSpinner));
        p.add(Box.createVerticalStrut(4));
        p.add(label("Скорость выхода, °C/мин"));
        p.add(fullWidth(rampSpinner));

        p.add(Box.createVerticalStrut(12));
        p.add(buildKnownFields());

        p.add(Box.createVerticalStrut(4));
        p.add(buildStateWord());

        p.add(Box.createVerticalStrut(12));
        p.add(buildRelays());

        p.add(Box.createVerticalStrut(6));
        p.add(buildAlarms());

        p.add(Box.createVerticalStrut(12));
        p.add(buildDebugEditor());

        p.add(Box.createVerticalStrut(12));
        p.add(label("Сырые байты статуса 8..39 (hex)"));
        p.add(fullWidth(rawBytesField));
        JPanel rawRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        rawRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        rawRow.add(applyRawBtn);
        rawRow.add(rawError);
        p.add(rawRow);

        p.add(Box.createVerticalStrut(12));
        p.add(buildSniffer());

        p.add(Box.createVerticalGlue());
        return p;
    }

    private JPanel buildSniffer() {
        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Сниффер UDP — поиск порта команд (кнопок)",
                TitledBorder.LEFT, TitledBorder.TOP));

        sniffStart.setPreferredSize(new Dimension(70, sniffStart.getPreferredSize().height));
        sniffCount.setPreferredSize(new Dimension(70, sniffCount.getPreferredSize().height));
        sniffCount.setToolTipText("Сколько портов подряд слушать");

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new JLabel("с порта"));
        row.add(sniffStart);
        row.add(new JLabel("кол-во"));
        row.add(sniffCount);
        row.add(sniffStartBtn);
        row.add(sniffStopBtn);
        g.add(row);

        sniffStatus.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.add(sniffStatus);
        g.add(new JLabel("Лог внизу. Лучше остановить эмулятор и слушать диапазон."));
        return g;
    }

    private void startSniff() {
        int start = ((Number) sniffStart.getValue()).intValue();
        int count = ((Number) sniffCount.getValue()).intValue();
        sniffStop();
        sniffer = new TestaUdpSniffer(start, count, line ->
                SwingUtilities.invokeLater(() -> addLog("SNIFF: " + line)));
        if (sniffer.start()) {
            sniffStartBtn.setEnabled(false);
            sniffStopBtn.setEnabled(true);
            sniffStatus.setText("Слушаю UDP " + start + ".." + (start + count - 1));
        }
    }

    private void sniffStop() {
        if (sniffer != null) {
            sniffer.stop();
            sniffer = null;
        }
        sniffStartBtn.setEnabled(true);
        sniffStopBtn.setEnabled(false);
        sniffStatus.setText("Сниффер остановлен");
    }

    private JPanel buildKnownFields() {
        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Известные поля статуса (short LE /100)",
                TitledBorder.LEFT, TitledBorder.TOP));

        JPanel grid = new JPanel(new GridLayout(0, 2, 10, 4));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int i = 0; i < KNOWN_OFFSETS.length; i++) {
            JSpinner sp = new JSpinner(new SpinnerNumberModel(
                    KNOWN_DEFAULTS[i], -320.0, 320.0, 0.5));
            sp.addChangeListener(e -> applyKnownFields());
            knownSpinners.add(sp);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(new JLabel(String.format("б%d", KNOWN_OFFSETS[i])));
            sp.setPreferredSize(new Dimension(64, sp.getPreferredSize().height));
            row.add(sp);
            JLabel name = new JLabel(KNOWN_LABELS[i]);
            name.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            row.add(name);
            grid.add(row);
        }
        g.add(grid);
        return g;
    }

    /** Сырое слово байт 24..25 (не /100) — неизвестный канал, для ручных опытов. */
    private JPanel buildStateWord() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(new JLabel("Слово состояния (сырое int16, байты 24-25):"));
        stateWord.setPreferredSize(new Dimension(80, stateWord.getPreferredSize().height));
        stateWord.addChangeListener(e -> applyStateWord());
        row.add(stateWord);
        row.setToolTipText("Штатка это поле не читает (режим у неё локальный). Пробный канал.");
        return row;
    }

    /** Пишет слово 24..25 сырым int16 LE (без /100). */
    private void applyStateWord() {
        int v = ((Number) stateWord.getValue()).intValue() & 0xFFFF;
        workRaw[16] = (byte) (v & 0xFF);
        workRaw[17] = (byte) ((v >>> 8) & 0xFF);
        pushWorkRaw();
    }

    /** Именованные битовые выходы (реле) в байтах 20 и 21 статусного кадра. */
    private JPanel buildRelays() {
        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Выходы / реле",
                TitledBorder.LEFT, TitledBorder.TOP));
        g.add(buildRelayByte(20, RELAY404_BITS, "Контур 404 (байт 20)"));
        g.add(Box.createVerticalStrut(6));
        g.add(buildRelayByte(21, RELAY23_BITS, "Контур 23 (байт 21)"));
        return g;
    }

    /** Аварии / ошибки — битовые флаги в байте 22. */
    private JPanel buildAlarms() {
        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Аварии / ошибки (байт 22)",
                TitledBorder.LEFT, TitledBorder.TOP));
        g.add(buildRelayByte(22, ALARM_BITS, "Биты 22"));
        return g;
    }

    /** Набор чекбоксов по битам одного байта; каждый сразу пишет бит в рабочий буфер. */
    private JPanel buildRelayByte(int byteOffset, String[] names, String title) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title,
                        TitledBorder.LEFT, TitledBorder.TOP),
                BorderFactory.createEmptyBorder(2, 6, 4, 6)));

        JCheckBox[] boxes = new JCheckBox[8];
        JPanel grid = new JPanel(new GridLayout(0, 4, 6, 2));
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int bit = 0; bit < 8; bit++) {
            final int b = bit;
            JCheckBox cb = new JCheckBox(names[bit]);
            cb.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
            cb.addActionListener(e -> applyRelayByte(byteOffset, boxes));
            boxes[bit] = cb;
            grid.add(cb);
        }
        group.add(grid);
        relayGroups.add(boxes);
        applyRelayByte(byteOffset, boxes);
        return group;
    }

    private void applyRelayByte(int byteOffset, JCheckBox[] boxes) {
        int v = 0;
        for (int bit = 0; bit < 8; bit++) {
            if (boxes[bit].isSelected()) {
                v |= 1 << bit;
            }
        }
        workRaw[byteOffset - 8] = (byte) v;
        pushWorkRaw();
    }

    /**
     * Записывает известные short-поля (LE /100) в общий рабочий буфер {@link #workRaw},
     * не трогая остальные (неизвестные) байты. Затем рассылает буфер.
     */
    private void applyKnownFields() {
        for (int i = 0; i < knownSpinners.size(); i++) {
            double v = ((Number) knownSpinners.get(i).getValue()).doubleValue();
            short sv = (short) Math.round(v * 100);
            int start = KNOWN_OFFSETS[i] - 8;
            workRaw[start] = (byte) (sv & 0xFF);
            workRaw[start + 1] = (byte) ((sv >>> 8) & 0xFF);
            if (KNOWN_OFFSETS[i] == 14) {
                emulator.setHumiditySetpoint(v);
            }
        }
        pushWorkRaw();
    }

    /** Отправляет текущий рабочий буфер в эмулятор и обновляет hex-поле (с учётом ручного кадра). */
    private void pushWorkRaw() {
        if (manualOn()) {
            System.arraycopy(workRaw, 0, manualFrame, 8, 32);
            emulator.setManualFrame(manualFrame);
        } else {
            emulator.setRawStatusBytes(workRaw.clone());
        }
        rawBytesField.setText(manualOn() ? toHex(manualFrame) : toHex(workRaw));
    }

    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format("%02X", b & 0xFF));
        }
        return sb.toString();
    }

    private static Integer[] intArrayToBox(int[] a) {
        Integer[] boxed = new Integer[a.length];
        for (int i = 0; i < a.length; i++) {
            boxed[i] = a[i];
        }
        return boxed;
    }

    /** Редактор отдельных байт для отладки неизвестных позиций: биты (8 чекбоксов) + число. */
    private JPanel buildDebugEditor() {
        JPanel g = new JPanel();
        g.setLayout(new BoxLayout(g, BoxLayout.Y_AXIS));
        g.setAlignmentX(Component.LEFT_ALIGNMENT);
        g.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Отладка байт (биты/байт)",
                TitledBorder.LEFT, TitledBorder.TOP));

        manualCheck.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        manualCheck.addActionListener(e -> toggleManual());
        g.add(manualCheck);

        debugAll.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        debugAll.addActionListener(e -> refreshDebugOffsets());
        g.add(debugAll);

        JPanel offRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
        offRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        offRow.add(new JLabel("байт"));
        debugOff.setAlignmentX(Component.LEFT_ALIGNMENT);
        debugOff.addActionListener(e -> loadDebugByte());
        offRow.add(debugOff);
        g.add(offRow);

        JPanel bitsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 1));
        bitsRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel bitsHint = new JLabel("7  6  5  4  3  2  1  0");
        bitsHint.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        JPanel bitsBox = new JPanel();
        bitsBox.setLayout(new BoxLayout(bitsBox, BoxLayout.Y_AXIS));
        bitsBox.add(bitsHint);
        bitsBox.add(bitsRow);
        g.add(bitsBox);

        for (int b = 0; b < 8; b++) {
            final int bit = 7 - b;
            JCheckBox cb = new JCheckBox();
            cb.setSelected(false);
            cb.addActionListener(e -> applyDebugByte());
            debugBits[b] = cb;
            bitsRow.add(cb);
        }

        JPanel valRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 1));
        valRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        valRow.add(new JLabel("значение"));
        debugValue.setPreferredSize(new Dimension(60, debugValue.getPreferredSize().height));
        debugValue.addChangeListener(e -> applyDebugValue());
        valRow.add(debugValue);
        debugHex.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        valRow.add(debugHex);
        g.add(valRow);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        btnRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        debugZeros.addActionListener(e -> setAllDebugBits(0x00));
        debugOnes.addActionListener(e -> setAllDebugBits(0xFF));
        debugPrev.addActionListener(e -> stepDebug(-1));
        debugNext.addActionListener(e -> stepDebug(+1));
        btnRow.add(debugPrev);
        btnRow.add(debugZeros);
        btnRow.add(debugOnes);
        btnRow.add(debugNext);
        g.add(btnRow);
        return g;
    }

    /** Переход к предыдущему/следующему адресу в списке смещений. */
    private void stepDebug(int dir) {
        int idx = debugOff.getSelectedIndex();
        int size = debugOff.getItemCount();
        int next = Math.max(0, Math.min(size - 1, idx + dir));
        if (next != idx) {
            debugOff.setSelectedIndex(next);
        }
        loadDebugByte();
    }

    /** Перестраивает список смещений. Ручной кадр: весь 0..39; иначе неизвестные или все 8..39. */
    private void refreshDebugOffsets() {
        Integer prev = (Integer) debugOff.getSelectedItem();
        int[] src;
        if (manualOn()) {
            src = FRAME_OFFSETS;
        } else {
            src = debugAll.isSelected() ? ALL_OFFSETS : UNKNOWN_OFFSETS;
        }
        debugOff.removeAllItems();
        for (int o : src) {
            debugOff.addItem(o);
        }
        if (prev != null && (manualOn() || prev >= 8)) {
            debugOff.setSelectedItem(prev);
        }
        loadDebugByte();
    }

    private boolean manualOn() {
        return manualCheck.isSelected();
    }

    /** Включение/выключение ручного кадра. */
    private void toggleManual() {
        if (manualOn()) {
            System.arraycopy(TestaCommands.buildStatusDatagram(
                    emulator.getActual(), emulator.getSetpoint(), emulator.getRawStatusBytes()),
                    0, manualFrame, 0, 40);
            emulator.setManualFrameEnabled(true);
            emulator.setManualFrame(manualFrame);
        } else {
            emulator.setManualFrameEnabled(false);
        }
        refreshDebugOffsets();
        refreshHexField();
    }

    /** Возвращает байт для отладчика: при ручном кадре — весь 0..39, иначе — из 8..39. */
    private int readEditByte(int offset) {
        if (manualOn()) {
            return manualFrame[offset] & 0xFF;
        }
        return workRaw[offset - 8] & 0xFF;
    }

    /** Пишет байт отладчиком и отправляет актуальный кадр. */
    private void writeEditByte(int offset, int v) {
        if (manualOn()) {
            manualFrame[offset] = (byte) v;
            emulator.setManualFrame(manualFrame);
        } else {
            workRaw[offset - 8] = (byte) v;
            emulator.setRawStatusBytes(workRaw.clone());
        }
        refreshHexField();
    }

    private void refreshHexField() {
        rawBytesField.setText(manualOn() ? toHex(manualFrame) : toHex(workRaw));
    }

    private void setAllDebugBits(int v) {
        syncingDebug = true;
        try {
            debugValue.setValue(v & 0xFF);
        } catch (IllegalArgumentException ignored) {
        }
        for (int b = 0; b < 8; b++) {
            debugBits[b].setSelected((v & (1 << (7 - b))) != 0);
        }
        syncingDebug = false;
        writeDebugByte(v & 0xFF);
    }

    private void loadDebugByte() {
        Object sel = debugOff.getSelectedItem();
        if (sel == null) {
            return;
        }
        int offset = (Integer) sel;
        int v = readEditByte(offset);
        syncingDebug = true;
        try {
            debugValue.setValue(v);
        } catch (IllegalArgumentException ignored) {
        }
        for (int b = 0; b < 8; b++) {
            debugBits[b].setSelected((v & (1 << (7 - b))) != 0);
        }
        syncingDebug = false;
        updateDebugHex(v);
    }

    private void applyDebugValue() {
        if (syncingDebug) {
            return;
        }
        int v = ((Number) debugValue.getValue()).intValue() & 0xFF;
        syncingDebug = true;
        try {
            for (int b = 0; b < 8; b++) {
                debugBits[b].setSelected((v & (1 << (7 - b))) != 0);
            }
        } finally {
            syncingDebug = false;
        }
        writeDebugByte(v);
    }

    private void applyDebugByte() {
        if (syncingDebug) {
            return;
        }
        int v = 0;
        for (int b = 0; b < 8; b++) {
            if (debugBits[b].isSelected()) {
                v |= 1 << (7 - b);
            }
        }
        syncingDebug = true;
        try {
            debugValue.setValue(v);
        } catch (IllegalArgumentException ignored) {
        }
        syncingDebug = false;
        writeDebugByte(v);
    }

    private void writeDebugByte(int v) {
        Object sel = debugOff.getSelectedItem();
        if (sel == null) {
            return;
        }
        int offset = (Integer) sel;
        writeEditByte(offset, v);
        updateDebugHex(v);
    }

    private void updateDebugHex(int v) {
        debugHex.setText("= 0x" + String.format("%02X", v & 0xFF));
    }

    private void applyRawBytes() {
        try {
            byte[] parsed = parseHex(rawBytesField.getText().trim());
            if (manualOn()) {
                if (parsed.length > 40) {
                    throw new IllegalArgumentException("не более 80 hex-цифр (40 байт)");
                }
                for (int i = 0; i < 40; i++) {
                    manualFrame[i] = (i < parsed.length) ? parsed[i] : 0;
                }
                emulator.setManualFrame(manualFrame);
                refreshHexField();
            } else {
                if (parsed.length > 32) {
                    throw new IllegalArgumentException("не более 64 hex-цифр (32 байта)");
                }
                System.arraycopy(parsed, 0, workRaw, 0, parsed.length);
                if (parsed.length < 32) {
                    for (int i = parsed.length; i < 32; i++) {
                        workRaw[i] = 0;
                    }
                }
                pushWorkRaw();
            }
            rawError.setForeground(GREEN);
            rawError.setText("OK (" + parsed.length + " байт)");
            loadDebugByte();
        } catch (IllegalArgumentException ex) {
            rawError.setForeground(new Color(0xB0, 0x00, 0x00));
            rawError.setText(ex.getMessage());
        }
    }

    private static byte[] parseHex(String s) {
        s = s.replaceAll("[\\s,;_]", "");
        if (s.isEmpty()) {
            return new byte[0];
        }
        if (s.length() % 2 != 0 || !s.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("только hex-цифры (0-9A-F)");
        }
        byte[] out = new byte[s.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(s.substring(i * 2, i * 2 + 2), 16);
        }
        return out;
    }

    private static String defaultRawBytesHex() {
        StringBuilder sb = new StringBuilder();
        for (int v = 1; v <= 32; v++) {
            sb.append(String.format("%02X", v));
        }
        return sb.toString();
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        JPanel screens = new JPanel(new GridLayout(1, 2, 12, 0));
        tempScreen.setHorizontalAlignment(SwingConstants.CENTER);
        humBig.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА", tempScreen));
        screens.add(panelBox("ТЕКУЩАЯ ВЛАЖНОСТЬ", humBig));
        top.add(screens, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(0, 1, 4, 2));
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "ПАРАМЕТРЫ"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        info.add(timeLabel);
        info.add(modeScreen);
        info.add(setpointScreen);
        info.add(humScreen);
        top.add(info, BorderLayout.CENTER);

        center.add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог обмена данными (hex TX/RX)"));
        logScroll.setPreferredSize(new Dimension(400, 180));

        JPanel logAutoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        logAutoRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        logAutoRow.add(logAuto);
        JPanel logWrap = new JPanel(new BorderLayout());
        logWrap.add(logAutoRow, BorderLayout.NORTH);
        logWrap.add(logScroll, BorderLayout.CENTER);
        center.add(logWrap, BorderLayout.CENTER);
        return center;
    }

    private static JPanel panelBox(String title, JLabel value) {
        JPanel box = new JPanel(new BorderLayout());
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), title),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)));
        box.add(value, BorderLayout.CENTER);
        return box;
    }

    private void addLog(String line) {
        logLines.add(line);
        while (logLines.size() > MAX_LOG) {
            logLines.remove(0);
        }
        logArea.setText(String.join("\n", logLines));
        if (logAuto.isSelected()) {
            logArea.setCaretPosition(logArea.getDocument().getLength());
        }
    }

    public void shutdown() {
        simTimer.stop();
        service.stop();
        sniffStop();
    }

    private static JLabel screenLabel() {
        return bigLabel(GREEN);
    }

    private static JLabel bigLabel(Color color) {
        JLabel l = new JLabel("--", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(color);
        return l;
    }

    private static JLabel infoLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        l.setHorizontalAlignment(SwingConstants.LEFT);
        return l;
    }

    private static JLabel label(String t) {
        JLabel l = new JLabel(t);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private static JComponent fullWidth(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
        return c;
    }
}
