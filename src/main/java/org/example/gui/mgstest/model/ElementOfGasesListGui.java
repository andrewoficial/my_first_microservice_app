package org.example.gui.mgstest.model;

import lombok.Data;

@Data
public class ElementOfGasesListGui {
    private String gasName;
    private byte gasCode;

    public ElementOfGasesListGui(String gasName, byte gasCode) {
        this.gasName = gasName;
        this.gasCode = gasCode;
    }

    @Override
    public String toString() {
        return gasName;
    }
}
