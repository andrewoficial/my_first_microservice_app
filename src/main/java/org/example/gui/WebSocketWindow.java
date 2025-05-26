package org.example.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.device.protVega.VEGA_WAN;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.example.services.connectionPool.AnyPoolService;
import org.example.services.connectionPool.WebSocketDataCollector;
import org.example.services.loggers.DeviceLogger;
import org.example.utilites.properties.MyProperties;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;

public class WebSocketWindow extends JDialog implements Rendeble {
    private final static Logger log = Logger.getLogger(WebSocketWindow.class);//Внешний логгер
    private MyProperties prop; //Файл настроек
    private AnyPoolService anyPoolService; //Сервис опросов (разных протоколов)
    private WebSocketDataCollector webSocketDataCollector;

    private DeviceLogger deviceLogger;
    boolean needLog;
    private JPanel panel1;
    private JTextField addressField;
    private JTextField login;
    private JButton getDataButton;
    private JButton connectButton;
    private JButton loginButton;
    private JTextPane textPane1;
    private JPasswordField passwordField;
    private JButton closeButton;
    private JPanel settingsPane;

    private WebSocketClient client = new StandardWebSocketClient();  // Чистый WebSocket
    private WebSocketSession webSocketSession;  // Сессия WebSocket
    private String currentToken;
    private DefaultTableModel gatewaysTableModel;
    private DefaultTableModel devicesTableModel;
    private JTable gatewaysTable;
    private JTable devicesTable;
    private JButton addDeviceButton;
    private JButton removeDeviceButton;
    private Checkbox needLogCheckbox;
    JComboBox<String> groupComboBox;
    StringBuilder history = new StringBuilder(); //ToDo переделать

