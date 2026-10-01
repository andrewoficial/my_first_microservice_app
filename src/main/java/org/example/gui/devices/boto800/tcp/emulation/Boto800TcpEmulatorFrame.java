package org.example.gui.devices.boto800.tcp.emulation;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

/**
 * Окно эмулятора BOTO-800 по TCP/IP — перехват протокола камеры.
 *
 * <p>Запуск: порт по умолчанию {@value Boto800TcpServerService#DEFAULT_PORT}, слушает все
 * интерфейсы. Включите сетевой порт камеры на адрес этого компьютера — обмен появится
 * в логе и в консоли.
 */
public class Boto800TcpEmulatorFrame extends JFrame {

    private final Boto800TcpEmulationPanel panel;

    public Boto800TcpEmulatorFrame() {
        super("BOTO 120 — Эмулятор TCP/IP (сниффер протокола)");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1180, 720);
        setLocationRelativeTo(null);
        panel = new Boto800TcpEmulationPanel();
        add(panel);
    }

    @Override
    public void dispose() {
        panel.shutdown();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new Boto800TcpEmulatorFrame().setVisible(true);
        });
    }
}
