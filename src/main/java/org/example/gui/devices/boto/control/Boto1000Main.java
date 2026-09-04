package org.example.gui.devices.boto.control;

import javax.swing.*;

/**
 * Окно управления термокамерой BOTO 1000 / China Modbus.
 */
public class Boto1000Main extends JFrame {

    public Boto1000Main() {
        super("BOTO 1000 — Управление (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        BotoControlPanel panel = new BotoControlPanel("BOTO 1000", 12, 100, 105, 100);
        add(panel);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { panel.shutdown(); }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Boto1000Main().setVisible(true));
    }
}
