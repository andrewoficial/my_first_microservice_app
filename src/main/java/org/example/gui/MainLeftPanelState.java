package org.example.gui;

import lombok.Getter;
import lombok.Setter;

public class MainLeftPanelState {
    @Setter
    @Getter
    private String command = "";

    @Setter
    @Getter
    private String prefix = "";

    @Setter
    @Getter
    private int dataBits = 0;

    @Setter
    @Getter
    private int dataBitsValue = 0;

    @Setter
    @Getter
    private int parityBit = 0;

    @Setter
    @Getter
    private int parityBitValue = 0;

    @Setter
    @Getter
    private int stopBits = 0;

    @Setter
    @Getter
    private int stopBitsValue = 0;

    @Setter
    @Getter
    private int baudRate = 0;

    @Setter
    @Getter
    private int baudRateValue = 0;

    @Setter
    @Getter
    private int protocol = 0;

    @Setter
    @Getter
    private int clientId = 0;

    @Setter
    @Getter
    private int tabNumber = 0;



}
