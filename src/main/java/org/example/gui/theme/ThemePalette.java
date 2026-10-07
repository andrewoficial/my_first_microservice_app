package org.example.gui.theme;

import java.awt.Color;

/**
 * Стандартные цвета текущей темы. Используется в кастомных участках GUI,
 * чтобы не хардкодить цвета конкретного Look&amp;Feel.
 * <p>
 * Реализации читают значения из соответствующего LAF; для FlatLaf/default — динамически
 * из {@link javax.swing.UIManager}, поэтому палитра должна запрашиваться в момент отрисовки.
 */
public interface ThemePalette {

    /** Фон обычных панелей и полей ввода. */
    Color panelBackground();

    /** Фон элементов (комбобоксы, кнопки, списки) — чуть темнее панели. */
    Color elementBackground();

    /** Фон трека (дорожки) скроллбара. */
    Color trackBackground();

    /** Акцентный цвет (выделение, фокус, выделенный текст). */
    Color accent();

    /** Основной цвет текста. */
    Color text();

    /** Цвет текста для disabled-состояния. */
    Color disabledText();

    /** Цвет границ/разделителей. */
    Color border();

    /** Фон выделенной вкладки/элемента. */
    Color selectedBackground();

    /** Цвет бегунка скроллбара. */
    Color scrollThumb();

    /** Цвет галочки/выделения в чекбоксах и списках. */
    Color checkMark();
}
