package org.example.gui;

import org.apache.log4j.Logger;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;


public class SimpleChartWindow extends JFrame {
    private static final Logger log = Logger.getLogger(SimpleChartWindow.class);
    private XYSeries series = new XYSeries("File Data");
    private ChartPanel chartPanel;

    public SimpleChartWindow() {
        super("File Data Chart");
        initUI();
        pack();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }

    private void initUI() {
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        JFreeChart chart = createChart(dataset);
        
        chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(800, 600));
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);
        
        add(chartPanel);
    }

    private JFreeChart createChart(final XYDataset dataset) {
        JFreeChart chart = ChartFactory.createXYLineChart(
            "Data Visualization", 
            "Measurement (Volts)", 
            "Temp (K)", 
            dataset
        );
        
        chart.setBackgroundPaint(Color.WHITE);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.lightGray);
        
        return chart;
    }

    public void updateData(ArrayList<Double> measurements, ArrayList<Double> temperatures) {
        series.clear();
        for (int i = 0; i < measurements.size(); i++) {
            series.add(measurements.get(i), temperatures.get(i));
        }
        chartPanel.repaint();
    }
}