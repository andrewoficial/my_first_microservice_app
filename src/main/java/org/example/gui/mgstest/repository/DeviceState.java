// DeviceState.java
package org.example.gui.mgstest.repository;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.model.answer.GetAllCoefficientsModel;
import org.example.gui.mgstest.model.answer.GetDeviceInfoModel;
import org.example.gui.mgstest.model.answer.MipexResponseModel;

@Setter
@Getter
public class DeviceState {
    private Boolean isBusy = false;
    private GetDeviceInfoModel deviceInfo;
    private GetAllCoefficientsModel allCoefficients;
    private MipexResponseModel lastMipexResponse;
    private int progressPercent = 0;
    private String progressMessage = "";
    private String currentOperation = "";
    private String showedName = "";
}