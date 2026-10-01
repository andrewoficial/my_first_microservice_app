package org.example.gui.devices.boto800.tcp.emulation;

import org.example.gui.devices.emulation.EmulatorCommandLog;
import org.example.gui.utilites.GuiUtilities;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Панель эмулятора BOTO-800 по TCP/IP — перехват («сниффер») протокола камеры.
 *
 * <p>Слева — настройки сервера и отладки, справа — экраны «текущая температура» /
 * «состояние», строка с найденными адресами регистров и подробный лог обмена
 * (hex + ASCII + расшифровка кадра).
 *
 * <p>Отладка выведена в консоль, терминал и файл {@code logs/Boto800-TCP_wire_*.log} —
 * см. {@link org.example.gui.devices.emulation.EmulatorTrace}. Все галочки дублируются
 * в панели, чтобы можно было включать/выключать их на лету во время перехвата.
 */
public class Boto800TcpEmulationPanel extends JPanel {

    private static final int MAX_LOG = 800;

    private final Boto800TcpEmulator emulator = new Boto800TcpEmulator();
    private final Boto800TcpServerService server = new Boto800TcpServerService(emulator);
    private final EmulatorCommandLog commandLog = new EmulatorCommandLog("Boto800-TCP");

    private final JTextField bindField = new JTextField("", 10);
    private final JSpinner portSpinner = new JSpinner(new SpinnerNumberModel(
            Boto800TcpServerService.DEFAULT_PORT, 0, 65535, 1));
    private final JSpinner idleSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 3600, 1));
    private final JSpinner learnSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JComboBox<Boto800TcpServerService.AnswerMode> modeCombo =
            new JComboBox<>(Boto800TcpServerService.AnswerMode.values());
    private final JCheckBox echoUnknownCb = new JCheckBox("Отвечать на незнакомую функцию (исключение)", true);
    private final JCheckBox autoLearnCb = new JCheckBox("Обучение: незнакомые адреса = значение ниже", true);
    private final JCheckBox terminalCb = new JCheckBox("Дублировать в терминал (System.out)", true);
    private final JCheckBox fileCb = new JCheckBox("Писать wire-файл logs/…", true);
    private final JCheckBox logAutoCb = new JCheckBox("автопрокрутка", true);

    private final JButton startBtn = new JButton("Старт");
    private final JButton stopBtn = new JButton("Стоп");
    private final JButton clearLogBtn = new JButton("Очистить лог");
    private final JButton clearMirrorBtn = new JButton("Сбросить зеркало адресов");
    private final JButton resetModelBtn = new JButton("Сбросить модель");

    private final JSpinner setpointSpinner = new JSpinner(new SpinnerNumberModel(25.0, -40.0, 400.0, 0.5));
    private final JButton applyBtn = new JButton("Применить уставку");
    private final JButton powerBtn = new JButton("ВКЛ / ВЫКЛ");

    private final JSpinner humiSpinner = new JSpinner(new SpinnerNumberModel(50.0, 0.0, 100.0, 0.5));
    private final JButton applyHumiBtn = new JButton("Применить влажность");
    private final JCheckBox humiOnCb = new JCheckBox("Поддерживать влагу (рег 18)", false);

    private final JSpinner statusWordSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 65535, 1));
    private final JSpinner learnDefault8Spinner = new JSpinner(new SpinnerNumberModel(250, 0, 65535, 10));

    private final JSpinner rampSpinner = new JSpinner(new SpinnerNumberModel(5.0, 0.1, 60.0, 0.5));
    private final JSpinner overshootSpinner = new JSpinner(new SpinnerNumberModel(2.0, 0.0, 25.0, 0.5));
    private final JSpinner fluctAmpSpinner = new JSpinner(new SpinnerNumberModel(0.3, 0.0, 10.0, 0.1));
    private final JSpinner fluctPeriodSpinner = new JSpinner(new SpinnerNumberModel(20.0, 1.0, 600.0, 1.0));

    private final JLabel measuredScreen = screenLabel();
    private final JLabel powerScreen = bigLabel(new Color(60, 150, 255));
    private final JLabel humiScreen = screenLabel();
    private final JLabel statusLabel = new JLabel("Эмулятор остановлен");
    private final JLabel clientLabel = infoLabel();
    private final JLabel modeInfo = infoLabel();
    private final JLabel setpointInfo = infoLabel();
    private final JLabel timeLabel = infoLabel();
    private final JLabel mirrorLabel = infoLabel();
    private final JLabel counterLabel = infoLabel();
    private final JTextArea logArea = new JTextArea();

    private final List<String> logLines = new CopyOnWriteArrayList<>();
    private final Timer simTimer;
    private long lastTickNanos = System.nanoTime();

    public Boto800TcpEmulationPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "B-TH-800 F · протокол «BOTO-800 Modbus» · TCP/IP (Modbus TCP) — сниффер. "
                        + "НЕ China Modbus (это B-TH-120 E: 12/100/105, ×100)",
                TitledBorder.LEFT, TitledBorder.TOP));

        add(createLeftPanel(), BorderLayout.WEST);
        add(createCenterPanel(), BorderLayout.CENTER);

        startBtn.addActionListener(e -> start());
        stopBtn.addActionListener(e -> stop());
        stopBtn.setEnabled(false);
        clearLogBtn.addActionListener(e -> clearLog());
        clearMirrorBtn.addActionListener(e -> {
            emulator.clearWritten();
            addLog("[event] зеркало адресов очищено");
        });
        resetModelBtn.addActionListener(e -> {
            emulator.reset();
            addLog("[event] модель сброшена: PV/уставка/питание сброшены, зеркало очищено");
        });
        applyBtn.addActionListener(e -> applySetpoint());
        powerBtn.addActionListener(e -> togglePower());
        applyHumiBtn.addActionListener(e -> applyHumidity());
        humiOnCb.addActionListener(e -> {
            emulator.setHumidityEnabled(humiOnCb.isSelected());
            addLog("[event] поддержка влаги (рег 18) = " + (humiOnCb.isSelected() ? 1 : 0));
        });
        statusWordSpinner.addChangeListener(e -> emulator.setStatusWord((int) intVal(statusWordSpinner)));
        learnDefault8Spinner.addChangeListener(e -> emulator.setLearnDefault((int) intVal(learnDefault8Spinner)));

        modeCombo.addActionListener(e -> {
            server.setAnswerMode((Boto800TcpServerService.AnswerMode) modeCombo.getSelectedItem());
            refreshInfo();
        });
        echoUnknownCb.addActionListener(e -> server.setAutoEchoUnknown(echoUnknownCb.isSelected()));
        autoLearnCb.addActionListener(e -> server.setAutoLearn(autoLearnCb.isSelected()));
        learnSpinner.addChangeListener(e -> server.emulator().setLearnDefault((int) intVal(learnSpinner)));
        idleSpinner.addChangeListener(e -> server.setIdleTimeoutMs((int) (intVal(idleSpinner) * 1000)));
        terminalCb.addActionListener(e -> server.trace().setTerminalEcho(terminalCb.isSelected()));
        fileCb.addActionListener(e -> server.trace().setFileEnabled(fileCb.isSelected()));

        rampSpinner.addChangeListener(e -> emulator.setRampRateDegPerMin(num(rampSpinner)));
        overshootSpinner.addChangeListener(e -> emulator.setOvershootDeg(num(overshootSpinner)));
        fluctAmpSpinner.addChangeListener(e -> emulator.setFluctuationAmpDeg(num(fluctAmpSpinner)));
        fluctPeriodSpinner.addChangeListener(e -> emulator.setFluctuationPeriodSec(num(fluctPeriodSpinner)));

        server.addLogListener(this::addLog);
        server.setCommandLog(commandLog);
        server.addConnectionListener(connected -> SwingUtilities.invokeLater(() ->
                statusLabel.setText(connected ? "Клиент подключён" : "Ожидание клиента")));
        server.addClientCountListener(n -> SwingUtilities.invokeLater(() ->
                clientLabel.setText("Клиентов: " + n)));
        server.trace().setTerminalEcho(terminalCb.isSelected()).setFileEnabled(fileCb.isSelected());

        simTimer = new Timer(100, e -> advanceSim());
        simTimer.start();
        refreshScreens();
        refreshInfo();
        addLog("[event] готов: выберите порт и нажмите «Старт», затем включите сетевой порт камеры");

        GuiUtilities.darkenInputs(this);
    }

    // ─── сервер ───────────────────────────────────────────────────────────

    private void start() {
        int port = (int) intVal(portSpinner);
        String bind = bindField.getText() == null ? "" : bindField.getText().trim();
        server.emulator().setLearnDefault((int) intVal(learnSpinner));
        if (server.start(port, bind)) {
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            refreshInfo();
        } else {
            statusLabel.setText("Ошибка запуска сервера (порт занят?)");
        }
    }

    private void stop() {
        server.stop();
        startBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        refreshInfo();
    }

    private void applySetpoint() {
        emulator.setSetpoint(num(setpointSpinner));
        addLog(String.format(Locale.US, "[event] уставка вручную → %.2f °C (в рег 10/11/60 уйдёт %d)",
                num(setpointSpinner), (int) Math.round(num(setpointSpinner) * Boto800TcpEmulator.SCALE)));
    }

    private void applyHumidity() {
        emulator.setHumiditySetpoint(num(humiSpinner));
        addLog(String.format(Locale.US, "[event] уставка влажности → %.2f %%", num(humiSpinner)));
    }

    private void togglePower() {
        emulator.setPowerOn(!emulator.isPowerOn());
        humiOnCb.setSelected(emulator.isHumidityEnabled());
        addLog("[event] режим камеры (рег 63) = " + (emulator.isPowerOn() ? "1 РАБОТА" : "0 СТОП"));
    }

    // ─── симуляция и экраны ───────────────────────────────────────────────

    private void advanceSim() {
        long now = System.nanoTime();
        double dtSec = (now - lastTickNanos) / 1_000_000_000.0;
        lastTickNanos = now;
        emulator.advance(Math.min(dtSec, 0.5));
        refreshScreens();
    }

    private void refreshScreens() {
        double set = emulator.getSetpoint();
        double meas = emulator.getMeasuredTemperature();
        measuredScreen.setText(String.format(Locale.US, "%.2f °C", meas));
        measuredScreen.setForeground(set < meas - 0.3 ? new Color(200, 90, 0)
                : set > meas + 0.3 ? new Color(0, 90, 180) : new Color(0, 140, 0));
        powerScreen.setText(emulator.isPowerOn() ? "РАБОТА" : "СТОП");
        humiScreen.setText(String.format(Locale.US, "%.1f %%", emulator.getHumidity()));
        timeLabel.setText("Время: " + java.time.LocalTime.now().withNano(0));
        setpointInfo.setText(String.format(Locale.US,
                "Уставка T: %.2f °C (рег 11) · H: %.1f %% (рег 15) · слово 8900: 0x%04X",
                set, emulator.getHumiditySetpoint(), emulator.getStatusWord()));
        mirrorLabel.setText("Адреса, куда писал прибор: " + emulator.writtenSummary());
        counterLabel.setText(String.format(Locale.US, "Обмен: RX %d B / %d пак. · TX %d B / %d пак. · wire-файл: %s",
                server.trace().rxBytes(), server.trace().rxPackets(),
                server.trace().txBytes(), server.trace().txPackets(),
                fileCb.isSelected() ? server.trace().wireFileName() : "выкл"));
    }

    private void refreshInfo() {
        String mode = server.isRunning()
                ? "слушает порт " + server.boundPort() + (bindField.getText().isBlank() ? " (0.0.0.0)" : " " + bindField.getText().trim())
                : "остановлен";
        modeInfo.setText("Сервер: " + mode);
        modeInfo.setForeground(server.isRunning() ? new Color(0, 140, 0) : Color.DARK_GRAY);
    }

    private void addLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logLines.add(line);
            while (logLines.size() > MAX_LOG) {
                logLines.remove(0);
            }
            logArea.setText(String.join("\n", logLines));
            if (logAutoCb.isSelected()) {
                logArea.setCaretPosition(logArea.getDocument().getLength());
            }
        });
    }

    private void clearLog() {
        logLines.clear();
        logArea.setText("");
    }

    // ─── UI ───────────────────────────────────────────────────────────────

    private JScrollPane createLeftPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        p.add(sectionLabel("Сервер (TCP)"));
        p.add(label("Адрес (пусто = все):"));
        p.add(bindField);
        p.add(label("Порт:"));
        p.add(portSpinner);
        p.add(label("Таймаут простоя, с (0 = нет):"));
        p.add(idleSpinner);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        btns.add(startBtn);
        btns.add(stopBtn);
        p.add(btns);

        p.add(new JLabel("Статус:"));
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(statusLabel);
        p.add(modeInfo);
        p.add(clientLabel);
        p.add(counterLabel);

        JPanel cmdRow = EmulatorCommandLog.createControls(commandLog);
        cmdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(cmdRow);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Отладка (брутфорс протокола)"));
        modeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        p.add(modeCombo);
        echoUnknownCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(echoUnknownCb);
        autoLearnCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(autoLearnCb);
        p.add(label("Значение для незнакомых адресов:"));
        p.add(learnSpinner);
        terminalCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(terminalCb);
        fileCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(fileCb);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Модель камеры (карта BOTO 800)"));
        p.add(label("Уставка температуры, °C (рег 11/60):"));
        p.add(setpointSpinner);
        JPanel modelBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        modelBtns.add(applyBtn);
        modelBtns.add(powerBtn);
        p.add(modelBtns);
        p.add(label("Уставка влажности, % (рег 15/61):"));
        p.add(humiSpinner);
        p.add(applyHumiBtn);
        humiOnCb.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(humiOnCb);
        p.add(label("Слово состояния 8900..8902 (0x%):"));
        p.add(statusWordSpinner);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Выход на режим"));
        p.add(label("Скорость выхода, °C/мин"));
        p.add(rampSpinner);
        p.add(label("Оверхед (перелёт), °C"));
        p.add(overshootSpinner);
        p.add(label("Размер флуктуации на полке, °C"));
        p.add(fluctAmpSpinner);
        p.add(label("Период флуктуации, с"));
        p.add(fluctPeriodSpinner);

        p.add(Box.createVerticalStrut(10));
        p.add(sectionLabel("Найденные регистры"));
        mirrorLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(mirrorLabel);
        JPanel mirrorBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        mirrorBtns.add(clearMirrorBtn);
        mirrorBtns.add(resetModelBtn);
        p.add(mirrorBtns);
        p.add(label("Значение для адресов вне карты (×10, напр. 250 = 25.0 °C):"));
        p.add(learnDefault8Spinner);
        p.add(label("Подтверждено по COM: 10 = T ×10, 60 = уставка, 63 = вкл/выкл"));
        p.add(label("Служебные блоки 5498/8108/8900/8964 отдаются нулями"));

        p.add(Box.createVerticalGlue());
        for (Component c : p.getComponents()) {
            if (c instanceof JComponent jc && !(c instanceof JPanel)) {
                jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, jc.getPreferredSize().height));
            }
        }
        JScrollPane scroller = new JScrollPane(p);
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroller.setBorder(null);
        scroller.setPreferredSize(new Dimension(340, 0));
        return scroller;
    }

    private JPanel createCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(8, 8));

        JPanel screens = new JPanel(new GridLayout(1, 3, 12, 0));
        screens.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        measuredScreen.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ТЕКУЩАЯ ТЕМПЕРАТУРА (рег 10)", measuredScreen));
        humiScreen.setHorizontalAlignment(SwingConstants.CENTER);
        screens.add(panelBox("ВЛАЖНОСТЬ (рег 14)", humiScreen));
        screens.add(panelBox("РЕЖИМ (рег 63)", powerScreen));

        JPanel info = new JPanel(new GridLayout(0, 1, 4, 2));
        info.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.DARK_GRAY), "ПАРАМЕТРЫ"),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        info.add(timeLabel);
        info.add(setpointInfo);

        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        top.add(screens, BorderLayout.NORTH);
        top.add(info, BorderLayout.SOUTH);
        center.add(top, BorderLayout.NORTH);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Лог перехвата (hex + ASCII + разбор кадра)"));
        logScroll.setPreferredSize(new Dimension(400, 220));

        JPanel logRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        logRow.add(logAutoCb);
        logRow.add(clearLogBtn);
        JPanel logWrap = new JPanel(new BorderLayout());
        logWrap.add(logRow, BorderLayout.NORTH);
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

    private static JLabel screenLabel() {
        JLabel l = new JLabel("-- °C", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(new Color(0, 140, 0));
        return l;
    }

    private static JLabel bigLabel(Color color) {
        JLabel l = new JLabel("--", SwingConstants.CENTER);
        l.setFont(new Font(Font.MONOSPACED, Font.BOLD, 34));
        l.setForeground(color);
        return l;
    }

    private static JLabel infoLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        l.setHorizontalAlignment(SwingConstants.LEFT);
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

    private static double num(JSpinner s) {
        return ((Number) s.getValue()).doubleValue();
    }

    private static long intVal(JSpinner s) {
        return ((Number) s.getValue()).longValue();
    }

    public void shutdown() {
        simTimer.stop();
        server.stop();
        server.trace().close();
    }
}
