package org.example.gui.mgstest.device;

import org.apache.log4j.Logger;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class AllCoef {
    // O2: 19 coefficients
    // O2: 19 coefficients (101 to 119)
    public double[] o2Coef = new double[19];
    // CO: 14 coefficients (201 to 214)
    public double[] coCoef = new double[14];
    // H2S: 14 coefficients (301 to 314)
    public double[] h2sCoef = new double[14];
    // CH4 pressure: 7 coefficients (401 to 407)
    public double[] ch4Pressure = new double[7];
    // Acceleration: 4 coefficients (501 to 504)
    public double[] acceleration = new double[4];
    // ppmMgKoefs: 4 coefficients (601 to 604)
    public double[] ppmMgKoefs = new double[4];
    // vRange: 6 coefficients (701-703, 801-803)
    public double[] vRange = new double[6];

    public static AllCoef parseAllCoef(byte[] data) {
        final Logger log = Logger.getLogger(AllCoef.class);
        if (data.length < 310) {  // 10 blocks * 32 + 3 ~310 bytes
            throw new IllegalArgumentException("Data too short, got " + data.length);
        }

        AllCoef info = new AllCoef();

        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

        // O2 coefficients from offset 27..178 (19 * 8 байт) floats (101 to 119)
        for (int i = 0; i < 19; i++) {
            info.o2Coef[i] = bb.getFloat(27 + i * 4);
        }
        for (int i = 0; i < data.length - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 101.0) {
                log.info("Found коэффициент 101 at offset " + i);
            }
        }
        log.info(" коэффициент 101 ожидался на позиции 27 ");


        for (int i = 0; i < data.length - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 201.0) {
                log.info("Found коэффициент 201 at offset " + i);
            }
        }
        // CO coefficients from offset 27 + 76 = 103: but per dump at 103=201; adjust to 103
        for (int i = 0; i < 14; i++) {
            info.coCoef[i] = bb.getFloat(103 + i * 4);
            //log.info("CO coef " + i + " (e.g., " + (i == 0 ? "InitOfs" : i == 1 ? "InitAmp" : i == 2 ? "TarOfs" : i == 3 ? "TarAmp" : i == 4 ? "dt" : "D" + (i-5)) + "): " + info.coCoef[i]);
        }
        for (int i = 0; i < data.length - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 301.0) {
                log.info("Found коэффициент 301 at offset " + i);
            }
        }
        // H2S coefficients from offset 103 + 56 = 159: 14 floats (301 to 314)
        for (int i = 0; i < 14; i++) {
            info.h2sCoef[i] = bb.getFloat(159 + i * 4);
            //log.info("H2S coef " + i + " (e.g., " + (i == 0 ? "InitOfs" : i == 1 ? "InitAmp" : i == 2 ? "TarOfs" : i == 3 ? "TarAmp" : i == 4 ? "dt" : "D" + (i-5)) + "): " + info.h2sCoef[i]);
        }
        for (int i = 0; i < data.length - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 501.0) {
                log.info("Found коэффициент 501 at offset " + i);
            }
        }
        // Acceleration from offset 159 + 56 = 215: 4 floats (501 to 504)
        for (int i = 0; i < 4; i++) {
            info.acceleration[i] = bb.getFloat(215 + i * 4);
            //log.info("Acceleration coef " + i + ": " + info.acceleration[i]);
        }
        log.info("Run search ch4Pressure");
        for (int i = 0; i < data.length - 8; i++) {
            long candidate = bb.getLong(i);
            if (candidate == 401.0) {
                log.info("Found коэффициент 401 at offset " + i + " as long");
            }
        }
        for (int i = 0; i < data.length - 8; i++) {
            int candidate = bb.getInt(i);
            if (candidate == 401) {
                log.info("Found коэффициент 401 at offset " + i + " as int");
            }
        }

        for (int i = 0; i < data.length - 8; i++) {
            short candidate = bb.getShort(i);
            short searchFor = 401;
            if (candidate == searchFor) {
                log.info("Found коэффициент 401 at offset " + i + " as short");
            }
        }

        for (int i = 0; i < data.length - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 402.0) {
                log.info("Found коэффициент 402.0 at offset " + i);
            }
        }
        log.info("Finish search ch4Pressure");
        // CH4 pressure from offset 215 + 16 = 231: 7 floats (401 to 407), including possible 0 at 231 as 401
        for (int i = 0; i < 7; i++) {
            info.ch4Pressure[i] = bb.getFloat(231 + i * 4);
            //log.info("CH4 pressure coef " + i + ": " + info.ch4Pressure[i]);
        }
        for (int i = 0; i < data.length - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 601.0) {
                log.info("Found коэффициент 601 at offset " + i);
            }
        }



        return info;
    }
    @Override
    public String toString() {
        return "AllCoef{\n" +
                "o2Coef=" + Arrays.toString(o2Coef) + "\n" +
                ", coCoef=" + Arrays.toString(coCoef) + "\n" +
                ", h2sCoef=" + Arrays.toString(h2sCoef) + "\n" +
                ", ch4Pressure=" + Arrays.toString(ch4Pressure) + "\n" +
                ", acceleration=" + Arrays.toString(acceleration) + "\n" +
                '}';
    }
}