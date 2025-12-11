package org.example.gui.accu10fd;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.example.gui.Rendeble;
import org.example.gui.accu10fd.table.AcuTableCreator;
import org.example.gui.accu10fd.table.AcuTableFileHandler;
import org.example.gui.accu10fd.table.GasData;
import org.example.gui.components.DecimalSpinner;
import org.example.gui.curve.file.Serialization;
import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.ParityList;
import org.example.utilites.MyUtilities;
import org.example.utilites.properties.MyProperties;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.List;
import java.util.function.IntFunction;

public class Acu10fdWindow extends JFrame implements Rendeble {
    private Logger log = null;
    private final MyProperties prop;
    private final Serialization ser = new Serialization();

    private JPanel mainPane;
    private JPanel comConnection;
    private JComboBox jcbComPortNumber;
    private JComboBox jcbComBarity;
    private JComboBox jcbComSpeed;
    private JButton jbOpenComPort;
    private JButton jbCloseComPort;
    private JPanel jpnColorStatus;
    private JLabel jlbStatus;
    private JComboBox jcbControlMode;
    private JButton jbCmdSetControlMode;
    private JButton jbCmdReadInstantFlow;
    private JButton jbCmdReadCumulativeFlow;
    private JButton jbCmdResetCumulativeFlow;
    private JSpinner jspTargetFlow;
    private JButton jbCmdSetFlow;
    private JButton jbCmdZeroing;
    private JButton jbCmdResetZeroing;
    private JPanel componentA;
    private JComboBox jcbFirstComponentName;
    private JSpinner jsFirstComponentConcentration;
    private JComboBox jcbSecondComponentName;
    private JSpinner jsSecondComponentConcentration;
    private JCheckBox jcbBackgroundDataPool;
    private JButton jbCmdCalculate;
    private JTextField jtfCalculateResult;
    private JButton jbCmdSetGasCoefficient;
    private JButton jbSearch;
    private JTextArea jtaStatus;
    private JButton jbCmdCalculateZeroGas;

    private SerialPort comPort;
    private final AcuTableFileHandler acuTableFileHandler;
    private double calculatedCoefficient;
    private boolean poolState;
    private Acu10fsCommander acu10fsCommander;

