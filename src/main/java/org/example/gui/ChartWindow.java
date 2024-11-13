package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.jfree.chart.*;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
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
import java.text.NumberFormat;
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
    private ArrayList<DeviceAnswer> deviceAnswers = new ArrayList<>();
    private ArrayList<Boolean> cbStates = new ArrayList<>(15);
    private HashSet<Integer> tabs = new HashSet<>();
    private ArrayList<Integer> tabsFieldCapacity = new ArrayList<>();
    private ArrayList<TimeSeries> timeSeries = new ArrayList<>();
    private TimeSeriesCollection collection = new TimeSeriesCollection();
    private ArrayList<JCheckBox> seriesBox = new ArrayList<>();

    JComboBox box = new JComboBox<>();
    private JPanel window;
    private JPanel graph;
    private JPanel setup;

    private JPanel controlPanel = new JPanel();

    private JSlider slider = new JSlider();

    private JTextField selectedValue = new JTextField();
    private int range = 0;

    private ChartPanel chartPanel = null;
    private int currHeight = 400;
    private int currWidth = 400;
    private Dimension dimension = new Dimension(currWidth, currHeight);
    private Dimension dimensionControlPanel = new Dimension();


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
        cbStates.set(num, true);
        seriesBox.get(num).setSelected(true);
    }

    private void initUI() {
        slider.setMaximum(1000);
        slider.setMinimum(10);
        selectedValue.setText("12345");

        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                range = slider.getValue();
            }
        });

        getLastData();
        JFreeChart chart = createChart(collection);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        chartPanel.setName("Chart Panel");
        chartPanel.setPreferredSize(dimension);
        add(chartPanel);

        setTitle("Time chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseMoved(ChartMouseEvent e) {
                ChartEntity entity = e.getEntity();
                if (entity instanceof XYItemEntity) {
                    XYItemEntity itemEntity = (XYItemEntity) entity;
                    int seriesIndex = itemEntity.getSeriesIndex();
                    int itemIndex = itemEntity.getItem();
                    double xValue = itemEntity.getDataset().getXValue(seriesIndex, itemIndex);
                    double yValue = itemEntity.getDataset().getYValue(seriesIndex, itemIndex);
                    selectedValue.setText(String.format("Series: %d, X: %.2f, Y: %.2f", seriesIndex, xValue, yValue));
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

        LogarithmicAxis yAxis = new LogarithmicAxis("Y");

        // Настройка логарифмической оси для предотвращения ошибок с отрицательными значениями.
        yAxis.setAllowNegativesFlag(true);  // Позволяет использовать отрицательные значения (экспериментально).
        yAxis.setExpTickLabelsFlag(true);
        yAxis.setAutoRangeNextLogFlag(true);
        plot.setRangeAxis(yAxis);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);

        /*
        plot.setRangeGridlinePaint(Color.ORANGE);
        plot.setDomainGridlinePaint(Color.ORANGE);
        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        renderer.setDefaultItemLabelsVisible(false);
        renderer.setDefaultItemLabelGenerator(renderer.getDefaultItemLabelGenerator());
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2);
        XYItemLabelGenerator generator = new StandardXYItemLabelGenerator("{0} {1} {2}", format, format);
        renderer.setDefaultItemLabelGenerator(generator);//ItemLabelsVisible(true);
        renderer.setDefaultItemLabelsVisible(false);
        chart.getLegend().setFrame(BlockBorder.NONE);
         */
        return chart;
    }


    private void updateCB() {
        seriesBox.clear();
        tabsFieldCapacity.clear();

        // Обновление списка чек-боксов
        for (Integer tab : tabs) {
            int fieldsCounter = getFieldsCountForTab(tab);
            System.out.printf("Найдено полей для вкладки %d: %d\n", tab, fieldsCounter);
            tabsFieldCapacity.add(fieldsCounter);
            addCheckBoxesForTab(tab, fieldsCounter);
        }

        // Синхронизация состояний чек-боксов
        syncCheckBoxStates();

        // Обновление панели управления
        updateControlPanel();
    }

    public static int getFieldsCountForTab(Integer tab) {

        if (Objects.equals(AnswerStorage.getAnswersForGraph(tab).get(0).getTabNumber(), tab)) {
            int size = AnswerStorage.getAnswersForGraph(tab).size();
            if (tab == 0) {
                return AnswerStorage.getAnswersForGraph(tab).get(size - 1).getFieldCount() + 1;
            }
            return AnswerStorage.getAnswersForGraph(tab).get(size - 1).getFieldCount();
        }

        return 1; // Возвращаем 1, если данные для вкладки не найдены
    }

    private void addCheckBoxesForTab(Integer tab, int fieldsCounter) {
        for (int j = 0; j < fieldsCounter; j++) {
            JCheckBox jb = new JCheckBox();
            jb.setName(seriesBox.size() + "");
            jb.setText("tab" + (tab + 1) + "_" + j);
            jb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    updateCheckBoxState(jb);
                }
            });
            seriesBox.add(jb);
            if (cbStates.size() < seriesBox.size()) {
                cbStates.add(jb.isSelected());
            }
        }
    }

    private void updateCheckBoxState(JCheckBox jb) {
        cbStates.clear();
        for (JCheckBox jCheckBox : seriesBox) {
            cbStates.add(jCheckBox.isSelected());
        }

        int numberSeries = Integer.parseInt(jb.getName());
        boolean newState = jb.isSelected();
        cbStates.set(numberSeries, newState);
        seriesBox.get(numberSeries).setSelected(newState);
        log.info("Setup visibility for " + jb.getName() + " now is " + newState);
        System.out.println("Setup visibility for " + jb.getName() + " now is " + newState);
    }

    private void syncCheckBoxStates() {
        for (int i = 0; i < seriesBox.size(); i++) {
            seriesBox.get(i).setSelected(cbStates.get(i));
        }
    }

    private void updateControlPanel() {
        controlPanel.removeAll();
        for (JCheckBox jCheckBox : seriesBox) {
            controlPanel.add(jCheckBox);
        }

        slider.setValue(range);
        controlPanel.add(slider);
        controlPanel.add(selectedValue);

        add(controlPanel, BorderLayout.SOUTH);
        pack();
    }

    private synchronized void getLastData() {
        tabs.clear();
        // Get the list of all tab numbers
        tabs.addAll(AnswerStorage.answersByTab.keySet());

        updateCB();

        for (int tab : tabs) {
            List<DeviceAnswer> recentAnswers = AnswerStorage.getRecentAnswersForGraph(tab, range);
            if (tabsFieldCapacity.size() <= tab) {
                continue;
            }
            for (int j = 0; j < tabsFieldCapacity.get(tab); j++) {
                int pointer = (tab) + j;
                /*
                if (pointer < 0)
                    pointer = 0;
                 */

                while (collection.getSeriesCount() <= pointer || collection.getSeries(pointer) == null) {
                    collection.addSeries(new TimeSeries("graph " + pointer));
                    //System.out.println("addSeries " + pointer);
                }


                collection.getSeries(pointer).clear();

                if (cbStates.get(pointer)) {
                    //System.out.println("pointer " + pointer + " will be showed");
                    for (DeviceAnswer answer : recentAnswers) {
                        if (answer != null && answer.getAnswerReceivedValues() != null &&
                                answer.getAnswerReceivedValues().getValues().length == tabsFieldCapacity.get(tab)) {

                            AnswerValues currentAnswers = answer.getAnswerReceivedValues();
                            double currentValues = currentAnswers.getValues()[j];
                            Millisecond millisecond = new Millisecond(convertToLocalDateViaMilisecond(answer.getAnswerReceivedTime()));

                            collection.getSeries(pointer).addOrUpdate(millisecond, currentValues);
                        }
                    }
                } else {
                    //System.out.println("pointer " + pointer + " will be removed");
                    collection.getSeries(pointer).clear();
                }
            }
        }
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
                isGraphBusy = false;
            }
        });

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
        window.setLayout(new GridLayoutManager(2, 2, new Insets(0, 0, 0, 0), -1, -1));
        graph = new JPanel();
        graph.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        window.add(graph, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        window.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        window.add(spacer2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        setup = new JPanel();
        setup.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        window.add(setup, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return window;
    }

}

