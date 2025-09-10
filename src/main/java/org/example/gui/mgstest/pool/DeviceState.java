// DeviceState.java
package org.example.gui.mgstest.pool;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.mgstest.device.AllCoef;
import org.example.gui.mgstest.device.DeviceInfo;

@Setter
@Getter
public class DeviceState {

    private DeviceInfo deviceInfo;
    private AllCoef allCoef;


}