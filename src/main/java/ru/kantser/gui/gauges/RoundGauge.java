package ru.kantser.gui.gauges;

import lombok.Getter;
import ru.kantser.gui.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Polygon;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

// ============================================
// Импорты для 2D-графики в Java:
// - Graphics: базовый класс для рисования, передается в paintComponent
// - Graphics2D: расширенный класс с поддержкой антиалиасинга, трансформаций, градиентов
// - RenderingHints: настройки качества рендеринга (антиалиасинг, сглаживание)
// - Arc2D, Ellipse2D, Line2D, RoundRectangle2D: геометрические фигуры для рисования
// - GradientPaint, RadialGradientPaint: градиентные заливки
// - Polygon: произвольный многоугольник по точкам
// - FontMetrics: для вычисления размеров текста
// ============================================

/**
 * Круглый спидометр в стиле FNIRSI: кольцо-фон 270° с разрывом снизу,
 * дуга значения с градиентом, риски, кружок с буквой единицы, подпись и
 * бокс значения.
 *
 * <p>Все координаты считаются от текущих {@code getWidth()}/{@code getHeight()},
 * поэтому размер влияет на отрисовку. Виджет подписан на {@link GaugeModel}:
 * изменение модели приводит к {@code repaint()} в EDT.
 *
 * <p>Как работает рисование в Swing:
 * 1. Каждый JComponent имеет метод paintComponent(Graphics g)
 * 2. Swing автоматически вызывает этот метод, когда нужно перерисовать компонент
 * 3. Graphics g - это "кисть" для рисования, передается Swing'ом
 * 4. НИКОГДА не вызывайте repaint() или setModel() внутри paintComponent - это вызовет бесконечный цикл
 */
public class RoundGauge extends JComponent {

    // ============================================
    // КОНСТАНТЫ БОКСА ЗНАЧЕНИЯ И СТРЕЛОК
    // ============================================

    /** Ширина бокса со значением в пикселях */
    private static final int VALUE_BOX_WIDTH = 92;
    /** Высота бокса со значением в пикселях */
    private static final int VALUE_BOX_HEIGHT = 26;
    /** Отступ бокса от нижнего края компонента */
    private static final int VALUE_BOX_OFFSET_BOTTOM = 32;

    /** Шаг изменения значения при одном нажатии на стрелку */
    private static final double ARROW_STEP = 1.0;

    // Параметры треугольников-стрелок.
    // Названия NEEDED_* - "желаемые" размеры; из них выводятся реальные
    // величины, которые участвуют в формулах.
    private static final int NEEDED_GAP_BETWEEN_TRIANGLES = 2;
    private static final int NEEDED_HIGH_OF_TRIANGLE = 6;
    private static final int NEEDED_WITH_OF_TRIANGLE_BASE = 10;

    // Треугольника два, поэтому желаемый отступ между их основаниями
    // делим пополам: по половинке уйдёт "вверх" и "вниз" от общей оси - симметрия.
    private static final int GAP_BETWEEN_TRIANGLES = NEEDED_GAP_BETWEEN_TRIANGLES / 2;
    // Длина основания треугольника считается от её середины влево и вправо,
    // поэтому храним ПОЛОВИНУ основания.
    private static final int HALF_WITH_OF_TRIANGLE_BASE = NEEDED_WITH_OF_TRIANGLE_BASE / 2;

    /**
     * Неизменяемый "снимок" геометрии спидометра в нижней части.
     *
     * <p>Содержит готовые Polygon'ы для обеих стрелок и прямоугольник
     * для поля ввода. геометрия удобна дважды:
     * ее можно и нарисовать через g2.fill/g2.draw при отрисовке, и спросить
     * contains(x, y) при обработке клика.Una и та же геометрия -
     * и рисуем, и проверяем попадание.
     *
     * <p>private static - это деталь реализации RoundGauge, извне не нужен.
     */
    private static final class GaugeGeometry {
        /** Треугольник вверх (▲) */
        final Polygon upArrow;
        /** Треугольник вниз (▼) */
        final Polygon downArrow;
        /** Область поля ввода: Rectangle над левой частью бокса */
        final Rectangle inputArea;

