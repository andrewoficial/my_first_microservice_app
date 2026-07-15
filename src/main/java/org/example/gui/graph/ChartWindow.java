package org.example.gui.graph;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.gui.Rendeble;
import org.example.gui.graph.data.AnswerLoader;
import org.example.gui.graph.data.AnswerValidator;
import org.example.gui.graph.ui.SeriesModel;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.example.services.GraphDataRepository;
import org.jfree.chart.*;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataset;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@Slf4j
public class ChartWindow extends JFrame implements Rendeble {
    private final MainLeftPanelStateCollection panelStateCollection = MainLeftPanelStateCollection.getInstance();
    private static final int HEIGHT_THRESHOLD = 15;
    private static final int WIDTH_THRESHOLD = 5;
    private static final int CONTROL_PANEL_MARGIN = 38;
    private volatile boolean isGraphBusy = false;
    private volatile boolean showTooltip = true;
    private final TimeSeriesCollection collection = new TimeSeriesCollection();

    private JPanel window;
    private JPanel graph;
    private JPanel setup;
    private final JPanel selectors = new JPanel();
    private final JPanel controlPanel = new JPanel();

    private final JSlider slider = new JSlider();

    private final JTextField selectedValue = new JTextField();
    private final JTextField lastReceivedValue = new JTextField();
    private final JCheckBox showTooltipCheckBox = new JCheckBox("Показывать значения всех кривых во всплывающем окне", true);
    private int range = 0;

    private ChartPanel chartPanel = null;
    private int currHeight = 400;
    private int currWidth = 400;
    private final Dimension dimension = new Dimension(currWidth, currHeight);
    private final SeriesModel seriesVisibility = new SeriesModel();
    private final AnswerLoader ansLoader = new AnswerLoader();
    private final AnswerValidator ansValidator = new AnswerValidator();
    private final Map<Integer, Long> lastProcessedTime = new ConcurrentHashMap<>();


