package org.example.gui.devices.tt5166.control;

import javax.swing.*;

/**
 * Окно панели управления климатической камерой TT5166.
 */
public class TT5166Main extends JFrame {

    private final TT5166ControlPanel panel;

    public TT5166Main() {
        super("TT5166 — Панель управления (Modbus RTU)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        panel = new TT5166ControlPanel();
        add(panel);
    }

    @Override
    public void dispose() { panel.shutdown(); super.dispose(); }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new TT5166Main().setVisible(true));
    }
}