        GaugeGeometry(int apexX, int apexY, Rectangle inputArea) {
            this.inputArea = inputArea;
            // Вершина обеих треугольников - одна и та же точка (apexX, apexY).
            // "Верхний" треугольник смотрит вершиной ВВЕРХ от оси, основанием - к оси.
            // "Нижний" - наоборот.
            upArrow = new Polygon(
                    new int[]{apexX, apexX - HALF_WITH_OF_TRIANGLE_BASE, apexX + HALF_WITH_OF_TRIANGLE_BASE},
                    new int[]{apexY - NEEDED_HIGH_OF_TRIANGLE - GAP_BETWEEN_TRIANGLES,
                              apexY - GAP_BETWEEN_TRIANGLES,
                              apexY - GAP_BETWEEN_TRIANGLES},
                    3);
            downArrow = new Polygon(
                    new int[]{apexX, apexX - HALF_WITH_OF_TRIANGLE_BASE, apexX + HALF_WITH_OF_TRIANGLE_BASE},
                    new int[]{apexY + NEEDED_HIGH_OF_TRIANGLE + GAP_BETWEEN_TRIANGLES,
                              apexY + GAP_BETWEEN_TRIANGLES,
                              apexY + GAP_BETWEEN_TRIANGLES},
                    3);
        }
    }

    // ============================================
    // ПОЛЯ КЛАССА
    // ============================================

    /**
     * Настоящий JTextField, наложенный поверх нарисованного бокса.
     *
     * <p>Почему не рисуем текст сами, а берём готовый компонент:
     * у JTextField уже реализованы курсор (Caret), выделение,
     * обработка клавиатуры, буфер обмена, фокус. Писать это руками -
     * это буквально переписать javax.swing.text.
     *
     * <p>Он "прозрачный": фон и рамку не рисует, поэтому визуально
     * бокс остаётся тем, что рисует paintValueBox.
     */
    private final JTextField valueField = createValueField();

    /**
     * Флаг защиты от циклической синхронизации.
     *
     * <p>Цикл выглядел бы так:
     *   модель изменилась → мы пишем текст в поле → DocumentListener
     *   поля думает "пользователь что-то ввёл!" → пишет в модель → ...
     *
     * Пока мы САМИ программно меняем текст, флаг = true,
     * и слушатель документа молчит.
     */
    private boolean updatingField = false;

    /**
     * Модель → поле ввода: показываем текущее значение.
     *
     * <p>Используется в двух местах:
     * 1. Слушатель модели — при каждом изменении.
     * 2. setModel() — ОДИН РАЗ при подключении модели, чтобы показать
     *    её СТАРТОВОЕ значение. Модель не шлёт событие "вот моё начальное
     *    значение" — вид должен спросить сам (pull вместо push).
     */
    private void syncFieldFromModel() {
        if (model == null) {
            return;
        }
        updatingField = true;
        try {
            valueField.setText(String.format("%.2f", model.getValue()));
        } finally {
            updatingField = false;
        }
    }

    /**
     * Слушатель изменений модели.
     * Когда модель (GaugeModel) изменяется (новое значение, состояние), она вызывает этого слушателя.
     * SwingUtilities.invokeLater - выполняет repaint() в Event Dispatch Thread (EDT).
     * Это важно, потому что все операции с UI в Swing должны выполняться в EDT.
     *
     * Доработан для синхронизации с полем ввода:
     * модель → поле: ставим флаг, чтобы DocumentListener
     * не воспринял это как ввод пользователя.
     */
    private final GaugeModel.ChangeListener modelListener =
            m -> SwingUtilities.invokeLater(() -> {
                syncFieldFromModel();
                repaint();
            });

    /**
     * Модель данных для спидометра (Pattern: Model-View-Presenter).
     * Содержит текущее значение, минимум, максимум, метку и единицу измерения.
     * @Getter - аннотация Lombok, автоматически генерирует getter метод getModel()
     */
    @Getter
    private GaugeModel model;

    /**
     * Акцентный цвет для спидометра (цвет дуги значения).
     * По умолчанию берется из темы Theme.ACCENT_DEFAULT.
     * Можно изменить через setAccent()
     */
    @Getter
    private Color accent = Theme.ACCENT_DEFAULT;

    // ============================================
    // КОНСТРУКТОРЫ
    // ============================================
    
