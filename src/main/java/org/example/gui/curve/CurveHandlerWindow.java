package org.example.gui.curve;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.curve.file.CurveFileAccessException;
import org.example.gui.curve.file.CurveFileSerializationException;
import org.example.gui.curve.file.FileHandler;
import org.example.gui.curve.file.Serialization;
import org.example.gui.curve.math.CalculatedCurveData;
import org.example.gui.curve.math.Calculator;
import org.example.gui.curve.math.CurveCalculationException;
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
    private final FileHandler fileHandler;
    private final MyProperties prop;
    private final Serialization ser = new Serialization();
    private final Calculator calc = new Calculator();
    private JPanel comConnection;
    private JComboBox jcbComPortNumber;
    private JComboBox jcbComBarity;
    private JComboBox jcbComSpeed;
    private JButton jbOpenComPort;
    private JTable jtbFromFilePreview;
    private JPanel jpnGraph;
    private JButton jbtSelectFile;
    private JPanel mainPane;
    private JPanel fileSelect;
    private JLabel jlbSelectedFile;
    private JTextField jtfFromFileCurveName;
    private JTextField jtfFromFileSerialNumber;
    private JTextField jtfFromFilePointLimit;
    private JComboBox jcbFromFileCurveFormat;
    private JTextField jtfFromFileTermCoef;
    private JTextField jtfFromFileNumbeCounts;
    private JButton jbCloseComPort;
    private JPanel jpFileSave;
    private JComboBox jcbFileExtensionForSave;
    private JButton jbtSaveFile;
    private JComboBox jcbMemoryAddres;
    private JComboBox jcbFileSourceForWrite;
    private JButton jbtWrite;
    private JPanel jpFileWrite;
    private JTextField jtfKelvinMeasured;
    private JTextField jtfVoltsMeasured;
    private JButton jbtCalculate;
    private JPanel jpEditGraph;
    private JPanel jpGraphActions;
    private JButton jbtClearGraph;
    private JButton jbtAddPolyFromFile;
    private JButton jbtAddPolyCalculated;
    private JTextField jtfCalculatedCurveName;
    private JTextField jtfCalculatedSerialNumber;
    private JLabel jlpCalculatedPointLimit;
    private JTextField jtfCalculatedPointLimit;
    private JComboBox jcbCalculatedCurveFormat;
    private JTextField jtfCalculatedTermCoef;
    private JTextField jtfCalculatedNumbeCounts;
    private JTable jtbCalculatedPreview;
    private JLabel jlbSavedFile;
    private JPanel jpnColorStatus;
    private JLabel jlbStatus;
    private JPanel jpDataTranserProggres;
    private JProgressBar jpbCommandSending;
    private JLabel jlbNearestPoint;
    private JLabel jlbAddingVolts;
    private JTabbedPane jtpFilePreview;
    private JPanel jpFromFileCurvePreview;
    private JPanel jpFromFileCurveInformation;
    private JPanel jpFromFileCurveName;
    private JPanel jpFromFileSerialNumber;
    private JPanel jpFromFilePointLimit;
    private JPanel jpFromFileCurveFormat;
    private JPanel jpFromFileTemperatCoeff;
    private JLabel jlbFromFileTermCoef;
    private JPanel jpFromFileNumberOfBreakpoints;
    private JLabel jlbFromFileNumbeCounts;
    private JPanel jpFromFileTable;
    private JScrollPane jspFromFileTable;
    private JPanel jpCalculatedCurvePreview;
    private JPanel jpCalculatedCurveInformation;
    private JLabel jlbCalculatedCurveName;
    private JLabel jlbCalculatedSerialNumber;
    private JLabel jlbCalculatedCurveFormat;
    private JLabel jlbCalculatedTermCoef;
    private JLabel jlbCalculatedNumbeCounts;
    private JPanel jpCalculatedTable;
    private JPanel jpFromDeviceCurvePreview;
    private JPanel jpCurveInformationSelected;
    private JPanel jpFromDeviceTable;
    private JTabbedPane tabbedPane1;
    private JScrollPane jspReadetNamesStandartCurves;
    private JScrollPane jspReadetNamesUsersCurves;
    private JTextField jtfFromDeviceCurveName;
    private JTextField jtfFromDeviceSerialNumber;
    private JTextField jtfFromDevicePointLimit;
    private JComboBox jcbFromDeviceCurveFormat;
    private JTextField jtfFromDeviceTermCoef;
    private JTextField jtfFromDeviceNumbeCounts;
    private JTable jtbFromDevicePreview;
    private JLabel jlbFromFileCurveName;
    private JLabel jlbFromFileSerialNumber;
    private JLabel jlbFromFilePointLimit;
    private JLabel jlbFromFileCurveFormat;
    private JPanel jtpFromFileCurvePreview;
    private JPanel jtpCalculatedCurvePreview;
    private JPanel jpnCalculatedCurveName;
    private JPanel jpCalculatedSerialNumber;
    private JPanel jpCalculatedPointLimit;
    private JPanel jpCalculatedCurveFormat;
    private JPanel jpCalculatedTemperatCoeff;
    private JPanel jpCalculatedNumberOfBreakpoints;
    private JScrollPane jspCalculatedTable;
    private JPanel jtpFromDeviceCurvePreview;
    private JPanel jpFromDeviceCurveName;
    private JLabel jlbFromDeviceCurveName;
    private JPanel jpFromDeviceSerialNumber;
    private JLabel jlbFromDeviceSerialNumber;
    private JPanel jpFromDevicePointLimit;
    private JLabel jlbFromDevicePointLimit;
    private JPanel jpFromDeviceCurveFormat;
    private JLabel jlbFromDeviceCurveFormat;
    private JPanel jpFromDeviceTemperatCoeff;
    private JLabel jlbFromDeviceTermCoef;
    private JPanel jpFromDeviceNumberOfBreakpoints;
    private JLabel jlbFromDeviceNumbeCounts;
    private JScrollPane jspFromDeviceTable;
    private JPanel jpnSelectedFile;
    private JScrollPane jspSelectedFile;
    private JPanel jpnSavedFile;
    private JScrollPane jspSavedFile;
    private JComboBox jcbFileSourceForSave;
    private JComboBox jcbFileSourceForCalc;
    private JButton jbtAddPolyFromDevice;
    private JButton jbCmdSleep;
    private JButton jbCmdWakeUp;
    private JButton jbCmdGetState;
    private XYSeries fromFileSeries = new XYSeries("From File");
    private XYSeries calculatedSeries = new XYSeries("Calculated");
    private XYSeries fromDeviceSeries = new XYSeries("From Device");
    private XYSeriesCollection dataset = new XYSeriesCollection();
    private JFreeChart chart;
    private ChartPanel chartPanel;
    private CurveStorage curveStorage = new CurveStorage();
    private ArrayList<CurveMetaData> curveMetaDtataInDeviceList = new ArrayList<>();
    private final Map<JButton, CurveMetaData> buttonCurveMap = new HashMap<>();
    private JPanel curvesContainerStandart; // Контейнер для кнопок
    private JPanel curvesContainerUsers; // Контейнер для кнопок
    private final ArrayList<CurveMetaData> curveMetaDataInDeviceList = new ArrayList<>();
    private SerialPort comPort = null;


    public CurveHandlerWindow(MyProperties prop) {

        $$$setupUI$$$();
        log = Logger.getLogger(CurveHandlerWindow.class);
        fileHandler = new FileHandler();
        this.prop = prop;
        setContentPane(mainPane);

        curvesContainerStandart = new JPanel();
        curvesContainerStandart.setLayout(new BoxLayout(curvesContainerStandart, BoxLayout.Y_AXIS));
        jspReadetNamesStandartCurves.setViewportView(curvesContainerStandart);

        curvesContainerUsers = new JPanel();
        curvesContainerUsers.setLayout(new BoxLayout(curvesContainerUsers, BoxLayout.Y_AXIS));
        jspReadetNamesUsersCurves.setViewportView(curvesContainerUsers);
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
        jbtAddPolyFromFile.addActionListener(this::paintPolyFromFile);
        jbtAddPolyCalculated.addActionListener(this::paintCalculatedPoly);
        jbtAddPolyFromDevice.addActionListener(this::paintPolyFromDevice);

        jbtCalculate.addActionListener(this::calculateActionHandler);
        jbOpenComPort.addActionListener(this::openPortActionHandler);
        jbCloseComPort.addActionListener(this::closePortActionHandler);
        jbCmdGetState.addActionListener(this::getStateCmdActionHandler);
        jbCmdSleep.addActionListener(this::sleepCmdActionHandler);
        jbCmdWakeUp.addActionListener(this::wakeCmdActionHandler);

        jbtWrite.addActionListener(this::writeInDeviceHandler);

        ListenerUtils.addDocumentListener(jtfCalculatedCurveName, this::updateOnEditeCurveName);
        ListenerUtils.addKeyListener(jtfCalculatedCurveName, this::updateOnEnterCurveName);

        ListenerUtils.addDocumentListener(jtfCalculatedSerialNumber, this::updateOnEditeCurveSerialNumber);
        ListenerUtils.addKeyListener(jtfCalculatedSerialNumber, this::updateOnEnterCurveSerialNumber);

        ListenerUtils.addDocumentListener(jtfCalculatedPointLimit, this::updateOnEditeCurvePointsLimit);
        ListenerUtils.addKeyListener(jtfCalculatedPointLimit, this::updateOnEnterCurvePointsLimit);

        jcbCalculatedCurveFormat.addActionListener(this::updateOnEditeCurveFormat);

        ListenerUtils.addDocumentListener(jtfCalculatedTermCoef, this::updateOnEditeCurveTermKoefType);
        ListenerUtils.addKeyListener(jtfCalculatedTermCoef, this::updateOnEnterCurveTermKoefType);

        ListenerUtils.addDocumentListener(jtfCalculatedNumbeCounts, this::updateOnEditeCurvePointsCount);
        ListenerUtils.addKeyListener(jtfCalculatedNumbeCounts, this::updateOnEnterCurvePointsCount);

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
            int readTimeout = 80;  // 1 секунда
            int writeTimeout = 120;  // 1 секунда
            comPort.setComPortTimeouts(
                    SerialPort.TIMEOUT_NONBLOCKING,
                    readTimeout,
                    writeTimeout
            );

            // Открываем порт
            boolean opened = comPort.openPort(readTimeout);

            if (opened) {
                jlbStatus.setText("Порт " + comPort.getDescriptivePortName() + " открыт");
            } else {
                doErrorMessage("Не удалось открыть порт. Код ошибки: " + comPort.getLastErrorCode(), "Такое иногда случается...");
                throw new ConnectException("Не удалось открыть порт. Код ошибки: " + comPort.getLastErrorCode());
            }

        } catch (Exception e) {
            jlbStatus.setText("Ошибка открытия порта");
            log.error("Ошибка при открытии COM-порта", e);
            doErrorMessage("Ошибка открытия порта:" + e.getMessage(), "Такое иногда случается...");
        }


        jlbStatus.setText(checkDeviceConnection());
        getListOfCurvesInDevice();
    }

    private void closePortActionHandler(ActionEvent actionEvent) {
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            jlbStatus.setText("Порт закрыт");
        }
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

    private void getStateCmdActionHandler(ActionEvent actionEvent) {
        if (comPort != null && comPort.isOpen()) {
            CurveDeviceCommander curveDeviceCommander = new CurveDeviceCommander(comPort);
            StateWords status = null;
            try {
                status = curveDeviceCommander.getState();
            } catch (Exception e) {
                doErrorMessage("Ошибка при запросе состояния прибора:"+e.getMessage(), "Ошибка запроса состояния");
                jlbStatus.setText("Ошибка при запросе состояния прибора:"+e.getMessage());
            }
            jlbStatus.setText("Текущее состояние прибора: " + status.getName());
        } else {
            doErrorMessage("Ошибка. Порт не инициализирован." , "Ошибка запроса состояния");
            jlbStatus.setText( "Ошибка 001. Порт не открыт.");
        }
    }

    private void sleepCmdActionHandler(ActionEvent actionEvent) {
        if (comPort != null && comPort.isOpen()) {
            CurveDeviceCommander curveDeviceCommander = new CurveDeviceCommander(comPort);
            try {
                curveDeviceCommander.sendSleep();
            } catch (Exception e) {
                doErrorMessage("Ошибка при отправке команды SLEEP прибора:"+e.getMessage(), "Ошибка отправки команды");
                jlbStatus.setText("Ошибка при отправке команды SLEEP прибора:"+e.getMessage());
            }
            jlbStatus.setText("Отправлена команда SLEEP");
        } else {
            doErrorMessage("Ошибка. Порт не инициализирован." , "Ошибка отправки SLEEP");
            jlbStatus.setText( "Ошибка 001. Порт не открыт.");
        }
    }

    private void wakeCmdActionHandler(ActionEvent actionEvent) {
        if (comPort != null && comPort.isOpen()) {
            CurveDeviceCommander curveDeviceCommander = new CurveDeviceCommander(comPort);
            try {
                curveDeviceCommander.sendWakeUp();
            } catch (Exception e) {
                doErrorMessage("Ошибка при отправке команды WAKEUP прибора:"+e.getMessage(), "Ошибка отправки команды");
                jlbStatus.setText("Ошибка при отправке команды WAKEUP прибора:"+e.getMessage());
            }
            jlbStatus.setText("Отправлена команда WAKEUP");
        } else {
            doErrorMessage("Ошибка. Порт не инициализирован." , "Ошибка отправки WAKEUP");
            jlbStatus.setText( "Ошибка 001. Порт не открыт.");
        }
    }
    private String getListOfCurvesInDevice() {
        log.debug("Начинаю отображение списка кривых");

        // Настройка прогресс-бара
        jpDataTranserProggres.setVisible(true);
        jpbCommandSending.setValue(0);
        jpbCommandSending.setStringPainted(true);

        new Thread(() -> {
            try {
                CurveDeviceCommander curveDeviceCommander = new CurveDeviceCommander(comPort);

                final ArrayList<CurveMetaData> resultHolder = curveDeviceCommander.getListOfCurvesInDevice(progress -> {
                    SwingUtilities.invokeLater(() -> {
                        jpbCommandSending.setValue(progress);
                        jlbStatus.setText("Чтение... " + progress + "%");
                        log.error("Чтение... " + progress + "%");
                    });
                });

                SwingUtilities.invokeLater(() -> {
                    jlbStatus.setText("Считывание завершено успешно!");
                    jpbCommandSending.setValue(100);
                    log.warn("В коллекции " + resultHolder.size());

                    curvesContainerUsers.removeAll();
                    curvesContainerStandart.removeAll();
                    // Добавляем кнопки для всех кривых
                    log.error("Начинаю перебор коллекции");
                    for (CurveMetaData curve : resultHolder) {
                        //log.error("Просматриваю кривую " + curve.getSensorModel());
                        if (curve.getIsUserCurve() != null && curve.getIsUserCurve()) {
                            curvesContainerUsers.add(createCurveButton(curve));
                        } else {
                            curvesContainerStandart.add(createCurveButton(curve));
                        }
                    }

                    // Сохраняем результат в основном списке
                    curveMetaDataInDeviceList.clear();
                    curveMetaDataInDeviceList.addAll(resultHolder);

                    // Принудительное обновление GUI
                    curvesContainerUsers.revalidate();
                    curvesContainerUsers.repaint();
                    curvesContainerStandart.revalidate();
                    curvesContainerStandart.repaint();

                    // Убеждаемся, что скроллпейн видим
                    jspReadetNamesStandartCurves.setVisible(true);

                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    jlbStatus.setText("Ошибка записи: " + ex.getMessage());
                    log.error("Ошибка записи в устройство", ex);
                });
            }
        }).start();

        return "Завершено получение списка кривых";
    }

    private JButton createCurveButton(CurveMetaData curve) {
        //log.info("Создаю кнопку кривой " + curve.getSensorModel());
        String buttonText = String.format("%s -- %s (SN: %s)",
                curve.getNumberInDeviceMemory(), curve.getSensorModel(), curve.getSerialNumber());

        JButton curveButton = new JButton(buttonText);
        curveButton.setAlignmentX(Component.LEFT_ALIGNMENT); // Выравнивание по левому краю
        curveButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, curveButton.getPreferredSize().height));

        // Сохраняем связь кнопки с данными кривой
        buttonCurveMap.put(curveButton, curve);

        // Добавляем всплывающую подсказку с деталями
        curveButton.setToolTipText(createTooltip(curve));

        // Обработчик клика
        curveButton.addActionListener(e -> {
            CurveMetaData selectedCurve = buttonCurveMap.get(e.getSource());
            if (selectedCurve != null) {
                loadCurveDataFromDevice(selectedCurve);
            }
        });

        return curveButton;
    }

    private void setCurveButtons(boolean flag){
        for (Map.Entry<JButton, CurveMetaData> jButtonCurveMetaDataEntry : buttonCurveMap.entrySet()) {
            jButtonCurveMetaDataEntry.getKey().setEnabled(flag);
        }
    }
    private void setAddressForWrite(int numberInDeviceMemory) {
        jcbMemoryAddres.setSelectedItem(numberInDeviceMemory);
    }
    private void loadCurveDataFromDevice(CurveMetaData curve) {
        setCurveButtons(false);
        setAddressForWrite(curve.getNumberInDeviceMemory());
        jpDataTranserProggres.setVisible(true);
        jpbCommandSending.setValue(0);
        jlbStatus.setText("Начато чтение кривой...");
        new Thread(() -> {
            log.warn("Будет отправлена команда CRVPT? " + curve.getNumberInDeviceMemory() + ",1");
            CurveDeviceCommander curveDeviceCommander = new CurveDeviceCommander(comPort);

            try {
                List<Map.Entry<Double, Double>> curvePoints = curveDeviceCommander.readCurveFromDevice(
                        curve.getNumberInDeviceMemory(),
                        progress -> {
                            // Обновляем прогресс в потоке GUI
                            SwingUtilities.invokeLater(() -> {
                                jpbCommandSending.setValue(progress);
                                jlbStatus.setText("Чтение полинома... " + progress + "%");
                                log.info("Прогресс чтения: " + progress + "%");
                            });
                        });

                // После успешного чтения
                SwingUtilities.invokeLater(() -> {
                    jlbStatus.setText("Считывание завершено успешно! Точек: " + curvePoints.size());
                    jpbCommandSending.setValue(100);
                    log.info("Прочитано точек: " + curvePoints.size());
                    CurveData readetCurveData = new CurveData();
                    curve.setNumberOfBreakpoints(curvePoints.size());
                    readetCurveData.setCurveMetaData(curve);
                    readetCurveData.setCurvePoints(curvePoints);
                    curveStorage.addOrUpdateCurve("FromDevice", readetCurveData);
                    // 11. Обновление таблицы и графика
                    // TODO: Доп. проверки
                    buildGraph(curveStorage.getCurve("FromDevice"), fromDeviceSeries);
                    updateDataTable(curveStorage.getCurve("FromDevice"), jtbFromDevicePreview);
                    updateCurveInfoFromDevice(curveStorage.getCurve("FromDevice").getCurveMetaData());
                    setCurveButtons(true);

                });
            } catch (Exception e) {
                setCurveButtons(true);
                doErrorMessage("Ошибка чтения: " + e.getMessage(), "Ошибка чтения кривой");
            }
        }).start(); // Запускаем фоновый поток


    }

    private String createTooltip(CurveMetaData curve) {
        return String.format("<html>Модель: %s<br>С/н: %s<br>Формат: %s<br>Макс.темп.: %dK</html>",
                curve.getSensorModel(),
                curve.getSerialNumber(),
                curve.getDataFormat().getName(),
                curve.getSetPointLimit());
    }


    // Вспомогательный метод для поиска кривой
    private CurveMetaData findCurveBySerial(String serial) {
        for (CurveMetaData curve : curveMetaDtataInDeviceList) {
            if (curve.getSerialNumber().equals(serial)) {
                return curve;
            }
        }
        return null;
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
        String curveType = (jcbFileSourceForWrite.getSelectedIndex() == 0) ? "FromFile" : "FromCalculated";
        log.info("Запись {" + curveType + "} файла");

        if (!curveStorage.isContains(curveType)) {
            jlbStatus.setText("Данные для записи " + curveType + " кривой не найдены");
            doErrorMessage("Данные для записи " + curveType + " кривой не найдены", "Ошибочка");
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
        log.info("Do updateOnEditeCurveFormat set [" +  ((CurveDataTypes) jcbCalculatedCurveFormat.getSelectedItem()) + "]");
        if (curveStorage.isContains("FromCalculated"))
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setDataFormat((CurveDataTypes) jcbCalculatedCurveFormat.getSelectedItem());
    }

    private void updateOnEditeCurvePointsCount() {
        Integer count = null;
        try {
            count = Integer.parseInt(jtfCalculatedNumbeCounts.getText());
        } catch (NumberFormatException e) {
            jtfCalculatedNumbeCounts.setText(String.valueOf(curveStorage.getCurve("FromCalculated").getCurveMetaData().getNumberOfBreakpoints()));
            log.warn("Исключение во время получения числа из строки CurvePointsCount" + e.getMessage());
            //e.printStackTrace();
        }
        if (count != null) {
            if(curveStorage.isContains("FromCalculated"))
                curveStorage.getCurve("FromCalculated").getCurveMetaData().setNumberOfBreakpoints(Integer.parseInt(jtfCalculatedNumbeCounts.getText()));
        }
    }

    private void updateOnEnterCurveSerialNumber() {
        if (jtfCalculatedSerialNumber.getText() == null || jtfCalculatedSerialNumber.getText().isEmpty()) {
            log.warn("Попытка установки null-строки как серийный номер сенсора");
            jtfCalculatedSerialNumber.setText(curveStorage.getCurve("FromCalculated").getCurveMetaData().getSerialNumber());
        } else {
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setSerialNumber(jtfCalculatedSerialNumber.getText());
        }
    }

    private void updateOnEditeCurveTermKoefType() {
        if (jtfCalculatedTermCoef.getText() != null && !jtfCalculatedTermCoef.getText().isEmpty()) {
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setTemperatureCoefficient(jtfCalculatedTermCoef.getText());
        }
    }

    private void updateOnEnterCurvePointsLimit() {
        if (jtfCalculatedPointLimit.getText() == null || jtfCalculatedPointLimit.getText().isEmpty()) {
            log.warn("Попытка установки null-строки CurvePointsLimit");
            jtfCalculatedPointLimit.setText(String.valueOf(curveStorage.getCurve("FromCalculated").getCurveMetaData().getSetPointLimit()));
        } else {
            Integer num = null;
            try {
                num = Integer.parseInt(jtfCalculatedPointLimit.getText());
            } catch (NumberFormatException ex) {
                jtfCalculatedPointLimit.setText(String.valueOf(curveStorage.getCurve("FromCalculated").getCurveMetaData().getSetPointLimit()));
                log.warn("Исключение во время получения числа из строки OnEnterCurvePointsLimit" + ex.getMessage());
                //ex.printStackTrace();
            }
            if (num != null) {
                curveStorage.getCurve("FromCalculated").getCurveMetaData().setSetPointLimit(num);
            }
        }
    }

    private void updateOnEnterCurvePointsCount() {
        if (jtfCalculatedNumbeCounts.getText() == null || jtfCalculatedNumbeCounts.getText().isEmpty()) {
            log.warn("Попытка установки null-строки CountEdited");
            jtfCalculatedNumbeCounts.setText(String.valueOf(curveStorage.getCurve("FromCalculated").getCurveMetaData().getNumberOfBreakpoints()));
        } else {
            Integer count = null;
            try {
                count = Integer.parseInt(jtfCalculatedNumbeCounts.getText());
            } catch (NumberFormatException e) {
                jtfCalculatedNumbeCounts.setText(String.valueOf(curveStorage.getCurve("FromCalculated").getCurveMetaData().getNumberOfBreakpoints()));
                log.warn("Исключение во время получения числа из строки OnEnterCurvePointsCount" + e.getMessage());
                //e.printStackTrace();
            }
            if (count != null) {
                curveStorage.getCurve("FromCalculated").getCurveMetaData().setNumberOfBreakpoints(Integer.parseInt(jtfCalculatedNumbeCounts.getText()));
            }
        }
    }

    private void updateOnEnterCurveTermKoefType() {
        if (jtfCalculatedTermCoef.getText() == null || jtfCalculatedTermCoef.getText().isEmpty()) {
            log.warn("Попытка установки null-строки как температурного коэффициента");
            jtfCalculatedTermCoef.setText(curveStorage.getCurve("FromCalculated").getCurveMetaData().getTemperatureCoefficient());
        } else {
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setTemperatureCoefficient(jtfCalculatedTermCoef.getText());
        }
    }

    private void updateOnEditeCurvePointsLimit() {
        Integer num = null;
        try {
            num = Integer.parseInt(jlpCalculatedPointLimit.getText());
        } catch (NumberFormatException e) {
            if (curveStorage.isContains("FromCalculated")){
                jlpCalculatedPointLimit.setText(String.valueOf(curveStorage.getCurve("FromCalculated").getCurveMetaData().getSetPointLimit()));
            }
            log.warn("Исключение во время получения числа из строки OnEditeCurvePointsLimit" + e.getMessage());
        }
        if (num != null) {
            if (curveStorage.isContains("FromCalculated"))
                curveStorage.getCurve("FromCalculated").getCurveMetaData().setSetPointLimit(num);
        }
    }

    private void updateOnEditeCurveSerialNumber() {
        if (jtfCalculatedSerialNumber.getText() != null && !jtfCalculatedSerialNumber.getText().isEmpty() && curveStorage.isContains("FromCalculated")) {
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setSerialNumber(jtfCalculatedSerialNumber.getText());
        }
    }

    private void updateOnEditeCurveName() {
        if(curveStorage.isContains("FromCalculated"))
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setSensorModel(jtfCalculatedCurveName.getText());
    }

    private void updateOnEnterCurveName() {
        if(curveStorage.isContains("FromCalculated"))
            curveStorage.getCurve("FromCalculated").getCurveMetaData().setSensorModel(jtfCalculatedCurveName.getText());
    }


    // Обработчик выбора файла
    private void handleFileSelection(ActionEvent e) {
        try {
            fileHandler.selectFileToOpen();
        } catch (CurveFileAccessException ex) {
            doErrorMessage("Ошибка при открытии файла: ["+ex.getMessage()+"]", "Ошибка");
            return;
        }
        if(fileHandler.getSelectedFileToOpen() != null){
            jlbSelectedFile.setText(fileHandler.getSelectedFileToOpen().getName());

            CurveData read = null;
            try {
                read = ser.deserializeCurveData(fileHandler.getSelectedFileToOpen());
            } catch (CurveFileSerializationException ex) {
                doErrorMessage("Ошибка при разборе файла: ["+ex.getMessage()+"]", "Ошибка");
                return;
            }
            updateCurveInfoFromFile(read.getCurveMetaData());
            curveStorage.addOrUpdateCurve("FromFile", read);

            // Обновление таблицы
            updateDataTable(read, jtbFromFilePreview);

            // Построение графика
            buildGraph(curveStorage.getCurve("FromFile"), fromFileSeries);
        }
    }

    public void clearGraph(ActionEvent e) {
        fromFileSeries.clear();
        fromDeviceSeries.clear();
        calculatedSeries.clear();

        repaintChart();
    }

    public void calculateActionHandler(ActionEvent e) {
        String kelvinString = jtfKelvinMeasured.getText();
        String voltsString = jtfVoltsMeasured.getText();

        CurveData selectedCurve = null;
        if(jcbFileSourceForCalc.getSelectedItem().equals("From Device")) {
            selectedCurve = curveStorage.getCurve("FromDevice");
        }else if(jcbFileSourceForCalc.getSelectedItem().equals("From File")){
            selectedCurve = curveStorage.getCurve("FromFile");
        }
        CalculatedCurveData calculatedCurveData = null;
        try {
            calculatedCurveData = calc.calculateActionHandlerNew(kelvinString, voltsString, selectedCurve);
        } catch (CurveCalculationException ex) {
            jlbStatus.setText("Произошла ошибка во время расчета кривой" + ex.getMessage());
            doErrorMessage("Произошла ошибка во время расчета кривой: " + ex.getMessage(), "Ошибка");
        }

        curveStorage.addOrUpdateCurve("FromCalculated", calculatedCurveData.getCurveData());
        jlbNearestPoint.setText(String.format("Ближ.т.: %.2fK", calculatedCurveData.getNearestPoint()));
        jlbAddingVolts.setText(String.format("Точки смещены на: %.4fV", calculatedCurveData.getShiftSize()));
        updateCurveInfoCalculated(calculatedCurveData.getCurveData().getCurveMetaData());
        buildGraph(calculatedCurveData.getCurveData(), calculatedSeries);
        updateDataTable(calculatedCurveData.getCurveData(), jtbCalculatedPreview);
    }


    public void paintPolyFromFile(ActionEvent e) {
        buildGraph(curveStorage.getCurve("FromFile"), fromFileSeries);
    }

    public void paintCalculatedPoly(ActionEvent e) {
        log.info("paintCalculatedPoly event");
        buildGraph(curveStorage.getCurve("FromCalculated"), calculatedSeries);
    }

    public void paintPolyFromDevice(ActionEvent e) {
        buildGraph(curveStorage.getCurve("FromDevice"), fromDeviceSeries);
    }


    private void saveFileActionHandler(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter;
        String extension;

        if (jcbFileExtensionForSave.getSelectedItem().equals("340")) {
            filter = new FileNameExtensionFilter("Poly files (*.340)", "340");
            extension = ".340";
        } else {
            filter = new FileNameExtensionFilter("Poly files (*.curve)", "curve");
            extension = ".curve";
        }

        fileChooser.setFileFilter(filter);

        if (fileHandler.getFilePath() != null) {
            fileChooser.setCurrentDirectory(new File(fileHandler.getFilePath()));
        }

        int result = fileChooser.showSaveDialog(null);

        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            if (!filePath.endsWith(extension)) {
                selectedFile = new File(filePath + extension);
            }
            //ToDO выбор источника данных
            CurveMetaData metaData = curveStorage.getCurve("FromCalculated").getCurveMetaData();
            List<Map.Entry<Double, Double>> curveData = curveStorage.getCurve("FromCalculated").getCurvePoints();

            try (PrintWriter writer = new PrintWriter(selectedFile)) {
                writer.println("Sensor Model:   " + metaData.getSensorModel());
                writer.println("Serial Number:  " + metaData.getSerialNumber());
                writer.println("Data Format:    " +
                        metaData.getDataFormat().getValue() + "      (" +
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

                        writer.printf(Locale.US, "%3d  %-11s   %s%n",
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

                jlbSavedFile.setText(selectedFile.getAbsolutePath());
            } catch (FileNotFoundException ex) {
                log.error("Ошибка сохранения файла", ex);
                doErrorMessage("Ошибка сохранения: " + ex.getMessage(), "Ошибка");
            }
        }
    }

    private void doErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);

    }

    private void initChart() {
        dataset.removeAllSeries();
        dataset.addSeries(fromFileSeries);
        dataset.addSeries(calculatedSeries);
        dataset.addSeries(fromDeviceSeries);

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
        // Настройки для КАЖДОЙ серии (0, 1, 2)
        // Серия 0 (fromFileSeries): Синий
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));

        // Серия 1 (calculatedSeries): Красный
        renderer.setSeriesPaint(1, Color.RED);
        renderer.setSeriesStroke(1, new BasicStroke(2.0f));

        // Серия 2 (fromDeviceSeries): Зеленый
        renderer.setSeriesPaint(2, Color.GREEN);
        renderer.setSeriesStroke(2, new BasicStroke(2.0f));

        plot.setRenderer(renderer);

        return chart;
    }



    private void fillCurveFormats() {
        jcbFromFileCurveFormat.removeAllItems();
        jcbCalculatedCurveFormat.removeAllItems();
        jcbFromDeviceCurveFormat.removeAllItems();
        for (CurveDataTypes dataType : CurveDataTypes.values()) {
            jcbFromFileCurveFormat.addItem(dataType);
            jcbCalculatedCurveFormat.addItem(dataType);
            jcbFromDeviceCurveFormat.addItem(dataType);
        }
    }

    private void fillSomeGuiElements() {
        jcbFileExtensionForSave.removeAllItems();
        jcbFileExtensionForSave.addItem("*.340");
        jcbFileExtensionForSave.addItem("*.curve");

        jcbFileSourceForSave.removeAllItems();
        jcbFileSourceForSave.addItem("From File");
        jcbFileSourceForSave.addItem("From Device");
        jcbFileSourceForSave.addItem("Calculated");

        jcbFileSourceForWrite.removeAllItems();
        jcbFileSourceForWrite.addItem("From File");
        jcbFileSourceForWrite.addItem("From Device");
        jcbFileSourceForWrite.addItem("Calculated");


        jcbFileSourceForCalc.removeAllItems();
        jcbFileSourceForCalc.addItem("From File");
        jcbFileSourceForCalc.addItem("From Device");


        //21-64
        jcbMemoryAddres.removeAllItems();
        for (int i = 21; i < 65; i++) {
            jcbMemoryAddres.addItem(i);
        }

    }

    private void updateCurveInfoFromFile(CurveMetaData curveMetaData) {
        if (curveMetaData.getSensorModel() != null)
            jtfFromFileCurveName.setText(curveMetaData.getSensorModel());
        if (curveMetaData.getSerialNumber() != null)
            jtfFromFileSerialNumber.setText(curveMetaData.getSerialNumber());
        if (curveMetaData.getDataFormat() != null)
            jcbFromFileCurveFormat.setSelectedIndex(curveMetaData.getDataFormat().getValue() - 1);
        if (curveMetaData.getSetPointLimit() != null)
            jtfFromFilePointLimit.setText(curveMetaData.getSetPointLimit().toString());
        if (curveMetaData.getTemperatureCoefficient() != null)
            jtfFromFileTermCoef.setText(curveMetaData.getTemperatureCoefficient());
        if (curveMetaData.getNumberOfBreakpoints() != null)
            jtfFromFileNumbeCounts.setText(curveMetaData.getNumberOfBreakpoints().toString());
    }

    private void updateCurveInfoCalculated(CurveMetaData curveMetaData) {
        if (curveMetaData.getSensorModel() != null)
            jtfCalculatedCurveName.setText(curveMetaData.getSensorModel());
        if (curveMetaData.getSerialNumber() != null)
            jtfCalculatedSerialNumber.setText(curveMetaData.getSerialNumber());
        if (curveMetaData.getDataFormat() != null)
            jcbCalculatedCurveFormat.setSelectedIndex(curveMetaData.getDataFormat().getValue() - 1);
        if (curveMetaData.getSetPointLimit() != null)
            jtfCalculatedPointLimit.setText(curveMetaData.getSetPointLimit().toString());
        if (curveMetaData.getTemperatureCoefficient() != null)
            jtfCalculatedTermCoef.setText(curveMetaData.getTemperatureCoefficient());
        if (curveMetaData.getNumberOfBreakpoints() != null)
            jtfCalculatedNumbeCounts.setText(curveMetaData.getNumberOfBreakpoints().toString());
    }

    private void updateCurveInfoFromDevice(CurveMetaData curveMetaData) {
        if (curveMetaData.getSensorModel() != null)
            jtfFromDeviceCurveName.setText(curveMetaData.getSensorModel());
        if (curveMetaData.getSerialNumber() != null)
            jtfFromDeviceSerialNumber.setText(curveMetaData.getSerialNumber());
        if (curveMetaData.getDataFormat() != null)
            jcbFromDeviceCurveFormat.setSelectedIndex(curveMetaData.getDataFormat().getValue() - 1);
        if (curveMetaData.getSetPointLimit() != null)
            jtfFromDevicePointLimit.setText(curveMetaData.getSetPointLimit().toString());
        if (curveMetaData.getTemperatureCoefficient() != null)
            jtfFromDeviceTermCoef.setText(curveMetaData.getTemperatureCoefficient());
        if (curveMetaData.getNumberOfBreakpoints() != null)
            jtfFromDeviceNumbeCounts.setText(curveMetaData.getNumberOfBreakpoints().toString());
    }


    private void updateDataTable(CurveData curveData, JTable table) {
        if(curveData == null){
            doErrorMessage("Для обновления таблицы 'из файла' передан пустой объект данных","Ошибочка");
            return;
        }

        if(curveData.getCurveMetaData() == null){
            doErrorMessage("Для обновления таблицы 'из файла' передан объект данных null полем MetaData","Ошибочка");
            return;
        }

        if(curveData.getCurveMetaData().getDataFormat() == null){
            doErrorMessage("Для обновления таблицы 'из файла' передан объект данных MetaData null полем DataFormat","Ошибочка");
            return;
        }
        // Заголовки столбцов
        String[] header = curveData.getCurveMetaData().getDataFormat().getName().split("vs");

        //Заполнение таблицы
        // Добавляем новые точки данных через лямбдочку
        ArrayList<String[]> rowsList = new ArrayList<>();
        curveData.getCurvePoints().forEach(it -> {
            rowsList.add(new String[]{String.valueOf(it.getKey()), String.valueOf(it.getValue())});
        });

        // Установка модели таблицы
        table.setModel(new DefaultTableModel(rowsList.toArray(new String[0][0]), header));
    }

    private void buildGraph(CurveData curveData, XYSeries series) {

        series.clear();
        if(curveData == null){
            if(series != null){
                doErrorMessage("В график переданы null данные для " + series.getDescription(), "Ужасы...");
            }else{
                doErrorMessage("В график переданы null данные и их контейнер", "Ужасы...");
            }
            jlbStatus.setText("Нет данных для графика");
            return;

        }
        curveData.getCurvePoints().forEach(it -> {
            series.add(it.getKey(), it.getValue());
        });
        repaintChart();
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
//Количество строк 1152

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        mainPane = new JPanel();
        mainPane.setLayout(new GridLayoutManager(8, 3, new Insets(0, 0, 0, 0), -1, -1));
        comConnection = new JPanel();
        comConnection.setLayout(new GridLayoutManager(8, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(comConnection, new GridConstraints(0, 0, 8, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbComPortNumber = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        jcbComPortNumber.setModel(defaultComboBoxModel1);
        comConnection.add(jcbComPortNumber, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComBarity = new JComboBox();
        comConnection.add(jcbComBarity, new GridConstraints(1, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComSpeed = new JComboBox();
        comConnection.add(jcbComSpeed, new GridConstraints(2, 0, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbOpenComPort = new JButton();
        jbOpenComPort.setText("Открыть com-порт");
        comConnection.add(jbOpenComPort, new GridConstraints(3, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCloseComPort = new JButton();
        jbCloseComPort.setText("Закрыть com-порт");
        comConnection.add(jbCloseComPort, new GridConstraints(4, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpnColorStatus = new JPanel();
        jpnColorStatus.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        comConnection.add(jpnColorStatus, new GridConstraints(6, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jlbStatus = new JLabel();
        jlbStatus.setText("Готов");
        jpnColorStatus.add(jlbStatus, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdGetState = new JButton();
        jbCmdGetState.setText("Запрос состояния");
        jpnColorStatus.add(jbCmdGetState, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        tabbedPane1 = new JTabbedPane();
        comConnection.add(tabbedPane1, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Пользовательские", panel1);
        jspReadetNamesUsersCurves = new JScrollPane();
        panel1.add(jspReadetNamesUsersCurves, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Стандартные", panel2);
        jspReadetNamesStandartCurves = new JScrollPane();
        panel2.add(jspReadetNamesStandartCurves, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jbCmdSleep = new JButton();
        jbCmdSleep.setText("Команда SLEEP");
        comConnection.add(jbCmdSleep, new GridConstraints(5, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdWakeUp = new JButton();
        jbCmdWakeUp.setText("Команда WAKEUP");
        comConnection.add(jbCmdWakeUp, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        fileSelect = new JPanel();
        fileSelect.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(fileSelect, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbtSelectFile = new JButton();
        jbtSelectFile.setText("Открыть файл");
        fileSelect.add(jbtSelectFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        fileSelect.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpnGraph = new JPanel();
        jpnGraph.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpnGraph, new GridConstraints(7, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(200, 200), new Dimension(600, 400), new Dimension(800, 1000), 0, false));
        jpFileSave = new JPanel();
        jpFileSave.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpFileSave, new GridConstraints(2, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbFileExtensionForSave = new JComboBox();
        jpFileSave.add(jcbFileExtensionForSave, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtSaveFile = new JButton();
        jbtSaveFile.setText("Записть в файл");
        jpFileSave.add(jbtSaveFile, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbFileSourceForSave = new JComboBox();
        jpFileSave.add(jcbFileSourceForSave, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpFileWrite = new JPanel();
        jpFileWrite.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpFileWrite, new GridConstraints(3, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbtWrite = new JButton();
        jbtWrite.setText("Записать в прибор");
        jpFileWrite.add(jbtWrite, new GridConstraints(1, 3, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpnSavedFile = new JPanel();
        jpnSavedFile.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpFileWrite.add(jpnSavedFile, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jspSavedFile = new JScrollPane();
        jpnSavedFile.add(jspSavedFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jlbSavedFile = new JLabel();
        jlbSavedFile.setText(" ");
        jspSavedFile.setViewportView(jlbSavedFile);
        jcbMemoryAddres = new JComboBox();
        jpFileWrite.add(jcbMemoryAddres, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbFileSourceForWrite = new JComboBox();
        jpFileWrite.add(jcbFileSourceForWrite, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpEditGraph = new JPanel();
        jpEditGraph.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpEditGraph, new GridConstraints(5, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jtfKelvinMeasured = new JTextField();
        jtfKelvinMeasured.setText("296");
        jpEditGraph.add(jtfKelvinMeasured, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jtfVoltsMeasured = new JTextField();
        jtfVoltsMeasured.setText("0,89586");
        jpEditGraph.add(jtfVoltsMeasured, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jbtCalculate = new JButton();
        jbtCalculate.setText("Рассчитать");
        jpEditGraph.add(jbtCalculate, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jlbNearestPoint = new JLabel();
        jlbNearestPoint.setText("Температура");
        jpEditGraph.add(jlbNearestPoint, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jlbAddingVolts = new JLabel();
        jlbAddingVolts.setText("Напряжение");
        jpEditGraph.add(jlbAddingVolts, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbFileSourceForCalc = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        jcbFileSourceForCalc.setModel(defaultComboBoxModel2);
        jpEditGraph.add(jcbFileSourceForCalc, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Данные");
        jpEditGraph.add(label1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpGraphActions = new JPanel();
        jpGraphActions.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpGraphActions, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbtClearGraph = new JButton();
        jbtClearGraph.setText("Очистить график");
        jpGraphActions.add(jbtClearGraph, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtAddPolyCalculated = new JButton();
        jbtAddPolyCalculated.setText("Построить из рассчитанного");
        jpGraphActions.add(jbtAddPolyCalculated, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtAddPolyFromFile = new JButton();
        jbtAddPolyFromFile.setText("Построить из файла");
        jpGraphActions.add(jbtAddPolyFromFile, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbtAddPolyFromDevice = new JButton();
        jbtAddPolyFromDevice.setText("Построить из прибора");
        jpGraphActions.add(jbtAddPolyFromDevice, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpDataTranserProggres = new JPanel();
        jpDataTranserProggres.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpDataTranserProggres, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jpbCommandSending = new JProgressBar();
        jpDataTranserProggres.add(jpbCommandSending, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtpFilePreview = new JTabbedPane();
        mainPane.add(jtpFilePreview, new GridConstraints(0, 1, 8, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null, 0, false));
        jtpFromFileCurvePreview = new JPanel();
        jtpFromFileCurvePreview.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jtpFilePreview.addTab("Из файла", jtpFromFileCurvePreview);
        jpFromFileCurvePreview = new JPanel();
        jpFromFileCurvePreview.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        jtpFromFileCurvePreview.add(jpFromFileCurvePreview, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, 300), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpFromFileCurveInformation = new JPanel();
        jpFromFileCurveInformation.setLayout(new GridLayoutManager(7, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurvePreview.add(jpFromFileCurveInformation, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpFromFileCurveName = new JPanel();
        jpFromFileCurveName.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurveInformation.add(jpFromFileCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromFileCurveName = new JLabel();
        jlbFromFileCurveName.setText("Curve Name");
        jpFromFileCurveName.add(jlbFromFileCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromFileCurveName = new JTextField();
        jtfFromFileCurveName.setEditable(false);
        jtfFromFileCurveName.setEnabled(false);
        jpFromFileCurveName.add(jtfFromFileCurveName, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer2 = new Spacer();
        jpFromFileCurveName.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromFileSerialNumber = new JPanel();
        jpFromFileSerialNumber.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurveInformation.add(jpFromFileSerialNumber, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromFileSerialNumber = new JLabel();
        jlbFromFileSerialNumber.setText("Serial Number");
        jpFromFileSerialNumber.add(jlbFromFileSerialNumber, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromFileSerialNumber = new JTextField();
        jtfFromFileSerialNumber.setEditable(false);
        jtfFromFileSerialNumber.setEnabled(false);
        jpFromFileSerialNumber.add(jtfFromFileSerialNumber, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        jpFromFileSerialNumber.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromFilePointLimit = new JPanel();
        jpFromFilePointLimit.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurveInformation.add(jpFromFilePointLimit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromFilePointLimit = new JLabel();
        jlbFromFilePointLimit.setText("Point Limit");
        jpFromFilePointLimit.add(jlbFromFilePointLimit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromFilePointLimit = new JTextField();
        jtfFromFilePointLimit.setEditable(false);
        jtfFromFilePointLimit.setEnabled(false);
        jpFromFilePointLimit.add(jtfFromFilePointLimit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        jpFromFilePointLimit.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromFileCurveFormat = new JPanel();
        jpFromFileCurveFormat.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurveInformation.add(jpFromFileCurveFormat, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromFileCurveFormat = new JLabel();
        jlbFromFileCurveFormat.setText("Curve Format");
        jpFromFileCurveFormat.add(jlbFromFileCurveFormat, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbFromFileCurveFormat = new JComboBox();
        jcbFromFileCurveFormat.setEnabled(false);
        jpFromFileCurveFormat.add(jcbFromFileCurveFormat, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer5 = new Spacer();
        jpFromFileCurveFormat.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromFileTemperatCoeff = new JPanel();
        jpFromFileTemperatCoeff.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurveInformation.add(jpFromFileTemperatCoeff, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromFileTermCoef = new JLabel();
        jlbFromFileTermCoef.setText("Term koef type");
        jpFromFileTemperatCoeff.add(jlbFromFileTermCoef, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromFileTermCoef = new JTextField();
        jtfFromFileTermCoef.setEditable(false);
        jtfFromFileTermCoef.setEnabled(false);
        jpFromFileTemperatCoeff.add(jtfFromFileTermCoef, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer6 = new Spacer();
        jpFromFileTemperatCoeff.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromFileNumberOfBreakpoints = new JPanel();
        jpFromFileNumberOfBreakpoints.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurveInformation.add(jpFromFileNumberOfBreakpoints, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromFileNumbeCounts = new JLabel();
        jlbFromFileNumbeCounts.setText("Points Count");
        jpFromFileNumberOfBreakpoints.add(jlbFromFileNumbeCounts, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromFileNumbeCounts = new JTextField();
        jtfFromFileNumbeCounts.setEditable(false);
        jtfFromFileNumbeCounts.setEnabled(false);
        jpFromFileNumberOfBreakpoints.add(jtfFromFileNumbeCounts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer7 = new Spacer();
        jpFromFileNumberOfBreakpoints.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromFileTable = new JPanel();
        jpFromFileTable.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromFileCurvePreview.add(jpFromFileTable, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jspFromFileTable = new JScrollPane();
        jpFromFileTable.add(jspFromFileTable, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 200), new Dimension(200, 350), new Dimension(250, 400), 0, false));
        jtbFromFilePreview = new JTable();
        jspFromFileTable.setViewportView(jtbFromFilePreview);
        final Spacer spacer8 = new Spacer();
        jpFromFileTable.add(spacer8, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer9 = new Spacer();
        jpFromFileTable.add(spacer9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jtpCalculatedCurvePreview = new JPanel();
        jtpCalculatedCurvePreview.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jtpFilePreview.addTab("Рассчитано", jtpCalculatedCurvePreview);
        jpCalculatedCurvePreview = new JPanel();
        jpCalculatedCurvePreview.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        jtpCalculatedCurvePreview.add(jpCalculatedCurvePreview, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCalculatedCurveInformation = new JPanel();
        jpCalculatedCurveInformation.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurvePreview.add(jpCalculatedCurveInformation, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpnCalculatedCurveName = new JPanel();
        jpnCalculatedCurveName.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurveInformation.add(jpnCalculatedCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCalculatedCurveName = new JLabel();
        jlbCalculatedCurveName.setText("Имя кривой");
        jpnCalculatedCurveName.add(jlbCalculatedCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCalculatedCurveName = new JTextField();
        jtfCalculatedCurveName.setEditable(true);
        jtfCalculatedCurveName.setEnabled(true);
        jpnCalculatedCurveName.add(jtfCalculatedCurveName, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer10 = new Spacer();
        jpnCalculatedCurveName.add(spacer10, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCalculatedSerialNumber = new JPanel();
        jpCalculatedSerialNumber.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurveInformation.add(jpCalculatedSerialNumber, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCalculatedSerialNumber = new JLabel();
        jlbCalculatedSerialNumber.setText("Имя сенсора");
        jpCalculatedSerialNumber.add(jlbCalculatedSerialNumber, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCalculatedSerialNumber = new JTextField();
        jtfCalculatedSerialNumber.setEditable(true);
        jtfCalculatedSerialNumber.setEnabled(true);
        jpCalculatedSerialNumber.add(jtfCalculatedSerialNumber, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer11 = new Spacer();
        jpCalculatedSerialNumber.add(spacer11, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCalculatedPointLimit = new JPanel();
        jpCalculatedPointLimit.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurveInformation.add(jpCalculatedPointLimit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlpCalculatedPointLimit = new JLabel();
        jlpCalculatedPointLimit.setText("Макс. знач. (K)");
        jpCalculatedPointLimit.add(jlpCalculatedPointLimit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCalculatedPointLimit = new JTextField();
        jtfCalculatedPointLimit.setEditable(true);
        jtfCalculatedPointLimit.setEnabled(true);
        jpCalculatedPointLimit.add(jtfCalculatedPointLimit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer12 = new Spacer();
        jpCalculatedPointLimit.add(spacer12, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCalculatedCurveFormat = new JPanel();
        jpCalculatedCurveFormat.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurveInformation.add(jpCalculatedCurveFormat, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCalculatedCurveFormat = new JLabel();
        jlbCalculatedCurveFormat.setText("Формат кривой");
        jpCalculatedCurveFormat.add(jlbCalculatedCurveFormat, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbCalculatedCurveFormat = new JComboBox();
        jcbCalculatedCurveFormat.setEditable(true);
        jcbCalculatedCurveFormat.setEnabled(true);
        jpCalculatedCurveFormat.add(jcbCalculatedCurveFormat, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer13 = new Spacer();
        jpCalculatedCurveFormat.add(spacer13, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCalculatedTemperatCoeff = new JPanel();
        jpCalculatedTemperatCoeff.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurveInformation.add(jpCalculatedTemperatCoeff, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCalculatedTermCoef = new JLabel();
        jlbCalculatedTermCoef.setText("Зависимость");
        jpCalculatedTemperatCoeff.add(jlbCalculatedTermCoef, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCalculatedTermCoef = new JTextField();
        jtfCalculatedTermCoef.setEditable(true);
        jtfCalculatedTermCoef.setEnabled(true);
        jpCalculatedTemperatCoeff.add(jtfCalculatedTermCoef, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer14 = new Spacer();
        jpCalculatedTemperatCoeff.add(spacer14, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCalculatedNumberOfBreakpoints = new JPanel();
        jpCalculatedNumberOfBreakpoints.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurveInformation.add(jpCalculatedNumberOfBreakpoints, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbCalculatedNumbeCounts = new JLabel();
        jlbCalculatedNumbeCounts.setText("Точек в кривой");
        jpCalculatedNumberOfBreakpoints.add(jlbCalculatedNumbeCounts, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCalculatedNumbeCounts = new JTextField();
        jtfCalculatedNumbeCounts.setEditable(true);
        jtfCalculatedNumbeCounts.setEnabled(true);
        jpCalculatedNumberOfBreakpoints.add(jtfCalculatedNumbeCounts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer15 = new Spacer();
        jpCalculatedNumberOfBreakpoints.add(spacer15, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpCalculatedTable = new JPanel();
        jpCalculatedTable.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCalculatedCurvePreview.add(jpCalculatedTable, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jspCalculatedTable = new JScrollPane();
        jpCalculatedTable.add(jspCalculatedTable, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 200), new Dimension(200, 350), new Dimension(250, 400), 0, false));
        jtbCalculatedPreview = new JTable();
        jspCalculatedTable.setViewportView(jtbCalculatedPreview);
        final Spacer spacer16 = new Spacer();
        jpCalculatedTable.add(spacer16, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer17 = new Spacer();
        jpCalculatedTable.add(spacer17, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jtpFromDeviceCurvePreview = new JPanel();
        jtpFromDeviceCurvePreview.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jtpFilePreview.addTab("Из прибора", jtpFromDeviceCurvePreview);
        jpFromDeviceCurvePreview = new JPanel();
        jpFromDeviceCurvePreview.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        jtpFromDeviceCurvePreview.add(jpFromDeviceCurvePreview, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpCurveInformationSelected = new JPanel();
        jpCurveInformationSelected.setLayout(new GridLayoutManager(6, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpFromDeviceCurvePreview.add(jpCurveInformationSelected, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(300, -1), new Dimension(320, -1), new Dimension(350, -1), 0, false));
        jpFromDeviceCurveName = new JPanel();
        jpFromDeviceCurveName.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationSelected.add(jpFromDeviceCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromDeviceCurveName = new JLabel();
        jlbFromDeviceCurveName.setText("Curve Name");
        jpFromDeviceCurveName.add(jlbFromDeviceCurveName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromDeviceCurveName = new JTextField();
        jtfFromDeviceCurveName.setEditable(false);
        jtfFromDeviceCurveName.setEnabled(false);
        jpFromDeviceCurveName.add(jtfFromDeviceCurveName, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer18 = new Spacer();
        jpFromDeviceCurveName.add(spacer18, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromDeviceSerialNumber = new JPanel();
        jpFromDeviceSerialNumber.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationSelected.add(jpFromDeviceSerialNumber, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromDeviceSerialNumber = new JLabel();
        jlbFromDeviceSerialNumber.setText("Serial Number");
        jpFromDeviceSerialNumber.add(jlbFromDeviceSerialNumber, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromDeviceSerialNumber = new JTextField();
        jtfFromDeviceSerialNumber.setEditable(false);
        jtfFromDeviceSerialNumber.setEnabled(false);
        jpFromDeviceSerialNumber.add(jtfFromDeviceSerialNumber, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer19 = new Spacer();
        jpFromDeviceSerialNumber.add(spacer19, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromDevicePointLimit = new JPanel();
        jpFromDevicePointLimit.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationSelected.add(jpFromDevicePointLimit, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromDevicePointLimit = new JLabel();
        jlbFromDevicePointLimit.setText("Point Limit");
        jpFromDevicePointLimit.add(jlbFromDevicePointLimit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromDevicePointLimit = new JTextField();
        jtfFromDevicePointLimit.setEditable(false);
        jtfFromDevicePointLimit.setEnabled(false);
        jpFromDevicePointLimit.add(jtfFromDevicePointLimit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer20 = new Spacer();
        jpFromDevicePointLimit.add(spacer20, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromDeviceCurveFormat = new JPanel();
        jpFromDeviceCurveFormat.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationSelected.add(jpFromDeviceCurveFormat, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromDeviceCurveFormat = new JLabel();
        jlbFromDeviceCurveFormat.setText("Curve Format");
        jpFromDeviceCurveFormat.add(jlbFromDeviceCurveFormat, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbFromDeviceCurveFormat = new JComboBox();
        jcbFromDeviceCurveFormat.setEditable(false);
        jcbFromDeviceCurveFormat.setEnabled(false);
        jpFromDeviceCurveFormat.add(jcbFromDeviceCurveFormat, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer21 = new Spacer();
        jpFromDeviceCurveFormat.add(spacer21, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromDeviceTemperatCoeff = new JPanel();
        jpFromDeviceTemperatCoeff.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationSelected.add(jpFromDeviceTemperatCoeff, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromDeviceTermCoef = new JLabel();
        jlbFromDeviceTermCoef.setText("Term koef type");
        jpFromDeviceTemperatCoeff.add(jlbFromDeviceTermCoef, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromDeviceTermCoef = new JTextField();
        jtfFromDeviceTermCoef.setEditable(false);
        jtfFromDeviceTermCoef.setEnabled(false);
        jpFromDeviceTemperatCoeff.add(jtfFromDeviceTermCoef, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer22 = new Spacer();
        jpFromDeviceTemperatCoeff.add(spacer22, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromDeviceNumberOfBreakpoints = new JPanel();
        jpFromDeviceNumberOfBreakpoints.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpCurveInformationSelected.add(jpFromDeviceNumberOfBreakpoints, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(150, -1), new Dimension(200, -1), new Dimension(200, -1), 0, false));
        jlbFromDeviceNumbeCounts = new JLabel();
        jlbFromDeviceNumbeCounts.setText("Points Count");
        jpFromDeviceNumberOfBreakpoints.add(jlbFromDeviceNumbeCounts, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfFromDeviceNumbeCounts = new JTextField();
        jtfFromDeviceNumbeCounts.setEditable(false);
        jtfFromDeviceNumbeCounts.setEnabled(false);
        jpFromDeviceNumberOfBreakpoints.add(jtfFromDeviceNumbeCounts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(80, -1), new Dimension(100, -1), new Dimension(100, -1), 0, false));
        final Spacer spacer23 = new Spacer();
        jpFromDeviceNumberOfBreakpoints.add(spacer23, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpFromDeviceTable = new JPanel();
        jpFromDeviceTable.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpFromDeviceCurvePreview.add(jpFromDeviceTable, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jspFromDeviceTable = new JScrollPane();
        jpFromDeviceTable.add(jspFromDeviceTable, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 200), new Dimension(200, 350), new Dimension(250, 400), 0, false));
        jtbFromDevicePreview = new JTable();
        jspFromDeviceTable.setViewportView(jtbFromDevicePreview);
        final Spacer spacer24 = new Spacer();
        jpFromDeviceTable.add(spacer24, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final Spacer spacer25 = new Spacer();
        jpFromDeviceTable.add(spacer25, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpnSelectedFile = new JPanel();
        jpnSelectedFile.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(jpnSelectedFile, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jspSelectedFile = new JScrollPane();
        jpnSelectedFile.add(jspSelectedFile, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        jlbSelectedFile = new JLabel();
        jlbSelectedFile.setText(" ");
        jspSelectedFile.setViewportView(jlbSelectedFile);
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
