/*
Файл содержит код, выполняющийся при взаимодействии с виндовс-окном приложения
 */
package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import org.example.services.ComPort;
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


public class MainWindow extends JDialog {
    /*
        Перевести в пул потоков

     */
    private ExecutorService thPool = Executors.newCachedThreadPool();
    private final MyProperties prop = new MyProperties();
    private final ArrayList <Thread> threads = new ArrayList<>();

    private final ArrayList <String> textToSendValue = new ArrayList<>();
    private final ArrayList <JScrollPane> logDataTransferJscrollPanel = new ArrayList<>();

    private ArrayList <JTextPane> logDataTransferJtextPanel = new ArrayList<>();

    private ArrayList <JTextPane> logDataTransferJtextPanel_NEW = new ArrayList<>();

    private ArrayList <PoolService> poolServices = new ArrayList<>();

    private ArrayList <ComPort> poolComConnections = new ArrayList<>();

    private ArrayList <Boolean> logState = new ArrayList<>();

    private JPanel contentPane;

    private JComboBox<String> CB_ComPorts;
    private JComboBox<String> CB_BaudRate;
    private JComboBox CB_DataBits;
    private JComboBox CB_Parity;
    private JComboBox CB_StopBit;
    private JButton BT_Open;
    private JButton BT_Close;
    private JCheckBox CB_Log;
    private JCheckBox CB_Autoconnect;
    private JButton BT_Update;
    private JTextField textToSend;
    private JButton BT_Send;
    private JComboBox CB_Protocol;
    private JCheckBox CB_Pool;

    private JTabbedPane tabbedPane1;
    private JButton BT_AddDev;
    private JButton BT_RemoveDev;
    private JTextField IN_PoolDelay;
    private JButton BT_Search;
    private JTextField textField1;
    private JButton buttonOK;
    private JButton buttonCancel;



    private ProtocolsList protocol = ProtocolsList.IGM10ASCII;

    public MainWindow() {


        poolComConnections.add(new ComPort());

        // Создание строки главного меню
        JMenuBar menuBar = new JMenuBar();

        // Добавление в главное меню выпадающих пунктов меню
        menuBar.add(createFileMenu());
        menuBar.add(createViewMenu());

        setJMenuBar(menuBar);
        setContentPane(contentPane);
        setModal(true);
        //getRootPane().setDefaultButton(buttonOK);

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);



        BaudRatesList[] baudRate = BaudRatesList.values();
        for (int i = 0; i < baudRate.length; i++) {
            CB_BaudRate.addItem( baudRate[i].getValue() + "");
            if(prop.getLastComSpeed() == baudRate[i].getValue()){
                CB_BaudRate.setSelectedIndex(i);
            }
        }

        DataBitsList[] dataBits = DataBitsList.values();
        for (int i = 0; i < dataBits.length; i++) {
            CB_DataBits.addItem( dataBits[i].getValue() );
            if(prop.getLastDataBits() == dataBits[i].getValue()){
                CB_DataBits.setSelectedIndex(i);
            }
        }

        ParityList[] parityLists = ParityList.values();
        for (int i = 0; i < parityLists.length; i++) {
            CB_Parity.addItem( parityLists[i].getName() );
            if(prop.getLastParity().equalsIgnoreCase(parityLists[i].getName())){
                CB_Parity.setSelectedIndex(i);
            }
        }

        StopBitsList[] stopBitsLists = StopBitsList.values();
        for (int i = 0; i < stopBitsLists.length; i++) {
            CB_StopBit.addItem( stopBitsLists[i].getValue() );
            if(prop.getLastStopBits() == stopBitsLists[i].getValue()){
                CB_StopBit.setSelectedIndex(i);
            }
        }


        for (int i = 0; i < poolComConnections.get(0).getAllPorts().size(); i++) {
            SerialPort currentPort = poolComConnections.get(0).getAllPorts().get(i);
            CB_ComPorts.addItem( currentPort.getSystemPortName() + " (" + MyUtilities.removeComWord(currentPort.getPortDescription()) + ")");
            if(i == poolComConnections.get(0).getComNumber()){
                CB_ComPorts.setSelectedIndex(i);
            }
        }

