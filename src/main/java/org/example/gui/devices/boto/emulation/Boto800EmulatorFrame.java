package org.example.gui.devices.boto.emulation;

import javax.swing.*;

/**
 * Окно эмулятора термокамеры BOTO 800.
 */
public class Boto800EmulatorFrame extends JFrame {

    private final BotoEmulationPanel panel;

    public Boto800EmulatorFrame() {
        super("BOTO 800 — Эмулятор (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        panel = new BotoEmulationPanel("BOTO 800", 10, 60, 63, 10);
        add(panel);
    }

    @Override
    public void dispose() { panel.shutdown(); super.dispose(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Boto800EmulatorFrame().setVisible(true));
    }
}
