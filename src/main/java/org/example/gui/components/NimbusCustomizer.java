package org.example.gui.components;

import org.apache.log4j.Logger;
import javax.swing.*;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.DimensionUIResource;
import java.awt.*;


/**
 * Класс для кастомизации Nimbus LookAndFeel.
 * Содержит статический метод для применения всех настроек.
 */
public class NimbusCustomizer {
    final public static Logger log = org.apache.log4j.Logger.getLogger(NimbusCustomizer.class);//Внешний логгер
    final public static Color accent = new Color(83, 83, 83);
    final public static Color defBackground = new Color(0x3D, 0x3D, 0x3D);
    final public static Color disabledBackground = new Color(0x2D, 0x2D, 0x2D);
    final public static Color disabledForeground = new Color(0x88, 0x88, 0x88);
    final public static Color scrollThumb = new Color(0x55, 0x55, 0x55);

    /**
     * Метод для кастомизации Nimbus LookAndFeel.
     * Устанавливает цвета, painters и UI для различных компонентов Swing.
     * Обновляет UI всех существующих окон после применения настроек.
     */
    public static void customize() {

        // Устанавливаем Look and Feel, если нужно
        log.info("Начинаю настройку интерфейса");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                 UnsupportedLookAndFeelException e) {
            log.warn("Ошибка применения стиля");
            //throw new RuntimeException(e);
        }

        // Заменяю стандартные painter'ы Nimbus для комбобокса
        UIDefaults defaults = UIManager.getLookAndFeelDefaults();

        // Основные настройки Nimbus
        defaults.put("nimbusBase", accent);
        defaults.put("control", defBackground);
        defaults.put("text", Color.WHITE);
        defaults.put("info", defBackground);
        defaults.put("nimbusDisabledText", disabledForeground);
        defaults.put("nimbusFocus", accent);
        defaults.put("nimbusSelection", accent);
        defaults.put("nimbusSelectionBackground", accent);
        defaults.put("textBackground", defBackground);
        defaults.put("textForeground", Color.WHITE);

