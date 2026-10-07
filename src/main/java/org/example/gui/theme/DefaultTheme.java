package org.example.gui.theme;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * «Default» — системный Look&amp;Feel без дополнительной кастомизации.
 */
@Slf4j
final class DefaultTheme {

    private DefaultTheme() {
    }

    static void apply() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                 | UnsupportedLookAndFeelException e) {
            log.warn("Не удалось применить системный Look&Feel: {}", e.toString());
        }
    }
}
