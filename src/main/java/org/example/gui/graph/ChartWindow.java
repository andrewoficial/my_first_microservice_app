package org.example.gui.graph;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.graph.data.AnswerLoader;
import org.example.gui.graph.data.AnswerValidator;
import org.example.gui.graph.ui.SeriesModel;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.jfree.chart.*;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static org.example.utilites.MyUtilities.convertToLocalDateViaMilisecond;

public class ChartWindow extends JFrame implements Rendeble {
    private static final int HEIGHT_THRESHOLD = 15;
    private static final int WIDTH_THRESHOLD = 5;
    private static final int CONTROL_PANEL_MARGIN = 38;
    private static final Logger log = Logger.getLogger(ChartWindow.class);
    private volatile boolean isGraphBusy = false;
    private final TimeSeriesCollection collection = new TimeSeriesCollection();

    private JPanel window;
    private JPanel graph;
    private JPanel setup;
    private final JPanel selectors = new JPanel();
    private final JPanel controlPanel = new JPanel();

    private final JSlider slider = new JSlider();

    private final JTextField selectedValue = new JTextField();
    private final JTextField lastReceivedValue = new JTextField();
    private int range = 0;

    private ChartPanel chartPanel = null;
    private int currHeight = 400;
    private int currWidth = 400;
    private final Dimension dimension = new Dimension(currWidth, currHeight);
    private final SeriesModel seriesVisibility = new SeriesModel();
    private final AnswerLoader ansLoader = new AnswerLoader();
    private final AnswerValidator ansValidator = new AnswerValidator();


    public ChartWindow() {
        super();
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                int newWidth = getWidth();
                int newHeight = getHeight();

                if (Math.abs(currHeight - newHeight) < HEIGHT_THRESHOLD) {
                    //System.out.println("skip Height");
                    return;
                }
                if (Math.abs(currWidth - newWidth) < WIDTH_THRESHOLD) {
                    //System.out.println("skip Width");
                    return;
                }
                currWidth = newWidth;
                currHeight = newHeight;
                dimension.setSize(controlPanel.getWidth(), currHeight - controlPanel.getHeight() - CONTROL_PANEL_MARGIN);
            }
        });
        initUI();
    }

    public ChartWindow(int num) {
        // num передается для того что бы в каждом окне был открыт ноый график
        super();
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                int newWidth = getWidth();
                int newHeight = getHeight();

                if (Math.abs(currHeight - newHeight) < HEIGHT_THRESHOLD) {
                    //System.out.println("skip Height");
                    return;
                }
                if (Math.abs(currWidth - newWidth) < WIDTH_THRESHOLD) {
                    //System.out.println("skip Width");
                    return;
                }
                currWidth = newWidth;
                currHeight = newHeight;
                dimension.setSize(controlPanel.getWidth(), currHeight - controlPanel.getHeight() - CONTROL_PANEL_MARGIN);
            }
        });
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout()); // Главный Layout окна

        // Левый блок с чекбоксами
        selectors.setLayout(new BoxLayout(selectors, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(selectors); // Делаем его скроллируемым
        scrollPane.setPreferredSize(new Dimension(200, getHeight()));

        // Панель с графиком
        JFreeChart chart = createChart(collection);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);

        // Нижняя панель управления
        controlPanel.setLayout(new FlowLayout());
        controlPanel.add(slider);
        controlPanel.add(lastReceivedValue);
        controlPanel.add(selectedValue);


        // Добавляем в главное окно
        add(scrollPane, BorderLayout.WEST);   // Слева — чекбоксы
        add(chartPanel, BorderLayout.CENTER); // В центре — график
        add(controlPanel, BorderLayout.SOUTH); // Внизу — панель управления

        setTitle("Time Chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


        slider.setMaximum(1000);
        slider.setMinimum(10);
        selectedValue.setText(" ");
        lastReceivedValue.setText(" ");

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                range = slider.getValue();
            }
        });


