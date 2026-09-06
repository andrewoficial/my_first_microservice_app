package org.example.gui.utilites;

import org.example.gui.components.NimbusCustomizer;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

public class GuiUtilities {

    private static void styleText(JTextComponent textField) {
        textField.setOpaque(true);
        textField.setBackground(NimbusCustomizer.defBackground);
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
    }

    public static JFormattedTextField changeFont(JFormattedTextField textField) {
        styleText(textField);
        return textField;
    }

    public static JTextField changeFont(JTextField textField) {
        styleText(textField);
        return textField;
    }

    public static JSpinner changeFont(JSpinner jSpinner) {
        if (jSpinner.getEditor() instanceof JSpinner.DefaultEditor) {
            JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) jSpinner.getEditor();
            styleText(editor.getTextField());
        }
        return jSpinner;
    }

    /** Рекурсивно затемняет все поля ввода (JSpinner/JTextField/JComboBox) внутри контейнера. */
    public static void darkenInputs(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JSpinner) {
                changeFont((JSpinner) comp);
            } else if (comp instanceof JTextField) {
                changeFont((JTextField) comp);
            } else if (comp instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) comp;
                cb.setOpaque(true);
                cb.setBackground(NimbusCustomizer.defBackground);
                cb.setForeground(Color.WHITE);
                Component ed = cb.getEditor().getEditorComponent();
                if (ed instanceof JTextField) {
                    changeFont((JTextField) ed);
                }
            } else if (comp instanceof Container) {
                darkenInputs((Container) comp);
            }
        }
    }
}
