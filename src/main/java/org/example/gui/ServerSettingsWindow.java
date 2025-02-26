package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.Main;
//import org.example.utilites.SpringLoader;
import org.example.utilites.properties.MyProperties;
import org.springframework.boot.autoconfigure.mail.MailProperties;
import org.springframework.context.ApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.springframework.stereotype.Component;


@Component
public class ServerSettingsWindow extends JDialog {
    private MyProperties myProperties = MyProperties.getInstance();
    private String driver;
    private JPanel serverParametersPanel;
    private JTextField IN_DbDriver;
    private JPasswordField IN_Pwd;
    private JButton saveButton;
    private JButton BT_StartServer;
    private JButton BT_StopServer;
    private JTextField IN_ServerPort;
    private JTextArea textArea1;
    private JTextField textField1;
    private JTextField textField2;
    private JButton addUserButton;
    private JTextField textField3;
    private JTextField IN_Login;
    private JTextField IN_Url;
    private JCheckBox offlineMode;


    private String springPort;


    private String dbUrl;
    private String dbUsername;
    private String dbPassword;
    private String dbDriver;
    private boolean offlineModeFlag = false;


    public ServerSettingsWindow() {

        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(serverParametersPanel);
        //System.out.println("USR GET " + Main.prop.getUsr());
        //IN_Login.setText(Main.prop.getUsr());
        //IN_Pwd.setText(Main.prop.getPwd());
        if (myProperties != null) {
            IN_ServerPort.setText(myProperties.getPrt());
        }

        // IN_Url.setText(Main.prop.getUrl());
        //IN_DbDriver.setText(Main.prop.getDrv());

        saveButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Pressed BT_Save (and check)");
                        springPort = IN_ServerPort.getText();
                        driver = IN_DbDriver.getText();
                        dbUsername = IN_Login.getText();
                        StringBuilder pwdInput = new StringBuilder();
                        for (char c : IN_Pwd.getPassword()) {
                            pwdInput.append(c);
                        }
                        dbPassword = pwdInput.toString();
                        dbUrl = IN_Url.getText();

                        checkParameters();
                    }
                }
        );
        BT_StartServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_StartServer");
                if (offlineModeFlag) {
                    Main.restart("srv-offline");
                } else {
                    Main.restart("srv-online");
                }

            }
        });


        BT_StopServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_StopServer");
                Main.restart("gui-only");
            }
        });

        offlineMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                offlineModeFlag = !offlineModeFlag;
                offlineMode.setSelected(offlineModeFlag);
            }
        });
    }


    private void checkParameters() {
        if (springPort == null) {
            springPort = "8080";
            IN_ServerPort.setText("8080");
        } else if (!this.stringIsNumeric(springPort)) {
            springPort = "8080";
            IN_ServerPort.setText("8080");
        } else if (springPort.length() > 5 || springPort.length() < 2) {
            springPort = "8080";
            IN_ServerPort.setText("8080");
        }
        myProperties.setPrt(springPort);
        if (driver == null) {
            driver = "org.postgresql.DriverNull";
        } else if (driver.length() < 2 || driver.length() > 500) {
            driver = "org.postgresql.DriverLength";
        } else {
            driver = driver.trim();
            //Main.prop.setDrv(driver);
        }

        if (dbUrl == null) {
            dbUrl = "jdbc:postgresql://somesite.com:5432/someDataBaseNull";
        } else if (dbUrl.length() < 2 || dbUrl.length() > 500) {
            dbUrl = "jdbc:postgresql://somesite.com:5432/someDataBaseLength";
        } else {
            dbUrl = dbUrl.trim();
            //Main.prop.setUrl(dbUrl);
        }

        if (dbPassword == null) {
            dbPassword = "some_passwordNull";
        } else if (dbPassword.length() < 2 || dbPassword.length() > 300) {
            dbPassword = "some_passwordLength";
        } else {
            dbPassword = dbPassword.trim();
            //Main.prop.setPwd(dbPassword);
        }

        if (dbUsername == null) {
            dbUsername = "some_usernameNull";
        } else if (dbUsername.length() < 2 || dbUsername.length() > 55) {
            dbUsername = "some_usernameLength";
        } else {
            dbUsername = dbUsername.trim();
            //Main.prop.setUsr(dbUsername);
        }
        //Main.prop.updateServerConf();
        //Main.prop.updateFile();

        System.out.println(this.driver);
        System.out.println(this.dbUsername);
        System.out.println(this.dbPassword);
        System.out.println(this.dbUrl);
        System.out.println(this.springPort);
    }

    private boolean stringIsNumeric(String str) {
        if (str == null) {
            return false;
        }
        try {
            int d = Integer.parseInt(str);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
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
        serverParametersPanel = new JPanel();
        serverParametersPanel.setLayout(new GridLayoutManager(12, 4, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.setMaximumSize(new Dimension(600, 600));
        serverParametersPanel.setMinimumSize(new Dimension(450, 450));
        serverParametersPanel.setPreferredSize(new Dimension(500, 500));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.add(panel1, new GridConstraints(0, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("platform");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_DbDriver = new JTextField();
        IN_DbDriver.setText("org.postgresql.Driver");
        panel1.add(IN_DbDriver, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer1 = new Spacer();
        serverParametersPanel.add(spacer1, new GridConstraints(11, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("url");
        serverParametersPanel.add(label2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_Url = new JTextField();
        IN_Url.setText("jdbc:postgresql://");
        serverParametersPanel.add(IN_Url, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("username");
        serverParametersPanel.add(label3, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_Login = new JTextField();
        IN_Login.setText("");
        serverParametersPanel.add(IN_Login, new GridConstraints(2, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("password");
        serverParametersPanel.add(label4, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_Pwd = new JPasswordField();
        IN_Pwd.setText("");
        serverParametersPanel.add(IN_Pwd, new GridConstraints(3, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        saveButton = new JButton();
        saveButton.setText("Save");
        serverParametersPanel.add(saveButton, new GridConstraints(6, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.add(panel2, new GridConstraints(7, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_StartServer = new JButton();
        BT_StartServer.setText("start");
        panel2.add(BT_StartServer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_StopServer = new JButton();
        BT_StopServer.setText("stop");
        panel2.add(BT_StopServer, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("port");
        serverParametersPanel.add(label5, new GridConstraints(5, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_ServerPort = new JTextField();
        IN_ServerPort.setText("8085");
        serverParametersPanel.add(IN_ServerPort, new GridConstraints(5, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        serverParametersPanel.add(scrollPane1, new GridConstraints(11, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea1 = new JTextArea();
        textArea1.setEditable(false);
        textArea1.setText("");
        scrollPane1.setViewportView(textArea1);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.add(panel3, new GridConstraints(8, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("userName");
        panel3.add(label6, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField1 = new JTextField();
        panel3.add(textField1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Password");
        panel3.add(label7, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField2 = new JTextField();
        panel3.add(textField2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        addUserButton = new JButton();
        addUserButton.setText("addUser");
        serverParametersPanel.add(addUserButton, new GridConstraints(10, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("UserRole");
        serverParametersPanel.add(label8, new GridConstraints(9, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField3 = new JTextField();
        serverParametersPanel.add(textField3, new GridConstraints(9, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.add(panel4, new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        offlineMode = new JCheckBox();
        offlineMode.setText("offlineMode");
        panel4.add(offlineMode, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        panel4.add(spacer2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return serverParametersPanel;
    }

}