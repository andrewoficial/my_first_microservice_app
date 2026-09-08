package org.example.gui.devices.boto120.emulation;

import javax.swing.*;

/**
 * Окно эмулятора-заглушки BOTO-120.
 */
public class Boto120EmulatorFrame extends JFrame {

    private final Boto120EmulationPanel panel;

    public Boto120EmulatorFrame() {
        super("BOTO 120 — Эмулятор-заглушка (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(800, 480);
        setLocationRelativeTo(null);
        panel = new Boto120EmulationPanel();
        add(panel);
    }

    @Override
    public void dispose() { panel.shutdown(); super.dispose(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Boto120EmulatorFrame().setVisible(true));
    }
}