package org.example.gui.main;

import ch.qos.logback.classic.Logger;
import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import lombok.extern.slf4j.Slf4j;
import org.example.Main;
import org.example.device.ProtocolComPort;
import org.example.device.ProtocolsList;
import org.example.device.protErstevakMtp4d.ERSTEVAK_MTP4D;
import org.example.device.protEctTc290.ECT_TC290;
import org.example.device.SlaveAddressable;
import org.example.device.SomeDevice;
import org.example.device.TemplatedAscii;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.SingleCommand;
import org.example.gui.*;
import org.example.gui.components.*;
import org.example.gui.main.left.hidParamForm;
import org.example.gui.main.left.wsParamForm;
import org.example.gui.devices.igm.modbus.IgmModbusSearchDialog;
import org.example.gui.devices.erstevak.ErstevakSearchDialog;
import org.example.gui.devices.ecttc290.EctTc290SearchDialog;
import org.example.gui.mainWindowUtilites.FolderPictureForLog;
import org.example.gui.mainWindowUtilites.TerminalLogTrimmer;
import org.example.gui.mainWindowUtilites.GuiStateManager;
import org.example.gui.mainWindowUtilites.CommandFieldFormatter;
import org.example.gui.mainWindowUtilites.TabManager;
import org.example.services.AnswerStorage;
import org.example.services.ConnectionSettingsService;
import org.example.services.LauncherUpdateCheckService;
import org.example.services.PollingService;
import org.example.services.PortLifecycleService;
import org.example.services.TabService;
import org.example.services.UpdateCheckService;
import org.example.services.connection.ConnectionType;
import org.example.services.connectionPool.AnyPoolService;
import org.example.services.transport.hid.HidDeviceEntry;
import org.example.services.transport.hid.HidDeviceTypeFilter;
import org.example.services.transport.hid.HidPort;
import org.example.services.TabAnswerPart;
import org.example.services.transport.serial.BaudRatesList;
import org.example.services.transport.serial.DataBitsList;
import org.example.services.transport.serial.ParityList;
import org.example.services.transport.serial.StopBitsList;
import org.example.services.loggers.DeviceLogger;
import org.example.utilites.*;
import org.example.services.connectionPool.ComDataCollector;
import org.example.utilites.Constants;
import org.example.utilites.properties.MyProperties;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
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

import static org.example.utilites.Constants.Gui.Windows.DEVICE_NAME_LIMIT;
import static org.example.utilites.MyUtilities.createDeviceByProtocol;

@Slf4j
public class MainWindow extends JFrame implements Rendeble {
    private ExecutorService uiThPool = Executors.newCachedThreadPool(); //Поток GUI
    private MainLeftPanelStateCollection leftPanState; // Класс, хранящий состояние клиентов (настройки в памяти)
    private MyProperties prop; //Файл настроек
    private AnyPoolService anyPoolService; //Сервис опросов (разных протоколов)
    private ConnectionSettingsService connectionSettingsService; //Сервис управления настройками подключения
    private PortLifecycleService portLifecycleService; //Сервис жизненного цикла портов
    private PollingService pollingService; //Сервис управления опросом
    private TabService tabService; //Сервис управления вкладками
    private final AnswerStorage answerStorage;
    private final UpdateCheckService updateCheckService; //Тихий сервис проверки обновлений
    private final LauncherUpdateCheckService launcherUpdateCheckService; //Тихий сервис проверки обновлений лаунчера
    private final GuiStateManager guiStateManager;
    private final TabManager tabManager;

    private final ConcurrentHashMap<Integer, Integer> lastReceivedPositionFromStorageMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, JTextPane> logDataTransferJtextPanelsMap = new ConcurrentHashMap<>();

    private final int DEFAULT_POOL_DELAY = 3000;
    private final AtomicInteger currentTabCount = new AtomicInteger();
    private final AtomicInteger currentActiveTab = new AtomicInteger(); //Текущая активная (выбранная) вкладка
    private final AtomicInteger currentActiveClientId = new AtomicInteger();
    private boolean initialCalls = true;
    /**
     * Skip writing connection type back to state while applying model → GUI.
     */
    private boolean suppressConnectionTypeEvents = false;
    private String lastFolderState = null; // Кешированное состояние папки для избежания лишних repaint

    private JPanel jpMainPanel;
    private JPanel jpAddRemove;
    private JPanel jpConnectionSetup;
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
    private JComboBox<ConnectionType> jcbConnectionType;

    /**
     * Host for connection setup cards (COM form content / HID form); not recreated on switch.
     */
    private JPanel connectionCardsPanel;
    private CardLayout connectionCardsLayout;
    private hidParamForm hidParamForm;
    private wsParamForm wsParamForm;
    /**
     * HID inventory (transport layer, twin of ComPort). Not a polling daemon.
     */
    private final HidPort hidPort = new HidPort();

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

    // Кэш устройства текущего протокола — нужен, чтобы понимать, бинарный ли протокол,
    // без пересоздания объекта на каждое событие ввода.
    private int cachedProtocolIndex = -1;
    private SomeDevice cachedProtocolDevice = null;
    // Устройство, которым построена текущая панель команд (его реестр строит байты).
    private SomeDevice currentPanelDevice = null;
    /**
     * Готовый список команд на индекс протокола. Повторное переключение вкладки его не собирает.
     */
    private final Map<Integer, JScrollPane> commandListCache = new HashMap<>();
    private boolean commandListVisible;

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
    private JButton jbSetTypicalParametrs;
    private JPanel jpProtocol;
    private JPanel jpComPorts;
    private JPanel jpNeedPool;
    private JTextField jtfDevName;
    private JLabel devName;
    private JPanel clientName;
    private JPanel clientSettings;
    private JButton cleanTerminalBtn;
    private JScrollPane jpConnectionSettingsScrollContainer;


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
        if (updateCheckService != null) {
            updateCheckService.start();
        }
        if (launcherUpdateCheckService != null) {
            launcherUpdateCheckService.start();
        }
        //log.info("prop driver " + prop.getDrv());
        JMenuBar menuBar = new JMenuBar();
        JmenuFile menu = new JmenuFile(prop, anyPoolService, answerStorage, updateCheckService, launcherUpdateCheckService);
        menuBar.add(menu.createFileMenu());
        menuBar.add(menu.createSettingsMenu());
        menuBar.add(menu.createViewMenu(uiThPool));
        menuBar.add(menu.createUtilitiesMenu(uiThPool));
        menuBar.add(menu.createControlPanelsMenu());
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
        MainLeftPanelStateCollection.renewInstance(leftPanState);
        ConcurrentHashMap<Integer, MainLeftPanelState> tmp = leftPanState.getClientIdTabState();
        for (Map.Entry<Integer, MainLeftPanelState> entry : tmp.entrySet()) {
            log.info("Command " + entry.getValue().getCommand() + " devName " + entry.getValue().getVisibleName());
        }

        for (Integer i : lastReceivedPositionFromStorageMap.keySet()) {

        }

        log.info("DevName in leftPaneState " + leftPanState.getDevName(
                leftPanState.getClientIdByTabNumber(0)));
        log.info("ClientIdByTabNumber in leftPaneState " +
                leftPanState.getClientIdByTabNumber(0));
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

