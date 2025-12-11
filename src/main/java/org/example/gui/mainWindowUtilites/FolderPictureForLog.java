package org.example.gui.mainWindowUtilites;

import com.intellij.uiDesigner.core.GridConstraints;
import org.example.gui.components.NimbusCustomizer;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class FolderPictureForLog {
    private static final Logger log = Logger.getLogger(FolderPictureForLog.class.getName());

    public JPanel getPicContainer(String lbl, boolean isPoolServiceFound, boolean isLogActive, File file) {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        container.setBackground(NimbusCustomizer.defBackground);

        if (isPoolServiceFound) {
            if (file == null) {
                isLogActive = false;
                //log.warning("File is null, setting isLogActive to false");
            }
            if (isLogActive) {
                container.setOpaque(false); // Прозрачный фон

                JButton folderButton = new JButton();
                folderButton.setToolTipText("Открыть расположение файла лога");
                folderButton.setBorderPainted(false);
                folderButton.setContentAreaFilled(false);
                folderButton.setFocusPainted(false);
                folderButton.setMargin(new Insets(0, 0, 0, 0));
                Icon folderIcon = UIManager.getIcon("FileView.directoryIcon");
                if (folderIcon == null) {
                    log.warning("FileView.directoryIcon not found, using default text");
                    folderButton.setText("Folder");
                } else {
                    folderButton.setIcon(folderIcon);
                }

                folderButton.addActionListener(e -> openExplorerWithSelectedFile(file));

                JLabel fileNameLabel = new JLabel(file.getName());
                fileNameLabel.setToolTipText(file.getAbsolutePath());

                container.add(folderButton);
                // container.add(fileNameLabel); // Раскомментируйте, если хотите показывать имя файла
            } else {
                container.add(new JLabel(" "), getDefaultGrid());
            }
        } else {
            container.add(new JLabel(" "), getDefaultGrid());
        }
        return container;
    }

    private void openExplorerWithSelectedFile(File file) {
        if (file == null || !file.exists()) {
            log.severe("File is null or does not exist: " + (file != null ? file.getAbsolutePath() : "null"));
            JOptionPane.showMessageDialog(null, "Файл лога не найден!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String os = System.getProperty("os.name").toLowerCase();
        log.info("Attempting to open file explorer for file: " + file.getAbsolutePath() + " on OS: " + os);

        try {
            if (os.contains("win")) {
                // Windows: команда explorer.exe с /select выделяет файл
                Runtime.getRuntime().exec(new String[]{"explorer.exe", "/select,", file.getAbsolutePath()});
                log.info("Opened folder in Windows Explorer with file selected: " + file.getAbsolutePath());
            } else if (os.contains("mac")) {
                // macOS: команда open -R должна выделять файл в Finder
                Runtime.getRuntime().exec(new String[]{"open", "-R", file.getAbsolutePath()});
                log.info("Opened folder in macOS Finder with file selected: " + file.getAbsolutePath());
            } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
                // Linux: пробуем файловые менеджеры с поддержкой выделения
                String parentDir = file.getParent();
                if (parentDir == null) {
                    throw new IOException("Parent directory is null");
                }

                // Список команд для файловых менеджеров
                String[][] commands = {
                        {"nautilus", "--select", file.getAbsolutePath()}, // GNOME (Ubuntu)
                        {"dolphin", "--select", file.getAbsolutePath()},  // KDE
                        {"thunar", file.getAbsolutePath()},               // XFCE (Thunar не поддерживает --select, просто открывает папку)
                        {"xdg-open", parentDir}                          // Запасной вариант, открывает папку без выделения
                };

                boolean success = false;
                for (String[] cmd : commands) {
                    try {
                        Process process = Runtime.getRuntime().exec(cmd);
                        process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
                        success = process.exitValue() == 0;
                        if (success) {
                            log.info("Opened folder using " + cmd[0] + " with file: " + file.getAbsolutePath());
                            break;
                        }
                    } catch (IOException | InterruptedException e) {
                        log.fine("Command " + cmd[0] + " failed: " + e.getMessage());
                    }
                }
                if (!success) {
                    throw new IOException("No suitable file manager found for Linux/Unix");
                }
            } else {
                // Неподдерживаемая ОС: используем Desktop API как запасной вариант
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    Desktop.getDesktop().open(file.getParentFile());
                    log.info("Opened folder using Desktop API: " + file.getParent());
                } else {
                    log.warning("Unsupported OS and Desktop API not available: " + os);
                    JOptionPane.showMessageDialog(null,
                            "Функция открытия папки не поддерживается в вашей ОС",
                            "Предупреждение", JOptionPane.WARNING_MESSAGE);
                }
            }
        } catch (IOException ex) {
            log.severe("Error opening file explorer: " + ex.getMessage());
            JOptionPane.showMessageDialog(null,
                    "Ошибка при открытии проводника: " + ex.getMessage(),
                    "Ошибка", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public GridConstraints getDefaultGrid() {
        return new GridConstraints(
                0, 0, 1, 1,
                GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE,
                GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED,
                null, null, null
        );
    }
}