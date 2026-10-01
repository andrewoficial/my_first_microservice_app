package ru.kantser.gui.theme;

import java.awt.Color;
import java.awt.Font;

/**
 * Единая палитра и геометрия библиотеки {@code ru.kantser.gui}.
 *
 * <p>Все виджеты библиотеки обязаны брать цвета, шрифты, толщины линий,
 * радиусы скруглений и размеры скосов <b>только отсюда</b>. Хардкод цветов
 * в самих виджетах недопустим.
 *
 * <p>Стиль — «FNIRSI»: почти чёрный фон, оранжевые обводки, градиенты,
 * скошенные углы. Тёплая «апельсиновая» гамма.
 */
public final class Theme {

    private Theme() {
    }

    // ------------------------------------------------------------------ фон
    public static final Color BG = new Color(0x14, 0x0A, 0x04);
    public static final Color BG_TOP = new Color(0x1C, 0x0E, 0x05);
    public static final Color BG_BOTTOM = new Color(0x0D, 0x06, 0x02);
    public static final Color PANEL_BG = new Color(0x22, 0x12, 0x07);

    // -------------------------------------------------------------- оранж
    public static final Color ORANGE = new Color(0xF2, 0x8C, 0x1E);
    public static final Color ORANGE_BRIGHT = new Color(0xFF, 0xA3, 0x33);
    public static final Color ORANGE_GLOW = new Color(0xFF, 0xC4, 0x6B);
    public static final Color ORANGE_SOFT = new Color(0xF2, 0x8C, 0x1E, 150);
    public static final Color ORANGE_DARK = new Color(0x5A, 0x2E, 0x08);

    // ---------------------------------------------------------------- текст
    public static final Color TEXT = new Color(0xFF, 0xF3, 0xE2);
    public static final Color TEXT_DIM = new Color(0xD8, 0xB9, 0x94);

    // -------------------------------------------------------------- акценты
    public static final Color ACCENT_VOLTAGE = new Color(0xFF, 0x7A, 0x1F);
    public static final Color ACCENT_CURRENT = new Color(0xFF, 0xB0, 0x3A);
    public static final Color ACCENT_POWER = new Color(0xFF, 0xD1, 0x66);
    public static final Color ACCENT_TEMP = new Color(0xFF, 0x4D, 0x2E);
    public static final Color ACCENT_DEFAULT = ORANGE;

    // ---------------------------------------------------------- внутренности
    public static final Color GAUGE_TRACK = ORANGE_DARK;
    public static final Color GAUGE_ARC_FROM = new Color(0xB0, 0x55, 0x0A);
    public static final Color TICK_MAJOR = TEXT_DIM;
    public static final Color TICK_MINOR = new Color(0x7A, 0x50, 0x28);
    public static final Color VALUE_BOX_BG = new Color(0x2A, 0x14, 0x06);
    public static final Color GAUGE_UNIT_TEXT = Color.WHITE;

    // ------------------------------------------------------------- кнопки
    public static final Color BTN_FILL_TOP = ORANGE_DARK;
    public static final Color BTN_FILL_BOTTOM = new Color(0x3A, 0x1C, 0x04);
    public static final Color BTN_FILL_TOP_HOVER = new Color(0xC8, 0x66, 0x0E);
    public static final Color BTN_FILL_BOTTOM_HOVER = new Color(0x8A, 0x42, 0x08);
    public static final Color BTN_FILL_TOP_PRESSED = new Color(0x7A, 0x3A, 0x06);
    public static final Color BTN_FILL_BOTTOM_PRESSED = new Color(0x30, 0x16, 0x03);
    public static final Color BTN_BORDER = ORANGE;
    public static final Color BTN_BORDER_HOVER = new Color(0xFF, 0xC4, 0x6B);
    public static final Color BTN_TEXT = TEXT;
    public static final Color BTN_TEXT_DISABLED = new Color(0x8A, 0x6E, 0x54);