    /**
     * Fills connection-type combo (COM/HID/WebSocket) and wraps setup panels in CardLayout.
     * Existing COM widgets stay as-is inside {@link #jpConnectionSetup};
     * HID → {@link hidParamForm}; WebSocket → {@link wsParamForm}.
     * Switching only shows the matching card — no rebuild of components.
     */
    private void initConnectionTypeUi() {
        connectionCardsLayout = new CardLayout();
        connectionCardsPanel = new JPanel(connectionCardsLayout);

        Container setupParent = jpConnectionSetup.getParent();
        if (setupParent == null) {
            log.error("jpConnectionSetup has no parent — connection type switcher skipped");
            return;
        }
        setupParent.remove(jpConnectionSetup);
        connectionCardsPanel.add(jpConnectionSetup, ConnectionType.COM.name());

        hidParamForm = new hidParamForm();
        connectionCardsPanel.add(hidParamForm.getRootPanel(), ConnectionType.HID.name());
        initHidDeviceListUi();

        wsParamForm = new wsParamForm();
        connectionCardsPanel.add(wsParamForm.getRootPanel(), ConnectionType.WEBSOCKET.name());
        initWsParamFormUi();

        setupParent.add(connectionCardsPanel, BorderLayout.CENTER);

        DefaultComboBoxModel<ConnectionType> model = new DefaultComboBoxModel<>();
        for (ConnectionType type : ConnectionType.selectableValues()) {
            model.addElement(type);
        }
        jcbConnectionType.setModel(model);
        jcbConnectionType.setSelectedItem(ConnectionType.COM);
        jcbConnectionType.addActionListener(e -> {
            if (suppressConnectionTypeEvents) {
                return;
            }
            ConnectionType selected = (ConnectionType) jcbConnectionType.getSelectedItem();
            persistConnectionTypeForActiveClient(selected);
            showConnectionTypePanel(selected);
        });

        showConnectionTypePanel(ConnectionType.COM);
        log.debug("Connection type UI: cards COM/HID/WEBSOCKET");
    }

    private void persistConnectionTypeForActiveClient(ConnectionType type) {
        if (type == null || leftPanState == null) {
            return;
        }
        int clientId = currentActiveClientId.get();
        if (clientId < 0 || !leftPanState.containClientId(clientId)) {
            return;
        }
        leftPanState.setConnectionType(clientId, type);
        log.debug("connectionType for clientId={} → {}", clientId, type.name());
    }

    /**
     * Restore connection-type combo + CardLayout card from LeftPane state for active client.
     * Called on tab switch (via updateGuiFromClass).
     */
    private void applyConnectionTypeFromModel() {
        if (jcbConnectionType == null || leftPanState == null) {
            return;
        }
        int clientId = currentActiveClientId.get();
        if (clientId < 0 || !leftPanState.containClientId(clientId)) {
            return;
        }
        ConnectionType type = leftPanState.getConnectionType(clientId);
        if (type == null || !type.isSelectable()) {
            type = ConnectionType.COM;
        }
        suppressConnectionTypeEvents = true;
        try {
            jcbConnectionType.setSelectedItem(type);
            if (jcbConnectionType.getSelectedItem() != type) {
                jcbConnectionType.setSelectedItem(ConnectionType.COM);
                type = ConnectionType.COM;
            }
            showConnectionTypePanel(type);
        } finally {
            suppressConnectionTypeEvents = false;
        }
    }

    /**
     * WS panel combos + field defaults (full apply from MyProperties after {@code prop} is set).
     * Connect/poll wiring — later (WebSocketDataCollector / AnyPoolService).
     */
    private void initWsParamFormUi() {
        if (wsParamForm == null) {
            return;
        }
        DefaultComboBoxModel<String> authModel = new DefaultComboBoxModel<>();
        authModel.addElement("Login / Password");
        authModel.addElement("Key");
        wsParamForm.getAuthTypeCombo().setModel(authModel);
        wsParamForm.getAuthTypeCombo().setSelectedIndex(0);

        // Same device protocols as COM for now (MGS/MKRS etc. later via WS path)
        DefaultComboBoxModel<String> protocolModel = new DefaultComboBoxModel<>();
        for (String name : ProtocolsList.getValues()) {
            protocolModel.addElement(name);
        }
        wsParamForm.getProtocolCombo().setModel(protocolModel);

        if ("user".equals(wsParamForm.getTimeoutField().getText())) {
            wsParamForm.getTimeoutField().setText("5000");
        }
    }

    /**
     * Prefill from Vega/WS settings used by {@link WebSocketWindow}.
     */
    private void applyWsFormDefaultsFromProperties() {
        if (wsParamForm == null || prop == null) {
            return;
        }
        String address = prop.getVegaAddress();
        if (address != null && !address.isBlank()) {
            applyWsAddressAndPort(address.trim());
        }
        if (prop.getVegaLogin() != null) {
            wsParamForm.getLoginField().setText(prop.getVegaLogin());
        }
        if (prop.getVegaPassword() != null) {
            wsParamForm.getPasswordField().setText(prop.getVegaPassword());
        }
    }

    /**
     * Accepts full URL {@code ws://host:port/...} or host-only; fills address + port fields.
     */
    private void applyWsAddressAndPort(String raw) {
        String host = raw;
        String port = wsParamForm.getPortField().getText();
        try {
            String stripped = raw;
            if (stripped.startsWith("ws://")) {
                stripped = stripped.substring(5);
            } else if (stripped.startsWith("wss://")) {
                stripped = stripped.substring(6);
            }
            int slash = stripped.indexOf('/');
            if (slash >= 0) {
                stripped = stripped.substring(0, slash);
            }
            int colon = stripped.lastIndexOf(':');
            if (colon > 0 && colon < stripped.length() - 1) {
                host = stripped.substring(0, colon);
                port = stripped.substring(colon + 1);
            } else {
                host = stripped;
            }
        } catch (Exception e) {
            log.debug("WS address parse fallback for '{}': {}", raw, e.getMessage());
            host = raw;
        }
        wsParamForm.getAddressField().setText(host);
        wsParamForm.getPortField().setText(port);
    }

    /**
     * Full URL for collectors / WebSocketDataCollector (same style as Vega address).
     */
    public String buildWsUrlFromForm() {
        if (wsParamForm == null) {
            return prop != null ? prop.getVegaAddress() : "ws://127.0.0.1:8002";
        }
        String host = wsParamForm.getAddressField().getText().trim();
        String port = wsParamForm.getPortField().getText().trim();
        if (host.startsWith("ws://") || host.startsWith("wss://")) {
            return host;
        }
        if (port == null || port.isBlank()) {
            return "ws://" + host;
        }
        return "ws://" + host + ":" + port;
    }

