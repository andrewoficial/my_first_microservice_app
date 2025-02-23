package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.Main;
import org.example.device.ProtocolsList;
import org.example.services.AnswerStorage;
import org.example.services.comPool.AnyPoolService;
import org.example.services.comPort.ComPort;
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.ConnectException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


public class MainWindow extends JFrame implements Rendeble {
    private final static Logger log = Logger.getLogger(MainWindow.class);//Внешний логгер

    private final ExecutorService uiThPool = Executors.newCachedThreadPool(); //Поток GUI
    private MainLeftPanelStateCollection leftPanState; // Класс, хранящий состояние клиентов (настройки в памяти)
    private MyProperties prop; //Файл настроек
    private AnyPoolService anyPoolService; //Сервис опросов (разных протоколов)
    private ComPort comPorts; //ToDo убрать работу с портом напрямую из GUI

    private final ConcurrentHashMap<Integer, Integer> lastReceivedPositionFromStorageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, JTextPane> logDataTransferJtextPanelsMap = new ConcurrentHashMap<>();

    private static int currTabCount = 0;
    private int tab = 0; //Текущая активная (выбранная) вкладка
    @Getter
    private final AtomicInteger currentClientId = new AtomicInteger();

    private JPanel contentPane;
    private JComboBox<String> CB_ComPorts;
    private JComboBox<String> CB_BaudRate;
    private JComboBox<Integer> CB_DataBits;
    private JComboBox<String> CB_Parity;
    private JComboBox<Integer> CB_StopBit;
    private JButton BT_Open;
    private JButton BT_Close;
    private JCheckBox CB_Log;
    private JCheckBox CB_Autoconnect;
    private JButton BT_Update;
    private JTextField textToSend;
    private JButton BT_Send;
    private JComboBox<String> CB_Protocol;
    private JCheckBox CB_Pool;
    private JTabbedPane tabbedPane1;
    private JButton BT_AddDev;
    private JButton BT_RemoveDev;
    private JTextField IN_PoolDelay;
    private JButton BT_Search;
    private JPanel addRemove;
    private JPanel portSetup;
    private JPanel Terminal;
    private JTextField prefOneToSend;
    private JPanel spacer_middle;
    private JPanel right_spacer;
    private JPanel left_spacer;


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