    public WebSocketWindow(MyProperties myProperties) {

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(panel1);
        prop = myProperties;
        restoreParameters();

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                webSocketDataCollector.reopenPort(99, null);
            }
        });

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                // Создаем JSON команду
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode rootNode = mapper.createObjectNode();
                rootNode.put("cmd", "auth_req");
                rootNode.put("login", login.getText());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < passwordField.getPassword().length; i++) {
                    sb.append(passwordField.getPassword()[i]);
                }
                rootNode.put("password", sb.toString());
                prop.setVegaPassword(sb.toString());
                prop.setVegaLogin(login.getText());

                String jsonString;
                try {
                    jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                    updateTextInPane1("Отправляем команду: " + jsonString);
                    webSocketDataCollector.sendOnce(jsonString, 99, true);
                } catch (JsonProcessingException exp) {
                    throw new RuntimeException(exp);
                }
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try {
                    webSocketDataCollector.closeSession(99);
                } catch (IOException ioException) {
                    updateTextInPane1("Ошибка при закрытии соединения к сокету" + ioException.getMessage());
                }
            }
        });

        try {
            VEGA_WAN vegaWan = new VEGA_WAN();
            webSocketDataCollector = new WebSocketDataCollector(vegaWan, prop.getVegaAddress(), 1000, true, 99, (clientId, action, data, cmd, payload) -> handleDataUpdate(clientId, action, data, cmd, payload));
            new Thread(webSocketDataCollector).start();
        } catch (ConnectException e) {
            log.warn("Ошибка создания подключение" + e.getMessage());
        }
        // Создаем таблицы
        createTables();
        renderData();
    }


    public void handleDataUpdate(String clientId, String action, String data, String cmd, JsonNode payload) {
        // Прямое обновление UI компонентов
        log.warn("Событие в соединении!");
        if (clientId == null || clientId.isEmpty()) {
            clientId = "null";
        }

        if (action == null || action.isEmpty()) {
            action = "null";
        }

        if (data == null || data.isEmpty()) {
            data = "null";
        }
        history.append("Event: ").append(new Date()).append(" : [").append(clientId).append("] ").append(" action: [").append(action).append("] data [").
                append(data).append("] ").append(" cmd [").append(cmd).append("] \n");
        textPane1.setText(history.toString());
        //dataTableModel.updateData(clientId, data);

        //String payload = message.getPayload();


        JsonNode rootNode = payload;


        if ("auth_resp".equals(cmd) && rootNode.get("status").asBoolean()) {
            currentToken = rootNode.get("token").asText();
            sendGetGatewaysRequest();
            sendGetDevicesRequest();
        } else if ("get_gateways_resp".equals(cmd)) {
            processGatewaysResponse(rootNode);
        } else if ("get_devices_resp".equals(cmd)) {
            processDevicesResponse(rootNode);
        } else if ("delete_devices_resp".equals(cmd)) {
            handleDeleteResponse(rootNode);
        } else if ("manage_devices_resp".equals(cmd)) {
            if (rootNode.get("status").asBoolean()) {
                sendGetDevicesRequest(); // Обновляем список после добавления
            }
        }
    }


    private void createTables() {
        setLayout(new BorderLayout());
        // Таблица для шлюзов
        gatewaysTableModel = new DefaultTableModel();
        gatewaysTableModel.addColumn("Gateway ID");
        gatewaysTableModel.addColumn("Комментарий");
        gatewaysTableModel.addColumn("Задержка");
        gatewaysTableModel.addColumn("Статус");
        gatewaysTable = new JTable(gatewaysTableModel);

        // Таблица для устройств
        devicesTableModel = new DefaultTableModel();
        devicesTableModel.addColumn("Device name");
        devicesTableModel.addColumn("DevEUI");
        devicesTableModel.addColumn("Last connection");
        devicesTableModel.addColumn("Group");
        devicesTableModel.addColumn("Class");
        devicesTable = new JTable(devicesTableModel);

        // Панель с кнопкой добавления
        //Панель с таблицей
        JPanel devicesPanel = new JPanel(new BorderLayout());
        devicesPanel.add(new JScrollPane(devicesTable), BorderLayout.CENTER);
        //Панель с кнопками
        JPanel buttonPanel = new JPanel();
        addDeviceButton = new JButton("Добавить устройство");
        removeDeviceButton = new JButton("Удалить устройство");
        needLogCheckbox = new Checkbox("Сохранять логи");
        buttonPanel.add(addDeviceButton);
        buttonPanel.add(removeDeviceButton);
        buttonPanel.add(needLogCheckbox);
        //Панель с панелями (лол)
        JPanel devAndBtnPane = new JPanel(new BorderLayout());
        devAndBtnPane.add(devicesPanel, BorderLayout.CENTER);
        devAndBtnPane.add(buttonPanel, BorderLayout.SOUTH);

        // Панель со вкладками
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Параметры", new JScrollPane(settingsPane));
        tabbedPane.addTab("Шлюзы", new JScrollPane(gatewaysTable));
        tabbedPane.addTab("Устройства", new JScrollPane(devAndBtnPane));

        add(tabbedPane, BorderLayout.CENTER);

        // Обработчик кнопки
        addDeviceButton.addActionListener(e -> showAddDeviceDialog());
        removeDeviceButton.addActionListener(e -> showDeleteDevicesDialog());
        needLogCheckbox.addItemListener(e -> setNeedLog(needLogCheckbox.getState()));
    }


    public void showAddDeviceDialog() {
        new AddDeviceDialog(this).setVisible(true);
    }


    //=====================Должно быть в зоне ответственности протокола
    private void sendGetGatewaysRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "get_gateways_req");
        if (webSocketDataCollector.getToken() != null) {
            request.put("token", webSocketDataCollector.getToken());
        }


        String jsonString;
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            updateTextInPane1("Отправляем команду: " + jsonString);
            webSocketDataCollector.sendOnce(jsonString, 99, true);
        } catch (JsonProcessingException exp) {
            throw new RuntimeException(exp);
        }

    }

    public void sendGetDevicesRequest() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode request = mapper.createObjectNode();
        request.put("cmd", "get_devices_req");
        if (currentToken != null) {
            request.put("token", webSocketDataCollector.getToken());
        }

        String jsonString;
        try {
            jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            updateTextInPane1("Отправляем команду: " + jsonString);
            webSocketDataCollector.sendOnce(jsonString, 99, true);
        } catch (JsonProcessingException exp) {
            throw new RuntimeException(exp);
        }
    }

    private void processGatewaysResponse(JsonNode rootNode) {
        log.info("Run process  GatewaysResponse");
        gatewaysTableModel.setRowCount(0);
        JsonNode gateways = rootNode.get("gateway_list");
        if (gateways != null && gateways.isArray()) {
            for (JsonNode gw : gateways) {
                String id = gw.get("gatewayId").asText();
                String info = gw.get("extraInfo").asText();
                String status = gw.get("active").asBoolean() ? "Активен" : "Неактивен";
                String lastOnline = gw.has("latency") ?
                        gw.get("latency").asLong() + " мс" : "N/A";

                gatewaysTableModel.addRow(new Object[]{id, info, lastOnline, status});
            }
        }
    }

    private void processDevicesResponse(JsonNode rootNode) {
        devicesTableModel.setRowCount(0);
        JsonNode devices = rootNode.get("devices_list");
        if (devices != null && devices.isArray()) {
            for (JsonNode dev : devices) {
                String devEui = dev.get("devEui").asText();
                String name = dev.get("devName").asText();
                String group = "N/A";
                if (dev.has("attributes") && dev.get("attributes").has("group")) {
                    group = dev.get("attributes").get("group").asText();
                }
                String lastData = dev.has("last_data_ts") ?
                        formatTimestamp(dev.get("last_data_ts").asLong()) : "N/A";
                String deviceClass = dev.get("class").asText();

                devicesTableModel.addRow(new Object[]{name, devEui, lastData, deviceClass});
            }
        }
    }

    private void handleDeleteResponse(JsonNode rootNode) {
        if (rootNode.get("status").asBoolean()) {
            JsonNode statusList = rootNode.get("device_delete_status");
            statusList.forEach(status -> {
                String devEui = status.get("devEui").asText();
                String result = status.get("status").asText();
                updateTextInPane1("Устройство " + devEui + ": " + result);
            });
            sendGetDevicesRequest(); // Обновляем список
        } else {
            JOptionPane.showMessageDialog(this, "Ошибка удаления: " + rootNode.get("err_string").asText());
        }
    }

    private void showDeleteDevicesDialog() {
        ArrayList<DeviceInfo> devices = new ArrayList<>();
        for (int i = 0; i < devicesTableModel.getRowCount(); i++) {
            devices.add(new DeviceInfo(
                    (String) devicesTableModel.getValueAt(i, 1),
                    (String) devicesTableModel.getValueAt(i, 0),
                    (String) devicesTableModel.getValueAt(i, 2)
            ));
        }

        new DeleteDevicesDialog(this, devices).setVisible(true);
    }

    private void setNeedLog(boolean needLog) {
        this.needLog = needLog;
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(timestamp));
    }

    @Override
    public void renderData() {
        // Обновляем таблицы при необходимости
        if (webSocketDataCollector.getToken() != null && (!webSocketDataCollector.getToken().isEmpty())) {
            //log.warn("Render with token:");
            //log.warn(webSocketDataCollector.getToken());
            gatewaysTable.repaint();
            devicesTable.repaint();
        }


    }

    @Override
    public boolean isEnable() {
        return true;
    }

    private void updateTextInPane1(String text) {
        System.out.println(text);
        Document doc = textPane1.getDocument();
        //final int maxLength = 25_000_000; // Примерно 50 МБ (25 млн символов)
        final int maxLength = 10_000; // Примерно 20 МБ (25 млн символов)


        try {
            if (text == null || text.isEmpty()) {
                return;
            }
            text = "\n" + text;
            int currentLength = doc.getLength();
            int newTextLength = text.length();

            // Проверяем, не превысит ли общая длина максимальный размер
            if (currentLength + newTextLength > maxLength) {
                // Вычисляем, сколько символов нужно удалить
                int overflow = (currentLength + newTextLength) - maxLength;
                int removeCount = Math.min(currentLength, overflow + 1024); // Удаляем с запасом
                doc.remove(0, removeCount);
            }
            // Вставляем новый текст
            doc.insertString(doc.getLength(), text, null);

        } catch (BadLocationException ex) {
            //ex.printStackTrace(); // Лучше залогировать ошибку

        }
        doc = null;

    }


    class DeleteDevicesDialog extends JDialog {
        private JTable devicesTable;
        private DefaultTableModel model;
        private WebSocketWindow parent;

        DeleteDevicesDialog(WebSocketWindow parent, ArrayList<DeviceInfo> devices) {
            super(parent, "Удаление устройств", true);
            setupUI(devices);
            pack();
            setLocationRelativeTo(parent);
            this.parent = parent;
        }

        private void setupUI(ArrayList<DeviceInfo> devices) {
            JPanel mainPanel = new JPanel(new BorderLayout());

            // Модель таблицы
            model = new DefaultTableModel() {
                @Override
                public Class<?> getColumnClass(int column) {
                    return column == 0 ? Boolean.class : String.class;
                }
            };
            model.addColumn("Выбрать");
            model.addColumn("DevEUI");
            model.addColumn("Имя");
            model.addColumn("Последняя активность");

            // Заполняем данными
            for (DeviceInfo device : devices) {
                model.addRow(new Object[]{false, device.getDevEui(), device.getName(), device.getLastActivity()});
            }

            devicesTable = new JTable(model);
            devicesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

            // Кнопка удаления
            JButton deleteButton = new JButton("Удалить выбранные");
            deleteButton.addActionListener(e -> deleteSelectedDevices());

            mainPanel.add(new JScrollPane(devicesTable), BorderLayout.CENTER);
            mainPanel.add(deleteButton, BorderLayout.SOUTH);

            add(mainPanel);
        }

        private void deleteSelectedDevices() {
            ArrayList<String> selectedDevEuis = new ArrayList<String>();
            for (int i = 0; i < model.getRowCount(); i++) {
                if ((Boolean) model.getValueAt(i, 0)) {
                    selectedDevEuis.add((String) model.getValueAt(i, 1));
                }
            }

            if (selectedDevEuis.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Выберите хотя бы одно устройство!");
                return;
            }

            sendDeleteRequest(selectedDevEuis);
            dispose();
        }

        private void sendDeleteRequest(ArrayList<String> devEuis) {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode request = mapper.createObjectNode();
            request.put("cmd", "delete_devices_req");
            request.put("token", (String) webSocketDataCollector.getToken());

            ArrayNode devicesArray = mapper.createArrayNode();
            devEuis.forEach(devicesArray::add);
            request.set("devices_list", devicesArray);
            webSocketDataCollector.sendOnce(request.asText(), 99, true);
        }

    }

    // 3. Добавим класс-модель для устройств
    class DeviceInfo {
        @Getter
        @Setter
        private String devEui;
        @Getter
        @Setter
        private String name;
        @Getter
        @Setter
        private String lastActivity;

        public DeviceInfo(String devEui, String name, String lastActivity) {
            this.devEui = devEui;
            this.name = name;
            this.lastActivity = lastActivity;
        }

    }

    class AddDeviceDialog extends JDialog {
        private JTextField devEuiField;
        private JTextField otaaAppEuiField;
        private JTextField otaaAppKeyField;
        private JTextField abpDevAddressField;
        private JTextField abpAppsKeyField;
        private JTextField abpNwksKeyField;
        private JTabbedPane tabbedPane;
        private JComboBox<String> groupComboBox;
        private JTextField devNameField;

        AddDeviceDialog(WebSocketWindow parent) {
            super(parent, "Добавить устройство", true);
            setupUI();
            pack();
            setLocationRelativeTo(parent);
        }

        private void setupUI() {
            JPanel mainPanel = new JPanel(new BorderLayout());

            // Вкладки
            tabbedPane = new JTabbedPane();
            tabbedPane.addTab("OTAA", createOtaaPanel());
            tabbedPane.addTab("ABP", createAbpPanel());

            // Кнопки
            JButton submitButton = new JButton("Добавить");
            submitButton.addActionListener(e -> submitDevice());

            mainPanel.add(tabbedPane, BorderLayout.CENTER);
            mainPanel.add(submitButton, BorderLayout.SOUTH);

            mainPanel.add(createGeneralPanel(), BorderLayout.NORTH);
            mainPanel.add(tabbedPane, BorderLayout.CENTER);

            add(mainPanel);
        }

        private JPanel createGeneralPanel() {
            JPanel panel = new JPanel(new GridLayout(0, 2));

            // Существующие поля
            devNameField = new JTextField(32);
            groupComboBox = new JComboBox<>(
                    new String[]{"Группа 1", "Группа 2", "Другая"});

            panel.add(new JLabel("Имя устройства:"));
            panel.add(devNameField);
            panel.add(new JLabel("Группа:"));
            panel.add(groupComboBox);

            return panel;
        }

        private JPanel createOtaaPanel() {
            JPanel panel = new JPanel(new GridLayout(0, 2));

            devEuiField = new JTextField(32);
            otaaAppEuiField = new JTextField(32);
            otaaAppKeyField = new JTextField(32);

            panel.add(new JLabel("DevEUI (16 hex):"));
            panel.add(devEuiField);
            panel.add(new JLabel("AppEUI (16 hex):"));
            panel.add(otaaAppEuiField);
            panel.add(new JLabel("AppKey (32 hex):"));
            panel.add(otaaAppKeyField);

            return panel;
        }

        private JPanel createAbpPanel() {
            JPanel panel = new JPanel(new GridLayout(0, 2));

            abpDevAddressField = new JTextField(32);
            abpAppsKeyField = new JTextField(32);
            abpNwksKeyField = new JTextField(32);

            panel.add(new JLabel("DevAddress (8 hex):"));
            panel.add(abpDevAddressField);
            panel.add(new JLabel("AppsKey (32 hex):"));
            panel.add(abpAppsKeyField);
            panel.add(new JLabel("NwksKey (32 hex):"));
            panel.add(abpNwksKeyField);

            return panel;
        }

        private void submitDevice() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode deviceNode = mapper.createObjectNode();

            // Общие поля
            deviceNode.put("devEui", devEuiField.getText().trim());

            deviceNode.put("devName", devNameField.getText().trim());

            // Группа (добавляем как атрибут)
            ObjectNode attributes = mapper.createObjectNode();
            attributes.put("group", groupComboBox.getSelectedItem().toString());
            deviceNode.set("attributes", attributes);
            // Параметры активации
            if (tabbedPane.getSelectedIndex() == 0) { // OTAA
                ObjectNode otaaNode = mapper.createObjectNode();
                otaaNode.put("appEui", otaaAppEuiField.getText().trim());
                otaaNode.put("appKey", otaaAppKeyField.getText().trim());
                deviceNode.set("OTAA", otaaNode);
            } else { // ABP
                ObjectNode abpNode = mapper.createObjectNode();
                abpNode.put("devAddress", abpDevAddressField.getText().trim());
                abpNode.put("appsKey", abpAppsKeyField.getText().trim());
                abpNode.put("nwksKey", abpNwksKeyField.getText().trim());
                deviceNode.set("ABP", abpNode);
            }

            // Формируем полный запрос
            ObjectNode request = mapper.createObjectNode();
            request.put("cmd", "manage_devices_req");
            request.put("token", webSocketDataCollector.getToken());
            request.set("devices_list", mapper.createArrayNode().add(deviceNode));

            String jsonString;
            try {
                jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
                updateTextInPane1("Отправляем команду: " + jsonString);
                webSocketDataCollector.sendOnce(jsonString, 99, true);
            } catch (JsonProcessingException exp) {
                throw new RuntimeException(exp);
            }

            dispose();
        }
    }

    private void restoreParameters() {
        log.info("Восстанвливаю параметры");
        this.login.setText(prop.getVegaLogin());
        this.passwordField.setText(prop.getVegaPassword());
        this.addressField.setText(prop.getVegaAddress());
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
        panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsPane = new JPanel();
        settingsPane.setLayout(new GridLayoutManager(5, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(settingsPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsPane.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        addressField = new JTextField();
        addressField.setText("ws://192.168.10.1:8002");
        panel2.add(addressField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsPane.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        login = new JTextField();
        login.setText("igm");
        panel3.add(login, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsPane.add(panel4, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        passwordField = new JPasswordField();
        passwordField.setText("");
        panel4.add(passwordField, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        settingsPane.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        getDataButton = new JButton();
        getDataButton.setText("GetData");
        panel5.add(getDataButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        connectButton = new JButton();
        connectButton.setText("Connect");
        panel5.add(connectButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        loginButton = new JButton();
        loginButton.setText("Login");
        panel5.add(loginButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel5.add(panel6, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        closeButton = new JButton();
        closeButton.setText("Close");
        panel6.add(closeButton, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        settingsPane.add(panel7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(200, 200), new Dimension(200, 200), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        panel7.add(scrollPane1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textPane1 = new JTextPane();
        textPane1.setEditable(false);
        textPane1.setText("");
        scrollPane1.setViewportView(textPane1);
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return panel1;
    }

}
