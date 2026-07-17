package org.example.gui.devices.arduino.feeboard.control;

import javax.swing.*;
import java.awt.*;

public class LampIndicator extends JPanel {
    private Color lampColor = Color.GRAY;
    private static final int LAMP_SIZE = 12;

    public LampIndicator() {
        setOpaque(false);
        setPreferredSize(new Dimension(LAMP_SIZE, LAMP_SIZE));
        setMinimumSize(new Dimension(LAMP_SIZE, LAMP_SIZE));
        setMaximumSize(new Dimension(LAMP_SIZE, LAMP_SIZE));
    }

    public void setLampColor(Color color) {
        this.lampColor = color;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int size = Math.min(getWidth(), getHeight());
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;
        g2.setColor(lampColor);
        g2.fillOval(x + 1, y + 1, size - 2, size - 2);
        g2.setColor(Color.DARK_GRAY);
        g2.drawOval(x + 1, y + 1, size - 2, size - 2);
        g2.dispose();
    }
}
