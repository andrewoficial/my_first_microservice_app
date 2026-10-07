package org.example.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.accu10fd.Acu10fdWindow;
import org.example.gui.curve.CurveHandlerWindow;
import org.example.gui.devices.arduino.emulator.mipex.MipexEmuMain;
import org.example.gui.devices.arduino.feeboard.control.FeeBoardMain;
import org.example.gui.devices.arduino.feeboard.emulation.FeeBoardTestFrame;
import org.example.gui.devices.binder.camera.control.BinderControlPanel;
import org.example.gui.devices.binder.camera.emulation.BinderEmulatorFrame;
import org.example.gui.devices.bkm4.control.Bkm4Main;
import org.example.gui.devices.bkm4.emulation.Bkm4EmulatorFrame;
import org.example.gui.devices.boto800.serial.control.Boto800Main;
import org.example.gui.devices.boto800.serial.emulation.Boto800EmulatorFrame;
import org.example.gui.devices.boto120.serial.control.Boto120Main;
import org.example.gui.devices.boto120.serial.emulation.Boto120EmulatorFrame;
import org.example.gui.devices.boto800.tcp.emulation.Boto800TcpEmulatorFrame;
import org.example.gui.devices.fnirsi.dps150.control.FnirsiDps150Main;
import org.example.gui.devices.fnirsi.dps150.emulation.FnirsiDps150EmulatorFrame;
import org.example.gui.devices.esp32.kantser.emu.ble.KantserBleMain;
import org.example.gui.devices.edvards.d39730880.control.d39730880Main;
import org.example.gui.devices.edvards.d39730880.emulation.EdwardsTicTestFrame;
import org.example.gui.devices.qidian.qdl80a.control.Qdl80aMain;
import org.example.gui.devices.qidian.qdl80a.emulation.Qdl80aTestFrame;
import org.example.gui.devices.stu.mcps.control.spbStuMcpsMain;
import org.example.gui.devices.stu.mcps.emulation.McpsTestFrame;
import org.example.gui.devices.testa.control.TestaControlPanel;
import org.example.gui.devices.testa.emulation.TestaEmulatorFrame;
import org.example.gui.devices.tt5166.control.TT5166Main;
import org.example.gui.devices.tt5166.emulation.TT5166EmulatorFrame;
import org.example.gui.graph.ChartWindow;
import org.example.gui.graph.data.AnswerLoader;
import org.example.gui.mgstest.MultigassensWindow;
import org.example.gui.settings.appearance.AppearanceSettingsWindow;
import org.example.gui.settings.server.ServerSettingsWindow;
import org.example.gui.settings.updates.UpdateSettingsWindow;
import org.example.gui.system.logs.ViewLogsWindow;
import org.example.gui.system.resources.DebugWindow;
import org.example.services.AnswerStorage;
import org.example.services.LauncherUpdateCheckService;
import org.example.services.UpdateCheckService;
import org.example.services.connectionPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;
import ru.kantser.gui.ShowcaseFrame;


@Slf4j
public class JmenuFile {
    private MyProperties prop;
    private final AnyPoolService anyPoolService;
    private final AnswerLoader answerLoader = new AnswerLoader();
    private final AnswerStorage answerStorage;
    private final UpdateCheckService updateCheckService;
    private final LauncherUpdateCheckService launcherUpdateCheckService;

    public JmenuFile (MyProperties extProp, AnyPoolService anyPoolService, AnswerStorage answerStorage){
        this(extProp, anyPoolService, answerStorage, null, null);
    }

    public JmenuFile (MyProperties extProp, AnyPoolService anyPoolService, AnswerStorage answerStorage,
                      UpdateCheckService updateCheckService){
        this(extProp, anyPoolService, answerStorage, updateCheckService, null);
    }

    public JmenuFile (MyProperties extProp, AnyPoolService anyPoolService, AnswerStorage answerStorage,
                      UpdateCheckService updateCheckService,
                      LauncherUpdateCheckService launcherUpdateCheckService){
        super();
        this.prop = extProp;
        this.anyPoolService = anyPoolService;
        this.answerStorage = answerStorage;
        this.updateCheckService = updateCheckService;
        this.launcherUpdateCheckService = launcherUpdateCheckService;
        if(anyPoolService == null){
            log.warn("В конструктор JmenuFile передан null anyPoolService");
        }
    }


