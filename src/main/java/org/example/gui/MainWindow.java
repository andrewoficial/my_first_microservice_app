package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import org.apache.log4j.Logger;
import org.example.Main;
import org.example.device.ProtocolsList;
import org.example.device.SomeDevice;
import org.example.device.TemplatedAscii;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.SingleCommand;
import org.example.gui.components.*;
import org.example.gui.mainWindowUtilites.FolderPictureForLog;
import org.example.gui.mainWindowUtilites.GuiStateManager;
import org.example.gui.mainWindowUtilites.PortManager;
import org.example.gui.mainWindowUtilites.TabManager;
import org.example.services.AnswerStorage;
import org.example.services.connectionPool.AnyPoolService;
import org.example.services.TabAnswerPart;
import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.ParityList;
import org.example.services.comPort.StopBitsList;
import org.example.services.loggers.DeviceLogger;
import org.example.utilites.*;
import org.example.services.connectionPool.ComDataCollector;
import org.example.utilites.properties.MyProperties;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.ConnectException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;

import static org.example.utilites.MyUtilities.createDeviceByProtocol;


public class MainWindow extends JFrame implements Rendeble {
    private final static Logger log = Logger.getLogger(MainWindow.class);//Внешний логгер
    private ExecutorService uiThPool = Executors.newCachedThreadPool(); //Поток GUI
    private MainLeftPanelStateCollection leftPanState; // Класс, хранящий состояние клиентов (настройки в памяти)
    private MyProperties prop; //Файл настроек
    private AnyPoolService anyPoolService; //Сервис опросов (разных протоколов)
    private final GuiStateManager guiStateManager;
    private final TabManager tabManager;
    private final PortManager portManager;

    private final ConcurrentHashMap<Integer, Integer> lastReceivedPositionFromStorageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, JTextPane> logDataTransferJtextPanelsMap = new ConcurrentHashMap<>();

    private final int DEFAULT_POOL_DELAY = 3000;
    private final AtomicInteger currentTabCount = new AtomicInteger();
    private final AtomicInteger currentActiveTab = new AtomicInteger(); //Текущая активная (выбранная) вкладка
    private final AtomicInteger currentActiveClientId = new AtomicInteger();
    private boolean initialCalls = true;

    private JPanel jpMainPanel;
    private JPanel jpAddRemove;
    private JPanel jpComPortSetup;
    private JPanel jpTerminalHistory;
    private JPanel jpConnectionType;
    private JPanel jpFolderIconPanel;
    private JPanel jpNonAsciCommandListPanel;
    private JPanel jpSendInput;
    private JPanel jpAsciiInput;
    private JPanel jpTerminalLogPanel;

    private JTabbedPane jtpDevicesTerminal;
    private JComboBox<String> jcbComPorts;
    private JComboBox<String> jcbBaudRate;
    private JComboBox<Integer> jcbDataBits;
    private JComboBox<String> jcbParity;
    private JComboBox<Integer> jcbStopBit;
    private JComboBox<String> jcbProtocol;
    private JComboBox<String> jcbConnectionType;

    private JButton jbComOpen;
    private JButton jbComClose;
    private JButton jbComUpdateList;
    private JButton jbTerminalSend;
    private JButton jbAddDev;
    private JButton jbRemoveDev;
    private JButton jbComSearch;

    private JCheckBox jCbNeedPool;
    private JCheckBox jCbNeedLog;
    private JCheckBox jCbAutoConnect;

    private JTextField jtfPoolDelay;
    private JTextField jtfTextToSend;
    private JTextField jtfPrefToSend;

    private JLabel jlbComPorts;
    private JLabel jlbComDataBits;
    private JLabel jlbComParity;
    private JLabel jlbComStopBit;
    private JLabel jlbComBaudRate;
    private JLabel jlbComProtocol;
    private JLabel jlbConnectionType;
    private JPanel jpConnectionSettings;
    private JPanel jpDataBits;
    private JPanel jpParity;
    private JPanel jpComStopBit;
    private JPanel jpOpenClose;


    private void initUI() {
        assert prop != null;


        setTitle(prop.getTitle() + " v" + prop.getVersion());
        URL resource = Main.class.getClassLoader().getResource("GUI_Images/Pic.png");
        if (resource != null) {
            ImageIcon pic = new ImageIcon(resource);
            this.setIconImage(pic.getImage());
            log.debug("Установка картинки");
        }
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        super.setVisible(true);
        super.pack();
    }

    private void createMenu() {
        assert prop != null;
        //log.info("prop driver " + prop.getDrv());
        JMenuBar menuBar = new JMenuBar();
        JmenuFile menu = new JmenuFile(prop, anyPoolService);
        menuBar.add(menu.createFileMenu());
        menuBar.add(menu.createSettingsMenu());
        menuBar.add(menu.createViewMenu(uiThPool));
        menuBar.add(menu.createUtilitiesMenu(uiThPool));
        menuBar.add(menu.createSystemParametrs(uiThPool));
        menuBar.add(menu.createInfo(uiThPool));
        setJMenuBar(menuBar);
        setContentPane(jpMainPanel);
        log.debug("Инициализация панели и меню завершена");
    }

