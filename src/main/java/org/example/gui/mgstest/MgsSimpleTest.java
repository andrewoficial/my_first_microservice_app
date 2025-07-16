package org.example.gui.mgstest;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.hid4java.HidDevice;
import org.hid4java.HidManager;
import org.hid4java.HidServices;
import org.jfree.data.json.JSONUtils;
import org.w3c.dom.ls.LSOutput;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.List;

public class MgsSimpleTest extends JFrame implements Rendeble {

    private Logger log = Logger.getLogger(MgsSimpleTest.class);


    private JTable deviceTable;
    private JTextField vendorFilter;
    private JTextField productFilter;
    private DefaultTableModel tableModel;
    private JPanel contentPane;
    private JButton button1;
    private JButton button2;
    private boolean MGS_found = false;
    private boolean MGS_CreadleFound = false;
    private String MGS_status = " not found";
    private HidServices hidServices;

    JPanel statusMGSpanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

    public MgsSimpleTest() {
        setTitle("MGS Test"); // Добавлен заголовок окна
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Добавлено поведение при закрытии

        //$$$setupUI$$$();
        contentPane = new JPanel(); // Создаем новую основную панель
        setContentPane(contentPane);

        initComponents();
        pack(); // Автоматически подбираем размер окна
        initComponents();
    }

    @Override
    public void renderData() {
        //updateDeviceList();
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    private void initComponents() {
        hidServices = HidManager.getHidServices();
        contentPane.setLayout(new BorderLayout());

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

        String[] columns = {"Vendor ID", "Product ID", "Manufacturer", "Product", "Serial Number"};
        tableModel = new DefaultTableModel(columns, 0);
        deviceTable = new JTable(tableModel);

        contentPane.add(filterPanel, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(deviceTable), BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new GridLayout(1, 1));
        statusPanel.add(statusMGSpanel);
        contentPane.add(statusPanel, BorderLayout.SOUTH);
    }

    private void updateDeviceList() {
        tableModel.setRowCount(0);

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

        MGS_CreadleFound = false;
        for (HidDevice device : devices) {
            if (device != null) {
                if (Integer.toHexString(device.getProductId()).equalsIgnoreCase("d0d0")) {
                    System.out.println("Найден кредл: " + device.getProduct());
                    MGS_status = " кредл найден";
                    MGS_CreadleFound = true;
                    poolMgs(device);
                    return;
                } else {
                    MGS_status = " кредл не найден";
                    updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
                }
            }
        }
        if (!MGS_CreadleFound) {
            MGS_status = " не найдено";
            updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
        }
    }

    private void poolMgs(HidDevice device) {
        MGS_status = " найден кредл";

        byte[] buffer = new byte[64];
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
        byte reportId = 0x3;
        int bytesRead = 0;
        if (device.isOpen()) {
            System.out.println("Перед отправкой все еще открыто");
        } else {
            System.out.println("Перед отправкой ЗАКРЫТО");
        }

        System.out.println("ReportID: " + reportId); //Это первый байт сообщения, поэтому убираем первый байт сообщения и заменяем его репортом
        reportId = (byte) 0x02;
        System.out.println("Отправка [1] сообщения:");
        byte[] payload1 = {(byte) 0xbc, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc};
        byte[] msg = generateMessageInitial(payload1);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);


        System.out.println("Отправка [2] сообщения:");
        reportId = (byte) 0x02;
        byte[] payload2 = {(byte) 0xb2, (byte) 0xcc, (byte) 0xcc, (byte) 0xcc};
        msg = generateMessageInitial(payload2);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);

