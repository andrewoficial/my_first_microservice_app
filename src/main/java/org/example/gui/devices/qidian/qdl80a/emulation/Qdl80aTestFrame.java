package org.example.gui.devices.qidian.qdl80a.emulation;

import org.example.gui.devices.stu.mcps.AsyncLogger;

import javax.swing.*;

public class Qdl80aTestFrame extends JFrame {

    private final Qdl80aTestResponderPanel responderPanel;

    public Qdl80aTestFrame() {
        super("QDL80A Test Responder (Modbus RTU Simulator)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1100, 750);
        setLocationRelativeTo(null);

        AsyncLogger logger = new AsyncLogger("qdl80a_test_responder.log");
        responderPanel = new Qdl80aTestResponderPanel(logger);
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
            new Qdl80aTestFrame().setVisible(true);
        });
    }
}