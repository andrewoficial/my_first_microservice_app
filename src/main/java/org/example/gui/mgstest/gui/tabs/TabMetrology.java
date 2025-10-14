package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetAlarmsModel;
import org.example.gui.mgstest.model.answer.GetVRangeModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.cmd.metrology.GetAlarms;
import org.example.gui.mgstest.transport.cmd.metrology.GetVRange;
import org.example.gui.mgstest.transport.cmd.metrology.SetAlarms;
import org.example.gui.mgstest.transport.cmd.metrology.SetVRange;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;

public class TabMetrology extends DeviceTab {
    private Logger log = Logger.getLogger(TabMetrology.class);

    @Setter
    private HidDevice selectedDevice;

    private final DeviceAsyncExecutor asyncExecutor;

    // Компоненты для VRange
    private JTextField vrangeO2Fom = new JTextField(10);
    private JTextField vrangeO2To = new JTextField(10);
    private JTextField vrangeCOFrom = new JTextField(10);
    private JTextField vrangeCOTo = new JTextField(10);
    private JTextField vrangeH2SFrom = new JTextField(10);
    private JTextField vrangeH2STo = new JTextField(10);

    // Компоненты для AlarmLimits
    private JTextField alarmO2Field1 = new JTextField(10);
    private JTextField alarmO2Field2 = new JTextField(10);
    private JTextField alarmCOField1 = new JTextField(10);
    private JTextField alarmCOField2 = new JTextField(10);
    private JTextField alarmH2SField1 = new JTextField(10);
    private JTextField alarmH2SField2 = new JTextField(10);
    private JTextField alarmCH4Field1 = new JTextField(10);
    private JTextField alarmCH4Field2 = new JTextField(10);

    // Компоненты для GasRange
    private JTextField gasCH4Field1 = new JTextField(10);
    private JTextField gasCH4Field2 = new JTextField(10);
    private JTextField gasCOField1 = new JTextField(10);
    private JTextField gasCOField2 = new JTextField(10);
    private JTextField gasH2SField1 = new JTextField(10);
    private JTextField gasH2SField2 = new JTextField(10);
    private JTextField gasO2Field1 = new JTextField(10);
    private JTextField gasO2Field2 = new JTextField(10);

    // Компоненты для SensStatus
    private JCheckBox sensCH4CheckBox = new JCheckBox();
    private JTextField sensCH4Field = new JTextField(10);
    private JCheckBox sensCOCheckBox = new JCheckBox();
    private JTextField sensCOField = new JTextField(10);
    private JCheckBox sensH2SCheckBox = new JCheckBox();
    private JTextField sensH2SField = new JTextField(10);
    private JCheckBox sensO2CheckBox = new JCheckBox();
    private JTextField sensO2Field = new JTextField(10);

    public TabMetrology(HidDevice selectedDevice, DeviceAsyncExecutor asyncExecutor) {
        super("Метрология");
        this.selectedDevice = selectedDevice;
        this.asyncExecutor = asyncExecutor;
        initComponents();
    }

    private void initComponents() {
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Раздел: VRange
        panel.add(createVRangePanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: AlarmLimits
        panel.add(createAlarmLimitsPanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: GasRange
        panel.add(createGasRangePanel());
        panel.add(Box.createRigidArea(new Dimension(0, 10)));

        // Раздел: SensStatus
        panel.add(createSensStatusPanel());

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
//            GetGasRange getGasRange = new GetGasRange();
//            asyncExecutor.executeCommand(getGasRange, null, selectedDevice);
        });

        setButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            CommandParameters parameters = new CommandParameters();
            try {
//                parameters.addInt(Integer.parseInt(gasCH4Field1.getText()));
//                parameters.addInt(Integer.parseInt(gasCH4Field2.getText()));
//                parameters.addInt(Integer.parseInt(gasCOField1.getText()));
//                parameters.addInt(Integer.parseInt(gasCOField2.getText()));
//                parameters.addInt(Integer.parseInt(gasH2SField1.getText()));
//                parameters.addInt(Integer.parseInt(gasH2SField2.getText()));
//                parameters.addInt(Integer.parseInt(gasO2Field1.getText()));
//                parameters.addInt(Integer.parseInt(gasO2Field2.getText()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.getPanel(), "Неверный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
//            SetGasRange setGasRange = new SetGasRange();
//            asyncExecutor.executeCommand(setGasRange, parameters, selectedDevice);
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
        addLabelCheckboxAndField(panel, gbc, "CH4:", sensCH4CheckBox, sensCH4Field, row++);
        addLabelCheckboxAndField(panel, gbc, "CO:", sensCOCheckBox, sensCOField, row++);
        addLabelCheckboxAndField(panel, gbc, "H2S:", sensH2SCheckBox, sensH2SField, row++);
        addLabelCheckboxAndField(panel, gbc, "O2:", sensO2CheckBox, sensO2Field, row++);

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
//            GetSensStatus getSensStatus = new GetSensStatus();
//            asyncExecutor.executeCommand(getSensStatus, null, selectedDevice);
        });

        setButton.addActionListener(e -> {
            checkDeviceState(selectedDevice);
            CommandParameters parameters = new CommandParameters();
            try {
//                parameters.addInt(sensCH4CheckBox.isSelected() ? 1 : 0);
//                parameters.addInt(Integer.parseInt(sensCH4Field.getText()));
//                parameters.addInt(sensCOCheckBox.isSelected() ? 1 : 0);
//                parameters.addInt(Integer.parseInt(sensCOField.getText()));
//                parameters.addInt(sensH2SCheckBox.isSelected() ? 1 : 0);
//                parameters.addInt(Integer.parseInt(sensH2SField.getText()));
//                parameters.addInt(sensO2CheckBox.isSelected() ? 1 : 0);
//                parameters.addInt(Integer.parseInt(sensO2Field.getText()));
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this.getPanel(), "Неверный формат ввода", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }
//            SetSensStatus setSensStatus = new SetSensStatus();
//            asyncExecutor.executeCommand(setSensStatus, parameters, selectedDevice);
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
                                          String labelText, JCheckBox checkBox, JTextField field,
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
        log.info("Updating metrology tab");
        if(state == null){
            log.info("  state == null");
            return;
        }

        if(state.getVRangeModel() != null){
            if(state.getVRangeModel().isLoaded()){
                GetVRangeModel model = state.getVRangeModel();
                vrangeO2Fom.setText(String.valueOf(model.getO2From()));
                vrangeO2To.setText(String.valueOf(model.getO2To()));
                vrangeCOFrom.setText(String.valueOf(model.getCoFrom()));
                vrangeCOTo.setText(String.valueOf(model.getCoTo()));
                vrangeH2SFrom.setText(String.valueOf(model.getH2sFrom()));
                vrangeH2STo.setText(String.valueOf(model.getH2sTo()));
                log.info("Updated vRange");
            }else{
                log.info("Flag isLoaded is false");
            }
        }else{
            log.info("state.getVRangeModel() == null");
        }

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