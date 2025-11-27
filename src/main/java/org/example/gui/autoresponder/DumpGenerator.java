package org.example.gui.autoresponder;

import javax.swing.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DumpGenerator {

    public static byte[] generateDump(DpsEmulatorWindow window) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Пример: vin (fake), vout_set, iout_set, zeros, temp, limits...
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(safetyGetValue(window.jtfVin))));
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(safetyGetValue(window.jtfVoltageSet))));
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(safetyGetValue(window.jtfCurrentSet))));
        // Add zeros and other fixed/floats from log
        byte[] zeros = new byte[12]; // Adjust
        baos.write(zeros);
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(safetyGetValue(window.jtfTemperature))));
        // Add more from log structure
        // Total len should be 0x8B (139)
        byte[] fixedPart = ByteUtils.hexStringToBytes("00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00"); // From log, adjust
        baos.write(fixedPart);
        return baos.toByteArray();
    }

    public static String safetyGetValue(JTextField jtfTemperature) {
        if(jtfTemperature == null) {
            return "0";
        }
        String value = jtfTemperature.getText();
        if (value == null) {
            return "0";
        }
        if(value.isEmpty()) {
            return "0";
        }
        if(value.equals(".")) {
            return "0.0";
        }
        if(value.contains(",")) {
            return value.replace(",", ".");
        }
        return value;
    }
}