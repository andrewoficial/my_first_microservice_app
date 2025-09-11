package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class CustomComboBoxUI extends BasicComboBoxUI {
    private static final int ARC = 8;
    private static final Color BORDER_COLOR = new Color(0x55, 0x55, 0x55);

    private final Color backgroundColor;
    private final Color foregroundColor;
    private final Color selectionColor;

    public CustomComboBoxUI() {
        this(new Color(0x2D, 0x2D, 0x2D), Color.WHITE, new Color(118, 149, 110));
    }

    public CustomComboBoxUI(Color bgColor, Color fgColor, Color selColor) {
        this.backgroundColor = bgColor;
        this.foregroundColor = fgColor;
        this.selectionColor = selColor;
    }

    @Override
    public void installUI(JComponent c) {
        super.installUI(c);
        comboBox.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        comboBox.setOpaque(false);
        comboBox.setBackground(backgroundColor);
        comboBox.setForeground(foregroundColor);
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = super.createArrowButton();
        button.setBackground(backgroundColor);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        return button;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Рисуем скругленный фон
        if (comboBox.isEnabled()) {
            g2.setColor(comboBox.getBackground());
        } else {
            g2.setColor(UIManager.getColor("Button.disabledBackground"));
        }
        g2.fill(new RoundRectangle2D.Float(0, 0, c.getWidth(), c.getHeight(), ARC, ARC));

        // Рисуем обводку
        g2.setColor(BORDER_COLOR);
        g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, c.getWidth() - 1, c.getHeight() - 1, ARC, ARC));

        g2.dispose();
        super.paint(g, c);
    }

    @Override
    protected ComboPopup createPopup() {
        BasicComboPopup popup = (BasicComboPopup) super.createPopup();

        // Настраиваем внешний вид выпадающего списка
        popup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        popup.setBackground(backgroundColor);

        // Применяем UI к scrollpane внутри popup
        SwingUtilities.invokeLater(() -> {
            JScrollPane sp = findScrollPane(popup);
            if (sp != null) {
                sp.setBorder(BorderFactory.createEmptyBorder());
                sp.getViewport().setBackground(backgroundColor);

                // Явно устанавливаем фон для списка внутри Viewport
                Component view = sp.getViewport().getView();
                if (view instanceof JList) {
                    JList<?> list = (JList<?>) view;
                    list.setBackground(backgroundColor);
                    list.setForeground(foregroundColor);
                    list.setSelectionBackground(selectionColor);
                    list.setSelectionForeground(Color.WHITE);
                }

                // Применяем кастомный UI для полос прокрутки
                sp.getVerticalScrollBar().setUI(new CustomScrollBarUI());
                sp.getHorizontalScrollBar().setUI(new CustomScrollBarUI());
            }
        });

        return popup;
    }

    private JScrollPane findScrollPane(Component c) {
        if (c instanceof JScrollPane) return (JScrollPane) c;
        if (c instanceof Container) {
            for (Component ch : ((Container) c).getComponents()) {
                JScrollPane r = findScrollPane(ch);
                if (r != null) return r;
            }
        }
        return null;
    }

    // Кастомный UI для полосы прокрутки
    public static class CustomScrollBarUI extends javax.swing.plaf.basic.BasicScrollBarUI {
        private static final int SCROLLBAR_WIDTH = 8;

        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0x55, 0x55, 0x55);
            this.trackColor = new Color(0x2D, 0x2D, 0x2D);
        }

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return createZeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return createZeroButton();
        }

        private JButton createZeroButton() {
            JButton button = new JButton();
            button.setPreferredSize(new Dimension(0, 0));
            button.setMinimumSize(new Dimension(0, 0));
            button.setMaximumSize(new Dimension(0, 0));
            return button;
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 4, 4);
            g2.dispose();
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(trackColor);
            g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(SCROLLBAR_WIDTH, SCROLLBAR_WIDTH);
        }
    }
}