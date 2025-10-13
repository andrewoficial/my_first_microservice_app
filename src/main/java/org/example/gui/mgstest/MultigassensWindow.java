package org.example.gui.mgstest;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.components.SimpleTabbedPane;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.repository.DeviceStateRepository;
import org.example.gui.mgstest.service.DeviceAnswerParser;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.service.DeviceManager;
import org.example.gui.mgstest.gui.tabs.TabCoefficients;
import org.example.gui.mgstest.gui.tabs.DeviceTab;
import org.example.gui.mgstest.gui.tabs.TabInfo;
import org.example.gui.mgstest.gui.tabs.TabSettings;
import org.example.gui.mgstest.gui.tabs.UartHistory;
import org.example.gui.mgstest.transport.CradleController;
import org.example.gui.mgstest.transport.DeviceCommand;

import org.example.gui.mgstest.transport.cmd.GetAllCoefficients;
import org.example.gui.mgstest.transport.HidCommandName;

import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class MultigassensWindow extends JFrame implements Rendeble, MgsExecutionListener {
    private Logger log = Logger.getLogger(MultigassensWindow.class);

    private JList<String> cradleList;
    private DefaultListModel<String> listModel;
    private JPanel contentPane;

    private JButton getCoefficientsButton;
    private JButton setCoefficientsButton;
    private JButton setCoefficientsButtonCo;


    private HidDevice selectedDevice;
    CradleController cradleController = new CradleController();
    private DeviceManager deviceManager = new DeviceManager();
    private DeviceStateRepository stateRepository = new DeviceStateRepository();
    private DeviceAnswerParser deviceAnswerParser;
    private JTabbedPane tabbedPane;
    private final Map<String, DeviceTab> tabs = new HashMap<>();
    private JPanel progressPanel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private DeviceAsyncExecutor asyncExecutor;
    public MultigassensWindow() {
        setTitle("MGS Test");
        this.deviceAnswerParser = new DeviceAnswerParser(cradleController, stateRepository);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(800, 600);
        Dimension maxSize = new Dimension(1000, 700);
        setMaximumSize(maxSize);
        this.asyncExecutor = new DeviceAsyncExecutor(stateRepository);
        this.asyncExecutor.addListener(this);
        contentPane = new JPanel();
        setContentPane(contentPane);

        initComponents();


        pack();
    }
    private void initProgressPanel() {
        progressPanel = new JPanel(new BorderLayout());
        progressBar = new JProgressBar(0, 100);
        statusLabel = new JLabel("Ready");

        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(statusLabel, BorderLayout.SOUTH);
        progressPanel.setVisible(false);

        contentPane.add(progressPanel, BorderLayout.SOUTH);
    }

    @Override
    public void renderData() {
        updateDeviceList();
    }

    @Override
    public boolean isEnable() {
        return true;
    }



    private void initComponents() {
        contentPane.setLayout(new BorderLayout());
        initTabs();
        initDeviceList();
        initButtons();
        initProgressPanel();
        updateDeviceList();
    }

    private void initTabs() {
        tabbedPane = new SimpleTabbedPane();

        TabInfo infoTab = new TabInfo(selectedDevice, asyncExecutor);
        tabs.put("info", infoTab);
        tabbedPane.addTab(infoTab.getTabName(), infoTab.getPanel());

        TabCoefficients coefficientsTab = new TabCoefficients(selectedDevice, asyncExecutor);
        tabs.put("coefficients", coefficientsTab);
        tabbedPane.addTab(coefficientsTab.getTabName(), coefficientsTab.getPanel());

        TabSettings settingsTab = new TabSettings(selectedDevice, asyncExecutor);
        tabs.put("settings", settingsTab);
        tabbedPane.addTab(settingsTab.getTabName(), settingsTab.getPanel());

        UartHistory uartHistory = new UartHistory(cradleController, selectedDevice, null, stateRepository, asyncExecutor);
        tabs.put("uartHistory", uartHistory);
        tabbedPane.addTab(uartHistory.getTabName(), uartHistory.getPanel());

        contentPane.add(tabbedPane, BorderLayout.CENTER);
    }

    private void initDeviceList() {
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(200, 400));

        listModel = new DefaultListModel<>();
        cradleList = new JList<>(listModel);
        cradleList.addListSelectionListener(e -> onDeviceSelected());

        leftPanel.add(new JLabel("Cradles:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(cradleList), BorderLayout.CENTER);
        contentPane.add(leftPanel, BorderLayout.WEST);
    }

    private void onDeviceSelected() {

        String selectedDisplayName = cradleList.getSelectedValue();
        int selectedNumber = cradleList.getSelectedIndex();
        // Обновляем вкладки с новым устройством
        log.info("Меняю устройство для отображения:" + selectedNumber);

        if (selectedDisplayName != null) {
            log.info("Выбрано " + selectedDisplayName);
            tabbedPane.setVisible(true);
            selectedDevice = deviceManager.getDeviceByDisplayName(selectedDisplayName);
            if (selectedDevice != null) {
                refreshGui();
            }
        }
    }

    private void initButtons() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        getCoefficientsButton = new JButton("GetCoef");
        getCoefficientsButton.addActionListener(e -> onGetCoefficients());
        buttonPanel.add(getCoefficientsButton);

        setCoefficientsButton = new JButton("SET O2");
        setCoefficientsButton.addActionListener(e -> onSetCoefficientsO2());
        buttonPanel.add(setCoefficientsButton);

        setCoefficientsButtonCo = new JButton("SET CO");
        setCoefficientsButtonCo.addActionListener(e -> onSetCoefficientsCO());
        buttonPanel.add(setCoefficientsButtonCo);

        contentPane.add(buttonPanel, BorderLayout.NORTH);
    }

    private void onGetCoefficients() {
        if(isDevNull(selectedDevice)) {return;}
        checkStateRepo(selectedDevice);
        if(isDevBusy(selectedDevice)) {return;}

        DeviceCommand command = new GetAllCoefficients();
        asyncExecutor.executeCommand(command, null, selectedDevice);
    }


    private void onSetCoefficientsO2() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, "No device selected");
            return;
        }

        try {
            deviceAnswerParser.setCoefficientsO2(selectedDevice);
            JOptionPane.showMessageDialog(this, "O2 coefficients set successfully");
        } catch (Exception ex) {
            log.warn("Error during setCoefficientsO2: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error setting O2 coefficients: " + ex.getMessage());
        }
    }

    private void onSetCoefficientsCO() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, "No device selected");
            return;
        }

        try {
            deviceAnswerParser.setCoefficientsCO(selectedDevice);
            JOptionPane.showMessageDialog(this, "CO coefficients set successfully");
        } catch (Exception ex) {
            log.warn("Error during setCoefficientsCO: " + ex.getMessage());
            JOptionPane.showMessageDialog(this, "Error setting CO coefficients: " + ex.getMessage());
        }
    }





    private boolean isDevNull(HidDevice dev){
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, "No device selected");
            return true;
        }
        return false;
    }

    private boolean isDevBusy(HidDevice device){
        if(asyncExecutor.isDeviceBusy(device)){
            JOptionPane.showMessageDialog(this, "Busy");
            return true;
        }
        return false;
    }

    private void checkStateRepo(HidDevice device){
        if(!stateRepository.contains(device)){
            stateRepository.put(device, new DeviceState());
        }
    }

    private void updateDeviceList() {
        listModel.clear();
        deviceManager.updateDeviceList();

        Map<String, HidDevice> deviceMap = deviceManager.getDeviceMap();
        for (Map.Entry<String, HidDevice> stringHidDeviceEntry : deviceMap.entrySet()) {
            if(stateRepository.contains(stringHidDeviceEntry.getValue()) && stateRepository.get(stringHidDeviceEntry.getValue()).getShowedName() != null ){
                listModel.addElement(stateRepository.get(stringHidDeviceEntry.getValue()).getShowedName());
            }
            listModel.addElement(stringHidDeviceEntry.getKey());
        }
    }

    private void refreshGui(){
            updateDeviceList();
            TabInfo infoTab = (TabInfo) tabs.get("info");
            TabCoefficients coefficientsTab = (TabCoefficients) tabs.get("coefficients");
            UartHistory uartHistory = (UartHistory) tabs.get("uartHistory");
            TabSettings tabSettings = (TabSettings) tabs.get("settings");
            updateDeviceInfo(selectedDevice);

            //uartHistory.setCradleController(cradleController);

            infoTab.setSelectedDevice(selectedDevice);
            coefficientsTab.setSelectedDevice(selectedDevice);
            uartHistory.setSelectedDevice(selectedDevice);
            tabSettings.setSelectedDevice(selectedDevice);

            if (stateRepository.contains(selectedDevice)) {
                DeviceState state = stateRepository.get(selectedDevice);
                infoTab.updateData(state);
                coefficientsTab.updateData(state);
                uartHistory.updateData(state);
                tabSettings.updateData(state);
            } else {
                infoTab.updateData(null);
                coefficientsTab.updateData(null);
                coefficientsTab.updateData(null);
                tabSettings.updateData(null);
            }
    }
    private void updateDeviceInfo(HidDevice deviceKey) {
        DeviceState state = stateRepository.get(deviceKey);
        log.info("Called updateDeviceInfo for key " + deviceKey);

        if (state != null && state.getDeviceInfo() != null) {
            log.info("Found device with number " + state.getDeviceInfo().getSerialNumber());
        } else {
            log.info("Empty data");
        }

        for (DeviceTab tab : tabs.values()) {
            log.info("Updating tab " + tab.getTabName());
            tab.updateData(state);
        }
    }

    // Реализация ExecutionListener
    @Override
    public void onExecutionFinished(HidDevice device, int progress, byte[] answer, HidCommandName commandName){
        log.info("Вот тут надо начать парсинг");
        try {
            deviceAnswerParser.parseByName(answer, commandName, device);
            refreshGui();
        } catch (Exception e) {
            log.warn("Исключение при парсинге ответа на команду " + commandName + " " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Исключение при парсинге ответа на команду " + commandName + " " + e.getMessage());
        }finally {
            updateUIForSelectedDevice();
        }
    }

    @Override
    public void onExecutionEvent(HidDevice deviceId, String message, boolean isError) {
        log.info("Вот тут отображать событие, но пока не важно");
    }

    @Override
    public void onProgressUpdate(HidDevice deviceId, int progress, String message) {
        log.info("Вот тут надо двигать прогрессбар");
        if(stateRepository.contains(deviceId)){
            stateRepository.get(deviceId).setProgressPercent(progress);
            stateRepository.get(deviceId).setProgressMessage(message);
        }
        if(isSelectedDevice(deviceId)){
            updateUIForSelectedDevice();
        }
    }

    private boolean isSelectedDevice(HidDevice deviceId) {
        if (selectedDevice == null) return false;
        return selectedDevice.equals(deviceId);
    }

    // В методе обновления UI при выборе устройства
    private void updateUIForSelectedDevice() {
        if (selectedDevice != null) {
            DeviceState state = stateRepository.get(selectedDevice);

            // Обновляем прогресс-бар и статус
            if (state != null) {
                progressBar.setValue(state.getProgressPercent());
                statusLabel.setText(state.getProgressMessage());
                progressPanel.setVisible(state.getIsBusy());
            }
        }
    }

    @Override
    public void dispose() {
        asyncExecutor.shutdown();
        super.dispose();
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