package org.example.gui.curve;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.ParityList;
import org.example.utilites.ListenerUtils;
import org.example.utilites.MyUtilities;
import org.example.utilites.properties.MyProperties;
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
import java.io.*;
import java.net.ConnectException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;


public class CurveHandlerWindow extends JFrame implements Rendeble {
    private Logger log = null;
    private final MyProperties prop;

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
    private JTextField jtfKelvinMeasured;
    private JTextField jtfVoltsMeasured;
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
    private JTable jtbFilePreviewEdite;
    private JLabel jlbSelectedFileEdited;
    private JPanel jpnColorStatus;
    private JLabel jlbStatus;
    private JPanel jpDataTranserProggres;
    private JProgressBar jpbCommandSending;
    private JLabel jlbNearestPoint;
    private JLabel jlbAddingVolts;
    private JPanel jpCurveInformationEdited;
    private JPanel jpTableEdite;
    private XYSeries readetSeries = new XYSeries("File Data");
    private XYSeries editedSeries = new XYSeries("Calculated Data");
    private XYSeriesCollection dataset = new XYSeriesCollection();
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private CurveStorage curveStorage = new CurveStorage();

    private SerialPort comPort = null;

    String filePath;

    /*
    ToDo
     1. Изменение параметров редактируемой кривой по изменению в полях ввода +
     2. Сохранение в файл +
     3. Открытие и проверка соединения по ком-порту +
     4. Запись полинома в прибор +
     *5. Запоминание последнего открытого файла
     *6. Чтение и создание полинома из прибора
     Потренировать Consumer
     */
    public CurveHandlerWindow(MyProperties prop) {

        $$$setupUI$$$();
        log = Logger.getLogger(CurveHandlerWindow.class);
        this.prop = prop;
        setContentPane(mainPane);
        updateComPortList();
        //Заполнение полей комбо-боксов
        initComboBox(jcbComBarity,
                ParityList.values(),
                i -> String.valueOf(ParityList.getNameLikeArray(i)));

        initComboBox(jcbComSpeed,
                BaudRatesList.values(),
                i -> String.valueOf(BaudRatesList.getNameLikeArray(i)));


        jbtSelectFile.addActionListener(this::handleFileSelection);
        jbtClearGraph.addActionListener(this::clearGraph);
        jbtAddReadetPoly.addActionListener(this::paintReadetPoly);
        jbtAddCalculatedPoly.addActionListener(this::paintEditedPoly);
        jbtCalculate.addActionListener(this::calculateActionHandler);
        jbOpenComPort.addActionListener(this::openPortActionHandler);
        jbtWrite.addActionListener(this::writeInDeviceHandler);

        ListenerUtils.addDocumentListener(jtfCurveNameEdited, this::updateOnEditeCurveName);
        ListenerUtils.addKeyListener(jtfCurveNameEdited, this::updateOnEnterCurveName);

        ListenerUtils.addDocumentListener(jtfSerialNumberEdited, this::updateOnEditeCurveSerialNumber);
        ListenerUtils.addKeyListener(jtfSerialNumberEdited, this::updateOnEnterCurveSerialNumber);

        ListenerUtils.addDocumentListener(jtfPointsLimitEdited, this::updateOnEditeCurvePointsLimit);
        ListenerUtils.addKeyListener(jtfPointsLimitEdited, this::updateOnEnterCurvePointsLimit);

        jcbCurveFormatEdited.addActionListener(this::updateOnEditeCurveFormat);

        ListenerUtils.addDocumentListener(jtfTermKoefTypeEdited, this::updateOnEditeCurveTermKoefType);
        ListenerUtils.addKeyListener(jtfTermKoefTypeEdited, this::updateOnEnterCurveTermKoefType);

        ListenerUtils.addDocumentListener(jtfPointsCountEdited, this::updateOnEditeCurvePointsCount);
        ListenerUtils.addKeyListener(jtfPointsCountEdited, this::updateOnEnterCurvePointsCount);

        jbtSaveFile.addActionListener(this::saveFileActionHandler);
        fillCurveFormats();
        fillSomeGuiElements();
        initChart();
    }

