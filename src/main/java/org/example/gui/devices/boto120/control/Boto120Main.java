package org.example.gui.devices.boto120.control;

import javax.swing.*;

/**
 * Окно панели-зонда термокамеры BOTO-120 (протокол уточняется).
 */
public class Boto120Main extends JFrame {

    private final Boto120ControlPanel panel;

    public Boto120Main() {
        super("BOTO 120 — Управление (зонд, Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 500);
        setLocationRelativeTo(null);
        panel = new Boto120ControlPanel();
        add(panel);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { panel.shutdown(); }
        });
    }

    @Override
    public void dispose() { panel.shutdown(); super.dispose(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Boto120Main().setVisible(true));
    }
}