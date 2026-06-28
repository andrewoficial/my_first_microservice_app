package org.example.gui.sbpStuMcpsTest;

import org.example.gui.sbpStuMcps.AsyncLogger;

import javax.swing.*;
import java.awt.*;

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
