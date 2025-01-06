package org.example.gui;

import lombok.Getter;
import lombok.Setter;

public class MainLeftPanelState {
    @Setter
    @Getter
    private int dataBits = 0;

    @Setter
    @Getter
    private int parityBit = 0;

    @Setter
    @Getter
    private int stopBits = 0;

    @Setter
    @Getter
    private int baudRate = 0;

    @Setter
    @Getter
    private int protocol = 0;



}
