package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.utilites.MyProperties;
import org.example.utilites.SpringLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


public class ServerSettingsWindow extends JDialog {
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

    private final ConfigurableApplicationContext ctx;
    private final SpringApplication app;

    private String springPort;


    private String dbUrl;
    private String username;
    private String password;


    public ServerSettingsWindow() {
        this.app = SpringLoader.app;
        this.ctx = SpringLoader.ctx;
        setModal(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(serverParametersPanel);

        saveButton.addActionListener(
                new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Pressed BT_StartServer");
                        springPort = IN_ServerPort.getText();
                        driver = IN_DbDriver.getText();
                        username = IN_Login.getText();
                        StringBuilder pwdInput = new StringBuilder();
                        for (char c : IN_Pwd.getPassword()) {
                            pwdInput.append(c);
                        }
                        password = pwdInput.toString();
                        dbUrl = IN_Url.getText();

                        checkParameters();
                    }
                }
        );
        BT_StartServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_StartServer");
                springPort = IN_ServerPort.getText();
                driver = IN_DbDriver.getText();
                username = IN_Login.getText();
                StringBuilder pwdInput = new StringBuilder();
                for (char c : IN_Pwd.getPassword()) {
                    pwdInput.append(c);
                }
                password = pwdInput.toString();
                dbUrl = IN_Url.getText();

                checkParameters();

                Map<String, Object> parameters = new HashMap<>();
                parameters.put("server.port", springPort);
                app.setDefaultProperties(parameters);
                SpringLoader.ctx = app.run();

            }
        });

        BT_StopServer.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Pressed BT_StopServer");
                SpringLoader.ctx.close();
            }
        });
    }

    private void checkParameters() {
        if (springPort == null) {
            springPort = "8080";
        } else if (!this.stringIsNumeric(springPort)) {
            springPort = "8080";
        } else if (springPort.length() > 5 || springPort.length() < 2) {
            springPort = "8080";
        }

        if (driver == null) {
            driver = "org.postgresql.DriverNull";
        } else if (driver.length() < 2 || driver.length() > 500) {
            driver = "org.postgresql.DriverLength";
        } else {
            driver = driver.trim();
        }

        if (dbUrl == null) {
            //dbUrl = "jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk";
            dbUrl = "jdbc:postgresql://somesite.com:5432/someDataBaseNull";
        } else if (dbUrl.length() < 2 || dbUrl.length() > 500) {
            dbUrl = "jdbc:postgresql://somesite.com:5432/someDataBaseLength";
        } else {
            dbUrl = dbUrl.trim();
        }

        if (password == null) {
            password = "some_passwordNull";
        } else if (password.length() < 2 || password.length() > 300) {
            password = "some_passwordLength";
        } else {
            password = password.trim();
        }

        if (username == null) {
            username = "some_usernameNull";
        } else if (username.length() < 2 || username.length() > 55) {
            username = "some_usernameLength";
        } else {
            username = username.trim();
        }


        MyProperties.driver = this.driver;
        MyProperties.usr = this.username;
        MyProperties.pwd = this.password;
        MyProperties.url = this.dbUrl;
        MyProperties.prt = this.springPort;

        System.out.println(this.driver);
        System.out.println(this.username);
        System.out.println(this.password);
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
        serverParametersPanel.setLayout(new GridLayoutManager(11, 4, new Insets(0, 0, 0, 0), -1, -1));
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
        serverParametersPanel.add(spacer1, new GridConstraints(10, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("url");
        serverParametersPanel.add(label2, new GridConstraints(1, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_Url = new JTextField();
        IN_Url.setText("jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk");
        serverParametersPanel.add(IN_Url, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("username");
        serverParametersPanel.add(label3, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_Login = new JTextField();
        IN_Login.setText("zhsiszsk");
        serverParametersPanel.add(IN_Login, new GridConstraints(2, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("password");
        serverParametersPanel.add(label4, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_Pwd = new JPasswordField();
        IN_Pwd.setText("EcrvEk0pw2UaY6jdKY16R3RGiBrefui1");
        serverParametersPanel.add(IN_Pwd, new GridConstraints(3, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        saveButton = new JButton();
        saveButton.setText("Save");
        serverParametersPanel.add(saveButton, new GridConstraints(5, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.add(panel2, new GridConstraints(6, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_StartServer = new JButton();
        BT_StartServer.setText("start");
        panel2.add(BT_StartServer, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_StopServer = new JButton();
        BT_StopServer.setText("stop");
        panel2.add(BT_StopServer, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("port");
        serverParametersPanel.add(label5, new GridConstraints(4, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        IN_ServerPort = new JTextField();
        IN_ServerPort.setText("8080");
        serverParametersPanel.add(IN_ServerPort, new GridConstraints(4, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        serverParametersPanel.add(scrollPane1, new GridConstraints(10, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        textArea1 = new JTextArea();
        textArea1.setEditable(false);
        textArea1.setText("");
        scrollPane1.setViewportView(textArea1);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 4, new Insets(0, 0, 0, 0), -1, -1));
        serverParametersPanel.add(panel3, new GridConstraints(7, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
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
        serverParametersPanel.add(addUserButton, new GridConstraints(9, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label8 = new JLabel();
        label8.setText("UserRole");
        serverParametersPanel.add(label8, new GridConstraints(8, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        textField3 = new JTextField();
        serverParametersPanel.add(textField3, new GridConstraints(8, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return serverParametersPanel;
    }

}