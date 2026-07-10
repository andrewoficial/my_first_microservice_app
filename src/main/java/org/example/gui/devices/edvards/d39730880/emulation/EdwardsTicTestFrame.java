package org.example.gui.devices.edvards.d39730880.emulation;

import javax.swing.*;

public class EdwardsTicTestFrame extends JFrame {

    private final EdwardsTicTestResponderPanel responderPanel;

    public EdwardsTicTestFrame() {
        super("Edwards TIC (D397) — Эмулятор");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1100, 650);
        setLocationRelativeTo(null);
        responderPanel = new EdwardsTicTestResponderPanel();
        add(responderPanel);
    }

    @Override
    public void dispose() {
        responderPanel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
            new EdwardsTicTestFrame().setVisible(true);
        });
    }
}