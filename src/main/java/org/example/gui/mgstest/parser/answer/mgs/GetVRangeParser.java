package org.example.gui.mgstest.parser.answer.mgs;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetVRangeModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetVRangeParser {
    private static final Logger log = Logger.getLogger(GetVRangeParser.class);
    
    public static GetVRangeModel parse(byte[] data) {
        validateDataLength(data);
        
        // Проверяем CRC для payload с 27 по 38 байт
        if (!CrcValidator.checkCrc(data, 27, 39, 39)) {
            throw new IllegalArgumentException("CRC validation failed for GetVRange");
        }

        GetVRangeModel settings = new GetVRangeModel();
        settings.setLoaded(false);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 27, 12); // 12 байт payload
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            log.info("Начинаем парсинг VRange. Доступно байт: " + buffer.remaining());

            short value;

            log.info("Чтение O2 from...");
            value = buffer.getShort();
            log.info("O2 from: " + value);
            settings.setO2From(value);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2 to...");
            value = buffer.getShort();
            log.info("O2 to: " + value);
            settings.setO2To(value);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO from...");
            value = buffer.getShort();
            log.info("CO from: " + value);
            settings.setCoFrom(value);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO to...");
            value = buffer.getShort();
            log.info("CO to: " + value);
            settings.setCoTo(value);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S from...");
            value = buffer.getShort();
            log.info("H2S from: " + value);
            settings.setH2sFrom(value);
            log.debug("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S to...");
            value = buffer.getShort();
            log.info("H2S to: " + value);
            settings.setH2sTo(value);
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