    /**
     * Пустой конструктор - для GUI-билдера (например, в IntelliJ IDEA или NetBeans).
     * Создает спидометр без модели. Модель можно назначить позже через setModel().
     * this(null) - вызывает основной конструктор с параметром null
     */
    public RoundGauge() {
        this(null);
    }

    /**
     * Основной конструктор.
     * @param model - модель данных для спидометра
     *
     * setOpaque(false) - делает компонент прозрачным.
     *   Если true, Swing будет сначала закрашивать фон компонента.
     *   Для нестеметрических компонентов обычно ставим false.
     * 
     * setPreferredSize(new Dimension(190, 225)) - задает предпочтительный размер Components.
     *   Это размер, который компонент "хочет" иметь. Layout Manager может его использовать.
     *   В данном случае: ширина=190px, высота=225px
     * 
     * setLayout(null) - координаты детям раздаём сами в doLayout()
     * setModel(model) - устанавливает модель и регистрирует слушатель
     *
     * ============================================
     * ОБРАБОТКА НАЖАТИЙ НА СТРЕЛКИ
     * ============================================
     */
    public RoundGauge(GaugeModel model) {
        setOpaque(false);
        setPreferredSize(new Dimension(190, 225));
        setLayout(null);      // координаты детям раздаём сами в doLayout()
        add(valueField);      // регистрируем поле как дочерний компонент
        setModel(model);

        // наблюдаем нажатия мыши на стрелки
        addMouseListener(new MouseAdapter() {
            /**
             * mousePressed, а не mouseClicked: pressed срабатывает при любом
             * нажатии, а clicked — только если нажали и отпустили на том же
             * месте без движения. Для кнопок обычно приятнее pressed.
             */
            @Override
            public void mousePressed(MouseEvent e) {
                // Без модели менять нечего — просто игнорируем клик
                if (model == null) {
                    return;
                }

                // Та же геометрия, что использовалась при рисовании.
                // Мы не "угадываем" координаты — мы спрашиваем у того же источника.
                GaugeGeometry geometry = gaugeGeometry();

                // Polygon реализует интерфейс Shape, а у Shape есть
                // contains(x, y) — проверка, лежит ли точка ВНУТРИ фигуры.
                // ВНУТРИ — важно: клик должен попасть в треугольник,
                // а не в его ограничивающий прямоугольник.
                if (geometry.upArrow.contains(e.getX(), e.getY())) {
                    // Зажим в [min, max] делает сама модель (DefaultGaugeModel.clamp),
                    // поэтому здесь не нужны проверки границ — передаём "сырое" значение.
                    model.setValue(model.getValue() + ARROW_STEP);
                } else if (geometry.downArrow.contains(e.getX(), e.getY())) {
                    model.setValue(model.getValue() - ARROW_STEP);
                }
                // Попал мимо обеих стрелок — ничего не делаем.
                // Клик по остальной части спидометра нам не интересен.
            }
        });
    }

    // ============================================
    // СОЗДАНИЕ ПОЛЯ ВВОДА
    // ============================================

