package ru.kantser.gui.buttons;

import ru.kantser.gui.theme.Theme;

import javax.swing.JComponent;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Кнопка со скосом в стиле FNIRSI. Направление скоса задаётся
 * {@link Skew}, поддерживается hover-подсветка, клик рассылается
 * зарегистрированным {@link ActionListener}.
 */
public class SkewButton extends JComponent {

    /** Направление скоса кнопки. */
    public enum Skew {
        LEFT,
        RIGHT,
        NONE
    }

    private final CopyOnWriteArrayList<ActionListener> listeners = new CopyOnWriteArrayList<>();

    private String text;
    private Skew skew;
    private boolean hover;
    private boolean pressed;

    public SkewButton(String text) {
        this(text, Skew.NONE);
    }

    public SkewButton(String text, Skew skew) {
        this.text = text == null ? "" : text;
        this.skew = skew == null ? Skew.NONE : skew;
        setOpaque(false);
        setFocusable(true);
        setPreferredSize(new Dimension(preferredWidth(), Theme.BUTTON_HEIGHT));
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                boolean fire = pressed && contains(e.getPoint());
                pressed = false;
                repaint();
                if (fire && isEnabled()) {
                    fireAction();
                }
            }
        };
        addMouseListener(ma);
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text == null ? "" : text;
        repaint();
    }

    public Skew getSkew() {
        return skew;
    }

    public void setSkew(Skew skew) {
        this.skew = skew == null ? Skew.NONE : skew;
        repaint();
    }

    public void addActionListener(ActionListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removeActionListener(ActionListener listener) {
        listeners.remove(listener);
    }

    private void fireAction() {
        ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, text);
        for (ActionListener listener : listeners) {
            listener.actionPerformed(event);
        }
    }

    private int preferredWidth() {
        FontMetrics fm = getFontMetrics(Theme.FONT_BUTTON);
        return Math.max(42, fm.stringWidth(text) + 34 + Theme.BUTTON_SKEW);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        java.awt.Color fillTop;
        java.awt.Color fillBottom;
        java.awt.Color border;
        if (!isEnabled()) {
            fillTop = Theme.BTN_FILL_BOTTOM;
            fillBottom = Theme.BTN_FILL_BOTTOM;
            border = Theme.ORANGE_DARK;
        } else if (pressed) {
            fillTop = Theme.BTN_FILL_TOP_PRESSED;
            fillBottom = Theme.BTN_FILL_BOTTOM_PRESSED;
            border = Theme.BTN_BORDER_HOVER;
        } else if (hover) {
            fillTop = Theme.BTN_FILL_TOP_HOVER;
            fillBottom = Theme.BTN_FILL_BOTTOM_HOVER;
            border = Theme.BTN_BORDER_HOVER;
        } else {
            fillTop = Theme.BTN_FILL_TOP;
            fillBottom = Theme.BTN_FILL_BOTTOM;
            border = Theme.BTN_BORDER;
        }

        java.awt.Shape shape = shape(w, h);

        // Мягкое свечение под кнопкой при наведении.
        if (hover && isEnabled()) {
            g2.setPaint(new RadialGradientPaint(w / 2f, h / 2f, Math.max(w, h) * 0.7f,
                    new float[]{0f, 1f},
                    new java.awt.Color[]{withAlpha(Theme.ORANGE_GLOW, 40), withAlpha(Theme.ORANGE_GLOW, 0)}));
            g2.fill(shape);
        }

        g2.setPaint(new GradientPaint(0, 0, fillTop, 0, h, fillBottom));
        g2.fill(shape);
        g2.setStroke(new BasicStroke(Theme.STROKE_BORDER));
        g2.setColor(border);
        g2.draw(shape);

        g2.setFont(Theme.FONT_BUTTON);
        g2.setColor(isEnabled() ? Theme.BTN_TEXT : Theme.BTN_TEXT_DISABLED);
        FontMetrics fm = g2.getFontMetrics();
        int tx = (w - fm.stringWidth(text)) / 2;
        if (skew == Skew.LEFT) {
            tx += Theme.BUTTON_SKEW / 2;
        } else if (skew == Skew.RIGHT) {
            tx -= Theme.BUTTON_SKEW / 2;
        }
        g2.drawString(text, tx, h / 2 + fm.getAscent() / 2 - 1);
        g2.dispose();
    }

    private java.awt.Shape shape(int w, int h) {
        double s = Theme.BUTTON_SKEW;
        double m = 0.5;
        switch (skew) {
            case LEFT:
                Path2D.Double left = new Path2D.Double();
                left.moveTo(m, h - m);
                left.lineTo(s, m);
                left.lineTo(w - m, m);
                left.lineTo(w - m, h - m);
                left.closePath();
                return left;
            case RIGHT:
                Path2D.Double right = new Path2D.Double();
                right.moveTo(m, m);
                right.lineTo(w - m, m);
                right.lineTo(w - s, h - m);
                right.lineTo(m, h - m);
                right.closePath();
                return right;
            default:
                double r = Theme.BUTTON_RADIUS;
                return new RoundRectangle2D.Double(m, m, w - 1, h - 1, r, r);
        }
    }

    private static java.awt.Color withAlpha(java.awt.Color color, int alpha) {
        return new java.awt.Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
