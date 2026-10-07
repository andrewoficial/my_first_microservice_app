package org.example.gui.theme;

import org.example.gui.components.NimbusCustomizer;

import java.awt.Color;

/**
 * Палитра Nimbus. Значения сгруппированы здесь, чтобы в коде компонентов не было
 * «магических» цветов, но при этом вид Nimbus оставался прежним.
 */
final class NimbusPalette implements ThemePalette {

    private static final Color TEXT = Color.WHITE;
    private static final Color DISABLED_TEXT = new Color(191, 191, 191);
    private static final Color BORDER = new Color(0x55, 0x55, 0x55);
    private static final Color ELEMENT_BG = new Color(0x2D, 0x2D, 0x2D);
    private static final Color SCROLL_TRACK = new Color(0x1E, 0x1E, 0x1E);
    private static final Color SCROLL_THUMB = new Color(0x64, 0x64, 0x64);
    private static final Color CHECK_MARK = new Color(118, 149, 110);

    @Override
    public Color panelBackground() {
        return NimbusCustomizer.defBackground;
    }

    @Override
    public Color elementBackground() {
        return ELEMENT_BG;
    }

    @Override
    public Color trackBackground() {
        return SCROLL_TRACK;
    }

    @Override
    public Color accent() {
        return NimbusCustomizer.accent;
    }

    @Override
    public Color text() {
        return TEXT;
    }

    @Override
    public Color disabledText() {
        return DISABLED_TEXT;
    }

    @Override
    public Color border() {
        return BORDER;
    }

    @Override
    public Color selectedBackground() {
        return NimbusCustomizer.disabledForeground;
    }

    @Override
    public Color scrollThumb() {
        return SCROLL_THUMB;
    }

    @Override
    public Color checkMark() {
        return CHECK_MARK;
    }
}
