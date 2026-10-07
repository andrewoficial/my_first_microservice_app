package org.example.gui.theme;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

/**
 * Единая точка применения темы оформления.
 * <p>
 * Порядок установки Look&amp;Feel — до создания Swing-компонентов. Менеджер держит
 * выбранную тему, применяет соответствующую реализацию и обновляет уже существующие окна.
 * Выбор темы хранится в настройках (см. {@code MyProperties.guiTheme}) и передаётся
 * сюда через {@link #apply(AppTheme)}.
 */
@Slf4j
public final class ThemeManager {

    private static volatile AppTheme current = AppTheme.NIMBUS;
    private static volatile AppTheme applied = null;
    private static volatile ThemePalette palette = new NimbusPalette();

    private ThemeManager() {
    }

    public static AppTheme getCurrent() {
        return current;
    }

    /** Палитра стандартных цветов текущей темы. */
    public static ThemePalette palette() {
        return palette;
    }

    public static void setCurrent(AppTheme theme) {
        if (theme != null) {
            current = theme;
        }
    }

    /** Применяет текущую выбранную тему. */
    public static void applyCurrent() {
        apply(current);
    }

    /**
     * Устанавливает указанную тему и перерисовывает существующие окна.
     * Повторный вызов с уже применённой темой игнорируется, чтобы не дублировать настройку.
     */
    public static void apply(AppTheme theme) {
        if (theme == null) {
            theme = AppTheme.FLAT_LAF;
        }
        current = theme;
        if (theme == applied) {
            return;
        }
        log.info("Применяю тему интерфейса: {}", theme.getDisplayName());
        switch (theme) {
            case NIMBUS -> {
                NimbusTheme.apply();
                palette = new NimbusPalette();
            }
            case FLAT_LAF -> {
                FlatLafTheme.apply();
                palette = new UiDefaultsPalette();
            }
            case DEFAULT -> {
                DefaultTheme.apply();
                palette = new UiDefaultsPalette();
            }
        }
        applied = theme;
        refreshWindows();
    }

    private static void refreshWindows() {
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
}
