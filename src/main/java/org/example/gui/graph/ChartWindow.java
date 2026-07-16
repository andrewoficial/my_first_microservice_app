package org.example.gui.graph;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.gui.Rendeble;
import org.example.gui.graph.data.AnswerLoader;
import org.example.gui.graph.data.AnswerValidator;
import org.example.gui.graph.ui.SeriesModel;
import org.example.services.AnswerStorage;
import org.example.services.GraphDataRepository;
import org.example.services.SpringContextHolder;
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
    private final JCheckBox limitDataCheckBox = new JCheckBox("Ограничивать набор данных", false);
    private final JLabel sliderValueLabel = new JLabel("60");
    private int range = 0;

    private ChartPanel chartPanel = null;
    private int currHeight = 400;
    private int currWidth = 400;
    private final Dimension dimension = new Dimension(currWidth, currHeight);
    private final SeriesModel seriesVisibility = new SeriesModel();
    private final AnswerLoader ansLoader = new AnswerLoader();
    private final AnswerValidator ansValidator = new AnswerValidator();
    private final Map<Integer, Long> lastProcessedTime = new ConcurrentHashMap<>();

    // Command-based series tracking
    private final Map<String, Integer> seriesNameToTabId = new HashMap<>();
    private final Map<String, String> seriesNameToCommand = new HashMap<>();
    private final Map<Integer, String> lastStableCommand = new HashMap<>();
    private final Set<JCheckBox> addedToPanel = Collections.newSetFromMap(new IdentityHashMap<>());
    // Cached TimeSeries refs: tabId → command → TimeSeries[]
    private final Map<Integer, Map<String, TimeSeries[]>> cachedSeriesRefs = new HashMap<>();


    public ChartWindow() {
        super();
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent componentEvent) {
                int newWidth = getWidth();
                int newHeight = getHeight();
                isGraphBusy = true;
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
        super();
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        selectors.setLayout(new BoxLayout(selectors, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(selectors);
        scrollPane.setPreferredSize(new Dimension(200, getHeight()));

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
                        sb.append("<b>").append(seriesName).append(":</b> ");
                        appendDecimal(sb, y);
                        sb.append("<br>");
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
        row3.add(sliderValueLabel);
        row3.add(limitDataCheckBox);
        showTooltipCheckBox.addActionListener(e -> showTooltip = showTooltipCheckBox.isSelected());
        row3.add(showTooltipCheckBox);

        controlPanel.add(row1);
        controlPanel.add(row2);
        controlPanel.add(row3);

        add(scrollPane, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

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
                sliderValueLabel.setText(String.valueOf(range));
                if (limitDataCheckBox.isSelected()) {
                    updateSeriesMaxItemCount(range);
                }
            }
        });

        limitDataCheckBox.addActionListener(e -> {
            if (limitDataCheckBox.isSelected()) {
                updateSeriesMaxItemCount(range);
            } else {
                updateSeriesMaxItemCount(Integer.MAX_VALUE);
            }
        });

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
                    selectedValue.setText("Series: " + seriesIndex + " Время: " + formattedDate + " Значение: " + yValue);
                }
            }

            @Override
            public void chartMouseClicked(ChartMouseEvent e) {
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
            LogarithmicAxis yAxis = new LogarithmicAxis("Y");
            yAxis.setAllowNegativesFlag(true);
            yAxis.setExpTickLabelsFlag(true);
            yAxis.setAutoRangeNextLogFlag(true);
            plot.setRangeAxis(yAxis);
        } else {
            NumberAxis yAxis = new NumberAxis("Y");
            yAxis.setAutoRangeIncludesZero(true);
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

    private void detectStabilityChanges() {
        AnswerStorage answerStorage = SpringContextHolder.getBean(AnswerStorage.class);
        for (Integer tab : answerStorage.getTabsInStorage()) {
            String currentStable = answerStorage.getStableCommand(tab);
            String prevStable = lastStableCommand.get(tab);

            if (!Objects.equals(currentStable, prevStable)) {
                if (currentStable != null) {
                    log.info("Stability changed for tab {}: '{}' → '{}'", tab, prevStable, currentStable);
                }
                lastStableCommand.put(tab, currentStable);
                lastProcessedTime.put(tab, 0L);
                cachedSeriesRefs.remove(tab);
                ansLoader.invalidateCache(tab);
            }
        }
    }

    private void updateCB() {
        AnswerStorage answerStorage = SpringContextHolder.getBean(AnswerStorage.class);
        boolean newCheckboxesAdded = false;
        for (Integer tab : answerStorage.getTabsInStorage()) {
            String stableCmd = answerStorage.getStableCommand(tab);
            if (stableCmd == null) continue;

            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForSelectedClientOrTab(tab);
            if (unitsInAnswer == null || unitsInAnswer.isEmpty()) continue;

            if (addCheckBoxesForTab(tab, stableCmd, unitsInAnswer)) {
                newCheckboxesAdded = true;
            }
        }
        if (newCheckboxesAdded) {
            updateControlPanel();
        }
    }


    private String generateNameForSeries(Integer tab, String command, Integer subMeasurement, ArrayList<String> unitsInAnswer) {
        if (panelStateCollection.containClientId(tab)) {
            String devName = panelStateCollection.getDevName(tab);
            if (devName != null && !devName.isEmpty()) {
                if (unitsInAnswer.size() > subMeasurement) {
                    return "[" + devName + "] [" + command + "] (" + subMeasurement + ")" + unitsInAnswer.get(subMeasurement);
                } else {
                    return "[" + devName + "] [" + command + "] (" + subMeasurement + ")";
                }
            }
        }

        if (unitsInAnswer.size() > subMeasurement) {
            return "tab" + tab + "_[" + command + "] (" + subMeasurement + ")" + unitsInAnswer.get(subMeasurement);
        } else {
            return "tab" + tab + "_[" + command + "] (" + subMeasurement + ")";
        }
    }

    private boolean addCheckBoxesForTab(Integer tab, String command, ArrayList<String> unitsInAnswer) {
        boolean newAdded = false;
        for (int j = 0; j < unitsInAnswer.size(); j++) {
            String nameForSeries = generateNameForSeries(tab, command, j, unitsInAnswer);
            if (!seriesVisibility.containSeries(nameForSeries)) {
                seriesVisibility.addSeries(nameForSeries);
                seriesNameToTabId.put(nameForSeries, tab);
                seriesNameToCommand.put(nameForSeries, command);
                seriesVisibility.getJBoxes().get(nameForSeries).addActionListener(e -> {
                    boolean isSelected = seriesVisibility.getJBoxes().get(nameForSeries).isSelected();
                    seriesVisibility.setVisibility(e.getActionCommand(), isSelected);
                    applySeriesVisibility();
                });
                newAdded = true;
            }
        }
        return newAdded;
    }


    private void updateControlPanel() {
        for (JCheckBox cb : seriesVisibility.getJBoxes().values()) {
            if (addedToPanel.add(cb)) {
                selectors.add(cb);
            }
        }
        selectors.revalidate();
        selectors.repaint();
    }

    private synchronized void getLastData() {
        detectStabilityChanges();
        updateCB();
        ensureAllSeriesExist();
        addNewPointsToSeries();
        applySeriesVisibility();
        updateLastReceivedValue();
    }

    private void ensureAllSeriesExist() {
        AnswerStorage answerStorage = SpringContextHolder.getBean(AnswerStorage.class);
        int maxItems = limitDataCheckBox.isSelected() ? range : Integer.MAX_VALUE;
        for (Integer tab : answerStorage.getTabsInStorage()) {
            String stableCmd = answerStorage.getStableCommand(tab);
            if (stableCmd == null) continue;

            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForSelectedClientOrTab(tab);
            if (unitsInAnswer == null) continue;

            int fieldCount = unitsInAnswer.size();
            TimeSeries[] refs = new TimeSeries[fieldCount];
            for (int j = 0; j < fieldCount; j++) {
                String seriesName = generateNameForSeries(tab, stableCmd, j, unitsInAnswer);
                if (collection.getSeries(seriesName) == null) {
                    TimeSeries ts = new TimeSeries(seriesName);
                    ts.setMaximumItemCount(maxItems);
                    collection.addSeries(ts);
                    seriesNameToTabId.put(seriesName, tab);
                    seriesNameToCommand.put(seriesName, stableCmd);
                }
                refs[j] = collection.getSeries(seriesName);
            }
            cachedSeriesRefs.computeIfAbsent(tab, k -> new HashMap<>()).put(stableCmd, refs);
        }
    }

    private void addNewPointsToSeries() {
        AnswerStorage answerStorage = SpringContextHolder.getBean(AnswerStorage.class);
        for (Integer tab : answerStorage.getTabsInStorage()) {
            String stableCmd = answerStorage.getStableCommand(tab);
            if (stableCmd == null) continue;

            long fromTime = lastProcessedTime.getOrDefault(tab, 0L);
            final long[] maxTime = {fromTime};

            TimeSeries[] seriesRefs = cachedSeriesRefs.getOrDefault(tab, Collections.emptyMap()).get(stableCmd);
            if (seriesRefs == null) continue;
            int fieldCount = seriesRefs.length;

            SpringContextHolder.getBean(GraphDataRepository.class).forEachHistoryPoint(tab, stableCmd, point -> {
                if (point.getEpochMilli() > fromTime) {
                    Millisecond ms = point.toJFreeMillisecond();
                    for (int j = 0; j < point.getFieldCount() && j < fieldCount; j++) {
                        TimeSeries ts = seriesRefs[j];
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
        AnswerStorage answerStorage = SpringContextHolder.getBean(AnswerStorage.class);
        JFreeChart chart = chartPanel.getChart();
        if (chart == null) return;
        XYPlot plot = chart.getXYPlot();
        if (plot == null) return;
        XYItemRenderer renderer = plot.getRenderer();
        if (renderer == null) return;

        for (int i = 0; i < collection.getSeriesCount(); i++) {
            String seriesName = (String) collection.getSeriesKey(i);
            Integer tabId = seriesNameToTabId.get(seriesName);
            String seriesCommand = seriesNameToCommand.get(seriesName);

            boolean userVisible = seriesVisibility.isVisible(seriesName);
            boolean commandVisible = false;
            if (tabId != null && seriesCommand != null) {
                String stableCmd = answerStorage.getStableCommand(tabId);
                commandVisible = (stableCmd != null && seriesCommand.equals(stableCmd));
            }

            boolean visible = userVisible && commandVisible;
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
        AnswerStorage answerStorage = SpringContextHolder.getBean(AnswerStorage.class);
        StringBuilder sb = new StringBuilder(256);
        for (Integer tab : answerStorage.getTabsInStorage()) {
            String stableCmd = answerStorage.getStableCommand(tab);
            if (stableCmd == null) continue;

            ArrayList<String> unitsInAnswer = ansLoader.getUnitsArrayForSelectedClientOrTab(tab);
            if (unitsInAnswer == null) continue;

            for (int j = 0; j < unitsInAnswer.size(); j++) {
                String seriesName = generateNameForSeries(tab, stableCmd, j, unitsInAnswer);
                if (seriesVisibility.isVisible(seriesName)) {
                    TimeSeries ts = collection.getSeries(seriesName);
                    if (ts != null && ts.getItemCount() > 0) {
                        double lastVal = ts.getValue(ts.getItemCount() - 1).doubleValue();
                        sb.append('[');
                        appendDecimal(sb, lastVal);
                        sb.append("] ").append(seriesName).append("   ");
                    }
                }
            }
        }
        lastReceivedValue.setText(sb.toString());
    }

    private static void appendDecimal(StringBuilder sb, double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            sb.append(value);
            return;
        }
        boolean negative = value < 0;
        if (negative) {
            value = -value;
            sb.append('-');
        }

        long whole = (long) value;
        long frac = Math.round((value - whole) * 10000);
        if (frac >= 10000) {
            whole++;
            frac = 0;
        }

        sb.append(whole).append('.');
        if (frac < 1000) sb.append('0');
        if (frac < 100) sb.append('0');
        if (frac < 10) sb.append('0');
        sb.append(frac);
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
