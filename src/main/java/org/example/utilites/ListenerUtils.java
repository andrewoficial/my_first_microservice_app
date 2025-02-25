package org.example.utilites;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


public class ListenerUtils {
    public static void addActionListener(JButton component, Runnable action) {
        component.addActionListener(createActionListener(action));
    }

    public static void addActionListener(JCheckBox component, Runnable action) {
        component.addActionListener(createActionListener(action));
    }

    public static void addDocumentListener(JTextComponent component, Runnable action) {
        component.getDocument().addDocumentListener(createDocumentListener(action));
    }

    public static void addKeyListener(JTextComponent component, Runnable action) {
        component.addKeyListener(createEnterKeyListener(action));
    }

    public static void addComboBoxListener(JComboBox<?> component, Runnable action) {
        component.addActionListener(createActionListener(action));
    }

    private static ActionListener createActionListener(Runnable action) {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingUtilities.invokeLater(action);
            }
        };
    }

    private static DocumentListener createDocumentListener(Runnable action) {
        return new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { trigger(); }
            @Override
            public void removeUpdate(DocumentEvent e) { trigger(); }
            @Override
            public void changedUpdate(DocumentEvent e) { trigger(); }

            private void trigger() {
                SwingUtilities.invokeLater(action);
            }
        };
    }

    private static KeyListener createEnterKeyListener(Runnable action) {
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    SwingUtilities.invokeLater(action);
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {}
        };
    }
}
