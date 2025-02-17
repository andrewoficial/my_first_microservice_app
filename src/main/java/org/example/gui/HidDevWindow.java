package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

import com.intellij.uiDesigner.core.Spacer;
import org.hid4java.*;

public class HidDevWindow extends JFrame implements Rendeble {
    private JTable deviceTable;
    private JTextField vendorFilter;
    private JTextField productFilter;
    private DefaultTableModel tableModel;
    private JPanel contentPane;
    private JButton button1;
    private JButton button2;
    private boolean MGS_found = false;
    private boolean MGS_CreadleFound = false;
    private boolean MKRS_found = false;
    private boolean MKRS_CreadleFound = false;
    private String MGS_status = " not found";
    private String MKRS_status = " not found";
    private HidServices hidServices;

    JPanel statusMGSpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
    JPanel statusMKRSpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));


    public HidDevWindow() {
        $$$setupUI$$$();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(contentPane);
        initComponents();
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
        // Устанавливаем правильный Layout
        contentPane.setLayout(new BorderLayout());

        // Панель фильтров
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        filterPanel.add(new JLabel("Vendor ID:"));
        vendorFilter = new JTextField(6);
        filterPanel.add(vendorFilter);
        filterPanel.add(new JLabel("Product ID:"));
        productFilter = new JTextField(6);
        filterPanel.add(productFilter);

        JButton filterButton = new JButton("Filter");
        filterButton.addActionListener(e -> updateDeviceList());
        filterPanel.add(filterButton);

        // Таблица устройств
        String[] columns = {"Vendor ID", "Product ID", "Manufacturer", "Product", "Serial Number"};
        tableModel = new DefaultTableModel(columns, 0);
        deviceTable = new JTable(tableModel);

        // Добавляем компоненты в contentPane
        contentPane.add(filterPanel, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(deviceTable), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new GridLayout(2, 1)); // Две строки для двух панелей
        statusPanel.add(statusMGSpanel);
        statusPanel.add(statusMKRSpanel);
        contentPane.add(statusPanel, BorderLayout.SOUTH);
        //contentPane.add(statusMGSpanel);
        //contentPane.add(statusMKRSpanel);

    }

    private void updateDeviceList() {
        tableModel.setRowCount(0); // Очистка таблицы


        List<HidDevice> devices = hidServices.getAttachedHidDevices();

        String vendorFilterText = vendorFilter.getText().trim();
        String productFilterText = productFilter.getText().trim();

        for (HidDevice device : devices) {
            if (matchesFilter(device, vendorFilterText, productFilterText)) {
                Object[] rowData = {
                        String.format("%04X", device.getVendorId()),
                        String.format("%04X", device.getProductId()),
                        device.getManufacturer(),
                        device.getProduct(),
                        device.getSerialNumber()
                };
                tableModel.addRow(rowData);
            }
        }
        MKRS_CreadleFound = false;
        MGS_CreadleFound = false;
        for (HidDevice device : devices) {

            if (device != null) {
                if (Integer.toHexString(device.getProductId()).equalsIgnoreCase("5754")) {
                    System.out.println("Найдено устройство: " + device.getProduct());
                    MKRS_CreadleFound = true;
                    poolMkrs(device);
                }
//                if (Integer.toHexString(device.getProductId()).equalsIgnoreCase("d0d0")) {
//                    System.out.println("Найдено устройство: " + device.getProduct());
//                    poolMkrs(device);
//                } else {
//                    MGS_status = " не найдено";
//                    updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
//                }

            }
        }
        if (!MKRS_CreadleFound) {
            MKRS_status = " не найдено";
            updateStatusPanel(statusMKRSpanel, "MKRS" + MKRS_status, MKRS_found);
        }
    }

    private void poolMkrs(HidDevice device) {


        byte[] buffer = new byte[512]; // Размер буфера зависит от устройства
        if (device.isOpen()) {
            device.close(); // Закрываем перед повторным открытием
        }

        if (!device.open()) {
            System.out.println(" == Ошибка открытия == ");
            return;
        }
        System.out.println("Успешно открыто");

        byte reportId = 1;
        //byte[] msg = {0x1, 0x0, 0x9, 0x55, 0x0, 0x1, (byte) 0xff, (byte) 0xff, 0x2, 0x28, 0x22, (byte) 0x9f, (byte) 0xf0}; перезагрузка
        byte[] msg = {0x01, 0x00, 0x09, 0x55, 0x00, (byte) 0x01, (byte) 0xff, (byte) 0xff, 0x2, 0x12, (byte) 0xA2, (byte) 0x8C, (byte) 0x00}; //get info
        System.out.println("Запись выполнена статус: " + device.write(msg, msg.length, reportId));
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            //throw new RuntimeException(e);
        }
        int bytesRead = device.read(buffer, 1500); // 1000 мс - таймаут

        MKRS_found = bytesRead > 0;
        if (MKRS_found) {
            MKRS_status = " найден прибор";
            System.out.println("Получен ответ от прибора");
        } else {
            MKRS_status = " найден кредл";
            System.out.println("Нет ответа от прибора");
        }
        device.setNonBlocking(true);
        device.close(); // Закрываем после использования

        updateStatusPanel(statusMKRSpanel, "MKRS" + MKRS_status, MKRS_found);
    }

    private void updateStatusPanel(JPanel panel, String label, boolean status) {
        panel.removeAll();
        panel.add(new JLabel(label));
        panel.revalidate();
        panel.repaint();
    }

//    private void poolMGS(HidDevice device) {
//        MKRS_status = " найден кредл";
//
//        byte[] buffer = new byte[64]; // Размер буфера зависит от устройства
//        if (device.isOpen()) {
//            System.out.println("Успешно открыто");
//        } else {
//            if (device.open()) {
//                System.out.println("Переоткрыто успешно ");
//            } else {
//                System.out.println("Ошибка открытия");
//                return;
//            }
//        }
//        byte reportId = 1;
//        //byte[] msg = {0x1, 0x0, 0x9, 0x55, 0x0, 0x1, (byte) 0xff, (byte) 0xff, 0x2, 0x28, 0x22, (byte) 0x9f, (byte) 0xf0}; перезагрузка
//        byte[] msg = {0x01, 0x00, 0x09, 0x55, 0x00, (byte) 0x01, (byte) 0xff, (byte) 0xff, 0x2, 0x12, (byte) 0xA2, (byte) 0x8C, (byte) 0x00}; //get info
//        System.out.println("Запись выполнена статус: " + device.write(msg, msg.length, reportId));
//        int bytesRead = device.read(buffer, 1000); // 1000 мс - таймаут
//
//        MKRS_found = bytesRead > 0;
//        updateStatusPanel(statusMKRSpanel, "MKRS" + MKRS_status, MKRS_found);
//    }

    private static String bytesToHex(byte[] bytes, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }

    private boolean matchesFilter(HidDevice device, String vendorFilter, String productFilter) {
        String deviceVendor = String.format("%04X", device.getVendorId());
        String deviceProduct = String.format("%04X", device.getProductId());

        return (vendorFilter.isEmpty() || deviceVendor.equalsIgnoreCase(vendorFilter)) &&
                (productFilter.isEmpty() || deviceProduct.equalsIgnoreCase(productFilter));
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
        contentPane.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(800, 800), null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        button2 = new JButton();
        button2.setText("Button");
        panel1.add(button2, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button1 = new JButton();
        button1.setText("Button");
        panel1.add(button1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        contentPane.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}


