package org.example.gui.devices.binder.camera.emulation;

import javax.swing.*;

/**
 * Окно эмулятора климатической камеры Binder (TCP).
 */
public class BinderEmulatorFrame extends JFrame {

    private final BinderEmulationPanel emulationPanel;

    public BinderEmulatorFrame() {
        super("Binder — Эмулятор камеры (TCP)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1080, 700);
        setLocationRelativeTo(null);
        emulationPanel = new BinderEmulationPanel();
        add(emulationPanel);
    }

    @Override
    public void dispose() {
        emulationPanel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new BinderEmulatorFrame().setVisible(true);
        });
    }
}
