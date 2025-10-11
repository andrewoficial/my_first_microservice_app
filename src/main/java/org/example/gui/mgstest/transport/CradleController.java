package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.hid4java.HidDevice;



public class CradleController {

    private final Logger log = Logger.getLogger(CradleController.class);




    public void setCoefficientsO2(HidDevice device) throws Exception {
        double[] coefs = new double[19];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 4620010.5;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        //setCoefForGas("o2", coefs, device);
    }

    public void setCoefficientsCO(HidDevice device) throws Exception {
        double[] coefs = new double[14];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 201.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        //setCoefForGas("co", coefs, device);
    }

    public void setCoefficientsH2S(HidDevice device) throws Exception {
        double[] coefs = new double[14];
        for (int i = 0; i < coefs.length; i++) {
            coefs[i] = i + 401.0;
        }
        log.info("Будут заданы " + coefs[0] + "..." + coefs[coefs.length-1] + " для газа ");
        //setCoefForGas("h2s", coefs, device);
    }






//FixMe: 711 строчек!!!
}