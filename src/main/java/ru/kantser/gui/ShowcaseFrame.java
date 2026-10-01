package ru.kantser.gui;

import ru.kantser.gui.buttons.Buttons;
import ru.kantser.gui.buttons.SkewButton;
import ru.kantser.gui.gauges.DefaultGaugeModel;
import ru.kantser.gui.gauges.Gauges;
import ru.kantser.gui.gauges.RoundGauge;
import ru.kantser.gui.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * Отдельное окно-витрина библиотеки {@code ru.kantser.gui}.
 *
 * <p>Показывает все виджеты со всеми вариантами параметров: несколько
 * спидометров с разными диапазонами (включая «живой», значение которого
 * меняет таймер) и кнопки со всеми тремя направлениями скоса.
 */
public class ShowcaseFrame extends JFrame {

    private final DefaultGaugeModel liveModel =
            new DefaultGaugeModel("Live Signal", "V", 0, 100, 0);
    private final JLabel liveValue = new JLabel();

    public ShowcaseFrame() {
        super("ru.kantser.gui — витрина виджетов");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(buildContent());
        setSize(980, 640);
        setLocationRelativeTo(null);
        startLiveTimer();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(Theme.BG);
        root.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));

        JLabel title = new JLabel("ru.kantser.gui · Theme / Gauges / Buttons");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ORANGE);
        root.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(buildGauges());
        center.add(Box.createVerticalStrut(18));
        center.add(buildButtons());
        center.add(Box.createVerticalStrut(18));
        center.add(buildCaption());
        root.add(center, BorderLayout.CENTER);

        return root;
    }

    private JPanel buildGauges() {
        JPanel row = new JPanel(new GridLayout(1, 4, 12, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        RoundGauge voltage = Gauges.round("Voltage Setting", "V", 0, 30, 24);
        RoundGauge current = Gauges.round("Current Setting", "A", 0, 5, 3.2);
        RoundGauge power = Gauges.round("Output Power", "W", 0, 1000, 480);
        RoundGauge live = Gauges.round(liveModel);

        voltage.setPreferredSize(new Dimension(200, 240));
        current.setPreferredSize(new Dimension(200, 240));
        power.setPreferredSize(new Dimension(200, 240));
        live.setPreferredSize(new Dimension(200, 240));

        row.add(voltage);
        row.add(current);
        row.add(power);
        row.add(live);
        return row;
    }

    private JPanel buildButtons() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel caption = new JLabel("SkewButton: LEFT / RIGHT / NONE (+ disabled)");
        caption.setFont(Theme.FONT_SUBTITLE);
        caption.setForeground(Theme.TEXT);
        caption.setAlignmentX(Component.LEFT_ALIGNMENT);
        column.add(caption);
        column.add(Box.createVerticalStrut(8));

        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        SkewButton left = Buttons.skewLeft("Skew LEFT");
        SkewButton right = Buttons.skewRight("Skew RIGHT");
        SkewButton none = Buttons.plain("Plain");
        SkewButton custom = Buttons.skew("Custom", SkewButton.Skew.LEFT);
        SkewButton disabled = Buttons.skewRight("Disabled");
        disabled.setEnabled(false);

        left.addActionListener(e -> liveValue.setText("Нажата кнопка: Skew LEFT"));
        right.addActionListener(e -> liveValue.setText("Нажата кнопка: Skew RIGHT"));
        none.addActionListener(e -> liveValue.setText("Нажата кнопка: Plain"));
        custom.addActionListener(e -> liveValue.setText("Нажата кнопка: Custom"));

        row.add(left);
        row.add(Box.createHorizontalStrut(10));
        row.add(right);
        row.add(Box.createHorizontalStrut(10));
        row.add(none);
        row.add(Box.createHorizontalStrut(10));
        row.add(custom);
        row.add(Box.createHorizontalStrut(10));
        row.add(disabled);
        column.add(row);
        return column;
    }

    private JPanel buildCaption() {
        JPanel row = new JPanel();
        row.setOpaque(false);
        row.setLayout(new BoxLayout(row, BoxLayout.X_AXIS));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        liveValue.setFont(Theme.FONT_BODY.deriveFont(Font.PLAIN));
        liveValue.setForeground(Theme.TEXT_DIM);
        liveValue.setText("Живой спидометр обновляется таймером…");
        row.add(liveValue);
        return row;
    }

    private void startLiveTimer() {
        final double[] t = {0};
        Timer timer = new Timer(120, e -> {
            t[0] += 0.12;
            double value = 50 + 45 * Math.sin(t[0]);
            liveModel.setValue(value);
        });
        timer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ShowcaseFrame().setVisible(true));
    }
}
