package org.example.gui.components;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.Path2D;

public class CustomScrollBarUI extends BasicScrollBarUI {

    @Override
    protected void configureScrollBarColors() {
        Rectangle rectangle = new Rectangle();
        rectangle.height = 5;
        rectangle.width = 5;
        Color color = new Color(242,242,242);
        this.thumbColor = color;    // Цвет бегунка
        this.thumbRect = rectangle;
        this.trackColor = Color.WHITE;    // Цвет дорожки
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return createZeroButton(); // Скрываем кнопки прокрутки
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return createZeroButton(); // Скрываем кнопки прокрутки
    }

    // Создаем невидимые кнопки
    private JButton createZeroButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(0, 0));
        button.setMinimumSize(new Dimension(0, 0));
        button.setMaximumSize(new Dimension(0, 0));
        return button;
    }


    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Отрисовка бегунка
        g2.setColor(thumbColor);
        g2.fillRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 0, 0);

        // Отрисовка обводки
        g2.setColor(Color.BLACK); // Цвет обводки
        g2.setStroke(new BasicStroke(1));
        //g2.drawRoundRect(thumbBounds.x, thumbBounds.y, thumbBounds.width, thumbBounds.height, 10, 10);

        // Рисуем сердечко в центре
//        int centerX = thumbBounds.x + thumbBounds.width / 2;
//        int centerY = thumbBounds.y + thumbBounds.height / 2;
//        drawHeart(g2, centerX, centerY, 5, new Color(222,222,222));
    }


    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
        g.setColor(trackColor);
        g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
    }

    private void drawHeart(Graphics2D g2, int x, int y, int size, Color color) {
        g2.setColor(color);
        Path2D heart = new Path2D.Double();

        heart.moveTo(x, y);
        heart.curveTo(x - size, y - size, x - size * 2, y + size, x, y + size * 2);
        heart.curveTo(x + size * 2, y + size, x + size, y - size, x, y);
        heart.closePath();

        g2.fill(heart);
    }

}