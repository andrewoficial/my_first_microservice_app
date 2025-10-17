package org.example.gui.mgstest.model.answer;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GetSensStatusModel {
    private boolean loaded;
    private boolean O2;
    private boolean CO;
    private boolean H2S;
    private boolean CH4;

    private byte O2_num;
    private byte CO_num;
    private byte H2S_num;
    private byte CH4_num;

}
