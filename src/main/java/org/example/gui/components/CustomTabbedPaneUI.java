package org.example.gui.components;

import org.example.gui.theme.ThemeManager;
import org.example.gui.theme.ThemePalette;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

// Создаем кастомный UI для TabbedPane
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {

    @Override
    protected void installDefaults() {
        super.installDefaults();
        ThemePalette palette = ThemeManager.palette();
        tabPane.setBackground(palette.panelBackground());
        tabPane.setForeground(palette.text());

        // Устанавливаем кастомные цвета
        tabPane.setBackgroundAt(0, palette.panelBackground());
        tabPane.setForegroundAt(0, palette.text());

        // Настраиваем отступы
        tabInsets = new Insets(1, 3, 5, 3);
        selectedTabPadInsets = new Insets(1, 3, 7, 3);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                    int x, int y, int w, int h, boolean isSelected) {
        ThemePalette palette = ThemeManager.palette();
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(isSelected ? palette.selectedBackground() : palette.panelBackground());

        // Рисуем прямоугольник для вкладки
        g2.fillRect(x, y, w, h);

        // Рисуем границу
        g2.setColor(palette.border());
        g2.drawRect(x, y, w, h);

        g2.dispose();
    }

    @Override
    protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                 int x, int y, int w, int h, boolean isSelected) {
        // Граница уже нарисована в paintTabBackground
    }

    @Override
    protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ThemeManager.palette().border());

        int width = tabPane.getWidth();
        int height = tabPane.getHeight();
        Insets insets = tabPane.getInsets();

        int x = insets.left;
        int y = insets.top;
        int w = width - insets.right - insets.left;
        int h = height - insets.top - insets.bottom;

        g2.drawRect(x, y, w - 1, h - 1);
        g2.dispose();
    }

    @Override
    protected void paintFocusIndicator(Graphics g, int tabPlacement,
                                     Rectangle[] rects, int tabIndex,
                                     Rectangle iconRect, Rectangle textRect,
                                     boolean isSelected) {
        // Убираем стандартную индикацию фокуса
    }
}