    public MainWindow(MyProperties myProperties, ComPort comPorts, AnyPoolService anyPoolService) {
        log.debug("Подготовка к рендеру окна....");
        if (anyPoolService == null || comPorts == null || myProperties == null) {
            log.warn("В конструктор MainWindow передан null anyPoolService/comPorts/myProperties");
        }

        this.anyPoolService = anyPoolService;
        this.prop = myProperties;
        this.comPorts = comPorts;

        createMenu();
        int clientId = restoreParameters();

        BaudRatesList[] baudRate = BaudRatesList.values();
        currentClientId.set(clientId);
        for (int i = 0; i < baudRate.length; i++) {
            CB_BaudRate.addItem(baudRate[i].getValue() + "");
            if (leftPanState.getBaudRate(clientId) == baudRate[i].getValue()) {
                CB_BaudRate.setSelectedIndex(i);
            }
        }

        DataBitsList[] dataBits = DataBitsList.values();
        for (int i = 0; i < dataBits.length; i++) {
            CB_DataBits.addItem(dataBits[i].getValue());
            if (leftPanState.getDataBits(clientId) == i) {
                CB_DataBits.setSelectedIndex(i);
            }
        }

        ParityList[] parityLists = ParityList.values();
        for (int i = 0; i < parityLists.length; i++) {
            CB_Parity.addItem(parityLists[i].getName());
            if (leftPanState.getParityBits(clientId) == i) {
                CB_Parity.setSelectedIndex(i);
            }
        }

        StopBitsList[] stopBitsLists = StopBitsList.values();
        for (int i = 0; i < stopBitsLists.length; i++) {
            CB_StopBit.addItem(stopBitsLists[i].getValue());
            if (leftPanState.getStopBits(clientId) == i) {
                CB_StopBit.setSelectedIndex(i);
            }
        }


        for (int i = 0; i < comPorts.getAllPorts().size(); i++) {
            SerialPort currentPort = comPorts.getAllPorts().get(i);
            CB_ComPorts.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (currentPort.getSystemPortName().equals(prop.getPorts()[0])) {
                CB_ComPorts.setSelectedIndex(i);
            }
        }


        ProtocolsList[] protocolsLists = ProtocolsList.values();
        for (int i = 0; i < protocolsLists.length; i++) {
            CB_Protocol.addItem(protocolsLists[i].getValue());
            if (leftPanState.getProtocol(clientId) == i) {
                CB_Protocol.setSelectedIndex(i);
            }
        }

        log.debug("Добавление слушателей действий");
        BT_Update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка обновления списка ком-портов на вкладке" + tab + " с ИД клиента " + currentClientId.get());
                comPorts.updatePorts();
                CB_ComPorts.removeAllItems();
                for (SerialPort serialPort : comPorts.getAllPorts()) {
                    CB_ComPorts.addItem(serialPort.getSystemPortName() + " (" + MyUtilities.removeComWord(serialPort.getPortDescription()) + ")");
                }
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                saveParameters();
                log.info("Выход из программы при активной вкладке:" + tab);
                anyPoolService.shutDownComDataCollectorThreadPool();
                uiThPool.shutdownNow();
                System.exit(0);
            }
        });

        BT_Open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка открытия ком-порта на вкладке " + tab);
                updateClassFromGui();
                ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(currentClientId.get());
                if (ps == null) {
                    log.info("Для вкладки " + tab + " с ИД клиента " + currentClientId.get() + "не найден сервис опроса");
                    try {

                        addCustomMessage(anyPoolService.createOrUpdateComDataCollector(leftPanState, currentClientId.get(), getCurrComSelection(), CB_Protocol.getSelectedIndex(), false, false, 1200));
                        ps = anyPoolService.findComDataCollectorByClientId(currentClientId.get());
                    } catch (ConnectException exc) {
                        log.error("Ошибка при открытии порта на вкладке " + tab + ". Поток не создан. Проброс сообщения: " + exc);
                        addCustomMessage("Ошибка при открытии порта на вкладке " + tab + ". Поток не создан. Проброс сообщения: " + exc);
                        return;
                    }

                    addCustomMessage("Работа с портом " + ps.getComPort().getSystemPortName() + " завершена.");
                } else {
                    String answer = ps.reopenPort(currentClientId.get(), BaudRatesList.getNameLikeArray(CB_BaudRate.getSelectedIndex()), DataBitsList.getNameLikeArray(CB_DataBits.getSelectedIndex()),
                            StopBitsList.getNameLikeArray(CB_StopBit.getSelectedIndex()), ParityList.values()[CB_Parity.getSelectedIndex()].getValue(),
                            false);
                    addCustomMessage(answer);
                }
                configureComPort(null);
                prop.setPortForTab(ps.getComPort().getSystemPortName(), tab);

                checkIsUsedPort(); //Выставляет блокировки кнопки открыть/закрыть
                saveParameters();
            }
        });

        BT_Close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка закрытия ком-порта" + tab);
                ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(currentClientId.get());
                if (ps != null) {
                    log.info("Для вкладки " + tab + " найден сервис опроса");
                    try {
                        addCustomMessage(ps.closePort(currentClientId.get()));
                    } catch (ConnectException exception) {
                        addCustomMessage(exception.getMessage());
                        return;
                    }
                    anyPoolService.shutDownComDataCollectorsThreadByClientId(currentClientId.get());
                } else {
                    addCustomMessage("Попытка закрыть ком-порт у несуществующего потока опроса");
                }
                checkIsUsedPort(); //Блокировка кнопок
            }
        });

        CB_Pool.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSend(false);
            }
        });

        BT_Send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSend(true);
                renderData();
            }
        });

        CB_Log.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(currentClientId.get());

                if (ps != null) {
                    ps.setNeedLog(CB_Log.isSelected(), currentClientId.get());
                } else {
                    log.info("Для текущей влкадки потока опроса не существует");
                }
            }
        });


        CB_BaudRate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setBaudRate(currentClientId.get(), CB_BaudRate.getSelectedIndex());
                leftPanState.setBaudRateValue(currentClientId.get(), BaudRatesList.getNameLikeArray(CB_BaudRate.getSelectedIndex()));
            }
        });
        CB_StopBit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setStopBits(currentClientId.get(), CB_StopBit.getSelectedIndex());
                leftPanState.setStopBitsValue(currentClientId.get(), StopBitsList.getNameLikeArray(CB_StopBit.getSelectedIndex()));
            }
        });
        CB_DataBits.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setDataBits(currentClientId.get(), CB_DataBits.getSelectedIndex());
                leftPanState.setDataBitsValue(currentClientId.get(), DataBitsList.getNameLikeArray(CB_DataBits.getSelectedIndex()));
            }
        });
        CB_Parity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setParityBits(currentClientId.get(), CB_Parity.getSelectedIndex());
                leftPanState.setParityBitsValue(currentClientId.get(), CB_Parity.getSelectedIndex());
            }
        });
        CB_Protocol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setProtocol(currentClientId.get(), CB_Protocol.getSelectedIndex());
            }
        });

        BT_AddDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int newTabIndex = tabbedPane1.getTabCount();
                int newClientId = leftPanState.getClientIdByTabNumber(newTabIndex);
                if (newClientId == -1) {
                    newClientId = leftPanState.getNewRandomId();
                    leftPanState.addPairClientIdTabNumber(clientId, newTabIndex);
                    MainLeftPanelState state = new MainLeftPanelState();
                    state.setTabNumber(newTabIndex);
                    state.setClientId(newClientId);
                    leftPanState.addOrUpdateIdState(newClientId, state);
                }
                // Создание компонентов вкладки
                JTextPane logPanel = createLogPanel(newClientId);
                JPanel tabPanel = createTabPanel(logPanel);


                initializeMaps(newClientId, logPanel);

                // Добавление вкладки
                tabbedPane1.addTab("dev" + (newTabIndex + 1), tabPanel);
                updateUIAfterAdd(newTabIndex);
            }

            private JTextPane createLogPanel(int clientId) {
                JTextPane panel = new JTextPane();
                panel.setText("Лог для клиента " + clientId);
                return panel;
            }

            private JPanel createTabPanel(JTextPane logPanel) {
                JScrollPane scrollPane = new JScrollPane(logPanel);
                scrollPane.setPreferredSize(new Dimension(400, 400));

                JPanel panel = new JPanel(new BorderLayout());
                panel.add(scrollPane, BorderLayout.CENTER);
                return panel;
            }

            private void initializeMaps(int clientId, JTextPane panel) {
                lastReceivedPositionFromStorageMap.put(clientId, 0);
                logDataTransferJtextPanelsMap.put(clientId, panel);
            }

            private void updateUIAfterAdd(int tabIndex) {
                currTabCount = tabbedPane1.getTabCount();
                BT_RemoveDev.setEnabled(true);
                tabbedPane1.setSelectedIndex(tabIndex);
                updateTabTitles();
            }

            public void updateTabTitles() {
                for (int i = 0; i < tabbedPane1.getTabCount(); i++) {
                    tabbedPane1.setTitleAt(i, "dev" + (i + 1));
                }
            }

        });

        BT_RemoveDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int tabToRemove = tabbedPane1.getSelectedIndex();
                if (tabToRemove < 0) return;

                Integer clientId = leftPanState.getClientIdByTabNumber(tabToRemove);
                if (clientId == null) return;

                // Удаление данных
                removeClientData(clientId);
                tabbedPane1.removeTabAt(tabToRemove);

                // Перестройка маппингов
                rebuildMappings(tabToRemove);
                updateUIAfterRemove();
            }

            private void removeClientData(int clientId) {
                leftPanState.removeEntryByClientId(clientId);
                lastReceivedPositionFromStorageMap.remove(clientId);
                logDataTransferJtextPanelsMap.remove(clientId);
                AnswerStorage.removeAnswersForTab(clientId);
            }


                private void rebuildMappings(int removedTab) {
                    Map<Integer, Integer> clientIdToOldTab = new LinkedHashMap<>();
                    Map<Integer, JTextPane> updatedLogPanelsMap = new HashMap<>();

                    for (int i = 0; i < tabbedPane1.getTabCount() + 1; i++) {
                        Integer clientId = leftPanState.getClientIdByTabNumber(i);
                        if (clientId != null && clientId != -1) {
                            clientIdToOldTab.put(clientId, i);
                        }
                    }

                    clientIdToOldTab.forEach((clientId, oldTab) -> {
                        if (oldTab < removedTab) {
                            leftPanState.addOrUpdateClientIdTabNumber(clientId, oldTab);
                            updatedLogPanelsMap.put(clientId, logDataTransferJtextPanelsMap.get(clientId));
                        } else if (oldTab > removedTab) {
                            leftPanState.addOrUpdateClientIdTabNumber(clientId, oldTab - 1);
                            updatedLogPanelsMap.put(clientId, logDataTransferJtextPanelsMap.get(clientId));
                        }
                    });

                    logDataTransferJtextPanelsMap.clear();
                    logDataTransferJtextPanelsMap.putAll(updatedLogPanelsMap);

            }

            private void updateUIAfterRemove() {
                currTabCount = tabbedPane1.getTabCount();
                BT_RemoveDev.setEnabled(currTabCount > 0);
                updateTabTitles();
            }

            public void updateTabTitles() {
                for (int i = 0; i < tabbedPane1.getTabCount(); i++) {
                    tabbedPane1.setTitleAt(i, "dev" + (i + 1));
                }
            }
        });


        textToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateClassFromGui();
                readAndUpdateInputPrefAndCommandValues();
            }
        });
        textToSend.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Не используется в этом примере
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // Проверяем, была ли нажата клавиша Enter
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Вызываем нужный метод
                    updateClassFromGui();
                    readAndUpdateInputPrefAndCommandValues();
                    startSend(true);
                    renderData();
                    //onEnterPressed(textToSend.getText());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Не используется в этом примере
            }
        });

        prefOneToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateClassFromGui();
                readAndUpdateInputPrefAndCommandValues();
            }

        });

        prefOneToSend.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                // Не используется в этом примере
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // Проверяем, была ли нажата клавиша Enter
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    // Вызываем нужный метод
                    updateClassFromGui();
                    readAndUpdateInputPrefAndCommandValues();
                    startSend(true);
                    renderData();
                    //onEnterPressed(textToSend.getText());
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // Не используется в этом примере
            }
        });
        CB_ComPorts.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                checkIsUsedPort();
            }
        });
        CB_Protocol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()) != null) {
                    if (ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()) == ProtocolsList.ERSTEVAK_MTP4D) {
                        BT_Search.setEnabled(true);
                    } else {
                        BT_Search.setEnabled(false);
                    }
                }
            }
        });

        tabbedPane1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                tab = tabbedPane1.getSelectedIndex();
                log.info("Фокус установлен на вкладку " + tab); //активна вкладка, выбрана вкладка
                currentClientId.set(leftPanState.getClientIdByTabNumber(tab));
                if (currentClientId.get() == -1) {
                    currentClientId.set(checkAndCreateGuiStateClass());
                }
                //textToSend.setText(textToSendValue.get(tab));
                CB_Pool.setSelected(anyPoolService.isComDataCollectorByClientIdActiveDataSurvey(tab));
                CB_Log.setSelected(anyPoolService.isComDataCollectorByClientIdLogged(tab));
                updateGuiFromClass();

                ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(tab);

                if (ps == null) {
                    //log.info("Для выбранной вкладки нет сервиса опроса");
                } else {
                    if (ps.getComPort() == null) {
                        //log.info("Для выбранной вкладки сервис опроса не содержит активного соединения");
                    } else {
                        updateComPortSelectorFromProp();
                    }
                }
                checkIsUsedPort();
            }
        });
        tab = 0;
        currentClientId.set(leftPanState.getClientIdByTabNumber(tab));
        if (currentClientId.get() == -1) {
            currentClientId.set(checkAndCreateGuiStateClass());
        }
        lastReceivedPositionFromStorageMap.put(currentClientId.get(), 0);

        int tabCount = Math.max(0, prop.getTabCounter());
        for (int i = 0; i < tabCount; i++) {
            BT_AddDev.doClick(); //Добавление новой вкладки (клик)
        }


        updateGuiFromClass();
        this.renderData();

        uiThPool.submit(new RenderThread(this));

        prefOneToSend.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void update() {
                if (tab < leftPanState.getSize()) {
                    ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(tab);
                    if (ps != null)
                        ps.setTextToSendString(prefOneToSend.getText(), textToSend.getText(), tab);
                }
            }
        });

        textToSend.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            //ToDo убрать повтор, это убого
            public void update() {
                if (tab < leftPanState.getSize()) {
                    ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(tab);
                    if (ps != null)
                        ps.setTextToSendString(prefOneToSend.getText(), textToSend.getText(), tab);
                }
            }
        });

        IN_PoolDelay.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                update();
            }

            public void removeUpdate(DocumentEvent e) {
                update();
            }

            public void insertUpdate(DocumentEvent e) {
                update();
            }

            public void update() {
                log.info("Инициировано обновление периода опроса для владки " + tab);
                if (anyPoolService.getComDataCollectors().size() > tab && anyPoolService.getComDataCollectors().get(tab) != null) {


                    anyPoolService.getComDataCollectors().get(tab).setPoolDelay(getPoolDelayFromGui());
                    log.info(" выполнено обновление периода опроса для владки " + tab);
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                anyPoolService.shutDownComDataCollectorThreadPool();
                e.getWindow().dispose();
            }
        });

        initUI();
        tabbedPane1.setSelectedIndex(0);
    }

    private int getPoolDelayFromGui() {
        int newDelay = 3000;

        if (IN_PoolDelay.getText() != null) {
            try {
                newDelay = Integer.parseInt(IN_PoolDelay.getText());
            } catch (NumberFormatException e) {
                IN_PoolDelay.setText("3000");
            }
        } else {
            IN_PoolDelay.setText("3000");
        }
        return newDelay;
    }

    private void configureComPort(SerialPort serialPort) {

    }

    public static void waitTab(int tabNumber) {
        /*
            Будет создан отдельный класс для выполнения команд чере Web-UI
         */
    }

    public static void webSend(int tabSend, String command) {
        /*
            Будет создан отдельный класс для выполнения команд чере Web-UI
         */
    }

    public static boolean isBusy(int tabNumber) {
        return true;
        /*
            Будет создан отдельный класс для выполнения команд чере Web-UI
         */
    }

    public void startSend(boolean isBtn) {
        saveParameters();
        try {
            //isBtn - вызов по кнопке / pool - вызов про чекбоксу
            anyPoolService.createOrUpdateComDataCollector(leftPanState, currentClientId.get(), getCurrComSelection(), getCurrProtocolSelection(),
                    getNeedPoolState(), isBtn, getCurrPoolDelay());
        } catch (ConnectException e) {
            addCustomMessage(" Ошибка начала отправки " + e.getMessage());
        }
    }


    private void updateComPortSelectorFromProp() {
        if (prop != null && prop.getPorts() != null && prop.getPorts().length > tab && prop.getPorts()[tab] != null) {
            CB_ComPorts.setSelectedIndex(searchComPortNumberByName(prop.getPorts()[tab]));
        }
    }


    private int searchComPortNumberByName(String name) {
        ArrayList<SerialPort> ports = comPorts.getAllPorts();
        for (SerialPort port : ports) {
            if (port.getSystemPortName().equalsIgnoreCase(name)) {
                return ports.indexOf(port);
            }
        }
        return -1;
    }

    private int checkAndCreateGuiStateClass() {
        int clientId = leftPanState.getClientIdByTabNumber(tab);
        if (clientId == -1) {
            log.warn(" Для вкладки " + tab + " clientId получился " + clientId + " создаю объект состояния");
            clientId = leftPanState.getNewRandomId();
            leftPanState.addPairClientIdTabNumber(clientId, tab);
            MainLeftPanelState state = new MainLeftPanelState();
            state.setTabNumber(tab);
            state.setClientId(clientId);
            leftPanState.addOrUpdateIdState(clientId, state);
            return clientId;
        }
        return clientId;
    }

    private void updateGuiFromClass() {

        int clientId = checkAndCreateGuiStateClass();
        log.warn("Привожу вид вкладки к состоянию класса " + clientId);
        CB_DataBits.setSelectedIndex(leftPanState.getDataBits(clientId));
        CB_Parity.setSelectedIndex(leftPanState.getParityBits(clientId));
        CB_StopBit.setSelectedIndex(leftPanState.getStopBits(clientId));
        CB_BaudRate.setSelectedIndex(leftPanState.getBaudRate(clientId));
        CB_Protocol.setSelectedIndex(leftPanState.getProtocol(clientId));
        textToSend.setText(leftPanState.getCommand(clientId));
        prefOneToSend.setText(leftPanState.getPrefix(clientId));
        updateComPortSelectorFromProp();
    }


    private void updateClassFromGui() {
        log.warn("Привожу состояние класса  к виду вкладки " + tab);
        int clientId = checkAndCreateGuiStateClass();
        log.warn("К ней относится clientId " + clientId);
        log.warn("Параметр  getCommand до изменения" + leftPanState.getCommand(clientId));
        leftPanState.setParityBits(clientId, CB_Parity.getSelectedIndex());
        leftPanState.setDataBits(clientId, CB_DataBits.getSelectedIndex());
        leftPanState.setStopBits(clientId, CB_StopBit.getSelectedIndex());
        leftPanState.setBaudRate(clientId, CB_BaudRate.getSelectedIndex());
        leftPanState.setProtocol(clientId, CB_Protocol.getSelectedIndex());
        leftPanState.setCommandToSend(clientId, textToSend.getText());
        leftPanState.setPrefixToSend(clientId, prefOneToSend.getText());
        log.warn("Параметр  getCommand после изменения" + leftPanState.getCommand(clientId));
    }

    private void readAndUpdateInputPrefAndCommandValues() {
        log.info("Изменение в поле ввода префикса или команды");
        if (leftPanState.getClientIdByTabNumber(tab) != -1) {
            log.info("Обновление в массивах");
            ComDataCollector ps = anyPoolService.getComDataCollectors().get(tab);
            if (ps != null) {
                log.info("Попытка обновить команды опроса в найденом потоке");
                anyPoolService.findComDataCollectorByClientId(currentClientId.get()).setTextToSendString(prepareTextToSend(tab)[0], prepareTextToSend(tab)[1], tab);
            } else {
                log.info("поток для обновления префикса и команды пуст");
            }
            saveParameters();
        } else {
            log.warn("Ошибка при обновлении пула префиксов для опроса");
        }
    }

    private String[] prepareTextToSend(int tab) {
        int id = leftPanState.getClientIdByTabNumber(tab);
        if (id != -1) {
            leftPanState.setCommandToSend(id, textToSend.getText());
            leftPanState.setPrefixToSend(id, prefOneToSend.getText());
            String str[] = new String[2];
            str[0] = leftPanState.getPrefix(id);
            str[1] = leftPanState.getCommand(id);
            return str;
        }
        log.error("Не найдено состояние вкладки " + tab + " при попытки преобразовать пару префикс/команда в массив");
        return new String[2];

    }


    private void checkIsUsedPort() {
//        currTabCount = leftPanState.getSize();
//        anyPoolService.closeUnusedComConnection(currTabCount);
//        int targetComNum = CB_ComPorts.getSelectedIndex();
//
//        //Проверка, что порт уже открыт (блокировка кнопки ОТКРЫТЬ)
//        boolean alreadyOpen = anyPoolService.isComPortInUse(targetComNum);
//
//        if (alreadyOpen) {
//            BT_Open.setEnabled(false);
//            CB_Protocol.setEnabled(false);
//            BT_Close.setEnabled(true);
//        } else {
//            BT_Open.setEnabled(true);
//            CB_Protocol.setEnabled(true);
//            BT_Close.setEnabled(false);
//        }
//        int rootTab = -1;
//
//        rootTab = anyPoolService.getRootTabForComConnection(targetComNum);
//
//        if (rootTab > -1) {
//            if (rootTab != tab && alreadyOpen) {
////                addCustomMessage("Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "' ");
////                log.info("Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "' Просматриваемая вкладка " + tab);
////                BT_Close.setEnabled(false);
////                BT_Open.setEnabled(false);
////                CB_Protocol.setEnabled(false);
//            } else {
//                log.info("Это и есть корневая вкладка для вкладки " + rootTab);
//            }
//        }
    }


    private int getCurrComSelection() {
        return CB_ComPorts.getSelectedIndex();
    }

    private int getCurrProtocolSelection() {
        return CB_Protocol.getSelectedIndex();
    }

    private boolean getNeedPoolState() {
        return CB_Pool.isSelected();
    }

    private int getCurrPoolDelay() {
        int poolDelay = 10000;
        try {
            poolDelay = Integer.parseInt(IN_PoolDelay.getText());
        } catch (Exception e1) {
            IN_PoolDelay.setText("10000");
        }
        return poolDelay;
    }


    //Добавить сообщение в терминал GUI на выбранную вкладку. Не сохраняется в истории.
    public void addCustomMessage(String str) {
        JTextPane editedPane = logDataTransferJtextPanelsMap.get(currentClientId.get());

        Document doc = editedPane.getDocument();
        str = MyUtilities.CUSTOM_FORMATTER.format(LocalDateTime.now()) + ":\t" + str + "\n";
        try {
            doc.insertString(doc.getLength(), str, null);
        } catch (BadLocationException ex) {
            //throw new RuntimeException(ex);
        }
        editedPane.setCaretPosition(doc.getLength());
    }

    public void renderData() {
        tab = tabbedPane1.getSelectedIndex();
        currentClientId.set(leftPanState.getClientIdByTabNumber(tab));
        if (currentClientId.get() == -1) {
            currentClientId.set(checkAndCreateGuiStateClass());
        }
        Document doc = logDataTransferJtextPanelsMap.get(currentClientId.get()).getDocument();
        //final int maxLength = 25_000_000; // Примерно 50 МБ (25 млн символов)
        final int maxLength = 10_000; // Примерно 20 МБ (25 млн символов)

//        try {
//            log.info("Text " + doc.getText(0, doc.getLength() - 1));
//        } catch (BadLocationException e) {
//            //throw new RuntimeException(e);
//        }
        //log.info("Ищу для клиента " + currentClientId.get());
        try {
            // Объект этого кеша, для которого хранится последнее считанное значение из хранилища
            TabAnswerPart an = AnswerStorage.getAnswersQueForTab(lastReceivedPositionFromStorageMap.get(currentClientId.get()), currentClientId.get(), true);
            //log.info("part position " + an.getPosition());
            lastReceivedPositionFromStorageMap.put(currentClientId.get(), an.getPosition());
            if (an.getAnswerPart().isEmpty()) {
                //return;
            }
            //log.info("continue " + an.getAnswerPart());
            int currentLength = doc.getLength();
            int newTextLength = an.getAnswerPart().length();

            // Проверяем, не превысит ли общая длина максимальный размер
            if (currentLength + newTextLength > maxLength) {
                // Вычисляем, сколько символов нужно удалить
                int overflow = (currentLength + newTextLength) - maxLength;
                int removeCount = Math.min(currentLength, overflow + 1024); // Удаляем с запасом
                doc.remove(0, removeCount);
            }
            // Вставляем новый текст
            doc.insertString(doc.getLength(), an.getAnswerPart(), null);
            //log.info("Текст  " + an.getAnswerPart() + " будет добавлен для клиента " + currentClientId.get());
            logDataTransferJtextPanelsMap.get(currentClientId.get()).setCaretPosition(doc.getLength());
            //logDataTransferJtextPanelsMap.get(currentClientId.get()).setText("dev " + an.getAnswerPart());
        } catch (BadLocationException ex) {
            //ex.printStackTrace(); // Лучше залогировать ошибку
            log.warn("Произошло исключение в ходе рендера окна с историей данных:" + ex.getMessage());
        }
        doc = null;
    }

    @Override
    public boolean isEnable() {
        return true;
    }


    //Типа смена контекста
    public void updateServices(MyProperties newProps, ComPort newComPort, AnyPoolService newService) {
        this.prop = newProps;
        this.comPorts = newComPort;
        this.anyPoolService = newService;
    }

    private void saveParameters() {
        log.debug("Обновление файла настроек со вкладки" + tab + " и ИД клиента " + currentClientId.get());
        prop.setLastLeftPanel(leftPanState);
        Logger root = Logger.getRootLogger();
        prop.setLogLevel(root.getLevel());
        prop.setTabCounter(currTabCount);
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
        textToSend = new JTextField();
        textToSend.setForeground(new Color(-10328984));
        textToSend.setText("M^");
        panel3.add(textToSend, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(25, 25), new Dimension(900, 25), new Dimension(-1, 25), 0, false));
        BT_Send = new JButton();
        BT_Send.setText("Отправить");
        panel3.add(BT_Send, new GridConstraints(0, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        right_spacer = new JPanel();
        right_spacer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(right_spacer, new GridConstraints(0, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(5, 5), new Dimension(5, 5), new Dimension(5, 5), 0, false));
        spacer_middle = new JPanel();
        spacer_middle.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(spacer_middle, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, new Dimension(2, 2), new Dimension(2, 2), new Dimension(2, 2), 0, false));
        left_spacer = new JPanel();
        left_spacer.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel3.add(left_spacer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, new Dimension(2, 2), new Dimension(2, 2), new Dimension(2, 2), 0, false));
        prefOneToSend = new JTextField();
        prefOneToSend.setText("");
        panel3.add(prefOneToSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(10, 25), new Dimension(40, 25), new Dimension(80, 25), 0, false));
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
        CB_DataBits = new JComboBox();
        CB_DataBits.setBackground(new Color(-11513259));
        CB_DataBits.setForeground(new Color(-16777216));
        panel5.add(CB_DataBits, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
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
        CB_Parity = new JComboBox();
        CB_Parity.setBackground(new Color(-11513259));
        CB_Parity.setForeground(new Color(-16777216));
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        CB_Parity.setModel(defaultComboBoxModel1);
        panel6.add(CB_Parity, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
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
        CB_StopBit = new JComboBox();
        CB_StopBit.setBackground(new Color(-11513259));
        CB_StopBit.setForeground(new Color(-16777216));
        panel7.add(CB_StopBit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        panel7.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel8 = new JPanel();
        panel8.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel8.setBackground(new Color(-16777216));
        panel8.setForeground(new Color(-16777216));
        portSetup.add(panel8, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_Open = new JButton();
        BT_Open.setBackground(new Color(-16777216));
        BT_Open.setForeground(new Color(-1));
        BT_Open.setText("Открыть");
        panel8.add(BT_Open, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_Close = new JButton();
        BT_Close.setBackground(new Color(-16777216));
        BT_Close.setForeground(new Color(-1));
        BT_Close.setText("Закрыть");
        panel8.add(BT_Close, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        CB_ComPorts = new JComboBox();
        CB_ComPorts.setBackground(new Color(-11513259));
        CB_ComPorts.setForeground(new Color(-16777216));
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        CB_ComPorts.setModel(defaultComboBoxModel2);
        panel9.add(CB_ComPorts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
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
        CB_Protocol = new JComboBox();
        CB_Protocol.setBackground(new Color(-11513259));
        CB_Protocol.setForeground(new Color(-16777216));
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        CB_Protocol.setModel(defaultComboBoxModel3);
        panel10.add(CB_Protocol, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer5 = new Spacer();
        panel10.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel11 = new JPanel();
        panel11.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel11.setBackground(new Color(-16777216));
        panel11.setForeground(new Color(-16777216));
        portSetup.add(panel11, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        CB_Pool = new JCheckBox();
        CB_Pool.setBackground(new Color(-16777216));
        CB_Pool.setForeground(new Color(-1));
        CB_Pool.setText("Опрос  ");
        panel11.add(CB_Pool, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_PoolDelay = new JTextField();
        IN_PoolDelay.setText("1000");
        panel11.add(IN_PoolDelay, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addRemove = new JPanel();
        addRemove.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        addRemove.setBackground(new Color(-16777216));
        addRemove.setForeground(new Color(-16777216));
        portSetup.add(addRemove, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_AddDev = new JButton();
        BT_AddDev.setBackground(new Color(-16777216));
        BT_AddDev.setForeground(new Color(-1));
        BT_AddDev.setText("Добавить у-во");
        addRemove.add(BT_AddDev, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_RemoveDev = new JButton();
        BT_RemoveDev.setBackground(new Color(-16777216));
        BT_RemoveDev.setForeground(new Color(-1));
        BT_RemoveDev.setText("Удалить тек.");
        addRemove.add(BT_RemoveDev, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel12 = new JPanel();
        panel12.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel12.setBackground(new Color(-16777216));
        panel12.setForeground(new Color(-16777216));
        portSetup.add(panel12, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_Update = new JButton();
        BT_Update.setBackground(new Color(-16777216));
        BT_Update.setForeground(new Color(-1));
        BT_Update.setText("Обновить");
        panel12.add(BT_Update, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel13 = new JPanel();
        panel13.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel13.setBackground(new Color(-16777216));
        panel13.setForeground(new Color(-16777216));
        portSetup.add(panel13, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_Search = new JButton();
        BT_Search.setAutoscrolls(false);
        BT_Search.setBackground(new Color(-10328984));
        BT_Search.setForeground(new Color(-1));
        BT_Search.setHideActionText(false);
        BT_Search.setText("Поиск сетевых адресов");
        panel13.add(BT_Search, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        CB_BaudRate = new JComboBox();
        CB_BaudRate.setBackground(new Color(-11513259));
        CB_BaudRate.setEnabled(true);
        CB_BaudRate.setForeground(new Color(-16777216));
        panel14.add(CB_BaudRate, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer6 = new Spacer();
        panel14.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel15 = new JPanel();
        panel15.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel15.setBackground(new Color(-16777216));
        panel15.setForeground(new Color(-16777216));
        portSetup.add(panel15, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        CB_Log = new JCheckBox();
        CB_Log.setBackground(new Color(-16777216));
        CB_Log.setEnabled(true);
        CB_Log.setForeground(new Color(-1));
        CB_Log.setText("Лог");
        panel15.add(CB_Log, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CB_Autoconnect = new JCheckBox();
        CB_Autoconnect.setBackground(new Color(-16777216));
        CB_Autoconnect.setEnabled(false);
        CB_Autoconnect.setForeground(new Color(-1));
        CB_Autoconnect.setText("Автоподключение");
        panel15.add(CB_Autoconnect, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
