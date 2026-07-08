package org.example.gui.devices.stu.mcps.emulation;

import org.example.gui.devices.stu.mcps.AsyncLogger;

import javax.swing.*;

public class McpsTestFrame extends JFrame {

    private final McpsTestResponderPanel responderPanel;

    public McpsTestFrame() {
        super("SPB_STU_MCPS Test Responder + Метрики");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(950, 650);
        setLocationRelativeTo(null);

        AsyncLogger logger = new AsyncLogger("mcps_test_responder.log");

        responderPanel = new McpsTestResponderPanel(logger);

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
            } catch (Exception ignored) {}
            new McpsTestFrame().setVisible(true);
        });
    }
}