    /**
     * HID list UI: same role as COM port combo + «Обновить список».
     * Scan/filter logic lives in {@link HidPort} (product id filter from Multigassens DeviceRepository).
     * Does not start a polling collector — that is the next step.
     */
    private void initHidDeviceListUi() {
        if (hidParamForm == null) {
            return;
        }
        DefaultComboBoxModel<HidDeviceTypeFilter> maskModel = new DefaultComboBoxModel<>();
        for (HidDeviceTypeFilter filter : HidDeviceTypeFilter.values()) {
            maskModel.addElement(filter);
        }
        hidParamForm.getDeviceTypeMaskCombo().setModel(maskModel);
        hidParamForm.getDeviceTypeMaskCombo().setSelectedItem(HidDeviceTypeFilter.ALL);

        // Form stub labels checkbox as «Лог» — treat as «filter known product ids»
        hidParamForm.getFilterByTypeCheckBox().setText("Только известные");
        hidParamForm.getFilterByTypeCheckBox().setSelected(true);
        hidParamForm.getFilterByTypeCheckBox().setToolTipText(
                "pid MultigasSense=" + Constants.HidCommunication.MULTIGASSENSE_TARGET_PRODUCT_ID
                        + ", MikroSense=" + Constants.HidCommunication.MIKROSENSE_TARGET_PRODUCT_ID);

        hidParamForm.getUpdateListButton().addActionListener(e -> updateHidDeviceList());
        hidParamForm.getFilterByTypeCheckBox().addActionListener(e -> updateHidDeviceList());
        hidParamForm.getDeviceTypeMaskCombo().addActionListener(e -> updateHidDeviceList());
        hidParamForm.getFoundDevicesCombo().addActionListener(e -> {
            Object selected = hidParamForm.getFoundDevicesCombo().getSelectedItem();
            if (selected instanceof HidDeviceEntry entry) {
                hidPort.setDevice(entry);
                log.debug("Выбрано HID: {}", entry.getDisplayLabel());
            }
        });
    }

    private void updateHidDeviceList() {
        if (hidParamForm == null) {
            return;
        }
        hidPort.updateDevices();
        boolean knownOnly = hidParamForm.getFilterByTypeCheckBox().isSelected();
        HidDeviceTypeFilter mask = (HidDeviceTypeFilter) hidParamForm.getDeviceTypeMaskCombo().getSelectedItem();
        Constants.SupportedHidDeviceType typeFilter =
                mask != null ? mask.getDeviceType() : null;

        List<HidDeviceEntry> list = hidPort.listDevices(knownOnly, typeFilter);
        JComboBox<HidDeviceEntry> found = hidParamForm.getFoundDevicesCombo();
        HidDeviceEntry previous = found.getSelectedItem() instanceof HidDeviceEntry e ? e : null;

        DefaultComboBoxModel<HidDeviceEntry> model = new DefaultComboBoxModel<>();
        for (HidDeviceEntry entry : list) {
            model.addElement(entry);
        }
        found.setModel(model);

        if (previous != null) {
            for (int i = 0; i < model.getSize(); i++) {
                if (previous.equals(model.getElementAt(i))) {
                    found.setSelectedIndex(i);
                    hidPort.setDevice(model.getElementAt(i));
                    return;
                }
            }
        }
        if (model.getSize() > 0) {
            found.setSelectedIndex(0);
            hidPort.setDevice(model.getElementAt(0));
        } else {
            hidPort.setDevice((HidDeviceEntry) null);
        }
        log.info("HID list UI: {} item(s), knownOnly={}, mask={}", model.getSize(), knownOnly, mask);
    }

    private void showConnectionTypePanel(ConnectionType type) {
        if (type == null || connectionCardsLayout == null || connectionCardsPanel == null) {
            return;
        }
        connectionCardsLayout.show(connectionCardsPanel, type.name());
        if (type == ConnectionType.HID) {
            updateHidDeviceList();
        }
        connectionCardsPanel.revalidate();
        connectionCardsPanel.repaint();
        log.debug("Показана панель подключения: {}", type);
    }

    public ConnectionType getSelectedConnectionType() {
        Object selected = jcbConnectionType != null ? jcbConnectionType.getSelectedItem() : null;
        return selected instanceof ConnectionType ct ? ct : ConnectionType.COM;
    }


