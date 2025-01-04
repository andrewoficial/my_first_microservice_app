package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.Main;
import org.example.utilites.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import static org.example.utilites.MyUtilities.createDeviceByProtocol;

public class UpdateWindow extends JDialog implements Rendeble {

    private JScrollPane SP_Field;
    private JPanel mainPanel;
    private JButton BT_Check;
    private JTextPane TP_CheckResult;
    private JTextField TF_WhatNews;
    private JButton BT_Download;
    private JProgressBar PB_Download;
    private JTextField TF_DownloadResult;
    private JTextPane TP_WhatsNews;
    private JTextPane TP_Field;
    private boolean isAvailableNewVersion = false;
    private int downloadProgress = 0;
    private final ProgramUpdater programUpdater = new ProgramUpdater();

    public UpdateWindow() {

        //String currentVersion = Main.currentVersion;
        String currentVersion = "LOL";


        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
        BT_Download.setEnabled(false);
        PB_Download.setMinimum(0);
        PB_Download.setMaximum(100);

        BT_Check.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TP_CheckResult.setText("Приступаю к проверке на существование новой версии");
                boolean errors = false;
                try {
                    isAvailableNewVersion = programUpdater.isAvailableNewVersion(programUpdater.getLatestVersion(), currentVersion);
                } catch (Exception ex) {
                    TP_CheckResult.setText("Произошла ошибка во время проверки обновлений " + ex.getMessage());
                    errors = true;
                }

                if (isAvailableNewVersion) {
                    TP_CheckResult.setText("Новая версия обнаружена! \n Найденная версия:" + programUpdater.getLatestVersion() + "\n Текущая версия: " + currentVersion);
                } else if (!errors) {
                    TP_CheckResult.setText("Обновлений не найдено! \n Найденная версия:" + programUpdater.getLatestVersion() + "\n Текущая версия: " + currentVersion);
                }

                if (isAvailableNewVersion) {
                    BT_Download.setEnabled(true);
                    try {
                        String whatsNewsStr = programUpdater.getInfo();
                        whatsNewsStr = whatsNewsStr.replaceAll("\r", "");
                        TP_WhatsNews.setText(whatsNewsStr);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }

                }

            }
        });

        BT_Download.addActionListener(new ActionListener() {


            @Override
            public void actionPerformed(ActionEvent e) {

                BT_Download.setEnabled(false);
                downloadProgress = 0;
                TF_DownloadResult.setText("Начинаю загрузку!");
                // Создаем фоновый поток для загрузки обновлений
                SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                    boolean error = false;

                    @Override
                    protected Void doInBackground() {
                        try {
                            downloadProgress = 0;
                            PB_Download.setValue(downloadProgress);


                            programUpdater.downloadUpdate();

                        } catch (IOException | InterruptedException exception) {
                            // Обработка ошибок
                            System.out.println("Ошибка в ходе загрузки: " + exception.getMessage());
                            TF_DownloadResult.setText(exception.getMessage());
                            error = true;
                        } finally {
                            BT_Download.setEnabled(true);
                        }
                        return null;
                    }


                    @Override
                    protected void done() {
                        // Выполняем действия по завершении
                        if (!error) {
                            TF_DownloadResult.setText("Загрузка завершена!");
                        }
                    }
                };

                // Запускаем фоновый поток
                worker.execute();
            }
        });

    }

    private void UpdateProgressBar() {

        if (programUpdater.isBusy()) {

            downloadProgress = programUpdater.getUpdatePercents();
            System.out.println("Progress" + downloadProgress);
            PB_Download.setValue(downloadProgress);
        } else {
            PB_Download.setValue(0);
            System.out.println("Wait");
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
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(9, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(400, 60), null, new Dimension(-1, 100), 0, false));
        BT_Check = new JButton();
        BT_Check.setText("Check");
        panel1.add(BT_Check, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        TP_CheckResult = new JTextPane();
        TP_CheckResult.setEditable(false);
        TP_CheckResult.setText("");
        panel1.add(TP_CheckResult, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_Download = new JButton();
        BT_Download.setText("GET");
        panel2.add(BT_Download, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        PB_Download = new JProgressBar();
        panel2.add(PB_Download, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        mainPanel.add(spacer2, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        TP_WhatsNews = new JTextPane();
        panel3.add(TP_WhatsNews, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, null, new Dimension(150, 50), null, 0, false));
        final Spacer spacer3 = new Spacer();
        mainPanel.add(spacer3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer4 = new Spacer();
        mainPanel.add(spacer4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        TF_DownloadResult = new JTextField();
        TF_DownloadResult.setEditable(false);
        mainPanel.add(TF_DownloadResult, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
        final Spacer spacer5 = new Spacer();
        mainPanel.add(spacer5, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }

    @Override
    public void renderData() {
        UpdateProgressBar();
    }

    @Override
    public boolean isEnable() {
        return this.isShowing();
    }
}

