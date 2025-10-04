package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetAllCoefficientsModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.transport.CradleController;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class TabCoefficients extends DeviceTab {
    private Logger log = Logger.getLogger(TabCoefficients.class);
    private CoefficientNames coefficientNamesNames = new CoefficientNames();
    @Setter
    private CradleController cradleController;
    @Setter
    private HidDevice selectedDevice;
    @Setter
    private DeviceState deviceState;

    private JPanel mainPanel;
    private JScrollPane scrollPane;

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
    private DecimalFormat decimalFormat = new DecimalFormat("0.##########");

    public TabCoefficients(CradleController cradleController, HidDevice selectedDevice, DeviceState deviceState) {
        super("Коэффициенты");
        this.selectedDevice = selectedDevice;
        this.cradleController = cradleController;
        this.deviceState = deviceState;
        initComponents();
    }

    private void initComponents() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Создаем панели для каждого типа коэффициентов
        createO2Panel();
        createCoPanel();
        createH2sPanel();
        createCh4PressurePanel();
        createAccelerationPanel();
        createPpmMgKoefsPanel();
        createVRangePanel();

        // Добавляем все панели на главную панель
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

    private void createO2Panel() {
        o2Panel = new JPanel();
        o2Panel.setLayout(new BoxLayout(o2Panel, BoxLayout.Y_AXIS));
        o2Panel.setBorder(BorderFactory.createTitledBorder("O2 Coefficients (101-119)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        o2Fields = new JTextField[19];

        for (int i = 0; i < 19; i++) {
            gridPanel.add(new JLabel(coefficientNamesNames.oxygen.get(i)));
            o2Fields[i] = new JTextField();
            o2Fields[i].setEditable(true); // Делаем поле редактируемым
            gridPanel.add(o2Fields[i]);
        }

        o2Panel.add(gridPanel);

        // Добавляем кнопку "Задать" для O2
        JButton setO2Button = new JButton("Задать коэффициенты O2");
        setO2Button.addActionListener(e -> setCoefficientsForGas("o2", o2Fields));
        o2Panel.add(setO2Button);
    }

    private void createCoPanel() {
        coPanel = new JPanel();
        coPanel.setLayout(new BoxLayout(coPanel, BoxLayout.Y_AXIS));
        coPanel.setBorder(BorderFactory.createTitledBorder("CO Coefficients (201-214)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        coFields = new JTextField[14];

        for (int i = 0; i < 14; i++) {
            gridPanel.add(new JLabel(coefficientNamesNames.coDt.get(i)));
            coFields[i] = new JTextField();
            coFields[i].setEditable(true); // Делаем поле редактируемым
            gridPanel.add(coFields[i]);
        }

        coPanel.add(gridPanel);

        // Добавляем кнопку "Задать" для CO
        JButton setCoButton = new JButton("Задать коэффициенты CO");
        setCoButton.addActionListener(e -> setCoefficientsForGas("co", coFields));
        coPanel.add(setCoButton);
    }

    private void createH2sPanel() {
        h2sPanel = new JPanel();
        h2sPanel.setLayout(new BoxLayout(h2sPanel, BoxLayout.Y_AXIS));
        h2sPanel.setBorder(BorderFactory.createTitledBorder("H2S Coefficients (301-314)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        h2sFields = new JTextField[14];

        for (int i = 0; i < 14; i++) {
            gridPanel.add(new JLabel(coefficientNamesNames.h2s.get(i)));
            h2sFields[i] = new JTextField();
            h2sFields[i].setEditable(true); // Делаем поле редактируемым
            gridPanel.add(h2sFields[i]);
        }

        h2sPanel.add(gridPanel);

        // Добавляем кнопку "Задать" для H2S
        JButton setH2sButton = new JButton("Задать коэффициенты H2S");
        setH2sButton.addActionListener(e -> setCoefficientsForGas("h2s", h2sFields));
        h2sPanel.add(setH2sButton);
    }

    private void createCh4PressurePanel() {
        ch4PressurePanel = new JPanel();
        ch4PressurePanel.setLayout(new BoxLayout(ch4PressurePanel, BoxLayout.Y_AXIS));
        ch4PressurePanel.setBorder(BorderFactory.createTitledBorder("CH4 Pressure Coefficients (401-407)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        ch4PressureFields = new JTextField[7];

        for (int i = 0; i < 7; i++) {
            gridPanel.add(new JLabel("Коэффициент " + (i + 401) + ":"));
            ch4PressureFields[i] = new JTextField();
            ch4PressureFields[i].setEditable(false);
            gridPanel.add(ch4PressureFields[i]);
        }

        ch4PressurePanel.add(gridPanel);
    }

    private void createAccelerationPanel() {
        accelerationPanel = new JPanel();
        accelerationPanel.setLayout(new BoxLayout(accelerationPanel, BoxLayout.Y_AXIS));
        accelerationPanel.setBorder(BorderFactory.createTitledBorder("Acceleration Coefficients (501-504)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        accelerationFields = new JTextField[4];

        for (int i = 0; i < 4; i++) {
            gridPanel.add(new JLabel("Коэффициент " + (i + 501) + ":"));
            accelerationFields[i] = new JTextField();
            accelerationFields[i].setEditable(false);
            gridPanel.add(accelerationFields[i]);
        }

        accelerationPanel.add(gridPanel);
    }

    private void createPpmMgKoefsPanel() {
        ppmMgKoefsPanel = new JPanel();
        ppmMgKoefsPanel.setLayout(new BoxLayout(ppmMgKoefsPanel, BoxLayout.Y_AXIS));
        ppmMgKoefsPanel.setBorder(BorderFactory.createTitledBorder("PPM Mg Coefficients (601-604)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        ppmMgKoefsFields = new JTextField[4];

        for (int i = 0; i < 4; i++) {
            gridPanel.add(new JLabel("Коэффициент " + (i + 601) + ":"));
            ppmMgKoefsFields[i] = new JTextField();
            ppmMgKoefsFields[i].setEditable(false);
            gridPanel.add(ppmMgKoefsFields[i]);
        }

        ppmMgKoefsPanel.add(gridPanel);
    }

    private void createVRangePanel() {
        vRangePanel = new JPanel();
        vRangePanel.setLayout(new BoxLayout(vRangePanel, BoxLayout.Y_AXIS));
        vRangePanel.setBorder(BorderFactory.createTitledBorder("V Range Coefficients (701-703, 801-803)"));

        JPanel gridPanel = new JPanel(new GridLayout(0, 2, 5, 5));
        vRangeFields = new JTextField[6];

        for (int i = 0; i < 6; i++) {
            String coefName = (i < 3) ? "Коэффициент " + (i + 701) : "Коэффициент " + (i + 798);
            gridPanel.add(new JLabel(coefName + ":"));
            vRangeFields[i] = new JTextField();
            vRangeFields[i].setEditable(false);
            gridPanel.add(vRangeFields[i]);
        }

        vRangePanel.add(gridPanel);
    }

    // Метод для установки коэффициентов через cradleController
    private void setCoefficientsForGas(String gasType, JTextField[] fields) {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(null, "Устройство не выбрано", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            double[] coefficients = new double[fields.length];
            for (int i = 0; i < fields.length; i++) {
                String forParse = fields[i].getText().replaceAll("\\,", ".");
                coefficients[i] = Double.parseDouble(forParse);
            }

            cradleController.setCoefForGas(gasType, coefficients, selectedDevice);
            JOptionPane.showMessageDialog(null, "Коэффициенты " + gasType.toUpperCase() + " успешно заданы",
                    "Успех", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException ex) {
            log.info("Некорректные числовые значения в полях коэффициентов" + ex.getMessage());
            JOptionPane.showMessageDialog(null, "Некорректные числовые значения в полях коэффициентов",
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "Ошибка при задании коэффициентов: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            log.error("Ошибка при задании коэффициентов", ex);
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
                o2Fields[i].setText(decimalFormat.format(coef.getO2Coef()[i]));
            }

            // Обновляем поля CO коэффициентов
            for (int i = 0; i < 14; i++) {
                coFields[i].setText(decimalFormat.format(coef.getCoCoef()[i]));
            }

            // Обновляем поля H2S коэффициентов
            for (int i = 0; i < 14; i++) {
                h2sFields[i].setText(decimalFormat.format(coef.getH2sCoef()[i]));
            }

            // Обновляем поля CH4 Pressure коэффициентов
            for (int i = 0; i < 7; i++) {
                ch4PressureFields[i].setText(decimalFormat.format(coef.getCh4Pressure()[i]));
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