    /**
     * Создаёт настроенное поле ввода.
     * Вынесено в отдельный метод, чтобы конструктор не разрастался.
     */
    private JTextField createValueField() {
        JTextField field = new JTextField();

        // Внешний вид: никакой собственной рамки и фона —
        // их рисует paintValueBox, иначе поле "перекроет" наш бокс.
        field.setOpaque(false);
        field.setBorder(null);

        // Шрифт и цвет — из темы, как у рисованного текста раньше.
        field.setFont(Theme.FONT_GAUGE_VALUE);
        field.setForeground(Theme.TEXT);
        field.setCaretColor(Theme.TEXT); // курсор тоже должен быть виден

        // Текст прижат влево, как раньше был drawString от x + 10.
        field.setHorizontalAlignment(SwingConstants.LEFT);

        // ============================================
        // СЛУШАТЕЛЬ: ПОЛЬЗОВАТЕЛЬ ВВЁЛ ЗНАЧЕНИЕ → МОДЕЛЬ
        // ============================================

        // Слушаем ДОКУМЕНТ, а не ActionListener:
        // ActionListener сработал бы только по Enter,
        // а DocumentListener — на КАЖДОЕ изменение текста
        // (ввод цифры, удаление, вставка). Вводим "4" → модель = 4,
        // дописываем "5" → модель = 45. Живой ввод.
        field.getDocument().addDocumentListener(new DocumentListener() {

            // В DocumentListener три метода, но логика одна —
            // поэтому общий код вынесен в commit().
            @Override
            public void insertUpdate(DocumentEvent e) {
                commit();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                commit();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                commit(); // для обычного JTextField почти не случается
            }

            /**
             * Пытаемся разобрать текст поля как число и записать в модель.
             *
             * <p>Промежуточные состояния ввода ("", "4.", "-") — это
             * НЕ ошибка, а норма: парсим только когда текст стал числом.
             * Модель в это время хранит последнее валидное значение,
             * а дуга спидометра за ней следит.
             *
             * <p>Зажим в [min, max] по-прежнему делает модель,
             * нам здесь думать не о чём.
             */
            private void commit() {
                if (updatingField || model == null) {
                    return; // меняем текст программно или модели нет — пропускаем
                }
                String text = field.getText().trim().replace(',', '.');
                if (text.isEmpty()) {
                    return; // пустое поле — не трогаем модель
                }
                try {
                    double parsed = Double.parseDouble(text);
                    model.setValue(parsed);
                } catch (NumberFormatException ignored) {
                    // Пользователь в процессе набора (например, "12.")
                    // Просто ждём, пока станет числом.
                }
            }
        });

        return field;
    }

    // ============================================
    // ГЕОМЕТРИЯ
    // ============================================

    /**
     * ЕДИНСТВЕННОЕ место, где считаются координаты стрелок и поля ввода.
     *
     * <p>Координаты зависят только от текущего размера компонента, поэтому
     * это чистая функция: нет полей-кэша, нет проблем с resize.
     * Вызывается из paintValueBox (рисование), из слушателя мыши (hit-test),
     * и из doLayout (размещение поля ввода).
     *
     * <p>Формулы дублируют то, что раньше было размазано по paintComponent
     * и paintValueBox: тот же бокс, те же apexX/apexY.
     */
    private GaugeGeometry gaugeGeometry() {
        int cx = getWidth() / 2;
        int boxX = cx - VALUE_BOX_WIDTH / 2;
        int boxY = getHeight() - VALUE_BOX_OFFSET_BOTTOM;

        int apexX = boxX + VALUE_BOX_WIDTH - 16;      // вершина стрелок, отступ справа 16px
        int apexY = boxY + VALUE_BOX_HEIGHT / 2;      // по центру бокса по вертикали

        // Область ввода: от левого края бокса (+ отступ 10, как был текст)
        // до стрелок (-16). Не даём полю наехать на треугольники.
        Rectangle inputArea = new Rectangle(
                boxX + 10,
                boxY + 3,
                VALUE_BOX_WIDTH - 10 - 16 - 6,   // ширина: минус отступы и стрелки
                VALUE_BOX_HEIGHT - 6);

        return new GaugeGeometry(apexX, apexY, inputArea);
    }

    // ============================================
    // РАЗМЕЩЕНИЕ КОМПОНЕНТОВ
    // ============================================

    /**
     * Размещает дочернее поле ввода.
     *
     * <p>У RoundGauge нет layout manager'а (setLayout(null) в конструкторе),
     * поэтому координаты ребёнку назначаем сами. doLayout() вызывается
     * Swing'ом автоматически, когда размер компонента изменился.
     */
    @Override
    public void doLayout() {
        GaugeGeometry geometry = gaugeGeometry();
        valueField.setBounds(geometry.inputArea);
    }

    // ============================================
    // МЕТОДЫ УСТАНОВКИ
    // ============================================
    
    /**
     * Установка новой модели для спидометра.
     * @param newModel - новая модель или null
     * 
     * Логика:
     * 1. Если была старая модель - удаляем наш слушатель из нее (чтобы не было утечек памяти)
     * 2. Сохраняем новую модель
     * 3. Если новая модель не null - добавляем наш слушатель к ней
     * 4. repaint() - инициируем перерисовку, так как данные изменились
     */
    public void setModel(GaugeModel newModel) {
        if (this.model != null) {
            this.model.removeChangeListener(modelListener);
        }
        this.model = newModel;
        if (this.model != null) {
            this.model.addChangeListener(modelListener);
            syncFieldFromModel();
        }
        repaint();
    }

