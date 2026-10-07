package org.example.gui.components;

import org.example.gui.theme.ThemeManager;
import org.example.gui.theme.ThemePalette;

import javax.swing.*;
import javax.swing.plaf.basic.BasicCheckBoxUI;
import java.awt.*;

public class CustomCheckBoxUI extends BasicCheckBoxUI {

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        ThemePalette palette = ThemeManager.palette();
        c.setOpaque(true); // Делаем компонент непрозрачным
        c.setBackground(palette.trackBackground()); // Фон по умолчанию
        c.setForeground(palette.text());
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        JCheckBox cb = (JCheckBox) c;
        ThemePalette palette = ThemeManager.palette();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Заливаем весь фон компонента
        g2.setColor(cb.isEnabled() ? cb.getBackground() : palette.trackBackground());
        g2.fillRect(0, 0, cb.getWidth(), cb.getHeight());

        // Определяем позицию и размер квадратика
        int iconSize = 16;
        int iconX = 0;
        int iconY = (cb.getHeight() - iconSize) / 2;

        // Рисуем фон квадратика
        g2.setColor(cb.isEnabled() ? cb.getBackground() : palette.trackBackground());
        g2.fillRect(iconX, iconY, iconSize, iconSize);

        // Рисуем границу квадратика
        g2.setColor(palette.border());
        g2.drawRect(iconX, iconY, iconSize, iconSize);

        // Если чекбокс выбран, рисуем галочку
        if (cb.isSelected()) {
            g2.setColor(cb.isEnabled() ? palette.checkMark() : palette.disabledText());
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(iconX + 3, iconY + 8, iconX + 6, iconY + 11);
            g2.drawLine(iconX + 6, iconY + 11, iconX + 13, iconY + 4);
        }

        g2.dispose();

        // Устанавливаем цвет текста
        cb.setForeground(cb.isEnabled() ? palette.text() : palette.disabledText());

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
