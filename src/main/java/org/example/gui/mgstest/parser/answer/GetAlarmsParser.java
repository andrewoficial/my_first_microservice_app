package org.example.gui.mgstest.parser.answer;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetAlarmsModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetAlarmsParser {
    private static final Logger log = Logger.getLogger(GetAlarmsParser.class);

    public static GetAlarmsModel parse(byte[] data) {
        validateDataLength(data);

        // Проверяем CRC для payload с 27 по 42 байт (предполагая аналогичную структуру)
        if (!CrcValidator.checkCrc(data, 27, 43, 43)) {
            throw new IllegalArgumentException("CRC validation failed for GetAlarmLimits");
        }

        GetAlarmsModel settings = new GetAlarmsModel();
        settings.setLoaded(false);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 27, 16); // 16 байт payload
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            log.info("Начинаем парсинг AlarmLimits. Доступно байт: " + buffer.remaining());

            short value;

            log.info("Чтение CH4 from...");
            value = buffer.getShort();
            log.info("CH4 from: " + value);
            settings.setCh4From(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2 from...");
            value = buffer.getShort();
            log.info("O2 from: " + value);
            settings.setO2From(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO from...");
            value = buffer.getShort();
            log.info("CO from: " + value);
            settings.setCoFrom(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S from...");
            value = buffer.getShort();
            log.info("H2S from: " + value);
            settings.setH2sFrom(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4 to...");
            value = buffer.getShort();
            log.info("CH4 to: " + value);
            settings.setCh4To(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2 to...");
            value = buffer.getShort();
            log.info("O2 to: " + value);
            settings.setO2To(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO to...");
            value = buffer.getShort();
            log.info("CO to: " + value);
            settings.setCoTo(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S to...");
            value = buffer.getShort();
            log.info("H2S to: " + value);
            settings.setH2sTo(value);
            log.info("Осталось байт: " + buffer.remaining());

            settings.setLoaded(true);
            log.info("GetAlarmLimits parsed successfully");

        } catch (Exception e) {
            log.error("Error parsing GetAlarmLimits: " + e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse GetAlarmLimits: " + e.getMessage(), e);
        }

        return settings;
    }

    private static void validateDataLength(byte[] data) {
        if (data.length < 47) { // 27 + 16 + 4 = 47
            throw new IllegalArgumentException("Data too short, expected at least 47 bytes, got " + data.length);
        }
    }
}