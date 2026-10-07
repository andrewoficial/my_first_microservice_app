package org.example.gui.theme;

import com.formdev.flatlaf.FlatLightLaf;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;

/**
 * Тема FlatLaf (светлая). FlatLaf сам управляет цветами через UI-свойства,
 * поэтому здесь только установка LAF и точечные правки.
 */
@Slf4j
final class FlatLafTheme {

    private FlatLafTheme() {
    }

    static void apply() {
        FlatLightLaf.setup();
        UIManager.put("Component.focusWidth", 1);
    }
}
