package org.example.gui.utilites;

import org.example.gui.components.NimbusCustomizer;

import javax.swing.*;
import java.awt.*;

public class GuiUtilities {

    public static void changeFont(JFormattedTextField textField) {
        textField.setOpaque(true);
        textField.setBackground(NimbusCustomizer.disabledForeground);
        textField.setForeground(Color.WHITE);
        textField.setCaretColor(Color.WHITE);
        textField.setSelectionColor(NimbusCustomizer.accent);
        textField.setSelectedTextColor(Color.WHITE);
        textField.setDisabledTextColor(new Color(191, 191, 191));
        textField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Отключение стилей Nimbus для текстового поля
        textField.putClientProperty("Nimbus.Overrides", new UIDefaults());
        textField.putClientProperty("Nimbus.Overrides.InheritDefaults", false);
        textField.revalidate();
        textField.repaint();
        //return textField;
    }
}
