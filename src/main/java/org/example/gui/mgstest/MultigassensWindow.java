package org.example.gui.mgstest;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.mgstest.device.DeviceInfo;
import org.example.gui.mgstest.transport.CradleController;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class MultigassensWindow extends JFrame implements Rendeble {

    private Logger log = Logger.getLogger(MultigassensWindow.class);

    private JTable deviceTable;
    private DefaultTableModel tableModel;
    private JPanel contentPane;
    private JButton shutdownButton;
    private JButton getInfoButton;
    private JButton alarmOnButton;
    private JButton alarmOffButton;
    private JButton opticCommandButton;
    private JButton beepCmd;
    private JButton blinkCmd;
    private JButton rebootCmd;
    private JButton resetBatteryButton;
    private JButton setSerialNumberButton;
    private boolean MGS_found = false;
    private boolean MGS_CreadleFound = false;
    private String MGS_status = " not found";
    private HidServices hidServices;
    private HidDevice selectedDevice;
    CradleController cradleController = new CradleController();
    JPanel statusMGSpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    public MultigassensWindow() {
        setTitle("MGS Test");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        contentPane = new JPanel();
        setContentPane(contentPane);

        initComponents();
        pack();
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
        hidServices = HidManager.getHidServices();
        contentPane.setLayout(new BorderLayout());

        String[] columns = {"Vendor ID", "Product ID", "Manufacturer", "Product", "Serial Number"};
        tableModel = new DefaultTableModel(columns, 0);
        deviceTable = new JTable(tableModel);

        contentPane.add(new JScrollPane(deviceTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        shutdownButton = new JButton("Shutdown Device");
        shutdownButton.addActionListener(e -> {
            if (selectedDevice != null) {
                //cradleController.
            } else {
                JOptionPane.showMessageDialog(this, "No device selected");
            }
        });
        buttonPanel.add(shutdownButton);

        getInfoButton = new JButton("Get Info");
        getInfoButton.addActionListener(e -> {

        });
        buttonPanel.add(getInfoButton);

        alarmOnButton = new JButton("Alarm On");
        alarmOnButton.addActionListener(e -> {
            if (selectedDevice != null) {
                cradleController.soundOn(selectedDevice);
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(alarmOnButton);

        alarmOffButton = new JButton("Alarm Off");
        alarmOffButton.addActionListener(e -> {
            if (selectedDevice != null) {
                cradleController.soundOff(selectedDevice);
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(alarmOffButton);

        opticCommandButton = new JButton("Send Optic Command");
        opticCommandButton.addActionListener(e -> {
            String text = JOptionPane.showInputDialog(this, "Enter text for optic command:");

        });
        buttonPanel.add(opticCommandButton);

        beepCmd = new JButton("Beep Test");
        beepCmd.addActionListener(e -> {
            if (selectedDevice != null) {
                cradleController.beepTest(selectedDevice);
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(beepCmd);

        blinkCmd = new JButton("Blink Test");
        blinkCmd.addActionListener(e -> {
            if (selectedDevice != null) {
                //cradleController.blinkTest(selectedDevice);
                byte[] deviceInfoRaw = null;
                try {
                    deviceInfoRaw = cradleController.getDeviceInfo(selectedDevice);
                } catch (Exception ex) {
                    log.warn(ex.getMessage());
                }

                try{
                    DeviceInfo info = DeviceInfo.parseDeviceInfo(deviceInfoRaw);
                }catch (Exception ex){
                    log.warn(ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(blinkCmd);

        rebootCmd = new JButton("Reboot");
        rebootCmd.addActionListener(e -> {
            if (selectedDevice != null) {
                cradleController.rebootCmd(selectedDevice);
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(rebootCmd);

        resetBatteryButton = new JButton("Reset Battery Counter");
        resetBatteryButton.addActionListener(e -> {
            if (selectedDevice != null) {
                cradleController.resetBatteryCounter(selectedDevice);
            } else {
                JOptionPane.showMessageDialog(this, "No device connected");
            }
        });
        buttonPanel.add(resetBatteryButton);

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

        updateDeviceList();
    }

    private void updateDeviceList() {
        tableModel.setRowCount(0);

        List<HidDevice> devices = hidServices.getAttachedHidDevices();

        MGS_CreadleFound = false;
        for (HidDevice device : devices) {
            if (device != null && Integer.toHexString(device.getProductId()).equalsIgnoreCase("d0d0")) {
                Object[] rowData = {
                        String.format("%04X", device.getVendorId()),
                        String.format("%04X", device.getProductId()),
                        device.getManufacturer(),
                        device.getProduct(),
                        device.getSerialNumber()
                };
                tableModel.addRow(rowData);

                System.out.println("Найден кредл: " + device.getProduct());
                MGS_status = " кредл найден";
                MGS_CreadleFound = true;
                MGS_found = true;
                selectedDevice = device;
                poolMgs(device);
            }
        }
        if (!MGS_CreadleFound) {
            MGS_status = " не найдено";
            MGS_found = false;
        }
        updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
    }

    private void poolMgs(HidDevice device) {
        MGS_status = " найден кредл";

        if (device.isOpen()) {
            System.out.println("Успешно открыто");
        } else {
            if (device.open()) {
                System.out.println("Переоткрыто успешно ");
            } else {
                System.out.println("Ошибка открытия");
                return;
            }
        }

        updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
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