        System.out.println("Отправка [3] сообщения:");
        reportId = (byte) 0x02;
        byte[] payload3 = {(byte) 0xbd, (byte) 0xcc};
        msg = generateMessageInitial(payload3);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [4] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload4 = {(byte) 0x55, (byte) 0xcc};
        msg = generateMessageInitial(payload4);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [5] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload5 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0d};
        msg = generateMessageInitial(payload5);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [6] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload6 = {(byte) 0x04, (byte) 0x02, (byte) 0x02, (byte) 0x2b};
        msg = generateMessageInitial(payload6);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [7] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload7 = {(byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00};
        msg = generateMessageConfigured(payload7);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();


        System.out.println("Отправка [777] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload777 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0d};
        msg = generateMessageConfigured(payload777);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [8] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload8 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21};
        msg = generateMessageConfigured(payload8);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [9] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload9 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x01, (byte) 0x03, (byte) 0x11, (byte) 0xd1, (byte) 0x01};
        msg = generateMessageConfigured(payload9);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [10] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload10 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x02, (byte) 0x0d, (byte) 0x54, (byte) 0x02, (byte) 0x65};
        msg = generateMessageConfigured(payload10);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [11] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload11 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6e, (byte) 0x00};
        msg = generateMessageConfigured(payload11);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [12] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload12 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x04, (byte) 0x00, (byte) 0x01};
        msg = generateMessageConfigured(payload12);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [13] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload13 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xfe};
        msg = generateMessageConfigured(payload13);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [14] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload14 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x06, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xbd};
        msg = generateMessageConfigured(payload14);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [15] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload15 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6e, (byte) 0x06, (byte) 0x00, (byte) 0x00};
        msg = generateMessageConfigured(payload15);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [16] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload16 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xe1, (byte) 0x40, (byte) 0xff, (byte) 0x01};
        msg = generateMessageConfigured(payload16);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [17] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload17 = {(byte) 0x02, (byte) 0x02};
        msg = generateMessageConfigured(payload17);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [18] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload18 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
        msg = generateMessageConfigured(payload18);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [19] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload19 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x00, (byte) 0x07};
        msg = generateMessageConfigured(payload19);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [20] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload20 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x08, (byte) 0x07};
        msg = generateMessageConfigured(payload20);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [21] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload21 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x10, (byte) 0x07};
        msg = generateMessageConfigured(payload21);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println("Отправка [22] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload22 = {(byte) 0x02, (byte) 0x02};
        msg = generateMessageConfigured(payload22);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        longSleep();

        System.out.println("==TurnOff device == Отправка [23] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload23 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
        msg = generateMessageConfigured(payload23);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        longSleep();

        System.out.println("==repeat == Отправка [24] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload24 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
        msg = generateMessageConfigured(payload24);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [25] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload25 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21};
        msg = generateMessageConfigured(payload25);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [26] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload26 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x01, (byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01};
        msg = generateMessageConfigured(payload26);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();

        System.out.println(" Отправка [27] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload27 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x02, (byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65};
        msg = generateMessageConfigured(payload27);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [28] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload28 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E};
        msg = generateMessageConfigured(payload28);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [29] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload29 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x04, (byte) 0x00, (byte) 0x24, (byte) 0x00, (byte) 0x01};
        msg = generateMessageConfigured(payload29);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [30] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload30 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01};
        msg = generateMessageConfigured(payload30);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [31] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload31 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x06, (byte) 0x1B, (byte) 0xDF, (byte) 0x05, (byte) 0xA5};
        msg = generateMessageConfigured(payload31);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [32] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload32 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x07, (byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        msg = generateMessageConfigured(payload32);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [33] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload33 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, (byte) 0x0B, (byte) 0x00, (byte) 0x00};
        msg = generateMessageConfigured(payload33);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();


        System.out.println(" Отправка [34] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload34 = {(byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xE1, (byte) 0x40, (byte) 0xFF, (byte) 0x01};
        msg = generateMessageConfigured(payload34);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [35] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload35 = {(byte) 0x02, (byte) 0x02};
        msg = generateMessageConfigured(payload35);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();


        System.out.println(" Отправка [36] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload36 = {(byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D};
        msg = generateMessageConfigured(payload36);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [37] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload37 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x00, (byte) 0x07};
        msg = generateMessageConfigured(payload37);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [38] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload38 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x08, (byte) 0x07};
        msg = generateMessageConfigured(payload38);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [39] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload39 = {(byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, (byte) 0x10, (byte) 0x07};
        msg = generateMessageConfigured(payload39);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        safetySleep();

        System.out.println(" Отправка [40] сообщения:");
        reportId = (byte) 0x01;
        byte[] payload40 = {(byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
        msg = generateMessageConfigured(payload40);
        printArrayLikeDeviceMonitor(msg);
        System.out.println("Размер сообщения: " + msg.length + " статус отправки " + device.write(msg, msg.length, reportId));
        safetySleep();

        bytesRead = device.read(buffer, 600);
        System.out.println("Прочитано в ответ: " + bytesRead);
        System.out.println("Ответ:");
        printArrayLikeDeviceMonitor(buffer);
        Arrays.fill(buffer, (byte) 0);
        System.out.println();
        longSleep();


        System.out.println();
        MGS_found = bytesRead > 0;
        updateStatusPanel(statusMGSpanel, "MGS" + MGS_status, MGS_found);
    }

    private void safetySleep() {
        try {
            Thread.sleep(200);
        } catch (InterruptedException ex) {
            log.error("Sleep error " + ex.getMessage());
        }
    }

    private void longSleep() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ex) {
            log.error("Sleep error " + ex.getMessage());
        }
    }

    private byte[] generateMessageInitial(byte[] data) {
        if (data.length > 64) {
            log.warn("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xcc);
        int counterLimit = Math.min(64, data.length);
        for (int i = 0; i < counterLimit; i++) {
            msg[i] = data[i];
        }
        return msg;
    }

    private byte[] generateMessageConfigured(byte[] data) {
        if (data.length > 64) {
            log.warn("Слишком большое сообщение");
        }
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0);
        int counterLimit = Math.min(64, data.length);
        for (int i = 0; i < counterLimit; i++) {
            msg[i] = data[i];
        }
        return msg;
    }

    private void printArrayLikeDeviceMonitor(byte[] data) {
        if (data.length > 64) {
            log.warn("Большой массив на вывод");
        }

        for (int i = 0; i < data.length; i++) {
            // Преобразование в беззнаковое значение (0-255)
            int unsignedValue = data[i] & 0xFF;
            // Форматирование в HEX с ведущим нулем
            System.out.printf("%02X ", unsignedValue);

            // Перенос строки каждые 16 байт (после текущего элемента)
            if ((i + 1) % 16 == 0) {
                System.out.println();
            }
        }

        // Добавляем перенос в конце, если последняя строка не полная
        if (data.length % 16 != 0) {
            System.out.println();
        }
    }

    private void updateStatusPanel(JPanel panel, String label, boolean status) {
        panel.removeAll();
        panel.add(new JLabel(label));
        panel.revalidate();
        panel.repaint();
    }

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
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button1 = new JButton();
        button1.setText("Button");
        panel1.add(button1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button2 = new JButton();
        button2.setText("Button");
        panel1.add(button2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

}

