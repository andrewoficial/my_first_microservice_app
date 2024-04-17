package org.example.gui;

import com.intellij.uiDesigner.core.GridLayoutManager;
import org.example.services.AnswerStorage;
import org.example.services.DeviceAnswer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.time.*;
import org.jfree.data.xy.XYDataItem;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import java.awt.*;
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

import static org.example.utilites.MyUtilities.convertToLocalDateViaMilisecond;

public class ChartWindow extends JDialog implements Rendeble {
    private TimeSeriesCollection dataset;

    private ArrayList<DeviceAnswer> deviceAnswers = new ArrayList<>();
    private HashSet<Integer> tabs = new HashSet<>();

    private ArrayList<TimeSeries> timeSeries = new ArrayList<>();
    private TimeSeriesCollection collection = new TimeSeriesCollection();

    public ChartWindow() {
        super();
        initUI();
    }

    private void initUI() {
        getLastData();
        JFreeChart chart = createChart(dataset);
        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        add(chartPanel);

        pack();
        setTitle("Time chart");
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
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

        XYPlot plot = chart.getXYPlot();

        var renderer = new XYLineAndShapeRenderer();

        renderer.setSeriesPaint(0, Color.RED);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesPaint(1, Color.BLUE);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));

        plot.setRenderer(renderer);
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(false);
        plot.setDomainGridlinesVisible(false);

        chart.getLegend().setFrame(BlockBorder.NONE);

        chart.setTitle(new TextTitle("Average Salary per Age",
                        new Font("Serif", Font.BOLD, 18)
                )
        );

        return chart;
    }


    private void getLastData() {
        deviceAnswers.clear();
        deviceAnswers.addAll(AnswerStorage.AN);

        tabs.clear();
        for (DeviceAnswer answer : deviceAnswers) {
            tabs.add(answer.getTabNumber());
        }
        timeSeries.clear();

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

        for (TimeSeries timeSery : timeSeries) {
            collection.addSeries(timeSery);
        }
        dataset = collection;
    }

    @Override
    public void renderData() {
        System.out.println("Rerender graph" + Thread.currentThread().getName());
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    }
}
