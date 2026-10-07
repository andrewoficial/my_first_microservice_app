package org.example.gui.settings.appearance;

import lombok.extern.slf4j.Slf4j;
import org.example.gui.theme.AppTheme;
import org.example.gui.theme.ThemeManager;
import org.example.utilites.properties.MyProperties;

import javax.swing.*;
import java.awt.*;

/**
 * Окно настроек внешнего вида: выбор темы оформления и (пока неизменяемый) язык.
 * «Сохранить» пишет тему в настройки и применяет её сразу.
 */
@Slf4j
public class AppearanceSettingsWindow extends JDialog {

    private final JComboBox<AppTheme> themeCombo = new JComboBox<>(AppTheme.values());
    private final JComboBox<String> languageCombo = new JComboBox<>(new String[]{"Русский"});

    public AppearanceSettingsWindow() {
        setModal(true);
        setTitle("Внешний вид");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(new JLabel("Тема оформления:"), gbc);
        gbc.gridx = 1;
        form.add(themeCombo, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(new JLabel("Язык:"), gbc);
        gbc.gridx = 1;
        languageCombo.setEnabled(false);
        form.add(languageCombo, gbc);

        JButton cancel = new JButton("Отмена");
        JButton save = new JButton("Сохранить");
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(save);

        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        setContentPane(content);

        loadValues();
        pack();
        setLocationRelativeTo(null);
    }

    private void loadValues() {
        MyProperties props = MyProperties.getInstance();
        AppTheme current = props != null
                ? AppTheme.fromKey(props.getGuiTheme())
                : ThemeManager.getCurrent();
        themeCombo.setSelectedItem(current);
        languageCombo.setSelectedItem("Русский");
    }

    private void onSave() {
        AppTheme selected = (AppTheme) themeCombo.getSelectedItem();
        if (selected == null) {
            selected = AppTheme.NIMBUS;
        }
        MyProperties props = MyProperties.getInstance();
        if (props != null) {
            props.setGuiTheme(selected.getKey());
        }
        ThemeManager.apply(selected);
        log.info("Тема оформления сохранена: {}", selected.getKey());
        dispose();
    }
}