    // ------------------------------------------------------------- индикаторы
    public static final Color LAMP_ON = new Color(0xFF, 0xC8, 0x3D);
    public static final Color LAMP_ON_DARK = new Color(0x8A, 0x5C, 0x0E);
    public static final Color LAMP_OFF = new Color(0x46, 0x3A, 0x30);
    public static final Color LAMP_OFF_DARK = new Color(0x1E, 0x14, 0x0C);

    public static final Color POWER_ON_TOP = new Color(0xFF, 0xA3, 0x33);
    public static final Color POWER_ON_BOTTOM = new Color(0xC8, 0x64, 0x0E);
    public static final Color POWER_OFF_TOP = new Color(0x3A, 0x1E, 0x0A);
    public static final Color POWER_OFF_BOTTOM = new Color(0x1E, 0x0E, 0x04);
    public static final Color POWER_ON_BORDER = new Color(0xFF, 0xD8, 0x8A);
    public static final Color POWER_OFF_BORDER = new Color(0x8A, 0x50, 0x1A);

    // ------------------------------------------------------------- графики
    public static final Color PLOT_BG = new Color(0x18, 0x0C, 0x04);
    public static final Color PLOT_GRID = new Color(0x4A, 0x2A, 0x10);

    // ------------------------------------------------------------ геометрия
    /** Сколько градусов занимает шкала спидометра. */
    public static final int GAUGE_ARC_DEGREES = 270;
    /** Начальный угол шкалы (разрыв снизу). */
    public static final int GAUGE_START_ANGLE = 135;
    /** Количество рисок на полной шкале. */
    public static final int GAUGE_TICKS = 54;
    /** Каждая N-я риска — длинная. */
    public static final int GAUGE_MAJOR_EVERY = 9;
    /** Радиус кольца-шкалы. */
    public static final float GAUGE_RING_WIDTH = 11f;
    /** Скругление боксов значений. */
    public static final int GAUGE_VALUE_BOX_RADIUS = 6;
    /** Диаметр кружка с буквой единицы. */
    public static final int GAUGE_UNIT_BADGE = 30;

    /** Базовая высота кнопки. */
    public static final int BUTTON_HEIGHT = 30;
    /** Радиус скругления кнопки без скоса. */
    public static final int BUTTON_RADIUS = 7;
    /** Глубина скоса кнопки (в пикселях). */
    public static final int BUTTON_SKEW = 8;

    /** Скругление углов панелей. */
    public static final int PANEL_RADIUS = 10;

    // -------------------------------------------------------------- штрихи
    public static final float STROKE_HAIRLINE = 1f;
    public static final float STROKE_BORDER = 1.6f;
    public static final float STROKE_TICK = 2f;
    public static final float STROKE_RING = GAUGE_RING_WIDTH;

    // ---------------------------------------------------------------- шрифты
    public static final String FONT_FAMILY = "Segoe UI";

    public static Font font(int style, int size) {
        return new Font(FONT_FAMILY, style, size);
    }

    public static final Font FONT_TITLE = font(Font.BOLD, 22);
    public static final Font FONT_SUBTITLE = font(Font.BOLD, 15);
    public static final Font FONT_BODY = font(Font.PLAIN, 13);
    public static final Font FONT_SMALL = font(Font.PLAIN, 12);
    public static final Font FONT_TINY = font(Font.PLAIN, 11);
    public static final Font FONT_GAUGE_LABEL = font(Font.PLAIN, 12);
    public static final Font FONT_GAUGE_UNIT = font(Font.BOLD, 16);
    public static final Font FONT_GAUGE_VALUE = font(Font.PLAIN, 12);
    public static final Font FONT_BUTTON = font(Font.PLAIN, 13);
    public static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 12);

    /** Подобрать акцент по единице измерения. */
    public static Color accentForUnit(String unit) {
        if (unit == null) {
            return ACCENT_DEFAULT;
        }
        switch (unit) {
            case "V":
                return ACCENT_VOLTAGE;
            case "A":
                return ACCENT_CURRENT;
            case "W":
                return ACCENT_POWER;
            case "°C":
            case "C":
                return ACCENT_TEMP;
            default:
                return ACCENT_DEFAULT;
        }
    }
}