        ProtocolsList[] protocolsLists = ProtocolsList.values();
        for (int i = 0; i < protocolsLists.length; i++) {
            CB_Protocol.addItem( protocolsLists[i].getValue() );
            if(prop.getLastProtocol().equalsIgnoreCase(protocolsLists[i].getValue())){
                CB_Protocol.setSelectedIndex(i);
            }
        }

        BT_Update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Update");
                poolComConnections.get(tabbedPane1.getSelectedIndex()).updatePorts();
                CB_ComPorts.removeAllItems();
                for (SerialPort port :  poolComConnections.get(tabbedPane1.getSelectedIndex()).getAllPorts()) {
                    CB_ComPorts.addItem( port.getSystemPortName() + " (" + MyUtilities.removeComWord(port.getPortDescription()) + ")");
                }
            }
        });

        BT_Open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Open");
                poolComConnections.get(tabbedPane1.getSelectedIndex()).setPort(CB_ComPorts.getSelectedIndex());
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.setComPortParameters(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()),8, 1, SerialPort.NO_PARITY, false);
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.setBaudRate(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.setNumDataBits(DataBitsList.getLikeArray(CB_DataBits.getSelectedIndex()));
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.setParity(ParityList.values()[CB_Parity.getSelectedIndex()].getValue()); //Работает за счет совпадения индексов с библиотечными
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.setNumStopBits(StopBitsList.getLikeArray(CB_StopBit.getSelectedIndex()));
                saveParameters(null);
            }
        });



        BT_Close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Close");
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.flushDataListener();
                poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort.closePort();
            }
        });
        CB_Pool.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.print("Pressed CB_Pool "); System.out.println(CB_Pool.isSelected());
                protocol = ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex());
                textToSendValue.set(tabbedPane1.getSelectedIndex(), textToSend.getText());
                String stringToSend = textToSendValue.get(tabbedPane1.getSelectedIndex());
                JTextPane paneForShow = logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1);
                logState.set(tabbedPane1.getSelectedIndex(), CB_Log.isSelected());
                SerialPort comPort = poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort;

                boolean pool = CB_Pool.isSelected();
                int poolDelay = 1000;
                try{
                    poolDelay = Integer.parseInt(IN_PoolDelay.getText());
                }catch (Exception e1){
                    IN_PoolDelay.setText("1000");
                }
                boolean logStateCb = logState.get(tabbedPane1.getSelectedIndex());
                String thName = "Pool_"+tabbedPane1.getTitleAt(tabbedPane1.getSelectedIndex());
                if(findPoolServiceByOpenedPort() != null){
                    System.out.println("Порт уже используется");
                    PoolService poolService = findPoolServiceByOpenedPort();
                    if(poolService.containTabDev(tabbedPane1.getSelectedIndex())){
                        System.out.println("Для текущей вкладки устройство существует в сервисе опроса");
                        return;
                    }else{
                        System.out.println("Для текущей вкладки устройство не существует в сервисе опроса");
                        poolService.addDeviceToService(
                                tabbedPane1.getSelectedIndex(),
                                stringToSend,
                                logDataTransferJtextPanel.get(tabbedPane1.getSelectedIndex()),
                                logStateCb,
                                "Pool_"+tabbedPane1.getTitleAt(tabbedPane1.getSelectedIndex())
                        );
                        return;
                    }

                }
                System.out.println("Начинаю работу с потоком " + thName);

                if(pool && (! MyUtilities.containThreadByName(threads, thName))){ //Надо запустить опрос но потока еще нету
                    System.out.println("Надо запустить опрос но потока еще нету");
                    poolServices.add(new PoolService(
                            ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()),
                            textToSendValue.get(tabbedPane1.getSelectedIndex()),
                            logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1),
                            poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort,
                            poolDelay,
                            logState.get(tabbedPane1.getSelectedIndex()),
                            tabbedPane1.getSelectedIndex()));

                    Thread myThread = new Thread(poolServices.get(poolServices.size() - 1));
                    myThread.setName("Pool_"+tabbedPane1.getTitleAt(tabbedPane1.getSelectedIndex()));
                    threads.add(myThread);
                    myThread.start();
                    System.out.println("Поток создан и запущен");
                    System.out.println(myThread.getState());
                }else if(pool && MyUtilities.containThreadByName(threads, thName)){//Надо запустить опрос и поток уже существует
                    System.out.println("Надо запустить опрос и поток уже существует");
                    Thread myThread = MyUtilities.getThreadByName(threads, thName);

                    if( myThread != null){
                        System.out.println(myThread.getState());
                        if(! myThread.isAlive()){
                            threads.remove(MyUtilities.getThreadByName(threads, thName));
                            poolServices.add(new PoolService(
                                    ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()),
                                    textToSendValue.get(tabbedPane1.getSelectedIndex()),
                                    logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1),
                                    poolComConnections.get(tabbedPane1.getSelectedIndex()).activePort,
                                    poolDelay,
                                    logState.get(tabbedPane1.getSelectedIndex()),
                                    tabbedPane1.getSelectedIndex()));
                            myThread = new Thread(poolServices.get(poolServices.size() - 1));

                            myThread.setName("Pool_"+tabbedPane1.getTitleAt(tabbedPane1.getSelectedIndex()));
                            threads.add(myThread);
                            myThread.start();
                            System.out.println("Поток запущен повторно");
                        }
                    }else{
                        System.out.println("Поток для повторного запуска не найден");
                    }
                } else if ((! pool) && MyUtilities.containThreadByName(threads, thName)) {//Надо остановить опрос и поток уже существует
                    System.out.println("Надо остановить опрос и поток уже существует");
                    Thread myThread = MyUtilities.getThreadByName(threads, thName);
                    if( myThread != null){
                        System.out.println(myThread.getState()); //RUNNABLE
                        if(myThread.isAlive()){
                            while (myThread.isAlive()){
                                myThread.interrupt();
                            }
                            System.out.println("Поток приостановлен");
                        }
                    }else{
                        System.out.println("Поток для остановки не найден");
                    }

                }else {
                    System.out.println("Потока не было, останавливать нечего");
                }


                //sendCommand(textToSendString);

            }
        });


        BT_Send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Send");
                //protocol = ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex());
                //System.out.println("Protocol: " + protocol);
                //textToSendValue.set(tabbedPane1.getSelectedIndex(), textToSend.getText());
                //logState.set(tabbedPane1.getSelectedIndex(), CB_Log.isSelected());
                //sendCommand(textToSendString);
                for (int i = 0; i < 10000; i++) {


                            //logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setText(poolService.getAnswersForTab(tabbedPane1.getSelectedIndex()).toString());
                            String str = "234234";
                            logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setText(null);
                            Document doc = logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).getDocument();

                            //logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setText(str);
                            textField1.setText(str);
                            str = null;
                            doc = null;


                }

            }
        });

        BT_AddDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_AddDev");
                textToSendValue.add("");
                logState.add(true);
                JPanel panel = new JPanel();
                panel.setLayout( new BorderLayout());
                logDataTransferJscrollPanel.add(new JScrollPane());
                logDataTransferJtextPanel.add(new JTextPane());
                logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setEditable(false);
                logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setDoubleBuffered(true);
                logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1).setText("dev" + (tabbedPane1.getTabCount() + 1) + "1 \n 1 \n 1 \n 1 1 \n 1 \n 1 \n 1 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n 1 \n ");
                logDataTransferJscrollPanel.get(logDataTransferJscrollPanel.size() -1).setViewportView(logDataTransferJtextPanel.get(logDataTransferJtextPanel.size() - 1));
                logDataTransferJscrollPanel.get(logDataTransferJscrollPanel.size() - 1).setPreferredSize(new Dimension(400,400));

                poolComConnections.add(new ComPort());
                panel.add(logDataTransferJscrollPanel.get(logDataTransferJscrollPanel.size() - 1), BorderLayout.CENTER);
                panel.setDoubleBuffered(true);
                panel.setEnabled(false);
                tabbedPane1.addTab("dev" + (tabbedPane1.getTabCount() + 1), panel);
                checkIsUsedPort();
                if(tabbedPane1.getTabCount() > 0){
                    BT_RemoveDev.setEnabled(true);
                }
            }

        });

        BT_RemoveDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int curTab = tabbedPane1.getSelectedIndex();
                System.out.println("Pressed BT_RemoveDev on Tab" + curTab);
                textToSendValue.remove(curTab);
                logState.remove(curTab);
                System.out.println("Надо остановить опрос и поток уже существует");
                String thName = "Pool_"+tabbedPane1.getTitleAt(curTab);
                Thread myThread = MyUtilities.getThreadByName(threads, thName);
                if( myThread != null){
                    System.out.println(myThread.getState()); //RUNNABLE
                    if(myThread.isAlive()){
                        while (myThread.isAlive()){
                            myThread.interrupt();
                        }
                        System.out.println("Поток приостановлен");
                        logDataTransferJtextPanel.get(curTab);
                    }
                }else{
                    System.out.println("Поток для остановки не найден");
                }
                if(poolComConnections.size() >= curTab){
                    if (poolComConnections.get(curTab).activePort != null){
                        poolComConnections.get(curTab).activePort.closePort();
                    }

                    poolComConnections.remove(curTab);
                }


                tabbedPane1.removeTabAt(curTab);
                if(tabbedPane1.getTabCount() == 0){
                    BT_RemoveDev.setEnabled(false);
                }
            }
        });
        textToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(textToSendValue.size() > tabbedPane1.getSelectedIndex()){
                    textToSendValue.set(tabbedPane1.getSelectedIndex(), textToSend.getText());
                    if(poolServices.size() > tabbedPane1.getSelectedIndex()) {
                        poolServices.get(tabbedPane1.getSelectedIndex()).setTextToSendString(textToSend.getText(), tabbedPane1.getSelectedIndex());
                    }
                }else{
                    System.out.println("Ошибка при обновлении пула команд для опроса");
                }
            }
        });

        CB_Log.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(logState.size() > tabbedPane1.getSelectedIndex()){
                    logState.set(tabbedPane1.getSelectedIndex(), CB_Log.isSelected());
                    if(poolServices.size() > tabbedPane1.getSelectedIndex()) {
                        poolServices.get(tabbedPane1.getSelectedIndex()).setNeedLog(CB_Log.isSelected(), tabbedPane1.getSelectedIndex());
                    }
                }else{
                    System.out.println("Ошибка при обновлении пула команд для ведения лога");
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
                if(ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()) != null){
                    if(ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()) == ProtocolsList.ERSTEVAK_MTP4D){
                        BT_Search.setEnabled(true);
                    }else{
                        BT_Search.setEnabled(false);
                    }
                }
            }
        });

        tabbedPane1.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                System.out.println("Tab: " + tabbedPane1.getSelectedIndex());
                textToSend.setText(textToSendValue.get(tabbedPane1.getSelectedIndex()));
                String thName = "Pool_"+tabbedPane1.getTitleAt(tabbedPane1.getSelectedIndex());
                boolean threadExist = MyUtilities.containThreadByName(threads, thName);
                boolean comInUse = checkIsUsedPort();

                if(threadExist){
                    CB_Pool.setSelected(true);
                    //System.out.println(poolServices.get(tabbedPane1.getSelectedIndex()).getProtocolForJCombo());
                    CB_Protocol.setSelectedIndex(poolServices.get(tabbedPane1.getSelectedIndex()).getProtocolForJCombo());
                    CB_ComPorts.setSelectedIndex(poolServices.get(tabbedPane1.getSelectedIndex()).getComPortForJCombo());
                    CB_Log.setSelected(poolServices.get(tabbedPane1.getSelectedIndex()).isNeedLog(tabbedPane1.getSelectedIndex()));
                    //CB_Protocol.setSelectedIndex(ProtocolsList.getByName);
                } else if (comInUse) {
                    PoolService ps = findPoolServiceByOpenedPort();
                    assert ps != null;
                    CB_Log.setSelected(ps.isLoggedTab(tabbedPane1.getSelectedIndex()));
                }
            }
        });



        textToSendValue.add(textToSend.getText());
        textToSendValue.add(textToSend.getText());
        logState.add(true);
        logState.add(true);
        BT_AddDev.doClick();


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
                //System.out.println("Update command");
                if(tabbedPane1.getSelectedIndex() < textToSendValue.size())
                    textToSendValue.set(tabbedPane1.getSelectedIndex(), textToSend.getText());
                if(tabbedPane1.getSelectedIndex() < poolServices.size())
                    poolServices.get(tabbedPane1.getSelectedIndex()).setTextToSendString(textToSend.getText(), tabbedPane1.getSelectedIndex());
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
                System.out.println("Update PoolDelay");
                if(poolServices.get(tabbedPane1.getSelectedIndex()) != null)
                    poolServices.get(tabbedPane1.getSelectedIndex()).setPoolDelay(IN_PoolDelay.getText());
            }
        });
    }

    private boolean checkIsUsedPort(){
        //poolComConnections.get(tabbedPane1.getSelectedIndex()).setPort(CB_ComPorts.getSelectedIndex());
        for (PoolService poolService : poolServices) {
            if(CB_ComPorts.getSelectedIndex() == poolService.getComPortForJCombo()){
                BT_Open.setEnabled(false);
                CB_Protocol.setEnabled(false);
                CB_Protocol.setSelectedIndex(poolService.getComPortForJCombo());
                return true;
            }
        }
        BT_Open.setEnabled(true);
        CB_Protocol.setEnabled(true);
        return false;
    }

    private PoolService findPoolServiceByOpenedPort(){
        for (PoolService poolService : poolServices) {
            if(CB_ComPorts.getSelectedIndex() == poolService.getComPortForJCombo()){
                return poolService;
            }
        }
        return null;
    }
    private void onOK() {
        System.out.println("Pressed BT_Ok");
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }

