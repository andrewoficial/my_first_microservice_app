package org.example.gui.devices.emulation;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Лог команд виртуальной камеры (панели имитации).
 *
 * <p>Ведёт отдельный файл {@code logs/<имя камеры>_commands.log} со строками вида
 * <pre>[дата время] [принята команда: ...]</pre>
 * и, при включённом «развернутом логе», дополнительно
 * <pre>[дата время] [запрос данных командой: ...]</pre>
 * Управление галочками собирает {@link #createControls(EmulatorCommandLog)}.
 */
public final class EmulatorCommandLog {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String cameraName;
    private final Path file;
    private volatile boolean enabled = false;
    private volatile boolean extended = false;

    public EmulatorCommandLog(String cameraName) {
        this.cameraName = cameraName;
        String safe = cameraName.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
        this.file = Paths.get("logs", safe + "_commands.log");
    }

    public void setEnabled(boolean e) { enabled = e; }
    public boolean isEnabled() { return enabled; }
    public void setExtended(boolean e) { extended = e; }
    public boolean isExtended() { return extended; }

    /** Записать полученную от клиента команду (всегда при включённом логе). */
    public void command(String description) {
        if (!enabled) return;
        write("[принята команда: " + description + "]");
    }

    /** Записать рутинный запрос данных (только при включённом «развернутом логе»). */
    public void dataRequest(String detail) {
        if (!enabled || !extended) return;
        write("[запрос данных командой: " + detail + "]");
    }

    /** Имя файла лога (для подсказки в панели). */
    public String fileName() { return file.toString(); }

    private void write(String tag) {
        String line = "[" + LocalDateTime.now().format(TS) + "] " + tag;
        try {
            Path parent = file.getParent();
            if (parent != null) Files.createDirectories(parent);
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                w.write(line);
                w.newLine();
            }
        } catch (IOException e) {
            // лог не должен ломать эмулятор
        }
    }

    /**
     * Галочки «Лог команд» и «Развернутый лог» + путь файла.
     * Подключаются к {@code log} (включённость сразу применяется к панели).
     */
    public static JPanel createControls(EmulatorCommandLog log) {
        JCheckBox cbLog = new JCheckBox("Лог команд", log.isEnabled());
        JCheckBox cbExt = new JCheckBox("Развернутый лог", log.isExtended());
        JLabel lbl = new JLabel("→ " + log.fileName());
        lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 10));
        lbl.setForeground(Color.GRAY);

        cbLog.addActionListener(e -> {
            log.setEnabled(cbLog.isSelected());
            cbExt.setEnabled(cbLog.isSelected());
        });
        cbExt.addActionListener(e -> log.setExtended(cbExt.isSelected()));
        cbExt.setEnabled(cbLog.isSelected());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        row.add(cbLog);
        row.add(cbExt);
        row.add(lbl);
        return row;
    }
}