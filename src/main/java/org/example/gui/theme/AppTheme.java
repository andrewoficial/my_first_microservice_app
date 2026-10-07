package org.example.gui.theme;

/**
 * Доступные темы оформления приложения.
 * Ключ ({@link #getKey()}) хранится в настройках (configAccess.properties, параметр guiTheme).
 */
public enum AppTheme {

    NIMBUS("nimbus", "Nimbus"),
    FLAT_LAF("flatlaf", "FlatLaf Light"),
    DEFAULT("default", "Default (системный)");

    private final String key;
    private final String displayName;

    AppTheme(String key, String displayName) {
        this.key = key;
        this.displayName = displayName;
    }

    public String getKey() {
        return key;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    /**
     * Разбирает сохранённый ключ темы. Неизвестное/пустое значение → {@link #NIMBUS}.
     */
    public static AppTheme fromKey(String key) {
        if (key != null) {
            String k = key.trim();
            for (AppTheme theme : values()) {
                if (theme.key.equalsIgnoreCase(k)) {
                    return theme;
                }
            }
        }
        return NIMBUS;
    }
}
