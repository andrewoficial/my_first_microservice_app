package org.example.gui.mgstest.parser.answer;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.exception.WrongCrc;
import org.example.gui.mgstest.model.answer.GetAllCoefficientsModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetAllCoefficientsParser {
    private static final Logger log = Logger.getLogger(GetAllCoefficientsParser.class);

    private static void validateDataLength(byte[] data) {
        if (data.length < 310) {
            throw new IllegalArgumentException("Data too short, got " + data.length);
        }
    }

    private static void validateCrc(byte[] data) throws WrongCrc {
        if( ! CrcValidator.checkCrc(data, 27, 259, 259)){
            throw new WrongCrc(data, 27, 259, 259, "GetAllCoefficientsParser");
        }
    }

    private static void parseO2Coefficients(GetAllCoefficientsModel coef, ByteBuffer bb) {
        // O2 coefficients from offset 27..178 (19 * 8 байт) floats (101 to 119)
        for (int i = 0; i < 19; i++) {
            coef.getO2Coef()[i] = bb.getFloat(27 + i * 4);
        }
        String a = "dffs";
        a.indexOf("fdsd");
        // Debug логика
        for (int i = 0; i < bb.capacity() - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 101.0) {
                log.info("Found коэффициент 101 at offset " + i);
            }
        }
        log.info(" коэффициент 101 ожидался на позиции 27 ");
    }

    private static void parseCOCoefficients(GetAllCoefficientsModel coef, ByteBuffer bb) {
        // CO coefficients from offset 27 + 76 = 103: but per dump at 103=201; adjust to 103
        for (int i = 0; i < 14; i++) {
            coef.getCoCoef()[i] = bb.getFloat(103 + i * 4);
        }

        // Debug логика
        for (int i = 0; i < bb.capacity() - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 201.0) {
                log.info("Found коэффициент 201 at offset " + i);
            }
        }
    }

    private static void parseH2SCoefficients(GetAllCoefficientsModel coef, ByteBuffer bb) {
        // H2S coefficients from offset 103 + 56 = 159: 14 floats (301 to 314)
        for (int i = 0; i < 14; i++) {
            coef.getH2sCoef()[i] = bb.getFloat(159 + i * 4);
        }

        // Debug логика
        for (int i = 0; i < bb.capacity() - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 301.0) {
                log.info("Found коэффициент 301 at offset " + i);
            }
        }
    }

    private static void parseAccelerationCoefficients(GetAllCoefficientsModel coef, ByteBuffer bb) {
        // Acceleration from offset 159 + 56 = 215: 4 floats (501 to 504)
        for (int i = 0; i < 4; i++) {
            coef.getAcceleration()[i] = bb.getFloat(215 + i * 4);
        }

        // Debug логика
        for (int i = 0; i < bb.capacity() - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 501.0) {
                log.info("Found коэффициент 501 at offset " + i);
            }
        }
    }

    private static void parseCh4PressureCoefficients(GetAllCoefficientsModel coef, ByteBuffer bb) {
        log.info("Run search ch4Pressure");

        // Debug логика для CH4
        for (int i = 0; i < bb.capacity() - 8; i++) {
            long candidate = bb.getLong(i);
            if (candidate == 401.0) {
                log.info("Found коэффициент 401 at offset " + i + " as long");
            }
        }

        for (int i = 0; i < bb.capacity() - 8; i++) {
            int candidate = bb.getInt(i);
            if (candidate == 401) {
                log.info("Found коэффициент 401 at offset " + i + " as int");
            }
        }

        for (int i = 0; i < bb.capacity() - 8; i++) {
            short candidate = bb.getShort(i);
            short searchFor = 401;
            if (candidate == searchFor) {
                log.info("Found коэффициент 401 at offset " + i + " as short");
            }
        }

        for (int i = 0; i < bb.capacity() - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 402.0) {
                log.info("Found коэффициент 402.0 at offset " + i);
            }
        }
        log.info("Finish search ch4Pressure");

        // CH4 pressure from offset 215 + 16 = 231: 7 floats (401 to 407)
        for (int i = 0; i < 7; i++) {
            coef.getCh4Pressure()[i] = bb.getFloat(231 + i * 4);
        }
        // Особенность: первый элемент как int
        coef.getCh4Pressure()[0] = bb.getInt(231);
    }

    private static void parsePpmMgKoefs(GetAllCoefficientsModel coef, ByteBuffer bb) {
        // Debug логика для ppmMgKoefs
        for (int i = 0; i < bb.capacity() - 8; i++) {
            double candidate = bb.getFloat(i);
            if (candidate == 601.0) {
                log.info("Found коэффициент 601 at offset " + i);
            }
        }

        // TODO: Добавить парсинг для ppmMgKoefs
        // for (int i = 0; i < 4; i++) {
        //     coef.getPpmMgKoefs()[i] = bb.getFloat(XXX + i * 4);
        // }
    }

    private static void parseVRange(GetAllCoefficientsModel coef, ByteBuffer bb) {
        // TODO: Добавить парсинг для vRange
        // for (int i = 0; i < 6; i++) {
        //     coef.getVRange()[i] = bb.getFloat(XXX + i * 4);
        // }
    }

    public static GetAllCoefficientsModel parseAllCoef(byte[] coefRaw) {
        validateDataLength(coefRaw);
        validateCrc(coefRaw);
        ByteBuffer byteBuffer = ByteBuffer.wrap(coefRaw).order(ByteOrder.LITTLE_ENDIAN);
        GetAllCoefficientsModel getAllCoefficients = new GetAllCoefficientsModel();
        parseO2Coefficients(getAllCoefficients, byteBuffer);
        parseCOCoefficients(getAllCoefficients, byteBuffer);
        parseH2SCoefficients(getAllCoefficients, byteBuffer);
        parseAccelerationCoefficients(getAllCoefficients, byteBuffer);
        parseCh4PressureCoefficients(getAllCoefficients, byteBuffer);
        parsePpmMgKoefs(getAllCoefficients, byteBuffer);
        parseVRange(getAllCoefficients, byteBuffer);
        return getAllCoefficients;
    }
}