    public ChartWindow() {
        super();
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                int newWidth = getWidth();
                int newHeight = getHeight();
                isGraphBusy = true;
                if (Math.abs(currHeight - newHeight) < HEIGHT_THRESHOLD) {
                    //System.out.println("skip Height");
                    //return;
                }
                if (Math.abs(currWidth - newWidth) < WIDTH_THRESHOLD) {
                    //System.out.println("skip Width");
                    //return;
                }
                currWidth = newWidth;
                currHeight = newHeight;
                dimension.setSize(controlPanel.getWidth(), currHeight - controlPanel.getHeight() - CONTROL_PANEL_MARGIN);
                repaint();
                isGraphBusy = false;
            }
        });
        initUI();
    }

    public ChartWindow(int num) {
        // num передается для того что бы в каждом окне был открыт ноый график
        super();
//        this.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent componentEvent) {
//                int newWidth = getWidth();
//                int newHeight = getHeight();
//
//                if (Math.abs(currHeight - newHeight) < HEIGHT_THRESHOLD) {
//                    //System.out.println("skip Height");
//                    //return;
//                }
//                if (Math.abs(currWidth - newWidth) < WIDTH_THRESHOLD) {
//                    //System.out.println("skip Width");
//                    //return;
//                }
//                //currWidth = newWidth;
//                //currHeight = newHeight;
//                dimension.setSize(controlPanel.getWidth(), currHeight - controlPanel.getHeight() - CONTROL_PANEL_MARGIN);
//            }
//        });
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
        chartPanel = new ChartPanel(chart) {
            @Override
            public String getToolTipText(MouseEvent e) {
                if (!showTooltip) return null;
                JFreeChart ch = getChart();
                if (ch == null) return null;
                XYPlot plot = ch.getXYPlot();
                if (plot == null) return null;

                Rectangle2D dataArea = getScreenDataArea();
                if (dataArea == null || !dataArea.contains(e.getPoint())) return null;

                ValueAxis domainAxis = plot.getDomainAxis();
                double xValue = domainAxis.java2DToValue(e.getX(), dataArea, plot.getDomainAxisEdge());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");
                String formattedDate = sdf.format(new Date((long) xValue));

                StringBuilder sb = new StringBuilder();
                sb.append("<html><div style='background-color:white; color:black; padding:3px; border:1px solid gray;'>");
                sb.append("<b>").append(formattedDate).append("</b><br>");

                TimeSeriesCollection tsc = (TimeSeriesCollection) plot.getDataset();
                XYItemRenderer renderer = plot.getRenderer();

                for (int i = 0; i < tsc.getSeriesCount(); i++) {
                    if (renderer == null || !renderer.isSeriesVisible(i)) continue;

                    TimeSeries ts = tsc.getSeries(i);
                    String seriesName = (String) ts.getKey();
                    Double y = getYValueAtX(ts, xValue);

                    if (y != null && !Double.isNaN(y)) {
                        sb.append(String.format("<b>%s:</b> %.4f<br>", seriesName, y));
                    }
                }
                sb.append("</div></html>");
                return sb.toString();
            }
        };
        chartPanel.setDisplayToolTips(true);
        chartPanel.setInitialDelay(200);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);

        // Нижняя панель управления
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row1.add(new JLabel("Последнее принятое: "));
        row1.add(lastReceivedValue);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row2.add(new JLabel("Значение выбранной точки: "));
        row2.add(selectedValue);

        JPanel row3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        row3.add(new JLabel("Размер выборки: "));
        row3.add(slider);
        showTooltipCheckBox.addActionListener(e -> showTooltip = showTooltipCheckBox.isSelected());
        row3.add(showTooltipCheckBox);

        controlPanel.add(row1);
        controlPanel.add(row2);
        controlPanel.add(row3);


        // Добавляем в главное окно
        add(scrollPane, BorderLayout.WEST);   // Слева — чекбоксы
        add(chartPanel, BorderLayout.CENTER); // В центре — график
        add(controlPanel, BorderLayout.SOUTH); // Внизу — панель управления

        setTitle("Time Chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);


        slider.setMaximum(1000);
        slider.setMinimum(10);
        range = 60;
        slider.setValue(range);
        selectedValue.setText(" ");
        lastReceivedValue.setText(" ");

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                range = slider.getValue();
                updateSeriesMaxItemCount(range);
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
                    refreshControlPane();
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

    private Double getYValueAtX(TimeSeries series, double targetX) {
        if (series.getItemCount() == 0) return null;

        int low = 0;
        int high = series.getItemCount() - 1;

        while (low <= high) {
            int mid = (low + high) / 2;
            TimeSeriesDataItem item = series.getDataItem(mid);
            double midX = item.getPeriod().getFirstMillisecond();

            if (Math.abs(midX - targetX) < 0.5) {
                Number yNum = item.getValue();
                return (yNum != null) ? yNum.doubleValue() : null;
            }

            if (midX < targetX) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }

        Double y1 = null;
        if (low < series.getItemCount()) {
            TimeSeriesDataItem item1 = series.getDataItem(low);
            Number yNum = item1.getValue();
            y1 = (yNum != null) ? yNum.doubleValue() : null;
        }

        Double y2 = null;
        if (high >= 0) {
            TimeSeriesDataItem item2 = series.getDataItem(high);
            Number yNum = item2.getValue();
            y2 = (yNum != null) ? yNum.doubleValue() : null;
        }

        if (y1 == null) return y2;
        if (y2 == null) return y1;

        double x1 = series.getDataItem(low).getPeriod().getFirstMillisecond();
        double x2 = series.getDataItem(high).getPeriod().getFirstMillisecond();
        double d1 = Math.abs(x1 - targetX);
        double d2 = Math.abs(x2 - targetX);

        return (d1 <= d2) ? y1 : y2;
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
        //if tab found in paneState
        if (panelStateCollection.containClientId(tab)) {
            String fromPanelStateCollection = panelStateCollection.getDevName(tab);
            if (fromPanelStateCollection != null && !fromPanelStateCollection.isEmpty()) {
                if (unitsInAnswer.size() > subMeasurement) {
                    return ("[" + fromPanelStateCollection + "] (" + subMeasurement + ")" + unitsInAnswer.get(subMeasurement));
                } else {
                    return ("[" + fromPanelStateCollection + "] (" + subMeasurement + ")");
                }
            }
        } else {
            log.warn("Tab " + tab + " not found in panelStateCollection");
            ArrayList<MainLeftPanelState> states = panelStateCollection.getIdTabStateAsList();
            for (MainLeftPanelState state : states) {
                log.info("Contain " + state.getClientId());
            }
        }

        //if tab not found in pane state
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
                        applySeriesVisibility();
                    }
                });
            }

        }
    }


    private void updateControlPanel() {
        log.info("Обновление панели управления");
        //controlPanel.removeAll();
        selectors.removeAll();
        for (Map.Entry<String, JCheckBox> stringJCheckBoxEntry : this.seriesVisibility.getJBoxes().entrySet()) {
            selectors.add(stringJCheckBoxEntry.getValue());//Добавление чек-боксов (работает верно)
        }
        //slider.setValue(range);
        //controlPanel.add(slider);
        //controlPanel.add(selectedValue);
        //controlPanel.add(lastReceivedValue);

        //add(controlPanel, BorderLayout.SOUTH);
        selectors.revalidate();
        selectors.repaint();
//pack();
        //repaint();
    }

    private void refreshControlPane() {
        controlPanel.revalidate();
        controlPanel.repaint();
    }

    private synchronized void getLastData() {
        updateCB();

        ensureAllSeriesExist();
        addNewPointsToSeries();
        applySeriesVisibility();

        updateLastReceivedValue();
        refreshControlPane();
    }

    private void ensureAllSeriesExist() {
        for (Integer tab : AnswerStorage.getListOfTabsInStorage()) {
            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForTab(tab);
            if (unitsInAnswer == null) continue;
            for (int j = 0; j < unitsInAnswer.size(); j++) {
                String seriesName = generateNameForSeries(tab, j, unitsInAnswer);
                if (collection.getSeries(seriesName) == null) {
                    TimeSeries ts = new TimeSeries(seriesName);
                    ts.setMaximumItemCount(range);
                    collection.addSeries(ts);
                }
            }
        }
    }

    private void addNewPointsToSeries() {
        for (Integer tab : AnswerStorage.getListOfTabsInStorage()) {
            long fromTime = lastProcessedTime.getOrDefault(tab, 0L);
            final long[] maxTime = {fromTime};

            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForTab(tab);
            if (unitsInAnswer == null) continue;

            GraphDataRepository.getInstance().forEachHistoryPoint(tab, point -> {
                if (point.getEpochMilli() > fromTime) {
                    Millisecond ms = point.toJFreeMillisecond();
                    for (int j = 0; j < point.getFieldCount() && j < unitsInAnswer.size(); j++) {
                        String seriesName = generateNameForSeries(tab, j, unitsInAnswer);
                        TimeSeries ts = collection.getSeries(seriesName);
                        if (ts != null) {
                            Double val = point.getValue(j);
                            if (val != null) {
                                ts.addOrUpdate(ms, val);
                            }
                        }
                    }
                    maxTime[0] = Math.max(maxTime[0], point.getEpochMilli());
                }
            });

            lastProcessedTime.put(tab, maxTime[0]);
        }
    }

    private void applySeriesVisibility() {
        JFreeChart chart = chartPanel.getChart();
        if (chart == null) return;
        XYPlot plot = chart.getXYPlot();
        if (plot == null) return;
        XYItemRenderer renderer = plot.getRenderer();
        if (renderer == null) return;

        for (int i = 0; i < collection.getSeriesCount(); i++) {
            String seriesName = (String) collection.getSeriesKey(i);
            boolean visible = seriesVisibility.isVisible(seriesName);
            renderer.setSeriesVisible(i, visible);
            renderer.setSeriesVisibleInLegend(i, visible);
        }
    }

    private void updateSeriesMaxItemCount(int maxCount) {
        for (int i = 0; i < collection.getSeriesCount(); i++) {
            collection.getSeries(i).setMaximumItemCount(maxCount);
        }
    }

    private void updateLastReceivedValue() {
        StringBuilder sb = new StringBuilder();
        for (Integer tab : AnswerStorage.getListOfTabsInStorage()) {
            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForTab(tab);
            if (unitsInAnswer == null) continue;
            for (int j = 0; j < unitsInAnswer.size(); j++) {
                String seriesName = generateNameForSeries(tab, j, unitsInAnswer);
                if (seriesVisibility.isVisible(seriesName)) {
                    TimeSeries ts = collection.getSeries(seriesName);
                    if (ts != null && ts.getItemCount() > 0) {
                        double lastVal = ts.getValue(ts.getItemCount() - 1).doubleValue();
                        sb.append("[").append(String.format("%.4f", lastVal)).append("] ").append(seriesName).append("   ");
                    }
                }
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

