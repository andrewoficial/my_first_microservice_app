package org.example.gui.system.logs;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.example.gui.Rendeble;
import org.example.gui.mainWindowUtilites.FolderPictureForLog;
import org.example.gui.system.resources.DebugWindow;
import org.example.services.loggers.PoolLogger;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ViewLogsWindow extends JDialog implements Rendeble {
    private Logger log = null;
    private final JTextArea systemLogArea = new JTextArea();
    private final JTextArea comTransferLogArea = new JTextArea();
    private final JTextArea socketTransferLogArea = new JTextArea();
    private final JTextArea hidDevTransferLogArea = new JTextArea();

    private Path systemLogPath;
    private Path comLogPath;

    private JPanel systemLog;
    private JPanel comTransferLog;
    private JPanel socketTransferLog;
    private JPanel hidDevTransferLog;
    private JPanel buttons;
    private JScrollPane systemLogScroll;
    private JScrollPane comTransferLogScroll;
    private JScrollPane socketTransferLogScroll;
    private JScrollPane hidDevTransferLogScroll;
    private JButton clearSysScroll;
    private JButton clearComScroll;
    private JButton clearHidScroll;
    private JButton clearSocketScroll;
    private JButton archiveLogsAndClearFolderBtn;
    private JPanel mainPanel;
    private FolderPictureForLog folderPictureForLog = new FolderPictureForLog();

    public ViewLogsWindow() {
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        // Создаем основной интерфейс
        createUI();

        setContentPane(mainPanel);
        log = Logger.getLogger(DebugWindow.class);
        log.info("Открыто окно с выводом логов системы");

        // Инициализация текстовых областей
        initTextArea(systemLogScroll, systemLogArea);
        initTextArea(comTransferLogScroll, comTransferLogArea);
        initTextArea(socketTransferLogScroll, socketTransferLogArea);
        initTextArea(hidDevTransferLogScroll, hidDevTransferLogArea);

        // Настройка кнопок
        setupButtons();

        // Настройка окна
        setTitle("Просмотр системных логов");
        pack();
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void createUI() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Панель для логов
        JPanel logsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        logsPanel.setBorder(BorderFactory.createTitledBorder("Логи системы"));

        // Системный лог
        JPanel systemPanel = new JPanel(new BorderLayout());
        systemPanel.setBorder(BorderFactory.createTitledBorder("Системный лог"));
        systemLogScroll = new JScrollPane();
        systemPanel.add(systemLogScroll, BorderLayout.CENTER);
        logsPanel.add(systemPanel);

        // COM-лог
        JPanel comPanel = new JPanel(new BorderLayout());
        comPanel.setBorder(BorderFactory.createTitledBorder("COM-обмен"));
        comTransferLogScroll = new JScrollPane();
        comPanel.add(comTransferLogScroll, BorderLayout.CENTER);
        logsPanel.add(comPanel);

        // Socket-лог
        JPanel socketPanel = new JPanel(new BorderLayout());
        socketPanel.setBorder(BorderFactory.createTitledBorder("Socket-обмен"));
        socketTransferLogScroll = new JScrollPane();
        socketPanel.add(socketTransferLogScroll, BorderLayout.CENTER);
        logsPanel.add(socketPanel);

        // HID-лог
        JPanel hidPanel = new JPanel(new BorderLayout());
        hidPanel.setBorder(BorderFactory.createTitledBorder("HID-обмен"));
        hidDevTransferLogScroll = new JScrollPane();
        hidPanel.add(hidDevTransferLogScroll, BorderLayout.CENTER);
        logsPanel.add(hidPanel);

        mainPanel.add(logsPanel, BorderLayout.CENTER);

        // Панель кнопок
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        buttonPanel.setBorder(BorderFactory.createTitledBorder("Управление логами"));

        // Панель для кнопок очистки
        JPanel clearButtonsPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        clearSysScroll = new JButton("Очистить системный");
        clearComScroll = new JButton("Очистить COM");
        clearHidScroll = new JButton("Очистить HID");
        clearSocketScroll = new JButton("Очистить Socket");

        clearButtonsPanel.add(clearSysScroll);
        clearButtonsPanel.add(clearComScroll);
        clearButtonsPanel.add(clearHidScroll);
        clearButtonsPanel.add(clearSocketScroll);

        // Панель для кнопки архивации
        JPanel archivePanel = new JPanel(new BorderLayout());
        archiveLogsAndClearFolderBtn = new JButton("Архивировать логи и очистить папку");
        archivePanel.add(archiveLogsAndClearFolderBtn, BorderLayout.CENTER);

        buttonPanel.add(clearButtonsPanel);
        buttonPanel.add(archivePanel);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void initTextArea(JScrollPane scrollPane, JTextArea textArea) {
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText("Загрузка логов...");

        // Автоматическая прокрутка вниз
        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        scrollPane.setViewportView(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    }

    private void setupButtons() {
        clearSysScroll.addActionListener(e -> clearLog(systemLogArea));
        clearComScroll.addActionListener(e -> clearLog(comTransferLogArea));
        clearHidScroll.addActionListener(e -> clearLog(hidDevTransferLogArea));
        clearSocketScroll.addActionListener(e -> clearLog(socketTransferLogArea));
        archiveLogsAndClearFolderBtn.addActionListener(e -> archiveLogs());
    }


    @Override
    public void renderData() {
        // Определяем пути к лог-файлам
        systemLogPath = getSystemLogPath();

        comLogPath = getComLogPath();

        // Загружаем логи
        loadLogContent(systemLogPath, systemLogArea);
        loadLogContent(comLogPath, comTransferLogArea);

        // Для остальных логов оставляем заглушки
        hidDevTransferLogArea.setText("HID Device logs will be available in future versions");
        socketTransferLogArea.setText("Socket transfer logs will be implemented soon");
    }

    private Path getComLogPath() {
        try {
            PoolLogger comDataTransferLogger = PoolLogger.getInstance();
            Path logPath = comDataTransferLogger.getLogFile();
            File logFile = new File(logPath.toUri());
            if (logFile != null && logFile.exists()) {
                return logFile.toPath();
            }

            // Если основной файл недоступен, ищем самый свежий лог в директории
            Path logsDir = Paths.get("logs");
            if (Files.exists(logsDir)) {
                return Files.list(logsDir)
                        .filter(path -> path.getFileName().toString().contains("SumLog"))
                        .filter(Files::isRegularFile)
                        .max(Comparator.comparingLong(
                                path -> {
                                    try {
                                        return Files.getLastModifiedTime((
                                                Path) path).toMillis();
                                    } catch (IOException e) {
                                        return 0L;
                                    }
                                }))
                        .orElse(null);

            }
        } catch (Exception e) {
            log.error("Error getting COM log path", e);
        }

        // Fallback с текущей датой
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " SumLog.txt";
        return Paths.get("logs", fileName);
    }

    private Path getSystemLogPath() {
        try {
            // Проверяем все аппендеры, включая дочерние логгеры
            Logger rootLogger = Logger.getRootLogger();
            Path path = findFileAppenderPath(rootLogger);
            if (path != null) return path;

            // Проверяем логгер текущего класса
            Logger currentLogger = Logger.getLogger(getClass());
            if (currentLogger != rootLogger) {
                path = findFileAppenderPath(currentLogger);
                if (path != null) return path;
            }

            // Проверяем другие возможные логгеры
            Logger systemLogger = Logger.getLogger("org.example");
            if (systemLogger != rootLogger && systemLogger != currentLogger) {
                path = findFileAppenderPath(systemLogger);
                if (path != null) return path;
            }
        } catch (Exception e) {
            log.error("Error getting system log path", e);
        }

        // Fallback с текущей датой
        String fileName = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")) + " EM-LogFile.log";
        return Paths.get("logs", fileName);
    }

    private Path findFileAppenderPath(Logger logger) {
        Enumeration<Appender> appenders = logger.getAllAppenders();
        while (appenders.hasMoreElements()) {
            Appender appender = appenders.nextElement();
            if (appender instanceof FileAppender) {
                String filePath = ((FileAppender) appender).getFile();
                if (filePath != null && !filePath.isEmpty()) {
                    Path path = Paths.get(filePath);
                    if (Files.exists(path)) {
                        return path;
                    }
                }
            }
        }
        return null;
    }

    private void loadLogContent(Path path, JTextArea textArea) {
        if (path == null || !Files.exists(path)) {
            textArea.setText("Log file not found: " + path);
            return;
        }

        try {
            String content = new String(Files.readAllBytes(path));
            textArea.setText(content);
            textArea.setCaretPosition(textArea.getDocument().getLength()); // Scroll to bottom
        } catch (IOException e) {
            textArea.setText("Error reading log: " + e.getMessage());
            log.error("Error loading log content", e);
        }
    }

    private void clearLog(JTextArea textArea) {
        textArea.setText("");

        // Для системного лога используем log4j reset
        if (textArea == systemLogArea && systemLogPath != null) {
            try {
                Files.write(systemLogPath, new byte[0]);
                log.info("System log cleared");
            } catch (IOException e) {
                log.error("Error clearing system log", e);
            }
        }
    }

    private void archiveLogs() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Log Archive");
        fileChooser.setSelectedFile(new File("logs_archive_" +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".zip"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archiveFile = fileChooser.getSelectedFile();

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    createLogArchive(archiveFile.toPath());
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get(); // Проверяем исключения
                        JOptionPane.showMessageDialog(ViewLogsWindow.this,
                                "Logs archived successfully!\n" + archiveFile.getAbsolutePath(),
                                "Archive Complete",
                                JOptionPane.INFORMATION_MESSAGE);
                    } catch (Exception e) {
                        log.error("Archive creation failed", e);
                        JOptionPane.showMessageDialog(ViewLogsWindow.this,
                                "Error creating archive: " + e.getMessage(),
                                "Archive Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void createLogArchive(Path archivePath) throws IOException {
        Path logsDir = Paths.get("logs");

        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archivePath))) {
            Files.walk(logsDir)
                    .filter(path -> !Files.isDirectory(path))
                    .forEach(path -> {
                        try {
                            String relativePath = logsDir.relativize(path).toString();
                            zos.putNextEntry(new ZipEntry(relativePath));
                            Files.copy(path, zos);
                            zos.closeEntry();
                        } catch (IOException e) {
                            log.error("Error adding to archive: " + path, e);
                        }
                    });
        }
        log.info("Logs archived to: " + archivePath);
    }

    @Override
    public boolean isEnable() {
        return this.isShowing();
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
        mainPanel.setLayout(new GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        systemLog = new JPanel();
        systemLog.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(systemLog, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        systemLogScroll = new JScrollPane();
        systemLog.add(systemLogScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        comTransferLog = new JPanel();
        comTransferLog.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(comTransferLog, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        comTransferLogScroll = new JScrollPane();
        comTransferLog.add(comTransferLogScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        socketTransferLog = new JPanel();
        socketTransferLog.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(socketTransferLog, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        socketTransferLogScroll = new JScrollPane();
        socketTransferLog.add(socketTransferLogScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        hidDevTransferLog = new JPanel();
        hidDevTransferLog.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(hidDevTransferLog, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        hidDevTransferLogScroll = new JScrollPane();
        hidDevTransferLog.add(hidDevTransferLogScroll, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null, 0, false));
        buttons = new JPanel();
        buttons.setLayout(new GridLayoutManager(2, 4, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(buttons, new GridConstraints(2, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        clearSysScroll = new JButton();
        clearSysScroll.setText("Системного лога");
        buttons.add(clearSysScroll, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JLabel label1 = new JLabel();
        label1.setText("Очистить:");
        buttons.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearComScroll = new JButton();
        clearComScroll.setText("COM-обмена");
        buttons.add(clearComScroll, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearHidScroll = new JButton();
        clearHidScroll.setText("HID-обмена");
        buttons.add(clearHidScroll, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        clearSocketScroll = new JButton();
        clearSocketScroll.setText("socked-обмена");
        buttons.add(clearSocketScroll, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(3, 0, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        archiveLogsAndClearFolderBtn = new JButton();
        archiveLogsAndClearFolderBtn.setText("Архивировать логи и очистить папку");
        panel1.add(archiveLogsAndClearFolderBtn, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        panel1.add(spacer1, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return mainPanel;
    }
}