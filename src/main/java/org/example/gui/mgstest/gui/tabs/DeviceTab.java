package org.example.gui.mgstest.gui.tabs;

import org.example.gui.mgstest.model.DeviceState;

import javax.swing.*;

public abstract class DeviceTab implements DeviceSettable {
    protected JPanel panel;
    protected String tabName;
    
    public DeviceTab(String tabName) {
        this.tabName = tabName;
        this.panel = new JPanel();
    }
    
    public JPanel getPanel() {
        return panel;
    }
    
    public String getTabName() {
        return tabName;
    }
    
    public abstract void updateData(DeviceState state);
    public abstract void saveData(DeviceState state);
}