    /**
     * Установка акцентного цвета для спидометра.
     * @param accent - новый цвет или null (если null, цвет не изменяется)
     * 
     * repaint() - перерисовываем компонент, так как цвет изменился
     */
    public void setAccent(Color accent) {
        if (accent != null) {
            this.accent = accent;
            repaint();
        }
    }

    // ============================================
    // ГЛАВНЫЙ МЕТОД РИСОВАНИЯ
    // ============================================
    
    /**
     * Переопределенный метод рисования компонента.
     * Swing автоматически вызывает этот метод, когда нужно нарисовать компонент.
     * 
     * @param g - объект Graphics для рисования. Swing передает его автоматически.
     * 
     * ВАЖНО:
     * - НИКОГДА не вызывайте repaint() или setModel() внутри paintComponent - это вызовет бесконечный цикл
     * - Все операции рисования должны быть внутри этого метода
     * - g.create() создает копию Graphics объекта для безопасной работы
     */
    @Override
    protected void paintComponent(Graphics g) {
        // Преобразуем Graphics в Graphics2D для доступа к расширенным возможностям:
        // - антиалиасинг
        // - градиенты
        // - трансформации (поворот, масштаб)
        // - настройки рендеринга
        Graphics2D g2 = (Graphics2D) g.create();
        
        // Включаем антиалиасинг для сглаживания краев фигур.
        // KEY_ANTIALIASING - ключ настройки
        // VALUE_ANTIALIAS_ON - значение для включения антиалиасинга
        // без антиалиасинга линии будут "пиксельные", ступенчатые
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ============================================
        // ВЫЧИСЛЕНИЕ БАЗОВЫХ КООРДИНАТ
        // ============================================
        
        // Получаем текущие размеры компонента.
        // В момент вызова paintComponent компонент уже создан и имеет размеры,
        // которые были установлены Layout Manager'ом.
        // getWidth() и getHeight() возвращают ширину и высоту в пикселях
        int w = getWidth();
        int h = getHeight();
        
        // Вычисляем центр окружности спидометра:
        // cx - центр по оси X (горизонталь)
        // cy - центр по оси Y (вертикаль), смещен вниз на 56% от высоты (0.56 * h)
        // Это делается, чтобы спидометр располагался не по центру компонента,
        // а немного ниже - для лучшего визуального баланса
        int cx = w / 2;
        int cy = (int) (h * 0.56);
        
        // Вычисляем радиус кольца спидометра:
        // - w / 2 - 12: половина ширины минус отступ (12 пикселей от краев)
        // - cy - 4: расстояние от центра до верхней границы минус отступ
        // Math.min() выбирает меньшее значение, чтобы кольцо вписывалось в компонент
        int r = Math.min(w / 2 - 12, cy - 4);
        
        // Если радиус слишком маленький (< 20 пикселей), не рисуем ничего
        // Это защита от деления на ноль или рисования невидимых элементов
        if (r < 20) {
            g2.dispose();// Освобождаем ресурсы Graphics2D
            return; // Выходим из метода
        }

        // ============================================
        // ПОЛУЧЕНИЕ ДАННЫХ ИЗ МОДЕЛИ
        // ============================================
        
        // Извлекаем данные из модели (model может быть null при первом вызове)
        // если model == null, используем значения по умолчанию
        String label = model != null ? model.getLabel() : "";    // Метка спидометра (например, "Скорость")
        String unit = model != null ? model.getUnit() : "";       // Единица измерения (например, "км/ч")
        double min = model != null ? model.getMin() : 0.0;      // Минимум шкалы
        double max = model != null ? model.getMax() : 1.0;      // Максимум шкалы
        double value = model != null ? model.getValue() : 0.0;   // Текущее значение

        // ============================================
        // РИСОВАНИЕ КОЛЬЦА-ФОНА (ТРЕК)
        // ============================================
        
        // Создаем дугу (arc) - это часть окружности
        // Arc2D.Double параметры:
        // - cx - r, cy - r: координаты верхнего левого угла ограничивающего прямоугольника
        // - 2 * r, 2 * r: ширина и высота ограничивающего прямоугольника ( он квадратный)
        // - Theme.GAUGE_START_ANGLE: начальный угол дуги (обычно 135° - верхний левый угол)
        // - Theme.GAUGE_ARC_DEGREES: угол дуги (обычно 270° - три четверти окружности)
        // - Arc2D.OPEN: тип дуги - не соединенная линия (а не сектор)
        // Это создает кольцо с разрывом внизу (как спидометр)
        Arc2D.Double ring = new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r,
                Theme.GAUGE_START_ANGLE, Theme.GAUGE_ARC_DEGREES, Arc2D.OPEN);
        
