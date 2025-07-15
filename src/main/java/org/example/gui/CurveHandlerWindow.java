package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.gui.curve.CurveData;
import org.example.gui.curve.CurveDataTypes;
import org.example.gui.curve.CurveMetaData;
import org.example.gui.curve.CurveStorage;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class CurveHandlerWindow extends JDialog {
    private Logger log = null;

    private JPanel comConnection;
    private JComboBox jcbComPortNumber;
    private JComboBox jcbComBarity;
    private JComboBox jcbComSpeed;
    private JButton jbOpenComPort;
    private JTable jtbFilePreviewRead;
    private JPanel jpnGraph;
    private JButton jbtSelectFile;
    private JPanel mainPane;
    private JPanel fileSelect;
    private JPanel jpTableRead;
    private JScrollPane jspForTableRead;
    private JLabel jlbSelectedFile;
    private JPanel jpOpenedCurvePreview;
    private JPanel jpCurveInformation;
    private JPanel jpFields;
    private JPanel jpCurveName;
    private JTextField jtfCurveName;
    private JPanel jpSerialNumber;
    private JTextField jvtSerialNumber;
    private JPanel jpPointLimit;
    private JTextField jvtPointLimit;
    private JPanel jpCurveFormat;
    private JComboBox jcbCurveFormat;
    private JPanel jpTemperatCoeff;
    private JTextField jtfTemperatureCoefficient;
    private JLabel TermKoefs;
    private JPanel jpNumberOfBreakpoints;
    private JLabel jlNumberOfBreakpoints;
    private JTextField jtfNumberOfBreakpoints;
    private JButton jbCloseComPort;
    private JPanel jpFileSave;
    private JComboBox jcbFileExtension;
    private JButton jbtSaveFile;
    private JComboBox jcbMemoryAddres;
    private JComboBox jcbNewOrOld;
    private JButton jbtWrite;
    private JPanel jpFileWrite;
    private JTextField jtfMeasured;
    private JTextField jtfRealMeans;
    private JButton jbtCalculate;
    private JPanel jpEditGraph;
    private JPanel jpGraphActions;
    private JButton jbtClearGraph;
    private JButton jbtAddReadetPoly;
    private JButton jbtAddCalculatedPoly;
    private JPanel jpEditedCurvePreview;
    private JLabel jlbCurveNameEdited;
    private JTextField jtfCurveNameEdited;
    private JLabel jlbSerialNumberEdited;
    private JTextField jtfSerialNumberEdited;
    private JLabel jlpPointsLimitEdited;
    private JTextField jtfPointsLimitEdited;
    private JLabel jlbCurveFormatEdited;
    private JComboBox jcbCurveFormatEdited;
    private JLabel jlbTermKoefTypeEdited;
    private JTextField jtfTermKoefTypeEdited;
    private JLabel jlbPointsCountEdited;
    private JTextField jtfPointsCountEdited;
    private XYSeries readetSeries = new XYSeries("File Data");
    private XYSeries calculateSeries = new XYSeries("Calculated Data");
    private XYSeriesCollection dataset = new XYSeriesCollection(readetSeries);
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private CurveStorage curveStorage = new CurveStorage();

    public CurveHandlerWindow() {

        $$$setupUI$$$();
        log = Logger.getLogger(CurveHandlerWindow.class);
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPane);
        jbtSelectFile.addActionListener(this::handleFileSelection);
        jbtClearGraph.addActionListener(this::clearGraph);
        jbtAddReadetPoly.addActionListener(this::paintReadetPoly);
        //jbtAddCalculatedPoly.addActionListener(this::paintCalculatedPoly);

        initChart();
    }

    // Обработчик выбора файла
    private void handleFileSelection(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();

        // Настройка фильтра (пример для текстовых файлов)

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Poly files (*.curve, *.340)", "curve", "340");

        fileChooser.setFileFilter(filter);

        // Показать диалог выбора
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            // 1. Запомнить путь (можно использовать в других частях программы)
            System.out.println("Выбран файл: " + filePath);
            jlbSelectedFile.setText(filePath);

            // 2. Обработать файл
            processSelectedFile(selectedFile);
        }
    }

    public void clearGraph(ActionEvent e) {
        readetSeries.clear();
        calculateSeries.clear();
        repaintChart();
    }

    public void paintReadetPoly(ActionEvent e) {

    }

    private void processSelectedFile(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            List<String[]> tableData = new ArrayList<>();
            String line;
            int lineCount = 0;
            StringBuilder sb = new StringBuilder();
            int needLineScip = 5;
            if (file.getName().endsWith(".340")) {
                needLineScip = 9;
            } else if (file.getName().endsWith(".curve")) {
                needLineScip = 6;
            }
            // Пропуск заголовков (первые 5 строк)
            while ((line = reader.readLine()) != null && lineCount < needLineScip) {
                sb.append(line);
                sb.append("\n");
                lineCount++;
            }

            // Чтение данных
            int i = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = new String[2];
                if (file.getName().endsWith(".340")) {
                    //log.info("Ориентируюсь на позиции в строке");
                    if (line.length() < 26) {
                        log.warn("Слишком короткая строка" + line.length());
                        i = 0;
                        continue;
                    }
                    if (!(line.charAt(3) == ' ')) {
                        log.warn("Неверное положение пробела после номера строки: " + line.charAt(3));
                        i = 0;
                        continue;
                    }
                    String lineNumber = line.substring(0, 3);
                    lineNumber = lineNumber.trim();

                    String units = line.substring(5, 19);
                    units = units.trim();

                    String temperature = line.substring(19, 26);
                    temperature = temperature.trim();

                    parts[0] = units;
                    parts[1] = temperature;
                    //System.out.println("Line number: + " + i + "Readet line: " + lineNumber + "Line: " + line + " units " + units + " temp " + temperature);
                } else {
                    //log.info("Ориентируюсь на нарезку");
                    parts = line.split("\t"); // Разделитель - табуляция

                    //System.out.println("Line number: + " + i + "Line: " + line + " units " + parts[0] + " temp " + parts[1]);
                }
                i++;

                if (parts.length == 2) {
                    tableData.add(parts);
                } else {
                    System.out.println("Scip: " + parts.length);
                }
            }
            CurveMetaData curveMetaData = new CurveMetaData();

            curveMetaData = updateCurveMetaDara(sb.toString());
            updateOpenedCurveInfo(curveMetaData);
            CurveData curveData = new CurveData();
            curveData.setCurveMetaData(curveMetaData);
            fillCurveData(curveData, tableData);

            curveStorage.addOrUpdateCurve("Opened", curveData);
            // Обновление таблицы
            updateTableOpenedFile(curveData);

            // Построение графика (ваша реализация)
            buildReadData(curveStorage.getCurve("Opened"));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Ошибка чтения файла: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initChart() {
        chart = createChart(dataset);
        chartPanel = new ChartPanel(chart);
        chartPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartPanel.setBackground(Color.white);

        // Установите layout для jpnGraph
        jpnGraph.setLayout(new BorderLayout());
        jpnGraph.add(chartPanel, BorderLayout.CENTER);
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

        // Настройка рендерера
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        plot.setRenderer(renderer);

        return chart;
    }

    private CurveMetaData updateCurveMetaDara(String fileHeader) {
        CurveMetaData curveMetaData = new CurveMetaData();
        if (fileHeader == null || fileHeader.isEmpty()) {
            log.warn("fileHeader == null || fileHeader.isEmpty()");
            return null;
        }

        String[] fileStrings = fileHeader.split("\n");
        if (fileStrings.length == 0) {
            log.warn("fileStrings.length == 0");
        }

        for (String fileString : fileStrings) {
            System.out.println("fileString: " + fileString);
            if (fileString.startsWith("Sensor Model:")) {
                String value = fileString.replace("Sensor Model:", "").trim();
                curveMetaData.setSensorModel(value);
            } else if (fileString.startsWith("Serial Number:")) {
                curveMetaData.setSerialNumber(fileString.replace("Serial Number:", "").trim());
            } else if (fileString.startsWith("Data Format:")) {
                String dataFormat = fileString.replace("Data Format:", "");
                CurveDataTypes dataTypes = null;
                if (dataFormat.contains("\t")) {
                    dataFormat = dataFormat.trim();
                    dataFormat = dataFormat.replace("    ", "\t");
                    try {
                        Integer foundedNumber = Integer.parseInt(dataFormat.split("\t")[0]);
                        System.out.println("foundedNumber: " + foundedNumber);
                        CurveDataTypes[] arrayCurves = CurveDataTypes.values();
                        dataTypes = arrayCurves[foundedNumber];
                    } catch (NumberFormatException ex) {
                        log.warn(ex.getMessage());
                    }
                } else {
                    dataTypes = CurveDataTypes.V_V_K;
                    log.warn("Set default data type");
                }
                curveMetaData.setDataFormat(dataTypes);
            } else if (fileString.startsWith("SetPoint Limit:")) {
                String setPointsLimitString = fileString.replace("SetPoint Limit:", "").trim();
                Integer setPointLimit = 800;
                if (setPointsLimitString.isEmpty()) {
                    log.warn("Set points limit is empty");
                    curveMetaData.setSetPointLimit(setPointLimit);
                    continue;
                }

                if (setPointsLimitString.contains("      ")) {
                    setPointsLimitString = setPointsLimitString.split("      ")[0];
                    setPointsLimitString = setPointsLimitString.trim();
                }
                try {
                    setPointLimit = Integer.parseInt(setPointsLimitString);
                } catch (NumberFormatException ex) {
                    log.warn(ex.getMessage());
                    curveMetaData.setSetPointLimit(setPointLimit);
                    continue;
                }
                curveMetaData.setSetPointLimit(setPointLimit);
            } else if (fileString.startsWith("Temperature coefficient:")) {
                System.out.println("Temperature coefficient:");
                String temperatureCoefficient = fileString.replace("Temperature coefficient:", "").trim();
                curveMetaData.setTemperatureCoefficient(temperatureCoefficient);
            } else if (fileString.startsWith("Number of Breakpoints:")) {
                String numberOfBreakpoints = fileString.replace("Number of Breakpoints:", "").trim();
                Integer numberOfBreakpointsInt = 160;
                try {
                    numberOfBreakpointsInt = Integer.parseInt(numberOfBreakpoints);
                } catch (NumberFormatException ex) {
                    log.warn(ex.getMessage());
                    curveMetaData.setNumberOfBreakpoints(numberOfBreakpointsInt);
                    continue;
                }
                curveMetaData.setNumberOfBreakpoints(numberOfBreakpointsInt);
            }
        }
        return curveMetaData;
    }

    private void fillCurveFormats() {
        jcbCurveFormat.removeAllItems();
        for (CurveDataTypes dataType : CurveDataTypes.values()) {
            jcbCurveFormat.addItem(dataType);
        }
    }

    private void updateOpenedCurveInfo(CurveMetaData curveMetaData) {
        fillCurveFormats();
        if (curveMetaData.getSensorModel() != null)
            jtfCurveName.setText(curveMetaData.getSensorModel());
        if (curveMetaData.getSerialNumber() != null)
            jvtSerialNumber.setText(curveMetaData.getSerialNumber());
        if (curveMetaData.getDataFormat() != null)
            jcbCurveFormat.setSelectedItem(curveMetaData.getDataFormat());
        if (curveMetaData.getSetPointLimit() != null)
            jvtPointLimit.setText(curveMetaData.getSetPointLimit().toString());
        if (curveMetaData.getTemperatureCoefficient() != null)
            jtfTemperatureCoefficient.setText(curveMetaData.getTemperatureCoefficient());
        if (curveMetaData.getNumberOfBreakpoints() != null)
            jtfNumberOfBreakpoints.setText(curveMetaData.getNumberOfBreakpoints().toString());
    }

    private void updateTableOpenedFile(CurveData curveData) {
        // Заголовки столбцов
        String[] header = curveData.getCurveMetaData().getDataFormat().getName().split("vs");

        //Заполнение таблицы
        // Добавляем новые точки данных через лямбдочку
        ArrayList<String[]> rowsList = new ArrayList<>();
        curveData.getCurvePoints().forEach(it -> {
            rowsList.add(new String[]{String.valueOf(it.getKey()), String.valueOf(it.getValue())});
        });

        // Установка модели таблицы
        jtbFilePreviewRead.setModel(new DefaultTableModel(rowsList.toArray(new String[0][0]), header));
    }


    private void repaintChart() {
        if (chartPanel == null) {
            initChart();
        }
        // Обновляем график
        chartPanel.repaint();

        // Автомасштабирование
        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);
        plot.getRangeAxis().setAutoRange(true);
    }

    private void fillCurveData(CurveData curveData, List<String[]> data) {
        for (String[] row : data) {
            curveData.addCurvePointFromString(row[0], row[1]);
        }
    }

    private void buildReadData(CurveData curveData) {
        // Очищаем существующие данные
        readetSeries.clear();
        // Добавляем новые точки данных через лямбдочку
        curveData.getCurvePoints().forEach(it -> {
            readetSeries.add(it.getKey(), it.getValue());
        });
        // Перересовываем график
        repaintChart();
    }

    private void buildCalculatedData(List<String[]> data) {
        // Очищаем существующие данные
        calculateSeries.clear();

        // Добавляем новые точки данных
        for (String[] row : data) {
            try {
                double measurement = Double.parseDouble(row[0]);
                double temperature = Double.parseDouble(row[1]);
                calculateSeries.add(measurement, temperature);
            } catch (NumberFormatException ignored) {
                // Пропускаем некорректные строки
            }
        }
        repaintChart();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPane = new JPanel();
        mainPane.setLayout(new GridLayoutManager(6, 4, new Insets(0, 0, 0, 0), -1, -1));
        comConnection = new JPanel();
        comConnection.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(comConnection, new GridConstraints(0, 0, 6, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbComPortNumber = new JComboBox();
        comConnection.add(jcbComPortNumber, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComBarity = new JComboBox();
        comConnection.add(jcbComBarity, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComSpeed = new JComboBox();
        comConnection.add(jcbComSpeed, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbOpenComPort = new JButton();
        jbOpenComPort.setText("connect");
        comConnection.add(jbOpenComPort, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        comConnection.add(spacer1, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jbCloseComPort = new JButton();
        jbCloseComPort.setText("Disconnect");
        comConnection.add(jbCloseComPort, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileSelect = new JPanel();
        fileSelect.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(fileSelect, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbtSelectFile = new JButton();
        jbtSelectFile.setText("Select");
        fileSelect.add(jbtSelectFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jlbSelectedFile = new JLabel();
        jlbSelectedFile.setText(" ");
        fileSelect.add(jlbSelectedFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        fileSelect.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpnGraph = new JPanel();
        jpnGraph.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpnGraph, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(200, 200), new Dimension(600, 400), new Dimension(800, 1000), 0, false));
        jpOpenedCurvePreview = new JPanel();
        jpOpenedCurvePreview.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpOpenedCurvePreview, new GridConstraints(0, 1, 6, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, 300), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCurveInformation = new JPanel();
        jpCurveInformation.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpOpenedCurvePreview.add(jpCurveInformation, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpFields = new JPanel();
        jpFields.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformation.add(jpFields, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCurveName = new JPanel();
        jpCurveName.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFields.add(jpCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Curve Name");
        jpCurveName.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCurveName = new JTextField();
        jtfCurveName.setEditable(false);
        jtfCurveName.setEnabled(false);
        jpCurveName.add(jtfCurveName, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        jpCurveName.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpSerialNumber = new JPanel();
        jpSerialNumber.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFields.add(jpSerialNumber, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Serial Number");
        jpSerialNumber.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jvtSerialNumber = new JTextField();
        jvtSerialNumber.setEditable(false);
        jvtSerialNumber.setEnabled(false);
        jpSerialNumber.add(jvtSerialNumber, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        jpSerialNumber.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpPointLimit = new JPanel();
        jpPointLimit.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFields.add(jpPointLimit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Point Limit");
        jpPointLimit.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jvtPointLimit = new JTextField();
        jvtPointLimit.setEditable(false);
        jvtPointLimit.setEnabled(false);
        jpPointLimit.add(jvtPointLimit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer5 = new Spacer();
        jpPointLimit.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCurveFormat = new JPanel();
        jpCurveFormat.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFields.add(jpCurveFormat, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Curve Format");
        jpCurveFormat.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbCurveFormat = new JComboBox();
        jcbCurveFormat.setEnabled(false);
        jpCurveFormat.add(jcbCurveFormat, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer6 = new Spacer();
        jpCurveFormat.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpTemperatCoeff = new JPanel();
        jpTemperatCoeff.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFields.add(jpTemperatCoeff, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        TermKoefs = new JLabel();
        TermKoefs.setText("Term koef type");
        jpTemperatCoeff.add(TermKoefs, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfTemperatureCoefficient = new JTextField();
        jtfTemperatureCoefficient.setEditable(false);
        jtfTemperatureCoefficient.setEnabled(false);
        jpTemperatCoeff.add(jtfTemperatureCoefficient, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer7 = new Spacer();
        jpTemperatCoeff.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpNumberOfBreakpoints = new JPanel();
        jpNumberOfBreakpoints.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFields.add(jpNumberOfBreakpoints, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlNumberOfBreakpoints = new JLabel();
        jlNumberOfBreakpoints.setText("Points Count");
        jpNumberOfBreakpoints.add(jlNumberOfBreakpoints, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfNumberOfBreakpoints = new JTextField();
        jtfNumberOfBreakpoints.setEditable(false);
        jtfNumberOfBreakpoints.setEnabled(false);
        jpNumberOfBreakpoints.add(jtfNumberOfBreakpoints, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer8 = new Spacer();
        jpNumberOfBreakpoints.add(spacer8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpTableRead = new JPanel();
        jpTableRead.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpOpenedCurvePreview.add(jpTableRead, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jspForTableRead = new JScrollPane();
        jpTableRead.add(jspForTableRead, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 200), new Dimension(200, 350), new Dimension(250, 400), 0, false));
        jtbFilePreviewRead = new JTable();
        jspForTableRead.setViewportView(jtbFilePreviewRead);
        final Spacer spacer9 = new Spacer();
        jpTableRead.add(spacer9, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer10 = new Spacer();
        jpTableRead.add(spacer10, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer11 = new Spacer();
        jpOpenedCurvePreview.add(spacer11, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpFileSave = new JPanel();
        jpFileSave.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpFileSave, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbFileExtension = new JComboBox();
        jpFileSave.add(jcbFileExtension, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer12 = new Spacer();
        jpFileSave.add(spacer12, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jbtSaveFile = new JButton();
        jbtSaveFile.setText("Save File");
        jpFileSave.add(jbtSaveFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpFileWrite = new JPanel();
        jpFileWrite.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpFileWrite, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbMemoryAddres = new JComboBox();
        jpFileWrite.add(jcbMemoryAddres, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbNewOrOld = new JComboBox();
        jpFileWrite.add(jcbNewOrOld, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtWrite = new JButton();
        jbtWrite.setText("Write");
        jpFileWrite.add(jbtWrite, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpEditGraph = new JPanel();
        jpEditGraph.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpEditGraph, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jtfMeasured = new JTextField();
        jpEditGraph.add(jtfMeasured, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jtfRealMeans = new JTextField();
        jpEditGraph.add(jtfRealMeans, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jbtCalculate = new JButton();
        jbtCalculate.setText("Calculate");
        jpEditGraph.add(jbtCalculate, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpGraphActions = new JPanel();
        jpGraphActions.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpGraphActions, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbtClearGraph = new JButton();
        jbtClearGraph.setText("Clear Graph");
        jpGraphActions.add(jbtClearGraph, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtAddCalculatedPoly = new JButton();
        jbtAddCalculatedPoly.setText("Add Calculated Poly");
        jpGraphActions.add(jbtAddCalculatedPoly, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtAddReadetPoly = new JButton();
        jbtAddReadetPoly.setText("Add Readet Poly");
        jpGraphActions.add(jbtAddReadetPoly, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpEditedCurvePreview = new JPanel();
        jpEditedCurvePreview.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpEditedCurvePreview, new GridConstraints(0, 2, 6, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpEditedCurvePreview.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCurveNameEdited = new JLabel();
        jlbCurveNameEdited.setText("Curve Name");
        panel3.add(jlbCurveNameEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCurveNameEdited = new JTextField();
        jtfCurveNameEdited.setEditable(true);
        jtfCurveNameEdited.setEnabled(true);
        panel3.add(jtfCurveNameEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer13 = new Spacer();
        panel3.add(spacer13, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbSerialNumberEdited = new JLabel();
        jlbSerialNumberEdited.setText("Serial Number");
        panel4.add(jlbSerialNumberEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfSerialNumberEdited = new JTextField();
        jtfSerialNumberEdited.setEditable(true);
        jtfSerialNumberEdited.setEnabled(true);
        panel4.add(jtfSerialNumberEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer14 = new Spacer();
        panel4.add(spacer14, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel5, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlpPointsLimitEdited = new JLabel();
        jlpPointsLimitEdited.setText("Point Limit");
        panel5.add(jlpPointsLimitEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfPointsLimitEdited = new JTextField();
        jtfPointsLimitEdited.setEditable(true);
        jtfPointsLimitEdited.setEnabled(true);
        panel5.add(jtfPointsLimitEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer15 = new Spacer();
        panel5.add(spacer15, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel6, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCurveFormatEdited = new JLabel();
        jlbCurveFormatEdited.setText("Curve Format");
        panel6.add(jlbCurveFormatEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbCurveFormatEdited = new JComboBox();
        jcbCurveFormatEdited.setEditable(true);
        jcbCurveFormatEdited.setEnabled(true);
        panel6.add(jcbCurveFormatEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer16 = new Spacer();
        panel6.add(spacer16, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbTermKoefTypeEdited = new JLabel();
        jlbTermKoefTypeEdited.setText("Term koef type");
        panel7.add(jlbTermKoefTypeEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfTermKoefTypeEdited = new JTextField();
        jtfTermKoefTypeEdited.setEditable(true);
        jtfTermKoefTypeEdited.setEnabled(true);
        panel7.add(jtfTermKoefTypeEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer17 = new Spacer();
        panel7.add(spacer17, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel8, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbPointsCountEdited = new JLabel();
        jlbPointsCountEdited.setText("Points Count");
        panel8.add(jlbPointsCountEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfPointsCountEdited = new JTextField();
        jtfPointsCountEdited.setEditable(true);
        jtfPointsCountEdited.setEnabled(true);
        panel8.add(jtfPointsCountEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer18 = new Spacer();
        panel8.add(spacer18, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel9, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel9.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 200), new Dimension(200, 350), new Dimension(250, 400), 0, false));
        final JTable table1 = new JTable();
        scrollPane1.setViewportView(table1);
        final Spacer spacer19 = new Spacer();
        panel9.add(spacer19, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer20 = new Spacer();
        panel9.add(spacer20, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer21 = new Spacer();
        panel1.add(spacer21, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer22 = new Spacer();
        panel1.add(spacer22, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPane;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
