package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.gui.names.CoefficientNames;
import org.example.gui.mgstest.model.answer.GetAllCoefficientsModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.cmd.SetCOCoefs;
import org.example.gui.mgstest.transport.cmd.SetH2SCoefs;
import org.example.gui.mgstest.transport.cmd.SetO2Polys;
import org.example.gui.mgstest.transport.cmd.GetAllCoefficients;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class TabCoefficients extends DeviceTab {
    private Logger log = Logger.getLogger(TabCoefficients.class);
    private CoefficientNames coefficientNamesNames = new CoefficientNames();
    @Setter
    private HidDevice selectedDevice;

    private final DeviceAsyncExecutor asyncExecutor;

    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private JButton refreshButton;

    // Панели для каждого типа коэффициентов
    private JPanel o2Panel;
    private JPanel coPanel;
    private JPanel h2sPanel;
    private JPanel ch4PressurePanel;
    private JPanel accelerationPanel;
    private JPanel ppmMgKoefsPanel;
    private JPanel vRangePanel;

    // Массивы полей для каждого типа коэффициентов
    private JTextField[] o2Fields;
    private JTextField[] coFields;
    private JTextField[] h2sFields;
    private JTextField[] ch4PressureFields;
    private JTextField[] accelerationFields;
    private JTextField[] ppmMgKoefsFields;
    private JTextField[] vRangeFields;

    // Формат для отображения чисел
    private DecimalFormat decimalFormat = new DecimalFormat();

    // Количество столбцов для коэффициентов
    private final int COLUMNS_COUNT = 3;

    enum CHANNELS {
        O2, CO, H2S, CH4_PRESSURE, ACCELERATION, PPM_MG_KOEFS, V_RANGE;
    }

    public TabCoefficients(HidDevice selectedDevice, DeviceAsyncExecutor asyncExecutor) {
        super("Коэффициенты");
        this.selectedDevice = selectedDevice;
        this.asyncExecutor = asyncExecutor;
        initComponents();
    }

    private void initComponents() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Создаем кнопку обновления
        createRefreshButton();

        // Создаем панели для каждого типа коэффициентов
        createO2Panel();
        createCoPanel();
        createH2sPanel();
        createCh4PressurePanel();
        createAccelerationPanel();
        createPpmMgKoefsPanel();
        createVRangePanel();

        // Добавляем кнопку обновления и все панели на главную панель
        mainPanel.add(refreshButton);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10))); // Отступ
        mainPanel.add(o2Panel);
        mainPanel.add(coPanel);
        mainPanel.add(h2sPanel);
        mainPanel.add(ch4PressurePanel);
        mainPanel.add(accelerationPanel);
        mainPanel.add(ppmMgKoefsPanel);
        mainPanel.add(vRangePanel);

        // Добавляем прокрутку
        scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
    }

    private void createRefreshButton() {
        refreshButton = new JButton("Обновить коэффициенты");
        refreshButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        refreshButton.addActionListener(e -> refreshCoefficients());
    }

    private void refreshCoefficients() {
        if (selectedDevice != null) {
            DeviceCommand command = new GetAllCoefficients();
            asyncExecutor.executeCommand(command, null, selectedDevice);
        } else {
            JOptionPane.showMessageDialog(panel, "Устройство не выбрано", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JPanel createCoefficientGrid(String[] labels, JTextField[] fields, boolean editable) {
        JPanel gridPanel = new JPanel(new GridLayout(0, COLUMNS_COUNT * 2, 5, 5));

        for (int i = 0; i < labels.length; i++) {
            gridPanel.add(new JLabel(labels[i]));
            fields[i] = new JTextField();
            fields[i].setEditable(editable);
            gridPanel.add(fields[i]);
        }

        return gridPanel;
    }

    private void createO2Panel() {
        o2Panel = new JPanel();
        o2Panel.setLayout(new BoxLayout(o2Panel, BoxLayout.Y_AXIS));
        o2Panel.setBorder(BorderFactory.createTitledBorder("O2 Coefficients (101-119)"));

        o2Fields = new JTextField[19];
        JPanel gridPanel = createCoefficientGrid(coefficientNamesNames.oxygen.toArray(new String[0]), o2Fields, true);

        o2Panel.add(gridPanel);

        // Добавляем кнопку "Задать" для O2
        JButton setO2Button = new JButton("Задать коэффициенты O2");
        setO2Button.addActionListener(e -> setCoefficientsForGas(CHANNELS.O2, o2Fields));
        setO2Button.setAlignmentX(Component.CENTER_ALIGNMENT);
        o2Panel.add(Box.createRigidArea(new Dimension(0, 5)));
        o2Panel.add(setO2Button);
    }

    private void createCoPanel() {
        coPanel = new JPanel();
        coPanel.setLayout(new BoxLayout(coPanel, BoxLayout.Y_AXIS));
        coPanel.setBorder(BorderFactory.createTitledBorder("CO Coefficients (201-214)"));

        coFields = new JTextField[14];
        JPanel gridPanel = createCoefficientGrid(coefficientNamesNames.coDt.toArray(new String[0]), coFields, true);

        coPanel.add(gridPanel);

        // Добавляем кнопку "Задать" для CO
        JButton setCoButton = new JButton("Задать коэффициенты CO");
        setCoButton.addActionListener(e -> setCoefficientsForGas(CHANNELS.CO, coFields));
        setCoButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        coPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        coPanel.add(setCoButton);
    }

    private void createH2sPanel() {
        h2sPanel = new JPanel();
        h2sPanel.setLayout(new BoxLayout(h2sPanel, BoxLayout.Y_AXIS));
        h2sPanel.setBorder(BorderFactory.createTitledBorder("H2S Coefficients (301-314)"));

        h2sFields = new JTextField[14];
        JPanel gridPanel = createCoefficientGrid(coefficientNamesNames.h2s.toArray(new String[0]), h2sFields, true);

        h2sPanel.add(gridPanel);

        // Добавляем кнопку "Задать" для H2S
        JButton setH2sButton = new JButton("Задать коэффициенты H2S");
        setH2sButton.addActionListener(e -> setCoefficientsForGas(CHANNELS.H2S, h2sFields));
        setH2sButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        h2sPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        h2sPanel.add(setH2sButton);
    }

    private void createCh4PressurePanel() {
        ch4PressurePanel = new JPanel();
        ch4PressurePanel.setLayout(new BoxLayout(ch4PressurePanel, BoxLayout.Y_AXIS));
        ch4PressurePanel.setBorder(BorderFactory.createTitledBorder("CH4 Pressure Coefficients (401-407)"));

        String[] labels = new String[7];
        for (int i = 0; i < 7; i++) {
            labels[i] = "Коэффициент " + (i + 401) + ":";
        }

        ch4PressureFields = new JTextField[7];
        JPanel gridPanel = createCoefficientGrid(labels, ch4PressureFields, false);

        ch4PressurePanel.add(gridPanel);
    }

    private void createAccelerationPanel() {
        accelerationPanel = new JPanel();
        accelerationPanel.setLayout(new BoxLayout(accelerationPanel, BoxLayout.Y_AXIS));
        accelerationPanel.setBorder(BorderFactory.createTitledBorder("Acceleration Coefficients (501-504)"));

        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) {
            labels[i] = "Коэффициент " + (i + 501) + ":";
        }

        accelerationFields = new JTextField[4];
        JPanel gridPanel = createCoefficientGrid(labels, accelerationFields, false);

        accelerationPanel.add(gridPanel);
    }

    private void createPpmMgKoefsPanel() {
        ppmMgKoefsPanel = new JPanel();
        ppmMgKoefsPanel.setLayout(new BoxLayout(ppmMgKoefsPanel, BoxLayout.Y_AXIS));
        ppmMgKoefsPanel.setBorder(BorderFactory.createTitledBorder("PPM Mg Coefficients (601-604)"));

        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) {
            labels[i] = "Коэффициент " + (i + 601) + ":";
        }

        ppmMgKoefsFields = new JTextField[4];
        JPanel gridPanel = createCoefficientGrid(labels, ppmMgKoefsFields, false);

        ppmMgKoefsPanel.add(gridPanel);
    }

    private void createVRangePanel() {
        vRangePanel = new JPanel();
        vRangePanel.setLayout(new BoxLayout(vRangePanel, BoxLayout.Y_AXIS));
        vRangePanel.setBorder(BorderFactory.createTitledBorder("V Range Coefficients (701-703, 801-803)"));

        String[] labels = new String[6];
        for (int i = 0; i < 6; i++) {
            if (i < 3) {
                labels[i] = "Коэффициент " + (i + 701) + ":";
            } else {
                labels[i] = "Коэффициент " + (i + 798) + ":";
            }
        }

        vRangeFields = new JTextField[6];
        JPanel gridPanel = createCoefficientGrid(labels, vRangeFields, false);

        vRangePanel.add(gridPanel);
    }

    // Метод для установки коэффициентов через cradleController
    private void setCoefficientsForGas(CHANNELS channel, JTextField[] fields) {
        checkDeviceState(selectedDevice);
        float[] coefficients = new float[fields.length];
        try {
            for (int i = 0; i < fields.length; i++) {
                String forParse = fields[i].getText().replaceAll("\\,", ".");
                coefficients[i] = Float.parseFloat(forParse);
            }
        }catch (NumberFormatException | NullPointerException ex){
            throw new IllegalArgumentException("Неверно заполнены поля с коэффициентами!" + ex.getMessage());
        }
        if(channel == CHANNELS.O2){
            DeviceCommand command = new SetO2Polys();
            CommandParameters parameters = new CommandParameters();
            parameters.setCoefficients(coefficients);
            asyncExecutor.executeCommand(command, parameters, selectedDevice);
        } else if (channel == CHANNELS.CO) {
            DeviceCommand command = new SetCOCoefs();
            CommandParameters parameters = new CommandParameters();
            parameters.setCoefficients(coefficients);
            asyncExecutor.executeCommand(command, parameters, selectedDevice);
        } else if(channel == CHANNELS.H2S){
            DeviceCommand command = new SetH2SCoefs();
            CommandParameters parameters = new CommandParameters();
            parameters.setCoefficients(coefficients);
            asyncExecutor.executeCommand(command, parameters, selectedDevice);
        }
    }

    private void checkDeviceState(HidDevice device) {
        if (device == null) {
            log.warn("device == null");
            throw new IllegalStateException("device == null");
        }
        if (asyncExecutor.isDeviceBusy(selectedDevice)) {
            throw new IllegalStateException("Busy");
        }
    }

    @Override
    public void updateData(DeviceState state) {
        log.info("Обновляю данные для коэффициентов...");
        if(state == null){
            log.warn("null объект state");
        }else if(state.getAllCoefficients() == null){
            log.warn("null объект getAllCoefficients внутри state");
        }

        if (state != null && state.getAllCoefficients() != null) {
            GetAllCoefficientsModel coef = state.getAllCoefficients();

            // Обновляем поля O2 коэффициентов
            for (int i = 0; i < 19; i++) {
                o2Fields[i].setText(String.valueOf((float) coef.getO2Coef()[i]));
            }

            // Обновляем поля CO коэффициентов
            for (int i = 0; i < 14; i++) {
                coFields[i].setText(String.valueOf((float) coef.getCoCoef()[i]));
            }

            // Обновляем поля H2S коэффициентов
            for (int i = 0; i < 14; i++) {
                h2sFields[i].setText(String.valueOf((float) coef.getH2sCoef()[i]));
            }

            // Обновляем поля CH4 Pressure коэффициентов
            for (int i = 0; i < 7; i++) {
                ch4PressureFields[i].setText(String.valueOf((float) coef.getCh4Pressure()[i]));
            }

            // Обновляем поля Acceleration коэффициентов
            for (int i = 0; i < 4; i++) {
                accelerationFields[i].setText(decimalFormat.format(coef.getAcceleration()[i]));
            }

            // Обновляем поля PPM Mg коэффициентов
            for (int i = 0; i < 4; i++) {
                ppmMgKoefsFields[i].setText(decimalFormat.format(coef.getPpmMgKoefs()[i]));
            }

            // Обновляем поля V Range коэффициентов
            for (int i = 0; i < 6; i++) {
                vRangeFields[i].setText(decimalFormat.format(coef.getVRange()[i]));
            }
        } else {
            // Очищаем все поля, если данных нет
            log.warn("Передано пустое состояние");
            clearAllFields();
        }
    }

    private void clearAllFields() {
        JTextField[][] allFields = {
                o2Fields, coFields, h2sFields, ch4PressureFields,
                accelerationFields, ppmMgKoefsFields, vRangeFields
        };

        for (JTextField[] fields : allFields) {
            if (fields != null) {
                for (JTextField field : fields) {
                    if (field != null) {
                        field.setText("");
                    }
                }
            }
        }
    }

    @Override
    public void saveData(DeviceState state) {
        // Для коэффициентов обычно не требуется сохранение через UI
        // Но если потребуется, можно реализовать здесь
    }
}