// DeviceState.java
package org.example.gui.mgstest.repository;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.model.answer.*;
import org.example.gui.mgstest.transport.cmd.metrology.GetAlarms;

@Setter
@Getter
public class DeviceState {
    private Boolean isBusy = false;
    private GetDeviceInfoModel deviceInfo;
    private GetAllCoefficientsModel allCoefficients;
    private MipexResponseModel lastMipexResponse;
    private GetAllSettingsModel allSettings;
    private GetVRangeModel vRangeModel;
    private GetAlarmsModel alarmsModel;
    private int progressPercent = 0;
    private String progressMessage = "";
    private String currentOperation = "";
    private String showedName = "";
}