        // Устанавливаем стиль линии (обводки):
        // - Theme.GAUGE_RING_WIDTH: толщина линии (քում)
// Дуга значения: рисуем с CAP_BUTT — конец линии обрезается точно
// по углу значения. С CAP_ROUND полуконцевой колпачок вылезал бы
// за угол на пол-ширины линии и залезал на соседнюю риску.
        // - JOIN_ROUND: скругленные соединения (если линия ломаная)
        g2.setStroke(new java.awt.BasicStroke(Theme.GAUGE_RING_WIDTH,
                java.awt.BasicStroke.CAP_BUTT, java.awt.BasicStroke.JOIN_ROUND));
        
        // Устанавливаем цвет для трека (фона)
        g2.setColor(Theme.GAUGE_TRACK);
        
        // Рисуем дугу (только контур, без заливки)
        g2.draw(ring);

        // ============================================
        // РИСОВАНИЕ ДУГИ ЗНАЧЕНИЯ (ПРОГРЕСС)
        // ============================================
        
        // Вычисляем диапазон значений
        double span = max - min;
        
        // Вычисляем долю (фракцию) текущего значения от общего диапазона:
        // - Если span <= 0 (min == max), то frac = 0 (защита от деления на ноль)
        // - Иначе: (value - min) / span - нормализуем значение в диапазон [0, 1]
        double frac = span <= 0 ? 0 : (value - min) / span;
        
        // Ограничиваем frac в диапазоне [0, 1] на случай, если value < min или value > max
        frac = Math.max(0, Math.min(1, frac));
        
        // Устанавливаем градиентную заливку для дуги:
        // GradientPaint параметры:
        // - (cx - r, cy): начальная точка градиента (левый край)
        // - Theme.GAUGE_ARC_FROM: начальный цвет градиента (обычно светлый)
        // - (cx + r, cy): конечная точка градиента (правый край)
        // - accent: конечный цвет градиента (акцентный цвет)
        // Это создает плавный переход от начала дуги к концу
        g2.setPaint(new GradientPaint(cx - r, cy, Theme.GAUGE_ARC_FROM, cx + r, cy, accent));
        
        // Рисуем дугу значения:
        // Отличие от ring - угол дуги умножается на frac
        // Так дуга будет заполняться пропорционально значению
        // При frac=0: дуга будет точкой
        // При frac=1: дуга будет полной (270°)
        g2.draw(new Arc2D.Double(cx - r, cy - r, 2 * r, 2 * r,
                Theme.GAUGE_START_ANGLE, Theme.GAUGE_ARC_DEGREES * frac, Arc2D.OPEN));

        // ============================================
        // РИСОВАНИЕ РИСОК (ДЕЛЕНИЙ ШКАЛЫ)
        // ============================================
        
        // Устанавливаем тонкую линию для рисок
        g2.setStroke(new java.awt.BasicStroke(Theme.STROKE_TICK));
        
