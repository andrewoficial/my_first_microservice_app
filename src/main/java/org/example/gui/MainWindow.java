package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.Main;
import org.example.device.ProtocolsList;
import org.example.gui.mainWindowUtilites.GuiStateManager;
import org.example.gui.mainWindowUtilites.PortManager;
import org.example.gui.mainWindowUtilites.TabManager;
import org.example.services.AnswerStorage;
import org.example.services.comPool.AnyPoolService;
import org.example.services.TabAnswerPart;
import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.ParityList;
import org.example.services.comPort.StopBitsList;
import org.example.utilites.*;
import org.example.services.comPool.ComDataCollector;
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

    private JPanel addRemove;
    private JPanel portSetup;
    private JPanel Terminal;
    private JPanel spacer_middle;
    private JPanel right_spacer;
    private JPanel left_spacer;
    private JPanel contentPane;
    private JTabbedPane tabbedPane1;
    private JComboBox<String> comboBox_ComPorts;
    private JComboBox<String> comboBox_BaudRate;
    private JComboBox<Integer> comboBox_DataBits;
    private JComboBox<String> comboBox_Parity;
    private JComboBox<Integer> comboBox_StopBit;
    private JComboBox<String> comboBox_Protocol;
    private JButton button_Open;
    private JButton button_Close;
    private JButton button_Update;
    private JButton button_Send;
    private JButton button_AddDev;
    private JButton button_RemoveDev;
    private JButton button_Search;

    private JCheckBox checkBox_Pool;
    private JCheckBox checkBox_Log;
    private JCheckBox checkBox_Autoconnect;

    private JTextField textField_poolDelay;
    private JTextField textField_textToSend;
    private JTextField textField_prefToSend;


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
        JMenuBar menuBar = new JMenuBar();
        JmenuFile menu = new JmenuFile(prop, anyPoolService);
        menuBar.add(menu.createFileMenu());
        menuBar.add(menu.createSettingsMenu());
        menuBar.add(menu.createViewMenu(uiThPool));
        menuBar.add(menu.createUtilitiesMenu(uiThPool));
        menuBar.add(menu.createSystemParametrs(uiThPool));
        menuBar.add(menu.createInfo(uiThPool));
        setJMenuBar(menuBar);
        setContentPane(contentPane);
        log.debug("Инициализация панели и меню завершена");
    }

    private int restoreParameters() {
        log.info("Восстанвливаю параметры");
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


    public MainWindow(MyProperties myProperties, AnyPoolService anyPoolService) {
        log.debug("Подготовка к рендеру окна....");
        if (anyPoolService == null || myProperties == null) {
            log.warn("В конструктор MainWindow передан null anyPoolService/comPorts/myProperties");
        }

        this.anyPoolService = anyPoolService;
        this.prop = myProperties;

        createMenu();
        currentActiveClientId.set(restoreParameters());

        //Заполнение полей комбо-боксов
        initComboBox(comboBox_BaudRate, BaudRatesList.values(),
                i -> String.valueOf(BaudRatesList.getNameLikeArray(i)),
                () -> leftPanState.getBaudRate(currentActiveClientId.get()));

        initComboBox(comboBox_Parity, ParityList.values(),
                i -> String.valueOf(ParityList.getNameLikeArray(i)),
                () -> leftPanState.getParityBits(currentActiveClientId.get()));

        initComboBox(comboBox_Protocol, ProtocolsList.values(),
                i -> ProtocolsList.getLikeArrayEnum(i).getValue(),
                () -> leftPanState.getProtocol(currentActiveClientId.get()));

        initComboBox(comboBox_StopBit, StopBitsList.values(),
                StopBitsList::getNameLikeArray,
                () -> leftPanState.getStopBits(currentActiveClientId.get()));

        initComboBox(comboBox_DataBits, DataBitsList.values(),
                DataBitsList::getNameLikeArray,
                () -> leftPanState.getDataBits(currentActiveClientId.get()));

        guiStateManager = new GuiStateManager(leftPanState, currentActiveClientId, currentActiveTab);
        tabManager = new TabManager(tabbedPane1, leftPanState, logDataTransferJtextPanelsMap, lastReceivedPositionFromStorageMap, button_RemoveDev);
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

        tabbedPane1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                currentActiveTab.set(tabbedPane1.getSelectedIndex());
                log.info("Фокус установлен на вкладку " + currentActiveTab); //активна вкладка, выбрана вкладка
                currentActiveClientId.set(leftPanState.getClientIdByTabNumber(currentActiveTab.get()));
                if (currentActiveClientId.get() == -1) {
                    currentActiveClientId.set(checkAndCreateGuiStateClass());
                }
                checkBox_Pool.setSelected(anyPoolService.isComDataCollectorByClientIdActiveDataSurvey(currentActiveTab.get()));
                checkBox_Log.setSelected(anyPoolService.isComDataCollectorByClientIdLogged(currentActiveTab.get()));
                updateGuiFromClass();
                ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(currentActiveTab.get());
                if (ps != null) {
                    if (ps.getComPort() != null) {
                        updateComPortSelectorFromProp();
                    }
                }
                checkIsUsedPort();
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
        tabbedPane1.setSelectedIndex(0);
        uiThPool.submit(new RenderThread(this));
    }

    private void updateComPortList() {
        for (int i = 0; i < SerialPort.getCommPorts().length; i++) {
            SerialPort currentPort = SerialPort.getCommPorts()[i];
            comboBox_ComPorts.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (currentPort.getSystemPortName().equals(prop.getPorts()[0])) {
                comboBox_ComPorts.setSelectedIndex(i);
            }
        }
    }

    // Инициализация слушателей
    private void initDocumentListeners() {
        // Text Fields
        ListenerUtils.addDocumentListener(textField_poolDelay, this::updatePoolDelay);
        ListenerUtils.addDocumentListener(textField_prefToSend, this::updateTextToSend);
        ListenerUtils.addDocumentListener(textField_textToSend, this::updateTextToSend);

        // Key Listeners
        ListenerUtils.addKeyListener(textField_textToSend, this::updateTextAndSendFromEnter);
        ListenerUtils.addKeyListener(textField_prefToSend, this::updateTextAndSendFromEnter);

        // Combo Boxes
        ListenerUtils.addComboBoxListener(comboBox_ComPorts, this::checkIsUsedPort);
        ListenerUtils.addComboBoxListener(comboBox_Parity, this::updateParity);
        ListenerUtils.addComboBoxListener(comboBox_Protocol, this::updateProtocol);
        ListenerUtils.addComboBoxListener(comboBox_BaudRate, this::updateBaudRate);
        ListenerUtils.addComboBoxListener(comboBox_StopBit, this::updateStopBit);
        ListenerUtils.addComboBoxListener(comboBox_DataBits, this::updateDataBits);

        // Buttons
        ListenerUtils.addActionListener(button_Open, this::openComPort);
        ListenerUtils.addActionListener(button_Close, this::closeComPort);
        ListenerUtils.addActionListener(button_Send, this::updateTextAndSendFromEnter);
        ListenerUtils.addActionListener(button_AddDev, this::addTab);
        ListenerUtils.addActionListener(button_RemoveDev, this::removeTab);
        ListenerUtils.addActionListener(button_Update, this::updateComPortList);

        // CheckBoxes
        ListenerUtils.addActionListener(checkBox_Pool, this::updateTextAndSendFromCheckBox);
        ListenerUtils.addActionListener(checkBox_Log, this::updateLogCheckBox);
    }

    private void removeTab() {
        tabManager.removeTab();
        currentTabCount.set(tabbedPane1.getTabCount());
    }

    private void addTab() {
        tabManager.addTab();
        currentTabCount.set(tabbedPane1.getTabCount());
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
    }


    private void updateParity() {
        leftPanState.setParityBits(currentActiveClientId.get(), comboBox_Parity.getSelectedIndex());
        leftPanState.setParityBitsValue(currentActiveClientId.get(), comboBox_Parity.getSelectedIndex());
    }

    private void updateBaudRate() {
        leftPanState.setBaudRate(currentActiveClientId.get(), comboBox_BaudRate.getSelectedIndex());
        leftPanState.setBaudRateValue(currentActiveClientId.get(), BaudRatesList.getNameLikeArray(comboBox_BaudRate.getSelectedIndex()));
    }

    private void updateStopBit() {
        leftPanState.setStopBits(currentActiveClientId.get(), comboBox_StopBit.getSelectedIndex());
        leftPanState.setStopBitsValue(currentActiveClientId.get(), StopBitsList.getNameLikeArray(comboBox_StopBit.getSelectedIndex()));
    }

    private void updateDataBits() {
        leftPanState.setDataBits(currentActiveClientId.get(), comboBox_DataBits.getSelectedIndex());
        leftPanState.setDataBitsValue(currentActiveClientId.get(), DataBitsList.getNameLikeArray(comboBox_DataBits.getSelectedIndex()));
    }

    private void updateProtocol() {
        leftPanState.setProtocol(currentActiveClientId.get(), comboBox_Protocol.getSelectedIndex());
        if (ProtocolsList.getLikeArrayEnum(comboBox_Protocol.getSelectedIndex()) != null)
            button_Search.setEnabled(ProtocolsList.getLikeArrayEnum(comboBox_Protocol.getSelectedIndex()) == ProtocolsList.ERSTEVAK_MTP4D);
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
            ps.setNeedLog(checkBox_Log.isSelected(), currentActiveClientId.get());
        } else {
            log.info("Для текущей влкадки потока опроса не существует");
        }
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
            return Integer.parseInt(textField_poolDelay.getText());
        } catch (NumberFormatException e) {
            textField_poolDelay.setText(String.valueOf(DEFAULT_POOL_DELAY));
            return DEFAULT_POOL_DELAY;
        }
    }

    public void startSend(boolean isBtn) {
        saveParameters();
        try {
            //isBtn - вызов по кнопке / pool - вызов про чекбоксу
            anyPoolService.createOrUpdateComDataCollector(leftPanState, currentActiveClientId.get(), getCurrComSelection(), getCurrProtocolSelection(),
                    getNeedPoolState(), isBtn, getCurrPoolDelay());
        } catch (ConnectException e) {
            addCustomMessage(" Ошибка начала отправки " + e.getMessage());
        }
    }

    private void updateComPortSelectorFromProp() {
        if (prop != null && prop.getPorts() != null && prop.getPorts().length > currentActiveTab.get() && prop.getPorts()[currentActiveTab.get()] != null) {
            comboBox_ComPorts.setSelectedIndex(anyPoolService.searchComPortNumberByName(prop.getPorts()[currentActiveTab.get()]));
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
                comboBox_DataBits, comboBox_Parity, comboBox_StopBit,
                comboBox_BaudRate, comboBox_Protocol, textField_textToSend,
                textField_prefToSend
        );
    }


    private void updateClassFromGui() {
        guiStateManager.updateModelFromGui(
                comboBox_Parity, comboBox_DataBits, comboBox_StopBit,
                comboBox_BaudRate, comboBox_Protocol, textField_textToSend,
                textField_prefToSend
        );
    }

    private void readAndUpdateInputPrefAndCommandValues() {
        log.info("Изменение в поле ввода префикса или команды");
        if (leftPanState.getClientIdByTabNumber(currentActiveTab.get()) != -1) {
            if (!anyPoolService.getComDataCollectors().isEmpty()) {
                ComDataCollector ps = anyPoolService.getComDataCollectors().get(currentActiveTab.get());
                if (ps != null)
                    anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get()).setTextToSendString(leftPanState.getPrefix(currentActiveClientId.get()), leftPanState.getCommand(currentActiveClientId.get()), currentActiveClientId.get());
                saveParameters();
            }
        }
    }

    private void checkIsUsedPort() {
        anyPoolService.closeUnusedComConnection(currentTabCount.get());
        boolean alreadyOpen = anyPoolService.isComPortInUse(getCurrComSelection());

        button_Open.setEnabled(!alreadyOpen);
        comboBox_Protocol.setEnabled(!alreadyOpen);
        button_Close.setEnabled(alreadyOpen);

        int rootTab = anyPoolService.getRootTabForComConnection(getCurrComSelection());
        if (rootTab > -1)
            return;
        if (rootTab != currentActiveTab.get() && alreadyOpen) {
            addCustomMessage("Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "' ");
            log.info("Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "' Просматриваемая вкладка " + currentActiveTab);
            button_Close.setEnabled(false);
            button_Open.setEnabled(false);
            comboBox_Protocol.setEnabled(false);
        }
    }

    private int getCurrComSelection() {
        return comboBox_ComPorts.getSelectedIndex();
    }

    private int getCurrProtocolSelection() {
        return comboBox_Protocol.getSelectedIndex();
    }

    private boolean getNeedPoolState() {
        return checkBox_Pool.isSelected();
    }

    private int getCurrPoolDelay() {
        int poolDelay = 10000;
        try {
            poolDelay = Integer.parseInt(textField_poolDelay.getText());
        } catch (Exception e1) {
            textField_poolDelay.setText("10000");
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

    public void renderData() {
        Integer clientId = currentActiveClientId.get();
        Document doc = logDataTransferJtextPanelsMap.get(clientId).getDocument();
        final int maxLength = 10_000;

        try {
            int lastPosition = lastReceivedPositionFromStorageMap.getOrDefault(clientId, 0);
            int queueOffsetInt = AnswerStorage.queueOffset.getOrDefault(clientId, 0);

            // Синхронизация доступа к позиции
            synchronized (lastReceivedPositionFromStorageMap) {
                if (lastPosition < queueOffsetInt) {
                    lastPosition = queueOffsetInt;
                    lastReceivedPositionFromStorageMap.put(clientId, lastPosition);
                }
            }

            TabAnswerPart an = AnswerStorage.getAnswersQueForTab(lastPosition, clientId, true);

            if (an.getAnswerPart() == null || an.getAnswerPart().isEmpty()) {
                log.info("Нет новых данных для клиента [" + clientId + "]");
                return;
            }
            // Обновляем позицию атомарно
            synchronized (lastReceivedPositionFromStorageMap) {
                lastReceivedPositionFromStorageMap.put(clientId, an.getPosition());
            }

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
    public void updateServices(MyProperties newProps, AnyPoolService newService) {
        this.prop = newProps;
        this.anyPoolService = newService;
    }

    private void saveParameters() {
        log.debug("Обновление файла настроек со вкладки" + currentActiveTab + " и ИД клиента " + currentActiveClientId.get());
        log.debug("Обновление файла настроек со вкладки" + currentActiveTab + " и ИД клиента " + currentActiveClientId.get());
        prop.setLastLeftPanel(leftPanState);
        prop.setLogLevel(Logger.getRootLogger().getLevel());
        prop.setTabCounter(currentTabCount.get());
        prop.setIdentAndTabBounding(AnswerStorage.getDeviceTabPair());
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
        contentPane.setBackground(new Color(-16777216));
        Font contentPaneFont = UIManager.getFont("Tree.font");
        if (contentPaneFont != null) contentPane.setFont(contentPaneFont);
        contentPane.setForeground(new Color(-16777216));
        contentPane.setMaximumSize(new Dimension(900, 900));
        contentPane.setMinimumSize(new Dimension(530, 530));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.setBackground(new Color(-16777216));
        panel1.setForeground(new Color(-11513259));
        contentPane.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(450, 350), null, null, 0, false));
        Terminal = new JPanel();
        Terminal.setLayout(new GridLayoutManager(3, 1, new Insets(0, 0, 0, 0), -1, -1));
        Terminal.setBackground(new Color(-16777216));
        Terminal.setForeground(new Color(-16777216));
        panel1.add(Terminal, new GridConstraints(0, 1, 3, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(210, 350), new Dimension(250, 390), null, 0, false));
        tabbedPane1 = new JTabbedPane();
        tabbedPane1.setForeground(new Color(-10328984));
        Terminal.add(tabbedPane1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setBackground(new Color(-16777216));
        panel2.setForeground(new Color(-16777216));
        Terminal.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, new Dimension(-1, 10), new Dimension(-1, 10), new Dimension(-1, 10), 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 6, new Insets(0, 0, 0, 0), -1, -1));
        Terminal.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 30), new Dimension(-1, 30), new Dimension(-1, 30), 0, false));
        textField_textToSend = new JTextField();
        textField_textToSend.setForeground(new Color(-10328984));
        textField_textToSend.setText("M^");
        panel3.add(textField_textToSend, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(25, 25), new Dimension(900, 25), new Dimension(-1, 25), 0, false));
        button_Send = new JButton();
        button_Send.setText("Отправить");
        panel3.add(button_Send, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        right_spacer = new JPanel();
        right_spacer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(right_spacer, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(5, 5), new Dimension(5, 5), new Dimension(5, 5), 0, false));
        spacer_middle = new JPanel();
        spacer_middle.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(spacer_middle, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, new Dimension(2, 2), new Dimension(2, 2), new Dimension(2, 2), 0, false));
        left_spacer = new JPanel();
        left_spacer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(left_spacer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, new Dimension(2, 2), new Dimension(2, 2), new Dimension(2, 2), 0, false));
        textField_prefToSend = new JTextField();
        textField_prefToSend.setText("");
        panel3.add(textField_prefToSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(10, 25), new Dimension(40, 25), new Dimension(80, 25), 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTHWEST, GridConstraints.FILL_NONE, 1, 1, new Dimension(250, 390), new Dimension(250, 390), new Dimension(250, 390), 0, false));
        portSetup = new JPanel();
        portSetup.setLayout(new GridLayoutManager(12, 1, new Insets(0, 0, 0, 0), -1, -1));
        portSetup.setBackground(new Color(-16777216));
        portSetup.setForeground(new Color(-16777216));
        panel4.add(portSetup, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(250, 390), new Dimension(250, 390), new Dimension(250, 390), 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel5.setBackground(new Color(-16777216));
        panel5.setForeground(new Color(-16777216));
        portSetup.add(panel5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setBackground(new Color(-16777216));
        label1.setForeground(new Color(-1));
        label1.setText("Биты данных");
        panel5.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox_DataBits = new JComboBox();
        comboBox_DataBits.setBackground(new Color(-11513259));
        comboBox_DataBits.setForeground(new Color(-16777216));
        panel5.add(comboBox_DataBits, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer1 = new Spacer();
        panel5.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel6.setBackground(new Color(-16777216));
        panel6.setForeground(new Color(-16777216));
        portSetup.add(panel6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setForeground(new Color(-1));
        label2.setText("Чётность");
        panel6.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox_Parity = new JComboBox();
        comboBox_Parity.setBackground(new Color(-11513259));
        comboBox_Parity.setForeground(new Color(-16777216));
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        comboBox_Parity.setModel(defaultComboBoxModel1);
        panel6.add(comboBox_Parity, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer2 = new Spacer();
        panel6.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel7 = new JPanel();
        panel7.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel7.setBackground(new Color(-16777216));
        panel7.setForeground(new Color(-16777216));
        portSetup.add(panel7, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setBackground(new Color(-16777216));
        label3.setForeground(new Color(-1));
        label3.setText("Стоп бит");
        panel7.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox_StopBit = new JComboBox();
        comboBox_StopBit.setBackground(new Color(-11513259));
        comboBox_StopBit.setForeground(new Color(-16777216));
        panel7.add(comboBox_StopBit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.setBackground(new Color(-16777216));
        panel8.setForeground(new Color(-16777216));
        portSetup.add(panel8, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button_Open = new JButton();
        button_Open.setBackground(new Color(-16777216));
        button_Open.setForeground(new Color(-1));
        button_Open.setText("Открыть");
        panel8.add(button_Open, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button_Close = new JButton();
        button_Close.setBackground(new Color(-16777216));
        button_Close.setForeground(new Color(-1));
        button_Close.setText("Закрыть");
        panel8.add(button_Close, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel9 = new JPanel();
        panel9.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel9.setBackground(new Color(-16777216));
        panel9.setForeground(new Color(-16777216));
        portSetup.add(panel9, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setBackground(new Color(-1));
        label4.setForeground(new Color(-1));
        label4.setText("Порт");
        panel9.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox_ComPorts = new JComboBox();
        comboBox_ComPorts.setBackground(new Color(-11513259));
        comboBox_ComPorts.setForeground(new Color(-16777216));
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        comboBox_ComPorts.setModel(defaultComboBoxModel2);
        panel9.add(comboBox_ComPorts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        panel9.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel10 = new JPanel();
        panel10.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel10.setBackground(new Color(-16777216));
        panel10.setForeground(new Color(-16777216));
        portSetup.add(panel10, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setBackground(new Color(-16777216));
        label5.setForeground(new Color(-1));
        label5.setText("Протокол");
        panel10.add(label5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox_Protocol = new JComboBox();
        comboBox_Protocol.setBackground(new Color(-11513259));
        comboBox_Protocol.setForeground(new Color(-16777216));
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        comboBox_Protocol.setModel(defaultComboBoxModel3);
        panel10.add(comboBox_Protocol, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer5 = new Spacer();
        panel10.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel11.setBackground(new Color(-16777216));
        panel11.setForeground(new Color(-16777216));
        portSetup.add(panel11, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkBox_Pool = new JCheckBox();
        checkBox_Pool.setBackground(new Color(-16777216));
        checkBox_Pool.setForeground(new Color(-1));
        checkBox_Pool.setText("Опрос  ");
        panel11.add(checkBox_Pool, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField_poolDelay = new JTextField();
        textField_poolDelay.setText("1000");
        panel11.add(textField_poolDelay, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addRemove = new JPanel();
        addRemove.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        addRemove.setBackground(new Color(-16777216));
        addRemove.setForeground(new Color(-16777216));
        portSetup.add(addRemove, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button_AddDev = new JButton();
        button_AddDev.setBackground(new Color(-16777216));
        button_AddDev.setForeground(new Color(-1));
        button_AddDev.setText("Добавить у-во");
        addRemove.add(button_AddDev, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        button_RemoveDev = new JButton();
        button_RemoveDev.setBackground(new Color(-16777216));
        button_RemoveDev.setForeground(new Color(-1));
        button_RemoveDev.setText("Удалить тек.");
        addRemove.add(button_RemoveDev, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel12.setBackground(new Color(-16777216));
        panel12.setForeground(new Color(-16777216));
        portSetup.add(panel12, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button_Update = new JButton();
        button_Update.setBackground(new Color(-16777216));
        button_Update.setForeground(new Color(-1));
        button_Update.setText("Обновить");
        panel12.add(button_Update, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel13.setBackground(new Color(-16777216));
        panel13.setForeground(new Color(-16777216));
        portSetup.add(panel13, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        button_Search = new JButton();
        button_Search.setAutoscrolls(false);
        button_Search.setBackground(new Color(-10328984));
        button_Search.setForeground(new Color(-1));
        button_Search.setHideActionText(false);
        button_Search.setText("Поиск сетевых адресов");
        panel13.add(button_Search, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel14 = new JPanel();
        panel14.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel14.setBackground(new Color(-16777216));
        panel14.setEnabled(true);
        panel14.setForeground(new Color(-16777216));
        portSetup.add(panel14, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setBackground(new Color(-16777216));
        label6.setForeground(new Color(-1));
        label6.setText("Скорость   ");
        panel14.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        comboBox_BaudRate = new JComboBox();
        comboBox_BaudRate.setBackground(new Color(-11513259));
        comboBox_BaudRate.setEnabled(true);
        comboBox_BaudRate.setForeground(new Color(-16777216));
        panel14.add(comboBox_BaudRate, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer6 = new Spacer();
        panel14.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel15.setBackground(new Color(-16777216));
        panel15.setForeground(new Color(-16777216));
        portSetup.add(panel15, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        checkBox_Log = new JCheckBox();
        checkBox_Log.setBackground(new Color(-16777216));
        checkBox_Log.setEnabled(true);
        checkBox_Log.setForeground(new Color(-1));
        checkBox_Log.setText("Лог");
        panel15.add(checkBox_Log, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        checkBox_Autoconnect = new JCheckBox();
        checkBox_Autoconnect.setBackground(new Color(-16777216));
        checkBox_Autoconnect.setEnabled(false);
        checkBox_Autoconnect.setForeground(new Color(-1));
        checkBox_Autoconnect.setText("Автоподключение");
        panel15.add(checkBox_Autoconnect, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer7 = new Spacer();
        panel15.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }


}
