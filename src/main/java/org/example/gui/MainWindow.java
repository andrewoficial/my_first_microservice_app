/*
Файл содержит код, выполняющийся при взаимодействии с виндовс-окном приложения
 */
package org.example.gui;

import com.fazecast.jSerialComm.SerialPort;
import org.example.Main;
import org.example.utilites.BaudRatesList;
import org.example.utilites.ProtocolsList;
import org.example.services.PoolService;
import org.example.utilites.MyUtilities;
import org.springframework.boot.SpringApplication;

import javax.swing.*;
import java.awt.event.*;
import java.util.ArrayList;

import static org.example.Main.comPorts;

public class MainWindow extends JDialog {

    private ArrayList <Thread> threads = new ArrayList<>();
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
    private JTextPane receivedText;
    private JComboBox CB_Protocol;
    private JCheckBox CB_Pool;

    private JCheckBox CB_Server;
    private JTextField IN_ServerPort;
    private JTabbedPane tabbedPane1;
    private JButton buttonOK;
    private JButton buttonCancel;
    private String textToSendString = "";


    private ProtocolsList protocol = ProtocolsList.IGM10ASCII;

    public MainWindow() {
        SpringApplication.run(Main.class);
        setContentPane(contentPane);
        setModal(true);
        //getRootPane().setDefaultButton(buttonOK);



        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                //onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        for(BaudRatesList baudRate : BaudRatesList.values()){
            CB_BaudRate.addItem( baudRate.getValue() + "");

            //CB_ComPorts.addItem( port.getSystemPortName());
        }
        CB_BaudRate.setSelectedIndex(6);


        for (SerialPort port : comPorts.getAllPorts()) {
            CB_ComPorts.addItem( port.getSystemPortName() + " (" + MyUtilities.removeComWord(port.getPortDescription()) + ")");
            //CB_ComPorts.addItem( port.getSystemPortName());
        }
        for (ProtocolsList protocol : ProtocolsList.values()) {
            CB_Protocol.addItem( protocol.getValue());
            //CB_ComPorts.addItem( port.getSystemPortName());
        }
        BT_Update.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Update");
                comPorts.updatePorts();
                CB_ComPorts.removeAllItems();
                for (SerialPort port : comPorts.getAllPorts()) {
                    CB_ComPorts.addItem( port.getSystemPortName() + " (" + MyUtilities.removeComWord(port.getPortDescription()) + ")");
                    //CB_ComPorts.addItem( port.getSystemPortName());
                }

            }
        });

        BT_Open.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Open");

                //System.out.println(CB_ComPorts.getSelectedIndex());
                comPorts.setPort(CB_ComPorts.getSelectedIndex());
                //System.out.println("Speed index:" + CB_BaudRate.getSelectedIndex());
                System.out.println("Speed value:" + BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
                comPorts.activePort.setBaudRate(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()));
                comPorts.activePort.setComPortParameters(BaudRatesList.getLikeArray(CB_BaudRate.getSelectedIndex()),8, 1, SerialPort.NO_PARITY, false);
            }
        });



        BT_Close.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_Close");
                comPorts.activePort.flushDataListener();
                comPorts.activePort.closePort();
            }
        });
        CB_Pool.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.print("Pressed CB_Pool "); System.out.println(CB_Pool.isSelected());
                String thName = "Pool_"+tabbedPane1.getTitleAt(tabbedPane1.getSelectedIndex());
                System.out.println("Начинаю работу с потоком " + thName);
                boolean pool = CB_Pool.isSelected();
                if(pool && (! MyUtilities.containThreadByName(threads, thName))){ //Надо запустить опрос но потока еще нету
                    System.out.println("Надо запустить опрос но потока еще нету");
                    Thread myThread = new Thread(new PoolService(protocol, textToSendString, receivedText, CB_Pool));
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
                            myThread = new Thread(new PoolService(protocol, textToSendString, receivedText, CB_Pool));
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
                            myThread.interrupt();
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
                protocol = ProtocolsList.getLikeArrayEnum(CB_Protocol.getSelectedIndex());
                System.out.println("Protocol: " + protocol);
                textToSendString = textToSend.getText();
                //sendCommand(textToSendString);




            }
        });
        textToSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });
    }


    private void onOK() {
        // add your code here
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


}
