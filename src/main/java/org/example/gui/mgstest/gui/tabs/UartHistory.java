package org.example.gui.mgstest.gui.tabs;

import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.model.answer.MipexResponseModel;
import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.repository.DeviceStateRepository;
import org.example.gui.mgstest.service.DeviceAsyncExecutor;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.CradleController;
import org.example.gui.mgstest.transport.DeviceCommand;
import org.example.gui.mgstest.transport.cmd.mkrs.transfer.SendCommandMkrs;
import org.example.services.comPort.StringEndianList;
import org.example.gui.mgstest.transport.cmd.mgs.transfer.SendExternalUartCommand;
import org.example.gui.mgstest.transport.cmd.mgs.transfer.SendSpiCommand;
import org.example.gui.mgstest.transport.cmd.mgs.transfer.SendUartCommand;
import org.example.utilites.Constants;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class UartHistory extends DeviceTab {
    private Logger log = Logger.getLogger(UartHistory.class);
    @Setter
    private CradleController cradleController;
    @Setter
    private HidSupportedDevice selectedDevice;
    @Setter
    private DeviceState deviceState;
    private DeviceStateRepository stateRepository;
    private DeviceAsyncExecutor asyncExecutor;

    private JTextField commandField = new JTextField(30);
    private JButton sendButton = new JButton("Отправить");
    private JTextArea historyArea = new JTextArea();
    private String lastSentCommand = null;
    private long lastResponseTime = 0;

    // Выпадающие списки для настроек
    private JComboBox<StringEndianList> lineEndingComboBox;
    private JComboBox<TransportDirection> directionComboBox;

    // Enum для направлений отправки
    public enum TransportDirection {
        UART,
        SPI,
        EXTERNAL_UART
    }

    public UartHistory(CradleController cradleController, HidSupportedDevice selectedDevice, DeviceState deviceState,
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

        // Выбор окончания строки
        inputPanel.add(new JLabel("Окончание:"));
        lineEndingComboBox = new JComboBox<>(StringEndianList.values());
        lineEndingComboBox.setSelectedItem(StringEndianList.CR_LF);
        inputPanel.add(lineEndingComboBox);

        // Выбор направления отправки
        inputPanel.add(new JLabel("Направление:"));
        directionComboBox = new JComboBox<>(TransportDirection.values());
        directionComboBox.setSelectedItem(TransportDirection.UART);
        inputPanel.add(directionComboBox);

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

        // Получаем выбранные значения из выпадающих списков
        org.example.services.comPort.StringEndianList ending = (org.example.services.comPort.StringEndianList) lineEndingComboBox.getSelectedItem();
        TransportDirection direction = (TransportDirection) directionComboBox.getSelectedItem();

        if(selectedDevice.getDeviceType() == Constants.SupportedHidDeviceType.MULTIGASSENSE) {
            // Создаем соответствующую команду в зависимости от направления
            CommandParameters param = new CommandParameters();
            param.setStringArgument(command);
            param.setEndian(ending);
            DeviceCommand sender = createCommandForDirection(direction);

            // Выполняем команду
            asyncExecutor.executeCommand(sender, param, selectedDevice);
        }else if(selectedDevice.getDeviceType() == Constants.SupportedHidDeviceType.MIKROSENSE) {
            CommandParameters param = new CommandParameters();
            param.setStringArgument(command);
            param.setEndian(ending);
            if(direction == TransportDirection.UART){
                param.setIntArgument(0x01);
            }else if(direction == TransportDirection.EXTERNAL_UART){
                param.setIntArgument(0x02);
            }else if(direction == TransportDirection.SPI){
                JOptionPane.showMessageDialog(null, "Не доступно для данного устройства");
                return;
            }
            DeviceCommand sender = new SendCommandMkrs();

            // Выполняем команду
            asyncExecutor.executeCommand(sender, param, selectedDevice);
        }


    }

    private DeviceCommand createCommandForDirection(TransportDirection direction) {
        switch (direction) {
            case SPI:
                return new SendSpiCommand();
            case EXTERNAL_UART:
                return new SendExternalUartCommand();
            case UART:
            default:
                return new SendUartCommand();
        }
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