package org.example.gui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.example.utilites.update.LauncherUpdater;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;

/**
 * Окно ручной проверки/загрузки обновлений лаунчера ({@code launcher.exe}).
 * Построено по образцу {@link UpdateWindow}, но источник — GitHub-репозиторий
 * {@code andrewoficial/elephant-monitor-launcher}, а установленная версия
 * определяется по SHA-256 файла лаунчера.
 */
public class UpdateLauncherWindow extends JDialog implements Rendeble {
    private JPanel mainPanel;
    private JLabel LB_Source;
    private JComboBox CB_Source;
    private JButton BT_Check;
    private JTextPane TP_CheckResult;
    private JScrollPane scrollWhatsNews;
    private JTextPane TP_WhatsNews;
    private JButton BT_Download;
    private JProgressBar PB_Download;
    private JTextField TF_DownloadResult;

    private final LauncherUpdater launcherUpdater = new LauncherUpdater();

    private Path installedLauncher;
    private LauncherUpdater.LauncherReleaseInfo pendingRelease;

    public UpdateLauncherWindow() {
        $$$setupUI$$$();

        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(mainPanel);

        BT_Download.setEnabled(false);
        PB_Download.setMinimum(0);
        PB_Download.setMaximum(100);
        PB_Download.setStringPainted(true);

        CB_Source.addItem("GitHub (elephant-monitor-launcher)");

        BT_Check.addActionListener(e -> doCheck());
        BT_Download.addActionListener(e -> doDownload());
    }

    private void doCheck() {
        BT_Check.setEnabled(false);
        BT_Download.setEnabled(false);
        pendingRelease = null;
        TP_CheckResult.setText("Проверяю обновления лаунчера...");
        TP_WhatsNews.setText("");
        TF_DownloadResult.setText("Начинаю проверку!");

        new SwingWorker<LauncherUpdater.CheckResult, Void>() {
            @Override
            protected LauncherUpdater.CheckResult doInBackground() {
                return launcherUpdater.checkForUpdate();
            }

            @Override
            protected void done() {
                BT_Check.setEnabled(true);
                LauncherUpdater.CheckResult result;
                try {
                    result = get();
                } catch (Exception ex) {
                    result = null;
                }
                renderResult(result);
            }
        }.execute();
    }

    private void renderResult(LauncherUpdater.CheckResult result) {
        if (result == null || result.failed) {
            installedLauncher = result != null ? result.localLauncher : null;
            String error = result != null && result.error != null ? result.error : "неизвестная ошибка";
            TP_CheckResult.setText("Не удалось проверить обновления лаунчера.\n" + error);
            TP_WhatsNews.setText("Проверка не удалась.");
            TF_DownloadResult.setText("Проверка не удалась");
            return;
        }

        installedLauncher = result.localLauncher;

        StringBuilder sb = new StringBuilder();
        sb.append("Установленный лаунчер: ").append(result.localLauncher).append("\n");
        sb.append("SHA-256: ").append(shortHash(result.localHash)).append("\n");
        if (result.latest != null) {
            sb.append("Последняя версия: ").append(orDash(result.latest.version)).append("\n");
        }

        if (result.updateAvailable && result.latest != null) {
            pendingRelease = result.latest;
            BT_Download.setEnabled(result.latest.hasDownload());
            sb.append("\nДоступно обновление лаунчера.");
            TP_CheckResult.setText(sb.toString());

            String notes = result.latest.notes;
            TP_WhatsNews.setText(notes != null && !notes.isBlank()
                    ? notes
                    : "Есть новая версия лаунчера, но описание изменений отсутствует.");
            TF_DownloadResult.setText("Проверка завершена: есть обновление!");
        } else {
            sb.append("\nОбновлений лаунчера не найдено.");
            TP_CheckResult.setText(sb.toString());
            TP_WhatsNews.setText("Обновлений не найдено.");
            TF_DownloadResult.setText("Проверка завершена!");
        }
    }

    private void doDownload() {
        if (pendingRelease == null || installedLauncher == null) {
            return;
        }
        BT_Download.setEnabled(false);
        PB_Download.setValue(0);
        TF_DownloadResult.setText("Загружаю лаунчер...");

        new SwingWorker<Path, Void>() {
            private Exception error;

            @Override
            protected Path doInBackground() {
                try {
                    return launcherUpdater.download(pendingRelease, installedLauncher);
                } catch (Exception ex) {
                    error = ex;
                    return null;
                }
            }

            @Override
            protected void done() {
                if (error != null) {
                    TF_DownloadResult.setText("Ошибка загрузки: " + error.getMessage());
                } else {
                    Path saved = null;
                    try {
                        saved = get();
                    } catch (Exception ignored) {
                        // get() уже отработал в doInBackground
                    }
                    PB_Download.setValue(100);
                    TF_DownloadResult.setText(saved != null
                            ? "Загружено: " + saved
                            : "Загрузка завершена!");
                }
                BT_Download.setEnabled(true);
            }
        }.execute();
    }

    private static String orDash(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.isBlank()) {
            return "—";
        }
        return hash.length() <= 16 ? hash : hash.substring(0, 16) + "…";
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(6, 1, new Insets(12, 12, 12, 12), -1, -1));
        panel1.add(mainPanel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 8), -1, -1));
        mainPanel.add(panel2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        LB_Source = new JLabel();
        LB_Source.setText("Источник:");
        panel2.add(LB_Source, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        CB_Source = new JComboBox();
        panel2.add(CB_Source, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_Check = new JButton();
        BT_Check.setText("Проверить");
        panel2.add(BT_Check, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(140, 32), new Dimension(140, 32), new Dimension(140, 48), 0, false));
        final JScrollPane scrollPane1 = new JScrollPane();
        mainPanel.add(scrollPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, new Dimension(-1, 80), new Dimension(-1, 130), null, 0, false));
        TP_CheckResult = new JTextPane();
        TP_CheckResult.setEditable(false);
        scrollPane1.setViewportView(TP_CheckResult);
        final Spacer spacer1 = new Spacer();
        mainPanel.add(spacer1, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        scrollWhatsNews = new JScrollPane();
        mainPanel.add(scrollWhatsNews, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 200), new Dimension(-1, 320), null, 0, false));
        TP_WhatsNews = new JTextPane();
        TP_WhatsNews.setEditable(false);
        scrollWhatsNews.setViewportView(TP_WhatsNews);
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(1, 2, new Insets(4, 0, 0, 0), 6, -1));
        mainPanel.add(panel3, new GridConstraints(4, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        BT_Download = new JButton();
        BT_Download.setText("Скачать");
        panel3.add(BT_Download, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(140, 32), new Dimension(140, 32), new Dimension(140, 48), 0, false));
        PB_Download = new JProgressBar();
        panel3.add(PB_Download, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        TF_DownloadResult = new JTextField();
        TF_DownloadResult.setEditable(false);
        mainPanel.add(TF_DownloadResult, new GridConstraints(5, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(300, 30), null, null, 0, false));
    }

    @Override
    public void renderData() {
        // прогресс загрузки отображается по завершении; отдельный опрос не требуется
    }

    @Override
    public boolean isEnable() {
        return this.isShowing();
    }
}
