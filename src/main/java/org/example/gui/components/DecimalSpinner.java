package org.example.gui.components;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
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

        // Редактор с форматом чисел
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(this, "#0.###");
        setEditor(editor);

        JFormattedTextField textField = editor.getTextField();

        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');

        DecimalFormat df = new DecimalFormat("#0.###", symbols);

        NumberFormatter formatter = new NumberFormatter(df) {
            @Override
            public Object stringToValue(String text) throws ParseException {
                if (text != null) {
                    text = text.replace(',', '.');
                }
                Object val = super.stringToValue(text);
                if (val instanceof Number) {
                    return ((Number) val).doubleValue(); // всегда Double!
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
    }
}
