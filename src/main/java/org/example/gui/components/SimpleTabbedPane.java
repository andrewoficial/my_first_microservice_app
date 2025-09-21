package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import java.awt.*;

public class SimpleTabbedPane extends JTabbedPane {
    private static final Color TAB_BACKGROUND = NimbusCustomizer.defBackground;
    private static final Color SELECTED_TAB_BACKGROUND = NimbusCustomizer.disabledForeground;
    private static final Color TAB_FOREGROUND = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(0x55, 0x55, 0x55);


    public SimpleTabbedPane() {
        super();
        setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                highlight = BORDER_COLOR;
                lightHighlight = BORDER_COLOR;
                shadow = BORDER_COLOR;
                darkShadow = BORDER_COLOR;
                focus = TAB_BACKGROUND;

                // Увеличиваем отступы для вкладок
                tabInsets = new Insets(1, 1, 5, 1);
                selectedTabPadInsets = new Insets(1, 1, 9, 1);
                contentBorderInsets = new Insets(0, 0, 0, 0);
                // Устанавливаем кастомные цвета
//                tabPane.setBackgroundAt(0, TAB_BACKGROUND);
//                tabPane.setForegroundAt(0, TAB_FOREGROUND);
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                g.setColor(BORDER_COLOR);
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

        setBackground(TAB_BACKGROUND);
        setForeground(TAB_FOREGROUND);
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
    }
}