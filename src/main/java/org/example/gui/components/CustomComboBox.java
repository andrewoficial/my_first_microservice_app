package org.example.gui.components;

import javax.swing.*;
import java.awt.*;

public class CustomComboBox {
    
    public static <T> JComboBox<T> create() {
        return create(new Color(0x2D, 0x2D, 0x2D), Color.WHITE, new Color(118, 149, 110));
    }
    
    public static <T> JComboBox<T> create(Color backgroundColor, Color foregroundColor, Color selectionColor) {
        JComboBox<T> comboBox = new JComboBox<T>() {
            @Override
            public void updateUI() {
                super.updateUI();
                // Устанавливаем кастомный UI после обновления
                setUI(new CustomComboBoxUI(backgroundColor, foregroundColor, selectionColor));
            }
        };
        
        // Базовые настройки
        comboBox.setBackground(backgroundColor);
        comboBox.setForeground(foregroundColor);
        comboBox.setRenderer(new CustomListCellRenderer<>(backgroundColor, foregroundColor, selectionColor));
        
        return comboBox;
    }
    
    // Кастомный рендерер для элементов комбобокса
    private static class CustomListCellRenderer<T> extends DefaultListCellRenderer {
        private final Color backgroundColor;
        private final Color foregroundColor;
        private final Color selectionColor;
        
        public CustomListCellRenderer(Color bgColor, Color fgColor, Color selColor) {
            this.backgroundColor = bgColor;
            this.foregroundColor = fgColor;
            this.selectionColor = selColor;
        }
        
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                                                     boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (!isSelected) {
                setBackground(backgroundColor);
                setForeground(foregroundColor);
            } else {
                setBackground(selectionColor);
                setForeground(Color.WHITE);
            }
            
            setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            return this;
        }
    }
}