package org.example.gui.theme;

import org.example.gui.components.NimbusCustomizer;

/**
 * Тема Nimbus. Вся кастомизация осталась в {@link NimbusCustomizer}
 * (его константы используются и в других компонентах). Этот класс — тонкий адаптер
 * под общий интерфейс тем, чтобы позже можно было перенести логику сюда без правок вызовов.
 */
final class NimbusTheme {

    private NimbusTheme() {
    }

    static void apply() {
        NimbusCustomizer.customize();
    }
}
