package org.example.device.connectParameters;

import lombok.Getter;
import lombok.Setter;
import org.example.services.comPort.*;

public class ComConnectParameters {

    @Getter @Setter
    private DataBitsList dataBits = DataBitsList.B8;

    @Getter @Setter
    private ParityList parity = ParityList.P_NO;

    @Getter @Setter
    private StopBitsList stopBits = StopBitsList.S1;

    @Getter @Setter
    private BaudRatesList baudRate = BaudRatesList.B9600;

    @Getter @Setter
    private StringEndianList stringEndian = StringEndianList.CR_LF;

    @Getter @Setter
    private int millisLimit = 400;

    @Getter @Setter
    private int repeatWaitTime = 250;

    @Getter @Setter
    private int millisReadLimit = 150;

    @Getter @Setter
    private int millisWriteLimit = 150;

}
