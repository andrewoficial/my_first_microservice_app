package org.example.gui;

import javax.swing.*;

class DummyFrame extends JFrame {
    DummyFrame(String title) {
        super(title);
        setUndecorated(true);
        setVisible(true);
        setLocationRelativeTo(null);
        //setIconImages(iconImages);
    }
}