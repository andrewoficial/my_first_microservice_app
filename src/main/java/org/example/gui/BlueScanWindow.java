package org.example.gui;

import com.intellij.uiDesigner.core.GridLayoutManager;
import org.sputnikdev.bluetooth.manager.*;
import org.sputnikdev.bluetooth.manager.impl.BluetoothManagerBuilder;
import org.sputnikdev.bluetooth.URL;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class BlueScanWindow extends JFrame implements DeviceDiscoveryListener {
    private JComboBox<String> adapterComboBox;
    private JButton scanButton;
    private JButton stopButton;
    private JTable devicesTable;
    private DefaultTableModel tableModel;
    private final List<DiscoveredDevice> discoveredDevices = new CopyOnWriteArrayList<>();
    private final BluetoothManager manager;
    private volatile boolean isScanning = false;

    public BlueScanWindow() {
        super("BLE Scanner");
        setSize(800, 600);
        manager = new BluetoothManagerBuilder().build();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        initUI();
        initBluetooth();
    }

    private void initUI() {
        // Панель выбора адаптера
        JPanel topPanel = new JPanel(new FlowLayout());
        adapterComboBox = new JComboBox<>();
        scanButton = new JButton("Сканировать");
        stopButton = new JButton("Остановить");
        stopButton.setEnabled(false);
        topPanel.add(new JLabel("Адаптер:"));
        topPanel.add(adapterComboBox);
        topPanel.add(scanButton);
        topPanel.add(stopButton);

        // Таблица устройств
        String[] columns = {"Адрес", "Имя", "RSSI", "Данные"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        devicesTable = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(devicesTable);

        // Обработчики событий
        scanButton.addActionListener(this::startScanning);
        stopButton.addActionListener(e -> stopScanning());

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void initBluetooth() {
        try {
             //Для Windows


            Set<DiscoveredAdapter> adapters = manager.getDiscoveredAdapters();
            for (DiscoveredAdapter adapter : adapters) {
                adapterComboBox.addItem(adapter.getURL().getAdapterAddress());
            }
            if (adapters.isEmpty()) {
                adapterComboBox.addItem("No adapters found");
                scanButton.setEnabled(false);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка инициализации Bluetooth Manager: " + e.getMessage());
        }
    }

    private void startScanning(ActionEvent e) {
        if (manager == null) return;

        tableModel.setRowCount(0);
        discoveredDevices.clear();
        isScanning = true;
        scanButton.setEnabled(false);
        stopButton.setEnabled(true);

        // Регистрируем слушатель
        manager.addDeviceDiscoveryListener(this);
        try {
            // Начинаем сканирование
            manager.start(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Ошибка сканирования: " + ex.getMessage());
            stopScanning();
        }
    }

    private void stopScanning() {
        if (isScanning) {
            try {
                // Останавливаем сканирование
                manager.stop();
                manager.removeDeviceDiscoveryListener(this);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка остановки сканирования: " + ex.getMessage());
            }
            isScanning = false;
        }
        scanButton.setEnabled(true);
        stopButton.setEnabled(false);
    }

    private void updateTable(DiscoveredDevice device) {
        SwingUtilities.invokeLater(() -> {
            Object[] row = {
                    device.getURL().getDeviceAddress(),
                    device.getName() != null ? device.getName() : "Неизвестно",
                    device.getRSSI(),
                    parseAdvertisementData(device)
            };
            tableModel.addRow(row);
        });
    }

    private String parseAdvertisementData(DiscoveredDevice device) {
        // Пример: получение UUID сервисов или других данных
        StringBuilder data = new StringBuilder();
        DeviceGovernor deviceGovernor = manager.getDeviceGovernor(device.getURL());
        if (deviceGovernor != null && deviceGovernor.getServiceData() != null) {
            deviceGovernor.getServiceData().forEach((uuid, bytes) -> data.append(uuid).append("; "));
        }
        return data.length() > 0 ? data.toString() : "Нет данных";
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BlueScanWindow().setVisible(true));
    }


    @Override
    public void discovered(DiscoveredDevice discoveredDevice) {
        // Проверяем, новое ли это устройство
        if (!discoveredDevices.contains(discoveredDevice)) {
            discoveredDevices.add(discoveredDevice);
            updateTable(discoveredDevice);
        }
    }

    @Override
    public void deviceLost(DiscoveredDevice lostDevice) {
        DeviceDiscoveryListener.super.deviceLost(lostDevice);
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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
    }
}
