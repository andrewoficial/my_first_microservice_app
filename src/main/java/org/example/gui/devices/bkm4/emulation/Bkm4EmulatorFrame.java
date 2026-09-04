package org.example.gui.devices.bkm4.emulation;

import javax.swing.*;

/**
 * Окно эмулятора блока коммутации БКМ-4.
 */
public class Bkm4EmulatorFrame extends JFrame {

    private final Bkm4EmulationPanel emulationPanel;

    public Bkm4EmulatorFrame() {
        super("БКМ-4 — Эмулятор (RS-232C)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 620);
        setLocationRelativeTo(null);
        emulationPanel = new Bkm4EmulationPanel();
        add(emulationPanel);
    }

    @Override
    public void dispose() {
        emulationPanel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Bkm4EmulatorFrame().setVisible(true));
    }
}