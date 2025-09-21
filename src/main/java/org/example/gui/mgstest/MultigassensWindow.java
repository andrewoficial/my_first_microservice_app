package org.example.gui.mgstest;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.components.SimpleButton;
import org.example.gui.components.SimpleTabbedPane;
import org.example.gui.mgstest.device.AllCoef;
import org.example.gui.mgstest.device.DeviceInfo;
import org.example.gui.mgstest.pool.DeviceState;
import org.example.gui.mgstest.pool.DeviceStateStorage;
import org.example.gui.mgstest.tabs.TabCoefficients;
import org.example.gui.mgstest.tabs.DeviceTab;
import org.example.gui.mgstest.tabs.TabInfo;
import org.example.gui.mgstest.tabs.TabSettings;
import org.example.gui.mgstest.transport.CradleController;
import org.example.gui.mgstest.transport.MipexResponse;
import org.example.services.comPort.StringEndianList;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultigassensWindow extends JFrame implements Rendeble {
    private Logger log = Logger.getLogger(MultigassensWindow.class);

    private JList<String> cradleList;
    private DefaultListModel<String> listModel;
    private JPanel contentPane;

    private TabInfo infoTab;
    private JButton shutdownButton;
    private JButton setCoefficientsButton;
    private JButton setCoefficientsButtonCo;
    private JButton getInfoButton;
    private JButton opticCommandButton;
    private JButton setSerialNumberButton;
    private boolean MGS_found = false;
    private boolean MGS_CreadleFound = false;
    private String MGS_status = " not found";
    private HidServices hidServices;
    private HidDevice selectedDevice;
    CradleController cradleController = new CradleController();
    JPanel statusMGSpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    private Map<String, String> deviceIdToSerialMap = new HashMap<>();
    private Map<String, String> serialToDeviceIdMap = new HashMap<>();
    private Map<String, HidDevice> deviceMap = new HashMap<>();
    private DeviceStateStorage stateStorage = DeviceStateStorage.getInstance();
    private JTabbedPane tabbedPane;
    private Map<String, DeviceTab> tabs = new HashMap<>();

    public MultigassensWindow() {
        setTitle("MGS Test");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600); // Временно изменил размер окна (проверить шрифты)

        contentPane = new JPanel();
        setContentPane(contentPane);

        initComponents();
        pack();
    }

    @Override
    public void renderData() {
        updateDeviceList();
        deviceMap.clear();
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    private void initComponents() {
        hidServices = HidManager.getHidServices();
        contentPane.setLayout(new BorderLayout());

        // Панель с информацией о устройстве
        tabbedPane = new SimpleTabbedPane();
        TabInfo infoTab = new TabInfo(cradleController, selectedDevice, null);
        tabs.put("info", infoTab);
        tabbedPane.addTab(infoTab.getTabName(), infoTab.getPanel());

        TabCoefficients coefficientsTab = new TabCoefficients(cradleController, selectedDevice, null);
        tabs.put("coefficients", coefficientsTab);
        tabbedPane.addTab(coefficientsTab.getTabName(), coefficientsTab.getPanel());

        TabSettings settingsTab = new TabSettings();
        tabs.put("settings", settingsTab);
        tabbedPane.addTab(settingsTab.getTabName(), settingsTab.getPanel());

        contentPane.add(tabbedPane, BorderLayout.CENTER);

        String[] columns = {"Vendor ID", "Product ID", "Manufacturer", "Product", "Serial Number"};
        // Панель с списком кредлов слева
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 400));

        listModel = new DefaultListModel<>();
        cradleList = new JList<>(listModel);
        cradleList.addListSelectionListener(e -> {
            String selectedDisplayName = cradleList.getSelectedValue();
            if (selectedDisplayName != null) {
                selectedDevice = deviceMap.get(selectedDisplayName);
                if (selectedDevice != null) {
                    String deviceKey = selectedDevice.getPath();
                    String storageKey = deviceIdToSerialMap.get(deviceKey);
                    if (storageKey != null) {
                        updateDeviceInfo(storageKey);
                        infoTab.setCradleController(cradleController);
                        coefficientsTab.setCradleController(cradleController);

                        infoTab.setSelectedDevice(selectedDevice);
                        coefficientsTab.setSelectedDevice(selectedDevice);
                        if(stateStorage.get(storageKey) == null){
                            log.error("stateStorage.get(storageKey) == null");
                        }else{
                            infoTab.setDeviceState(stateStorage.get(storageKey));
                            coefficientsTab.updateData(stateStorage.get(storageKey));
                        }
                    }
                }
            }
        });

        leftPanel.add(new JLabel("Cradles:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(cradleList), BorderLayout.CENTER);

        contentPane.add(leftPanel, BorderLayout.WEST);



        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        shutdownButton = new JButton("GetCoef");
        shutdownButton.addActionListener(e -> {
            if (selectedDevice != null) {
                //cradleController.

                byte[] coefRaw = null;
                try {
                    coefRaw = cradleController.getAllCoefGui(selectedDevice);
                } catch (Exception ex) {
                    log.warn("Ошибка во время выполнения getAllCoef" + ex.getMessage());
                    //throw new RuntimeException(ex);
                }
                if(coefRaw != null){
                    AllCoef coef = AllCoef.parseAllCoef(coefRaw);
                    if(stateStorage.get(generateStorageKey(selectedDevice)) != null){
                        stateStorage.get(generateStorageKey(selectedDevice)).setAllCoef(coef);
                    }else{
                        DeviceState state = new DeviceState();
                        state.setAllCoef(coef);
                        stateStorage.put(generateStorageKey(selectedDevice), state);
                    }
                    coefficientsTab.updateData(stateStorage.get(generateStorageKey(selectedDevice)));
                    log.info("Parsed coef: " + coef.toString());
                }else{
                    log.warn("coefRaw == null)");
                }

            } else {
                JOptionPane.showMessageDialog(this, "No device selected");
            }
        });
        buttonPanel.add(shutdownButton);

        setCoefficientsButton = new JButton("SET O2");
        setCoefficientsButton.addActionListener(e -> {
            if (selectedDevice != null) {
                //cradleController.
                byte[] coefRaw = null;
                try {
                    cradleController.setCoefficientsO2(selectedDevice);
                } catch (Exception ex) {
                    log.warn("Ошибка во время выполнения setCoefficientsO2" + ex.getMessage());
                    //throw new RuntimeException(ex);
                }

            } else {
                JOptionPane.showMessageDialog(this, "No device selected");
            }
        });
        buttonPanel.add(setCoefficientsButton);

        setCoefficientsButtonCo = new JButton("SET CO");
        setCoefficientsButtonCo.addActionListener(e -> {
            if (selectedDevice != null) {
                //cradleController.
                byte[] coefRaw = null;
                try {
                    cradleController.setCoefficientsCO(selectedDevice);
                } catch (Exception ex) {
                    log.warn("Ошибка во время выполнения setCoefficientsCO" + ex.getMessage());
                    //throw new RuntimeException(ex);
                }

            } else {
                JOptionPane.showMessageDialog(this, "No device selected");
            }
        });
        buttonPanel.add(setCoefficientsButtonCo);

        getInfoButton = new JButton("Get Info");
        getInfoButton.addActionListener(e -> {

        });
        buttonPanel.add(getInfoButton);



        opticCommandButton = new JButton("Send Optic Command");
        opticCommandButton.addActionListener(e -> {

            String text = JOptionPane.showInputDialog(this, "Enter text for optic command:").trim();
            MipexResponse response = new MipexResponse(0L, "Нет ответа от прибора");
            try {
                StringBuilder sb = new StringBuilder();
                sb.append(text);
                sb.append('\r');
                response = cradleController.sendMipex(sb.toString(), selectedDevice);
            } catch (Exception ex) {
                log.info(ex.getMessage());
                response = new MipexResponse(0L, ex.getMessage());
                //throw new RuntimeException(ex);
            }
            JOptionPane.showMessageDialog(this, response.text);
        });
        buttonPanel.add(opticCommandButton);



        setSerialNumberButton = new JButton("Set Serial Number");
        setSerialNumberButton.addActionListener(e -> {
            if (selectedDevice != null) {
                String input = JOptionPane.showInputDialog(this,
                        "Enter serial number (8 digits):",
                        "Set Serial Number",
                        JOptionPane.QUESTION_MESSAGE);

                if (input != null && !input.trim().isEmpty()) {
                    try {
                        // Проверяем, что введено ровно 8 цифр
                        if (input.matches("\\d{8}")) {
                            long serialNumber = Long.parseLong(input);
                            cradleController.setSerialNumber(selectedDevice, serialNumber);
                            JOptionPane.showMessageDialog(this,
                                    "Serial number set successfully: " + serialNumber,
                                    "Success",
                                    JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(this,
                                    "Please enter exactly 8 digits",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this,
                                "Invalid number format",
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(setSerialNumberButton);

        contentPane.add(buttonPanel, BorderLayout.NORTH);

        JPanel statusPanel = new JPanel(new GridLayout(1, 1));
        statusPanel.add(statusMGSpanel);
        contentPane.add(statusPanel, BorderLayout.SOUTH);
        setupGetInfoButton();
        updateDeviceList();
    }

    private String generateStorageKey(HidDevice device){
        String deviceKey = device.getPath();
        String serialNumber = device.getSerialNumber();

        // Если серийный номер доступен, используем его как основной ключ
        return  (serialNumber != null && !serialNumber.isEmpty()) ? serialNumber : deviceKey;
    }
    private void updateDeviceList() {
        listModel.clear();
        deviceMap.clear();

        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        hidServices.stop();
        MGS_CreadleFound = false;
        for (HidDevice device : devices) {
            if (device != null && Integer.toHexString(device.getProductId()).equalsIgnoreCase("d0d0")) {
                String deviceKey = device.getPath();
                String serialNumber = device.getSerialNumber();

                // Если серийный номер доступен, используем его как основной ключ
                String displayName = String.format("%s (%s)",
                        device.getProduct(),
                        serialNumber != null && !serialNumber.isEmpty() ? serialNumber : "No SN");

                //Добавление в список слева (JList)
                listModel.addElement(displayName);

                // Сохранение в deviceMap по отображаемому имени
                deviceMap.put(displayName, device);


                String storageKey = generateStorageKey(device);
                // Сохраняем mapping между идентификаторами
                deviceIdToSerialMap.put(deviceKey, storageKey);
                if (serialNumber != null) {
                    serialToDeviceIdMap.put(serialNumber, deviceKey);
                }

                // Используем storageKey для хранилища состояний
                if (!stateStorage.contains(storageKey)) {
                    stateStorage.put(storageKey, new DeviceState());
                }

                MGS_CreadleFound = true;
                MGS_found = true;
                MGS_status = " кредл найден";
            }
        }

        if (!MGS_CreadleFound) {
            MGS_status = " не найдено";
            MGS_found = false;
        }
        updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
    }

    private void onDeviceSelected(String displayName) {
        HidDevice device = deviceMap.get(displayName);
        if (device != null) {
            selectedDevice = device;
            String deviceKey = device.getPath();
            String storageKey = deviceIdToSerialMap.get(deviceKey);

            if (storageKey != null) {
                updateDeviceInfo(storageKey);
            }
        }
    }

    // При получении информации об устройстве
    private void onDeviceInfoReceived(HidDevice device, DeviceInfo info) {
        String deviceKey = device.getPath();
        String oldStorageKey = deviceIdToSerialMap.get(deviceKey);
        String newStorageKey = String.valueOf(info.getSerialNumber());

        // Если серийный номер изменился или мы раньше использовали deviceKey
        if (!newStorageKey.equals(oldStorageKey)) {
            // Переносим состояние на новый ключ
            DeviceState oldState = stateStorage.get(oldStorageKey);
            if (oldState != null) {
                stateStorage.put(newStorageKey, oldState);
                stateStorage.remove(oldStorageKey);
            }

            // Обновляем маппинги
            deviceIdToSerialMap.put(deviceKey, newStorageKey);
            serialToDeviceIdMap.put(newStorageKey, deviceKey);
        }

        updateDeviceInfo(newStorageKey);
    }

    private void updateDeviceInfo(String deviceKey) {
        DeviceState state = stateStorage.get(deviceKey);

        log.info("Вызвано updateDeviceInfo для ключа " + deviceKey);
        if(state.getDeviceInfo() != null){
            log.info("Найден прибор с номером " + state.getDeviceInfo().getSerialNumber());
        }else{
            log.info("Пустые данные ");
        }

        // Обновляем все вкладки
        for (DeviceTab tab : tabs.values()) {
            log.info("Обновляю для вкладки " + tab.getTabName());
            tab.updateData(state);
        }
    }

    // В методе обработки кнопки Get Info нужно обновлять состояние устройства
    private void setupGetInfoButton() {
        getInfoButton.addActionListener(e -> {
            if (selectedDevice != null) {
                byte[] deviceInfoRaw = null;
                try{
                    deviceInfoRaw = cradleController.getDeviceInfo(selectedDevice);
                } catch (Exception ex) {
                    log.warn("Failed getDeviceInfo: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error getting device info" + ex.getMessage());
                }
                log.info("Завершил получение данных");
                DeviceState state = null;
                DeviceInfo info = null;
                String storageKey = null;
                try {
                    if(deviceInfoRaw == null){
                        JOptionPane.showMessageDialog(this, "deviceInfoRaw is null");
                        log.info("Полученные данные пусты!");
                        return;
                    }
                    log.info("Начал парсинг массива данных");
                    info = DeviceInfo.parseDeviceInfo(deviceInfoRaw);
                    // Сохраняем информацию в хранилище
                    String deviceKey = selectedDevice.getPath();
                    storageKey = deviceIdToSerialMap.get(deviceKey);
                    log.info("Найденный ключ устройства " + storageKey);
                    state = stateStorage.get(storageKey);
                    if (state == null) {
                        state = new DeviceState();
                        stateStorage.put(storageKey, state);
                        log.info("Устройство было добавлено в пул состояний " + storageKey);
                    }


                } catch (Exception ex) {
                    log.warn("Failed to get device info: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error getting device info " + ex.getMessage());
                }

                try {
                    log.info("Для состояния обновляю данные " + info.serialNumber);
                    state.setDeviceInfo(info);
                }catch (Exception ex){
                    log.warn("Failed to get device info: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error setDeviceInfo " + ex.getMessage());
                }

                try {
                    log.info("Вызываю  updateDeviceInfo для ключа " + storageKey);
                    updateDeviceInfo(storageKey);
                }catch (Exception ex){
                    log.warn("Failed to get device info: " + ex.getMessage());
                    JOptionPane.showMessageDialog(this, "Error updateDeviceInfo " + ex.getMessage());
                }

            } else {
                JOptionPane.showMessageDialog(this, "No device selected");
            }
        });
    }

    private void updateStatusPanel(JPanel panel, String label, boolean status) {
        panel.removeAll();
        panel.add(new JLabel(label));
        panel.revalidate();
        panel.repaint();
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
        contentPane = new JPanel();
        contentPane.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}