// DeviceState.java
package org.example.gui.mgstest.repository;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.model.answer.GetAllCoefficients;
import org.example.gui.mgstest.model.answer.GetDeviceInfo;

@Setter
@Getter
public class DeviceState {

    private GetDeviceInfo deviceInfo;
    private GetAllCoefficients allCoefficients;


}