package org.example.gui.mgstest.parser.answer.mgs;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetSensStatusModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetSensStatusParser {
    private static final Logger log = Logger.getLogger(GetSensStatusParser.class);

    public static GetSensStatusModel parse(byte[] data) {
        validateDataLength(data);

        // Проверяем CRC для payload с 27 по 31 байт
        if (!CrcValidator.checkCrc(data, 27, 31, 31)) {
            throw new IllegalArgumentException("CRC validation failed for GetVRange");
        }

        GetSensStatusModel settings = new GetSensStatusModel();
        settings.setLoaded(false);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 27, 4); // 16 байт payload
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            log.info("Начинаем парсинг GetSensStatus. Доступно байт: " + buffer.remaining());

            // Читаем 4 байта
            byte b0 = buffer.get();
            byte b1 = buffer.get();
            byte b2 = buffer.get();
            byte b3 = buffer.get();
            log.info("O2: " + b0);
            log.info("CO: " + b1);
            log.info("H2S: " + b2);
            log.info("CH4: " + b3);

            // Присваиваем значения полям
            settings.setO2(Boolean.parseBoolean(String.valueOf(b0 > 0)));
            settings.setCO(Boolean.parseBoolean(String.valueOf((b1 > 0))));
            settings.setH2S(Boolean.parseBoolean(String.valueOf((b2 > 0))));
            settings.setCH4(Boolean.parseBoolean(String.valueOf((b3 > 0))));

            settings.setO2_num(b0);
            settings.setCO_num(b1);
            settings.setH2S_num(b2);
            settings.setCH4_num(b3);
            log.info("O2: " + settings.isO2());
            log.info("CO: " + settings.isCO());
            log.info("H2S: " + settings.isH2S());
            log.info("CH4: " + settings.isCH4());

            log.info("O2: " + (int)settings.getO2_num());
            log.info("CO: " + (int)settings.getCO_num());
            log.info("H2S: " + (int)settings.getH2S_num());
            log.info("CH4: " + (int)settings.getCH4_num());
            settings.setLoaded(true);

            log.info("GetSensStatus parsed successfully");

        } catch (Exception e) {
            log.error("Error parsing GetSensStatus: " + e.getMessage(), e);
            throw new IllegalArgumentException("Failed to GetSensStatus: " + e.getMessage(), e);
        }

        return settings;
    }

    private static void validateDataLength(byte[] data) {
        if (data.length < 43) { // 27 + 12 + 4 = 43
            throw new IllegalArgumentException("Data too short, expected at least 43 bytes, got " + data.length);
        }
    }
}