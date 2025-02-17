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

import org.example.services.AnswerStorage;
import org.example.services.comPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;

import static org.example.device.SomeDevice.log;
import static org.example.gui.ChartWindow.getFieldsCountForTab;

public class JmenuFile {
    private static final Logger logger = Logger.getLogger(JmenuFile.class);
    private final MyProperties prop;
    private final AnyPoolService anyPoolService;


    public JmenuFile (MyProperties prop, AnyPoolService anyPoolService){
        super();
        this.prop = prop;
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
        // добавим все в меню
        viewMenu.add(sysDebug);
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
                tabs.addAll(AnswerStorage.answersByTab.keySet());

                // Обновление списка чек-боксов
                for (Integer tab : tabs) {
                    int fieldsCounter = getFieldsCountForTab(tab).size();
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



        // добавим все в меню
        utilitiesMenu.add(grabber);
        utilitiesMenu.add(hidDevices);
        utilitiesMenu.add(commandList);
        utilitiesMenu.add(tabMarkersSetting);
        utilitiesMenu.add(webSocket);


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
                WebSocketWindow webSocketWindow = new WebSocketWindow();
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
        tabMarkersSetting.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
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
        return utilitiesMenu;
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