    private int restoreParameters() {
        //log.info("Восстанвливаю параметры");
        MainLeftPanelStateCollection restoredFromFile = prop.getLeftPanelStateCollection();
        if (restoredFromFile == null) {//Если в настройках ничего нет
            log.warn(" В настрйоках нету состояний. Создаю с нуля");
            this.prop = MyProperties.getInstance();
            checkAndCreateGuiStateClass();
            restoredFromFile = leftPanState;
        }
        leftPanState = restoredFromFile;
        return leftPanState.getClientIdByTabNumber(0);
    }

    private <T extends Enum<?>, U> void initComboBox(JComboBox<U> comboBox, T[] values, IntFunction<U> valueExtractor, IntSupplier currentIndexSupplier) {
        for (int i = 0; i < values.length; i++) {
            comboBox.addItem(valueExtractor.apply(i));
            if (i == currentIndexSupplier.getAsInt()) {
                comboBox.setSelectedIndex(i);
            }
        }
    }


    public MainWindow(MyProperties myProperties, AnyPoolService anyPoolService, MainLeftPanelStateCollection leftPanelStateCollection) {
        NimbusCustomizer.customize();
        $$$setupUI$$$();
        log.debug("Подготовка к рендеру окна....");
        if (anyPoolService == null || myProperties == null) {
            log.warn("В конструктор MainWindow передан null anyPoolService/comPorts/myProperties");
        }

        this.anyPoolService = anyPoolService;
        this.prop = myProperties;

        createMenu();
        currentActiveClientId.set(restoreParameters());

        //Заполнение полей комбо-боксов
        initComboBox(jcbBaudRate, BaudRatesList.values(),
                i -> String.valueOf(BaudRatesList.getNameLikeArray(i)),
                () -> leftPanState.getBaudRate(currentActiveClientId.get()));

        initComboBox(jcbParity, ParityList.values(),
                i -> String.valueOf(ParityList.getNameLikeArray(i)),
                () -> leftPanState.getParityBits(currentActiveClientId.get()));

        initComboBox(jcbProtocol, ProtocolsList.values(),
                i -> ProtocolsList.getLikeArrayEnum(i).getValue(),
                () -> leftPanState.getProtocol(currentActiveClientId.get()));

        initComboBox(jcbStopBit, StopBitsList.values(),
                StopBitsList::getNameLikeArray,
                () -> leftPanState.getStopBits(currentActiveClientId.get()));

        initComboBox(jcbDataBits, DataBitsList.values(),
                DataBitsList::getNameLikeArray,
                () -> leftPanState.getDataBits(currentActiveClientId.get()));


        guiStateManager = new GuiStateManager(leftPanState, currentActiveClientId, currentActiveTab);
        tabManager = new TabManager(jtpDevicesTerminal, leftPanState, logDataTransferJtextPanelsMap, lastReceivedPositionFromStorageMap, jbRemoveDev);
        portManager = new PortManager(anyPoolService, prop, guiStateManager, leftPanState);
        updateComPortList();

        log.debug("Добавление слушателей действий");

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                saveParameters();
                log.info("Выход из программы при активной вкладке:" + currentActiveTab);
                anyPoolService.shutDownComDataCollectorThreadPool();
                uiThPool.shutdownNow();
                System.exit(0);
            }
        });

        jtpDevicesTerminal.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                currentActiveTab.set(jtpDevicesTerminal.getSelectedIndex());
                //log.info("Фокус установлен на вкладку " + currentActiveTab); //активна вкладка, выбрана вкладка
                currentActiveClientId.set(leftPanState.getClientIdByTabNumber(currentActiveTab.get()));
                if (currentActiveClientId.get() == -1) {
                    currentActiveClientId.set(checkAndCreateGuiStateClass());
                }
                jCbNeedPool.setSelected(anyPoolService.isComDataCollectorByClientIdActiveDataSurvey(currentActiveClientId.get()));
                jCbNeedLog.setSelected(anyPoolService.isComDataCollectorByClientIdLogged(currentActiveClientId.get()));


                updateGuiFromClass();
                updateComPortSelectorFromProp();
                checkIsUsedPort();
                updateFolderPicture();
            }
        });

        currentActiveTab.set(0);
        currentActiveClientId.set(leftPanState.getClientIdByTabNumber(currentActiveTab.get()));
        if (currentActiveClientId.get() == -1) {
            currentActiveClientId.set(checkAndCreateGuiStateClass());
        }
        lastReceivedPositionFromStorageMap.put(currentActiveClientId.get(), 0);

        int tabCount = Math.max(0, prop.getTabCounter());
        for (int i = 0; i < tabCount; i++) {
            addTab();
        }


        //this.renderData();


        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                anyPoolService.shutDownComDataCollectorThreadPool();
                e.getWindow().dispose();
            }
        });

        initUI();
        initDocumentListeners();
        //updateGuiFromClass();
        updateClassFromGui();
        jtpDevicesTerminal.setSelectedIndex(0);
        uiThPool.submit(new RenderThread(this));
        initialCalls = false;
    }


    private void updateComPortList() {

        jcbComPorts.removeAllItems();
        for (int i = 0; i < SerialPort.getCommPorts().length; i++) {
            SerialPort currentPort = SerialPort.getCommPorts()[i];
            jcbComPorts.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (currentPort.getSystemPortName().equals(prop.getPorts()[0])) {
                jcbComPorts.setSelectedIndex(i);
            }
        }
    }

    // Инициализация слушателей
    private void initDocumentListeners() {
        // Text Fields
        ListenerUtils.addDocumentListener(jtfPoolDelay, this::updatePoolDelay);
        ListenerUtils.addDocumentListener(jtfPrefToSend, this::updateTextToSend);
        ListenerUtils.addDocumentListener(jtfTextToSend, this::updateTextToSend);

        // Key Listeners
        ListenerUtils.addKeyListener(jtfTextToSend, this::updateTextAndSendFromEnter);
        ListenerUtils.addKeyListener(jtfPrefToSend, this::updateTextAndSendFromEnter);

        // Combo Boxes
        ListenerUtils.addComboBoxListener(jcbComPorts, this::checkIsUsedPort);
        ListenerUtils.addComboBoxListener(jcbParity, this::updateParity);
        ListenerUtils.addComboBoxListener(jcbProtocol, this::updateProtocol);
        ListenerUtils.addComboBoxListener(jcbBaudRate, this::updateBaudRate);
        ListenerUtils.addComboBoxListener(jcbStopBit, this::updateStopBit);
        ListenerUtils.addComboBoxListener(jcbDataBits, this::updateDataBits);

        // Buttons
        ListenerUtils.addActionListener(jbComOpen, this::openComPort);
        ListenerUtils.addActionListener(jbComClose, this::closeComPort);
        ListenerUtils.addActionListener(jbTerminalSend, this::updateTextAndSendFromEnter);
        ListenerUtils.addActionListener(jbAddDev, this::addTab);
        ListenerUtils.addActionListener(jbRemoveDev, this::removeTab);
        ListenerUtils.addActionListener(jbComUpdateList, this::updateComPortList);

        // CheckBoxes
        ListenerUtils.addActionListener(jCbNeedPool, this::updateTextAndSendFromCheckBox);
        ListenerUtils.addActionListener(jCbNeedLog, this::updateLogCheckBox);
    }

    private void removeTab() {
        tabManager.removeTab();
        currentTabCount.set(jtpDevicesTerminal.getTabCount());
    }

    private void addTab() {
        tabManager.addTab();
        currentTabCount.set(jtpDevicesTerminal.getTabCount());
    }


    private void openComPort() {
        updateClassFromGui();
        log.info(leftPanState.getBaudRateValue(currentActiveClientId.get()));


        addCustomMessage(portManager.openPort(currentActiveClientId.get(), getCurrComSelection(), getCurrProtocolSelection()));
        checkIsUsedPort(); //Выставляет блокировки кнопки открыть/закрыть
        if (anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get()) != null) {
            prop.setPortForTab(anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get()).getComPort().getSystemPortName(), currentActiveTab.get());

        }
        saveParameters();
    }

    private void closeComPort() {
        addCustomMessage(portManager.closePort(currentActiveClientId.get()));
        checkIsUsedPort(); //Блокировка кнопок
        updateFolderPicture();
    }


    private void updateParity() {
        leftPanState.setParityBits(currentActiveClientId.get(), jcbParity.getSelectedIndex());
        leftPanState.setParityBitsValue(currentActiveClientId.get(), jcbParity.getSelectedIndex());
    }

    private void updateBaudRate() {
        leftPanState.setBaudRate(currentActiveClientId.get(), jcbBaudRate.getSelectedIndex());
        leftPanState.setBaudRateValue(currentActiveClientId.get(), BaudRatesList.getNameLikeArray(jcbBaudRate.getSelectedIndex()));
    }

    private void updateStopBit() {
        leftPanState.setStopBits(currentActiveClientId.get(), jcbStopBit.getSelectedIndex());
        leftPanState.setStopBitsValue(currentActiveClientId.get(), StopBitsList.getNameLikeArray(jcbStopBit.getSelectedIndex()));
    }

    private void updateDataBits() {
        leftPanState.setDataBits(currentActiveClientId.get(), jcbDataBits.getSelectedIndex());
        leftPanState.setDataBitsValue(currentActiveClientId.get(), DataBitsList.getNameLikeArray(jcbDataBits.getSelectedIndex()));
    }

    private void updateProtocol() {
        leftPanState.setProtocol(currentActiveClientId.get(), jcbProtocol.getSelectedIndex());
        // Устанавливаем BorderLayout для основной панели
        jpSendInput.setLayout(new BorderLayout());

        if (ProtocolsList.getLikeArrayEnum(jcbProtocol.getSelectedIndex()) != null) {
            jbComSearch.setEnabled(ProtocolsList.getLikeArrayEnum(jcbProtocol.getSelectedIndex()) == ProtocolsList.ERSTEVAK_MTP4D);
            SomeDevice device = createDeviceByProtocol(ProtocolsList.getLikeArrayEnum(jcbProtocol.getSelectedIndex()));

            if (device instanceof TemplatedAscii) {
                showTemplatedAsciiPanel(device);
            } else if (!device.isASCII()) {
                showNonAsciiPanel(device);
            } else {
                showAsciiPanel();
            }
        }
    }

    private void showExtendetAsciiPanel(SomeDevice device) {
        // Убираем все компоненты с sendInpuntJpane
        jpSendInput.removeAll();

        // Настраиваем nonAsciCommandListPanel
        jpNonAsciCommandListPanel.setLayout(new BorderLayout());

        HashMap<String, SingleCommand> commandList = device.getCommandListClass().getCommandPool();

        // Основная панель с вертикальным расположением
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        for (Map.Entry<String, SingleCommand> entry : commandList.entrySet()) {
            log.info("Создаю панель для " + entry.getKey());

            // Панель для отдельной команды
            JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            commandPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel commandLabel = new JLabel(entry.getValue().getDescription());
            commandPanel.add(commandLabel);

            List<ArgumentDescriptor> arguments = entry.getValue().getArguments();
            Map<String, JTextField> argsInput = new ConcurrentHashMap<>();
            Map<String, Object> argsValue = new ConcurrentHashMap<>();
            for (ArgumentDescriptor arg : arguments) {

                log.info("  Для " + entry.getKey() + " добавил поле ввода для " + arg.getName());

                JPanel argPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                JLabel argLabel = new JLabel(arg.getName());
                JTextField textField = new JTextField(arg.getDefaultValue().toString(), 10);
                argsInput.put(arg.getName(), textField);
                argsValue.put(arg.getName(), 0.0f);
                argPanel.add(argLabel);
                argPanel.add(textField);
                commandPanel.add(argPanel);
            }

            JButton sendButton = new JButton("задать");
            sendButton.setPreferredSize(new Dimension(120, 25));
            sendButton.addActionListener(e -> {
                for (Map.Entry<String, Object> stringObjectEntry : argsValue.entrySet()) {
                    Float argument = 0.0f;
                    try {
                        argument = Float.parseFloat(argsInput.get(stringObjectEntry.getKey()).getText());
                        argsValue.put(stringObjectEntry.getKey(), argument);
                    } catch (NumberFormatException ex) {
                        log.warn("Error when parse argument " + ex.getMessage());
                        return;
                    }
                }


                byte[] cmdForSend = entry.getValue().build(argsValue);
                log.info("Set command in input field to " + MyUtilities.bytesToHex(cmdForSend));
                jtfTextToSend.setText(MyUtilities.byteArrayToString(cmdForSend));
                leftPanState.setRawCommand(currentActiveClientId.get(), cmdForSend);
                log.info("Saved  " + MyUtilities.bytesToHex(jtfTextToSend.getText().getBytes()));
            });

            commandPanel.add(sendButton);
            mainPanel.add(commandPanel);
            mainPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
        }

        // Добавляем скроллинг
        JScrollPane scrollPane = new JScrollPane(mainPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.getVerticalScrollBar().setUI(new CustomScrollBarUI());
        scrollPane.setPreferredSize(new Dimension(jpSendInput.getWidth(), 100));

        jpNonAsciCommandListPanel.removeAll();
        jpNonAsciCommandListPanel.add(scrollPane, BorderLayout.CENTER);

        // Добавляем nonAsciCommandListPanel и asciiInput в sendInpuntJpane
        jpSendInput.add(jpNonAsciCommandListPanel, BorderLayout.CENTER);
        jpSendInput.add(jpAsciiInput, BorderLayout.SOUTH);

        // Устанавливаем размеры
        jpNonAsciCommandListPanel.setPreferredSize(new Dimension(jpSendInput.getWidth(), 100));
        jpAsciiInput.setPreferredSize(new Dimension(jpSendInput.getWidth(), 50));

        jpSendInput.revalidate();
        jpSendInput.repaint();
    }

    private void showTemplatedAsciiPanel(SomeDevice device) {
        jtfTextToSend.setEnabled(true);
        jtfPrefToSend.setEnabled(true);

        showExtendetAsciiPanel(device);
    }

    private void showNonAsciiPanel(SomeDevice device) {
        jtfTextToSend.setEnabled(false);
        jtfPrefToSend.setEnabled(false);

        showExtendetAsciiPanel(device);
    }

    private void showAsciiPanel() {
        jtfTextToSend.setEnabled(true);
        jtfPrefToSend.setEnabled(true);

        // Убираем все компоненты с sendInpuntJpane
        jpSendInput.removeAll();

        // Добавляем только asciiInput
        jpSendInput.add(jpAsciiInput, BorderLayout.CENTER);
        jpAsciiInput.setPreferredSize(new Dimension(jpSendInput.getWidth(), 50));

        // Очищаем и скрываем nonAsciCommandListPanel
        jpNonAsciCommandListPanel.removeAll();
        jpNonAsciCommandListPanel.setPreferredSize(new Dimension(0, 0));

        jpSendInput.revalidate();
        jpSendInput.repaint();
    }

    // Обработчики действий
    private void updatePoolDelay() {
        log.info("Инициировано обновление периода опроса для вкладки {" + currentActiveTab + "}");
        Optional.ofNullable(getComDataCollectorSafe())
                .ifPresent(collector -> {
                    collector.setPoolDelay(getPoolDelayFromGui());
                    log.info("Выполнено обновление периода опроса для вкладки {" + currentActiveTab + "}");
                });
    }

    private void updateTextAndSendFromEnter() {
        updateTextToSend();
        startSend(true);

        if (uiThPool.isShutdown() || uiThPool.isTerminated()) {
            log.info("Перезапуск пула потоков рендера...");
            uiThPool = Executors.newSingleThreadExecutor(); // Создаём новый пул
            uiThPool.submit(new RenderThread(this));
        }


    }

    private void updateTextAndSendFromCheckBox() {
        //if ()
        updateTextToSend();
        startSend(false);
        renderData();
    }


    private void updateTextToSend() {
        if (isValidTab()) {
            updateClassFromGui();
            readAndUpdateInputPrefAndCommandValues();
        }
    }

    private void updateLogCheckBox() {
        ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get());
        if (ps != null) {
            ps.setNeedLog(jCbNeedLog.isSelected(), currentActiveClientId.get());
        } else {
            log.info("Для текущей влкадки потока опроса не существует");
        }
        updateFolderPicture();
    }

    // Вспомогательные методы
    private ComDataCollector getComDataCollectorSafe() {
        List<ComDataCollector> collectors = anyPoolService.getComDataCollectors();
        return collectors.size() > currentActiveTab.get() ? collectors.get(currentActiveTab.get()) : null;
    }

    private boolean isValidTab() {
        return currentActiveTab.get() <= leftPanState.getSize() && currentActiveTab.get() >= 0;
    }


    private int getPoolDelayFromGui() {
        try {
            return Integer.parseInt(jtfPoolDelay.getText());
        } catch (NumberFormatException e) {
            jtfPoolDelay.setText(String.valueOf(DEFAULT_POOL_DELAY));
            return DEFAULT_POOL_DELAY;
        }
    }

    public void startSend(boolean isBtn) {
        saveParameters();
        try {
            //isBtn - вызов по кнопке / pool - вызов про чекбоксу
            anyPoolService.createOrUpdateComDataCollector(leftPanState, currentActiveClientId.get(), getCurrComSelection(), getCurrProtocolSelection(),
                    getNeedPoolState(), isBtn, getCurrPoolDelay());
            updateFolderPictureMethod();
        } catch (ConnectException e) {
            addCustomMessage(" Ошибка начала отправки " + e.getMessage());
        }
    }

    private void updateComPortSelectorFromProp() {
        if (prop != null && prop.getPorts() != null && prop.getPorts().length > currentActiveTab.get() && prop.getPorts()[currentActiveTab.get()] != null) {
            int portNumber = -1;
            if (anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get()) != null) {
                portNumber = anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get()).getComPortForJCombo();
            } else if (leftPanState.getComPortComboNumber(currentActiveClientId.get()) != 0) {
                portNumber = leftPanState.getComPortComboNumber(currentActiveClientId.get());
            } else {
                if (prop != null && prop.getPorts() != null && prop.getPorts().length > currentActiveTab.get() && prop.getPorts()[currentActiveTab.get()] != null) {
                    portNumber = anyPoolService.searchComPortNumberByName(prop.getPorts()[currentActiveTab.get()]);
                }
            }
            jcbComPorts.setSelectedIndex(portNumber);
        }
    }


    private int checkAndCreateGuiStateClass() {
        int clientId = leftPanState.getClientIdByTabNumber(currentActiveTab.get());
        if (clientId == -1) {
            log.warn(" Для вкладки " + currentActiveTab + " clientId получился " + clientId + " создаю объект состояния");
            clientId = leftPanState.getNewRandomId();
            leftPanState.addPairClientIdTabNumber(clientId, currentActiveTab.get());
            MainLeftPanelState state = new MainLeftPanelState();
            state.setTabNumber(currentActiveTab.get());
            state.setClientId(clientId);
            leftPanState.addOrUpdateIdState(clientId, state);
            return clientId;
        }
        return clientId;
    }

    private void updateGuiFromClass() {
        guiStateManager.updateGuiFromModel(
                jcbDataBits, jcbParity, jcbStopBit,
                jcbBaudRate, jcbProtocol, jtfTextToSend,
                jtfPrefToSend
        );
    }


    private void updateClassFromGui() {
        guiStateManager.updateModelFromGui(
                jcbParity, jcbDataBits, jcbStopBit,
                jcbBaudRate, jcbProtocol, jtfTextToSend,
                jtfPrefToSend
        );
    }

    private void readAndUpdateInputPrefAndCommandValues() {
        log.info("Изменение в поле ввода префикса или команды");
        if (leftPanState.containClientId(currentActiveTab.get())) {
            ComDataCollector ps = anyPoolService.getComDataCollectors().get(currentActiveTab.get());
            if (ps != null)
                anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get()).setTextToSendString(leftPanState.getPrefix(currentActiveClientId.get()), leftPanState.getCommand(currentActiveClientId.get()), currentActiveClientId.get());
            saveParameters();

        }
    }

    private void checkIsUsedPort() {
        anyPoolService.closeUnusedComConnection(currentTabCount.get());
        boolean alreadyOpen = anyPoolService.isComPortInUse(getCurrComSelection());
        jCbNeedPool.setEnabled(alreadyOpen);
        jbTerminalSend.setEnabled(alreadyOpen);
        jbComOpen.setEnabled(!alreadyOpen);
        jcbProtocol.setEnabled(!alreadyOpen);
        jbComClose.setEnabled(alreadyOpen);

        int rootTab = anyPoolService.getRootTabForComConnection(getCurrComSelection());
        if (rootTab >= -1)
            return;
        if (rootTab != currentActiveTab.get() && alreadyOpen) {
            addCustomMessage("Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "' ");
            log.info("Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "' Просматриваемая вкладка " + currentActiveTab);
            jbComClose.setEnabled(false);
            jbComOpen.setEnabled(false);
            jcbProtocol.setEnabled(false);
        }
        updateFolderPictureMethod();
    }

    private int getCurrComSelection() {
        return jcbComPorts.getSelectedIndex();
    }

    private int getCurrProtocolSelection() {
        return jcbProtocol.getSelectedIndex();
    }

    private boolean getNeedPoolState() {
        return jCbNeedPool.isSelected();
    }

    private int getCurrPoolDelay() {
        int poolDelay = 10000;
        try {
            poolDelay = Integer.parseInt(jtfPoolDelay.getText());
        } catch (Exception e1) {
            jtfPoolDelay.setText("10000");
        }
        return poolDelay;
    }

    //Добавляет сообщение в терминал GUI на выбранную вкладку. Не сохраняется в истории.
    public void addCustomMessage(String str) {
        Document doc = logDataTransferJtextPanelsMap.get(currentActiveClientId.get()).getDocument();
        str = MyUtilities.CUSTOM_FORMATTER.format(LocalDateTime.now()) + ":\t" + str + "\n";
        try {
            doc.insertString(doc.getLength(), str, null);
        } catch (BadLocationException ex) {
            //throw new RuntimeException(ex);
        }
        logDataTransferJtextPanelsMap.get(currentActiveClientId.get()).setCaretPosition(doc.getLength());
    }

    public void updateFolderPictureLater() {
        Runnable setTextRun = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1500);
                    updateFolderPictureMethod();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        SwingUtilities.invokeLater(setTextRun);
    }

    public void updateFolderPicture() {
        if (!initialCalls) {
            updateFolderPictureLater();
        }
    }

    public void updateFolderPictureMethod() {
        ComDataCollector cdc = anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get());
        FolderPictureForLog fpg = new FolderPictureForLog();
        jpFolderIconPanel.setLayout(new BoxLayout(jpFolderIconPanel, BoxLayout.Y_AXIS));
        if (cdc != null) {
            DeviceLogger currentLogger = cdc.getLogger(currentActiveClientId.get());
            if (currentLogger != null) {
                log.info("Set folder state open");
                jpFolderIconPanel.removeAll();
                JPanel btnPane = fpg.getPicContainer("Open", true, true, currentLogger.getLogFileCSV());
                jpFolderIconPanel.add(btnPane);
                jpFolderIconPanel.revalidate();
                jpFolderIconPanel.repaint();
            } else {
                log.info("Not started");
                jpFolderIconPanel.removeAll();
                JPanel btnPane = fpg.getPicContainer("Not available", true, false, null);
                jpFolderIconPanel.add(btnPane);
                jpFolderIconPanel.revalidate();
                jpFolderIconPanel.repaint();
            }
        } else {
            log.info("Set folder state can not started");
            jpFolderIconPanel.removeAll();
            JPanel btnPane = fpg.getPicContainer("Not available", false, false, null);
            jpFolderIconPanel.add(btnPane);
            jpFolderIconPanel.revalidate();
            jpFolderIconPanel.repaint();
        }
    }

    public void renderData() {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::renderData);
            return;
        }
        Integer clientId = currentActiveClientId.get();
        Document doc = logDataTransferJtextPanelsMap.get(clientId).getDocument();
        final int maxLength = 10_000;

        try {
            int lastPosition = lastReceivedPositionFromStorageMap.getOrDefault(clientId, 0); //  ConcurrentHashMap<Integer, Integer>
            int queueOffsetInt = AnswerStorage.queueOffset.getOrDefault(clientId, 0);

            // Синхронизация доступа к позиции
            if (lastPosition < queueOffsetInt) {
                lastPosition = queueOffsetInt;
                lastReceivedPositionFromStorageMap.put(clientId, lastPosition);
            }

            TabAnswerPart an = AnswerStorage.getAnswersQueForTab(lastPosition, clientId, true);

            if (an.getAnswerPart() == null || an.getAnswerPart().isEmpty()) {
                //log.info("Нет новых данных для клиента [" + clientId + "]");
                return;
            }
            //log.info("Будут отображены новые данные для клиента [" + clientId + "], позиция: " + an.getPosition());

            // Обновляем позицию атомарно
            lastReceivedPositionFromStorageMap.put(clientId, an.getPosition());


            // Очистка и добавление новых данных
            if (doc.getLength() + an.getAnswerPart().length() > maxLength) {
                doc.remove(0, doc.getLength());
            }
            doc.insertString(doc.getLength(), an.getAnswerPart(), null);

            // Автоскролл к новому содержимому
            logDataTransferJtextPanelsMap.get(clientId).setCaretPosition(doc.getLength());

        } catch (BadLocationException ex) {
            log.warn("Произошло исключение при рендере окна: " + ex.getMessage());
        }
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    //Типа смена контекста
    public void updateServices(MyProperties newProps, AnyPoolService newService, MainLeftPanelStateCollection leftPanelStateCollection) {
        this.prop = newProps;
        this.anyPoolService = newService;
        this.leftPanState = leftPanelStateCollection;
    }

    private void saveParameters() {
        log.debug("Обновление файла настроек со вкладки" + currentActiveTab + " и ИД клиента " + currentActiveClientId.get());
        log.debug("Обновление файла настроек со вкладки" + currentActiveTab + " и ИД клиента " + currentActiveClientId.get());
        prop.setLastLeftPanel(leftPanState);
        prop.setLogLevel(Logger.getRootLogger().getLevel());
        prop.setTabCounter(currentTabCount.get());
        prop.setIdentAndTabBounding(AnswerStorage.getDeviceTabPair());
    }


    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        jpMainPanel = new JPanel();
        jpMainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(10, 10, 10, 10), -1, -1));
        Font jpMainPanelFont = UIManager.getFont("Tree.font");
        if (jpMainPanelFont != null) jpMainPanel.setFont(jpMainPanelFont);
        jpMainPanel.setMaximumSize(new Dimension(1200, 1200));
        jpMainPanel.setMinimumSize(new Dimension(530, 530));
        jpMainPanel.setPreferredSize(new Dimension(900, 700));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:max(d;4px):noGrow,left:4dlu:noGrow,fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        jpMainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(450, 400), new Dimension(700, 600), null, 0, false));
        jpTerminalHistory = new JPanel();
        jpTerminalHistory.setLayout(new BorderLayout(0, 0));
        CellConstraints cc = new CellConstraints();
        panel1.add(jpTerminalHistory, cc.xywh(3, 1, 1, 3, CellConstraints.FILL, CellConstraints.FILL));
        jpSendInput = new JPanel();
        jpSendInput.setLayout(new BorderLayout(0, 0));
        jpTerminalHistory.add(jpSendInput, BorderLayout.NORTH);
        jpNonAsciCommandListPanel = new JPanel();
        jpNonAsciCommandListPanel.setLayout(new BorderLayout(0, 0));
        jpSendInput.add(jpNonAsciCommandListPanel, BorderLayout.WEST);
        jpAsciiInput = new JPanel();
        jpAsciiInput.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpAsciiInput.setMinimumSize(new Dimension(450, 45));
        jpAsciiInput.setOpaque(true);
        jpAsciiInput.setPreferredSize(new Dimension(450, 45));
        jpSendInput.add(jpAsciiInput, BorderLayout.CENTER);
        jtfPrefToSend = new JTextField();
        jtfPrefToSend.setPreferredSize(new Dimension(60, 40));
        jtfPrefToSend.setText("");
        jpAsciiInput.add(jtfPrefToSend, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(10, 35), new Dimension(40, 35), new Dimension(80, 35), 0, false));
        jtfTextToSend = new JTextField();
        jtfTextToSend.setMargin(new Insets(2, 9, 2, 6));
        jtfTextToSend.setPreferredSize(new Dimension(100, 40));
        jtfTextToSend.setText("M^");
        jpAsciiInput.add(jtfTextToSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(25, 35), new Dimension(900, 35), new Dimension(-1, 35), 0, false));
        jbTerminalSend = new JButton();
        jbTerminalSend.setMargin(new Insets(5, 5, 5, 5));
        jbTerminalSend.setText("Отправить");
        jpAsciiInput.add(jbTerminalSend, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpTerminalLogPanel = new JPanel();
        jpTerminalLogPanel.setLayout(new BorderLayout(0, 0));
        jpTerminalHistory.add(jpTerminalLogPanel, BorderLayout.CENTER);
        jtpDevicesTerminal = new JTabbedPane();
        jpTerminalLogPanel.add(jtpDevicesTerminal, BorderLayout.CENTER);
        jpConnectionSettings = new JPanel();
        jpConnectionSettings.setLayout(new BorderLayout(0, 0));
        jpConnectionSettings.setMaximumSize(new Dimension(275, 2147483647));
        jpConnectionSettings.setMinimumSize(new Dimension(275, 594));
        jpConnectionSettings.setPreferredSize(new Dimension(275, 627));
        panel1.add(jpConnectionSettings, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.TOP));
        jpConnectionType = new JPanel();
        jpConnectionType.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionType.setEnabled(true);
        jpConnectionSettings.add(jpConnectionType, BorderLayout.NORTH);
        jlbConnectionType = new JLabel();
        jlbConnectionType.setText("Тип соединения");
        jpConnectionType.add(jlbConnectionType, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbConnectionType = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        jcbConnectionType.setModel(defaultComboBoxModel1);
        jpConnectionType.add(jcbConnectionType, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(120, -1), new Dimension(120, -1), new Dimension(120, -1), 0, false));
        final Spacer spacer1 = new Spacer();
        jpConnectionType.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpComPortSetup = new JPanel();
        jpComPortSetup.setLayout(new GridLayoutManager(12, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.setEnabled(true);
        jpConnectionSettings.add(jpComPortSetup, BorderLayout.CENTER);
        jpDataBits = new JPanel();
        jpDataBits.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(jpDataBits, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComDataBits = new JLabel();
        jlbComDataBits.setText("Биты данных");
        jpDataBits.add(jlbComDataBits, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbDataBits = new JComboBox();
        jpDataBits.add(jcbDataBits, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer2 = new Spacer();
        jpDataBits.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpParity = new JPanel();
        jpParity.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(jpParity, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComParity = new JLabel();
        jlbComParity.setText("Чётность");
        jpParity.add(jlbComParity, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbParity = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        jcbParity.setModel(defaultComboBoxModel2);
        jpParity.add(jcbParity, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        jpParity.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpComStopBit = new JPanel();
        jpComStopBit.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(jpComStopBit, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComStopBit = new JLabel();
        jlbComStopBit.setText("Стоп бит");
        jpComStopBit.add(jlbComStopBit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbStopBit = new JComboBox();
        jpComStopBit.add(jcbStopBit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        jpComStopBit.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpOpenClose = new JPanel();
        jpOpenClose.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(jpOpenClose, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(240, 29), new Dimension(240, 29), null, 0, false));
        jbComOpen = new JButton();
        jbComOpen.setAlignmentY(0.0f);
        jbComOpen.setHideActionText(false);
        jbComOpen.setText("Открыть");
        jpOpenClose.add(jbComOpen, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, 25), new Dimension(119, 25), new Dimension(200, 200), 0, false));
        jbComClose = new JButton();
        jbComClose.setAlignmentY(0.0f);
        jbComClose.setText("Закрыть");
        jpOpenClose.add(jbComClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, 25), new Dimension(119, 25), new Dimension(200, 200), 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComPorts = new JLabel();
        jlbComPorts.setText("Порт");
        panel2.add(jlbComPorts, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComPorts = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        jcbComPorts.setModel(defaultComboBoxModel3);
        panel2.add(jcbComPorts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer5 = new Spacer();
        panel2.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(panel3, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComProtocol = new JLabel();
        jlbComProtocol.setText("Протокол");
        panel3.add(jlbComProtocol, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbProtocol = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        jcbProtocol.setModel(defaultComboBoxModel4);
        panel3.add(jcbProtocol, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer6 = new Spacer();
        panel3.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(panel4, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jCbNeedPool = new JCheckBox();
        jCbNeedPool.setText("Опрос  ");
        panel4.add(jCbNeedPool, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("мс");
        panel4.add(label1, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfPoolDelay = new JTextField();
        jtfPoolDelay.setText("1000");
        panel4.add(jtfPoolDelay, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jpAddRemove = new JPanel();
        jpAddRemove.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(jpAddRemove, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(240, 29), new Dimension(240, 29), null, 0, false));
        jbAddDev = new JButton();
        jbAddDev.setAlignmentX(0.0f);
        jbAddDev.setAlignmentY(0.0f);
        jbAddDev.setText("Добавить у-во");
        jpAddRemove.add(jbAddDev, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, 25), new Dimension(119, 25), new Dimension(200, 200), 0, false));
        jbRemoveDev = new JButton();
        jbRemoveDev.setAlignmentX(0.0f);
        jbRemoveDev.setAlignmentY(0.0f);
        jbRemoveDev.setAutoscrolls(false);
        jbRemoveDev.setText("Удалить тек.");
        jpAddRemove.add(jbRemoveDev, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, 25), new Dimension(119, 25), new Dimension(200, 200), 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(panel5, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 100), new Dimension(-1, 100), new Dimension(-1, 100), 0, false));
        jbComUpdateList = new JButton();
        jbComUpdateList.setHideActionText(false);
        jbComUpdateList.setText("Обновить список портов");
        panel5.add(jbComUpdateList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(260, 25), new Dimension(400, 200), 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(panel6, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jbComSearch = new JButton();
        jbComSearch.setAlignmentY(0.0f);
        jbComSearch.setAutoscrolls(false);
        jbComSearch.setHideActionText(false);
        jbComSearch.setText("Поиск сетевых адресов");
        panel6.add(jbComSearch, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(260, 25), new Dimension(400, 200), 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.setEnabled(true);
        jpComPortSetup.add(panel7, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComBaudRate = new JLabel();
        jlbComBaudRate.setBackground(new Color(-11184811));
        jlbComBaudRate.setText("Скорость   ");
        panel7.add(jlbComBaudRate, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbBaudRate = new JComboBox();
        jcbBaudRate.setEnabled(true);
        panel7.add(jcbBaudRate, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer7 = new Spacer();
        panel7.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpComPortSetup.add(panel8, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jCbNeedLog = new JCheckBox();
        jCbNeedLog.setEnabled(true);
        jCbNeedLog.setText("Лог");
        panel8.add(jCbNeedLog, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jCbAutoConnect = new JCheckBox();
        jCbAutoConnect.setEnabled(false);
        jCbAutoConnect.setText("Автоподключение");
        panel8.add(jCbAutoConnect, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpFolderIconPanel = new JPanel();
        jpFolderIconPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpFolderIconPanel.setEnabled(true);
        panel8.add(jpFolderIconPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jpMainPanel;
    }


    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