    public JMenu createFileMenu()
    {
        // Создание выпадающего меню
        JMenu file = new JMenu("Файл");
        // Пункт меню "Открыть" с изображением
        JMenuItem open = new JMenuItem("Открыть",
                new ImageIcon("images/open.png"));
        // Пункт меню из команды с выходом из программы
        JMenuItem exit = new JMenuItem(new ExitAction());
        // Добавление к пункту меню изображения
        exit.setIcon(new ImageIcon("images/exit.png"));
        // Добавим в меню пункта open
        file.add(open);
        // Добавление разделителя
        file.addSeparator();
        file.add(exit);

        open.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println ("ActionListener.actionPerformed : open");
            }
        });
        return file;
    }

    /**
     * Вложенный класс завершения работы приложения
     */
    class ExitAction extends AbstractAction
    {
        private static final long serialVersionUID = 1L;
        ExitAction() {
            putValue(NAME, "Выход");
        }
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }



    /**
     * Функция создания меню "Система"
     */
    public JMenu createSystemParametrs(ExecutorService thPool)
    {
        // создадим выпадающее меню
        JMenu viewMenu = new JMenu("Система");
        // меню-флажки
        JMenuItem sysDebug  = new JMenuItem("Ресурсы системы");
        JMenuItem sysLogs  = new JMenuItem("Просмотр логов");
        // добавим все в меню
        viewMenu.add(sysDebug);
        viewMenu.add(sysLogs);
        sysDebug.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Debug Window");
                DebugWindow logWindows = DebugWindow.getInstance();
                if (!logWindows.isVisible()) {
                    logWindows.setName("Debug Window");
                    logWindows.setTitle("Debug Window");
                    logWindows.pack();
                    logWindows.setVisible(true);
                    logWindows.startMonitor();
                    thPool.submit(new RenderThread(logWindows));
                } else {
                    logWindows.toFront();
                }


            }
        });
        sysLogs.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                ViewLogsWindow logWindows = new ViewLogsWindow();
                logWindows.setName("Logs Window");
                logWindows.setTitle("Logs Window");
                logWindows.pack();
                logWindows.setModal(false);
                logWindows.setVisible(true);
                thPool.submit(new RenderThread(logWindows));
            }
        });
        return viewMenu;
    }

    /**
     * Функция создания меню "Система"
     */
    public JMenu createInfo(ExecutorService thPool)
    {
        // создадим выпадающее меню
        JMenu viewMenu = new JMenu("Справка");
        // меню-флажки
        JMenuItem sysAbout  = new JMenuItem("О программе");
        JMenuItem sysUpdate  = new JMenuItem("Проверка обновлений программы");
        JMenuItem sysLauncherUpdate  = new JMenuItem("Проверка обновлений лаунчера");
        // добавим все в меню
        viewMenu.add(sysAbout);
        viewMenu.add(sysUpdate);
        viewMenu.add(sysLauncherUpdate);

        sysAbout.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("About Window");
                About about = new About();
                about.setVisible(true);
            }
        });
        sysUpdate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Update Window");
                UpdateWindow updateWindow = new UpdateWindow();
                updateWindow.setName("Update Window");
                updateWindow.setTitle("Update Window");
                updateWindow.pack();
                updateWindow.setModal(false);
                updateWindow.setVisible(true);
                thPool.submit(new RenderThread(updateWindow));


            }
        });
        sysLauncherUpdate.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Update Launcher Window");
                UpdateLauncherWindow launcherWindow = new UpdateLauncherWindow();
                launcherWindow.setName("Update Launcher Window");
                launcherWindow.setTitle("Обновление лаунчера");
                launcherWindow.pack();
                launcherWindow.setModal(false);
                launcherWindow.setVisible(true);
                thPool.submit(new RenderThread(launcherWindow));
            }
        });
        installUpdateIndicators(viewMenu, sysUpdate, updateCheckService,
                sysLauncherUpdate, launcherUpdateCheckService);
        return viewMenu;
    }

    /**
     * Тихий индикатор обновлений: периодически опрашивает оба сервиса
     * ({@link UpdateCheckService} и {@link LauncherUpdateCheckService}) и, если проверка
     * завершилась успешно и есть новая версия, дописывает жёлтую точку в конце текста
     * соответствующего пункта. У меню «Справка» точка появляется, если обновление есть
     * хотя бы для одного из них (хлебные крошки). При неудачной проверке интерфейс не меняется.
     */
    private void installUpdateIndicators(JMenu parentMenu,
                                         JMenuItem programItem, UpdateCheckService programService,
                                         JMenuItem launcherItem, LauncherUpdateCheckService launcherService) {
        if (programService == null && launcherService == null) {
            return;
        }
        final String baseProgram = programItem.getText();
        final String baseLauncher = launcherItem.getText();
        final String baseMenu = parentMenu.getText();
        final String dotColor = "#E6A700";
        final javax.swing.Timer timer = new javax.swing.Timer(1500, null);
        timer.addActionListener(e -> {
            boolean programPending = programService != null && programService.isPending();
            boolean launcherPending = launcherService != null && launcherService.isPending();
            if (programPending || launcherPending) {
                return;
            }
            boolean programShow = programService != null && programService.hasUpdate();
            boolean launcherShow = launcherService != null && launcherService.hasUpdate();
            programItem.setText(withUpdateDot(baseProgram, dotColor, programShow));
            launcherItem.setText(withUpdateDot(baseLauncher, dotColor, launcherShow));
            parentMenu.setText(withUpdateDot(baseMenu, dotColor, programShow || launcherShow));
            timer.stop();
        });
        timer.setInitialDelay(0);
        timer.start();
    }

    /** Возвращает текст с жёлтой точкой в конце (справа от слова) либо исходный текст. */
    private static String withUpdateDot(String text, String color, boolean show) {
        if (!show || text == null) {
            return text;
        }
        return "<html>" + text + " <font color='" + color + "'>\u25CF</font></html>";
    }


    /**
     * Функция создания меню "Настройки"
     */
    public JMenu createSettingsMenu()
    {
        // создадим выпадающее меню
        JMenu viewMenu = new JMenu("Настройки");
        // меню-флажки
        JMenuItem logging  = new JMenuItem("Ведение лога");
        JMenuItem server  = new JMenuItem("Сервер");
        JMenuItem updates  = new JMenuItem("Обновления");
        JMenuItem debugging = new JMenuItem("Отладка");
        JMenuItem appearance = new JMenuItem("Внешний вид");
        JCheckBoxMenuItem showAllHid = new JCheckBoxMenuItem("Показывать все HID-устройства");
        showAllHid.setSelected(prop != null && prop.isShowAllHidDevices());
        showAllHid.setToolTipText("Полное перечисление HID (включая мышь/клавиатуру). "
                + "Выключено — сканируются только известные приборы.");
        // меню-переключатели уровня логирования
        JRadioButtonMenuItem heavyModeItem = new JRadioButtonMenuItem("Работа в нагруженном режиме");
        JRadioButtonMenuItem normalModeItem = new JRadioButtonMenuItem("Работа в обычном режиме");
        JRadioButtonMenuItem debugModeItem = new JRadioButtonMenuItem("Работа в режиме отладки");
        // организуем переключатели в логическую группу
        ButtonGroup bg = new ButtonGroup();
        bg.add(heavyModeItem);
        bg.add(normalModeItem);
        bg.add(debugModeItem);
        // добавим все в меню
        viewMenu.add(logging);
        viewMenu.add(server);
        viewMenu.add(updates);
        viewMenu.add(debugging);
        viewMenu.add(appearance);
        viewMenu.add(showAllHid);
        // разделитель можно создать и явно
        viewMenu.add( new JSeparator());
        viewMenu.add(heavyModeItem);
        viewMenu.add(normalModeItem);
        viewMenu.add(debugModeItem);

        heavyModeItem.addActionListener(new ActionListener(){
           @Override
           public void actionPerformed(ActionEvent arg0) {
               setLogLevelForAllLoggers(Level.ERROR);
           }
        });
        normalModeItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setLogLevelForAllLoggers(Level.INFO);
            }
        });
        debugModeItem.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                setLogLevelForAllLoggers(Level.DEBUG);
            }
        });

        showAllHid.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (prop != null) {
                    prop.setShowAllHidDevices(showAllHid.isSelected());
                }
                log.info("showAllHidDevices = {}", showAllHid.isSelected());
            }
        });


        logging.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("LogWindows");
                LogSettingWindows logWindows = new LogSettingWindows(prop);
                logWindows.setName("Log settings");
                logWindows.setTitle("Log settings");
                logWindows.pack();
                logWindows.setVisible(true);
            }
        });
        server.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Server Windows");
                ServerSettingsWindow srvWindows = new ServerSettingsWindow();
                srvWindows.setName("Server settings");
                srvWindows.setTitle("Server settings");
                srvWindows.pack();
                srvWindows.setVisible(true);
            }
        });
        updates.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Update Settings Window");
                UpdateSettingsWindow win = new UpdateSettingsWindow();
                win.setTitle("Настройки обновлений");
                win.setVisible(true);
            }
        });
        appearance.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                AppearanceSettingsWindow win = new AppearanceSettingsWindow();
                win.setVisible(true);
            }
        });
        return viewMenu;
    }

    private void setLogLevelForAllLoggers(ch.qos.logback.classic.Level level) {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)
                org.slf4j.LoggerFactory.getLogger( org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(level);
        prop.setLogLevel(rootLogger.getLevel());
    }

    /**
     * Функция создания меню "Представления"
     */
    public JMenu createViewMenu(ExecutorService thPool)
    {
        // создадим выпадающее меню
        JMenu viewMenu = new JMenu("Представления");
        // меню-флажки
        JMenuItem graph  = new JMenuItem("График");
        JMenuItem scheme  = new JMenuItem("Схема");
        JMenuItem graphMany  = new JMenuItem("График (окна)");



        // добавим все в меню
        viewMenu.add(graph);
        viewMenu.add(graphMany);
        viewMenu.add(scheme);


        graph.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Graph");
                ChartWindow chartWindow = new ChartWindow();
                chartWindow.setName("График");
                chartWindow.setTitle("График");
                chartWindow.pack();
                chartWindow.setVisible(true);
                chartWindow.renderData();
                System.out.println(chartWindow.isShowing());
                //chartWindow.isEnabled();
                thPool.submit(new RenderThread(chartWindow));
            }
        });

        graphMany.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Graph");

                HashSet<Integer> tabs = new HashSet<>();
                ArrayList<Integer> tabsFieldCapacity = new ArrayList<>();

                // Get the list of all tab numbers
                tabs.addAll(answerStorage.getListOfTabsInStorage());

                // Обновление списка чек-боксов
                for (Integer tab : tabs) {
                    int fieldsCounter = answerLoader.getUnitsArrayForSelectedClientOrTab(tab).size();
                    tabsFieldCapacity.add(fieldsCounter);
                }

                for (int i = 0; i < tabsFieldCapacity.size(); i++) {
                    ChartWindow chartWindow = new ChartWindow(i);
                    chartWindow.setName("График");
                    chartWindow.setTitle("График");
                    chartWindow.pack();
                    chartWindow.setVisible(true);
                    chartWindow.renderData();
                    System.out.println(chartWindow.isShowing() + " " + i);
                    //chartWindow.isEnabled();
                    thPool.submit(new RenderThread(chartWindow));
                }
            }
        });

        return viewMenu;
    }

    /**
     * Функция создания меню "Утилиты"
     */
    public JMenu createUtilitiesMenu(ExecutorService thPool)
    {
        // создадим выпадающее меню
        JMenu utilitiesMenu = new JMenu("Утилиты");
        // меню-флажки
        JMenuItem grabber  = new JMenuItem("Перехват трафика");
        JMenuItem hidDevices  = new JMenuItem("HID - устройвства");
        JMenuItem commandList  = new JMenuItem("Список команд");
        JMenuItem tabMarkersSetting  = new JMenuItem("Переадресация вкладок");
        JMenuItem webSocket  = new JMenuItem("WebSocket (Vega)");
        JMenuItem bleScan  = new JMenuItem("bleScan");
        JMenuItem customRules = new JMenuItem("Пользовательские правила");
        JMenuItem curveWindow = new JMenuItem("Полиномы TC290");
        JMenuItem acuTenFd = new JMenuItem("Расходомер ACU10FD-MM");
        JMenuItem mgsTest = new JMenuItem("MGSTest");
        JMenuItem spbStuMcps = new JMenuItem("SPB_STU_MCPS");
        JMenuItem spbStuMcpsTest = new JMenuItem("SPB_STU_MCPS Test");
        JMenuItem feeBoard = new JMenuItem("ARD_FEE_BRD_METER");
        JMenuItem feeBoardTest = new JMenuItem("ARD_FEE_BRD_METER Test");
        JMenuItem mipexEmu = new JMenuItem("ARD_MIPEX_EMU");


        // добавим все в меню
        utilitiesMenu.add(grabber);
        utilitiesMenu.add(hidDevices);
        utilitiesMenu.add(commandList);
        utilitiesMenu.add(tabMarkersSetting);
        utilitiesMenu.add(webSocket);
        utilitiesMenu.add(bleScan);
        utilitiesMenu.add(customRules);
        utilitiesMenu.add(curveWindow);
        utilitiesMenu.add(acuTenFd);
        utilitiesMenu.add(mgsTest);
        utilitiesMenu.add(spbStuMcps);
        utilitiesMenu.add(spbStuMcpsTest);
        utilitiesMenu.add(feeBoard);
        utilitiesMenu.add(feeBoardTest);
        utilitiesMenu.add(mipexEmu);


        grabber.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Grabber Window");
                GrabberWindow grabberWindow = new GrabberWindow();
                grabberWindow.setName("Grabber Window");
                grabberWindow.setTitle("Grabber Window");
                grabberWindow.pack();
                grabberWindow.setVisible(true);
                grabberWindow.renderData();
                System.out.println(grabberWindow.isShowing());
                //chartWindow.isEnabled();
                thPool.submit(new RenderThread(grabberWindow));
            }
        });
        hidDevices.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("HidDevWindow");
                HidDevWindow HidDevWindow = new HidDevWindow();
                HidDevWindow.setName("HidDevWindow");
                HidDevWindow.setTitle("HidDevWindow");
                HidDevWindow.pack();
                HidDevWindow.setVisible(true);
                HidDevWindow.renderData();
                System.out.println(HidDevWindow.isShowing());
                //chartWindow.isEnabled();
                thPool.submit(new RenderThread(HidDevWindow));
            }
        });
        webSocket.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("webSocketWindow Window");
                WebSocketWindow webSocketWindow = new WebSocketWindow(prop, answerStorage);
                webSocketWindow.setName("webSocketWindow Window");
                webSocketWindow.setTitle("webSocketWindow Window");
                webSocketWindow.pack();
                webSocketWindow.setVisible(true);
                webSocketWindow.renderData();
                System.out.println(webSocketWindow.isShowing());
                //chartWindow.isEnabled();
                thPool.submit(new RenderThread(webSocketWindow));
            }
        });
        commandList.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("Command List Window");
                CommandsWindow commandsWindow = new CommandsWindow();
                commandsWindow.setName("Command List Window");
                commandsWindow.setTitle("Command List Window");
                commandsWindow.pack();
                commandsWindow.setVisible(true);
                //commandsWindow.renderData();
                System.out.println(commandsWindow.isShowing());
                //chartWindow.isEnabled();
                //thPool.submit(new RenderThread(commandsWindow));
            }
        });
        bleScan.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("BLE_SCAN Window");
                BlueScanWindow blueScanWindow = new BlueScanWindow();
                blueScanWindow.setName("BLE List");
                blueScanWindow.setTitle("BLE List");
                blueScanWindow.pack();
                blueScanWindow.setVisible(true);
                //commandsWindow.renderData();
                System.out.println(blueScanWindow.isShowing());
                //chartWindow.isEnabled();
                //thPool.submit(new RenderThread(commandsWindow));
            }
        });
        tabMarkersSetting.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("asdfasdf" + arg0.toString() + "sdfsdf");
                System.out.println("Tab Marker Setting");
                TabMarkersSettings tabMarkersSettings = new TabMarkersSettings(prop, anyPoolService, answerStorage);
                tabMarkersSettings.setName("Tab Marker Setting");
                tabMarkersSettings.setTitle("Tab Marker Setting");
                tabMarkersSettings.pack();
                tabMarkersSettings.setVisible(true);
                //commandsWindow.renderData();
                System.out.println(tabMarkersSettings.isShowing());
                //chartWindow.isEnabled();
                //thPool.submit(new RenderThread(commandsWindow));
            }
        });
        customRules.addActionListener(new ActionListener()
        {
         @Override
         public void actionPerformed(ActionEvent arg0) {
             System.out.println("arguments [" + arg0.toString() + "] ");
             System.out.println("Custom Rules Setting");
             RuleManagmentDialog ruleManagmentDialog = new RuleManagmentDialog(prop, anyPoolService, answerStorage);
             ruleManagmentDialog.setName("Rules Setting");
             ruleManagmentDialog.setTitle("Rules Setting");
             ruleManagmentDialog.pack();
             ruleManagmentDialog.setVisible(true);
             //commandsWindow.renderData();
             System.out.println(ruleManagmentDialog.isShowing());
             //chartWindow.isEnabled();
             //thPool.submit(new RenderThread(commandsWindow));
         }
        });
        curveWindow.addActionListener(new ActionListener()
        {
         @Override
         public void actionPerformed(ActionEvent arg0) {
             System.out.println("arguments [" + arg0.toString() + "] ");
             System.out.println("Curve Handler Window");
             CurveHandlerWindow curveHandlerWindow = new CurveHandlerWindow(prop);
             curveHandlerWindow.setName("Curve Handler Window");
             curveHandlerWindow.pack();
             curveHandlerWindow.setVisible(true);
             //commandsWindow.renderData();
             System.out.println(curveHandlerWindow.isShowing());
             //chartWindow.isEnabled();
             //thPool.submit(new RenderThread(commandsWindow));
         }
        });
        acuTenFd.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("arguments [" + arg0.toString() + "] ");
                System.out.println("acuTenFd");
                Acu10fdWindow acu10fdWindow = new Acu10fdWindow(prop);
                acu10fdWindow.pack();
                acu10fdWindow.setVisible(true);
                //commandsWindow.renderData();
                System.out.println(acu10fdWindow.isShowing());
                //chartWindow.isEnabled();
                RenderThread render = new RenderThread(acu10fdWindow);
                render.setRenderDelay(3000L);
                thPool.submit(render);
            }
        });
        mgsTest.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("arguments [" + arg0.toString() + "] ");
                System.out.println("MGS simple test Window");
                MultigassensWindow mgsSimpleTest = new MultigassensWindow();
                mgsSimpleTest.setName("MGS simple test Window");
                mgsSimpleTest.pack();
                mgsSimpleTest.setVisible(true);
                //commandsWindow.renderData();
                System.out.println(mgsSimpleTest.isShowing());
                //chartWindow.isEnabled();
                //thPool.submit(new RenderThread(commandsWindow));
            }
        });
        spbStuMcps.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("arguments [" + arg0.toString() + "] ");
                System.out.println("SPB_STU_MCPS Control Window");
                spbStuMcpsMain mcpsPanel = new spbStuMcpsMain();
                JFrame frame = new JFrame("SPB_STU_MCPS — Управление каналами");
                frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                frame.setContentPane(mcpsPanel.getMainPanel());
                frame.pack();
                frame.setSize(980, 520);
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
                System.out.println(frame.isShowing());
            }
        });
        spbStuMcpsTest.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("arguments [" + arg0.toString() + "] ");
                System.out.println("SPB_STU_MCPS Test Window");
                McpsTestFrame testFrame = new McpsTestFrame();
                testFrame.setVisible(true);
            }
        });
        feeBoard.addActionListener(e -> {
            System.out.println("ARD_FEE_BRD_METER Control Window");
            FeeBoardMain panel = new FeeBoardMain();
            JFrame frame = new JFrame("ARD_FEE_BRD_METER — Управление");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(980, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
        feeBoardTest.addActionListener(e -> {
            System.out.println("ARD_FEE_BRD_METER Test Window");
            new FeeBoardTestFrame().setVisible(true);
        });
        mipexEmu.addActionListener(e -> {
            System.out.println("ARD_MIPEX_EMU Control Window");
            MipexEmuMain panel = new MipexEmuMain();
            JFrame frame = new JFrame("ARD_MIPEX_EMU — Управление");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(980, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        return utilitiesMenu;
    }

    /**
     * Функция создания меню "Панели управления"
     */
    public JMenu createControlPanelsMenu() {
        JMenu controlPanelsMenu = new JMenu("Панели управления");

        // STU → mcps → пункты
        JMenu stuMenu = new JMenu("STU");
        JMenu mcpsMenu = new JMenu("mcps");

        JMenuItem stuControl = new JMenuItem("Панель управления");
        JMenuItem stuEmulation = new JMenuItem("Панель эмуляции");
        JMenuItem stuInfo = new JMenuItem("Справочная информация");

        mcpsMenu.add(stuControl);
        mcpsMenu.add(stuEmulation);
        mcpsMenu.add(stuInfo);
        stuMenu.add(mcpsMenu);

        // Qidian → qdl80a → пункты
        JMenu qdMenu = new JMenu("Qidian");
        JMenu qdl80aMenu = new JMenu("qdl80a");

        JMenuItem qdControl = new JMenuItem("Панель управления");
        JMenuItem qdEmulation = new JMenuItem("Панель эмуляции");
        JMenuItem qdInfo = new JMenuItem("Справочная информация");

        qdl80aMenu.add(qdControl);
        qdl80aMenu.add(qdEmulation);
        qdl80aMenu.add(qdInfo);
        qdMenu.add(qdl80aMenu);

        // Edwards → D39730880 → пункты
        JMenu edwardsMenu = new JMenu("Edwards");
        JMenu d397Menu = new JMenu("D39730880");

        JMenuItem edControl = new JMenuItem("Панель управления");
        JMenuItem edEmulation = new JMenuItem("Панель эмуляции");
        JMenuItem edInfo = new JMenuItem("Справочная информация");

        d397Menu.add(edControl);
        d397Menu.add(edEmulation);
        d397Menu.add(edInfo);
        edwardsMenu.add(d397Menu);

        // Arduino → FeeBoard / MipexEmu
        JMenu arduinoMenu = new JMenu("Arduino");
        JMenu feeMenu = new JMenu("FeeBoard (CCM)");
        JMenu mipexMenu = new JMenu("Mipex Emu");

        JMenuItem feeControl = new JMenuItem("Панель управления");
        JMenuItem feeEmulation = new JMenuItem("Панель эмуляции");
        JMenuItem feeInfo = new JMenuItem("Справочная информация");

        JMenuItem mipexControl = new JMenuItem("Панель управления");
        JMenuItem mipexInfo = new JMenuItem("Справочная информация");

        feeMenu.add(feeControl);
        feeMenu.add(feeEmulation);
        feeMenu.add(feeInfo);
        mipexMenu.add(mipexControl);
        mipexMenu.add(mipexInfo);
        arduinoMenu.add(feeMenu);
        arduinoMenu.add(mipexMenu);

        // ESP32 → Kantser BLE Emu
        JMenu esp32Menu = new JMenu("ESP32");
        JMenu kantserBleMenu = new JMenu("Kantser BLE Emu");

        JMenuItem kantserControl = new JMenuItem("Панель управления");
        JMenuItem kantserInfo = new JMenuItem("Справочная информация");

        kantserBleMenu.add(kantserControl);
        kantserBleMenu.add(kantserInfo);
        esp32Menu.add(kantserBleMenu);

        // Binder → Camera (TCP) климатическая камера
        JMenu binderMenu = new JMenu("Binder");
        JMenu binderCameraMenu = new JMenu("Camera (TCP)");

        JMenuItem binderControl = new JMenuItem("Панель управления");
        JMenuItem binderEmulation = new JMenuItem("Панель эмуляции");
        JMenuItem binderInfo = new JMenuItem("Справочная информация");

        binderCameraMenu.add(binderControl);
        binderCameraMenu.add(binderEmulation);
        binderCameraMenu.add(binderInfo);
        binderMenu.add(binderCameraMenu);

        // БКМ-4 → блок коммутации (RS-232C)
        JMenu bkm4Menu = new JMenu("БКМ-4");
        JMenuItem bkm4Control = new JMenuItem("Панель управления");
        JMenuItem bkm4Emulation = new JMenuItem("Панель эмуляции");
        JMenuItem bkm4Info = new JMenuItem("Справочная информация");
        bkm4Menu.add(bkm4Control);
        bkm4Menu.add(bkm4Emulation);
        bkm4Menu.add(bkm4Info);

        controlPanelsMenu.add(stuMenu);
        controlPanelsMenu.add(qdMenu);
        controlPanelsMenu.add(edwardsMenu);
        controlPanelsMenu.add(arduinoMenu);
        controlPanelsMenu.add(esp32Menu);
        controlPanelsMenu.add(binderMenu);
        controlPanelsMenu.add(bkm4Menu);

        // Testa → климатическая камера (UDP)
        JMenu testaMenu = new JMenu("Testa");
        JMenuItem testaControl = new JMenuItem("Панель управления");
        JMenuItem testaEmulation = new JMenuItem("Панель эмуляции");
        testaMenu.add(testaControl);
        testaMenu.add(testaEmulation);
        controlPanelsMenu.add(testaMenu);

        JMenu botoMenu = new JMenu("Термокамеры BOTO");

        // BOTO 800 → Serial (RS-232C, Modbus RTU) и TCP/IP (Modbus TCP, эмулятор-сниффер)
        JMenu boto800Menu = new JMenu("BOTO 800");

        JMenu boto800SerialMenu = new JMenu("Serial");
        JMenuItem boto800Control = new JMenuItem("Панель управления");
        JMenuItem boto800Emulation = new JMenuItem("Панель эмуляции");
        JMenuItem boto800SerialInfo = new JMenuItem("Справочная информация");
        boto800SerialMenu.add(boto800Control);
        boto800SerialMenu.add(boto800Emulation);
        boto800SerialMenu.add(boto800SerialInfo);

        JMenu boto800TcpMenu = new JMenu("TCP/IP");
        JMenuItem boto800TcpEmulation = new JMenuItem("Панель эмуляции (сниффер)");
        JMenuItem boto800TcpInfo = new JMenuItem("Справочная информация");
        boto800TcpMenu.add(boto800TcpEmulation);
        boto800TcpMenu.add(boto800TcpInfo);

        boto800Menu.add(boto800SerialMenu);
        boto800Menu.add(boto800TcpMenu);
        botoMenu.add(boto800Menu);

        // BOTO 120 → только Serial (RS-232C, Modbus RTU)
        JMenu boto120Menu = new JMenu("BOTO 120");
        JMenu boto120SerialMenu = new JMenu("Serial");
        JMenuItem boto120SerialControl = new JMenuItem("Панель управления");
        JMenuItem boto120SerialEmulation = new JMenuItem("Панель эмуляции");
        JMenuItem boto120SerialInfo = new JMenuItem("Справочная информация");
        boto120SerialMenu.add(boto120SerialControl);
        boto120SerialMenu.add(boto120SerialEmulation);
        boto120SerialMenu.add(boto120SerialInfo);
        boto120Menu.add(boto120SerialMenu);
        botoMenu.add(boto120Menu);

        controlPanelsMenu.add(botoMenu);

        // FNIRSI → DPS150 (бинарный протокол, USB-serial, 115200 8N1)
        JMenu fnirsiMenu = new JMenu("FNIRSI");
        JMenu dps150Menu = new JMenu("DPS150");
        JMenuItem dps150Control = new JMenuItem("Панель управления");
        JMenuItem dps150Emulation = new JMenuItem("Панель эмуляции");
        JMenuItem dps150Info = new JMenuItem("Справочная информация");
        dps150Menu.add(dps150Control);
        dps150Menu.add(dps150Emulation);
        dps150Menu.add(dps150Info);
        fnirsiMenu.add(dps150Menu);
        controlPanelsMenu.add(fnirsiMenu);

        // Витрина кастомных виджетов ru.kantser.gui
        JMenuItem showcaseItem = new JMenuItem("Витрина виджетов ru.kantser.gui");
        controlPanelsMenu.add(showcaseItem);

        // TT5166 → климатическая камера (Modbus RTU, 38400 8E1)
        JMenu tt5166Menu = new JMenu("TT5166");
        JMenuItem tt5166Control = new JMenuItem("Панель управления");
        JMenuItem tt5166Emulation = new JMenuItem("Панель эмуляции");
        JMenuItem tt5166Info = new JMenuItem("Справочная информация");
        tt5166Menu.add(tt5166Control);
        tt5166Menu.add(tt5166Emulation);
        tt5166Menu.add(tt5166Info);
        controlPanelsMenu.add(tt5166Menu);

        stuControl.addActionListener(e -> {
            System.out.println("STU MCPS Control Panel");
            spbStuMcpsMain mcpsPanel = new spbStuMcpsMain();
            JFrame frame = new JFrame("STU:mcps — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(mcpsPanel.getMainPanel());
            frame.pack();
            frame.setSize(980, 520);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        stuEmulation.addActionListener(e -> {
            System.out.println("STU MCPS Emulation Panel");
            McpsTestFrame testFrame = new McpsTestFrame();
            testFrame.setVisible(true);
        });

        stuInfo.addActionListener(e -> {
            System.out.println("STU MCPS Info");
            JOptionPane.showMessageDialog(null,
                    "Раздел находится в разработке",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        qdControl.addActionListener(e -> {
            System.out.println("QDL80A Control Panel");
            Qdl80aMain qdl80aPanel = new Qdl80aMain();
            JFrame frame = new JFrame("Qidian:qdl80a — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(qdl80aPanel.getMainPanel());
            frame.pack();
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        qdEmulation.addActionListener(e -> {
            System.out.println("QDL80A Emulation Panel");
            Qdl80aTestFrame testFrame = new Qdl80aTestFrame();
            testFrame.setVisible(true);
        });

        qdInfo.addActionListener(e -> {
            System.out.println("QDL80A Info");
            JOptionPane.showMessageDialog(null,
                    "Раздел находится в разработке",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        edControl.addActionListener(e -> {
            System.out.println("Edwards TIC Control Panel");
            d39730880Main edwardsPanel = new d39730880Main();
            JFrame frame = new JFrame("Edwards:D39730880 — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(edwardsPanel.getMainPanel());
            frame.pack();
            frame.setSize(900, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        edEmulation.addActionListener(e -> {
            System.out.println("Edwards TIC Emulation Panel");
            EdwardsTicTestFrame testFrame = new EdwardsTicTestFrame();
            testFrame.setVisible(true);
        });

        edInfo.addActionListener(e -> {
            System.out.println("Edwards TIC Info");
            JOptionPane.showMessageDialog(null,
                    "Раздел находится в разработке",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        feeControl.addActionListener(e -> {
            System.out.println("Arduino FeeBoard Control Panel");
            FeeBoardMain panel = new FeeBoardMain();
            JFrame frame = new JFrame("Arduino:FeeBoard — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(980, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        feeEmulation.addActionListener(e -> {
            System.out.println("Arduino FeeBoard Emulation Panel");
            new FeeBoardTestFrame().setVisible(true);
        });

        feeInfo.addActionListener(e -> {
            JOptionPane.showMessageDialog(null,
                    "CCM Fee Board / CurMeter (ARD_FEE_BRD_METER).\n" +
                            "Протокол: ccm_fee.md\n" +
                            "Команды: F, MMESU, LOGO, LOG, CONC?, FPWR/FPWR?,\n" +
                            "SENSON/SENSOFF, GCOEF, REBOOT, SREV?, SRAL?, %**,\n" +
                            "SLAS/SDAS, SPOLY0/1, SVOLT0, STRGLV, SV2AMP, SCABD, URTMOD.",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        mipexControl.addActionListener(e -> {
            System.out.println("Arduino MipexEmu Control Panel");
            MipexEmuMain panel = new MipexEmuMain();
            JFrame frame = new JFrame("Arduino:MipexEmu — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(980, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        mipexInfo.addActionListener(e -> {
            JOptionPane.showMessageDialog(null,
                    "Arduino Mipex / multi-mode emulator (ARD_MIPEX_EMU).\n" +
                            "Протокол: mip_emu.md · 57600 8N1 CR\n" +
                            "Режимы FMOD: 0 legacy, 1 V-meter, 2 Mipex II (осн.),\n" +
                            "3 Mipex-14, 4–6 платы сопряжения (в разр.).\n" +
                            "Команды: F, F?, LOG, CONC?, CONST?, ID, TERM?/TERM,\n" +
                            "FMOD, CMOD, GMOD, TMOD, SAPR, SDAC, SMCV, GMCV, KALB,\n" +
                            "SSTAT, SREV?, SRAL?, %**, S085, !, MMES, UART/мост/отладка.",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        binderControl.addActionListener(e -> {
            System.out.println("Binder Camera (TCP) Control Panel");
            BinderControlPanel binderPanel = new BinderControlPanel();
            JFrame frame = new JFrame("Binder:Camera (TCP) — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(binderPanel.getMainPanel());
            frame.pack();
            frame.setSize(1000, 680);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        binderEmulation.addActionListener(e -> {
            System.out.println("Binder Camera (TCP) Emulation Panel");
            new BinderEmulatorFrame().setVisible(true);
        });

        binderInfo.addActionListener(e -> {
            JOptionPane.showMessageDialog(null,
                    "Климатическая камера Binder (TCP, порт 10001).\n" +
                            "Протокол: binder.md · Modbus-фрейм + CRC-16/Modbus.\n" +
                            "Команды: SetT (0x10, рег. 0x1581, float °C),\n" +
                            "GetT (0x03, рег. 0x11A9, float °C),\n" +
                            "SetHC (0x06, рег. 0x158B) — влажность (t>-6 вкл, t<-12 выкл).",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        testaControl.addActionListener(e -> {
            System.out.println("Testa Control Panel");
            TestaControlPanel panel = new TestaControlPanel();
            JFrame frame = new JFrame("Testa — Панель управления (UDP)");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(900, 620);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        testaEmulation.addActionListener(e -> {
            System.out.println("Testa Emulation Panel");
            new TestaEmulatorFrame().setVisible(true);
        });

        bkm4Control.addActionListener(e -> {
            System.out.println("BKM-4 Control Panel");
            Bkm4Main bkm4Panel = new Bkm4Main();
            JFrame frame = new JFrame("БКМ-4 — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(bkm4Panel.getMainPanel());
            frame.pack();
            frame.setSize(960, 620);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        bkm4Emulation.addActionListener(e -> {
            System.out.println("BKM-4 Emulation Panel");
            new Bkm4EmulatorFrame().setVisible(true);
        });

        bkm4Info.addActionListener(e -> {
            JOptionPane.showMessageDialog(null,
                    "Блок коммутации БКМ-4 (RS-232C, 9600 8N1, ASCII/CR).\n" +
                            "Протокол: bkm4.md\n" +
                            "Команды: &A?/&A0(ручн.)/&A1(внешн.) — режим,\n" +
                            "&V?/&V0..&V4 — газовый клапан,\n" +
                            "&S?/&Sxxxx (0..3000) — уставка расхода,\n" +
                            "&F? — фактический расход,\n" +
                            "&G?/&G0(выкл)/&G1(вкл) — генерация.",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        boto800Control.addActionListener(e -> {
            System.out.println("BOTO 800 Serial — панель управления");
            Boto800Main panel = new Boto800Main();
            panel.setVisible(true);
        });
        boto800Emulation.addActionListener(e -> {
            System.out.println("BOTO 800 Serial — панель эмуляции");
            new Boto800EmulatorFrame().setVisible(true);
        });
        boto800SerialInfo.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Термокамера BOTO-800 — Serial (RS-232C, Modbus RTU 9600 8N1, slave 1).\n" +
                        "Карта: boto_800_register_map.md\n" +
                        "Блок реального времени читается одним запросом 10..49 (40 регистров):\n" +
                        "  10 = текущая T °C (raw/10), 11 = уставка T, 12 = зеркало режима,\n" +
                        "  13 = MV темп. 0..100, 14 = влажность %, 15 = уставка влаги,\n" +
                        "  16 = зеркало режима, 17 = MV влаги, 18 = вкл/выкл поддержку влаги,\n" +
                        "  19 = подсветка, 20 = маска ошибок,\n" +
                        "  31 = камера вкл/выкл, 32/33/34 = время работы ч/м/с,\n" +
                        "  38/39 = нижний/верхний предел температуры, 40/41 = пределы влаги.\n" +
                        "Записи уставок: 60 = температура (raw/10), 61 = влага, 63 = 1 работа / 0 стоп.\n" +
                        "Важно: рег 39 и 41 должны отдавать реальные пределы, иначе штатная\n" +
                        "программа не даст ввести уставку (считает предел = 0).",
                "BOTO 800 · Serial — справка",
                JOptionPane.INFORMATION_MESSAGE));

        boto800TcpEmulation.addActionListener(e -> {
            System.out.println("BOTO 800 TCP/IP — панель эмуляции (сниффер протокола)");
            new Boto800TcpEmulatorFrame().setVisible(true);
        });
        boto800TcpInfo.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Термокамера BOTO-800 — TCP/IP (Modbus TCP, порт по умолчанию 8000).\n" +
                        "\n" +
                        "ПРОТОКОЛ ПОДТВЕРЖДЁН перехватом: это Modbus TCP — тот же Modbus RTU,\n" +
                        "но с MBAP-заголовком вместо CRC:\n" +
                        "  00 01 | 00 00 | 00 06 | 01 | 03 | 23 1C | 00 02\n" +
                        "  TID   |  PID  |  LEN  |UID | fn |  reg  |  qty\n" +
                        "PDU байт-в-байт как в RTU, ответ эхо-ит TID и UID.\n" +
                        "\n" +
                        "Эмулятор поднимает TCP-сервер и подробно журналирует весь обмен:\n" +
                        "hex + ASCII + разбор кадра в консоль, терминал, GUI-лог и\n" +
                        "logs/Boto800-TCP_wire_*.log.\n" +
                        "Сервер сам различает Modbus/TCP (MBAP) и «сырой» Modbus/RTU по TCP,\n" +
                        "считает границу кадра по CRC и эхо-ит transactionId/unitId.\n" +
                        "На незнакомый код функции соединение НЕ рвётся — шлётся исключение\n" +
                        "IllegalFunction (0x81), чтобы был виден весь сеанс обмена.\n" +
                        "\n" +
                        "Поведение штатной программы (видно в логе):\n" +
                        "  • при подключении пишет 0 в рег 8108 (0x1FAC);\n" +
                        "  • читает 8900 ×64 (0x22C4), 8964 ×24 (0x2304), 8988 ×2;\n" +
                        "  • далее цикл ~220 мс: читает 10 ×40, затем 8900 ×3.\n" +
                        "Служебные блоки отдаются нулями, иначе GUI ломается об абсурдные пределы.\n" +
                        "Адреса, куда прибор пишет, запоминаются — это и есть карта регистров.",
                "BOTO 800 · TCP/IP — справка",
                JOptionPane.INFORMATION_MESSAGE));

        boto120SerialControl.addActionListener(e -> {
            System.out.println("BOTO 120 Serial — панель управления (зонд)");
            new Boto120Main().setVisible(true);
        });
        boto120SerialEmulation.addActionListener(e -> {
            System.out.println("BOTO 120 Serial — панель эмуляции (заглушка)");
            new Boto120EmulatorFrame().setVisible(true);
        });
        boto120SerialInfo.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Термокамера BOTO-120 — Serial (RS-232C, Modbus RTU 9600 8N1, slave 1).\n" +
                        "Протокол подтверждён перехватом штатной программы и полевым запросом.\n" +
                        "Регистры: 12 = PV ×100 (0.03 — чтение 0x03),\n" +
                        "100 = уставка ×100 (запись 0x06), 105 = вкл/выкл 1/0 (запись 0x06).\n" +
                        "Порядок штатной программы: ВКЛ (105=1) → чтение PV → уставка (100).\n" +
                        "Неизвестные регистры читаются нулями; безопасный скан чтения — в панели управления.\n" +
                        "\n" +
                        "ВАЖНО: BOTO 120 и BOTO 800 — разные приборы с разными картами!\n" +
                        "У BOTO 120 масштаб ×100 и адреса 12/100/105,\n" +
                        "у BOTO 800 масштаб ×10 и адреса 10/60/63. Не путайте панели.",
                "BOTO 120 · Serial — справка",
                JOptionPane.INFORMATION_MESSAGE));

        dps150Control.addActionListener(e -> {
            System.out.println("FNIRSI DPS150 — панель управления");
            new FnirsiDps150Main().setVisible(true);
        });
        dps150Emulation.addActionListener(e -> {
            System.out.println("FNIRSI DPS150 — панель эмуляции");
            new FnirsiDps150EmulatorFrame().setVisible(true);
        });
        dps150Info.addActionListener(e -> JOptionPane.showMessageDialog(null,
                "Источник питания FNIRSI DPS150 (USB-serial, 115200 8N1).\n" +
                        "Бинарный протокол: кадр F1 cmd type len data cs,\n" +
                        "ответ F0 A1 type len data cs, контрольная сумма — сумма\n" +
                        "байт от type до конца данных по модулю 256.\n" +
                        "Чтение: getModel (DPS-150), getSwVersion, getHwVersion, getVin,\n" +
                        "  getLimitVout (E2, макс. напряжение), getLimitCurrent (E3, макс. ток),\n" +
                        "  getPower (C3: выход V/A/Вт), getTemp (C4), getOutput (DB), getBrightness (D6).\n" +
                        "Запись: setVout (0..30.0 В, float LE), setIout (0..5.0 А, float LE),\n" +
                        "  setOutput (0/1), setBrightness (0..14).",
                "FNIRSI DPS150 — справка",
                JOptionPane.INFORMATION_MESSAGE));

        showcaseItem.addActionListener(e -> {
            System.out.println("ru.kantser.gui — витрина виджетов");
            new ShowcaseFrame().setVisible(true);
        });

        tt5166Control.addActionListener(e -> {
            System.out.println("TT5166 Control Panel");
            TT5166Main panel = new TT5166Main();
            panel.setVisible(true);
        });
        tt5166Emulation.addActionListener(e -> {
            System.out.println("TT5166 Emulation Panel");
            new TT5166EmulatorFrame().setVisible(true);
        });
        tt5166Info.addActionListener(e -> {
            JOptionPane.showMessageDialog(null,
                    "Климатическая камера TT5166 (Modbus RTU, 38400 8E1).\n" +
                            "getData (0x03, рег. 0x0000, 6: темп PV/SV, выход, влага PV/SV, выход),\n" +
                            "getFault (0x03, рег. 0x001B) — код ошибки,\n" +
                            "start/stop (0x05, катушки 0x0000/0x0001),\n" +
                            "setConstTemp (0x06, рег. 0x0026, °C×10),\n" +
                            "setConstHum (0x06, рег. 0x0027, %×10),\n" +
                            "getState (0x03, рег. 0x0018), getProgramTime (0x03, рег. 0x0006).",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        kantserControl.addActionListener(e -> {
            System.out.println("ESP32 Kantser BLE Emu Control Panel");
            KantserBleMain panel = new KantserBleMain();
            JFrame frame = new JFrame("ESP32:KantserBLE — Панель управления");
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
            frame.setContentPane(panel.getMainPanel());
            frame.pack();
            frame.setSize(980, 720);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });

        kantserInfo.addActionListener(e -> {
            JOptionPane.showMessageDialog(null,
                    "ESP32 Kantser BLE simulator (ESP_KANTSER_BLE_EMU).\n" +
                            "Протокол: 115200 8N1 CR\n" +
                            "Команды: HELP, GDUI?, SREV?, BLST?, ADST?,\n" +
                            "SCH1..SCH4 (концентрации), STER (смещение темп.),\n" +
                            "ADVE/ADVD (реклама), REBT, LRBC, LSBA, LLBA,\n" +
                            "CMMD/SMAC/GMAC (MAC мастера и устройства).",
                    "Справочная информация",
                    JOptionPane.INFORMATION_MESSAGE);
        });

        return controlPanelsMenu;
    }
}

/*
    public static void main(String[] args) {

        EventQueue.invokeLater(() -> {

            var ex = new LineChartEx2();
            ex.setVisible(true);
        });
    }
 */