    private void openPortActionHandler(ActionEvent actionEvent) {
        Integer selectedBaudRate = null;
        Integer selectedParity = null;

        jlbStatus.setText("Открываю");
        if (comPort != null) {
            if (comPort.isOpen()) {
                comPort.closePort();
                jlbStatus.setText("Переоткрываю...");
            }
        }
        try {
            // Получаем выбранные параметры
            selectedBaudRate = BaudRatesList.getNameLikeArray(jcbComSpeed.getSelectedIndex());
            log.info("SelectedBaudRate = " + selectedBaudRate);
            selectedParity = ParityList.getValueKikeArray(jcbComBarity.getSelectedIndex());
            log.info("SelectedParity = " + selectedParity);

            SerialPort[] ports = SerialPort.getCommPorts();
            if (ports.length == 0 || jcbComPortNumber.getSelectedIndex() >= ports.length) {
                jlbStatus.setText("Выбранный порт не найден в системе о.о");
                return;
            }

            comPort = ports[jcbComPortNumber.getSelectedIndex()];
            if (comPort == null) {
                jlbStatus.setText("Выбранный порт оказался null");
                return;
            }
        } catch (Exception e) {
            jlbStatus.setText("Ошибка разбора параметров");
            log.error("Ошибка разбора параметров", e);
            JOptionPane.showMessageDialog(
                    null,
                    "Ошибка разбора параметров: " + e.getMessage(),
                    "Ошибка разбора параметров",
                    JOptionPane.ERROR_MESSAGE
            );
        }
        try {
            // Проверка на заполненность
            if (selectedBaudRate == null || selectedParity == null || comPort == null) {
                throw new IllegalArgumentException("Не все параметры порта выбраны");
            }


            // Устанавливаем параметры соединения
            comPort.setComPortParameters(
                    selectedBaudRate,  // Скорость
                    8,                               // Биты данных (фиксировано)
                    SerialPort.ONE_STOP_BIT,         // Стоп-биты (фиксировано)
                    selectedParity       // Четность
            );

            // Устанавливаем таймауты (значения по умолчанию)
            int readTimeout = 1000;  // 1 секунда
            int writeTimeout = 1000;  // 1 секунда
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_READ_SEMI_BLOCKING,
                    readTimeout,
                    writeTimeout
            );

            // Открываем порт
            boolean opened = comPort.openPort(readTimeout);

            if (opened) {
                jlbStatus.setText("Порт " + comPort.getDescriptivePortName() + " открыт");
            } else {
                throw new ConnectException("Не удалось открыть порт. Код ошибки: " + comPort.getLastErrorCode());
            }

        } catch (Exception e) {
            jlbStatus.setText("Ошибка открытия порта");
            log.error("Ошибка при открытии COM-порта", e);
            JOptionPane.showMessageDialog(
                    null,
                    "Ошибка открытия порта: " + e.getMessage(),
                    "Ошибка подключения",
                    JOptionPane.ERROR_MESSAGE
            );
        }


