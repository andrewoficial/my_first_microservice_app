package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.jfree.chart.*;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogarithmicAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.labels.ItemLabelAnchor;
import org.jfree.chart.labels.ItemLabelPosition;
import org.jfree.chart.labels.StandardXYItemLabelGenerator;
import org.jfree.chart.labels.XYItemLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static org.example.utilites.MyUtilities.convertToLocalDateViaMilisecond;

public class ChartWindow extends JFrame implements Rendeble {
    private static final Logger log = Logger.getLogger(ChartWindow.class);
    //private TimeSeriesCollection dataset;

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
            public void componentResized(ComponentEvent componentEvent) {

                if (Math.abs(currHeight - getHeight()) < 15) {
                    System.out.println("scip Height");
                    return;
                }
                if (Math.abs(currWidth - getWidth()) < 5) {
                    System.out.println("scip Width");
                    return;
                }
                currWidth = getWidth();
                currHeight = getHeight();
                dimension.setSize(controlPanel.getWidth(), currHeight - controlPanel.getHeight() - 38); //37 dunno....
                System.out.println();

            }
        });
        initUI();

    }

    private void initUI() {

        //controlPanel.setMaximumSize(dimensionControlPanel);
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
        //chartPanel.setMaximumDrawWidth(4000);
        chartPanel.setPreferredSize(dimension);
        add(chartPanel);
        System.out.println(getComponentCount());
        System.out.println(getComponent(getComponentCount() - 1));
        System.out.println(getComponent(getComponentCount() - 1).getName());


        //updateCB();

        //add(slider);


        setTitle("Time chart");
        setLocationRelativeTo(null);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        chartPanel.addChartMouseListener(new ChartMouseListener() {

            public void chartMouseClicked(ChartMouseEvent e) {
                if (e.getEntity() != null) {
                    XYItemEntity ent = null;
                    try {
                        ent = (XYItemEntity) e.getEntity();
                    } catch (ClassCastException exception) {
                        System.out.println("Wrong class" + exception.getMessage());
                    }
                    if (ent != null) {
                        //System.out.println(ent.toString());
                        selectedValue.setText("Выбрано[ seriesIndex:" + ent.getSeriesIndex() +
                                " значение:" + ent.getDataset().getYValue(ent.getSeriesIndex(), ent.getItem()) + "] ");
                    }
                    //System.out.println("==");
                    //System.out.println(e.getEntity());
                    //XYItemEntity: series = 0, item = 3, dataset = org.jfree.data.time.TimeSeriesCollection@357bbd6
                    //System.out.println(e.getEntity().getURLText());
                    //System.out.println(dataset.getSeries(0).getDataItem(3));

                    //dataset.getSeries(0).getDataItem(3);
                    //System.out.println(dataset.getSeries(0).getValue(3));

                    //e.getChart().getXYPlot().getRenderer().setSeriesItemLabelsVisible(0, true);

                    //System.out.println("==");
                }
            }

            public void chartMouseMoved(ChartMouseEvent e) {
            }

        });
    }

    private JFreeChart createChart(final XYDataset dataset) {

        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Graph",
                "Time",
                "Value",
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        //LogarithmicAxis yAxis = new LogarithmicAxis("Y"); // Отключил из-за неподдержки отрицательных значений
        //yAxis.setExpTickLabelsFlag(true);// Отключил из-за неподдержки отрицательных значений
        //yAxis.setAutoRangeNextLogFlag(true);// Отключил из-за неподдержки отрицательных значений
        var renderer = new XYLineAndShapeRenderer();


        //plot.addAnnotation(tempMarker);


        //plot.setRangeAxis(yAxis);// Отключил из-за неподдержки отрицательных значений
        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setDomainGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.ORANGE);
        plot.setDomainGridlinePaint(Color.ORANGE);


        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));
        //renderer.setSeriesItemLabelsVisible(1, Boolean.TRUE);//
        renderer.setDefaultItemLabelsVisible(false);//
        renderer.setDefaultItemLabelGenerator(renderer.getDefaultItemLabelGenerator());


        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(2); // etc.
        XYItemLabelGenerator generator =
                new StandardXYItemLabelGenerator("{0} {1} {2}", format, format);

        renderer.setDefaultItemLabelGenerator(generator);//ItemLabelsVisible(true);
        renderer.setDefaultItemLabelsVisible(false);


        chart.getLegend().setFrame(BlockBorder.NONE);


        return chart;
    }


    private void updateCB() {
        seriesBox.clear();
        tabsFieldCapacity.clear();
        //cbStates.clear();
        for (Integer tab : tabs) {
            int fieldsCounter = 1;
            //System.out.println("Количество ответов" + deviceAnswers.size());
            for (DeviceAnswer deviceAnswer : deviceAnswers) {
                //System.out.println("Сравниваю " + deviceAnswer.getTabNumber() + " и " + tab);
                if (Objects.equals(deviceAnswer.getTabNumber(), tab)) {
                    //System.out.println("    BREAK");
                    fieldsCounter = deviceAnswer.getFieldCount();
                    break;
                }
                //log.debug("Для вкладки " + tab + " еще нет данных");
                //System.out.println("Для вкладки " + tab + " еще нет данных");
            }
            //log.debug("Для вкладки " + tab + " выбрано полей:" + fieldsCounter);
            //System.out.println("Для вкладки " + tab + " выбрано полей:" + fieldsCounter);
            tabsFieldCapacity.add(fieldsCounter);
            for (int j = 0; j < fieldsCounter; j++) {
                JCheckBox jb = new JCheckBox();
                jb.setName(seriesBox.size() + "");
                jb.setText("tab" + (tab + 1) + "_" + j);
                jb.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
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
                });
                seriesBox.add(jb);
                if (cbStates.size() < seriesBox.size()) {
                    for (JCheckBox jCheckBox : seriesBox) {
                        cbStates.add(jCheckBox.isSelected());
                    }
                }

            }
        }

        for (int i = 0; i < seriesBox.size(); i++) {
            seriesBox.get(i).setSelected(cbStates.get(i));
        }

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

    private void getLastData() {
        deviceAnswers.clear();

        int to = Math.max(AnswerStorage.AN.size() - range - 1, 0);
        for (int i = AnswerStorage.AN.size() - 1; i > to; i--) {
            deviceAnswers.add(AnswerStorage.AN.get(i));
        }

        tabs.clear();
        for (DeviceAnswer answer : deviceAnswers) {
            tabs.add(answer.getTabNumber());
        }

        updateCB();

        for (int tab = 0; tab < tabs.size(); tab++) {
            for (int j = 0; j < tabsFieldCapacity.get(tab); j++) {
                int pointer = tab + j;

                while (collection.getSeriesCount() <= pointer || collection.getSeries(pointer) == null) {
                    collection.addSeries(new TimeSeries("graph " + pointer));
                }
                collection.getSeries(pointer).clear();

                if (cbStates.get(pointer)) {
                    for (DeviceAnswer answer : deviceAnswers) {
                        if (answer != null && answer.getAnswerReceivedValues() != null &&
                                Objects.equals(answer.getTabNumber(), tab) &&
                                answer.getAnswerReceivedValues().getValues().length == tabsFieldCapacity.get(tab)) {

                            AnswerValues currentAnswers = answer.getAnswerReceivedValues();
                            double currentValues = currentAnswers.getValues()[j];
                            Millisecond millisecond = new Millisecond(convertToLocalDateViaMilisecond(answer.getAnswerReceivedTime()));

                            collection.getSeries(pointer).addOrUpdate(millisecond, currentValues);
                        }
                    }
                } else {
                    collection.getSeries(pointer).clear();
                }
            }
        }
    }

    @Override
    public void renderData() {
        log.trace("Обновление графика в потоке " + Thread.currentThread().getName());
        getLastData();
        repaint();
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

