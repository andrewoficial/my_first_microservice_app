package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class SimpleButton extends JButton {
    private Color normalColor = new Color(242, 242, 242);
    private Color hoverColor = new Color(230, 230, 230);
    private Color pressedColor = new Color(220, 220, 220);
    
    public SimpleButton(String text) {
        super(text);
        setUI(new SimpleButtonUI());
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.BLACK);
        setFont(getFont().deriveFont(Font.PLAIN, 12f));
        
        // Добавляем эффекты при наведении и нажатии
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBackground(normalColor);
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

    private class SimpleButtonUI extends BasicButtonUI {
        @Override
        public void installUI(JComponent c) {
            super.installUI(c);
            AbstractButton button = (AbstractButton) c;
            button.setOpaque(false);
            button.setBorder(BorderFactory.createEmptyBorder(5, 15, 5, 15));
        }

        @Override
        public void paint(Graphics g, JComponent c) {
            AbstractButton b = (AbstractButton) c;
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Рисуем фон
            if (b.getModel().isPressed()) {
                g2.setColor(pressedColor);
            } else if (b.getModel().isRollover()) {
                g2.setColor(hoverColor);
            } else {
                g2.setColor(normalColor);
            }
            
            // Прямоугольник для фона без скруглений
            g2.fillRect(0, 0, c.getWidth(), c.getHeight());
            
            // Рисуем обводку
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(1));
            g2.drawRect(0, 0, c.getWidth()-1, c.getHeight()-1);

            // Рисуем текст
            FontMetrics fm = g2.getFontMetrics();
            int textWidth = fm.stringWidth(b.getText());
            int textHeight = fm.getHeight();

            g2.setColor(b.getForeground());
            g2.drawString(b.getText(),
                (c.getWidth() - textWidth) / 2,
                (c.getHeight() + textHeight) / 2 - fm.getDescent());

            super.paint(g, c);
        }
        
        @Override
        public Dimension getPreferredSize(JComponent c) {
            Dimension size = super.getPreferredSize(c);
            return new Dimension(Math.max(size.width, 80), Math.max(size.height, 30));
        }
    }
}