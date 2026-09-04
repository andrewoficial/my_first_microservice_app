package org.example.gui.devices.boto.emulation;

import javax.swing.*;

/**
 * Окно эмулятора термокамеры BOTO 1000 / China Modbus.
 */
public class Boto1000EmulatorFrame extends JFrame {

    private final BotoEmulationPanel panel;

    public Boto1000EmulatorFrame() {
        super("BOTO 1000 — Эмулятор (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        panel = new BotoEmulationPanel("BOTO 1000", 12, 100, 105, 100);
        add(panel);
    }

    @Override
    public void dispose() { panel.shutdown(); super.dispose(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Boto1000EmulatorFrame().setVisible(true));
    }
}
