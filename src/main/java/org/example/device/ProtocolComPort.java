package org.example.device;

import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.ParityList;
import org.example.services.comPort.StopBitsList;

public interface ProtocolComPort {
    DataBitsList getDefaultDataBit();
    ParityList getDefaultParity();
    BaudRatesList getDefaultBaudRate();
    StopBitsList getDefaultStopBit();

}
