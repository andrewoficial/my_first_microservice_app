package org.example.gui.mgstest.transport;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.model.answer.GetSensStatusModel;
import org.example.services.comPort.StringEndianList;

@Getter @Setter
public class CommandParameters {
    String type;
    String stringArgument;
    Long longArgument;
    int intArgument;
    float [] coefficients;
    int [] intArguments;
    short [] shortArguments;
    StringEndianList endian;

    GetSensStatusModel sensStatusModel;
}
