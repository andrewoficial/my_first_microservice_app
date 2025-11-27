// org/example/gui/autoresponder/DumpGenerator.java
package org.example.gui.autoresponder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class DumpGenerator {

    public static byte[] generateDump(DpsEmulatorWindow window) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // Пример: vin (fake), vout_set, iout_set, zeros, temp, limits...
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(window.jtfVin.getText())));
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(window.jtfVoltageSet.getText())));
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(window.jtfCurrentSet.getText())));
        // Add zeros and other fixed/floats from log
        byte[] zeros = new byte[12]; // Adjust
        baos.write(zeros);
        baos.write(ByteUtils.floatToBytes(Float.parseFloat(window.jtfTemperature.getText())));
        // Add more from log structure
        // Total len should be 0x8B (139)
        byte[] fixedPart = ByteUtils.hexStringToBytes("00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00 a0 40 00 00 80 3f 00 00"); // From log, adjust
        baos.write(fixedPart);
        return baos.toByteArray();
    }
}