//        JFreeChart chart = createChart(collection);
//        chartPanel = new ChartPanel(chart);
//        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
//        chartPanel.setBackground(Color.white);
//        chartPanel.setName("Chart Panel");
//        //chartPanel.setPreferredSize(dimension);
//        JPanel jPanel = new JPanel();
//        jPanel.add(chartPanel, 1);
//        //jPanel.add(chartPanel, 2);
//        //jPanel.add(controlPanel, 3);
//
//        add(jPanel);
//
//
//        setTitle("Time chart");
//        setLocationRelativeTo(null);
//        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseMoved(ChartMouseEvent e) {
                ChartEntity entity = e.getEntity();
                if (entity instanceof XYItemEntity) {
                    XYItemEntity itemEntity = (XYItemEntity) entity;
                    int seriesIndex = itemEntity.getSeriesIndex();
                    int itemIndex = itemEntity.getItem();
                    double xValue = itemEntity.getDataset().getXValue(seriesIndex, itemIndex);
                    Date date = new Date((long) xValue);

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
                    String formattedDate = sdf.format(date);
                    double yValue = itemEntity.getDataset().getYValue(seriesIndex, itemIndex);
                    //selectedValue.setText(String.format("Series: %d, X: %.2f, Y: %.2f", seriesIndex, xValue, yValue));
                    selectedValue.setText("Series: " + seriesIndex + " Время: " + formattedDate + " Значение: " + yValue);
                }
            }

            @Override
            public void chartMouseClicked(ChartMouseEvent e) {
                // Ничего не делаем при клике
            }
        });
    }

    private JFreeChart createChart(final XYDataset dataset) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Graph", "Time", "Value", dataset, true, true, false);
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        boolean useLogarithmicScale = false;
        if (useLogarithmicScale) {
            // Логарифмическая ось
            LogarithmicAxis yAxis = new LogarithmicAxis("Y");
            yAxis.setAllowNegativesFlag(true);  // Позволяет использовать отрицательные значения
            yAxis.setExpTickLabelsFlag(true);
            yAxis.setAutoRangeNextLogFlag(true);
            plot.setRangeAxis(yAxis);
        } else {
            // Обычная линейная ось
            NumberAxis yAxis = new NumberAxis("Y");
            yAxis.setAutoRangeIncludesZero(true); // Включает ноль в диапазон оси
            plot.setRangeAxis(yAxis);
        }

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);

        return chart;
    }

    private void updateCB() {
        // Обновление списка чек-боксов
        for (Integer tab : AnswerStorage.getListOfTabsInStorage()) {
            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForTab(tab);
            log.info("Найдено полей для вкладки " + tab + ": " + unitsInAnswer.size());
            addCheckBoxesForTab(tab, unitsInAnswer.size(), unitsInAnswer);
        }

        // Обновление панели управления
        updateControlPanel();
    }


    private String generateNameForSeries(Integer tab, Integer subMeasurement, ArrayList<String> unitsInAnswer) {
        if (unitsInAnswer.size() > subMeasurement) {
            return ("tab" + tab + "_" + "(" + subMeasurement + ")" + unitsInAnswer.get(subMeasurement));
        } else {
            return "tab" + tab + "_" + "(" + subMeasurement + ")";
        }
    }

    private void addCheckBoxesForTab(Integer tab, int fieldsCounter, ArrayList<String> unitsInAnswer) {
        for (int j = 0; j < fieldsCounter; j++) {
            String nameForSeries = generateNameForSeries(tab, j, unitsInAnswer);
            if (!seriesVisibility.containSeries(nameForSeries)) {
                log.info("Добавляю чекбоксы для " + nameForSeries);
                seriesVisibility.addSeries(nameForSeries);
                seriesVisibility.getJBoxes().get(nameForSeries).addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        boolean isSelected = seriesVisibility.getJBoxes().get(nameForSeries).isSelected();
                        seriesVisibility.setVisibility(e.getActionCommand(), isSelected);
                    }
                });
            }

        }
    }


    private void updateControlPanel() {
        log.info("Обновление панели управления");
        controlPanel.removeAll();
        selectors.removeAll();
        for (Map.Entry<String, JCheckBox> stringJCheckBoxEntry : this.seriesVisibility.getJBoxes().entrySet()) {
            selectors.add(stringJCheckBoxEntry.getValue());//Добавление чек-боксов (работает верно)
        }
        slider.setValue(range);
        controlPanel.add(slider);
        controlPanel.add(selectedValue);
        controlPanel.add(lastReceivedValue);

        add(controlPanel, BorderLayout.SOUTH);
        pack();
    }

    private synchronized void getLastData() {

        log.info("Начинаю получение данных");
        // Get the list of all tab numbers
        log.info("Получил список клиентов" + AnswerStorage.getListOfTabsInStorage().toString());
        updateCB();
        log.info("Закончил обновление чекбоксов ");
        int pointer = 0;
        collection.removeAllSeries();//Удаление всех серий
        StringBuilder sb = new StringBuilder();
        for (Integer tab : AnswerStorage.getListOfTabsInStorage()) {
            log.info("Просматриваю для клиента " + tab);
            List<DeviceAnswer> recentAnswers = AnswerStorage.getRecentAnswersForGraph(tab, range);

            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForTab(tab);

            for (int j = 0; j < unitsInAnswer.size(); j++) {
                String seriesName = generateNameForSeries(tab, j, unitsInAnswer);
                log.info("Запрашиваю данные для " + seriesName);
                //log.info("Для вкладки " + tab + " для ответа " + j + " был получен указатель " + pointer);
                //ToDo Защита от других команд вначале опроса (поиск существующих ответов)


                if (seriesVisibility.isVisible(seriesName)) {
                    collection.addSeries(new TimeSeries(seriesName)); //Добавление новой (Только если нужно)
                    log.info("pointer " + seriesName + " will be showed");
                    Double lastPointValue = null;
                    for (DeviceAnswer answer : recentAnswers) {
                        //log.info("answer tab " + answer.getTabNumber() + " field ");

                        if (ansValidator.isCorrectAnswerValue(answer, tab, unitsInAnswer.size(), answer.getFieldCount())) {

                            AnswerValues currentAnswers = answer.getAnswerReceivedValues();
                            double currentValues = currentAnswers.getValues()[j];
                            Millisecond millisecond = new Millisecond(convertToLocalDateViaMilisecond(answer.getAnswerReceivedTime()));

                            collection.getSeries(collection.getSeries().size() - 1).addOrUpdate(millisecond, currentValues);
                            lastPointValue = currentValues;
                            //log.info("addOrUpdate " + millisecond + " " + currentValues);
                        }
                    }
                    sb.append("[").append(lastPointValue).append("] ").append(seriesName).append("   ");
                }
                pointer++;
            }
        }
        lastReceivedValue.setText(sb.toString());
    }


    @Override
    public void renderData() {
        if (isGraphBusy) {
            return;
        }
        isGraphBusy = true;
        log.trace("Обновление графика в потоке " + Thread.currentThread().getName());


        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getLastData();
                repaint();
            }
        });
        isGraphBusy = false;

    }

    @Override
    public boolean isEnable() {
        return this.isShowing();
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        window = new JPanel();
        window.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        graph = new JPanel();
        graph.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        window.add(graph, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        setup = new JPanel();
        setup.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        window.add(setup, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return window;
    }

}

