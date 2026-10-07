package org.example.gui.components;

import org.example.gui.theme.ThemeManager;
import org.example.gui.theme.ThemePalette;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SimpleButton extends JButton {

    public SimpleButton(String text) {
        super(text);
        ThemePalette palette = ThemeManager.palette();
        Color normalColor = palette.elementBackground();
        Color hoverColor = normalColor.brighter();
        Color pressedColor = normalColor.darker();
        Color borderColor = palette.border();

        setBackground(normalColor);
        setForeground(palette.text());
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
