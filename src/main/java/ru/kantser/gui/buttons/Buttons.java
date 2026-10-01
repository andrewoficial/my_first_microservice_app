package ru.kantser.gui.buttons;

/**
 * Статическая фабрика кнопок {@link SkewButton}.
 */
public final class Buttons {

    private Buttons() {
    }

    public static SkewButton skew(String text, SkewButton.Skew skew) {
        return new SkewButton(text, skew);
    }

    public static SkewButton skewLeft(String text) {
        return new SkewButton(text, SkewButton.Skew.LEFT);
    }

    public static SkewButton skewRight(String text) {
        return new SkewButton(text, SkewButton.Skew.RIGHT);
    }

    public static SkewButton plain(String text) {
        return new SkewButton(text, SkewButton.Skew.NONE);
    }
}
