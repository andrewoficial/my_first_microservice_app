package org.example.gui;


import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;

import org.example.gui.accu10fd.Acu10fdWindow;
import org.example.gui.curve.CurveHandlerWindow;
import org.example.gui.devices.edvards.d39730880.control.d39730880Main;
import org.example.gui.devices.edvards.d39730880.emulation.EdwardsTicTestFrame;
import org.example.gui.devices.qidian.qdl80a.control.Qdl80aMain;
import org.example.gui.devices.qidian.qdl80a.emulation.Qdl80aTestFrame;
import org.example.gui.devices.stu.mcps.control.spbStuMcpsMain;
import org.example.gui.devices.stu.mcps.emulation.McpsTestFrame;
import org.example.gui.graph.ChartWindow;
import org.example.gui.graph.data.AnswerLoader;
import org.example.gui.mgstest.MultigassensWindow;
import org.example.gui.system.logs.ViewLogsWindow;
import org.example.gui.system.resources.DebugWindow;
import org.example.services.AnswerStorage;
import org.example.services.connectionPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;



public class JmenuFile {
    private static final Logger logger = Logger.getLogger(JmenuFile.class);
    private MyProperties prop;
    private final AnyPoolService anyPoolService;
    private final AnswerLoader answerLoader = new AnswerLoader();

    public JmenuFile (MyProperties extProp, AnyPoolService anyPoolService){
        super();
        this.prop = extProp;
        this.anyPoolService = anyPoolService;
        if(anyPoolService == null){
            logger.warn("В конструктор JmenuFile передан null anyPoolService");
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
                DebugWindow logWindows = new DebugWindow();
                logWindows.setName("Debug Window");
                logWindows.setTitle("Debug Window");
                logWindows.pack();
                logWindows.setModal(false);
                logWindows.setVisible(true);
                logWindows.startMonitor();
                thPool.submit(new RenderThread(logWindows));


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
        JMenuItem sysUpdate  = new JMenuItem("Проверка обновлений");
        // добавим все в меню
        viewMenu.add(sysUpdate);
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
        return viewMenu;
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
        JMenuItem debugging = new JMenuItem("Отладка");
        // меню-переключатели
        JRadioButtonMenuItem one = new JRadioButtonMenuItem("Работа в обычном режиме");
        JRadioButtonMenuItem two = new JRadioButtonMenuItem("Работа в режиме отладки");
        // организуем переключатели в логическую группу
        ButtonGroup bg = new ButtonGroup();
        bg.add(one);
        bg.add(two);
        // добавим все в меню
        viewMenu.add(logging);
        viewMenu.add(server);
        viewMenu.add(debugging);
        // разделитель можно создать и явно
        viewMenu.add( new JSeparator());
        viewMenu.add(one);
        viewMenu.add(two);

        one.addActionListener(new ActionListener(){
           @Override
           public void actionPerformed(ActionEvent arg0) {
               //Logger.getLogger(JmenuFile.class).setLevel(Level.ERROR);
               System.out.println("Level ERROR");
               Logger root = Logger.getRootLogger();
               Enumeration allLoggers = root.getLoggerRepository().getCurrentCategories();
               root.setLevel(org.apache.log4j.Level.ERROR);
               while (allLoggers.hasMoreElements()){
                   Category tmpLogger = (Category) allLoggers.nextElement();
                   tmpLogger .setLevel(org.apache.log4j.Level.ERROR);
               }
               prop.setLogLevel(root.getLevel());
           }
        });
        two.addActionListener(new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent arg0) {
                //Logger.getLogger(JmenuFile.class).setLevel(Level.DEBUG);
                Logger root = Logger.getRootLogger();
                Enumeration allLoggers = root.getLoggerRepository().getCurrentCategories();
                root.setLevel(org.apache.log4j.Level.DEBUG);
                while (allLoggers.hasMoreElements()){
                    Category tmpLogger = (Category) allLoggers.nextElement();
                    tmpLogger .setLevel(org.apache.log4j.Level.DEBUG);
                }
                prop.setLogLevel(root.getLevel());
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
        return viewMenu;
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
                tabs.addAll(AnswerStorage.getListOfTabsInStorage());

                // Обновление списка чек-боксов
                for (Integer tab : tabs) {
                    int fieldsCounter = answerLoader.getUnitsArrayForTab(tab).size();
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
        JMenuItem webSocket  = new JMenuItem("webSocket");
        JMenuItem bleScan  = new JMenuItem("bleScan");
        JMenuItem customRules = new JMenuItem("Пользовательские правила");
        JMenuItem curveWindow = new JMenuItem("Полиномы TC290");
        JMenuItem acuTenFd = new JMenuItem("Расходомер ACU10FD-MM");
        JMenuItem mgsTest = new JMenuItem("MGSTest");
        JMenuItem spbStuMcps = new JMenuItem("SPB_STU_MCPS");
        JMenuItem spbStuMcpsTest = new JMenuItem("SPB_STU_MCPS Test");


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
                WebSocketWindow webSocketWindow = new WebSocketWindow(prop);
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
                TabMarkersSettings tabMarkersSettings = new TabMarkersSettings(prop, anyPoolService);
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
             RuleManagmentDialog ruleManagmentDialog = new RuleManagmentDialog(prop, anyPoolService);
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

        controlPanelsMenu.add(stuMenu);
        controlPanelsMenu.add(qdMenu);
        controlPanelsMenu.add(edwardsMenu);

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