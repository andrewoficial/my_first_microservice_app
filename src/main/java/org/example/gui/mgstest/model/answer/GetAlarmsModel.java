package org.example.gui.mgstest.model.answer;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GetAlarmsModel {
    private boolean loaded;
    private short o2From;
    private short o2To;
    private short coFrom;
    private short coTo;
    private short h2sFrom;
    private short h2sTo;
    private short ch4From;
    private short ch4To;

}