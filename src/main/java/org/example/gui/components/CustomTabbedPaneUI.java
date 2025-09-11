package org.example.gui.components;

import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

// Создаем кастомный UI для TabbedPane
public class CustomTabbedPaneUI extends BasicTabbedPaneUI {
    private static final Color TAB_BACKGROUND = new Color(0x2D, 0x2D, 0x2D);
    private static final Color SELECTED_TAB_BACKGROUND = new Color(118, 149, 110);
    private static final Color TAB_FOREGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(0x55, 0x55, 0x55);

    @Override
    protected void installDefaults() {
        super.installDefaults();
        tabPane.setBackground(TAB_BACKGROUND);
        tabPane.setForeground(TAB_FOREGROUND);
        
        // Устанавливаем кастомные цвета
        tabPane.setBackgroundAt(0, TAB_BACKGROUND);
        tabPane.setForegroundAt(0, TAB_FOREGROUND);
        
        // Настраиваем отступы
        tabInsets = new Insets(5, 10, 5, 10);
        selectedTabPadInsets = new Insets(3, 8, 3, 8);
    }

    @Override
    protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex, 
                                    int x, int y, int w, int h, boolean isSelected) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        if (isSelected) {
            g2.setColor(SELECTED_TAB_BACKGROUND);
        } else {
            g2.setColor(TAB_BACKGROUND);
        }
        
        // Рисуем прямоугольник для вкладки
        g2.fillRect(x, y, w, h);
        
        // Рисуем границу
        g2.setColor(BORDER_COLOR);
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
        g2.setColor(BORDER_COLOR);
        
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