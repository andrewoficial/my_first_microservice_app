/*
Файл содержит код, выполняющийся при взаимодействии с виндовс-окном приложения
 */
package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.Main;
import org.example.services.AnswerStorage;
import org.example.services.ComPort;
import org.example.services.TabAnswerPart;
import org.example.utilites.*;
import org.example.services.PoolService;
import org.jfree.data.json.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.w3c.dom.ls.LSOutput;

import javax.lang.model.element.Name;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainWindow extends JFrame implements Rendeble {
    public static MainWindow mainWindow = null;
    private JPanel contentPane;
    private static int currTabCount = 0;
    private final static Logger log = Logger.getLogger(MainWindow.class);
    private int countRender = 0;

    private final ExecutorService thPool = Executors.newCachedThreadPool();
    private final ExecutorService uiThPool = Executors.newCachedThreadPool();

    //MyProperties prop = Main.prop;

    private final MyProperties prop;
    private final ComPort comPorts;


    private final ArrayList<String> textToSendValue = new ArrayList<>();
    private final ArrayList<String> prefToSendValue = new ArrayList<>();
    private final ArrayList<JTextPane> logDataTransferJtextPanel = new ArrayList<>();

    @Getter
    private static final ArrayList<PoolService> poolServices = new ArrayList<>();

    private MainLeftPanelStateCollection leftPanState = new MainLeftPanelStateCollection();


    private final ArrayList<Integer> lastGotedValueFromStorage = new ArrayList<>();//Очередь кэша

    private TabAnswerPart an = new TabAnswerPart(null, -1);

    private JComboBox<String> CB_ComPorts = new JComboBox<>();
    private JComboBox<String> CB_BaudRate = new JComboBox<>();
    private JComboBox CB_DataBits = new JComboBox<>();
    private JComboBox CB_Parity = new JComboBox<>();
    private JComboBox CB_StopBit = new JComboBox<>();
    private JButton BT_Open = new JButton();
    private JButton BT_Close = new JButton();
    private JCheckBox CB_Log = new JCheckBox();
    private JCheckBox CB_Autoconnect;
    private JButton BT_Update = new JButton();
    private JTextField textToSend = new JTextField();
    private JButton BT_Send = new JButton();
    private JComboBox CB_Protocol = new JComboBox<>();
    private JCheckBox CB_Pool = new JCheckBox();
    private JTabbedPane tabbedPane1 = new JTabbedPane();
    private JButton BT_AddDev = new JButton();
    private JButton BT_RemoveDev = new JButton();
    private JTextField IN_PoolDelay = new JTextField();
    private JButton BT_Search = new JButton();
    private JPanel addRemove;
    private JPanel portSetup;
    private JPanel Terminal;
    private JTextField prefOneToSend;

    /**
     * Current tab
     **/
    private int tab = 0; //Текущая вкладка


    private void initUI() {
        assert prop != null;
        setTitle(prop.getTitle() + " v" + prop.getVersion());
        URL resource = Main.class.getClassLoader().getResource("GUI_Images/Pic.png");
        if (resource != null) {
            ImageIcon pic = new ImageIcon(resource);
            this.setIconImage(pic.getImage());
            log.debug("Установка картинки");
        }
        this.pack();
        this.setVisible(true);
    }


    public MainWindow(MyProperties myProperties, ComPort comPorts) {

        this.prop = myProperties;
        this.comPorts = comPorts;
        initUI();

        //this.prop = new MyProperties();
        //this.comPorts = new ComPort();
        MainWindow.mainWindow = this;

        log.debug("Подготовка к рендеру окна....");
        log.debug(Thread.currentThread().getName());
        contentPane.getName();
        log.debug("Получено имя окна " + contentPane.getName());
        //poolComConnections.add(new ComPort());
        ComPort tmpComPorts = new ComPort();
        log.info("Объект ком-портов добавлен в пул");
        JmenuFile jmenu = new JmenuFile(prop);
        log.info("В менюю программы переданы восстановленные параметры");
        // Создание строки главного меню
        JMenuBar menuBar = new JMenuBar();

        // Добавление в главное меню выпадающих пунктов меню
        menuBar.add(jmenu.createFileMenu());
        menuBar.add(jmenu.createSettingsMenu());
        menuBar.add(jmenu.createViewMenu(uiThPool));
        menuBar.add(jmenu.createUtilitiesMenu(uiThPool));
        menuBar.add(jmenu.createSystemParametrs(uiThPool));
        menuBar.add(jmenu.createInfo(uiThPool));
        log.debug("Завершено создание элементов меню");
        setJMenuBar(menuBar);
        setContentPane(contentPane);
        log.debug("Инициализация панели и меню завершена");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        MainLeftPanelStateCollection restoredFromFile = prop.getLeftPanelStateCollection();
        BaudRatesList[] baudRate = BaudRatesList.values();
        for (int i = 0; i < baudRate.length; i++) {
            CB_BaudRate.addItem(baudRate[i].getValue() + "");
            if (restoredFromFile.getBaudRate(0) == baudRate[i].getValue()) {
                CB_BaudRate.setSelectedIndex(i);
            }
        }

        DataBitsList[] dataBits = DataBitsList.values();
        for (int i = 0; i < dataBits.length; i++) {
            CB_DataBits.addItem(dataBits[i].getValue());
            if (restoredFromFile.getDataBits(0) == dataBits[i].getValue()) {
                CB_DataBits.setSelectedIndex(i);
            }
        }

        ParityList[] parityLists = ParityList.values();
        for (int i = 0; i < parityLists.length; i++) {
            CB_Parity.addItem(parityLists[i].getName());
            if (restoredFromFile.getParityBits(0) == i) {
                CB_Parity.setSelectedIndex(i);
            }
        }

        StopBitsList[] stopBitsLists = StopBitsList.values();
        for (int i = 0; i < stopBitsLists.length; i++) {
            CB_StopBit.addItem(stopBitsLists[i].getValue());
            if (restoredFromFile.getStopBits(0) == i) {
                CB_StopBit.setSelectedIndex(i);
            }
        }


        for (int i = 0; i < tmpComPorts.getAllPorts().size(); i++) {
            SerialPort currentPort = tmpComPorts.getAllPorts().get(i);
            CB_ComPorts.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (currentPort.getSystemPortName().equals(prop.getPorts()[0])) {
                CB_ComPorts.setSelectedIndex(i);
            }
        }

        //Восстановление выбранных ранее (до закрытия программы) портов
        //poolComConnections.clear();
        for (int i = 0; i < prop.getPorts().length; i++) { //Перебор по вкладкам из файла с настройками
            for (SerialPort somePort : tmpComPorts.getAllPorts()) { //Перебор по доступным портам
                if (prop.getPorts()[i] != null)
                    if (prop.getPorts()[i].equals(somePort.getSystemPortName())) {
                        //что-то надо делать
                    }
                //poolComConnections.add(tmpComPorts);
            }
        }


        ProtocolsList[] protocolsLists = ProtocolsList.values();
        for (int i = 0; i < protocolsLists.length; i++) {
            CB_Protocol.addItem(protocolsLists[i].getValue());
            if (restoredFromFile.getProtocol(0) == i) {
                CB_Protocol.setSelectedIndex(i);
            }
        }

        log.debug("Добавление слушатлей действий");
        BT_Update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка обновления списка ком-портов" + tab);
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
                log.info("Выход из программы" + tab);

//                if (SpringLoader.ctx != null && SpringLoader.ctx.isRunning()) {
//                    SpringLoader.ctx.close();
//                }


                thPool.shutdownNow();
                uiThPool.shutdownNow();
                MainWindow.super.dispose();
                System.exit(0);
            }
        });

        BT_Open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка открытия ком-порта" + tab);
                boolean needCreatePoolServ = true;
                SerialPort editedPort = null;
                PoolService ps = null;
                for (PoolService poolService : poolServices) {
                    if (poolService.containTabDev(tab)) {
                        needCreatePoolServ = false;
                        editedPort = poolService.getComPort();
                        ps = poolService;
                        log.info("Для вкладки " + tab + " найден сервис опроса");
                    }
                }
                if (needCreatePoolServ) {
                    createPoolService(tab, false, false, 1200);
                    for (PoolService poolService : poolServices) {
                        if (poolService.containTabDev(tab)) {
                            needCreatePoolServ = false;
                            editedPort = poolService.getComPort();
                            ps = poolService;
                        }
                    }
                }
                if (editedPort == null && ps != null) {
                    //ps.setupComConnection(selectedCom.activePort);
                }


                if (editedPort != null) {
                    if (editedPort.isOpen()) {
                        editedPort.closePort();
                        editedPort.setComPortParameters(BaudRatesList.getNameLikeArray(CB_BaudRate.getSelectedIndex()), 8, 1, SerialPort.NO_PARITY, false);
                        editedPort.setBaudRate(BaudRatesList.getNameLikeArray(CB_BaudRate.getSelectedIndex()));
                        editedPort.setNumDataBits(DataBitsList.getNameLikeArray(CB_DataBits.getSelectedIndex()));
                        editedPort.setParity(ParityList.values()[CB_Parity.getSelectedIndex()].getValue()); //Работает за счет совпадения индексов с библиотечными
                        editedPort.setNumStopBits(StopBitsList.getNameLikeArray(CB_StopBit.getSelectedIndex()));
                        //poolComConnections.get(tab).activePort.removeDataListener();
                        editedPort.removeDataListener();
                        saveParameters();
                        editedPort.openPort();
                        addCustomMessage("Порт переоткрыт.");
                    } else {
                        editedPort.setComPortParameters(BaudRatesList.getNameLikeArray(CB_BaudRate.getSelectedIndex()), 8, 1, SerialPort.NO_PARITY, false);
                        editedPort.setBaudRate(BaudRatesList.getNameLikeArray(CB_BaudRate.getSelectedIndex()));
                        editedPort.setNumDataBits(DataBitsList.getNameLikeArray(CB_DataBits.getSelectedIndex()));
                        editedPort.setParity(ParityList.values()[CB_Parity.getSelectedIndex()].getValue()); //Работает за счет совпадения индексов с библиотечными
                        editedPort.setNumStopBits(StopBitsList.getNameLikeArray(CB_StopBit.getSelectedIndex()));
                        //poolComConnections.get(tab).activePort.removeDataListener();
                        editedPort.removeDataListener();
                        saveParameters();
                        editedPort.openPort();
                        addCustomMessage("Порт сконфигурирован и открыт");
                    }
                    if (editedPort.isOpen()) {
                        addCustomMessage("Порт " + editedPort.getSystemPortName() + " открыт успешно! ");
                        prop.setPortForTab(editedPort.getSystemPortName(), tab);
                    } else {
                        addCustomMessage("Ошибка открытия порта " + editedPort.getSystemPortName() + "! Код ошибки: " + editedPort.getLastErrorCode());
                    }

                } else {
                    log.info("Для вкладки " + tab + " найден сервис опроса даже после принудительного создания сервиса опроса");
                }

                checkIsUsedPort();
            }
        });

        BT_Close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка закрытия ком-порта" + tab);
                PoolService ps = null;
                for (PoolService poolService : poolServices) {
                    if (poolService.containTabDev(tab)) {
                        ps = poolService;
                        log.info("Для вкладки " + tab + " найден сервис опроса");
                    }
                }

                if (ps == null) {
                    addCustomMessage("Попытка закрыть ком-порт у несуществующего потока опроса");
                    return;
                }
                ps.getComPort().flushDataListener();
                ps.getComPort().removeDataListener();
                ps.getComPort().flushIOBuffers();
                ps.getComPort().closePort();


                log.info("На вкладке " + tab + " попытка закрыть ком порт");
                log.info("Порт " + ps.getComPort().getSystemPortName());

                if (!ps.getComPort().isOpen()) {
                    addCustomMessage("Порт " + poolServices.get(tab).getComPort().getSystemPortName() + " закрыт");
                } else {
                    addCustomMessage("Ошибка обращения к порту");
                    log.warn("Ошибка обращения к порту");
                }

                poolServices.remove(tab);
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
            }
        });

        CB_Log.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PoolService ps = findPoolServiceByTabNumber();
                if (ps != null) {
                    ps.setNeedLog(CB_Log.isSelected(), tab);
                } else {
                    log.info("Для текущей влкадки потока опроса не существует");
                }
            }
        });

        BT_Send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Pressed BT_Send");
                renderData();

            }
        });

        CB_BaudRate.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setBaudRate(tab, CB_BaudRate.getSelectedIndex());
            }
        });
        CB_StopBit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setStopBits(tab, CB_StopBit.getSelectedIndex());
            }
        });
        CB_DataBits.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setDataBits(tab, CB_DataBits.getSelectedIndex());
            }
        });
        CB_Parity.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setParityBits(tab, CB_Parity.getSelectedIndex());
            }
        });
        CB_Protocol.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                leftPanState.setProtocol(tab, CB_Protocol.getSelectedIndex());
            }
        });

        BT_AddDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка добавить устройство");

                tab = tabbedPane1.getTabCount();
                leftPanState.addEntry();
                textToSendValue.add(textToSend.getText());
                prefToSendValue.add(prefOneToSend.getText());
                updateLeftPaneStateClassFromUI();

                lastGotedValueFromStorage.add(tab, 0);//инициализация очереди

                JTextPane logDataTransferJtextPanelForAdd = new JTextPane();
                JScrollPane logDataTransferJscrollPanelForAdd = new JScrollPane();
                JPanel panelForAdd = new JPanel();

                logDataTransferJtextPanelForAdd.setName("Jtext dev" + tab);
                logDataTransferJscrollPanelForAdd.setName("Jscroll dev" + tab);
                panelForAdd.setName("JPanel dev" + tab);

                panelForAdd.setLayout(new BorderLayout());

                int numForAdd = logDataTransferJtextPanel.size();
                logDataTransferJtextPanel.add(logDataTransferJtextPanelForAdd);
                logDataTransferJtextPanelForAdd.setEditable(false);
                logDataTransferJtextPanelForAdd.setDoubleBuffered(true);
                logDataTransferJtextPanelForAdd.setText("dev " + (tabbedPane1.getTabCount() + 1) + "\n sample string \n");

                logDataTransferJscrollPanelForAdd.setViewportView(logDataTransferJtextPanel.get(numForAdd));
                logDataTransferJscrollPanelForAdd.setPreferredSize(new Dimension(400, 400));
                logDataTransferJscrollPanelForAdd.setName("dev " + (tabbedPane1.getTabCount() + 1));

                panelForAdd.add(logDataTransferJscrollPanelForAdd, BorderLayout.CENTER);

                panelForAdd.setEnabled(false);


                StringBuilder sb = new StringBuilder();
                sb.append("dev");
                sb.append((tabbedPane1.getTabCount() + 1));
                boolean needRename = false;
                for (int i = 0; i < currTabCount; i++) {
                    if (tabbedPane1.getTitleAt(i).equalsIgnoreCase(sb.toString())) {
                        needRename = true;
                    }
                }
                if (needRename) {

                    for (int i = 0; i < tabbedPane1.getTabCount(); i++) {
                        tabbedPane1.setSelectedIndex(i);
                        tab = i;
                        try {
                            Thread.sleep(200);
                        } catch (InterruptedException ex) {
                            //throw new RuntimeException(ex);
                        }
                        log.info("Изменена нумерация вкладки с " + tabbedPane1.getTitleAt(i) + " на dev" + (i + 1));
                        addCustomMessage("Изменена нумерация вкладки с " + tabbedPane1.getTitleAt(i) + " на dev" + (i + 1));
                        tabbedPane1.setTitleAt(i, "dev" + (i + 1));
                    }
                }
                tabbedPane1.addTab(sb.toString(), panelForAdd);

                //poolComConnections.add(new ComPort());

                currTabCount = tabbedPane1.getTabCount();
                checkIsUsedPort();
                if (tabbedPane1.getTabCount() > 0) {
                    BT_RemoveDev.setEnabled(true);
                }
            }

        });

        BT_RemoveDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tab = tabbedPane1.getSelectedIndex();
                log.info("Нажата кнопка удалить устройство на вкладке " + tab);
                textToSendValue.remove(tab);
                prefToSendValue.remove(tab);
                lastGotedValueFromStorage.remove(tab);
                logDataTransferJtextPanel.remove(tab);

                leftPanState.removeEntry(tab);


                PoolService ps = findPoolServiceByOpenedPort();
                if (ps != null) {
                    if (ps.containTabDev(tab)) {
                        ps.setNeedPool(tab, false);
                        log.info("Задача была удалена");
                    } else {
                        log.info("Выбранная вкладка не найдена в потоке");
                    }
                }
                if (poolServices.size() > tab) {
                    SerialPort currentConnection = poolServices.get(tab).getComPort();
                    if (currentConnection != null) {
                        log.info(currentConnection.getSystemPortName());
                        currentConnection.closePort();
                        log.info("Будет закрыт");
                        poolServices.get(tab).setNeedPool(tab, false);
                        poolServices.remove(tab);
                        //poolComConnections.remove(tab);


                    } else {
                        log.info("Уже закрыт");
                        poolServices.remove(tab);
                        //poolComConnections.remove(tab);

                    }
                }

                tabbedPane1.removeTabAt(tab);
                AnswerStorage.removeAnswersForTab(tab);


                currTabCount = tabbedPane1.getTabCount();

                if (currTabCount == 0) {
                    BT_RemoveDev.setEnabled(false);
                }
                saveParameters();
            }
        });
        textToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (textToSendValue.size() > tab || prefToSendValue.size() > tab) {
                    textToSendValue.set(tab, textToSend.getText());
                    prefToSendValue.set(tab, prefOneToSend.getText());
                    updateLeftPaneStateClassFromUI();
                    saveParameters();
                    if (poolServices.size() > tab) {
                        poolServices.get(tab).setTextToSendString(prefOneToSend.getText() + textToSend.getText(), tab);
                    }
                } else {
                    log.warn("Ошибка при обновлении пула команд для опроса");
                }
            }
        });
        prefOneToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (textToSendValue.size() > tab || prefToSendValue.size() > tab) {
                    textToSendValue.set(tab, textToSend.getText());
                    prefToSendValue.set(tab, prefOneToSend.getText());

                    saveParameters();
                    if (poolServices.size() > tab) {
                        poolServices.get(tab).setTextToSendString(prefOneToSend.getText() + textToSend.getText(), tab);
                    }
                } else {
                    log.warn("Ошибка при обновлении пула префиксов для опроса");
                }
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
                //textToSend.setText(textToSendValue.get(tab));
                CB_Pool.setSelected(isPooled());
                CB_Log.setSelected(isLogged());
                textToSend.setText(textToSendValue.get(tab));
                prefOneToSend.setText(prefToSendValue.get(tab));
                updateLeftPaneFromClass();

                PoolService ps = null;

                for (PoolService poolService : poolServices) {
                    if (poolService.containTabDev(tab)) {
                        ps = poolService;
                        break;
                    }
                }
                if (ps == null) {
                    log.info("Для выбранной вкладки нет сервиса опроса");
                } else {
                    SerialPort serial = ps.getComPort();
                    if (serial == null) {
                        log.info("Для выбранной вкладки сервис опроса не содержит активного соединения");
                    } else {
                        ArrayList<SerialPort> portsListForUpdateState = comPorts.getAllPorts();
                        for (int i = 0; i < portsListForUpdateState.size(); i++) {
                            if (prop.getPorts().length <= tab) {
                                log.warn("Ошибка...");

                            } else {
                                if (portsListForUpdateState.get(i).getSystemPortName().equals(prop.getPorts()[tab])) {
                                    CB_ComPorts.setSelectedIndex(i);
                                } else {
                                    log.warn("Пропущен ком-порт для выбора. Сравнение... Номер вкладки: " + i +
                                            " Название ком порта по номеру:" + portsListForUpdateState.get(i).getSystemPortName() +
                                            " Название ком порта в свойствах для номера вкладки:" + prop.getPorts()[tab]);
                                    /* Искать тут */
                                }
                            }

                        }
                    }
                }
                checkIsUsedPort();


            }
        });

        prefToSendValue.add(prefOneToSend.getText());
        textToSendValue.add(textToSend.getText());
        lastGotedValueFromStorage.add(tab, 0);


        int tabCount = Math.max(0, prop.getTabCounter());
        for (int i = 0; i < tabCount; i++) {
            BT_AddDev.doClick(); //Добавление новой вкладки (клик)
        }


        if (prop.getCommands() != null && prop.getCommands().length > 0) {
            textToSendValue.clear();
            for (int i = 0; i < prop.getCommands().length; i++) {
                textToSendValue.add(prop.getCommands()[i]);
                if (tab == i) {
                    textToSend.setText(prop.getCommands()[i]);
                }
            }
        }
        if (prop.getPrefixes() != null && prop.getPrefixes().length > 0) {
            prefToSendValue.clear();
            for (int i = 0; i < prop.getPrefixes().length; i++) {
                prefToSendValue.add(prop.getPrefixes()[i]);
                if (tab == i) {
                    prefOneToSend.setText(prop.getPrefixes()[i]);
                }
            }
        }


        leftPanState = prop.getLeftPanelStateCollection();

        tab = 0;
        textToSend.setText(textToSendValue.get(tab));
        prefOneToSend.setText(prefToSendValue.get(tab));
        updateLeftPaneFromClass();
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
                if (tab < prefToSendValue.size()) {
                    PoolService ps = findPoolServiceByTabNumber();
                    if (ps != null)
                        ps.setTextToSendString(prefOneToSend.getText() + textToSend.getText(), tab);
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

            public void update() {
                if (tab < textToSendValue.size()) {
                    PoolService ps = findPoolServiceByTabNumber();
                    if (ps != null)
                        ps.setTextToSendString(prefOneToSend.getText() + textToSend.getText(), tab);
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
                if (poolServices.size() > tab && poolServices.get(tab) != null) {
                    poolServices.get(tab).setPoolDelay(IN_PoolDelay.getText());
                    log.info(" выполнено обновление периода опроса для владки " + tab);
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                thPool.shutdownNow();
                e.getWindow().dispose();
            }
        });


    }


    public static void waitTab(int tabNumber) {
        /*
        int mySuperWatchdog = 0;
        while (MainWindow.isBusy(tabNumber)) {
            mySuperWatchdog++;
            try {
                System.out.println("Ожидаю освобождение вкладки...");
                Thread.sleep(100);
            } catch (InterruptedException e) {
                //throw new RuntimeException(e);
            }
            if (mySuperWatchdog > 1000) {
                log.error("Выход из ожидания выполнения отправки через web-интерфейс по вачдогу!");
                break;
            }
        }

         */
    }

    public static void webSend(int tabSend, String command) {
        /*
        MainWindow.mainWindow.setCurrentTab(tabSend);
        System.out.println("Установил активную вкладку " + tabSend);
        MainWindow.waitTab(tabSend);
        // MainWindow.mainWindow.addCustomMessage("Команда из web-интерфейса не была отправлена. Метод не реализован. Текст команды." + command);
        String prevCommand = MainWindow.mainWindow.getTextToSendValue(tabSend);
        System.out.println("Предыдущая команда была " + prevCommand);


        MainWindow.mainWindow.setTextToSendValue(tabSend, command);
        MainWindow.mainWindow.startSend(true);
        System.out.println("Установил новую команду " + command);
        System.out.println("Инициировал отправку");
        MainWindow.waitTab(tabSend);
        //Возможно нужно ждать
        MainWindow.mainWindow.setCurrentTab(tabSend);
        System.out.println("Установил активную вкладку " + tabSend);
        MainWindow.mainWindow.setTextToSendValue(tabSend, prevCommand);
        System.out.println("Установил новую команду " + prevCommand);

        MainWindow.mainWindow.startSend(true);
        System.out.println("Инициировал отправку");

         */
    }

    public void setTextToSendValue(int tabInp, String text) {
        textToSendValue.set(tab, textToSend.getText());
        if (tabbedPane1.getTabCount() > tabInp && tab == tabInp) {
            if (text == null || text.isEmpty()) {

            } else {
                textToSend.setText(text);
                textToSendValue.set(tabInp, text);
            }
        }
    }

    public String getTextToSendValue(int tabInp) {
        if (tabbedPane1.getTabCount() > tabInp && tab == tabInp) {
            return textToSendValue.get(tabInp);
        } else {
            return null;
        }

    }

    public void setCurrentTab(int tabInp) {
        if (tabbedPane1.getTabCount() > tabInp) {
            this.tab = tabInp;
            tabbedPane1.setSelectedIndex(tabInp);
        }
    }


    public static boolean isBusy(int tabNumber) {
        return true;
        /*
        if (tabNumber < 0 || tabNumber >= MainWindow.getCurrTabCount()) {
            log.warn("Обращение с неверным номером вкладки. Возвращаю статус свободна.");
            return false;
        }
        ArrayList<PoolService> poolServices = MainWindow.getPoolServices();
        for (PoolService poolService : poolServices) {
            if (poolService.containTabDev(tabNumber)) {
                return poolService.isComBusy();
            }
        }
        log.warn("Для указанной вкладки не найден сервис опроса. предпологается, что ком-порт свободен.");
        return false;

         */
    }


    public void startSend(boolean isBtn) {
        //isBtn - вызов по кнопке / pool - вызов про чекбоксу
        if (isBtn) {
            log.info("Инициализация отправки по нажатию кнопки ОТПРАВИТЬ");
        } else {
            log.info("Инициализация отправки по по таймеру в цикле");
        }
        log.info("Статус опроса на владке " + tab + " " + CB_Pool.isSelected());
        prefToSendValue.set(tab, prefOneToSend.getText());
        textToSendValue.set(tab, textToSend.getText());


        saveParameters();
        boolean pool = CB_Pool.isSelected();
        int poolDelay = 1000;
        try {
            poolDelay = Integer.parseInt(IN_PoolDelay.getText());
        } catch (Exception e1) {
            IN_PoolDelay.setText("10000");
        }

        createPoolService(tab, pool, isBtn, poolDelay);

    }

    private void updateLeftPaneFromClass() {
        CB_DataBits.setSelectedIndex(leftPanState.getDataBits(tab));
        CB_Parity.setSelectedIndex(leftPanState.getParityBits(tab));
        CB_StopBit.setSelectedIndex(leftPanState.getStopBits(tab));
        CB_BaudRate.setSelectedIndex(leftPanState.getBaudRate(tab));
        CB_Protocol.setSelectedIndex(leftPanState.getProtocol(tab));
    }

    private void updateLeftPaneStateClassFromUI() {
        leftPanState.setParityBits(tab, CB_Parity.getSelectedIndex());
        leftPanState.setDataBits(tab, CB_DataBits.getSelectedIndex());
        leftPanState.setStopBits(tab, CB_StopBit.getSelectedIndex());
        leftPanState.setBaudRate(tab, CB_BaudRate.getSelectedIndex());
        leftPanState.setProtocol(tab, CB_Protocol.getSelectedIndex());
    }

    private void createPoolService(int tab, boolean pool, boolean isBtn, int poolDelay) {
        PoolService psT = findPoolServiceByTabNumber();
        PoolService psC = findPoolServiceByOpenedPort();
        PoolService ps = null;
        if (psT != null)
            ps = psT;//by Tab
        else
            ps = psC;//by Com Port

        if (ps != null) {
            log.info("Порт уже используется, проверка  среди запущенных потоков");
            if (ps.containTabDev(tab)) {
                log.info("Для текущей вкладки устройство существует в потоке опроса");
                if (pool || isBtn) {
                    if (isBtn) {
                        log.info("Разовая отправка");
                        ps.sendOnce(prefToSendValue.get(tab) + textToSendValue.get(tab), tab, false);

                    } else {
                        log.info("Команда к запуску");
                        ps.setNeedPool(tab, true);
                        log.info("pool delay " + ps.getPoolDelay());
                        log.info("text to send " + ps.getTextToSensByTab(tab));
                        log.info("isNeedPool " + ps.isNeedPool(tab));
                        log.info("isLoggedTab " + ps.isLoggedTab(tab));
                        log.info("isRootTab " + ps.isRootTab(tab));
                        log.info("isNeedLog " + ps.isNeedLog(tab));
                        ps.setPoolDelay(String.valueOf(poolDelay));
                        //ps.run();
                        thPool.submit(ps);
                        try {
                            Thread.sleep(60);
                        } catch (InterruptedException e) {
                            //throw new RuntimeException(e);
                        }
                    }


                } else {
                    log.info("Команда к остановке опроса");
                    ps.setNeedPool(tab, false);
                    if (ps.isRootTab(tab)) {
                        log.info("Текущий поток является корневым для других");
                    } else {
                        log.info("Вкладка одинока. Поток будет завершен");
                        if (poolServices.size() > tab) {
                            poolServices.get(tab).getComPort().closePort();
                            poolServices.remove(tab);
                        } else {
                            log.warn("Попытка завершения потока со владки по номеру большей, чем количество потоков");
                        }

                    }
                }
            } else {

                if (!isBtn) {
                    log.info("Для текущей вкладки устройство не существует в потоке опроса (по чек-боксу)");
                    ps.addDeviceToService(tab, prefToSendValue.get(tab) + textToSendValue.get(tab), false, true);
                } else {
                    log.info("Для текущей вкладки устройство не существует в потоке опроса (по кнопке)");
                    ps.addDeviceToService(tab, prefToSendValue.get(tab) + textToSendValue.get(tab), false, false);
                }

            }
        } else {
            log.info("Порт не используется, создание нового потока");
            boolean forEvent;
            if (isBtn) {
                forEvent = true;
            } else {
                forEvent = !pool;
            }
            log.info("соединение будет обрабатывать event события => " + forEvent);
            ComPort avaComPorts = new ComPort();
            avaComPorts.setPort(CB_ComPorts.getSelectedIndex());
            poolServices.add(new PoolService(
                    ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()),
                    prefToSendValue.get(tab) + textToSendValue.get(tab),
                    avaComPorts.activePort,
                    poolDelay,
                    false,
                    forEvent,
                    tab));

            if (isBtn) {
                poolServices.get(poolServices.size() - 1).setNeedPool(tab, false);
                poolServices.get(poolServices.size() - 1).sendOnce(prefToSendValue.get(tab) + textToSendValue.get(tab), tab, false);
                thPool.submit(poolServices.get(poolServices.size() - 1));
                log.info("Поток создан и запущен один раз");
                try {
                    Thread.sleep(60);
                } catch (InterruptedException e) {
                    //throw new RuntimeException(e);
                }
            } else if (pool) {
                poolServices.get(poolServices.size() - 1).setNeedPool(tab, true);
                thPool.submit(poolServices.get(poolServices.size() - 1));
                log.info("Поток создан и запущен");
            } else {

                poolServices.get(poolServices.size() - 1).setNeedPool(tab, false);
                thPool.submit(poolServices.get(poolServices.size() - 1));

                //poolServices.get(poolServices.size() - 1).run();
                log.info("Поток создан и запущен для Event");
            }
        }
    }

    private void checkIsUsedPort() {
        currTabCount = tabbedPane1.getTabCount();
        closeUnusedPorts();

        int targetComNum = CB_ComPorts.getSelectedIndex();
        int mainTabForCom = 0;

        //Проверка, что порт уже открыт (блокировка кнопки ОТКРЫТЬ)
        boolean alreadyOpen = false;
        for (int j = 0; j < poolServices.size(); j++) {
            if (poolServices.get(j) != null &&
                    poolServices.get(j).getComPort() != null &&
                    poolServices.get(j).getComPort().isOpen() &&
                    poolServices.get(j).getComPortForJCombo() == targetComNum) {
                alreadyOpen = true;
            }
        }

        if (alreadyOpen) {
            BT_Open.setEnabled(false);
            CB_Protocol.setEnabled(false);
            //addCustomMessage("Соединение с выбранным портом уже установлено");
        } else {
            BT_Open.setEnabled(true);
            CB_Protocol.setEnabled(true);
        }


        int rootTab = -1;
        for (int i = 0; i < currTabCount; i++) {
            for (int j = 0; j < poolServices.size(); j++) {
                if (poolServices.get(j).getComPort() != null) {//Не виртуальный опрос
                    if (poolServices.get(j).getComPortForJCombo() == targetComNum) {//Выбранный порт был выбран ранее
                        if (poolServices.get(j).isRootTab(i)) {//Просматриваемая вкладка корневая для опроса
                            //rootTab = i;
                            //log.info("Нашел корневую");
                            //rootTab = i;
                        } else if (poolServices.get(j).containTabDev(i)) {//просматриваемая вкладка виртуальная, но содержится
                            log.info("Нашел виртуальную");
                            rootTab = i;
                        }
                    }
                }
            }
        }
        if (rootTab > -1) {
            if (rootTab != tab) {
                log.info("Корневая вкладка для ком-порта " + rootTab);
                addCustomMessage("Управление выбранным ком-портом возможно на вкладке " + (rootTab + 1));
                BT_Close.setEnabled(false);
                BT_Open.setEnabled(false);
                CB_Protocol.setEnabled(false);

            } else {
                log.info("Это и есть корневая вкладка для ком-порта " + rootTab);
                BT_Close.setEnabled(true);
                BT_Open.setEnabled(true);
                CB_Protocol.setEnabled(true);
            }
        }


    }

    private void closeUnusedPorts() {
        /*
        int coutTabs = getCurrTabCount();
        for (int i = 0; i < poolServices.size(); i++) {
            if (!(i < coutTabs)) {
                poolServices.get(i).getComPort().closePort();
                AnswerStorage.removeAnswersForTab(i);
                poolServices.remove(i);
            }
        }

         */
    }

    private PoolService findPoolServiceByOpenedPort() {
        for (PoolService poolService : poolServices) {
            if (CB_ComPorts.getSelectedIndex() == poolService.getComPortForJCombo()) {
                return poolService;
            }
        }
        return null;
    }

    private PoolService findPoolServiceByTabNumber() {
        for (PoolService poolService : poolServices) {
            if (poolService.containTabDev(tab)) {
                return poolService;
            }
        }
        return null;
    }

    private boolean isPooled() {
        PoolService ps = findPoolServiceByTabNumber();
        if (ps != null) {
            return ps.isNeedPool(tab);
        }
        return false;
    }

    private boolean isLogged() {
        PoolService ps = findPoolServiceByTabNumber();
        if (ps != null) {
            return ps.isNeedLog(tab);
        }
        return false;
    }

    private String getPoolText() {
        PoolService ps = findPoolServiceByTabNumber();
        if (ps != null) {
            return ps.getTextToSensByTab(tab);
        }
        return "";
    }

    private boolean havePoolService() {
        for (PoolService poolService : poolServices) {
            if (poolService.containTabDev(tab)) {
                return true;
            }
        }
        return false;
    }


    public void addCustomMessage(String str) {
        Document doc = logDataTransferJtextPanel.get(tab).getDocument();
        str = MyUtilities.CUSTOM_FORMATTER.format(LocalDateTime.now()) + ":\t" + str + "\n";
        try {
            doc.insertString(doc.getLength(), str, null);
        } catch (BadLocationException ex) {
            //throw new RuntimeException(ex);
        }
        logDataTransferJtextPanel.get(tab).setCaretPosition(doc.getLength());
    }

    public void renderData() {
        tab = tabbedPane1.getSelectedIndex();
        Document doc = logDataTransferJtextPanel.get(tab).getDocument();

        try {
            an = AnswerStorage.getAnswersQueForTab(lastGotedValueFromStorage.get(tab), tab, true);
            lastGotedValueFromStorage.set(tab, an.getPosition());
            doc.insertString(doc.getLength(), an.getAnswerPart(), null);
            logDataTransferJtextPanel.get(tab).setCaretPosition(doc.getLength());//Прокрутка текста
        } catch (BadLocationException ex) {
            //throw new RuntimeException(ex);
        }
        doc = null;
        countRender++;
        if (countRender > 90) {
            System.gc(); //Runtime.getRuntime().gc();
        }

    }

    @Override
    public boolean isEnable() {
        //return this.isShowing();
        return true;
    }


    public static int getCurrTabCount() {
        return 5; //ToDo fix it
    }


    private void saveParameters() {
        log.debug("Обновление файла настроек со вкладки" + tab);

        /*if (poolComConnections.get(tab).activePort != null) {
            prop.setLastPorts(poolComConnections, currTabCount);
        }
         */
        prop.setLastLeftPanel(leftPanState);
        Logger root = Logger.getRootLogger();
        prop.setLogLevel(root.getLevel());
        prop.setTabCounter(currTabCount);
        prop.setLastCommands(textToSendValue);
        prop.setLastPrefixes(prefToSendValue);

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
        panel3.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        Terminal.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textToSend = new JTextField();
        textToSend.setForeground(new Color(-10328984));
        textToSend.setText("M^");
        panel3.add(textToSend, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(100, 40), new Dimension(150, 25), new Dimension(-1, 25), 0, false));
        BT_Send = new JButton();
        BT_Send.setText("Отправить");
        panel3.add(BT_Send, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        prefOneToSend = new JTextField();
        prefOneToSend.setText("001");
        panel3.add(prefOneToSend, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(50, 40), new Dimension(60, 25), new Dimension(70, 25), 0, false));
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
