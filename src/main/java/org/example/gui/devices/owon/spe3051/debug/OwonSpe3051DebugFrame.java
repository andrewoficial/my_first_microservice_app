package org.example.gui.devices.owon.spe3051.debug;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Окно панели отладки источника питания OWON SPE3051.
 */
public class OwonSpe3051DebugFrame extends JFrame {

    private final OwonSpe3051DebugPanel panel;

    public OwonSpe3051DebugFrame() {
        super("OWON SPE3051 — Отладка");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(980, 720);
        setLocationRelativeTo(null);
        panel = new OwonSpe3051DebugPanel();
        add(panel);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                panel.shutdown();
            }
        });
    }

    @Override
    public void dispose() {
        panel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new OwonSpe3051DebugFrame().setVisible(true));
    }
}
