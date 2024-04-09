/*
Файл содержит код, выполняющийся при взаимодействии с виндовс-окном приложения
 */
package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import org.example.services.AnswerStorage;
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

    private final ExecutorService thPool = Executors.newCachedThreadPool();
    private final MyProperties prop = new MyProperties();
    private final ArrayList <String> textToSendValue = new ArrayList<>();
    private final ArrayList <JScrollPane> logDataTransferJscrollPanel = new ArrayList<>();
    private ArrayList <JTextPane> logDataTransferJtextPanel = new ArrayList<>();
    private ArrayList <PoolService> poolServices = new ArrayList<>();
    private ArrayList <ComPort> poolComConnections = new ArrayList<>();
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




    private ProtocolsList protocol = ProtocolsList.IGM10ASCII;

    /**
    Number active tab
     **/
    private int tab = 0;

    public MainWindow() {
        poolComConnections.add(new ComPort());
        JmenuFile jmenu = new JmenuFile();
        // Создание строки главного меню
        JMenuBar menuBar = new JMenuBar();

        // Добавление в главное меню выпадающих пунктов меню
        menuBar.add(jmenu.createFileMenu());
        menuBar.add(jmenu.createViewMenu());
        menuBar.add(jmenu.createSystemParametrs());

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
                poolComConnections.get(tab).updatePorts();
                CB_ComPorts.removeAllItems();
                for (SerialPort port :  poolComConnections.get(tab).getAllPorts()) {
                    CB_ComPorts.addItem( port.getSystemPortName() + " (" + MyUtilities.removeComWord(port.getPortDescription()) + ")");
                }
            }
        });

        BT_Open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Open");
                poolComConnections.get(tab).setPort(CB_ComPorts.getSelectedIndex());
                poolComConnections.get(tab).activePort.setComPortParameters(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()),8, 1, SerialPort.NO_PARITY, false);
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
                System.out.println("Pressed BT_Close");
                poolComConnections.get(tab).activePort.flushDataListener();
                poolComConnections.get(tab).activePort.closePort();
            }
        });
        CB_Pool.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.print("Pressed CB_Pool "); System.out.println(CB_Pool.isSelected());
                protocol = ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex());
                textToSendValue.set(tab, textToSend.getText());
                String stringToSend = textToSendValue.get(tab);
                boolean pool = CB_Pool.isSelected();
                int poolDelay = 1000;
                try{
                    poolDelay = Integer.parseInt(IN_PoolDelay.getText());
                }catch (Exception e1){
                    IN_PoolDelay.setText("1000");
                }
                PoolService psT = findPoolServiceByTabNumber();
                PoolService psC = findPoolServiceByOpenedPort();
                PoolService ps = null;
                if(psT != null)
                    ps = psT;
                else
                    ps = psC;

                if(ps != null){
                    System.out.println("Порт уже используется, проверка  среди запущенных потоков");
                    if(ps.containTabDev(tab)){
                        System.out.println("Для текущей вкладки устройство существует в потоке опроса");
                        if(pool){
                            System.out.println("Команда к запуску");
                            ps.setNeedPool(tab, true);
                        }else{
                            System.out.println("Команда к остановке опроса");
                            if(ps.isRootTab(tab)){
                                System.out.println("Текущий поток является корневым для других");
                                ps.setNeedPool(tab, false);
                            }else{
                                System.out.println("Вкладка одинока. Поток будет завершен");
                                ps.setNeedPool(tab, false);
                                poolServices.remove(tab);
                            }
                        }
                        ps.setNeedPool(tab, pool);
                    }else{
                        System.out.println("Для текущей вкладки устройство не существует в потоке опроса");
                        ps.addDeviceToService(tab, stringToSend, false);
                    }
                }else{
                    System.out.println("Порт не используется, создание нового потока");
                    poolServices.add(new PoolService(
                            ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex()),
                            textToSendValue.get(tab),
                            poolComConnections.get(tab).activePort,
                            poolDelay,
                            false,
                            tab));
                    thPool.submit(poolServices.get(poolServices.size() - 1));
                    System.out.println("Поток создан и запущен");
                }
            }
        });

        CB_Log.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PoolService ps = findPoolServiceByTabNumber();
                if(ps != null){
                    ps.setNeedLog(CB_Log.isSelected(), tab);
                }else{
                    System.out.println("Для текущей влкадки потока опроса не существует");
                }
            }
        });

        BT_Send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Send");
                Document doc = logDataTransferJtextPanel.get(tab).getDocument();//Пробовал через док
                try {
                    doc.remove(0, doc.getLength());
                    doc.insertString(doc.getLength(), AnswerStorage.getAnswersForTab(tab, true), null);
                } catch (BadLocationException ex) {
                    //throw new RuntimeException(ex);
                }
                doc = null;
                System.gc(); //Runtime.getRuntime().gc();
            }
        });

        BT_AddDev.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_AddDev");
                textToSendValue.add("");
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
                tab = tabbedPane1.getSelectedIndex();
                System.out.println("Pressed BT_RemoveDev on Tab" + tab);
                textToSendValue.remove(tab);
                PoolService ps = findPoolServiceByOpenedPort();
                if(ps != null){
                    if(ps.containTabDev(tab)){
                        ps.setNeedPool(tab, false);
                        System.out.println("Задача была удалена");
                    }else{
                        System.out.println("Выбранная вкладка не найдена в потоке");
                    }
                }
                tabbedPane1.removeTabAt(tab);
                if(tabbedPane1.getTabCount() == 0){
                    BT_RemoveDev.setEnabled(false);
                }
            }
        });
        textToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(textToSendValue.size() > tab){
                    textToSendValue.set(tab, textToSend.getText());
                    if(poolServices.size() > tab) {
                        poolServices.get(tab).setTextToSendString(textToSend.getText(), tab);
                    }
                }else{
                    System.out.println("Ошибка при обновлении пула команд для опроса");
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
                tab = tabbedPane1.getSelectedIndex();
                System.out.println("Tab: " + tab);
                textToSend.setText(textToSendValue.get(tab));
                CB_Pool.setSelected(isPooled());
                CB_Log.setSelected(isLogged());
                textToSend.setText(getPoolText());
            }
        });

        textToSendValue.add(textToSend.getText());
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
                if(tab < textToSendValue.size()) {
                    PoolService ps = findPoolServiceByTabNumber();
                    if(ps != null)
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
                System.out.println("Update PoolDelay");
                if(poolServices.get(tab) != null)
                    poolServices.get(tab).setPoolDelay(IN_PoolDelay.getText());
            }
        });

        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                thPool.shutdownNow();
                e.getWindow().dispose();
            }
        });
    }

    private void checkIsUsedPort(){
        //poolComConnections.get(tab).setPort(CB_ComPorts.getSelectedIndex());
        for (PoolService poolService : poolServices) {
            if(CB_ComPorts.getSelectedIndex() == poolService.getComPortForJCombo()){
                BT_Open.setEnabled(false);
                CB_Protocol.setEnabled(false);
                CB_Protocol.setSelectedIndex(poolService.getProtocolForJCombo());
                return;
            }
        }
        BT_Open.setEnabled(true);
        CB_Protocol.setEnabled(true);
    }

    private PoolService findPoolServiceByOpenedPort(){
        for (PoolService poolService : poolServices) {
            if(CB_ComPorts.getSelectedIndex() == poolService.getComPortForJCombo()){
                return poolService;
            }
        }
        return null;
    }

    private PoolService findPoolServiceByTabNumber(){
        for (PoolService poolService : poolServices) {
            if(poolService.containTabDev(tab)){
                return poolService;
            }
        }
        return null;
    }

    private boolean isPooled (){
        PoolService ps = findPoolServiceByTabNumber();
        if(ps != null){
            return ps.isNeedPool(tab);
        }
        return false;
    }

    private boolean isLogged (){
        PoolService ps = findPoolServiceByTabNumber();
        if(ps != null){
            return ps.isNeedLog(tab);
        }
        return false;
    }

    private String getPoolText(){
        PoolService ps = findPoolServiceByTabNumber();
        if(ps != null){
            return ps.getTextToSensByTab(tab);
        }
        return "";
    }
    private boolean havePoolService(){
        for (PoolService poolService : poolServices) {
            if(poolService.containTabDev(tab)){
                return true;
            }
        }
        return false;
    }
    private void onOK() {
        System.out.println("Pressed BT_Ok");
        dispose();
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
            prop.setLastComPort(poolComConnections.get(tab).getCurrentComName());
            prop.setLastComSpeed(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
            prop.setLastDataBits(DataBitsList.getLikeArray(CB_DataBits.getSelectedIndex()));
            prop.setLastParity(ParityList.values()[CB_Parity.getSelectedIndex()].getName());
            prop.setLastStopBits(StopBitsList.getLikeArray(CB_StopBit.getSelectedIndex()));
            prop.setLastProtocol(ProtocolsList.getLikeArray(CB_Protocol.getSelectedIndex()));
        }else{
            for (String s : parametersArray) {
                switch (s){
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
                        System.out.println("Unknown parameter");

                }
            }
        }

    }

}
