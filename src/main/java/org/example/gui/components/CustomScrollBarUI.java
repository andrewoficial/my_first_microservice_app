package org.example.gui.components;

import org.example.gui.theme.ThemeManager;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class CustomScrollBarUI extends BasicScrollBarUI {

    private static final int SCROLL_BAR_WIDTH = 12; // Ширина всей полосы прокрутки
    private static final int THUMB_MARGIN = 2; // Отступ для бегунка (уменьшает длину)

    @Override
    protected void configureScrollBarColors() {
        this.thumbColor = ThemeManager.palette().scrollThumb();
        this.trackColor = ThemeManager.palette().trackBackground();
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
        return button;
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Уменьшаем бегунок по длине (для вертикальной полосы)
        int x = thumbBounds.x;
        int y = thumbBounds.y + THUMB_MARGIN;
        int width = thumbBounds.width;
        int height = thumbBounds.height - 2 * THUMB_MARGIN;

        // Для горизонтальной полосы нужно уменьшать по ширине
        if (scrollbar.getOrientation() == JScrollBar.HORIZONTAL) {
            x = thumbBounds.x + THUMB_MARGIN;
            y = thumbBounds.y;
            width = thumbBounds.width - 2 * THUMB_MARGIN;
            height = thumbBounds.height;
        }

        Color thumb = ThemeManager.palette().scrollThumb();
        g2.setColor(thumb);
        g2.fillRoundRect(x, y, width, height, 6, 6);

        // Рисуем риски (насечки) - например, три горизонтальные линии для вертикального скроллбара
        if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
            g2.setColor(thumb.brighter());
            int lineY = y + height / 2;
            g2.drawLine(x + 3, lineY, x + width - 3, lineY);
            g2.drawLine(x + 3, lineY - 3, x + width - 3, lineY - 3);
            g2.drawLine(x + 3, lineY + 3, x + width - 3, lineY + 3);
        } else {
            // Для горизонтального скроллбара риски вертикальные
            g2.setColor(thumb.brighter());
            int lineX = x + width / 2;
            g2.drawLine(lineX, y + 3, lineX, y + height - 3);
            g2.drawLine(lineX - 3, y + 3, lineX - 3, y + height - 3);
            g2.drawLine(lineX + 3, y + 3, lineX + 3, y + height - 3);
        }

        g2.dispose();
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ThemeManager.palette().trackBackground());
        g2.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        return new Dimension(SCROLL_BAR_WIDTH, SCROLL_BAR_WIDTH);
    }
}