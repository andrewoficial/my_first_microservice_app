package org.example.gui;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WebSocketWindow extends JDialog implements Rendeble {
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

    public WebSocketWindow() {
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(panel1);

        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Путь к WebSocket
                String url = "ws://127.0.0.1:8002";
                if (addressField.getText() != null) {
                    url = addressField.getText();
                }
                connectWebSocket(url);
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

                String jsonString;
                try {
                    jsonString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
                    updateTextInPane1("Отправляем команду: " + jsonString);
                    sendMessage(jsonString);  // Отправляем команду
                } catch (JsonProcessingException exp) {
                    throw new RuntimeException(exp);
                }
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (webSocketSession != null && webSocketSession.isOpen()) {
                        webSocketSession.close();  // Закрываем соединение
                        updateTextInPane1("Соединение закрыто");
                    } else {
                        updateTextInPane1("Нет активных соединений");
                    }
                } catch (IOException ioException) {
                    updateTextInPane1("Ошибка при закрытии соединения к сокету" + ioException.getMessage());
                }
            }
        });

        // Создаем таблицы
        createTables();
        renderData();
    }

    public String getCurrentToken() {
        return currentToken;
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
        devicesTableModel.addColumn("DevEUI");
        devicesTableModel.addColumn("Имя");
        devicesTableModel.addColumn("Последние данные");
        devicesTableModel.addColumn("Класс");
        devicesTable = new JTable(devicesTableModel);

        // Панель с кнопкой добавления
        JPanel devicesPanel = new JPanel(new BorderLayout());
        devicesPanel.add(new JScrollPane(devicesTable), BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        addDeviceButton = new JButton("Добавить устройство");
        buttonPanel.add(devicesTable);
        buttonPanel.add(addDeviceButton);
        devicesPanel.add(buttonPanel, BorderLayout.SOUTH);
        // Панель с вкладками
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Параметры", new JScrollPane(settingsPane));
        tabbedPane.addTab("Шлюзы", new JScrollPane(gatewaysTable));
        tabbedPane.addTab("Устройства", new JScrollPane(buttonPanel));




        add(tabbedPane, BorderLayout.CENTER);

        // Обработчик кнопки
        addDeviceButton.addActionListener(e -> showAddDeviceDialog());
    }

    public void sendJsonRequest(ObjectNode request) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(request);
            sendMessage(json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void showAddDeviceDialog() {
        new AddDeviceDialog(this).setVisible(true);
    }

    private void connectWebSocket(String url) {
        try {
            WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
            webSocketSession = client.doHandshake(new MyTextWebSocketHandler(), headers, URI.create(url)).get();
            updateTextInPane1("Подключение установлено");
        } catch (Exception e) {
            updateTextInPane1("Ошибка при подключении к сокету" + e.getMessage());
            //e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            if (webSocketSession != null && webSocketSession.isOpen()) {
                webSocketSession.sendMessage(new TextMessage(message));
            } else {
                updateTextInPane1("Соединение не установлено!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class MyTextWebSocketHandler extends TextWebSocketHandler {
        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
            // Получаем ответ от сервера
            String payload = message.getPayload();
            updateTextInPane1("Ответ от сервера: " + payload);
            SwingUtilities.invokeLater(() -> {
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode rootNode = mapper.readTree(payload);
                    String cmd = rootNode.get("cmd").asText();

                    if ("auth_resp".equals(cmd) && rootNode.get("status").asBoolean()) {
                        currentToken = rootNode.get("token").asText();
                        sendGetGatewaysRequest();
                        sendGetDevicesRequest();
                    } else if ("get_gateways_resp".equals(cmd)) {
                        processGatewaysResponse(rootNode);
                    } else if ("get_devices_resp".equals(cmd)) {
                        processDevicesResponse(rootNode);
                    } else if ("manage_devices_resp".equals(cmd)) {
                        if (rootNode.get("status").asBoolean()) {
                            sendGetDevicesRequest(); // Обновляем список после добавления
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }

        private void sendGetGatewaysRequest() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode request = mapper.createObjectNode();
            request.put("cmd", "get_gateways_req");
            if (currentToken != null) {
                request.put("token", currentToken);
            }
            sendJsonRequest(request);
        }

        private void sendGetDevicesRequest() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode request = mapper.createObjectNode();
            request.put("cmd", "get_devices_req");
            if (currentToken != null) {
                request.put("token", currentToken);
            }
            sendJsonRequest(request);
        }


    }


    private void processGatewaysResponse(JsonNode rootNode) {
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
                String lastData = dev.has("last_data_ts") ?
                        formatTimestamp(dev.get("last_data_ts").asLong()) : "N/A";
                String deviceClass = dev.get("class").asText();

                devicesTableModel.addRow(new Object[]{devEui, name, lastData, deviceClass});
            }
        }
    }

    private String formatTimestamp(long timestamp) {
        return new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date(timestamp));
    }

    @Override
    public void renderData() {
        // Обновляем таблицы при необходимости

        if (currentToken != null && (!currentToken.isEmpty())) {
            gatewaysTable.repaint();
            devicesTable.repaint();
        }

    }

    @Override
    public boolean isEnable() {
        return false;
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

    class AddDeviceDialog extends JDialog {
        private JTextField devEuiField;
        private JTextField otaaAppEuiField;
        private JTextField otaaAppKeyField;
        private JTextField abpDevAddressField;
        private JTextField abpAppsKeyField;
        private JTextField abpNwksKeyField;
        private JTabbedPane tabbedPane;

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

            add(mainPanel);
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
            request.put("token", currentToken);
            request.set("devices_list", mapper.createArrayNode().add(deviceNode));

            // Отправляем запрос
            sendJsonRequest(request);
            dispose();
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
