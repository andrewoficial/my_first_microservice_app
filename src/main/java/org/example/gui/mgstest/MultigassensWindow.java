package org.example.gui.mgstest;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.components.SimpleTabbedPane;
import org.example.gui.mgstest.gui.tabs.*;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.service.MgsExecutionListener;
import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.repository.DeviceStateRepository;
import org.example.gui.mgstest.service.DeviceAnswerParser;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.repository.DeviceRepository;
import org.example.gui.mgstest.transport.CradleController;
import org.example.gui.mgstest.transport.DeviceCommand;

import org.example.gui.mgstest.transport.cmd.mgs.GetAllCoefficients;
import org.example.gui.mgstest.transport.HidCommandName;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class MultigassensWindow extends JFrame implements Rendeble, MgsExecutionListener {
    private final Logger log = Logger.getLogger(MultigassensWindow.class);

    private JList<HidSupportedDevice> deviceList;
    private DefaultListModel<HidSupportedDevice> listModel = new DefaultListModel<>();
    private JPanel contentPane;

    private JButton getCoefficientsButton;
    private JButton setCoefficientsButton;
    private JButton setCoefficientsButtonCo;


    private HidSupportedDevice selectedDevice;
    CradleController cradleController = new CradleController();
    private final DeviceRepository deviceRepository;
    private final DeviceStateRepository stateRepository = new DeviceStateRepository();
    private final DeviceAnswerParser deviceAnswerParser;
    private JTabbedPane tabbedPane;
    private final Map<String, DeviceTab> tabs = new HashMap<>();
    private JPanel progressPanel;
    private JProgressBar progressBar;
    private JLabel statusLabel;
    private final DeviceAsyncExecutor asyncExecutor;
    public MultigassensWindow() {
        setTitle("MGS Test");
        this.deviceAnswerParser = new DeviceAnswerParser(cradleController, stateRepository);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(1100, 850);
        Dimension maxSize = new Dimension(1100, 850);
        setMaximumSize(maxSize);
        this.asyncExecutor = new DeviceAsyncExecutor(stateRepository);
        this.asyncExecutor.addListener(this);
        contentPane = new JPanel();
        contentPane.setPreferredSize(new Dimension(1100, 850));
        setContentPane(contentPane);
        deviceRepository = new DeviceRepository(stateRepository);
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
        log.info("Обновляю список приборов");
        deviceRepository.updateDeviceList();
        //listModel.removeAllElements();//Потеря ссылок, а та та
        HashSet<HidSupportedDevice> list = deviceRepository.getDeviceList();
        for (HidSupportedDevice supportedDevice : list) {
            if(listModel.contains(supportedDevice)){
                //listModel.addElement(supportedDevice);
                //log.info("Вот тут с новым именем " + supportedDevice.getHidDevice().getSerialNumber());
            }else{
                listModel.addElement(supportedDevice);
                //log.info("Не содержится " + supportedDevice.getHidDevice().getSerialNumber());
            }
        }

        revalidate();
        repaint();
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
        renderData();
    }

    private void initTabs() {
        tabbedPane = new SimpleTabbedPane();

        TabInfo infoTab = new TabInfo(selectedDevice, asyncExecutor);
        tabs.put("info", infoTab);
        tabbedPane.addTab(infoTab.getTabName(), infoTab.getPanel());

        TabCoefficients coefficientsTab = new TabCoefficients(selectedDevice, asyncExecutor);
        tabs.put("coefficients", coefficientsTab);
        tabbedPane.addTab(coefficientsTab.getTabName(), coefficientsTab.getPanel());

        TabMetrology tabMetrology = new TabMetrology(selectedDevice, asyncExecutor);
        tabs.put("tabMetrology", tabMetrology);
        tabbedPane.addTab(tabMetrology.getTabName(), tabMetrology.getPanel());

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


        deviceList = new JList<>(listModel);
        deviceList.addListSelectionListener(e -> onDeviceSelected());

        leftPanel.add(new JLabel("Cradles:"), BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(deviceList), BorderLayout.CENTER);
        contentPane.add(leftPanel, BorderLayout.WEST);
    }

    private void onDeviceSelected() {
        log.info("Set selected device deviceList " + deviceList.getSelectedValue());
        if(deviceList.getSelectedValue() == null ){
            log.warn("Выбрано NULL!!");
        }
        selectedDevice = deviceList.getSelectedValue();
        refreshGui();
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





    private boolean isDevNull(HidSupportedDevice dev){
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(this, "No device selected");
            return true;
        }
        return false;
    }

    private boolean isDevBusy(HidSupportedDevice device){
        if(asyncExecutor.isDeviceBusy(device)){
            JOptionPane.showMessageDialog(this, "Busy");
            return true;
        }
        return false;
    }

    private void checkStateRepo(HidSupportedDevice device){
        if(!stateRepository.contains(device)){
            stateRepository.put(device, new DeviceState());
        }
    }



    private void refreshGui(){
            for (DeviceTab value : tabs.values()) {
                value.setSelectedDevice(selectedDevice);
            }

            if (stateRepository.contains(selectedDevice)) {
                DeviceState state = stateRepository.get(selectedDevice);
                for (DeviceTab tab : tabs.values()) {
                    log.info("Updating tab for values " + tab.getTabName());
                    tab.updateData(state);
                }
            } else {
                for (DeviceTab tab : tabs.values()) {
                    log.info("Updating tab for empty values " + tab.getTabName());
                    tab.updateData(null);
                }
            }
    }

    private boolean isSelectedDevice(HidSupportedDevice deviceId) {
        if (selectedDevice == null)
            return false;

        return selectedDevice.equals(deviceId);
    }

    // Обновление полей State у выбранного Device
    private void updateUIForSelectedDevice() {
        if (selectedDevice != null) {
            DeviceState state = stateRepository.get(selectedDevice);

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

    // Реализация ExecutionListener
    @Override
    public void onExecutionFinished(HidSupportedDevice device, int progress, byte[] answer, HidCommandName commandName){
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
    public void onExecutionEvent(HidSupportedDevice deviceId, String message, boolean isError) {
        log.info("Событие " + message);
        renderData();
        if(isError){
            log.info(message + " is error " + isError);
            JOptionPane.showMessageDialog(this, "Ошибка выполнения команды " + message + " ");
        }

    }

    @Override
    public void onProgressUpdate(HidSupportedDevice deviceId, int progress, String message) {
        log.info("Изменение прогресса " + message);
        if(stateRepository.contains(deviceId)){
            stateRepository.get(deviceId).setProgressPercent(progress);
            stateRepository.get(deviceId).setProgressMessage(message);
        }
        if(isSelectedDevice(deviceId)){
            updateUIForSelectedDevice();
            renderData();
        }
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