//--------------------------------------------------------
    /**
     * Функция создания меню "Файл"
     */
    private JMenu createFileMenu()
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
    //--------------------------------------------------------
    // создадим забавное меню
    /**
     * Функция создания меню
     */
    private JMenu createViewMenu()
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
                System.out.println("ServerWindows");
                ServerSettingsWindow srvWindows = new ServerSettingsWindow(prop);
                srvWindows.setName("Server settings");
                srvWindows.setTitle("Server settings");
                srvWindows.pack();
                srvWindows.setVisible(true);
            }
        });
        return viewMenu;
    }

    //--------------------------------------------------------
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

    /* --- Метод обновления настроек ---
        Если вызван с параметром NULL, то
        обновляет все.
        Если массив строк, то обновляет
        перечисленное в массиве (по названию
        параметров)
     */
    private void saveParameters(String [] parametersArray){
        if(parametersArray == null){
            prop.setLastComPort(poolComConnections.get(tabbedPane1.getSelectedIndex()).getCurrentComName());
            prop.setLastComSpeed(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
            prop.setLastDataBits(DataBitsList.getLikeArray(CB_DataBits.getSelectedIndex()));
            prop.setLastParity(ParityList.values()[CB_Parity.getSelectedIndex()].getName());
            prop.setLastStopBits(StopBitsList.getLikeArray(CB_StopBit.getSelectedIndex()));
            prop.setLastProtocol(ProtocolsList.getLikeArray(CB_Protocol.getSelectedIndex()));
        }else{
            for (String s : parametersArray) {
                switch (s){
                    case "LastComPort":
                        prop.setLastComPort(poolComConnections.get(tabbedPane1.getSelectedIndex()).getCurrentComName());
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
                        System.out.println("Unknown parameter");

                }
            }
        }
    }
}