        jlbStatus.setText(checkDeviceConnection());
    }

    private String checkDeviceConnection() {
        if (comPort != null && comPort.isOpen()) {
            CurveDeviceCommander curveDeviceCommander = new CurveDeviceCommander(comPort);
            String status = curveDeviceCommander.pingDevice();
            jlbStatus.setText(status);
            return status;
        } else {
            return "Ошибка 001. Порт не открыт.";
        }
    }

    private void writeInDeviceHandler(ActionEvent actionEvent) {
        CurveDeviceCommander devCommander = new CurveDeviceCommander(comPort);
        //JPanel jpDataTranserProggres eже создана и инициализирована в Intelige добавить прогресс бар
        if (!devCommander.isPortConsistent()) {
            log.warn("Порт не инициализирован");
            jlbStatus.setText("Порт не нициализирован");
            return;
        }
        String status = devCommander.pingDevice();
        if ("".equalsIgnoreCase(status)) {
            log.warn(status);
            jlbStatus.setText(status);
            return;
        }
        // Определение адреса памяти. Доступны адреса с 21 по 64.
        int memoryAddress = jcbMemoryAddres.getSelectedIndex() + 21;
        // Определение кривой для записи
        String curveType = (jcbNewOrOld.getSelectedIndex() == 0) ? "Opened" : "Edited";
        log.info("Запись {" + curveType + "} файла");

        if (!curveStorage.isContains(curveType)) {
            jlbStatus.setText("Данные для записи " + curveType + " кривой не найдены");
            return;
        }

        // Получение данных кривой
        CurveData curve = curveStorage.getCurve(curveType);

        // Настройка прогресс-бара
        jpDataTranserProggres.setVisible(true);
        jpbCommandSending.setValue(0);
        jpbCommandSending.setStringPainted(true);

        new Thread(() -> {
            try {
                devCommander.writeCurveToDevice(curve, memoryAddress, progress -> {
                    // Обновление UI в потоке EDT
                    SwingUtilities.invokeLater(() -> {
                        jpbCommandSending.setValue(progress);
                        jlbStatus.setText("Запись... " + progress + "%");
                    });
                });

                SwingUtilities.invokeLater(() -> {
                    jlbStatus.setText("Запись завершена успешно!");
                    jpbCommandSending.setValue(100);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    jlbStatus.setText("Ошибка записи: " + ex.getMessage());
                    log.error("Ошибка записи в устройство", ex);
                });
            }
        }).start();
    }

    private void updateOnEditeCurveFormat(ActionEvent actionEvent) {
        System.out.println("Set " + ((CurveDataTypes) jcbCurveFormatEdited.getSelectedItem()));
        if (curveStorage.isContains("Edited"))
            curveStorage.getCurve("Edited").getCurveMetaData().setDataFormat((CurveDataTypes) jcbCurveFormatEdited.getSelectedItem());
    }

    private void updateOnEditeCurvePointsCount() {
        Integer count = null;
        try {
            count = Integer.parseInt(jtfPointsCountEdited.getText());
        } catch (NumberFormatException e) {
            jtfPointsCountEdited.setText(String.valueOf(curveStorage.getCurve("Edited").getCurveMetaData().getNumberOfBreakpoints()));
            log.warn("Исключение во время получения числа из строки CurvePointsCount" + e.getMessage());
            //e.printStackTrace();
        }
        if (count != null) {
            curveStorage.getCurve("Edited").getCurveMetaData().setNumberOfBreakpoints(Integer.parseInt(jtfPointsCountEdited.getText()));
        }
    }

    private void updateOnEnterCurveSerialNumber() {
        if (jtfSerialNumberEdited.getText() == null || jtfSerialNumberEdited.getText().isEmpty()) {
            log.warn("Попытка установки null-строки как серийный номер сенсора");
            jtfSerialNumberEdited.setText(curveStorage.getCurve("Edited").getCurveMetaData().getSerialNumber());
        } else {
            curveStorage.getCurve("Edited").getCurveMetaData().setSerialNumber(jtfSerialNumberEdited.getText());
        }
    }

    private void updateOnEditeCurveTermKoefType() {
        if (jtfTermKoefTypeEdited.getText() != null && !jtfTermKoefTypeEdited.getText().isEmpty()) {
            curveStorage.getCurve("Edited").getCurveMetaData().setTemperatureCoefficient(jtfTermKoefTypeEdited.getText());
        }
    }

    private void updateOnEnterCurvePointsLimit() {
        if (jtfPointsLimitEdited.getText() == null || jtfPointsLimitEdited.getText().isEmpty()) {
            log.warn("Попытка установки null-строки CurvePointsLimit");
            jtfPointsLimitEdited.setText(String.valueOf(curveStorage.getCurve("Edited").getCurveMetaData().getSetPointLimit()));
        } else {
            Integer num = null;
            try {
                num = Integer.parseInt(jtfPointsLimitEdited.getText());
            } catch (NumberFormatException ex) {
                jtfPointsLimitEdited.setText(String.valueOf(curveStorage.getCurve("Edited").getCurveMetaData().getSetPointLimit()));
                log.warn("Исключение во время получения числа из строки OnEnterCurvePointsLimit" + ex.getMessage());
                //ex.printStackTrace();
            }
            if (num != null) {
                curveStorage.getCurve("Edited").getCurveMetaData().setSetPointLimit(num);
            }
        }
    }

    private void updateOnEnterCurvePointsCount() {
        if (jtfPointsCountEdited.getText() == null || jtfPointsCountEdited.getText().isEmpty()) {
            log.warn("Попытка установки null-строки CountEdited");
            jtfPointsCountEdited.setText(String.valueOf(curveStorage.getCurve("Edited").getCurveMetaData().getNumberOfBreakpoints()));
        } else {
            Integer count = null;
            try {
                count = Integer.parseInt(jtfPointsCountEdited.getText());
            } catch (NumberFormatException e) {
                jtfPointsCountEdited.setText(String.valueOf(curveStorage.getCurve("Edited").getCurveMetaData().getNumberOfBreakpoints()));
                log.warn("Исключение во время получения числа из строки OnEnterCurvePointsCount" + e.getMessage());
                //e.printStackTrace();
            }
            if (count != null) {
                curveStorage.getCurve("Edited").getCurveMetaData().setNumberOfBreakpoints(Integer.parseInt(jtfPointsCountEdited.getText()));
            }
        }
    }

    private void updateOnEnterCurveTermKoefType() {
        if (jtfTermKoefTypeEdited.getText() == null || jtfTermKoefTypeEdited.getText().isEmpty()) {
            log.warn("Попытка установки null-строки как температурного коэффициента");
            jtfTermKoefTypeEdited.setText(curveStorage.getCurve("Edited").getCurveMetaData().getTemperatureCoefficient());
        } else {
            curveStorage.getCurve("Edited").getCurveMetaData().setTemperatureCoefficient(jtfTermKoefTypeEdited.getText());
        }
    }

    private void updateOnEditeCurvePointsLimit() {
        Integer num = null;
        try {
            num = Integer.parseInt(jlpPointsLimitEdited.getText());
        } catch (NumberFormatException e) {
            jlpPointsLimitEdited.setText(String.valueOf(curveStorage.getCurve("Edited").getCurveMetaData().getSetPointLimit()));
            log.warn("Исключение во время получения числа из строки OnEditeCurvePointsLimit" + e.getMessage());
        }
        if (num != null) {
            curveStorage.getCurve("Edited").getCurveMetaData().setSetPointLimit(num);
        }
    }

    private void updateOnEditeCurveSerialNumber() {
        if (jtfSerialNumberEdited.getText() != null && !jtfSerialNumberEdited.getText().isEmpty()) {
            curveStorage.getCurve("Edited").getCurveMetaData().setSerialNumber(jtfSerialNumberEdited.getText());
        }
    }

    private void updateOnEditeCurveName() {
        curveStorage.getCurve("Edited").getCurveMetaData().setSensorModel(jtfCurveNameEdited.getText());
    }

    private void updateOnEnterCurveName() {
        curveStorage.getCurve("Edited").getCurveMetaData().setSensorModel(jtfCurveNameEdited.getText());
    }


    // Обработчик выбора файла
    private void handleFileSelection(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();

        // Настройка фильтра (пример для текстовых файлов)

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "Poly files (*.curve, *.340)", "curve", "340");

        fileChooser.setFileFilter(filter);

        if (filePath != null) {
            fileChooser.setCurrentDirectory(new File(filePath));
        }
        // Показать диалог выбора
        int result = fileChooser.showOpenDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {

            File selectedFile = fileChooser.getSelectedFile();
            filePath = selectedFile.getAbsolutePath();

            // 1. Запомнить путь (можно использовать в других частях программы)
            System.out.println("Выбран файл: " + filePath);
            jlbSelectedFile.setText(filePath);

            // 2. Обработать файл
            processSelectedFile(selectedFile);
        }
    }

    public void clearGraph(ActionEvent e) {
        readetSeries.clear();
        editedSeries.clear();
        repaintChart();
    }

    public void calculateActionHandler(ActionEvent e) {
        String kelvinString = jtfKelvinMeasured.getText();
        kelvinString = kelvinString.trim();
        kelvinString = kelvinString.replaceAll(",", ".");

        String voltsString = jtfVoltsMeasured.getText();
        voltsString = voltsString.trim();
        voltsString = voltsString.replaceAll(",", ".");
        double measuredKelvin = Double.parseDouble(kelvinString);
        double measuredVolts = Double.parseDouble(voltsString);

        // 2. Проверка наличия открытой кривой
        if (!curveStorage.isContains("Opened")) {
            jlbStatus.setText("Открытая кривая не найдена");
            log.warn("Попытка расчета без открытой кривой");
            return;
        }

        // 3. Получение данных открытой кривой
        CurveData openedCurve = curveStorage.getCurve("Opened");
        List<Map.Entry<Double, Double>> openedPoints = openedCurve.getCurvePoints();

        // 4. Поиск ближайшей точки по температуре
        double minDiff = Double.MAX_VALUE; //ToDo Ограничение на отклонение
        Map.Entry<Double, Double> nearestPoint = null;

        for (Map.Entry<Double, Double> point : openedPoints) {
            double tempDiff = Math.abs(point.getValue() - measuredKelvin);
            if (tempDiff < minDiff) {
                minDiff = tempDiff;
                nearestPoint = point;
            }
        }

        // 5. Проверка найденной точки
        if (nearestPoint == null) {
            jlbStatus.setText("Ошибка: не найдено точек в кривой");
            log.error("Не удалось найти ближайшую точку в открытой кривой");
            return;
        }

        // 6. Расчет разницы напряжений
        double voltageDiff = measuredVolts - nearestPoint.getKey();

        // 7. Обновление меток
        jlbNearestPoint.setText(String.format("Ближ.т.: %.2fK", nearestPoint.getValue()));
        jlbAddingVolts.setText(String.format("Точки смещены на: %.4fV", voltageDiff));

        // 8. Создание/обновление отредактированной кривой
        CurveData editedCurve;
        if (curveStorage.isContains("Edited")) {
            editedCurve = curveStorage.getCurve("Edited");
        } else {
            jlbStatus.setText("Создание кривой с нуля еще не реализовано");
            log.error("Создание кривой с нуля еще не реализовано");
            return;
        }

        // 9. Применение смещения ко всем точкам
        List<Map.Entry<Double, Double>> editedPoints = new ArrayList<>();
        for (Map.Entry<Double, Double> point : openedPoints) {
            // Создаем новую точку со смещенным напряжением
            double newVoltage = point.getKey() + voltageDiff;
            editedPoints.add(new AbstractMap.SimpleEntry<>(newVoltage, point.getValue()));
        }
        editedCurve.setCurvePoints(editedPoints);

        // 10. Логирование
        log.info("Расчет завершен. Ближайшая точка: " + nearestPoint.getValue() + "K (" + nearestPoint.getKey() + "V), Смещение: " + voltageDiff + "V");
        log.info("Отредактировано точек: " + editedPoints.size());

        // 11. Обновление таблицы и графика
        buildEditedData(editedCurve);
        updateTableEditedFile(editedCurve);

        jlbStatus.setText("Расчет завершен успешно");


    }

    public void paintReadetPoly(ActionEvent e) {
        buildReadData(curveStorage.getCurve("Opened"));
    }

    public void paintEditedPoly(ActionEvent e) {
        buildEditedData(curveStorage.getCurve("Edited"));
    }


    private void saveFileActionHandler(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter;
        String extension;

        if (jcbFileExtension.getSelectedItem().equals("340")) {
            filter = new FileNameExtensionFilter("Poly files (*.340)", "340");
            extension = ".340";
        } else {
            filter = new FileNameExtensionFilter("Poly files (*.curve)", "curve");
            extension = ".curve";
        }

        fileChooser.setFileFilter(filter);

        if (filePath != null) {
            fileChooser.setCurrentDirectory(new File(filePath));
        }

        int result = fileChooser.showSaveDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            if (!filePath.endsWith(extension)) {
                selectedFile = new File(filePath + extension);
            }

            CurveMetaData metaData = curveStorage.getCurve("Edited").getCurveMetaData();
            List<Map.Entry<Double, Double>> curveData = curveStorage.getCurve("Edited").getCurvePoints();

            try (PrintWriter writer = new PrintWriter(selectedFile)) {
                writer.println("Sensor Model:   " + metaData.getSensorModel());
                writer.println("Serial Number:  " + metaData.getSerialNumber());
                writer.println("Data Format:    " +
                        metaData.getDataFormat().ordinal() + "      (" +
                        metaData.getDataFormat() + ")");
                writer.println("SetPoint Limit: " +
                        metaData.getSetPointLimit() + "      (Kelvin)");

                if (".340".equals(extension)) {
                    writer.println("Temperature coefficient:  " +
                            metaData.getTemperatureCoefficient());
                    writer.println("Number of Breakpoints:   " +
                            metaData.getNumberOfBreakpoints());
                    writer.println();
                    writer.println("No.  Units  Temperature (K)");
                    writer.println();
                } else {
                    writer.println();
                    writer.println("Measurement (Volts)\tTemp (K)");
                }

                // Форматирование чисел в экспоненциальную форму
                Function<Double, String> formatExponential = value ->
                        String.format(Locale.US, "%.5E", value)
                                .replace("E-0", "E-")
                                .replace("E+0", "E+")
                                .replace("E0", "E+");

                for (int i = 0; i < curveData.size(); i++) {
                    Map.Entry<Double, Double> point = curveData.get(i);
                    double value = point.getKey();
                    double temperature = point.getValue();

                    if (".340".equals(extension)) {
                        String valueStr = (Math.abs(value) < 0.1 || Math.abs(value) >= 1000)
                                ? formatExponential.apply(value)
                                : String.format(Locale.US, "%.5f", value);

                        String tempStr = String.format(Locale.US, "%.3f", temperature);

                        writer.printf(Locale.US, "%3d  %-11s  %s%n",
                                i + 1,
                                valueStr.length() > 11 ? valueStr.substring(0, 11) : valueStr,
                                tempStr);
                    } else {
                        // Для curve форматируем оба значения в экспоненциальную форму
                        String valueStr = formatExponential.apply(value);
                        String tempStr = formatExponential.apply(temperature);

                        writer.printf(Locale.US, "%-11s\t%-11s%n",
                                valueStr.length() > 11 ? valueStr.substring(0, 11) : valueStr,
                                tempStr.length() > 11 ? tempStr.substring(0, 11) : tempStr);
                    }
                }

                jlbSelectedFileEdited.setText(selectedFile.getAbsolutePath());
            } catch (FileNotFoundException ex) {
                log.error("Ошибка сохранения файла", ex);
                JOptionPane.showMessageDialog(null,
                        "Ошибка сохранения: " + ex.getMessage(),
                        "Ошибка",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
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
            line = reader.readLine();
            while (line != null && lineCount < needLineScip) {
                sb.append(line);
                sb.append("\n");
                lineCount++;
                line = reader.readLine();
            }

            // Чтение данных
            int i = 0;
            while (line != null) {

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
                    //System.out.println("Line number: + " + i + "Readet lineNumber: " + lineNumber + "Line: " + line + " units " + units + " temp " + temperature);
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
                line = reader.readLine();
            }
            CurveMetaData curveOpenedMetaData = new CurveMetaData();
            CurveMetaData curveEditedMetaData = new CurveMetaData();

            curveOpenedMetaData = updateCurveMetaDara(sb.toString());
            curveEditedMetaData = updateCurveMetaDara(sb.toString());

            updateOpenedCurveInfo(curveOpenedMetaData);
            updateEditedCurveInfo(curveEditedMetaData);

            CurveData curveOpenedData = new CurveData();
            curveOpenedData.setCurveMetaData(curveOpenedMetaData);
            fillCurveData(curveOpenedData, tableData);

            CurveData curveEditedData = new CurveData();
            curveEditedData.setCurveMetaData(curveEditedMetaData);
            fillCurveData(curveEditedData, tableData);

            curveStorage.addOrUpdateCurve("Opened", curveOpenedData);
            curveStorage.addOrUpdateCurve("Edited", curveEditedData);

            // Обновление таблицы
            updateTableOpenedFile(curveOpenedData);
            updateTableEditedFile(curveEditedData);


            // Построение графика
            buildReadData(curveStorage.getCurve("Opened"));
            buildEditedData(curveStorage.getCurve("Edited"));

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null,
                    "Ошибка чтения файла: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initChart() {
        dataset.removeAllSeries();
        dataset.addSeries(readetSeries);
        dataset.addSeries(editedSeries);
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
        jcbCurveFormatEdited.removeAllItems();
        for (CurveDataTypes dataType : CurveDataTypes.values()) {
            jcbCurveFormat.addItem(dataType);
            jcbCurveFormatEdited.addItem(dataType);
        }
    }

    private void fillSomeGuiElements() {
        jcbFileExtension.removeAllItems();
        jcbFileExtension.addItem("340");
        jcbFileExtension.addItem("curve");

        jcbNewOrOld.removeAllItems();
        jcbNewOrOld.addItem("Opened File");
        jcbNewOrOld.addItem("Edited File");

        //21-64
        jcbMemoryAddres.removeAllItems();
        for (int i = 21; i < 65; i++) {
            jcbMemoryAddres.addItem(i);
        }

    }

    private void updateOpenedCurveInfo(CurveMetaData curveMetaData) {
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

    private void updateEditedCurveInfo(CurveMetaData curveMetaData) {
        if (curveMetaData.getSensorModel() != null)
            jtfCurveNameEdited.setText(curveMetaData.getSensorModel());
        if (curveMetaData.getSerialNumber() != null)
            jtfSerialNumberEdited.setText(curveMetaData.getSerialNumber());
        if (curveMetaData.getDataFormat() != null)
            jcbCurveFormatEdited.setSelectedItem(curveMetaData.getDataFormat());
        if (curveMetaData.getSetPointLimit() != null)
            jtfPointsLimitEdited.setText(curveMetaData.getSetPointLimit().toString());
        if (curveMetaData.getTemperatureCoefficient() != null)
            jtfTermKoefTypeEdited.setText(curveMetaData.getTemperatureCoefficient());
        if (curveMetaData.getNumberOfBreakpoints() != null)
            jtfPointsCountEdited.setText(curveMetaData.getNumberOfBreakpoints().toString());
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

    private void updateTableEditedFile(CurveData curveData) {
        // Заголовки столбцов
        String[] header = curveData.getCurveMetaData().getDataFormat().getName().split("vs");

        //Заполнение таблицы
        // Добавляем новые точки данных через лямбдочку
        ArrayList<String[]> rowsList = new ArrayList<>();
        curveData.getCurvePoints().forEach(it -> {
            rowsList.add(new String[]{String.valueOf(it.getKey()), String.valueOf(it.getValue())});
        });

        // Установка модели таблицы
        jtbFilePreviewEdite.setModel(new DefaultTableModel(rowsList.toArray(new String[0][0]), header));
    }


    private void repaintChart() {
        if (chartPanel == null) {
            System.out.println("Понадобилась реинциализация графика");
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

    private <T extends Enum<?>, U> void initComboBox(JComboBox<U> comboBox, T[] values, IntFunction<U> valueExtractor) {
        for (int i = 0; i < values.length; i++) {
            comboBox.addItem(valueExtractor.apply(i));
            if (i == 12) {
                comboBox.setSelectedIndex(i);
            }
        }
        if (values.length > 12)
            comboBox.setSelectedIndex(12);
    }

    private void updateComPortList() {
        jcbComPortNumber.removeAllItems();
        for (int i = 0; i < SerialPort.getCommPorts().length; i++) {
            SerialPort currentPort = SerialPort.getCommPorts()[i];
            jcbComPortNumber.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (currentPort.getSystemPortName().equals(prop.getPorts()[0])) {
                jcbComPortNumber.setSelectedIndex(i);
            }
        }
    }

    private void buildEditedData(CurveData curveData) {
        // Очищаем существующие данные
        editedSeries.clear();
        // Добавляем новые точки данных через лямбдочку
        curveData.getCurvePoints().forEach(it -> {
            editedSeries.add(it.getKey(), it.getValue());
        });
        // Перересовываем график
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
        mainPane.setLayout(new GridLayoutManager(7, 4, new Insets(0, 0, 0, 0), -1, -1));
        comConnection = new JPanel();
        comConnection.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(comConnection, new GridConstraints(0, 0, 7, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbComPortNumber = new JComboBox();
        comConnection.add(jcbComPortNumber, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComBarity = new JComboBox();
        comConnection.add(jcbComBarity, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComSpeed = new JComboBox();
        comConnection.add(jcbComSpeed, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbOpenComPort = new JButton();
        jbOpenComPort.setText("connect");
        comConnection.add(jbOpenComPort, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCloseComPort = new JButton();
        jbCloseComPort.setText("Disconnect");
        comConnection.add(jbCloseComPort, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpnColorStatus = new JPanel();
        jpnColorStatus.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        comConnection.add(jpnColorStatus, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jlbStatus = new JLabel();
        jlbStatus.setText("Ready");
        jpnColorStatus.add(jlbStatus, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        comConnection.add(spacer1, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
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
        mainPane.add(jpnGraph, new GridConstraints(6, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(200, 200), new Dimension(600, 400), new Dimension(800, 1000), 0, false));
        jpOpenedCurvePreview = new JPanel();
        jpOpenedCurvePreview.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpOpenedCurvePreview, new GridConstraints(0, 1, 7, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, 300), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCurveInformation = new JPanel();
        jpCurveInformation.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpOpenedCurvePreview.add(jpCurveInformation, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCurveName = new JPanel();
        jpCurveName.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformation.add(jpCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
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
        jpCurveInformation.add(jpSerialNumber, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
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
        jpCurveInformation.add(jpPointLimit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
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
        jpCurveInformation.add(jpCurveFormat, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
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
        jpCurveInformation.add(jpTemperatCoeff, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
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
        jpCurveInformation.add(jpNumberOfBreakpoints, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
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
        jpFileSave = new JPanel();
        jpFileSave.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpFileSave, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbFileExtension = new JComboBox();
        jpFileSave.add(jcbFileExtension, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtSaveFile = new JButton();
        jbtSaveFile.setText("Save File");
        jpFileSave.add(jbtSaveFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jlbSelectedFileEdited = new JLabel();
        jlbSelectedFileEdited.setText(" ");
        jpFileSave.add(jlbSelectedFileEdited, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        jpEditGraph.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpEditGraph, new GridConstraints(4, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jtfKelvinMeasured = new JTextField();
        jtfKelvinMeasured.setText("296");
        jpEditGraph.add(jtfKelvinMeasured, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jtfVoltsMeasured = new JTextField();
        jtfVoltsMeasured.setText("0,89586");
        jpEditGraph.add(jtfVoltsMeasured, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jbtCalculate = new JButton();
        jbtCalculate.setText("Calculate");
        jpEditGraph.add(jbtCalculate, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jlbNearestPoint = new JLabel();
        jlbNearestPoint.setText("Температура");
        jpEditGraph.add(jlbNearestPoint, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jlbAddingVolts = new JLabel();
        jlbAddingVolts.setText("Напряжение");
        jpEditGraph.add(jlbAddingVolts, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpGraphActions = new JPanel();
        jpGraphActions.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpGraphActions, new GridConstraints(5, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        jpEditedCurvePreview.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpEditedCurvePreview, new GridConstraints(0, 2, 7, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCurveInformationEdited = new JPanel();
        jpCurveInformationEdited.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpEditedCurvePreview.add(jpCurveInformationEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationEdited.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCurveNameEdited = new JLabel();
        jlbCurveNameEdited.setText("Curve Name");
        panel1.add(jlbCurveNameEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCurveNameEdited = new JTextField();
        jtfCurveNameEdited.setEditable(true);
        jtfCurveNameEdited.setEnabled(true);
        panel1.add(jtfCurveNameEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer11 = new Spacer();
        panel1.add(spacer11, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationEdited.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbSerialNumberEdited = new JLabel();
        jlbSerialNumberEdited.setText("Serial Number");
        panel2.add(jlbSerialNumberEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfSerialNumberEdited = new JTextField();
        jtfSerialNumberEdited.setEditable(true);
        jtfSerialNumberEdited.setEnabled(true);
        panel2.add(jtfSerialNumberEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer12 = new Spacer();
        panel2.add(spacer12, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationEdited.add(panel3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlpPointsLimitEdited = new JLabel();
        jlpPointsLimitEdited.setText("Point Limit");
        panel3.add(jlpPointsLimitEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfPointsLimitEdited = new JTextField();
        jtfPointsLimitEdited.setEditable(true);
        jtfPointsLimitEdited.setEnabled(true);
        panel3.add(jtfPointsLimitEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer13 = new Spacer();
        panel3.add(spacer13, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationEdited.add(panel4, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCurveFormatEdited = new JLabel();
        jlbCurveFormatEdited.setText("Curve Format");
        panel4.add(jlbCurveFormatEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbCurveFormatEdited = new JComboBox();
        jcbCurveFormatEdited.setEditable(true);
        jcbCurveFormatEdited.setEnabled(true);
        panel4.add(jcbCurveFormatEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer14 = new Spacer();
        panel4.add(spacer14, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationEdited.add(panel5, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbTermKoefTypeEdited = new JLabel();
        jlbTermKoefTypeEdited.setText("Term koef type");
        panel5.add(jlbTermKoefTypeEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfTermKoefTypeEdited = new JTextField();
        jtfTermKoefTypeEdited.setEditable(true);
        jtfTermKoefTypeEdited.setEnabled(true);
        panel5.add(jtfTermKoefTypeEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer15 = new Spacer();
        panel5.add(spacer15, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationEdited.add(panel6, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbPointsCountEdited = new JLabel();
        jlbPointsCountEdited.setText("Points Count");
        panel6.add(jlbPointsCountEdited, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfPointsCountEdited = new JTextField();
        jtfPointsCountEdited.setEditable(true);
        jtfPointsCountEdited.setEnabled(true);
        panel6.add(jtfPointsCountEdited, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer16 = new Spacer();
        panel6.add(spacer16, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpTableEdite = new JPanel();
        jpTableEdite.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpEditedCurvePreview.add(jpTableEdite, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        jpTableEdite.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 200), new Dimension(200, 350), new Dimension(250, 400), 0, false));
        jtbFilePreviewEdite = new JTable();
        scrollPane1.setViewportView(jtbFilePreviewEdite);
        final Spacer spacer17 = new Spacer();
        jpTableEdite.add(spacer17, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer18 = new Spacer();
        jpTableEdite.add(spacer18, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpDataTranserProggres = new JPanel();
        jpDataTranserProggres.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpDataTranserProggres, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jpbCommandSending = new JProgressBar();
        jpDataTranserProggres.add(jpbCommandSending, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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

    @Override
    public void renderData() {
        //ToDo придумать что нибудь красивое
    }

    @Override
    public boolean isEnable() {
        return true;
    }
}
