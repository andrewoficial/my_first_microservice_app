package org.example.gui.components;

import org.example.gui.theme.ThemeManager;
import org.example.gui.theme.ThemePalette;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SimpleTabbedPane extends JTabbedPane {

    public SimpleTabbedPane() {
        super();
        setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                Color border = ThemeManager.palette().border();
                highlight = border;
                lightHighlight = border;
                shadow = border;
                darkShadow = border;
                focus = ThemeManager.palette().panelBackground();

                // Увеличиваем отступы для вкладок
                tabInsets = new Insets(1, 1, 5, 1);
                selectedTabPadInsets = new Insets(1, 1, 9, 1);
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                g.setColor(ThemeManager.palette().border());
                g.drawRect(x, y, w, h);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Не рисуем границу вокруг контента
            }

            @Override
            protected int calculateTabWidth(int tabPlacement, int tabIndex, FontMetrics metrics) {
                // Добавляем дополнительное пространство для вкладок
                return super.calculateTabWidth(tabPlacement, tabIndex, metrics) + 20;
            }

            @Override
            protected int calculateTabHeight(int tabPlacement, int tabIndex, int fontHeight) {
                // Увеличиваем высоту вкладок
                return super.calculateTabHeight(tabPlacement, tabIndex, fontHeight) + 8;
            }
        });

        ThemePalette palette = ThemeManager.palette();
        setBackground(palette.panelBackground());
        setForeground(palette.text());
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
    }
}