    public MainWindow(MyProperties myProperties, AnyPoolService anyPoolService, MainLeftPanelStateCollection leftPanelStateCollection, ConnectionSettingsService connectionSettingsService, PortLifecycleService portLifecycleService, PollingService pollingService, TabService tabService, AnswerStorage answerStorage, UpdateCheckService updateCheckService, LauncherUpdateCheckService launcherUpdateCheckService) {
        if (leftPanelStateCollection == null) {
            log.warn("В конструктор MainWindow передан null leftPanelStateCollection");
        }
        NimbusCustomizer.customize();
        $$$setupUI$$$();
        initConnectionTypeUi();
        log.debug("Подготовка к рендеру окна....");
        if (anyPoolService == null || myProperties == null) {
            log.warn("В конструктор MainWindow передан null anyPoolService/comPorts/myProperties");
        }

        this.anyPoolService = anyPoolService;
        if (anyPoolService != null) {
            anyPoolService.setLogFileStartedListener(this::onLogFileStarted);
        }
        this.prop = myProperties;
        this.connectionSettingsService = connectionSettingsService;
        this.portLifecycleService = portLifecycleService;
        this.pollingService = pollingService;
        this.tabService = tabService;
        this.answerStorage = answerStorage;
        this.updateCheckService = updateCheckService;
        this.launcherUpdateCheckService = launcherUpdateCheckService;
        applyWsFormDefaultsFromProperties();

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


        guiStateManager = new GuiStateManager(leftPanState, currentActiveTab);
        tabManager = new TabManager(jtpDevicesTerminal, leftPanState, tabService, logDataTransferJtextPanelsMap, lastReceivedPositionFromStorageMap, jbRemoveDev);
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
                lastFolderState = null; // Сброс кеша — у каждой вкладки свой статус папки
                checkIsUsedPort();
                updateFolderPicture();
            }
        });

        currentActiveTab.set(0);
        currentActiveClientId.set(connectionSettingsService.ensureClientId(currentActiveTab.get()));
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
        //
        updateClassFromGui();
        jtpDevicesTerminal.setSelectedIndex(0);
        uiThPool.submit(new RenderThread(this));
        initialCalls = false;
        updateGuiFromClass();
        // Индекс протокола уже выставлен при заполнении списка, повторный setSelectedIndex
        // событие не шлёт. Панель команд без этого вызова на старте не собирается.
        updateProtocol();
    }


    private void updateComPortList() {
        jcbComPorts.removeAllItems();
        SerialPort[] portList = SerialPort.getCommPorts(); // Нативный вызов — только один раз
        for (int i = 0; i < portList.length; i++) {
            SerialPort currentPort = portList[i];
            jcbComPorts.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (currentPort.getSystemPortName().equals(prop.getPorts()[0])) {
                jcbComPorts.setSelectedIndex(i);
            }
        }
    }

    //Подстановка параметров ком-порта для протокола
    private void setTypicalParameters() {
        SomeDevice device = createDeviceByProtocol(ProtocolsList.getLikeArrayEnum(jcbProtocol.getSelectedIndex()));
        if (device instanceof ProtocolComPort) {
            ProtocolComPort castedDevice = (ProtocolComPort) device;

            log.info("Для выбранного устройства типовая скорость: " + castedDevice.getDefaultBaudRate());
            int num = BaudRatesList.getLikeArrayOrderByValue(castedDevice.getDefaultBaudRate());
            log.info("Для выбранного устройства типовая скорость: (номер в списке)" + num);
            jcbBaudRate.setSelectedIndex(num);

            log.info("Для выбранного устройства стандартное значение dataBit: " + castedDevice.getDefaultDataBit());
            num = DataBitsList.getLikeArrayOrderByValue(castedDevice.getDefaultDataBit());
            log.info("Для выбранного устройства стандартное значение dataBit: (номер в списке)" + num);
            jcbDataBits.setSelectedIndex(num);

            log.info("Для выбранного устройства стандартное значение четности: " + castedDevice.getDefaultParity());
            num = ParityList.getLikeArrayOrderByValue(castedDevice.getDefaultParity());
            log.info("Для выбранного устройства стандартное значение четности: (номер в списке)" + num);
            jcbParity.setSelectedIndex(num);

            log.info("Для выбранного устройства стандартное значение StopBit: " + castedDevice.getDefaultStopBit());
            num = StopBitsList.getLikeArrayOrderByValue(castedDevice.getDefaultStopBit());
            log.info("Для выбранного устройства стандартное значение StopBit: (номер в списке)" + num);
            jcbStopBit.setSelectedIndex(num);
        } else {
            log.info("Для выбранного устройства типовая скорость не задана ");
        }
    }

    // Инициализация слушателей
    private void initDocumentListeners() {
        // Text Fields
        ListenerUtils.addDocumentListener(jtfPoolDelay, this::updatePoolDelay);
        ListenerUtils.addDocumentListener(jtfPrefToSend, this::updateTextToSend);
        ListenerUtils.addDocumentListener(jtfTextToSend, this::updateTextToSend);
        ListenerUtils.addDocumentListener(jtfDevName, this::updateDevName);

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
        ListenerUtils.addActionListener(jbSetTypicalParametrs, this::setTypicalParameters);
        ListenerUtils.addActionListener(jbComSearch, this::startComSearch);
        ListenerUtils.addActionListener(cleanTerminalBtn, this::clearTerminal);

        // CheckBoxes
        ListenerUtils.addActionListener(jCbNeedPool, this::updateTextAndSendFromCheckBox);
        ListenerUtils.addActionListener(jCbNeedLog, this::updateLogCheckBox);
    }

    private void clearTerminal() {
        Integer clientId = currentActiveClientId.get();
        JTextPane pane = logDataTransferJtextPanelsMap.get(clientId);
        if (pane == null) {
            return;
        }
        Document doc = pane.getDocument();
        try {
            doc.remove(0, doc.getLength());
        } catch (BadLocationException ex) {
            log.warn("Не удалось очистить терминал клиента {}: {}", clientId, ex.getMessage());
        }
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
        PortLifecycleService.PortOpenResult result = portLifecycleService.openPort(
                currentActiveClientId.get(), getCurrComSelection(), getCurrProtocolSelection());
        addCustomMessage(result.getMessage());
        checkIsUsedPort();
        if (result.isSuccess() && result.getPortSystemName() != null) {
            prop.setPortForTab(result.getPortSystemName(), currentActiveTab.get());
        }
        saveParameters();
    }

    private void closeComPort() {
        addCustomMessage(portLifecycleService.closePort(currentActiveClientId.get()));
        checkIsUsedPort();
        updateFolderPicture();
    }

    /**
     * Поиск сетевых адресов приборов на текущем порту.
     * Для IGM (Modbus RTU), Erstevak MTP4D (ASCII, {@code NNN}) и ECT_TC290 (ASCII, {@code @NN})
     * используется свой диалог. Найденный адрес подставляется в поле «Адрес».
     */
    private void startComSearch() {
        if (!(currentPanelDevice instanceof ProtocolComPort)) {
            addCustomMessage("Поиск сетевых адресов доступен только для устройств с адресом");
            return;
        }
        boolean erstevak = currentPanelDevice instanceof ERSTEVAK_MTP4D;
        boolean ect = currentPanelDevice instanceof ECT_TC290;
        if (!erstevak && !ect && !(currentPanelDevice instanceof SlaveAddressable)) {
            addCustomMessage("Поиск сетевых адресов доступен только для устройств с адресом");
            return;
        }
        Object portItem = jcbComPorts.getSelectedItem();
        if (!(portItem instanceof String) || ((String) portItem).isEmpty()) {
            addCustomMessage("Не выбран COM-порт для поиска");
            return;
        }

        // Берём типовые параметры протокола (для IGM это 19200 8E1, для Erstevak 9600 8E1),
        // а не текущее состояние комбо, чтобы поиск не зависел от того, нажимали ли «типовые параметры».
        ProtocolComPort p = (ProtocolComPort) currentPanelDevice;
        int baud = p.getDefaultBaudRate().getValue();
        int dataBits = p.getDefaultDataBit().getValue();
        int stopBits = p.getDefaultStopBit().getValue();
        int parity = p.getDefaultParity().getValue();
        String portName = ((String) portItem).split(" ")[0];

        if (erstevak) {
            addCustomMessage("Запуск поиска адресов Erstevak: " + portName + ", " + baud + " бод");
            ErstevakSearchDialog dialog = new ErstevakSearchDialog(this, portName, baud, dataBits, stopBits, parity);
            dialog.startSearch();
            dialog.setVisible(true);

            List<ErstevakSearchDialog.FoundDevice> found = dialog.getFoundDevices();
            if (found.isEmpty()) {
                addCustomMessage("Устройства не найдены");
                return;
            }
            ErstevakSearchDialog.FoundDevice first = found.get(0);
            jtfPrefToSend.setText(first.formattedAddress());
            applySlaveAddressFromPrefix();
            addCustomMessage("Найдено устройств: " + found.size() + ". Адрес: " + first.formattedAddress());
            return;
        }

        if (ect) {
            addCustomMessage("Запуск поиска адресов ECT_TC290: " + portName + ", " + baud + " бод");
            EctTc290SearchDialog dialog = new EctTc290SearchDialog(this, portName, baud, dataBits, stopBits, parity);
            dialog.startSearch();
            dialog.setVisible(true);

            List<EctTc290SearchDialog.FoundDevice> found = dialog.getFoundDevices();
            if (found.isEmpty()) {
                addCustomMessage("Устройства не найдены");
                return;
            }
            EctTc290SearchDialog.FoundDevice first = found.get(0);
            jtfPrefToSend.setText(first.formattedAddress());
            applySlaveAddressFromPrefix();
            addCustomMessage("Найдено устройств: " + found.size() + ". Адрес: " + first.formattedAddress());
            return;
        }

        addCustomMessage("Запуск поиска адресов: " + portName + ", " + baud + " бод");
        IgmModbusSearchDialog dialog = new IgmModbusSearchDialog(this, portName, baud, dataBits, stopBits, parity);
        dialog.startSearch();
        dialog.setVisible(true);

        List<IgmModbusSearchDialog.FoundDevice> found = dialog.getFoundDevices();
        if (found.isEmpty()) {
            addCustomMessage("Устройства не найдены");
            return;
        }
        IgmModbusSearchDialog.FoundDevice first = found.get(0);
        jtfPrefToSend.setText(String.valueOf(first.address));
        applySlaveAddressFromPrefix();
        addCustomMessage("Найдено устройств: " + found.size() + ". Адрес: " + first.address);
    }

    private void updateDevName() {
        connectionSettingsService.setDeviceName(currentActiveClientId.get(), jtfDevName.getText());
        if (jtfDevName.getText() != null && jtfDevName.getText().length() <= DEVICE_NAME_LIMIT && jtfDevName.getText().length() > 1) {
            jtpDevicesTerminal.setTitleAt(currentActiveTab.get(), jtfDevName.getText());
        }
    }

    private void updateParity() {
        connectionSettingsService.updateParity(currentActiveClientId.get(), jcbParity.getSelectedIndex());
    }

    private void updateBaudRate() {
        connectionSettingsService.updateBaudRate(currentActiveClientId.get(), jcbBaudRate.getSelectedIndex());
    }

    private void updateStopBit() {
        connectionSettingsService.updateStopBits(currentActiveClientId.get(), jcbStopBit.getSelectedIndex());
    }

    private void updateDataBits() {
        connectionSettingsService.updateDataBits(currentActiveClientId.get(), jcbDataBits.getSelectedIndex());
    }

    private void updateProtocol() {
        leftPanState.setProtocol(currentActiveClientId.get(), jcbProtocol.getSelectedIndex());
        // Устанавливаем BorderLayout для основной панели
        jpSendInput.setLayout(new BorderLayout());

        if (ProtocolsList.getLikeArrayEnum(jcbProtocol.getSelectedIndex()) != null) {
            ProtocolsList protocol = ProtocolsList.getLikeArrayEnum(jcbProtocol.getSelectedIndex());
            SomeDevice device = getCurrentProtocolDevice();
            if (device == null) {
                currentPanelDevice = null;
                return;
            }
            currentPanelDevice = device;
            jbComSearch.setEnabled(protocol == ProtocolsList.ERSTEVAK_MTP4D || device instanceof SlaveAddressable);

            if (device instanceof TemplatedAscii) {
                showTemplatedAsciiPanel(device);
            } else if (!device.isASCII()) {
                showNonAsciiPanel(device);
            } else {
                showAsciiPanel();
            }

            // Устройства с сетевым адресом: поле префикса используется как адрес (1-247).
            if (device instanceof SlaveAddressable) {
                jtfPrefToSend.setEnabled(true);
                jtfPrefToSend.setToolTipText("Сетевой адрес устройства (1-247)");
                applySlaveAddressFromPrefix();
            } else {
                jtfPrefToSend.setToolTipText(null);
            }
        } else {
            currentPanelDevice = null;
        }
    }

    private void showExtendetAsciiPanel(SomeDevice device) {
        int protocolIndex = jcbProtocol.getSelectedIndex();
        if (commandListVisible && commandListCache.containsKey(protocolIndex)
                && jpNonAsciCommandListPanel.getParent() == jpSendInput) {
            return;
        }

        jpSendInput.removeAll();
        jpNonAsciCommandListPanel.setLayout(new BorderLayout());

        JScrollPane scrollPane = commandListCache.get(protocolIndex);
        if (scrollPane == null) {
            scrollPane = buildCommandList(device);
            commandListCache.put(protocolIndex, scrollPane);
        }

        jpNonAsciCommandListPanel.removeAll();
        jpNonAsciCommandListPanel.add(scrollPane, BorderLayout.CENTER);

        jpSendInput.add(jpNonAsciCommandListPanel, BorderLayout.CENTER);
        jpSendInput.add(jpAsciiInput, BorderLayout.SOUTH);

        jpNonAsciCommandListPanel.setMinimumSize(new Dimension(0, 0));
        jpNonAsciCommandListPanel.setPreferredSize(new Dimension(jpSendInput.getWidth(), 100));
        setSendInputPreferredHeight(200);
        commandListVisible = true;

        jpSendInput.revalidate();
        jpSendInput.repaint();
    }

    private JScrollPane buildCommandList(SomeDevice device) {
        HashMap<String, SingleCommand> commandList = device.getCommandListClass().getCommandPool();

        // Основная панель с вертикальным расположением
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        for (Map.Entry<String, SingleCommand> entry : commandList.entrySet()) {
            // Панель для отдельной команды
            JPanel commandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            commandPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            JLabel commandLabel = new JLabel(entry.getValue().getDescription());
            commandPanel.add(commandLabel);

            List<ArgumentDescriptor> arguments = entry.getValue().getArguments();
            Map<String, JTextField> argsInput = new ConcurrentHashMap<>();
            Map<String, Object> argsValue = new ConcurrentHashMap<>();
            for (ArgumentDescriptor arg : arguments) {

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
                    String text = argsInput.get(stringObjectEntry.getKey()).getText();
                    ArgumentDescriptor argDesc = entry.getValue().getArguments().stream()
                            .filter(a -> a.getName().equals(stringObjectEntry.getKey()))
                            .findFirst().orElse(null);
                    if (argDesc != null && argDesc.getType() == String.class) {
                        argsValue.put(stringObjectEntry.getKey(), text);
                    } else {
                        Float argument = 0.0f;
                        try {
                            argument = Float.parseFloat(text);
                            argsValue.put(stringObjectEntry.getKey(), argument);
                        } catch (NumberFormatException ex) {
                            log.warn("Error when parse argument " + ex.getMessage());
                            return;
                        }
                    }
                }


                byte[] cmdForSend = entry.getValue().build(argsValue);
                boolean isTemplatedAscii = device instanceof TemplatedAscii;
                String fieldText = CommandFieldFormatter.toFieldText(cmdForSend, isTemplatedAscii);
                log.info("Set command in input field to " + fieldText
                        + " (hex " + MyUtilities.bytesToHexString(cmdForSend) + ")");
                jtfTextToSend.setText(fieldText);
                leftPanState.setRawCommand(currentActiveClientId.get(), cmdForSend);
                log.info("Saved RAW command: " + MyUtilities.bytesToHexString(cmdForSend));
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
        return scrollPane;
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

    private void setSendInputPreferredHeight(int height) {
        int width = jpSendInput.getPreferredSize().width;
        if (width <= 0) {
            width = 450;
        }
        jpSendInput.setPreferredSize(new Dimension(width, height));
        Container parent = jpSendInput.getParent();
        if (parent != null) {
            parent.revalidate();
        }
    }

    private void showAsciiPanel() {
        jtfTextToSend.setEnabled(true);
        jtfPrefToSend.setEnabled(true);
        if (!commandListVisible && jpAsciiInput.getParent() == jpSendInput) {
            return;
        }

        // Убираем все компоненты с sendInpuntJpane
        jpSendInput.removeAll();

        // Добавляем только asciiInput
        jpSendInput.add(jpAsciiInput, BorderLayout.CENTER);

        // Очищаем и скрываем nonAsciCommandListPanel
        jpNonAsciCommandListPanel.removeAll();
        jpNonAsciCommandListPanel.setPreferredSize(new Dimension(0, 0));
        commandListVisible = false;
        setSendInputPreferredHeight(jpAsciiInput.getPreferredSize().height);

        jpSendInput.revalidate();
        jpSendInput.repaint();
    }

    // Обработчики действий
    private void updatePoolDelay() {
        pollingService.updatePoolDelay(currentActiveClientId.get(), getPoolDelayFromGui());
    }

    private void updateTextAndSendFromEnter() {
        updateTextToSend();
        startSend(true);

        if (uiThPool.isShutdown() || uiThPool.isTerminated()) {
            log.info("Перезапуск пула потоков рендера...");
            uiThPool = Executors.newSingleThreadExecutor();
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
            applySlaveAddressFromPrefix();
            updateRawCommandFromField();
        }
    }

    /**
     * Для устройств с {@link SlaveAddressable} поле префикса трактуется как сетевой адрес (1-247).
     * Адрес применяется к реестру панель-девайса, поэтому следующая построенная команда
     * уйдёт с нужным slave-адресом.
     */
    private void applySlaveAddressFromPrefix() {
        if (!(currentPanelDevice instanceof SlaveAddressable)) {
            return;
        }
        ((SlaveAddressable) currentPanelDevice).setSlaveAddress(parseSlaveAddress(jtfPrefToSend.getText()));
    }

    private static int parseSlaveAddress(String text) {
        if (text == null) {
            return 1;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return 1;
        }
        try {
            int address = Integer.parseInt(trimmed);
            if (address >= 1 && address <= 247) {
                return address;
            }
        } catch (NumberFormatException ignored) {
        }
        return 1;
    }

    /**
     * Для бинарных (не-ASCII) протоколов в текстовом поле GUI лежит hex-представление команды
     * (только для справки). Наполняем rawCommand из него, чтобы отправка работала и после
     * перезапуска без повторного нажатия «задать». Для ASCII-протоколов rawCommand не используется.
     */
    private void updateRawCommandFromField() {
        SomeDevice device = getCurrentProtocolDevice();
        if (device == null || device.isASCII()) {
            return;
        }
        int clientId = currentActiveClientId.get();
        if (clientId < 0 || !leftPanState.containClientId(clientId)) {
            return;
        }
        byte[] raw = tryParseHex(jtfTextToSend.getText());
        if (raw != null) {
            leftPanState.setRawCommand(clientId, raw);
        }
    }

    private SomeDevice getCurrentProtocolDevice() {
        int protocolIndex = jcbProtocol.getSelectedIndex();
        if (protocolIndex != cachedProtocolIndex) {
            cachedProtocolIndex = protocolIndex;
            try {
                cachedProtocolDevice = createDeviceByProtocol(ProtocolsList.getLikeArrayEnum(protocolIndex));
            } catch (RuntimeException e) {
                log.warn("Не удалось определить устройство протокола по индексу " + protocolIndex + ": " + e.getMessage());
                cachedProtocolDevice = null;
            }
        }
        return cachedProtocolDevice;
    }

    private static byte[] tryParseHex(String text) {
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        for (String part : trimmed.split("\\s+")) {
            if (!part.matches("[0-9A-Fa-f]{1,2}")) {
                return null;
            }
        }
        try {
            return MyUtilities.hexStringToBytes(trimmed);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void updateLogCheckBox() {
        pollingService.toggleLog(currentActiveClientId.get(), jCbNeedLog.isSelected());
        updateFolderPicture();
    }

    // Вспомогательные методы
    private ComDataCollector getComDataCollectorSafe() {
        return pollingService.getCollector(currentActiveTab.get(), leftPanState.getSize());
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
        PollingService.StartSendResult result = pollingService.startPolling(
                currentActiveClientId.get(), getCurrComSelection(), getCurrProtocolSelection(),
                getNeedPoolState(), isBtn, getCurrPoolDelay());
        if (!result.isSuccess()) {
            addCustomMessage(result.getErrorMessage());
        }
        updateFolderPictureMethod();
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
        return connectionSettingsService.ensureClientId(currentActiveTab.get());
    }

    private void updateGuiFromClass() {
        guiStateManager.updateGuiFromModel(
                jcbDataBits, jcbParity, jcbStopBit,
                jcbBaudRate, jcbProtocol, jtfTextToSend,
                jtfPrefToSend, jtfDevName
        );
        applyConnectionTypeFromModel();
    }


    private void updateClassFromGui() {
        guiStateManager.updateModelFromGui(
                jcbParity, jcbDataBits, jcbStopBit,
                jcbBaudRate, jcbProtocol, jtfTextToSend,
                jtfPrefToSend, jtfDevName
        );
    }

    private void readAndUpdateInputPrefAndCommandValues() {
        if (leftPanState.containClientId(currentActiveTab.get())) {
            pollingService.updateCommand(currentActiveClientId.get(),
                    leftPanState.getPrefix(currentActiveClientId.get()),
                    leftPanState.getCommand(currentActiveClientId.get()));
            saveParameters();
        }
    }

    private void checkIsUsedPort() {
        PortLifecycleService.PortStatus status = portLifecycleService.checkPortStatus(
                getCurrComSelection(), currentActiveTab.get());

        jCbNeedPool.setEnabled(status.isPortInUse());
        jbTerminalSend.setEnabled(status.isPortInUse());
        jbComOpen.setEnabled(!status.isPortInUse());
        jcbProtocol.setEnabled(!status.isPortInUse());
        jbComClose.setEnabled(status.isPortInUse());

        if (status.getWarningMessage() != null) {
            addCustomMessage(status.getWarningMessage());
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

    /**
     * Колбэк потока опроса: файл лога этого clientId уже создан.
     * Иконка меняется только если эта вкладка сейчас на экране.
     */
    private void onLogFileStarted(int clientId) {
        SwingUtilities.invokeLater(() -> {
            if (clientId != currentActiveClientId.get()) {
                return;
            }
            lastFolderState = null;
            updateFolderPictureMethod();
        });
    }

    public void updateFolderPicture() {
        if (!initialCalls) {
            updateFolderPictureMethod();
        }
    }

    public void updateFolderPictureMethod() {
        ComDataCollector cdc = anyPoolService.findComDataCollectorByClientId(currentActiveClientId.get());
        String newState;

        if (cdc != null) {
            DeviceLogger currentLogger = cdc.getLogger(currentActiveClientId.get());
            if (currentLogger != null) {
                newState = "open:" + currentLogger.getLogFileCSV();
            } else {
                newState = "not_started";
            }
        } else {
            newState = "not_available";
        }

        // Пропускаем перерисовку если состояние не изменилось
        if (newState.equals(lastFolderState)) {
            return;
        }
        lastFolderState = newState;

        FolderPictureForLog fpg = new FolderPictureForLog();
        jpFolderIconPanel.setLayout(new BoxLayout(jpFolderIconPanel, BoxLayout.Y_AXIS));
        jpFolderIconPanel.removeAll();

        JPanel btnPane;
        if (cdc != null && cdc.getLogger(currentActiveClientId.get()) != null) {
            btnPane = fpg.getPicContainer("Open", true, true, cdc.getLogger(currentActiveClientId.get()).getLogFileCSV());
        } else if (cdc != null) {
            btnPane = fpg.getPicContainer("Not available", true, false, null);
        } else {
            btnPane = fpg.getPicContainer("Not available", false, false, null);
        }

        jpFolderIconPanel.add(btnPane);
        jpFolderIconPanel.revalidate();
        jpFolderIconPanel.repaint();
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
            int queueOffsetInt = answerStorage.getQueueOffset(clientId);

            // Синхронизация доступа к позиции
            if (lastPosition < queueOffsetInt) {
                lastPosition = queueOffsetInt;
                lastReceivedPositionFromStorageMap.put(clientId, lastPosition);
            }

            TabAnswerPart an = answerStorage.getAnswersQueForTab(lastPosition, clientId, true);

            if (an.getAnswerPart() == null || an.getAnswerPart().isEmpty()) {
                //log.info("Нет новых данных для клиента [" + clientId + "]");
                return;
            }
            //log.info("Будут отображены новые данные для клиента [" + clientId + "], позиция: " + an.getPosition());

            // Обновляем позицию атомарно
            lastReceivedPositionFromStorageMap.put(clientId, an.getPosition());


            trimOldestLogRecords(doc, an.getAnswerPart().length(), maxLength);
            doc.insertString(doc.getLength(), an.getAnswerPart(), null);

            // Автоскролл к новому содержимому
            logDataTransferJtextPanelsMap.get(clientId).setCaretPosition(doc.getLength());

        } catch (BadLocationException ex) {
            log.warn("Произошло исключение при рендере окна: " + ex.getMessage());
        }
    }

    private void trimOldestLogRecords(Document doc, int incomingLength, int maxLength) throws BadLocationException {
        if (doc.getLength() + incomingLength <= maxLength) {
            return;
        }
        String current = doc.getText(0, doc.getLength());
        String kept = TerminalLogTrimmer.fit(
                current,
                incomingLength,
                maxLength,
                TerminalLogTrimmer.OLDEST_RECORDS_TO_DROP);
        if (kept.length() == current.length()) {
            return;
        }
        doc.remove(0, doc.getLength());
        if (!kept.isEmpty()) {
            doc.insertString(0, kept, null);
        }
    }

    @Override
    public boolean isEnable() {
        return true;
    }

    //Смена контекста
    public void updateServices(MyProperties newProps, AnyPoolService newService, MainLeftPanelStateCollection leftPanelStateCollection) {
        this.prop = newProps;
        this.anyPoolService = newService;
        this.leftPanState = leftPanelStateCollection;
    }

    // FIXME: saveParameters() вызывается на каждое нажатие клавиши (через DocumentListener → readAndUpdateInputPrefAndCommandValues).
    //  Нужно сравнивать текущее состояние с последним сохранённым и писать на диск ТОЛЬКО при реальных изменениях.
    private void saveParameters() {
        log.debug("Обновление файла настроек со вкладки" + currentActiveTab + " и ИД клиента " + currentActiveClientId.get());
        prop.setLastLeftPanel(leftPanState);
        prop.setLogLevel(
                ((Logger) LoggerFactory.getLogger(
                        org.slf4j.Logger.ROOT_LOGGER_NAME)).getLevel());
        prop.setTabCounter(currentTabCount.get());
        prop.setIdentAndTabBounding(answerStorage.getDeviceTabPair());

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
        jpMainPanel.setLayout(new GridLayoutManager(1, 1, new Insets(2, 2, 2, 2), -1, -1));
        Font jpMainPanelFont = UIManager.getFont("Tree.font");
        if (jpMainPanelFont != null) jpMainPanel.setFont(jpMainPanelFont);
        jpMainPanel.setMaximumSize(new Dimension(-1, -1));
        jpMainPanel.setMinimumSize(new Dimension(530, 530));
        jpMainPanel.setPreferredSize(new Dimension(800, 650));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new FormLayout("fill:d:grow,left:4dlu:noGrow,fill:d:grow", "center:d:grow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow,top:4dlu:noGrow,center:max(d;4px):noGrow"));
        jpMainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(450, 400), new Dimension(800, 600), null, 0, false));
        jpTerminalHistory = new JPanel();
        jpTerminalHistory.setLayout(new BorderLayout(-1, -1));
        jpTerminalHistory.setPreferredSize(new Dimension(8000, 65));
        CellConstraints cc = new CellConstraints();
        panel1.add(jpTerminalHistory, cc.xywh(3, 1, 1, 5, CellConstraints.FILL, CellConstraints.FILL));
        jpSendInput = new JPanel();
        jpSendInput.setLayout(new BorderLayout(0, 0));
        jpSendInput.setMinimumSize(new Dimension(450, 70));
        jpSendInput.setPreferredSize(new Dimension(450, 70));
        jpTerminalHistory.add(jpSendInput, BorderLayout.NORTH);
        jpNonAsciCommandListPanel = new JPanel();
        jpNonAsciCommandListPanel.setLayout(new BorderLayout(0, 0));
        jpNonAsciCommandListPanel.setMinimumSize(new Dimension(0, 200));
        jpSendInput.add(jpNonAsciCommandListPanel, BorderLayout.WEST);
        jpAsciiInput = new JPanel();
        jpAsciiInput.setLayout(new GridLayoutManager(2, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpAsciiInput.setMinimumSize(new Dimension(450, 75));
        jpAsciiInput.setOpaque(true);
        jpAsciiInput.setPreferredSize(new Dimension(450, 75));
        jpSendInput.add(jpAsciiInput, BorderLayout.CENTER);
        jtfPrefToSend = new JTextField();
        jtfPrefToSend.setPreferredSize(new Dimension(60, 40));
        jtfPrefToSend.setText("");
        jpAsciiInput.add(jtfPrefToSend, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(10, 35), new Dimension(40, 35), new Dimension(80, 35), 0, false));
        jbTerminalSend = new JButton();
        jbTerminalSend.setMargin(new Insets(0, 0, 0, 0));
        jbTerminalSend.setText("Отправить");
        jpAsciiInput.add(jbTerminalSend, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, 1, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 35), new Dimension(-1, 35), new Dimension(-1, 35), 0, false));
        jtfTextToSend = new JTextField();
        jtfTextToSend.setMargin(new Insets(2, 9, 2, 6));
        jtfTextToSend.setPreferredSize(new Dimension(100, 40));
        jtfTextToSend.setText("M^");
        jpAsciiInput.add(jtfTextToSend, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(25, 35), new Dimension(900, 35), new Dimension(-1, 35), 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Адрес");
        jpAsciiInput.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Команда");
        jpAsciiInput.add(label2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpTerminalLogPanel = new JPanel();
        jpTerminalLogPanel.setLayout(new BorderLayout(0, 0));
        jpTerminalHistory.add(jpTerminalLogPanel, BorderLayout.CENTER);
        jtpDevicesTerminal = new JTabbedPane();
        jpTerminalLogPanel.add(jtpDevicesTerminal, BorderLayout.CENTER);
        jpConnectionSettingsScrollContainer = new JScrollPane();
        jpConnectionSettingsScrollContainer.setMaximumSize(new Dimension(300, 650));
        jpConnectionSettingsScrollContainer.setMinimumSize(new Dimension(300, 5));
        jpConnectionSettingsScrollContainer.setPreferredSize(new Dimension(300, 650));
        panel1.add(jpConnectionSettingsScrollContainer, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.TOP));
        jpConnectionSettingsScrollContainer.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(), null, TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION, null, null));
        jpConnectionSettings = new JPanel();
        jpConnectionSettings.setLayout(new BorderLayout(0, 0));
        jpConnectionSettings.setMaximumSize(new Dimension(280, 600));
        jpConnectionSettings.setMinimumSize(new Dimension(280, 100));
        jpConnectionSettings.setPreferredSize(new Dimension(280, 600));
        jpConnectionSettingsScrollContainer.setViewportView(jpConnectionSettings);
        jpConnectionSetup = new JPanel();
        jpConnectionSetup.setLayout(new GridLayoutManager(14, 1, new Insets(0, 0, 0, 0), 0, 0));
        jpConnectionSetup.setEnabled(true);
        jpConnectionSetup.setMaximumSize(new Dimension(280, 500));
        jpConnectionSetup.setMinimumSize(new Dimension(280, 400));
        jpConnectionSetup.setPreferredSize(new Dimension(280, 450));
        jpConnectionSettings.add(jpConnectionSetup, BorderLayout.CENTER);
        jpDataBits = new JPanel();
        jpDataBits.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpDataBits, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComDataBits = new JLabel();
        jlbComDataBits.setText("Биты данных");
        jpDataBits.add(jlbComDataBits, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbDataBits = new JComboBox();
        jpDataBits.add(jcbDataBits, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer1 = new Spacer();
        jpDataBits.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpParity = new JPanel();
        jpParity.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpParity, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComParity = new JLabel();
        jlbComParity.setText("Чётность");
        jpParity.add(jlbComParity, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbParity = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel1 = new DefaultComboBoxModel();
        jcbParity.setModel(defaultComboBoxModel1);
        jpParity.add(jcbParity, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer2 = new Spacer();
        jpParity.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpComStopBit = new JPanel();
        jpComStopBit.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpComStopBit, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComStopBit = new JLabel();
        jlbComStopBit.setText("Стоп бит");
        jpComStopBit.add(jlbComStopBit, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbStopBit = new JComboBox();
        jpComStopBit.add(jcbStopBit, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer3 = new Spacer();
        jpComStopBit.add(spacer3, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpOpenClose = new JPanel();
        jpOpenClose.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpOpenClose, new GridConstraints(9, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(240, 29), new Dimension(240, 29), null, 0, false));
        jbComOpen = new JButton();
        jbComOpen.setAlignmentY(0.0f);
        jbComOpen.setHideActionText(false);
        jbComOpen.setText("Открыть");
        jpOpenClose.add(jbComOpen, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, 25), new Dimension(119, 25), new Dimension(200, 200), 0, false));
        jbComClose = new JButton();
        jbComClose.setAlignmentY(0.0f);
        jbComClose.setText("Закрыть");
        jpOpenClose.add(jbComClose, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, new Dimension(100, 25), new Dimension(119, 25), new Dimension(200, 200), 0, false));
        jpComPorts = new JPanel();
        jpComPorts.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpComPorts, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComPorts = new JLabel();
        jlbComPorts.setText("Порт");
        jpComPorts.add(jlbComPorts, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbComPorts = new JComboBox();
        jcbComPorts.setEditable(false);
        final DefaultComboBoxModel defaultComboBoxModel2 = new DefaultComboBoxModel();
        jcbComPorts.setModel(defaultComboBoxModel2);
        jpComPorts.add(jcbComPorts, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer4 = new Spacer();
        jpComPorts.add(spacer4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpProtocol = new JPanel();
        jpProtocol.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpProtocol, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComProtocol = new JLabel();
        jlbComProtocol.setText("Протокол");
        jpProtocol.add(jlbComProtocol, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbProtocol = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel3 = new DefaultComboBoxModel();
        jcbProtocol.setModel(defaultComboBoxModel3);
        jpProtocol.add(jcbProtocol, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer5 = new Spacer();
        jpProtocol.add(spacer5, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jpNeedPool = new JPanel();
        jpNeedPool.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpNeedPool, new GridConstraints(11, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jCbNeedPool = new JCheckBox();
        jCbNeedPool.setText("Опрос  ");
        jpNeedPool.add(jCbNeedPool, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("мс");
        jpNeedPool.add(label3, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jtfPoolDelay = new JTextField();
        jtfPoolDelay.setText("1000");
        jpNeedPool.add(jtfPoolDelay, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        jpAddRemove = new JPanel();
        jpAddRemove.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(jpAddRemove, new GridConstraints(12, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(240, 29), new Dimension(240, 29), null, 0, false));
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
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(panel2, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        jbComUpdateList = new JButton();
        jbComUpdateList.setHideActionText(false);
        jbComUpdateList.setText("Обновить список портов");
        panel2.add(jbComUpdateList, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(260, 25), new Dimension(400, 200), 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(panel3, new GridConstraints(10, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        jbComSearch = new JButton();
        jbComSearch.setAlignmentY(0.0f);
        jbComSearch.setAutoscrolls(false);
        jbComSearch.setHideActionText(false);
        jbComSearch.setText("Поиск сетевых адресов");
        panel3.add(jbComSearch, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(260, 25), new Dimension(400, 200), 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        panel4.setEnabled(true);
        jpConnectionSetup.add(panel4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jlbComBaudRate = new JLabel();
        jlbComBaudRate.setBackground(new Color(-11184811));
        jlbComBaudRate.setText("Скорость   ");
        panel4.add(jlbComBaudRate, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbBaudRate = new JComboBox();
        jcbBaudRate.setEnabled(true);
        panel4.add(jcbBaudRate, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer6 = new Spacer();
        panel4.add(spacer6, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(panel5, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, 1, 1, null, null, null, 0, false));
        jCbNeedLog = new JCheckBox();
        jCbNeedLog.setEnabled(true);
        jCbNeedLog.setText("Лог");
        panel5.add(jCbNeedLog, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jCbAutoConnect = new JCheckBox();
        jCbAutoConnect.setEnabled(false);
        jCbAutoConnect.setText("Автоподключение");
        panel5.add(jCbAutoConnect, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jpFolderIconPanel = new JPanel();
        jpFolderIconPanel.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpFolderIconPanel.setEnabled(true);
        panel5.add(jpFolderIconPanel, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionSetup.add(panel6, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, 1, 1, null, null, null, 0, false));
        jbSetTypicalParametrs = new JButton();
        jbSetTypicalParametrs.setHorizontalTextPosition(0);
        jbSetTypicalParametrs.setText("Задать стандартные параметры");
        jbSetTypicalParametrs.setToolTipText("Задает параметры скорости для выбранного протокола");
        panel6.add(jbSetTypicalParametrs, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(260, 25), new Dimension(400, 200), 0, false));
        cleanTerminalBtn = new JButton();
        cleanTerminalBtn.setText("Очистить терминал");
        jpConnectionSetup.add(cleanTerminalBtn, new GridConstraints(13, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(260, 25), new Dimension(400, 200), 0, false));
        clientSettings = new JPanel();
        clientSettings.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        clientSettings.setMinimumSize(new Dimension(280, 80));
        clientSettings.setPreferredSize(new Dimension(280, 80));
        jpConnectionSettings.add(clientSettings, BorderLayout.NORTH);
        jpConnectionType = new JPanel();
        jpConnectionType.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        jpConnectionType.setEnabled(true);
        clientSettings.add(jpConnectionType, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        jlbConnectionType = new JLabel();
        jlbConnectionType.setText("Тип соединения");
        jpConnectionType.add(jlbConnectionType, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        jcbConnectionType = new JComboBox();
        final DefaultComboBoxModel defaultComboBoxModel4 = new DefaultComboBoxModel();
        jcbConnectionType.setModel(defaultComboBoxModel4);
        jpConnectionType.add(jcbConnectionType, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
        final Spacer spacer7 = new Spacer();
        jpConnectionType.add(spacer7, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        clientName = new JPanel();
        clientName.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        clientSettings.add(clientName, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        devName = new JLabel();
        devName.setText("Имя вкладки");
        clientName.add(devName, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer8 = new Spacer();
        clientName.add(spacer8, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        jtfDevName = new JTextField();
        clientName.add(jtfDevName, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(160, -1), new Dimension(160, -1), new Dimension(160, -1), 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return jpMainPanel;
    }


}
