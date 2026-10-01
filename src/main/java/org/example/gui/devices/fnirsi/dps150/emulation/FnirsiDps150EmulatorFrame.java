package org.example.gui.devices.fnirsi.dps150.emulation;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Окно эмулятора источника питания FNIRSI DPS150.
 */
public class FnirsiDps150EmulatorFrame extends JFrame {

    private final FnirsiDps150EmulationPanel panel;

    public FnirsiDps150EmulatorFrame() {
        super("FNIRSI DPS150 — Эмулятор");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1180, 760);
        setLocationRelativeTo(null);
        panel = new FnirsiDps150EmulationPanel();
        add(panel);
    }

    @Override
    public void dispose() {
        panel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FnirsiDps150EmulatorFrame().setVisible(true));
    }
}
