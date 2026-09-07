package org.example.gui.devices.tt5166.emulation;

import javax.swing.*;

/**
 * Окно эмулятора климатической камеры TT5166.
 */
public class TT5166EmulatorFrame extends JFrame {

    private final TT5166EmulationPanel panel;

    public TT5166EmulatorFrame() {
        super("TT5166 — Эмулятор (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        panel = new TT5166EmulationPanel();
        add(panel);
    }

    @Override
    public void dispose() { panel.shutdown(); super.dispose(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TT5166EmulatorFrame().setVisible(true));
    }
}