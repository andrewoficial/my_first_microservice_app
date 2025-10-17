// DeviceState.java
package org.example.gui.mgstest.model;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.model.answer.*;

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
    private GetGasRangeModel gasRangeModel;
    private GetSensStatusModel sensStatusModel;
    private int progressPercent = 0;
    private String progressMessage = "";
    private String currentOperation = "";
    private String showedName = "";
}