/*
Файл содержит код, выполняющийся при взаимодействии с виндовс-окном приложения
 */
package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.ComPort;
import org.example.services.TabAnswerPart;
import org.example.utilites.*;
import org.example.services.PoolService;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class MainWindow extends JFrame implements Rendeble {
    private JPanel contentPane;

    private static final Logger log = Logger.getLogger(MainWindow.class);
    private int countRender = 0;

    private final ExecutorService thPool = Executors.newCachedThreadPool();
    private final ExecutorService uiThPool = Executors.newCachedThreadPool();

    private final MyProperties prop = new MyProperties();
    private final ArrayList<String> textToSendValue = new ArrayList<>();
    private final ArrayList<JScrollPane> logDataTransferJscrollPanel = new ArrayList<>();
    private ArrayList<JTextPane> logDataTransferJtextPanel = new ArrayList<>();
    private ArrayList<PoolService> poolServices = new ArrayList<>();
    private ArrayList<ComPort> poolComConnections = new ArrayList<>();
    private ArrayList<Integer> lastGotedValueFromStorage = new ArrayList<>();

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
    private ProtocolsList protocol = ProtocolsList.IGM10ASCII;

    /**
     * Number active tab
     **/
    private int tab = 0;

    public MainWindow() {
        contentPane.getName();
        poolComConnections.add(new ComPort());
        JmenuFile jmenu = new JmenuFile(prop);
        // Создание строки главного меню
        JMenuBar menuBar = new JMenuBar();

        // Добавление в главное меню выпадающих пунктов меню
        menuBar.add(jmenu.createFileMenu());
        menuBar.add(jmenu.createSettingsMenu());
        menuBar.add(jmenu.createViewMenu(uiThPool));
        menuBar.add(jmenu.createSystemParametrs(uiThPool));

        setJMenuBar(menuBar);
        setContentPane(contentPane);

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);


        BaudRatesList[] baudRate = BaudRatesList.values();
        for (int i = 0; i < baudRate.length; i++) {
            CB_BaudRate.addItem(baudRate[i].getValue() + "");
            if (prop.getLastComSpeed() == baudRate[i].getValue()) {
                CB_BaudRate.setSelectedIndex(i);
            }
        }

        DataBitsList[] dataBits = DataBitsList.values();
        for (int i = 0; i < dataBits.length; i++) {
            CB_DataBits.addItem(dataBits[i].getValue());
            if (prop.getLastDataBits() == dataBits[i].getValue()) {
                CB_DataBits.setSelectedIndex(i);
            }
        }

        ParityList[] parityLists = ParityList.values();
        for (int i = 0; i < parityLists.length; i++) {
            CB_Parity.addItem(parityLists[i].getName());
            if (prop.getLastParity().equalsIgnoreCase(parityLists[i].getName())) {
                CB_Parity.setSelectedIndex(i);
            }
        }

        StopBitsList[] stopBitsLists = StopBitsList.values();
        for (int i = 0; i < stopBitsLists.length; i++) {
            CB_StopBit.addItem(stopBitsLists[i].getValue());
            if (prop.getLastStopBits() == stopBitsLists[i].getValue()) {
                CB_StopBit.setSelectedIndex(i);
            }
        }


        for (int i = 0; i < poolComConnections.get(0).getAllPorts().size(); i++) {
            SerialPort currentPort = poolComConnections.get(0).getAllPorts().get(i);
            CB_ComPorts.addItem(currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if (i == poolComConnections.get(0).getComNumber()) {
                CB_ComPorts.setSelectedIndex(i);
            }
        }

        ProtocolsList[] protocolsLists = ProtocolsList.values();
        for (int i = 0; i < protocolsLists.length; i++) {
            CB_Protocol.addItem(protocolsLists[i].getValue());
            if (prop.getLastProtocol().equalsIgnoreCase(protocolsLists[i].getValue())) {
                CB_Protocol.setSelectedIndex(i);
            }
        }

        BT_Update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка обновления списка ком-портов" + tab);
                poolComConnections.get(tab).updatePorts();
                CB_ComPorts.removeAllItems();
                for (SerialPort port : poolComConnections.get(tab).getAllPorts()) {
                    CB_ComPorts.addItem(port.getSystemPortName() + " (" + MyUtilities.removeComWord(port.getPortDescription()) + ")");
                }
            }
        });

        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent windowEvent) {
                log.info("Выход из программы" + tab);
                if (SpringLoader.ctx != null && SpringLoader.ctx.isRunning()) {
                    SpringLoader.ctx.close();
                }
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
                poolComConnections.get(tab).setPort(CB_ComPorts.getSelectedIndex());
                poolComConnections.get(tab).activePort.setComPortParameters(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()), 8, 1, SerialPort.NO_PARITY, false);
                poolComConnections.get(tab).activePort.setBaudRate(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
                poolComConnections.get(tab).activePort.setNumDataBits(DataBitsList.getLikeArray(CB_DataBits.getSelectedIndex()));
                poolComConnections.get(tab).activePort.setParity(ParityList.values()[CB_Parity.getSelectedIndex()].getValue()); //Работает за счет совпадения индексов с библиотечными
                poolComConnections.get(tab).activePort.setNumStopBits(StopBitsList.getLikeArray(CB_StopBit.getSelectedIndex()));
                saveParameters(null);
            }
        });


        BT_Close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка закрытия ком-порта" + tab);
                poolComConnections.get(tab).activePort.flushDataListener();
                poolComConnections.get(tab).activePort.closePort();
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

        BT_AddDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                log.info("Нажата кнопка добавить устройство");
                textToSendValue.add("001M^");
                lastGotedValueFromStorage.add(0);
                JPanel panel = new JPanel();
                panel.setLayout(new BorderLayout());
                logDataTransferJscrollPanel.add(new JScrollPane());
                logDataTransferJtextPanel.add(new JTextPane());
                logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setEditable(false);
                logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setDoubleBuffered(true);
                logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setText("dev " + (tabbedPane1.getTabCount() + 1) + "\n sample string \n");
                logDataTransferJscrollPanel.get(logDataTransferJscrollPanel.size() - 1).setViewportView(logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1));
                logDataTransferJscrollPanel.get(logDataTransferJscrollPanel.size() - 1).setPreferredSize(new Dimension(400, 400));

                poolComConnections.add(new ComPort());
                panel.add(logDataTransferJscrollPanel.get(logDataTransferJscrollPanel.size() - 1), BorderLayout.CENTER);
                panel.setEnabled(false);
                tabbedPane1.addTab("dev" + (tabbedPane1.getTabCount() + 1), panel);
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
                lastGotedValueFromStorage.remove(tab);
                PoolService ps = findPoolServiceByOpenedPort();
                if (ps != null) {
                    if (ps.containTabDev(tab)) {
                        ps.setNeedPool(tab, false);
                        log.info("Задача была удалена");
                    } else {
                        log.info("Выбранная вкладка не найдена в потоке");
                    }
                }
                tabbedPane1.removeTabAt(tab);
                if (tabbedPane1.getTabCount() == 0) {
                    BT_RemoveDev.setEnabled(false);
                }
            }
        });
        textToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (textToSendValue.size() > tab) {
                    textToSendValue.set(tab, textToSend.getText());
                    if (poolServices.size() > tab) {
                        poolServices.get(tab).setTextToSendString(textToSend.getText(), tab);
                    }
                } else {
                    log.warn("Ошибка при обновлении пула команд для опроса");
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
                log.info("Фокус установлен на вкладку " + tab);
                textToSend.setText(textToSendValue.get(tab));
                CB_Pool.setSelected(isPooled());
                CB_Log.setSelected(isLogged());
                textToSend.setText(getPoolText());
            }
        });

        textToSendValue.add(textToSend.getText());
        lastGotedValueFromStorage.add(0);
        BT_AddDev.doClick();
        uiThPool.submit(new RenderThread(this));

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
                        ps.setTextToSendString(textToSend.getText(), tab);
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

    private void startSend(boolean isBtn) {
        //isBtn - вызов по кнопке / pool - вызов про чекбоксу
        if (isBtn) {
            log.info("Инициализация отправки по нажатию кнопки ОТПРАВИТЬ");
        } else {
            log.info("Инициализация отправки по по таймеру в цикле");
        }
        log.info("Статус опроса на владке " + tab + " " + CB_Pool.isSelected());
        protocol = ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex());
        textToSendValue.set(tab, textToSend.getText());
        boolean pool = CB_Pool.isSelected();
        int poolDelay = 1000;
        try {
            poolDelay = Integer.parseInt(IN_PoolDelay.getText());
        } catch (Exception e1) {
            IN_PoolDelay.setText("1000");
        }
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
                        ps.sendOnce(textToSendValue.get(tab), tab, false);

                    } else {
                        log.info("Команда к запуску");
                        ps.setNeedPool(tab, true);
                        System.out.println("pool delay " + ps.getPoolDelay());
                        System.out.println("text to send " + ps.getTextToSensByTab(tab));
                        System.out.println("isNeedPool " + ps.isNeedPool(tab));
                        System.out.println("isLoggedTab " + ps.isLoggedTab(tab));
                        System.out.println("isRootTab " + ps.isRootTab(tab));
                        System.out.println("isNeedLog " + ps.isNeedLog(tab));
                        ps.setPoolDelay(String.valueOf(poolDelay));
                        //ps.run();
                        thPool.submit(ps);
                    }


                } else {
                    log.info("Команда к остановке опроса");
                    ps.setNeedPool(tab, false);
                    if (ps.isRootTab(tab)) {
                        log.info("Текущий поток является корневым для других");
                    } else {
                        log.info("Вкладка одинока. Поток будет завершен");
                        poolServices.remove(tab);
                    }
                }
            } else {
                log.info("Для текущей вкладки устройство не существует в потоке опроса");
                ps.addDeviceToService(tab, textToSendValue.get(tab), false);
            }
        } else {
            log.info("Порт не используется, создание нового потока");
            poolServices.add(new PoolService(
                    ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()),
                    textToSendValue.get(tab),
                    poolComConnections.get(tab).activePort,
                    poolDelay,
                    false,
                    tab));

            if (isBtn) {
                poolServices.get(poolServices.size() - 1).setNeedPool(tab, false);
                poolServices.get(poolServices.size() - 1).sendOnce(textToSendValue.get(tab), tab, false);
                thPool.submit(poolServices.get(poolServices.size() - 1));
                log.info("Поток создан и запущен один раз");
            } else {
                poolServices.get(poolServices.size() - 1).setNeedPool(tab, true);
                thPool.submit(poolServices.get(poolServices.size() - 1));
                log.info("Поток создан и запущен");
            }
        }
    }

    private void checkIsUsedPort() {
        //poolComConnections.get(tab).setPort(CB_ComPorts.getSelectedIndex());
        for (PoolService poolService : poolServices) {
            if (CB_ComPorts.getSelectedIndex() == poolService.getComPortForJCombo()) {
                BT_Open.setEnabled(false);
                CB_Protocol.setEnabled(false);
                CB_Protocol.setSelectedIndex(poolService.getProtocolForJCombo());
                return;
            }
        }
        BT_Open.setEnabled(true);
        CB_Protocol.setEnabled(true);
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


    public void renderData() {
        Document doc = logDataTransferJtextPanel.get(tab).getDocument();
        try {
            an = AnswerStorage.getAnswersQueForTab(lastGotedValueFromStorage.get(tab), tab, true);

            lastGotedValueFromStorage.set(tab, an.getPosition());
            doc.insertString(doc.getLength(), an.getAnswerPart(), null);
            logDataTransferJtextPanel.get(tab).setCaretPosition(doc.getLength());
        } catch (BadLocationException ex) {
            //throw new RuntimeException(ex);
        }
        doc = null;
        countRender++;
        if (countRender > 20) {
            System.gc(); //Runtime.getRuntime().gc();
        }
        log.info("Обновление данных для вкладки " + tab);
    }

    @Override
    public boolean isEnable() {
        //return this.isShowing();
        return true;
    }

    /* --- Метод обновления настроек ---
        Если вызван с параметром NULL, то
        обновляет все.
        Если массив строк, то обновляет
        перечисленное в массиве (по названию
        параметров)
     */
    private void saveParameters(String[] parametersArray) {
        log.info("Обновление файла настроек со вкладки" + tab);
        if (parametersArray == null) {
            prop.setLastComPort(poolComConnections.get(tab).getCurrentComName());
            prop.setLastComSpeed(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
            prop.setLastDataBits(DataBitsList.getLikeArray(CB_DataBits.getSelectedIndex()));
            prop.setLastParity(ParityList.values()[CB_Parity.getSelectedIndex()].getName());
            prop.setLastStopBits(StopBitsList.getLikeArray(CB_StopBit.getSelectedIndex()));
            prop.setLastProtocol(ProtocolsList.getLikeArray(CB_Protocol.getSelectedIndex()));
            Logger root = Logger.getRootLogger();
            prop.setLogLevel(root.getLevel());
        } else {
            for (String s : parametersArray) {
                switch (s) {
                    case "LastComPort":
                        prop.setLastComPort(poolComConnections.get(tab).getCurrentComName());
                        break;
                    case "LastComSpeed":
                        prop.setLastComSpeed(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
                        break;
                    case "LastDataBits":
                        prop.setLastDataBits(DataBitsList.getLikeArray(CB_DataBits.getSelectedIndex()));
                        break;
                    case "LastParity":
                        prop.setLastParity(ParityList.values()[CB_Parity.getSelectedIndex()].getName());
                        break;
                    case "LastStopBit":
                        prop.setLastStopBits(StopBitsList.getLikeArray(CB_StopBit.getSelectedIndex()));
                        break;
                    case "LastProtocol":
                        prop.setLastProtocol(ProtocolsList.getLikeArray(CB_Protocol.getSelectedIndex()));
                        break;
                    default:
                        log.warn("Попытка сохранения неизвестного параметра " + s);

                }
            }
        }

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
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        Terminal.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        textToSend = new JTextField();
        textToSend.setForeground(new Color(-10328984));
        textToSend.setText("001M^");
        panel3.add(textToSend, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(-1, 40), new Dimension(-1, 25), new Dimension(-1, 25), 0, false));
        BT_Send = new JButton();
        BT_Send.setText("Отправить");
        panel3.add(BT_Send, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
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
        IN_PoolDelay.setText("1200");
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