    public Acu10fdWindow(MyProperties prop) {
        $$$setupUI$$$();

        log = Logger.getLogger(Acu10fdWindow.class);
        this.prop = prop;
        setContentPane(mainPane);
        try {
            AcuTableCreator.createDefaultIfNotExists();
        } catch (IOException e) {
            doErrorMessage(e.getMessage(), "Ошибка работы с файлом коэффициентов");
        }
        AcuTableFileHandler tmpAcu = null;
        try {
            tmpAcu = new AcuTableFileHandler();
        } catch (IOException e) {
            doErrorMessage(e.getMessage(), "Разбора файла коэффициентов");
        }
        acuTableFileHandler = tmpAcu;
        tmpAcu = null;


        updateComPortList();
        //Заполнение полей комбо-боксов
        initComboBox(jcbComBarity,
                ParityList.values(),
                i -> String.valueOf(ParityList.getNameLikeArray(i)));

        initComboBox(jcbComSpeed,
                BaudRatesList.values(),
                i -> String.valueOf(BaudRatesList.getNameLikeArray(i)));

        initComboBox(jcbControlMode,
                ControlMod.values(),
                i -> String.valueOf(ControlMod.getNameLikeArray(i)));

        if (acuTableFileHandler != null) {
            initComboBoxStrings(jcbFirstComponentName,
                    acuTableFileHandler.getAllNames(),
                    i -> acuTableFileHandler.getAllNames().get(i));

            initComboBoxStrings(jcbSecondComponentName,
                    acuTableFileHandler.getAllNames(),
                    i -> acuTableFileHandler.getAllNames().get(i));
        }


        jbOpenComPort.addActionListener(this::openPortActionHandler);
        jbCloseComPort.addActionListener(this::closePortActionHandler);
        jbCmdReadInstantFlow.addActionListener(this::readInstantaneousFlow);
        jbCmdReadCumulativeFlow.addActionListener(this::readCumulativeFlow);
        jbCmdResetCumulativeFlow.addActionListener((this::clearCumulativeFlow));
        jbCmdSetControlMode.addActionListener(this::setControlMode);
        jbCmdZeroing.addActionListener(this::setZeroPoint);
        jbCmdResetZeroing.addActionListener(this::cancelZeroPoint);
        jbCmdCalculate.addActionListener(this::cmdCalculate);
        jbCmdSetGasCoefficient.addActionListener(this::setCoefficient);
        jcbBackgroundDataPool.addActionListener(this::changePoolState);
        jbSearch.addActionListener(this::testComPortSpeedsHandler);
        jbCmdCalculateZeroGas.addActionListener(this::calculateZeroGas);
        acu10fsCommander = new Acu10fsCommander(comPort);

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent windowEvent) {
                log.info("Закрытие окна работы с расходомером:");
                poolState = false;
                if (comPort != null && comPort.isOpen()) {
                    comPort.closePort();
                }

                if (acu10fsCommander != null && acu10fsCommander.getComPort() != null) {
                    acu10fsCommander.getComPort().closePort();
                }

            }
        });

        if (prop != null && prop.getPortAcu10fd() != 0) {
            jcbComPortNumber.setSelectedIndex(prop.getPortAcu10fd());
        }

        if (prop != null && prop.getSpeedAcu10fd() != 0) {
            jcbComSpeed.setSelectedIndex(prop.getSpeedAcu10fd());
        }

        if (prop != null && prop.getFirstGasAcu10fd() != 0) {
            jcbFirstComponentName.setSelectedIndex(prop.getFirstGasAcu10fd());
        }

        if (prop != null && prop.getSecondGasAcu10fd() != 0) {
            jcbSecondComponentName.setSelectedIndex(prop.getSecondGasAcu10fd());
        }

        if (prop != null && prop.getFirstGasAcu10fdConcentration() != 0) {
            jsFirstComponentConcentration.setValue(prop.getFirstGasAcu10fdConcentration());
        }

        if (prop != null && prop.getSecondGasAcu10fdConcentration() != 0) {
            jsSecondComponentConcentration.setValue(prop.getSecondGasAcu10fdConcentration());
        }


    }

    private void changePoolState(ActionEvent actionEvent) {
        log.info("Change pool state");
        if (poolState == false) {
            if (acu10fsCommander.isPortConsistent()) {
                poolState = jcbBackgroundDataPool.isSelected();
            } else {
                jcbBackgroundDataPool.setSelected(false);
            }
        } else {
            poolState = jcbBackgroundDataPool.isSelected();
        }

    }

    private void calculateZeroGas(ActionEvent actionEvent) {
        double conc1 = (Double) jsFirstComponentConcentration.getValue();
        double conc2 = 100.0 - conc1;
        jsSecondComponentConcentration.setValue(conc2);
    }

    private void backgroundPool() {
        if (poolState == false) {
            return;
        }
        if (!acu10fsCommander.isPortConsistent()) {
            poolState = false;
            jcbBackgroundDataPool.setSelected(false);
            return;
        }
        String firstAskResult = null;
        String firstAskError = null;
        String secondAskResult = null;
        String secondAskError = null;

        if (acu10fsCommander.isBusy()) return;
        try {
            firstAskResult = String.valueOf(acu10fsCommander.readCumulativeFlow());
        } catch (Exception e) {
            firstAskError = "Ошибка запроса в фоне CumulativeFlow \n" + e.getMessage();
            log.info(firstAskError);
        }

        try {
            secondAskResult = String.valueOf(acu10fsCommander.readInstantaneousFlow());
        } catch (Exception e) {
            secondAskError = "Ошибка запроса в фоне InstantaneousFlow \n" + e.getMessage();
            log.info(secondAskError);
        }

        String resultOne;
        String resultSecond;
        if (firstAskResult != null) {
            resultOne = "Текущий накопленный расход: \n" + firstAskResult;
        } else if (firstAskError != null) {
            resultOne = "Текущий накопленный расход \nне был считан: \n" + firstAskError;
        } else {
            resultOne = "Текущий накопленный расход \nне был считан: \n(неизвестная ошибка)";
        }

        if (secondAskResult != null) {
            resultSecond = "\nТекущий мгновенный расход: \n" + secondAskResult;
        } else if (secondAskError != null) {
            resultSecond = "\nТекущий мгновенный расход \nне был считан: \n" + secondAskError;
        } else {
            resultSecond = "\nТекущий мгновенный расход \nне был считан: \n(неизвестная ошибка)";
        }

        jtaStatus.setText(resultOne + resultSecond);

    }

    private void openPortActionHandler(ActionEvent actionEvent) {
        Integer selectedBaudRate = null;
        Integer selectedParity = null;

        jtaStatus.setText("Открываю");
        if (comPort != null) {
            if (comPort.isOpen()) {
                comPort.closePort();
                jtaStatus.setText("Переоткрываю...");
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
                jtaStatus.setText("Выбранный порт не найден в системе о.о");
                return;
            }

            comPort = ports[jcbComPortNumber.getSelectedIndex()];
            if (comPort == null) {
                jtaStatus.setText("Выбранный порт оказался null");
                return;
            }
        } catch (Exception e) {
            jtaStatus.setText("Ошибка разбора параметров");
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
                jtaStatus.setText("Порт " + comPort.getDescriptivePortName() + " открыт");
            } else {
                doErrorMessage("Не удалось открыть порт. Код ошибки: " + comPort.getLastErrorCode(), "Такое иногда случается...");
            }
        } catch (Exception e) {
            jtaStatus.setText("Ошибка открытия порта");
            log.error("Ошибка при открытии COM-порта", e);
            doErrorMessage("Ошибка открытия порта:" + e.getMessage(), "Такое иногда случается...");
        }
        acu10fsCommander.setComPort(comPort);
        prop.setPortAcu10fd(jcbComPortNumber.getSelectedIndex());
        prop.setSpeedAcu10fd(jcbComSpeed.getSelectedIndex());
    }

    private void closePortActionHandler(ActionEvent actionEvent) {
        if (comPort != null && comPort.isOpen()) {
            comPort.closePort();
            jtaStatus.setText("Порт закрыт");
            poolState = false;
            jcbBackgroundDataPool.setSelected(false);
        }
        if (acu10fsCommander != null && acu10fsCommander.getComPort() != null) {
            acu10fsCommander.getComPort().closePort();
        }
    }

    private void cmdCalculate(ActionEvent actionEvent) {
        // Получаем введенные концентрации
        double conc1 = (Double) jsFirstComponentConcentration.getValue();
        double conc2 = (Double) jsSecondComponentConcentration.getValue();
        double totalConc = conc1 + conc2;
        if (totalConc != 100) {
            doErrorMessage("Сумма объемов должна быть 100", "Ошибка введённых данных!");
        }
        // Получаем данные газов
        GasData firstGas = acuTableFileHandler.getByName((String) jcbFirstComponentName.getSelectedItem());
        GasData secondGas = acuTableFileHandler.getByName((String) jcbSecondComponentName.getSelectedItem());

        // Рассчитываем доли компонентов
        double w1 = conc1 / totalConc;
        double w2 = conc2 / totalConc;

        // Определяем молекулярные коэффициенты N (на основе типа молекул)
        double N1 = getMolecularCoefficient(firstGas.getName());
        double N2 = getMolecularCoefficient(secondGas.getName());

        // Рассчитываем коэффициент преобразования
        double numerator = 0.3106 * (N1 * w1 + N2 * w2);
        double denominator = (firstGas.getDensity() * firstGas.getSpecificHeat() * w1)
                + (secondGas.getDensity() * secondGas.getSpecificHeat() * w2);

        calculatedCoefficient = numerator / denominator;

        // Выводим результат
        log.info("Calculated coefficient: " + calculatedCoefficient);
        prop.setFirstGasAcu10fdConcentration(conc1);
        prop.setSecondGasAcu10fdConcentration(conc2);
        prop.setFirstGasAcu10fd(jcbFirstComponentName.getSelectedIndex());
        prop.setSecondGasAcu10fd(jcbSecondComponentName.getSelectedIndex());


        jtfCalculateResult.setText(String.valueOf(calculatedCoefficient));
    }

    // Метод для определения молекулярного коэффициента N
    private double getMolecularCoefficient(String gasName) {
        // Определяем тип молекулы по названию газа
        return switch (gasName) {
            case "Ar", "He" -> 1.01;    // Одноатомные
            case "CO", "N2" -> 1.00;     // Двухатомные
            case "CO2", "NO2" -> 0.94;   // Трехатомные
            default -> 0.88;              // Многоатомные (по умолчанию)
        };
    }

    private void setCoefficient(ActionEvent actionEvent) {
        if (calculatedCoefficient <= 0.0) {
            doErrorMessage("Не нужно задавать коэффициент равный или меньше нуля", "Ошибочка");
            return;
        }
        log.info("Double " + calculatedCoefficient);

        float coefficient = (float) calculatedCoefficient;
        log.info("Float " + coefficient);
        try {
            acu10fsCommander.setGasCoefficient(coefficient);
        } catch (Exception e) {
            doErrorMessage(e.getMessage(), "Err");
            throw new RuntimeException(e);
        }
    }

    private void readInstantaneousFlow(ActionEvent actionEvent) {
        float result = 5f;
        try {
            result = acu10fsCommander.readInstantaneousFlow();
        } catch (Exception e) {
            doErrorMessage(e.getMessage(), "Err");
            throw new RuntimeException(e);
        }
        jtaStatus.setText("Мгновенный расход: \n" + result);
    }

    private void readCumulativeFlow(ActionEvent actionEvent) {
        float result = 5f;
        try {
            result = acu10fsCommander.readCumulativeFlow();
        } catch (Exception e) {
            doErrorMessage(e.getMessage(), "Err");
            throw new RuntimeException(e);
        }
        jtaStatus.setText("Накопленный расход \n" + result);
    }

    private void clearCumulativeFlow(ActionEvent actionEvent) {
        try {
            acu10fsCommander.resetCumulativeFlow();
        } catch (Exception e) {
            doErrorMessage(e.getMessage(), "Err");
            throw new RuntimeException(e);
        }
        jtaStatus.setText("Накопленный расход сброшен\n");
    }

    private void setControlMode(ActionEvent actionEvent) {
        if (jcbControlMode.getSelectedItem().equals(ControlMod.ANALOG.getName())) {
            log.info("Set " + ControlMod.ANALOG + " mode");
            try {
                acu10fsCommander.setAnalogControlMode();
            } catch (Exception e) {
                doErrorMessage(e.getMessage(), "Err");
                throw new RuntimeException(e);
            }
            jtaStatus.setText("Установлен ANALOG режим работы\n");
        } else if (jcbControlMode.getSelectedItem().equals(ControlMod.DIGITAL.getName())) {
            log.info("Set " + ControlMod.DIGITAL + " mode");
            try {
                acu10fsCommander.setDigitalControlMode();
            } catch (Exception e) {
                doErrorMessage(e.getMessage(), "Err");
                throw new RuntimeException(e);
            }
            jtaStatus.setText("Установлен DIGITAL режим работы\n");
        } else {
            doErrorMessage("Неизвестный тип работы", "Ошибка");
        }
    }

    private void setZeroPoint(ActionEvent actionEvent) {
        log.info("Set zero point mode");
        try {
            acu10fsCommander.setZeroPoint();
        } catch (Exception e) {
            doErrorMessage(e.getMessage(), "Err");
            throw new RuntimeException(e);
        }
        jtaStatus.setText("Установлен нулевой уровень расхода\n");
    }

    private void cancelZeroPoint(ActionEvent actionEvent) {
        log.info("Cancel zero point mode");
        try {
            acu10fsCommander.cancelZeroPoint();
        } catch (Exception e) {
            doErrorMessage(e.getMessage(), "Err");
            throw new RuntimeException(e);
        }
        jtaStatus.setText("Отменён нулевой уровень расхода\n");
    }


    private void testComPortSpeedsHandler(ActionEvent actionEvent) {
        if (comPort == null || !comPort.isOpen()) {
            JOptionPane.showMessageDialog(this,
                    "Порт не открыт! Сначала откройте порт",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        Acu10fsCommander commander = new Acu10fsCommander(comPort);
        int foundBaud = commander.autoDetectBaudRate();

        if (foundBaud > 0) {
            // Применяем найденную скорость
            commander.applyOptimalBaudRate();

            // Обновляем UI
            int index = findBaudRateIndex(foundBaud);
            if (index >= 0) {
                jcbComSpeed.setSelectedIndex(index);
            }

            jtaStatus.setText("Найдена скорость: " + foundBaud + " бод");
            JOptionPane.showMessageDialog(this,
                    "Устройство найдено на скорости " + foundBaud + " бод",
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);
        } else {
            jtaStatus.setText("Устройство не найдено");
            JOptionPane.showMessageDialog(this,
                    "Не удалось определить скорость подключения",
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private int findBaudRateIndex(int baudRate) {
        for (int i = 0; i < jcbComSpeed.getItemCount(); i++) {
            String item = (String) jcbComSpeed.getItemAt(i);
            if (item.contains(String.valueOf(baudRate))) {
                return i;
            }
        }
        return -1;
    }

    // Вспомогательная функция для преобразования байтов в HEX-строку
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    private void doErrorMessage(String message, String title) {
        JOptionPane.showMessageDialog(null,
                message,
                title,
                JOptionPane.ERROR_MESSAGE);

    }

    private <T extends Enum<?>, U> void initComboBox(JComboBox<U> comboBox,
                                                     T[] values,
                                                     IntFunction<U> valueExtractor) {
        for (int i = 0; i < values.length; i++) {
            comboBox.addItem(valueExtractor.apply(i));
        }
    }

    private <T, U> void initComboBoxStrings(JComboBox<U> comboBox,
                                            List<T> values,
                                            IntFunction<U> valueExtractor) {

        for (int i = 0; i < values.size(); i++) {
            comboBox.addItem(valueExtractor.apply(i));
        }
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPane = new JPanel();
        mainPane.setLayout(new GridLayoutManager(14, 3, new Insets(0, 0, 0, 0), -1, -1));
        comConnection = new JPanel();
        comConnection.setLayout(new GridLayoutManager(8, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(comConnection, new GridConstraints(0, 0, 14, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, new Dimension(400, -1), 0, false));
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
        comConnection.add(jpnColorStatus, new GridConstraints(7, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jlbStatus = new JLabel();
        jlbStatus.setText("Готов");
        jpnColorStatus.add(jlbStatus, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtaStatus = new JTextArea();
        jpnColorStatus.add(jtaStatus, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        jcbControlMode = new JComboBox();
        jcbControlMode.setEnabled(false);
        comConnection.add(jcbControlMode, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdSetControlMode = new JButton();
        jbCmdSetControlMode.setEnabled(false);
        jbCmdSetControlMode.setText("Задать режим управления");
        comConnection.add(jbCmdSetControlMode, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbSearch = new JButton();
        jbSearch.setText("Поиск устройства (определение скорости)");
        comConnection.add(jbSearch, new GridConstraints(5, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdReadInstantFlow = new JButton();
        jbCmdReadInstantFlow.setText("Считать мгновенный расход");
        mainPane.add(jbCmdReadInstantFlow, new GridConstraints(0, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdReadCumulativeFlow = new JButton();
        jbCmdReadCumulativeFlow.setText("Считать накопленный расход");
        mainPane.add(jbCmdReadCumulativeFlow, new GridConstraints(1, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdResetCumulativeFlow = new JButton();
        jbCmdResetCumulativeFlow.setText("Сбросить накопленный расход");
        mainPane.add(jbCmdResetCumulativeFlow, new GridConstraints(2, 1, 2, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jspTargetFlow = new JSpinner();
        jspTargetFlow.setEnabled(false);
        mainPane.add(jspTargetFlow, new GridConstraints(4, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdSetFlow = new JButton();
        jbCmdSetFlow.setEnabled(false);
        jbCmdSetFlow.setText("Задать расход");
        mainPane.add(jbCmdSetFlow, new GridConstraints(4, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdZeroing = new JButton();
        jbCmdZeroing.setText("Калибровка нуля");
        mainPane.add(jbCmdZeroing, new GridConstraints(5, 1, 3, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdResetZeroing = new JButton();
        jbCmdResetZeroing.setText("Отмена калибровки нуля");
        mainPane.add(jbCmdResetZeroing, new GridConstraints(9, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(panel1, new GridConstraints(10, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        componentA = new JPanel();
        componentA.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(componentA, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbFirstComponentName = new JComboBox();
        componentA.add(jcbFirstComponentName, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        componentA.add(jsFirstComponentConcentration, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Первый компонент");
        componentA.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jcbSecondComponentName = new JComboBox();
        panel2.add(jcbSecondComponentName, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        panel2.add(jsSecondComponentConcentration, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Второй компонент");
        panel2.add(label2, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jbCmdCalculateZeroGas = new JButton();
        jbCmdCalculateZeroGas.setText("Вычислить");
        panel2.add(jbCmdCalculateZeroGas, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbBackgroundDataPool = new JCheckBox();
        jcbBackgroundDataPool.setText("Фоновый опрос данных");
        mainPane.add(jcbBackgroundDataPool, new GridConstraints(11, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPane.add(panel3, new GridConstraints(10, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbCmdCalculate = new JButton();
        jbCmdCalculate.setText("Рассчитать");
        panel4.add(jbCmdCalculate, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfCalculateResult = new JTextField();
        jtfCalculateResult.setEditable(false);
        jtfCalculateResult.setEnabled(true);
        panel4.add(jtfCalculateResult, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jbCmdSetGasCoefficient = new JButton();
        jbCmdSetGasCoefficient.setText("Задать");
        panel5.add(jbCmdSetGasCoefficient, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPane;
    }

    @Override
    public void renderData() {
        backgroundPool();
    }

    @Override
    public boolean isEnable() {
        return true;
    }


    private void createUIComponents() {
        //jsFirstComponentConcentration = new JSpinner();
        jsFirstComponentConcentration = new DecimalSpinner(95.6, 0.0, 100.0, 0.1);
        jsSecondComponentConcentration = new DecimalSpinner(4.4, 0.0, 100.0, 0.1);
    }

}
