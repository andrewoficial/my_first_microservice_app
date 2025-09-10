package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SimpleTabbedPane extends JTabbedPane {
    private final Color normalColor = new Color(249, 249, 249);
    private final Color selectedColor = new Color(240, 240, 240);
    private final Color borderColor = new Color(200, 200, 200);

    public SimpleTabbedPane() {
        super();
        setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = borderColor;
                lightHighlight = borderColor;
                shadow = borderColor;
                darkShadow = borderColor;
                focus = normalColor;

                // Увеличиваем отступы для вкладок
                tabInsets = new Insets(5, 10, 5, 10);
                selectedTabPadInsets = new Insets(5, 10, 5, 10);
                contentBorderInsets = new Insets(1, 0, 0, 0);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                g.setColor(borderColor);
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

        setBackground(normalColor);
        setForeground(Color.BLACK);
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
    }
}