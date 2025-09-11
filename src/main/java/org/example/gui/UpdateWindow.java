package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.gui.components.CustomScrollBarUI;
import org.example.utilites.*;
import org.example.utilites.properties.MyProperties;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ResourceBundle;


public class UpdateWindow extends JDialog implements Rendeble {
    private final JPanel panel1 = new JPanel();
    private JScrollPane SP_Field;
    private JPanel mainPanel;
    private JButton BT_Check;
    private JTextPane TP_CheckResult;
    private JTextField TF_WhatNews;
    private JButton BT_Download;
    private JProgressBar PB_Download;
    private JTextField TF_DownloadResult;
    private JTextPane TP_WhatsNews;
    private JPanel update_description;
    private JTextPane a1ÐÐÑÐTextPane;
    private JScrollPane scrollPane1;
    private JScrollPane scrollPaneWhatsNews;
    private JTextPane TP_Field;
    private boolean isAvailableNewVersion = false;
    private int downloadProgress = 0;
    private final ProgramUpdater programUpdater = new ProgramUpdater();

    public UpdateWindow() {

        $$$setupUI$$$();
        String currentVersion = MyProperties.getInstance().getVersion();


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
                TF_DownloadResult.setText("Начинаю проверку!");
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
                TF_DownloadResult.setText("Проверка завершена!");
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

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(9, 1, new Insets(0, 0, 0, 0), -1, -1));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel2.setEnabled(true);
        mainPanel.add(panel2, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 60), new Dimension(-1, 90), null, 0, false));
        scrollPane1 = new JScrollPane();
        panel2.add(scrollPane1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        TP_CheckResult = new JTextPane();
        TP_CheckResult.setEditable(false);
        TP_CheckResult.setText("");
        scrollPane1.setViewportView(TP_CheckResult);
        BT_Check = new JButton();
        BT_Check.setText("Проверить");
        panel2.add(BT_Check, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_SOUTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(6, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        BT_Download = new JButton();
        BT_Download.setText("Скачать");
        panel3.add(BT_Download, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(150, -1), new Dimension(150, -1), new Dimension(150, -1), 0, false));
        PB_Download = new JProgressBar();
        panel3.add(PB_Download, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel4, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        panel4.add(scrollPaneWhatsNews, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 60), new Dimension(-1, 80), null, 0, false));
        TP_WhatsNews = new JTextPane();
        scrollPaneWhatsNews.setViewportView(TP_WhatsNews);
        final Spacer spacer2 = new Spacer();
        mainPanel.add(spacer2, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        mainPanel.add(spacer3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        TF_DownloadResult = new JTextField();
        TF_DownloadResult.setEditable(false);
        mainPanel.add(TF_DownloadResult, new GridConstraints(7, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, 30), new Dimension(150, -1), null, 0, false));
        final Spacer spacer4 = new Spacer();
        mainPanel.add(spacer4, new GridConstraints(8, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        update_description = new JPanel();
        update_description.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        update_description.setBackground(new Color(-1));
        mainPanel.add(update_description, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        a1ÐÐÑÐTextPane = new JTextPane();
        a1ÐÐÑÐTextPane.setEditable(false);
        a1ÐÐÑÐTextPane.setText(this.$$$getMessageFromBundle$$$("ru_RU_gui", "field.updateWindow.updateDescription"));
        update_description.add(a1ÐÐÑÐTextPane, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(50, 50), new Dimension(150, 50), null, 0, false));
    }

    private static Method $$$cachedGetBundleMethod$$$ = null;

    private String $$$getMessageFromBundle$$$(String path, String key) {
        ResourceBundle bundle;
        try {
            Class<?> thisClass = this.getClass();
            if ($$$cachedGetBundleMethod$$$ == null) {
                Class<?> dynamicBundleClass = thisClass.getClassLoader().loadClass("com.intellij.DynamicBundle");
                $$$cachedGetBundleMethod$$$ = dynamicBundleClass.getMethod("getBundle", String.class, Class.class);
            }
            bundle = (ResourceBundle) $$$cachedGetBundleMethod$$$.invoke(null, path, thisClass);
        } catch (Exception e) {
            bundle = ResourceBundle.getBundle(path);
        }
        return bundle.getString(key);
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

    private void createUIComponents() {
        scrollPaneWhatsNews = new JScrollPane();

        // Применяем кастомный UI к вертикальной полосе
        JScrollBar vertical = scrollPaneWhatsNews.getVerticalScrollBar();
        vertical.setUI(new CustomScrollBarUI());

        // Применяем кастомный UI к горизонтальной полосе
        JScrollBar horizontal = scrollPaneWhatsNews.getHorizontalScrollBar();
        horizontal.setUI(new CustomScrollBarUI());

        // Устанавливаем viewport (если нужно)
        scrollPaneWhatsNews.setViewportView(TP_CheckResult);

    }
}