        // Настройки компонентов
        defaults.put("Label.foreground", Color.WHITE);
        defaults.put("TextArea.foreground", Color.WHITE);
        defaults.put("TextArea.background", new Color(0x2D, 0x2D, 0x2D));
        defaults.put("TextField.background", new Color(0x2D, 0x2D, 0x2D));
        defaults.put("TextField.foreground", Color.WHITE);
        defaults.put("TextField.caretForeground", Color.WHITE);
        defaults.put("TextField.border", BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Отступы
        defaults.put("Panel.background", defBackground);
        // Настройки для JSpinner
        defaults.put("Spinner.background", defBackground); // Темный фон
        defaults.put("Spinner.foreground", Color.WHITE); // Белый текст
        defaults.put("Spinner.caretForeground", Color.WHITE); // Цвет курсора
        defaults.put("Spinner.selectionBackground", accent); // Цвет выделения текста
        defaults.put("Spinner.selectionForeground", Color.WHITE); // Цвет текста при выделении
        defaults.put("Spinner.inactiveForeground", new Color(191, 191, 191)); // Цвет неактивного текста
        defaults.put("Spinner.border", BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Отступы
        UIManager.put("Spinner.background", NimbusCustomizer.defBackground);
        UIManager.put("Spinner.editor.background", NimbusCustomizer.defBackground);

        UIManager.put("Spinner:FormattedTextField[Enabled].backgroundPainter", null);
        UIManager.put("Spinner:FormattedTextField[Enabled].foreground", Color.WHITE);
        UIManager.put("Spinner:FormattedTextField[Enabled].caretForeground", Color.WHITE);
        UIManager.put("Spinner:FormattedTextField[Enabled].selectionBackground", NimbusCustomizer.accent);
        UIManager.put("Spinner:FormattedTextField[Enabled].selectionForeground", Color.WHITE);

        // Настройки для JTextPane
        defaults.put("TextPane.background", defBackground); // Темный фон
        defaults.put("TextPane.foreground", Color.WHITE); // Белый текст
        defaults.put("TextPane.caretForeground", Color.WHITE); // Цвет курсора
        defaults.put("TextPane.selectionBackground", accent); // Цвет выделения текста
        defaults.put("TextPane.selectionForeground", Color.WHITE); // Цвет текста при выделении
        defaults.put("TextPane.inactiveForeground", new Color(191, 191, 191)); // Цвет неактивного текста
        defaults.put("TextPane.border", BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Отступы
        // Настройки для FormattedTextField
        defaults.put("FormattedTextField.background", defBackground); // Темный фон
        defaults.put("FormattedTextField.foreground", Color.WHITE); // Белый текст
        defaults.put("FormattedTextField.caretForeground", Color.WHITE); // Цвет курсора
        defaults.put("FormattedTextField.selectionBackground", accent); // Цвет выделения текста
        defaults.put("FormattedTextField.selectionForeground", Color.WHITE); // Цвет текста при выделении
        defaults.put("FormattedTextField.inactiveForeground", new Color(191, 191, 191)); // Цвет неактивного текста
        defaults.put("FormattedTextField.border", BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Отступы




        // Отключаем стандартные эффекты Nimbus
        defaults.put("TextPane[Enabled].backgroundPainter", null);
        defaults.put("TextPane[Enabled].foregroundPainter", null);
        defaults.put("TextPane[Disabled].backgroundPainter", null);
        defaults.put("TextPane[Disabled].foregroundPainter", null);

        // Для полного контроля можно также настроить связанные компоненты
        defaults.put("EditorPane.background", new Color(0x2D, 0x2D, 0x2D));
        defaults.put("EditorPane.foreground", Color.WHITE);
        defaults.put("TextComponent.background", new Color(0x2D, 0x2D, 0x2D));
        defaults.put("TextComponent.foreground", Color.WHITE);

        // Настройки ComboBox
        defaults.put("ComboBox.background", accent);
        defaults.put("ComboBox.foreground", Color.WHITE);
        defaults.put("ComboBox.selectionBackground", accent);
        defaults.put("ComboBox.selectionForeground", Color.WHITE);
        defaults.put("ComboBox.listBackground", defBackground); // Фон выпадающего списк
        defaults.put("ComboBox.listSelectionBackground", accent); // Фон выделенного элемента
        defaults.put("ComboBox.focus", new Color(0x55, 0x55, 0x55));
        defaults.put("ComboBox.focusWidth", 1); // Толщина обводки фокуса
        defaults.put("ComboBox.focusInsets", new Insets(2, 2, 2, 2)); // Отступы обводки
        defaults.put("ComboBox.border", BorderFactory.createEmptyBorder(5, 5, 5, 5)); // Отступы

        defaults.put("Button.focus", new Color(0x55, 0x55, 0x55));
        defaults.put("TextField.focus", new Color(0x55, 0x55, 0x55));
        defaults.put("TextArea.focus", new Color(0x55, 0x55, 0x55));
        defaults.put("PopupMenu.background", new Color(0x2D, 0x2D, 0x2D));

        // Также добавьте эти общие настройки для списков
        defaults.put("List.background", new Color(0x2D, 0x2D, 0x2D)); // Фон всех списков
        defaults.put("ComboBox[Enabled].background", new Color(0x2D, 0x2D, 0x2D, 120)); // Фон всех списков
        defaults.put("List.foreground", Color.WHITE); // Текст всех списков
        defaults.put("List.selectionBackground", accent); // Фон выделения во всех списках
        defaults.put("List.selectionForeground", Color.WHITE); // Текст выделения во всех списках
        defaults.put("List[Selected].textForeground", Color.WHITE);
        defaults.put("List[Selected].textBackground", accent);
        defaults.put("List[Selected].background", accent);
        defaults.put("List[Selected].foreground", Color.WHITE);

        // Настройки кнопок
        defaults.put("Button.background", accent);
        defaults.put("Button.foreground", Color.WHITE);
        defaults.put("Button[Enabled].background", accent);

        // Настройки полосы прокрутки
        // Основные настройки полосы прокрутки
        defaults.put("ScrollBar.width", 12); // Уменьшаем ширину
        defaults.put("ScrollBar.thumb", new Color(0x66, 0x66, 0x66)); // Цвет бегунка
        defaults.put("ScrollBar.track", defBackground); // Цвет трека
        defaults.put("ScrollBar.thumbDarkShadow", new Color(0x44, 0x44, 0x44));
        defaults.put("ScrollBar.thumbHighlight", new Color(0x88, 0x88, 0x88));
        defaults.put("ScrollBar.thumbShadow", new Color(0x55, 0x55, 0x55));
        defaults.put("ScrollBar.trackHighlight", defBackground);

        defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30)); // Минимальный размер бегунка
        defaults.put("ScrollBar.maximumThumbSize", new Dimension(-1, 45)); // Максимальный размер бегунка (сработало!)

        // насечки на бегунок
        defaults.put("ScrollBar.thumbPainter", new Painter<JComponent>() {
            @Override
            public void paint(Graphics2D g, JComponent c, int width, int height) {
                g.setColor(new Color(0x66, 0x66, 0x66));
                g.fillRect(0, 0, width, height);

                g.setColor(new Color(0x44, 0x44, 0x44));
                if (height > width) { // Вертикальная полоса
                    int centerX = width / 2;
                    g.drawLine(centerX, height / 4, centerX, height / 4 + 2);
                    g.drawLine(centerX, height / 2, centerX, height / 2 + 2);
                    g.drawLine(centerX, 3 * height / 4, centerX, 3 * height / 4 + 2);
                } else { // Горизонтальная полоса
                    int centerY = height / 2;
                    g.drawLine(width / 4, centerY, width / 4 + 2, centerY);
                    g.drawLine(width / 2, centerY, width / 2 + 2, centerY);
                    g.drawLine(3 * width / 4, centerY, 3 * width / 4 + 2, centerY);
                }
            }
        });

        defaults.put("ScrollBar.thumbInsets", new Insets(10, 2, 10, 2)); // Отступы бегунка
        defaults.put("ScrollBar.thumbArc", 4); // Легкое скругление углов бегунка

        // Кастомные UI
        UIManager.put("TabbedPaneUI", CustomTabbedPaneUI.class.getName());
        UIManager.put("ScrollBarUI", CustomScrollBarUI.class.getName());
        //UIManager.put("CheckBoxUI", CustomCheckBoxUI.class.getName());
        CustomDecorator.customizeNimbusCheckBox(disabledForeground, Color.LIGHT_GRAY, Color.WHITE, Color.WHITE);

        // Для неактивных кнопок используем кастомный painter
        Painter<JComponent> nimbusDisabledPainter = (Painter<JComponent>) defaults.get("Button[Enabled].backgroundPainter");
        if (nimbusDisabledPainter != null) {
            defaults.put("Button[Disabled].backgroundPainter", new Painter<JComponent>() {
                @Override
                public void paint(Graphics2D g, JComponent c, int w, int h) {
                    nimbusDisabledPainter.paint(g, c, w, h);
                    g.setColor(disabledBackground);
                    g.fillRoundRect(0, 0, w, h, 6, 6);
                }
            });
        }

        // Обновляем UI всех окон
        for (Window window : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(window);
        }
    }
}
