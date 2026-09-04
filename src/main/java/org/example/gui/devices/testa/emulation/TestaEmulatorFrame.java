package org.example.gui.devices.testa.emulation;

import javax.swing.*;

/**
 * Окно эмулятора климатической камеры Testa (UDP).
 */
public class TestaEmulatorFrame extends JFrame {

    private final TestaEmulationPanel panel;

    public TestaEmulatorFrame() {
        super("Testa — Эмулятор (UDP)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        panel = new TestaEmulationPanel();
        add(panel);
    }

    @Override
    public void dispose() {
        panel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TestaEmulatorFrame().setVisible(true));
    }
}
