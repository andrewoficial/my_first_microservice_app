package org.example.gui.theme;

import javax.swing.UIManager;
import java.awt.Color;

/**
 * Палитра на основе текущих UI-свойств ({@link UIManager}). Подходит и для FlatLaf,
 * и для «Default» (системный LAF): цвета берутся в момент вызова, поэтому остаются
 * актуальными после смены LAF и в light/dark режимах.
 */
final class UiDefaultsPalette implements ThemePalette {

    private static final Color FALLBACK_ACCENT = new Color(0x3B82F6);
    private static final Color FALLBACK_BORDER = new Color(0xC0, 0xC0, 0xC0);

    private static Color get(String key, Color fallback) {
        Color color = UIManager.getColor(key);
        return color != null ? color : fallback;
    }

    @Override
    public Color panelBackground() {
        return get("Panel.background", Color.WHITE);
    }

    @Override
    public Color elementBackground() {
        return get("TextField.background", get("Panel.background", Color.WHITE));
    }

    @Override
    public Color trackBackground() {
        return get("ScrollBar.track", get("Panel.background", Color.WHITE));
    }

    @Override
    public Color accent() {
        return get("Component.accentColor", get("Component.focusColor", FALLBACK_ACCENT));
    }

    @Override
    public Color text() {
        return get("Label.foreground", Color.BLACK);
    }

    @Override
    public Color disabledText() {
        return get("Label.disabledForeground", Color.GRAY);
    }

    @Override
    public Color border() {
        return get("Component.borderColor", FALLBACK_BORDER);
    }

    @Override
    public Color selectedBackground() {
        return get("TabbedPane.selectedBackground",
                get("List.selectionBackground", accent()));
    }

    @Override
    public Color scrollThumb() {
        return get("ScrollBar.thumb", new Color(0xC0, 0xC0, 0xC0));
    }

    @Override
    public Color checkMark() {
        return get("CheckBox.icon.checkmarkColor", accent());
    }
}
