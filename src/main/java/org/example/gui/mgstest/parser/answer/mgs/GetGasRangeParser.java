package org.example.gui.mgstest.parser.answer.mgs;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetGasRangeModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetGasRangeParser {
    private static final Logger log = Logger.getLogger(GetGasRangeParser.class);

    public static GetGasRangeModel parse(byte[] data) {
        validateDataLength(data);

        // Проверяем CRC для payload с 27 по 38 байт
        if (!CrcValidator.checkCrc(data, 27, 43, 43)) {
            throw new IllegalArgumentException("CRC validation failed for GetVRange");
        }

        GetGasRangeModel settings = new GetGasRangeModel();
        settings.setLoaded(false);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 27, 16); // 16 байт payload
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            log.info("Начинаем парсинг GetGasRange. Доступно байт: " + buffer.remaining());

            char value;
            int valInt;
            log.info("Чтение O2 from...");
            valInt = buffer.getChar();
            log.info("O2 from: " + valInt);
            settings.setO2From(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2 to...");
            valInt = buffer.getChar();
            log.info("O2 to: " + valInt);
            settings.setO2To(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO from...");
            valInt = buffer.getChar();
            log.info("CO from: " + valInt);
            settings.setCoFrom(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO to...");
            valInt = buffer.getChar();
            log.info("CO to: " + valInt);
            settings.setCoTo(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S from...");
            valInt = buffer.getChar();
            log.info("H2S from: " + valInt);
            settings.setH2sFrom(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S to...");
            valInt = buffer.getChar();
            log.info("H2S to: " + valInt);
            settings.setH2sTo(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4 from...");
            valInt = buffer.getChar();
            log.info("CH4 from: " + valInt);
            settings.setCh4From(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4 to...");
            valInt = buffer.getChar();
            log.info("CH4 to: " + valInt);
            settings.setCh4To(valInt);
            log.debug("Осталось байт: " + buffer.remaining());

            settings.setLoaded(true);
            log.info("GetVRange parsed successfully");

        } catch (Exception e) {
            log.error("Error parsing GetVRange: " + e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse GetVRange: " + e.getMessage(), e);
        }

        return settings;
    }

    private static void validateDataLength(byte[] data) {
        if (data.length < 43) { // 27 + 12 + 4 = 43
            throw new IllegalArgumentException("Data too short, expected at least 43 bytes, got " + data.length);
        }
    }
}