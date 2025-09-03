package org.example.gui;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MainLeftPanelState {
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

}
