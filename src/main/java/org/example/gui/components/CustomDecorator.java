package org.example.gui.components;

import javax.swing.*;
import java.awt.*;

public class CustomDecorator {

    /** Метод для настройки только CheckBox в Nimbus */
    public static void customizeNimbusCheckBox(Color disabledSquareColor, Color disabledTextColor, Color enabledTextColor, Color selectedTextColor) {
        UIDefaults d = UIManager.getLookAndFeelDefaults();

        // Цвета текста для состояний
        d.put("CheckBox[Disabled].textForeground", disabledTextColor);
        d.put("CheckBox[Enabled].textForeground", enabledTextColor);
        d.put("CheckBox[Selected+Enabled].textForeground", selectedTextColor);
        d.put("CheckBox.textForeground", enabledTextColor); // базовый цвет текста

        // Кастомный painter для disabled квадрата чекбокса (черный квадрат или указанный цвет)
        Painter<JComponent> squarePainter = new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent c, int w, int h) {
                g.setColor(disabledSquareColor);
                g.fillRect(0, 0, w, h); // квадрат указанного цвета
                // Опционально: обводка или другие детали, если нужно
                g.setColor(disabledSquareColor.darker());
                g.drawRect(0, 0, w - 1, h - 1);
            }
        };

        // Применяем painter для disabled состояния чекбокса
        d.put("CheckBox[Disabled].iconPainter", squarePainter);
        d.put("CheckBox[Disabled+Selected].iconPainter", squarePainter); // если selected и disabled

        // Дополнительно, чтобы убедиться, что текст применяется, можно установить глобально
        UIManager.put("CheckBox.foreground", enabledTextColor);
        UIManager.put("CheckBox.disabledText", disabledTextColor);
    }
}