        // Цикл по всем рискам (делениям) на шкале
        // Theme.GAUGE_TICKS: общее количество рисок (например, 30)
        for (int i = 0; i <= Theme.GAUGE_TICKS; i++) {
            // Вычисляем угол для текущей риски:
            // Theme.GAUGE_START_ANGLE: начальный угол (135°)
            // Theme.GAUGE_ARC_DEGREES * i / Theme.GAUGE_TICKS: угол пропорциональный номеру риски
            // - 180: корректировка, так как в Java углы отсчитываются от оси X против часовой стрелки
            // Math.toRadians(): преобразование градусов в радианы (для Math.sin/cos)
            double angle = Math.toRadians(Theme.GAUGE_START_ANGLE
                    + (double) Theme.GAUGE_ARC_DEGREES * i / Theme.GAUGE_TICKS - 180);
            
            // Вычисляем косинус и синус угла для преобразования полярных координат в декартовы
            double c = Math.cos(angle);
            double s = Math.sin(angle);
            
            // Определяем, является ли риска основной (большой) или второстепенной
            // Theme.GAUGE_MAJOR_EVERY: каждая N-ная риска является основной (например, каждая 5-я)
            boolean major = i % Theme.GAUGE_MAJOR_EVERY == 0;
            
            // Вычисляем радиусы для начала и конца риски:
            // r1: внутренний радиус (ближе к центру) - всегда r - 14
            // r2: внешний радиус (дальше от центра) - зависит от типа риски:
            //   - major: r - 24 (длиннее)
            //   - minor: r - 19 (короче)
            int r1 = r - 14;
            int r2 = r - (major ? 24 : 19);
            
            // Устанавливаем цвет в зависимости от типа риски
            g2.setColor(major ? Theme.TICK_MAJOR : Theme.TICK_MINOR);
            
            // Рисуем линию от внутреннего радиуса к внешнему
            // Line2D.Double параметры: (x1, y1, x2, y2) - начало и конец линии
            // cx + c * r1, cy + s * r1: поддерживаем полярные координаты
            // c * r1 - это расстояние по оси X от центра, s * r1 - по оси Y
            g2.draw(new Line2D.Double(cx + c * r1, cy + s * r1, cx + c * r2, cy + s * r2));
        }

        // ============================================
        // РИСОВАНИЕ КРУЖКА С ЕДИНИЦЕЙ ИЗМЕРЕНИЯ
        // ============================================
        
        // Размер значка (кружка) берется из темы
        int badge = Theme.GAUGE_UNIT_BADGE;
        int badgeR = badge / 2; // радиус кружка
        
        // Вычисляем Y-координату для кружка:
        // cy - r / 3: немного выше центра (на 1/3 радиуса от центра вверх)
        int ly = cy - r / 3;
        
        // Устанавливаем радиальный градиент для заливки кружка:
        // RadialGradientPaint параметры:
        // - (cx, ly): центр градиента
        // - badgeR: радиус градиента
        // - new float[]{0f, 1f}: позиции градиента (0% и 100% от радиуса)
        // - new Color[]{accent.brighter(), accent.darker()}: цвета в этих позициях
        // получаем плавный переход от светлого к темному в кружке
        g2.setPaint(new RadialGradientPaint(cx, ly, badgeR,
                new float[]{0f, 1f}, new Color[]{accent.brighter(), accent.darker()}));
        
        // Рисуем заполненный круг (значок)
        // Ellipse2D.Double параметры: (x, y, width, height) - ограничивающий прямоугольник
        // Поскольку это квадрат, получается круг
        g2.fill(new Ellipse2D.Double(cx - badgeR, ly - badgeR, badge, badge));
        
        // Рисуем текст единицы измерения в кружке
        g2.setFont(Theme.FONT_GAUGE_UNIT); // Устанавливаем шрифт из темы
        g2.setColor(Theme.GAUGE_UNIT_TEXT); // Устанавливаем цвет текста из темы
        
        // FontMetrics - класс для измерения текста
        // нужен, чтобы центрировать текст по горизонтали
        FontMetrics fm = g2.getFontMetrics();
        
        // Рисуем строку:
        // - unit: текст для отображения (например, "км/ч")
        // - cx - fm.stringWidth(unit) / 2: X-координата (центрированный текст)
        // - ly + 6: Y-координата (немного ниже центра кружка)
        // fm.stringWidth(unit) возвращает ширину строки в пикселях
        g2.drawString(unit, cx - fm.stringWidth(unit) / 2, ly + 6);

        // ============================================
        // РИСОВАНИЕ ПОДПИСЕЙ
        // ============================================
        
        // Рисуем основную метку спидометра (например, "Скорость")
        g2.setFont(Theme.FONT_GAUGE_LABEL); // Шрифт для метки из темы
        g2.setColor(Theme.TEXT); // Цвет текста
        fm = g2.getFontMetrics();
        // Позиция: по центру горизонтально (cx), немного ниже центра вертикально (cy + 6)
        g2.drawString(label, cx - fm.stringWidth(label) / 2, cy + 6);

