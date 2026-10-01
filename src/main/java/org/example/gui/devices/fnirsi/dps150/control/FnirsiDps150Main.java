package org.example.gui.devices.fnirsi.dps150.control;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Окно панели управления источником питания FNIRSI DPS150.
 */
public class FnirsiDps150Main extends JFrame {

    private final FnirsiDps150ControlPanel panel;

    public FnirsiDps150Main() {
        super("FNIRSI DPS150 — Управление");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        panel = new FnirsiDps150ControlPanel();
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
        SwingUtilities.invokeLater(() -> new FnirsiDps150Main().setVisible(true));
    }
}
