package org.example.gui;


import org.apache.log4j.Category;
import org.apache.log4j.Logger;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.example.utilites.MyProperties;

import java.util.Properties;

public class JmenuFile {
    private static final Logger logger = Logger.getLogger(JmenuFile.class);
    private final MyProperties prop;


    public JmenuFile (MyProperties prop){
        super();
        this.prop = prop;
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
     * Функция создания меню "Настройки"
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
                LogSettingWindows logWindows = new LogSettingWindows();
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
     * Функция создания меню "Настройки"
     */
    public JMenu createViewMenu(ExecutorService thPool)
    {
        // создадим выпадающее меню
        JMenu viewMenu = new JMenu("Представления");
        // меню-флажки
        JMenuItem graph  = new JMenuItem("График");



        // добавим все в меню
        viewMenu.add(graph);


        graph.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                System.out.println("ViewMode");
                ChartWindow chartWindow = new ChartWindow();
                chartWindow.setName("Log settings");
                chartWindow.setTitle("Log settings");
                chartWindow.pack();
                chartWindow.setVisible(true);
                chartWindow.renderData();
                System.out.println(chartWindow.isShowing());
                //chartWindow.isEnabled();
                thPool.submit(new RenderThread(chartWindow));
            }
        });

        return viewMenu;
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