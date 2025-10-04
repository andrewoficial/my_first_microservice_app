package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.MipexResponseModel;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.repository.DeviceStateRepository;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.CradleController;
import org.example.gui.mgstest.transport.commands.SendUartCommand;
import org.hid4java.HidDevice;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UartHistory extends DeviceTab {
    private Logger log = Logger.getLogger(UartHistory.class);
    @Setter
    private CradleController cradleController;
    @Setter
    private HidDevice selectedDevice;
    @Setter
    private DeviceState deviceState;
    private DeviceStateRepository stateRepository;
    private DeviceAsyncExecutor asyncExecutor;

    private JTextField commandField = new JTextField(30);
    private JButton sendButton = new JButton("Отправить");
    private JTextArea historyArea = new JTextArea();
    private String lastSentCommand = null;
    private long lastResponseTime = 0;

    public UartHistory(CradleController cradleController, HidDevice selectedDevice, DeviceState deviceState,
                       DeviceStateRepository stateRepository, DeviceAsyncExecutor asyncExecutor) {
        super("UART History");
        this.cradleController = cradleController;
        this.selectedDevice = selectedDevice;
        this.deviceState = deviceState;
        this.stateRepository = stateRepository;
        this.asyncExecutor = asyncExecutor;
        initComponents();
    }

    private void initComponents() {
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Верхняя панель с вводом и кнопкой
        JPanel inputPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        inputPanel.add(new JLabel("Команда:"));
        inputPanel.add(commandField);
        inputPanel.add(sendButton);

        panel.add(inputPanel, BorderLayout.NORTH);

        // Область истории
        historyArea.setEditable(false);
        historyArea.setLineWrap(true);
        historyArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(historyArea);
        panel.add(scrollPane, BorderLayout.CENTER);

        // Обработчик кнопки
        sendButton.addActionListener(e -> sendCommand());
    }

    private void sendCommand() {
        if (selectedDevice == null) {
            JOptionPane.showMessageDialog(null, "Устройство не подключено");
            return;
        }

        // Проверка занятости
        if (stateRepository.contains(selectedDevice) && stateRepository.get(selectedDevice).getIsBusy()) {
            JOptionPane.showMessageDialog(null, "Устройство занято");
            return;
        }

        String command = commandField.getText().trim();
        if (command.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Введите команду");
            return;
        }

        lastSentCommand = command;
        appendToHistory("Команда: " + command, "Ожидание ответа...");

        CommandParameters param = new CommandParameters();
        param.setStringArgument(command);
        SendUartCommand uartCommand = new SendUartCommand();
        asyncExecutor.executeCommand(uartCommand, param, selectedDevice);

        commandField.setText("");
    }

    private void appendToHistory(String sent, String received) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = sdf.format(new Date());

        historyArea.append("[" + timestamp + "]\n");
        historyArea.append(sent + "\n");
        historyArea.append(received + "\n\n");

        // Прокрутка вниз
        historyArea.setCaretPosition(historyArea.getDocument().getLength());
    }

    @Override
    public void updateData(DeviceState state) {
        if (state == null) return;

        MipexResponseModel lastResponse = state.getLastMipexResponse();
        if (lastResponse != null && lastResponse.time > lastResponseTime && lastSentCommand != null) {
            appendToHistory("Ответ на: " + lastSentCommand, lastResponse.text);
            lastResponseTime = lastResponse.time;
            lastSentCommand = null;
        }
    }

    @Override
    public void saveData(DeviceState state) {
        // Сохранение не требуется
    }
}