        // Рисуем подпись значения (например, "Upper Limit 45.00км/ч")
        String caption = String.format("Upper Limit %.2f%s", value, unit);
        // String.format - форматирует строку:
        // %.2f - число с 2 знаками после запятой
        // %s - строка (unit)
        g2.setColor(Theme.TEXT_DIM); // Более темный цвет для подсказки
        fm = g2.getFontMetrics();
        // Позиция: по центру горизонтально, еще ниже (cy + 24)
        g2.drawString(caption, cx - fm.stringWidth(caption) / 2, cy + 24);

        // ============================================
        // РИСОВАНИЕ БОКСА ЗНАЧЕНИЯ СО СТРЕЛКАМИ
        // ============================================
        
        // Вызов метода для рисования прямоугольника со значением и стрелками.
        // Используем константы класса для позиционирования и размеров,
        // чтобы избежать дублирования "магических чисел"
        paintValueBox(g2, value, min, max);
        
        // ============================================
        // ОЧИСТКА г2
        // ============================================
        
        // ЗАЧЕМ НУЖНО ОСВОБОЖДАТЬ ГРАФИЧЕСКИЙ КОНТЕКСТ?
        // Graphics2D объекты используют системные ресурсы (например, буферы для рендеринга).
        // g2.dispose() освобождает эти ресурсы.
        // Это важно, чтобы избежать утечек памяти.
        // g2 был создан через g.create(), поэтому его обязательно нужно освободить.
        // Исходный объект g освобождать не нужно - его освобождает Swing
        g2.dispose();
    }

    /**
     * Рисует прямоугольник с рамкой и стрелками вверх/вниз.
     * 
     * @param g2 - графический контекст
     * @param value - текущее значение
     * @param min - минимальное значение
     * @param max - максимальное значение
     */
    private void paintValueBox(Graphics2D g2, double value, double min, double max) {
        // ============================================
        // РИСОВАНИЕ ФОНА БОКСА
        // ============================================
        
        GaugeGeometry geometry = gaugeGeometry();
        
        // Радиус скругления углов бокса
        int radius = Theme.GAUGE_VALUE_BOX_RADIUS;
        
        // Рисуем заполненный скругленный прямоугольник (фон)
        g2.setColor(Theme.VALUE_BOX_BG);
        g2.fill(new RoundRectangle2D.Double(
                geometry.inputArea.x - 10,  // x: левее области ввода на 10 пикселей
                geometry.inputArea.y - 3,  // y: выше области ввода на 3 пикселя
                VALUE_BOX_WIDTH,              // ширина бокса
                VALUE_BOX_HEIGHT,             // высота бокса
                radius, radius));            // радиусы скругления по X и Y
        
        // Рисуем контур скругленного прямоугольника
        g2.setColor(Theme.ORANGE_SOFT);
        g2.draw(new RoundRectangle2D.Double(
                geometry.inputArea.x - 10,
                geometry.inputArea.y - 3,
                VALUE_BOX_WIDTH,
                VALUE_BOX_HEIGHT,
                radius, radius));

        // ============================================
        // РИСОВАНИЕ СТРЕЛОК ВВЕРХ/ВНИЗ
        // ============================================

        // Стрелка вверх (▲):
        // - Подсвечивается cyan, если значение больше минимума
        // - Серого цвета, если значение равно или больше максимума
        g2.setColor(value < max ? Theme.ORANGE : Theme.TICK_MINOR);
        g2.fill(geometry.upArrow);

        // Стрелка вниз (▼):
        // - Подсвечивается cyan, если значение меньше минимума
        // - Серого цвета, если значение равно или меньше минимума (это же стрелка на уменьшение)
        g2.setColor(value > min ? Theme.ORANGE : Theme.TICK_MINOR);
        g2.fill(geometry.downArrow);

        // ============================================
        // Лёгкая подсветка области ввода при фокусе
        // ============================================
        
//        // Пользователь видит, что клавиатура сейчас работает в поле
//        if (valueField.hasFocus()) {
//            g2.setColor(Theme.CYAN_SOFT);
//            g2.draw(geometry.inputArea);
//        }
    }
}
