package org.example.gui.components;

import org.example.gui.utilites.GuiUtilities;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Locale;

public class DecimalSpinner extends JSpinner {

    public DecimalSpinner(double value, double min, double max, double step) {
        super(new SpinnerNumberModel(Double.valueOf(value),
                Double.valueOf(min),
                Double.valueOf(max),
                Double.valueOf(step)));

        // Отключение специфичных рендереров Nimbus для JSpinner и JFormattedTextField
        UIManager.put("Spinner[Enabled].backgroundPainter", null);
        UIManager.put("FormattedTextField[Enabled].backgroundPainter", null);
        UIManager.put("Spinner:FormattedTextField[Enabled].background", NimbusCustomizer.defBackground);
        UIManager.put("Spinner:FormattedTextField[Enabled].opaque", true);

        // Редактор с форматом чисел
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(this, "#0.########");
        setEditor(editor);

        JFormattedTextField textField = editor.getTextField();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');

        DecimalFormat df = new DecimalFormat("#0.########", symbols);

        NumberFormatter formatter = new NumberFormatter(df) {
            @Override
            public Object stringToValue(String text) throws ParseException {
                if (text != null) {
                    text = text.replace(',', '.');
                }
                Object val = super.stringToValue(text);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue();
                }
                return val;
            }

            @Override
            public String valueToString(Object value) throws ParseException {
                if (value instanceof Number) {
                    return super.valueToString(((Number) value).doubleValue());
                }
                return super.valueToString(value);
            }
        };

        formatter.setAllowsInvalid(false);
        formatter.setOverwriteMode(false);

        textField.setFormatterFactory(new DefaultFormatterFactory(formatter));
        GuiUtilities.changeFont(textField);
        System.out.println("=== Components of JSpinner ===");

    }

    // Метод для рекурсивного вывода всех компонентов
    private void printComponentHierarchy(Component comp, int level) {
        String indent = "  ".repeat(level);
        System.out.println(indent + "Component: " + comp.getClass().getName() +
                ", Name: " + comp.getName() +
                ", Background: " + (comp instanceof JComponent ? ((JComponent) comp).getBackground() : "N/A") +
                ", Opaque: " + (comp instanceof JComponent ? ((JComponent) comp).isOpaque() : "N/A"));
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                printComponentHierarchy(child, level + 1);
            }
        }
    }

}
