package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.gui.names.SettingNames;
import org.example.gui.mgstest.model.answer.GetAllSettingsModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.cmd.GetAllSettings;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class TabSettings extends DeviceTab {
    private Logger log = Logger.getLogger(TabSettings.class);
    private SettingNames settingNames = new SettingNames();
    @Setter
    private HidDevice selectedDevice;

    private final DeviceAsyncExecutor asyncExecutor;

    private JPanel mainPanel;
    private JScrollPane scrollPane;
    private JButton getSettingsButton;

    // Массив полей для настроек
    private JTextField[] settingFields;

    public TabSettings(HidDevice selectedDevice, DeviceAsyncExecutor asyncExecutor) {
        super("Настройки");
        this.selectedDevice = selectedDevice;
        this.asyncExecutor = asyncExecutor;
        initComponents();
    }

    private void initComponents() {
        // Основная панель использует BorderLayout
        panel.setLayout(new BorderLayout());

        // Создаем панель для кнопки (будет вверху)
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());
        createGetSettingsButton();
        buttonPanel.add(getSettingsButton);

        // Создаем основную панель для настроек (будет прокручиваться)
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // Создаем панель с настройками
        createSettingsPanel();

        // Добавляем прокрутку только для панели с настройками
        scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        // Добавляем компоненты на основную панель
        panel.add(buttonPanel, BorderLayout.NORTH);  // Кнопка вверху
        panel.add(scrollPane, BorderLayout.CENTER);  // Настройки в центре с прокруткой
    }

    private void createGetSettingsButton() {
        getSettingsButton = new JButton("Получить параметры");
        getSettingsButton.addActionListener(e -> getSettings());
    }

    private void getSettings() {
        if (selectedDevice != null) {
            DeviceCommand command = new GetAllSettings();
            asyncExecutor.executeCommand(command, null, selectedDevice);
        } else {
            JOptionPane.showMessageDialog(panel, "Устройство не выбрано", "Ошибка", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void createSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new BoxLayout(settingsPanel, BoxLayout.Y_AXIS));
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Параметры устройства"));

        // Создаем сетку для настроек (3 столбца: метка, поле, единица измерения если есть)
        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 5, 5));
        settingFields = new JTextField[settingNames.getSettingNames().size()];

        ArrayList<String> names = settingNames.getSettingNames();
        for (int i = 0; i < names.size(); i++) {
            gridPanel.add(new JLabel(names.get(i) + ":"));
            settingFields[i] = new JTextField();
            settingFields[i].setEditable(false); // Пока только для чтения
            gridPanel.add(settingFields[i]);
            
            // Добавляем третью колонку для единиц измерения (если нужно)
            String unit = getUnitForSetting(names.get(i));
            gridPanel.add(new JLabel(unit));
        }

        settingsPanel.add(gridPanel);
        mainPanel.add(settingsPanel);
    }

    private String getUnitForSetting(String settingName) {
        // Определяем единицы измерения для конкретных настроек
        if (settingName.contains("Time") || settingName.contains("TimeOut") || 
            settingName.contains("Seconds") || settingName.contains("Minutes")) {
            return "сек";
        } else if (settingName.contains("PWM")) {
            return "PWM";
        } else if (settingName.contains("BattLow") || settingName.contains("RssiLow") || 
                   settingName.contains("SnrLow")) {
            return "у.е.";
        } else if (settingName.contains("FreezeLimit") || settingName.contains("Coef")) {
            return "коэф.";
        } else if (settingName.contains("Week")) {
            return "нед.";
        } else if (settingName.contains("scalepoint")) {
            return "точка";
        } else {
            return "";
        }
    }

    @Override
    public void updateData(DeviceState state) {
        log.info("Обновляю данные для настроек...");

        if (state == null) {
            log.warn("null объект state");
            clearAllFields();
            return;
        }

        if (state.getAllSettings() != null && state.getAllSettings().isLoaded()) {
            GetAllSettingsModel settings = state.getAllSettings();

            // Обновляем поля на основе данных из settings
            settingFields[0].setText(String.valueOf(settings.getLedAlarmPWM()));
            settingFields[1].setText(String.valueOf(settings.getVibroPWM()));
            settingFields[2].setText(String.valueOf(settings.getLedAlarmTime()));
            settingFields[3].setText(String.valueOf(settings.getLedAlarmSlowTime()));
            settingFields[4].setText(String.valueOf(settings.getAlarmBeepOffTimeOut()));
            settingFields[5].setText(String.valueOf(settings.getAlarmTimeOut()));
            settingFields[6].setText(String.valueOf(settings.getLedRedSCRPWM()));
            settingFields[7].setText(String.valueOf(settings.getFreezeDeltaTemper()));
            settingFields[8].setText(String.valueOf(settings.getFreezeDeltaTime()));
            settingFields[9].setText(String.valueOf(settings.getVrefWarmUpTime()));
            settingFields[10].setText(String.valueOf(settings.getBattLow()));
            settingFields[11].setText(String.valueOf(settings.getLogState()));
            settingFields[12].setText(String.valueOf(settings.getPressureState()));
            settingFields[13].setText(String.valueOf(settings.getLifeTimeWeek()));
            settingFields[14].setText(String.valueOf(settings.getFreezeStatusMask()));
            settingFields[15].setText(String.valueOf(settings.getFreezeLimit()));
            settingFields[16].setText(String.valueOf(settings.getCoefVolToLEL()));
            settingFields[17].setText(String.valueOf(settings.getLogTimeOut()));
            settingFields[18].setText(String.valueOf(settings.getLogAlarmTimeOut()));
            settingFields[19].setText(String.valueOf(settings.getCh4BufferTerm()));
            settingFields[20].setText(String.valueOf(settings.getCh4BufferTime()));
            settingFields[21].setText(String.valueOf(settings.getCoefH2SppmToMg()));
            settingFields[22].setText(String.valueOf(settings.getCoefCOppmToMg()));
            settingFields[23].setText(String.valueOf(settings.getCoefCHEMppmToMg()));
            settingFields[24].setText(String.valueOf(settings.getNfcTimeOutDetectSeconds()));
            settingFields[25].setText(String.valueOf(settings.getNfcTimeOutWaitMinutes()));
            settingFields[26].setText(String.valueOf(settings.getSensorsUnits()));
            settingFields[27].setText(String.valueOf(settings.getO2Chem()));
            settingFields[28].setText(String.valueOf(settings.getSensorsPrecisions()));
            settingFields[29].setText(String.valueOf(settings.getSkipSelfTest()));
            settingFields[30].setText(String.valueOf(settings.getSensorsAutoZero()));
            settingFields[31].setText(String.valueOf(settings.getAltScreenTime()));
            settingFields[32].setText(String.valueOf(settings.getRssiLow()));
            settingFields[33].setText(String.valueOf(settings.getSnrLow()));
            settingFields[34].setText(String.valueOf(settings.getAlarmType()));
            settingFields[35].setText(String.valueOf(settings.getLostSec()));
            settingFields[36].setText(String.valueOf(settings.getLostPackets()));
            settingFields[37].setText(String.valueOf(settings.getO2Sim()));
            settingFields[38].setText(String.valueOf(settings.getCoSim()));
            settingFields[39].setText(String.valueOf(settings.getH2sSim()));
            settingFields[40].setText(String.valueOf(settings.getCh4Sim()));
            settingFields[41].setText(String.valueOf(settings.getScreenPosition()));
            settingFields[42].setText(String.valueOf(settings.getO2Scalepoint()));
            settingFields[43].setText(String.valueOf(settings.getCoScalepoint()));
            settingFields[44].setText(String.valueOf(settings.getH2sScalepoint()));
            settingFields[45].setText(String.valueOf(settings.getCh4Scalepoint()));
            settingFields[46].setText(String.valueOf(settings.getOptions()));
            settingFields[47].setText(String.valueOf(settings.getWeekToScale()));
            settingFields[48].setText(String.valueOf(settings.getTransportAlarmOffMin()));
            settingFields[49].setText(String.valueOf(settings.getUnfreeze()));

        } else {
            clearAllFields();
            if (state.getAllSettings() == null) {
                log.warn("GetAllSettings model is null");
            } else {
                log.warn("GetAllSettings model is not loaded");
            }
        }
    }

    private void clearAllFields() {
        if (settingFields != null) {
            for (JTextField field : settingFields) {
                if (field != null) {
                    field.setText("");
                }
            }
        }
    }

    @Override
    public void saveData(DeviceState state) {
        // Для настроек пока не реализовано сохранение
    }
}