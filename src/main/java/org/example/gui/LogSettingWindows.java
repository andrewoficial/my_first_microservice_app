package org.example.gui;

import javax.swing.*;

public class LogSettingWindows extends JDialog {
    private JPanel parametrPanel;

    public LogSettingWindows() {
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(parametrPanel);
    }
}
