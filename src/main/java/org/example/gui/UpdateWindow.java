package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.gui.components.CustomScrollBarUI;
import org.example.utilites.*;
import org.example.utilites.properties.MyProperties;
import org.example.utilites.update.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;


public class UpdateWindow extends JDialog implements Rendeble {
    private JPanel mainPanel;
    private JComboBox<String> CB_Source;
    private JButton BT_Check;
    private JTextPane TP_CheckResult;
    private JScrollPane scrollWhatsNews;
    private JTextPane TP_WhatsNews;
    private JButton BT_Download;
    private JProgressBar PB_Download;
    private JTextField TF_DownloadResult;
    private boolean isAvailableNewVersion = false;
    private int downloadProgress = 0;
    private JLabel LB_Source;
    private final ProgramUpdater programUpdater = new ProgramUpdater();

    private String pendingDownloadUrl = null;
    private String pendingDownloadFileName = null;

    public UpdateWindow() {

        $$$setupUI$$$();
        String currentVersion = MyProperties.getInstance().getVersion();

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);
        BT_Download.setEnabled(false);
        PB_Download.setMinimum(0);
        PB_Download.setMaximum(100);
        PB_Download.setStringPainted(true);

        CB_Source.addItem("Все источники");
        for (UpdateSource src : programUpdater.getActiveSources()) {
            CB_Source.addItem(src.name);
        }

        BT_Check.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedSource = (String) CB_Source.getSelectedItem();
                boolean checkAll = "Все источники".equals(selectedSource);

                TP_CheckResult.setText("Приступаю к проверке источников обновлений...");
                TF_DownloadResult.setText("Начинаю проверку!");
                pendingDownloadUrl = null;
                pendingDownloadFileName = null;

                StringBuilder report = new StringBuilder();
                report.append("Текущая версия: ").append(currentVersion).append("\n");
                if (checkAll) {
                    report.append("Проверяем все источники...\n\n");
                } else {
                    report.append("Проверяем источник: ").append(selectedSource).append("\n\n");
                }

                boolean anyNewer = false;
                String bestDlUrl = "";
                String bestFileName = "";
                StringBuilder fullChangelog = new StringBuilder();

                try {
                    java.util.List<SourceChangelog> changelogs =
                            programUpdater.getChangelogsSince(currentVersion);

                    for (SourceChangelog cl : changelogs) {
                        if (!checkAll && !cl.sourceName.equals(selectedSource)) {
                            continue;
                        }

                        report.append("=== Источник: ").append(cl.sourceName).append(" ===\n");

                        if (cl.hasError()) {
                            report.append("Ошибка: ").append(cl.error).append("\n\n");
                            continue;
                        }

                        if (!cl.hasNewer()) {
                            report.append("Нет новых версий.\n\n");
                            continue;
                        }

                        anyNewer = true;
                        report.append("Найдено новых версий: ").append(cl.newerReleases.size()).append("\n");

                        Release newest = cl.newerReleases.get(0);
                        if (bestDlUrl.isEmpty() && newest.hasDownload()) {
                            bestDlUrl = newest.downloadUrl;
                            try {
                                String[] parts = newest.downloadUrl.split("/");
                                bestFileName = parts[parts.length - 1];
                            } catch (Exception ex) {
                                bestFileName = "Elephant-Monitor-" + newest.version + ".jar";
                            }
                        }

                        for (Release rn : cl.newerReleases) {
                            String firstLine = rn.notes.trim().split("\n")[0];
                            if (firstLine.length() > 80) firstLine = firstLine.substring(0, 77) + "...";
                            report.append("  • ").append(rn.version).append(" — ").append(firstLine).append("\n");
                        }
                        report.append("\n");

                        fullChangelog.append("══════════════════════════════════════\n");
                        fullChangelog.append("Источник: ").append(cl.sourceName).append("\n");
                        fullChangelog.append(cl.getCombinedNotes()).append("\n\n");
                    }

                } catch (Exception ex) {
                    report.append("Критическая ошибка при проверке: ").append(ex.getMessage());
                }

                TP_CheckResult.setText(report.toString());

                isAvailableNewVersion = anyNewer;

                if (isAvailableNewVersion) {
                    BT_Download.setEnabled(true);
                    pendingDownloadUrl = bestDlUrl;
                    pendingDownloadFileName = bestFileName;

                    String whatsNewsStr = fullChangelog.length() > 0
                            ? fullChangelog.toString()
                            : "Есть новые версии, но не удалось получить описания изменений.";

                    whatsNewsStr = whatsNewsStr.replaceAll("\r", "");
                    TP_WhatsNews.setText(whatsNewsStr);
                } else {
                    BT_Download.setEnabled(false);
                    TP_WhatsNews.setText("Обновлений не найдено.");
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
                SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
                    boolean error = false;

                    @Override
                    protected Void doInBackground() {
                        try {
                            downloadProgress = 0;
                            PB_Download.setValue(downloadProgress);

                            if (pendingDownloadUrl != null && !pendingDownloadUrl.isEmpty()) {
                                programUpdater.downloadFromDirectUrl(pendingDownloadUrl, pendingDownloadFileName);
                            } else {
                                programUpdater.downloadUpdate();
                            }

                        } catch (IOException | InterruptedException exception) {
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
                        if (!error) {
                            TF_DownloadResult.setText("Загрузка завершена!");
                        }
                    }
                };

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
        mainPanel.setLayout(new GridLayoutManager(6, 1, new Insets(12, 12, 12, 12), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 8), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        LB_Source = new JLabel();
        LB_Source.setText("Источник:");
        panel1.add(LB_Source, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CB_Source = new JComboBox();
        panel1.add(CB_Source, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_Check = new JButton();
        BT_Check.setText("Проверить");
        panel1.add(BT_Check, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(140, 32), new Dimension(140, 32), new Dimension(140, 48), 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 80), new Dimension(-1, 130), null, 0, false));
        TP_CheckResult = new JTextPane();
        TP_CheckResult.setEditable(false);
        scrollPane1.setViewportView(TP_CheckResult);
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        mainPanel.add(scrollWhatsNews, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 200), new Dimension(-1, 320), null, 0, false));
        TP_WhatsNews = new JTextPane();
        TP_WhatsNews.setEditable(false);
        scrollWhatsNews.setViewportView(TP_WhatsNews);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(4, 0, 0, 0), 6, -1));
        mainPanel.add(panel2, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_Download = new JButton();
        BT_Download.setText("Скачать");
        panel2.add(BT_Download, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(140, 32), new Dimension(140, 32), new Dimension(140, 48), 0, false));
        PB_Download = new JProgressBar();
        panel2.add(PB_Download, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        TF_DownloadResult = new JTextField();
        TF_DownloadResult.setEditable(false);
        mainPanel.add(TF_DownloadResult, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, 30), null, null, 0, false));
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
        scrollWhatsNews = new JScrollPane();

        JScrollBar vertical = scrollWhatsNews.getVerticalScrollBar();
        vertical.setUI(new CustomScrollBarUI());

        JScrollBar horizontal = scrollWhatsNews.getHorizontalScrollBar();
        horizontal.setUI(new CustomScrollBarUI());

        TP_WhatsNews = new JTextPane();
        TP_WhatsNews.setEditable(false);
        scrollWhatsNews.setViewportView(TP_WhatsNews);
    }


}
