package org.example.gui.devices.owon.spe3051.control;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Окно панели управления источником питания OWON SPE3051.
 */
public class OwonSpe3051Main extends JFrame {

    private final OwonSpe3051ControlPanel panel;

    public OwonSpe3051Main() {
        super("OWON SPE3051 — Управление");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1280, 940);
        setLocationRelativeTo(null);
        panel = new OwonSpe3051ControlPanel();
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
        SwingUtilities.invokeLater(() -> new OwonSpe3051Main().setVisible(true));
    }
}
