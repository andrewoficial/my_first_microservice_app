package org.example.gui;

import lombok.Getter;
import lombok.Setter;
import static org.example.utilites.Constants.Gui.Windows.DEVICE_NAME_LIMIT;

@Setter
@Getter
public class MainLeftPanelState {
    private String visibleName = "";

    private String command = "";

    private String prefix = "";

    private int dataBits = 0;

    private int dataBitsValue = 0;

    private int parityBit = 0;

    private int parityBitValue = 0;

    private int stopBits = 0;

    private int stopBitsValue = 0;

    private int baudRate = 0;

    private int baudRateValue = 0;

    private int protocol = 0;

    private int clientId = 0;

    private int tabNumber = 0;

    private int comPortComboNumber = 0;

    private byte [] rawCommand = null;

    public void setVisibleName(String visibleName) {
        if(visibleName == null) return;
        if(visibleName.length() > DEVICE_NAME_LIMIT){
            visibleName = visibleName.substring(0, DEVICE_NAME_LIMIT);
        }
        this.visibleName = visibleName;
    }

}
