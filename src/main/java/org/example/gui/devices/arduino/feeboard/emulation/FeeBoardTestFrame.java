package org.example.gui.devices.arduino.feeboard.emulation;

import javax.swing.*;

public class FeeBoardTestFrame extends JFrame {

    private final FeeBoardTestResponderPanel responderPanel;

    public FeeBoardTestFrame() {
        super("ARD_FEE_BRD_METER — Эмулятор");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 560);
        setLocationRelativeTo(null);
        responderPanel = new FeeBoardTestResponderPanel();
        add(responderPanel);
    }

    @Override
    public void dispose() {
        responderPanel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new FeeBoardTestFrame().setVisible(true);
        });
    }
}
