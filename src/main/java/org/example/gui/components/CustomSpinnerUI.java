package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;

public class CustomSpinnerUI extends BasicSpinnerUI {
    private final Color background;

    public CustomSpinnerUI(Color background) {
        this.background = background;
    }

    @Override
    protected void installDefaults() {
        super.installDefaults();
        if (spinner != null) {
            spinner.setOpaque(true);
            spinner.setBackground(background);
            JComponent editor = spinner.getEditor();
            if (editor instanceof JSpinner.NumberEditor) {
                JFormattedTextField textField = ((JSpinner.NumberEditor) editor).getTextField();
                textField.setOpaque(true);
                textField.setBackground(background);
            }
        }
    }
}