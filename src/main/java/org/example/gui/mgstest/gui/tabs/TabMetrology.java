package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.gui.names.GasLists;
import org.example.gui.mgstest.model.ElementOfGasesListGui;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.model.answer.GetAlarmsModel;
import org.example.gui.mgstest.model.answer.GetGasRangeModel;
import org.example.gui.mgstest.model.answer.GetSensStatusModel;
import org.example.gui.mgstest.model.answer.GetVRangeModel;

import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.cmd.mgs.metrology.*;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class TabMetrology extends DeviceTab {
    private final Logger log = Logger.getLogger(TabMetrology.class);

    @Setter
    private HidSupportedDevice selectedDevice;

    private final DeviceAsyncExecutor asyncExecutor;

    GasLists gasLists = new GasLists();
    // Компоненты для VRange
    private final JTextField vrangeO2Fom = new JTextField(10);
    private final JTextField vrangeO2To = new JTextField(10);
    private final JTextField vrangeCOFrom = new JTextField(10);
    private final JTextField vrangeCOTo = new JTextField(10);
    private final JTextField vrangeH2SFrom = new JTextField(10);
    private final JTextField vrangeH2STo = new JTextField(10);

    // Компоненты для AlarmLimits
    private final JTextField alarmO2Field1 = new JTextField(10);
    private final JTextField alarmO2Field2 = new JTextField(10);
    private final JTextField alarmCOField1 = new JTextField(10);
    private final JTextField alarmCOField2 = new JTextField(10);
    private final JTextField alarmH2SField1 = new JTextField(10);
    private final JTextField alarmH2SField2 = new JTextField(10);
    private final JTextField alarmCH4Field1 = new JTextField(10);
    private final JTextField alarmCH4Field2 = new JTextField(10);

    // Компоненты для GasRange
    private final JTextField gasCH4Field1 = new JTextField(10);
    private final JTextField gasCH4Field2 = new JTextField(10);
    private final JTextField gasCOField1 = new JTextField(10);
    private final JTextField gasCOField2 = new JTextField(10);
    private final JTextField gasH2SField1 = new JTextField(10);
    private final JTextField gasH2SField2 = new JTextField(10);
    private final JTextField gasO2Field1 = new JTextField(10);
    private final JTextField gasO2Field2 = new JTextField(10);

    // Компоненты для SensStatus
    private final JCheckBox sensCH4CheckBox = new JCheckBox();
    private final JTextField sensCH4Field = new JTextField(10);
    private final JComboBox<ElementOfGasesListGui> sensCH4List = new JComboBox();
    private final JCheckBox sensCOCheckBox = new JCheckBox();
    private final JTextField sensCOField = new JTextField(10);
    private final JComboBox<ElementOfGasesListGui> sensCOList = new JComboBox();
    private final JCheckBox sensH2SCheckBox = new JCheckBox();
    private final JTextField sensH2SField = new JTextField(10);
    private final JComboBox<ElementOfGasesListGui> sensH2SList = new JComboBox();
    private final JCheckBox sensO2CheckBox = new JCheckBox();
    private final JTextField sensO2Field = new JTextField(10);
    private final JComboBox<ElementOfGasesListGui> sensO2List = new JComboBox();

    private JScrollPane scrollPane;

    public TabMetrology(HidSupportedDevice selectedDevice, DeviceAsyncExecutor asyncExecutor) {
        super("Метрология");
        this.selectedDevice = selectedDevice;
        this.asyncExecutor = asyncExecutor;
        initComponents();
    }

    private void initComponents() {
        // Создаем основную панель с вертикальным расположением
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Раздел: VRange
        mainPanel.add(createVRangePanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: AlarmLimits
        mainPanel.add(createAlarmLimitsPanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: GasRange
        mainPanel.add(createGasRangePanel());
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: SensStatus
        mainPanel.add(createSensStatusPanel());

        // Создаем прокручиваемую панель
        scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(600, 400)); // Установите нужный размер

        // Устанавливаем layout для основной панели таба и добавляем прокручиваемую область
        panel.setLayout(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);


        HashMap<Byte, String> O2 = gasLists.O2;
        for (Map.Entry<Byte, String> stringIntegerEntry : O2.entrySet()) {
            ElementOfGasesListGui elementOfGasesListGui = new ElementOfGasesListGui(stringIntegerEntry.getValue(), stringIntegerEntry.getKey());
            sensO2List.addItem(elementOfGasesListGui);
        }

        HashMap<Byte, String> H2S = gasLists.H2S;
        for (Map.Entry<Byte, String> stringIntegerEntry : H2S.entrySet()) {
            ElementOfGasesListGui elementOfGasesListGui = new ElementOfGasesListGui(stringIntegerEntry.getValue(), stringIntegerEntry.getKey());
            sensH2SList.addItem(elementOfGasesListGui);
        }

        HashMap<Byte, String> CO = gasLists.CO;
        for (Map.Entry<Byte, String> stringIntegerEntry : CO.entrySet()) {
            ElementOfGasesListGui elementOfGasesListGui = new ElementOfGasesListGui(stringIntegerEntry.getValue(), stringIntegerEntry.getKey());
            sensCOList.addItem(elementOfGasesListGui);
        }

        HashMap<Byte, String> CH4 = gasLists.CH4;
        for (Map.Entry<Byte, String> stringIntegerEntry : CH4.entrySet()) {
            ElementOfGasesListGui elementOfGasesListGui = new ElementOfGasesListGui(stringIntegerEntry.getValue(), stringIntegerEntry.getKey());
            sensCH4List.addItem(elementOfGasesListGui);
        }
        clearFields();
    }

    private JPanel createVRangePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("VRange"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        int row = 0;
        addLabelAndTwoFields(panel, gbc, "O2:", vrangeO2Fom, vrangeO2To, row++, true);
        addLabelAndTwoFields(panel, gbc, "CO:", vrangeCOFrom, vrangeCOTo, row++, true);
        addLabelAndTwoFields(panel, gbc, "H2S:", vrangeH2SFrom, vrangeH2STo, row++, true);

        // Кнопки
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        JButton setButton = new JButton("задать");
        JButton getButton = new JButton("считать");
        buttonPanel.add(setButton);
        buttonPanel.add(getButton);
        panel.add(buttonPanel, gbc);

        // Обработчики
        getButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            GetVRange getVrange = new GetVRange();
            asyncExecutor.executeCommand(getVrange, null, selectedDevice);
        });

        setButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            CommandParameters parameters = new CommandParameters();
            int [] arg = new int[6];

            try {
                arg [0] = (Integer.parseInt(vrangeO2Fom.getText()));
                arg [1] = (Integer.parseInt(vrangeO2To.getText()));
                arg [2] = (Integer.parseInt(vrangeCOFrom.getText()));
                arg [3] = (Integer.parseInt(vrangeCOTo.getText()));
                arg [4] = (Integer.parseInt(vrangeH2SFrom.getText()));
                arg [5] = (Integer.parseInt(vrangeH2STo.getText()));
                parameters.setIntArguments(arg);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.getPanel(), "Неверный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SetVRange setVrange = new SetVRange();
            asyncExecutor.executeCommand(setVrange, parameters, selectedDevice);
        });

        return panel;
    }

    private JPanel createAlarmLimitsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("AlarmLimits"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        int row = 0;
        addLabelAndTwoFields(panel, gbc, "O2:", alarmO2Field1, alarmO2Field2, row++, true);
        addLabelAndTwoFields(panel, gbc, "CO:", alarmCOField1, alarmCOField2, row++, true);
        addLabelAndTwoFields(panel, gbc, "H2S:", alarmH2SField1, alarmH2SField2, row++, true);
        addLabelAndTwoFields(panel, gbc, "CH4:", alarmCH4Field1, alarmCH4Field2, row++, true);

        // Кнопки
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        JButton setButton = new JButton("задать");
        JButton getButton = new JButton("считать");
        buttonPanel.add(setButton);
        buttonPanel.add(getButton);
        panel.add(buttonPanel, gbc);

        // Обработчики
        getButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            GetAlarms getAlarmLimits = new GetAlarms();
            asyncExecutor.executeCommand(getAlarmLimits, null, selectedDevice);
        });

        setButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            CommandParameters parameters = new CommandParameters();
            short [] arg = new short[8];
            try {
                // Ручная настройка соотношений поле--gui
                arg [0] = (Short.parseShort(alarmCH4Field1.getText()));//from
                arg [4] = (Short.parseShort(alarmCH4Field2.getText()));//to
                arg [1] = (Short.parseShort(alarmO2Field1.getText()));//from
                arg [5] = (Short.parseShort(alarmO2Field2.getText()));//to
                arg [2] = (Short.parseShort(alarmCOField1.getText()));//from
                arg [6] = (Short.parseShort(alarmCOField2.getText()));//to
                arg [3] = (Short.parseShort(alarmH2SField1.getText()));//from
                arg [7] = (Short.parseShort(alarmH2SField2.getText()));//to
                parameters.setShortArguments(arg);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.getPanel(), "Неверный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SetAlarms setAlarmLimits = new SetAlarms();
            asyncExecutor.executeCommand(setAlarmLimits, parameters, selectedDevice);
        });

        return panel;
    }

    private JPanel createGasRangePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("GasRange"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        int row = 0;
        addLabelAndTwoFields(panel, gbc, "CH4:", gasCH4Field1, gasCH4Field2, row++, true);
        addLabelAndTwoFields(panel, gbc, "CO:", gasCOField1, gasCOField2, row++, true);
        addLabelAndTwoFields(panel, gbc, "H2S:", gasH2SField1, gasH2SField2, row++, true);
        addLabelAndTwoFields(panel, gbc, "O2:", gasO2Field1, gasO2Field2, row++, true);

        // Кнопки
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        JButton setButton = new JButton("задать");
        JButton getButton = new JButton("считать");
        buttonPanel.add(setButton);
        buttonPanel.add(getButton);
        panel.add(buttonPanel, gbc);

        // Обработчики
        getButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            GetGasRange getGasRange = new GetGasRange();
            asyncExecutor.executeCommand(getGasRange, null, selectedDevice);
        });

        setButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            CommandParameters parameters = new CommandParameters();
            int [] arg = new int[8];
            try {
                arg[0] = (Integer.parseInt(gasO2Field1.getText()));
                arg[1] = (Integer.parseInt(gasO2Field2.getText()));
                arg[2] = (Integer.parseInt(gasCOField1.getText()));
                arg[3] = (Integer.parseInt(gasCOField2.getText()));
                arg[4] = (Integer.parseInt(gasH2SField1.getText()));
                arg[5] = (Integer.parseInt(gasH2SField2.getText()));
                arg[6] = (Integer.parseInt(gasCH4Field1.getText()));
                arg[7] = (Integer.parseInt(gasCH4Field2.getText()));
                parameters.setIntArguments(arg);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.getPanel(), "Неверный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SetGasRange setGasRange = new SetGasRange();
            asyncExecutor.executeCommand(setGasRange, parameters, selectedDevice);
        });

        return panel;
    }

    private JPanel createSensStatusPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("SensStatus"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 10, 5, 10);

        int row = 0;
        addLabelCheckboxAndField(panel, gbc, "CH4:", sensCH4CheckBox, sensCH4Field, sensCH4List, row++);
        addLabelCheckboxAndField(panel, gbc, "CO:", sensCOCheckBox, sensCOField, sensCOList, row++);
        addLabelCheckboxAndField(panel, gbc, "H2S:", sensH2SCheckBox, sensH2SField, sensH2SList, row++);
        addLabelCheckboxAndField(panel, gbc, "O2:", sensO2CheckBox, sensO2Field,sensO2List, row++);

        // Кнопки
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        JButton setButton = new JButton("задать");
        JButton getButton = new JButton("считать");
        buttonPanel.add(setButton);
        buttonPanel.add(getButton);
        panel.add(buttonPanel, gbc);

        // Обработчики
        getButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            GetSensStatus getSensStatus = new GetSensStatus();
            asyncExecutor.executeCommand(getSensStatus, null, selectedDevice);
        });

        setButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            CommandParameters parameters = new CommandParameters();
            GetSensStatusModel model = new GetSensStatusModel();
            ElementOfGasesListGui tmpGasModel;
            try {
                if(sensO2CheckBox.isSelected()) {
                    tmpGasModel = (ElementOfGasesListGui) sensO2List.getSelectedItem();
                    model.setO2_num(tmpGasModel.getGasCode());//Ignore NPE
                }else{
                    model.setO2_num((byte) 0);
                }
                if(sensCOCheckBox.isSelected()) {
                    tmpGasModel = (ElementOfGasesListGui) sensCOList.getSelectedItem();
                    model.setCO_num(tmpGasModel.getGasCode());
                }else{
                    model.setCO_num((byte) 0);
                }
                if(sensH2SCheckBox.isSelected()) {
                    tmpGasModel = (ElementOfGasesListGui) sensH2SList.getSelectedItem();
                    model.setH2S_num(tmpGasModel.getGasCode());
                }else{
                    model.setH2S_num((byte) 0);
                }
                if(sensCH4CheckBox.isSelected()) {
                    tmpGasModel = (ElementOfGasesListGui) sensCH4List.getSelectedItem();
                    model.setCH4_num(tmpGasModel.getGasCode());
                }else{
                    model.setCH4_num((byte) 0);
                }
                parameters.setSensStatusModel(model);

            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.getPanel(), "Неверный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            SetSensStatus setSensStatus = new SetSensStatus();
            asyncExecutor.executeCommand(setSensStatus, parameters, selectedDevice);
        });

        return panel;
    }

    private void addLabelAndTwoFields(JPanel panel, GridBagConstraints gbc,
                                      String labelText, JTextField field1, JTextField field2,
                                      int row, boolean editable) {
        field1.setEditable(editable);
        //field1.setBackground(editable ? Color.LIGHT_GRAY : Color.gray);
        field2.setEditable(editable);
        //field2.setBackground(editable ? Color.LIGHT_GRAY : Color.gray);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.35;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(field1, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.35;
        panel.add(field2, gbc);
    }

    private void addLabelCheckboxAndField(JPanel panel, GridBagConstraints gbc,
                                          String labelText, JCheckBox checkBox, JTextField field, JComboBox jComboBox,
                                          int row) {
        field.setEditable(true);
        //field.setBackground(Color.LIGHT_GRAY);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.35;
        gbc.anchor = GridBagConstraints.CENTER;
        panel.add(checkBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.35;
        panel.add(field, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.35;
        panel.add(jComboBox, gbc);
    }

    private void checkDeviceState(HidSupportedDevice device) throws IllegalStateException{
        if (device == null) {
            log.warn("device == null");
            asyncExecutor.notifyAboutErrorForDev(device, "device == null");
            throw new IllegalStateException("device == null");
        }
        if (asyncExecutor.isDeviceBusy(selectedDevice)) {
            asyncExecutor.notifyAboutErrorForDev(device, "Busy");
            throw new IllegalStateException("Busy");
        }
    }

    @Override
    public void updateData(DeviceState state) {
        log.info("Updating metrology tab");
        if(state == null){
            log.info("  state == null");
            return;
        }

        //getGasRangeModel
        if(state.getGasRangeModel() != null){
            if(state.getGasRangeModel().isLoaded()){
                GetGasRangeModel model = state.getGasRangeModel();
                gasO2Field1.setText(String.valueOf(model.getO2From()));
                gasO2Field2.setText(String.valueOf(model.getO2To()));
                gasCOField1.setText(String.valueOf(model.getCoFrom()));
                gasCOField2.setText(String.valueOf(model.getCoTo()));
                gasH2SField1.setText(String.valueOf(model.getH2sFrom()));
                gasH2SField2.setText(String.valueOf(model.getH2sTo()));
                gasCH4Field1.setText(String.valueOf(model.getCh4From()));
                gasCH4Field2.setText(String.valueOf(model.getCh4To()));
                log.info("Updated gasRange");
            }else{
                log.info("Flag isLoaded is false");
            }
        }else{
            log.info("state.gasRange() == null");
        }

        //getAlarmsModel
        if(state.getAlarmsModel() != null){
            if(state.getAlarmsModel().isLoaded()){
                GetAlarmsModel model = state.getAlarmsModel();
                alarmO2Field1.setText(String.valueOf(model.getO2From()));
                alarmO2Field2.setText(String.valueOf(model.getO2To()));
                alarmCOField1.setText(String.valueOf(model.getCoFrom()));
                alarmCOField2.setText(String.valueOf(model.getCoTo()));
                alarmH2SField1.setText(String.valueOf(model.getH2sFrom()));
                alarmH2SField2.setText(String.valueOf(model.getH2sTo()));
                alarmCH4Field1.setText(String.valueOf(model.getCh4From()));
                alarmCH4Field2.setText(String.valueOf(model.getCh4To()));
                log.info("Updated getAlarmsModel");
            }else{
                log.info("Flag isLoaded is false");
            }
        }else{
            log.info("state.getAlarmsModel() == null");
        }

        //getVRangeModel
        if(state.getVRangeModel() != null){
            if(state.getVRangeModel().isLoaded()){
                GetVRangeModel model = state.getVRangeModel();
                vrangeO2Fom.setText(String.valueOf(model.getO2From()));
                vrangeO2To.setText(String.valueOf(model.getO2To()));
                vrangeCOFrom.setText(String.valueOf(model.getCoFrom()));
                vrangeCOTo.setText(String.valueOf(model.getCoTo()));
                vrangeH2SFrom.setText(String.valueOf(model.getH2sFrom()));
                vrangeH2STo.setText(String.valueOf(model.getH2sTo()));
                log.info("Updated getVrangeModel");
            }else{
                log.info("Flag isLoaded is false");
            }
        }else{
            log.info("state.getVrangeModel() == null");
        }

        //GetSensStatusModel
        if(state.getSensStatusModel() != null){
            if(state.getSensStatusModel().isLoaded()){
                GetSensStatusModel model = state.getSensStatusModel();
                sensCH4CheckBox.setSelected(model.isCH4());
                sensCH4Field.setText(String.valueOf(model.getCH4_num()));
                ElementOfGasesListGui gasModel = new ElementOfGasesListGui(gasLists.CH4.get(model.getCH4_num()), model.getCH4_num());
                sensCH4List.setSelectedItem(gasModel);

                sensCOCheckBox.setSelected(model.isCO());
                sensCOField.setText(String.valueOf(model.getCO_num()));
                gasModel = new ElementOfGasesListGui(gasLists.CO.get(model.getCO_num()), model.getCO_num());
                sensCOList.setSelectedItem(gasModel);

                sensH2SCheckBox.setSelected(model.isH2S());
                sensH2SField.setText(String.valueOf(model.getH2S_num()));
                gasModel = new ElementOfGasesListGui(gasLists.H2S.get(model.getH2S_num()), model.getH2S_num());
                sensH2SList.setSelectedItem(gasModel);

                sensO2CheckBox.setSelected(model.isO2());
                sensO2Field.setText(String.valueOf(model.getO2_num()));
                gasModel = new ElementOfGasesListGui(gasLists.O2.get(model.getO2_num()), model.getO2_num());
                sensO2List.setSelectedItem(gasModel);

                log.info("Updated getSensStatusModel");
            }else{
                log.info("Flag isLoaded is false");
            }
        }else{
            log.info("state.getSensStatusModel() == null");
        }
    }

    @Override
    public void saveData(DeviceState state) {
        // TODO: Implement save logic if necessary
    }

    private void clearFields() {
        vrangeO2Fom.setText("");
        vrangeO2To.setText("");
        vrangeCOFrom.setText("");
        vrangeCOTo.setText("");
        vrangeH2SFrom.setText("");
        vrangeH2STo.setText("");

        alarmO2Field1.setText("");
        alarmO2Field2.setText("");
        alarmCOField1.setText("");
        alarmCOField2.setText("");
        alarmH2SField1.setText("");
        alarmH2SField2.setText("");
        alarmCH4Field1.setText("");
        alarmCH4Field2.setText("");

        gasCH4Field1.setText("");
        gasCH4Field2.setText("");
        gasCOField1.setText("");
        gasCOField2.setText("");
        gasH2SField1.setText("");
        gasH2SField2.setText("");
        gasO2Field1.setText("");
        gasO2Field2.setText("");

        sensCH4CheckBox.setSelected(false);
        sensCH4Field.setText("");
        sensCOCheckBox.setSelected(false);
        sensCOField.setText("");
        sensH2SCheckBox.setSelected(false);
        sensH2SField.setText("");
        sensO2CheckBox.setSelected(false);
        sensO2Field.setText("");
    }
}