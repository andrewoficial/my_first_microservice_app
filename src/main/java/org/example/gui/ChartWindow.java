package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
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
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static org.example.utilites.MyUtilities.convertToLocalDateViaMilisecond;

public class ChartWindow extends JDialog implements Rendeble {
    private static final Logger log = Logger.getLogger(ChartWindow.class);
    private TimeSeriesCollection dataset;

    private ArrayList<DeviceAnswer> deviceAnswers = new ArrayList<>();
    private ArrayList<Boolean> cbStates = new ArrayList<>();
    private HashSet<Integer> tabs = new HashSet<>();

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


    public ChartWindow() {
        super();
        initUI();

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
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);


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
                        System.out.println(ent.toString());
                        selectedValue.setText("Выбрано[ item:" + ent.getItem() +
                                " seriesIndex:" + ent.getSeriesIndex() +
                                " value:" + ent.getDataset().getYValue(ent.getSeriesIndex(), ent.getItem()) + "] ");
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
                "Test graph",
                "Time",
                "Pressure",
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        LogarithmicAxis yAxis = new LogarithmicAxis("Y");
        yAxis.setExpTickLabelsFlag(true);
        yAxis.setAutoRangeNextLogFlag(true);
        var renderer = new XYLineAndShapeRenderer();


        //plot.addAnnotation(tempMarker);


        plot.setRangeAxis(yAxis);
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
        renderer.setSeriesItemLabelsVisible(1, Boolean.TRUE);//
        renderer.setDefaultItemLabelsVisible(true);//
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
        for (Integer tab : tabs) {
            JCheckBox jb = new JCheckBox();
            jb.setName(String.valueOf(tab));
            jb.setText("tab" + (tab + 1));
            jb.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    log.info("Change visibility for " + jb.getName());
                    while (cbStates.size() <= tab) {
                        cbStates.add(true);
                    }
                    //System.out.println("seriesBox");
                    //seriesBox.get(Integer.parseInt(jb.getName()));
                    //System.out.println("cbStates");
                    //cbStates.get(Integer.parseInt(jb.getName()));
                    System.out.println("Setup visibility for " + jb.getName());
                    cbStates.set(Integer.parseInt(jb.getName()), jb.isSelected());
                    seriesBox.get(Integer.parseInt(jb.getName())).setSelected(cbStates.get(Integer.parseInt(jb.getName())));
                    //System.out.println("Change" + jb.getName());
                }
            });
            seriesBox.add(jb);
            //System.out.println("Array size now" + seriesBox.size());
        }

        for (int i = 0; i < cbStates.size(); i++) {
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
        //deviceAnswers.addAll(AnswerStorage.AN);

        tabs.clear();

        for (DeviceAnswer answer : deviceAnswers) {
            tabs.add(answer.getTabNumber());
        }
        timeSeries.clear();


        updateCB();


        int i = 0;
        for (Integer tab : tabs) {
            timeSeries.add(new TimeSeries("tab" + (tab + 1)));

            for (DeviceAnswer answer : deviceAnswers) {
                if (answer.getTabNumber() == i) {
                    if (!(answer.getAnswerReceivedValues() == null)) {
                        timeSeries.get(i).addOrUpdate(new Millisecond(convertToLocalDateViaMilisecond(answer.getAnswerReceivedTime())), answer.getAnswerReceivedValues().getValues()[0]);
                    }
                }
            }
            i++;
        }
        collection.removeAllSeries();
        i = 0;
        for (TimeSeries timeSery : timeSeries) {
            //System.out.println("Select to view " + i);
            if (cbStates.size() > i && cbStates.get(i))
                collection.addSeries(timeSery);

            i++;
        }

        dataset = collection;
    }

    @Override
    public void renderData() {
        log.info("Обновление графика в потоке " + Thread.currentThread().getName());
        getLastData();
        repaint();
        //dataset = collection;
        //createChart(dataset);
        //initUI();
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

