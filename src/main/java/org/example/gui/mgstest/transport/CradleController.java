package org.example.gui.mgstest.transport;

import lombok.extern.slf4j.Slf4j;
import org.example.gui.mgstest.model.HidSupportedDevice;

@Slf4j
public class CradleController {

    public void setCoefficientsO2(HidSupportedDevice device) throws Exception {
        double[] coefs = new double[19];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 4620010.5;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        //setCoefForGas("o2", coefs, device);
    }

    public void setCoefficientsCO(HidSupportedDevice device) throws Exception {
        double[] coefs = new double[14];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 201.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        //setCoefForGas("co", coefs, device);
    }

    public void setCoefficientsH2S(HidSupportedDevice device) throws Exception {
        double[] coefs = new double[14];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 401.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        //setCoefForGas("h2s", coefs, device);
    }






//раньше тут было 711 строчек, а теперь все прекрасно ^^
}