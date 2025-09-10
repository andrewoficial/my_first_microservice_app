package org.example.gui.components;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SimpleButton extends JButton {
    private final Color normalColor = new Color(249, 249, 249);
    private final Color hoverColor = new Color(230, 230, 230);
    private final Color pressedColor = new Color(220, 220, 220);
    private final Color borderColor = new Color(200, 200, 200);

    public SimpleButton(String text) {
        super(text);
        setBackground(normalColor);
        setForeground(Color.BLACK);
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
        setBorder(new LineBorder(borderColor, 1));
        setFocusPainted(false);
        setContentAreaFilled(true);
        setOpaque(true);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
                setBorder(new LineBorder(borderColor.darker(), 1));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(normalColor);
                setBorder(new LineBorder(borderColor, 1));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                setBackground(hoverColor);
            }
        });
    }

    public SimpleButton() {
        this("");
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, 80);
        d.height = Math.max(d.height, 30);
        return d;
    }
}