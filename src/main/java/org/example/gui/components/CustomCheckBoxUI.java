package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import java.awt.*;

public class CustomCheckBoxUI extends BasicCheckBoxUI {
    private static final Color DISABLED_BACKGROUND = new Color(0x1E, 0x1E, 0x1E);
    private static final Color DISABLED_FOREGROUND = new Color(0x66, 0x66, 0x66);
    private static final Color BORDER_COLOR = new Color(0x55, 0x55, 0x55);
    private static final Color CHECK_COLOR = new Color(118, 149, 110);

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        c.setOpaque(true); // Делаем компонент непрозрачным
        c.setBackground(DISABLED_BACKGROUND); // Устанавливаем черный фон по умолчанию
        c.setForeground(Color.WHITE);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JCheckBox cb = (JCheckBox) c;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Заливаем весь фон компонента
        if (cb.isEnabled()) {
            g2.setColor(cb.getBackground());
        } else {
            g2.setColor(DISABLED_BACKGROUND);
        }
        g2.fillRect(0, 0, cb.getWidth(), cb.getHeight());

        // Определяем позицию и размер квадратика
        int iconSize = 16;
        int iconX = 0;
        int iconY = (cb.getHeight() - iconSize) / 2;

        // Рисуем фон квадратика
        if (cb.isEnabled()) {
            g2.setColor(cb.getBackground());
        } else {
            g2.setColor(DISABLED_BACKGROUND);
        }
        g2.fillRect(iconX, iconY, iconSize, iconSize);

        // Рисуем границу квадратика
        g2.setColor(BORDER_COLOR);
        g2.drawRect(iconX, iconY, iconSize, iconSize);

        // Если чекбокс выбран, рисуем галочку
        if (cb.isSelected()) {
            if (cb.isEnabled()) {
                g2.setColor(CHECK_COLOR);
            } else {
                g2.setColor(DISABLED_FOREGROUND);
            }
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(iconX + 3, iconY + 8, iconX + 6, iconY + 11);
            g2.drawLine(iconX + 6, iconY + 11, iconX + 13, iconY + 4);
        }

        g2.dispose();

        // Устанавливаем цвет текста
        if (cb.isEnabled()) {
            cb.setForeground(Color.WHITE);
        } else {
            cb.setForeground(DISABLED_FOREGROUND);
        }

        // Вызываем родительский метод для отрисовки текста
        super.paint(g, c);
    }


    protected void paintFocusIndicator(Graphics g, JCheckBox cb,
                                       Rectangle bounds, Rectangle textRect, Rectangle iconRect) {
        // Убираем стандартную индикацию фокуса (синюю обводку)
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        d.width += 4; // Добавляем немного места для лучшего внешнего вида
        return d;
    }
}
