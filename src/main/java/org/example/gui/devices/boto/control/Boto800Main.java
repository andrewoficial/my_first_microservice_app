package org.example.gui.devices.boto.control;

import javax.swing.*;

/**
 * Окно управления термокамерой BOTO 800.
 */
public class Boto800Main extends JFrame {

    public Boto800Main() {
        super("BOTO 800 — Управление (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        BotoControlPanel panel = new BotoControlPanel("BOTO 800", 10, 60, 63, 10);
        add(panel);
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override public void windowClosing(java.awt.event.WindowEvent e) { panel.shutdown(); }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Boto800Main().setVisible(true));
    }
}
