package org.example.device.protBelead;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class BeLeadCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(BeLeadCommandRegistry.class);

    private static final byte START = (byte) 0xA5;
    private static final byte RD = 0x13;
    private static final byte WR = 0x15;
    private static final byte ACK = 0x16;
    private static final byte NAK = 0x19;
    private static final byte DAT = 0x1A;
    private static final byte DLE = 0x10;
    private static final byte EOF = 0x1F;

    private final Map<Integer, String> statusFlags = new HashMap<>();

    public BeLeadCommandRegistry() {
        initStatusFlags();
    }

    private void initStatusFlags() {
        // Assuming similar flags as Dynament; adjust if specific flags are known for BeLead
        statusFlags.put(0x0001, "Signal Timeout");
        statusFlags.put(0x0004, "Signal Noise");
        statusFlags.put(0x0040, "Detector Low");
        statusFlags.put(0x0080, "Reference Low");
        statusFlags.put(0x0800, "Voltage Monitor Error");
        statusFlags.put(0x1000, "Config Checksum Error");
        statusFlags.put(0x2000, "Private Checksum Error");
        statusFlags.put(0x4000, "User EEPROM Checksum Error");
        statusFlags.put(0x8000, "Program Checksum Error");
    }

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetAllDataCommand());
        commandList.addCommand(createGetSimpleDataCommand());
        commandList.addCommand(createSetZeroCommand());
        commandList.addCommand(createSetConcCommand());
    }

    private SingleCommand createGetAllDataCommand() {
        byte[] baseBody = buildReadCommand((byte) 0x01, null); // Исправлено: 0x01 вместо 0x13
        SingleCommand command = new SingleCommand(
                "getAllData",
                "getAllData - Получить все измеренные данные",
                "getAllData",
                baseBody,
                args -> baseBody, // No args
                this::parseDataResponse,
                39, // Исправлено: 39 байт для ответа со всеми данными
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetSimpleDataCommand() {
        byte[] baseBody = buildReadCommand((byte) 0x06, null);
        SingleCommand command = new SingleCommand(
                "getSimpleData",
                "getSimpleData - Получить упрощенные измеренные данные",
                "getSimpleData",
                baseBody,
                args -> baseBody, // No args
                this::parseSimpleDataResponse,
                23, // Исправлено: 23 байта для ответа с простыми данными
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetZeroCommand() {
        byte[] baseBody = buildWriteCommand((byte) 0x02, null);
        SingleCommand command = new SingleCommand(
                "setZero",
                "setZero - Установить калибровку нуля",
                "setZero",
                baseBody,
                args -> baseBody,
                this::parseAckNakResponse,
                6,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetConcCommand() {
        byte[] baseBody = buildWriteCommand((byte) 0x03, null);
        SingleCommand command = new SingleCommand(
                "setConc",
                "setConc [value] - Установить калибровку span",
                "setConc",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
                    byte[] valueBytes = bb.array();

                    // Преобразуем 4 байта в 8 байтов (каждый байт разбивается на два)
                    byte[] expandedValue = new byte[8];
                    for (int i = 0; i < 4; i++) {
                        expandedValue[i * 2] = (byte) ((valueBytes[i] >> 4) & 0x0F);
                        expandedValue[i * 2 + 1] = (byte) (valueBytes[i] & 0x0F);
                    }

                    return buildWriteCommand((byte) 0x03, expandedValue);
                },
                this::parseAckNakResponse,
                6,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> (Float) val >= 0
        ));
        return command;
    }

    public byte[] buildReadCommand(byte variableId, byte[] data) {
        log.info("Return buildReadCommand body " + MyUtilities.bytesToHex(buildCommand(RD, variableId, data, true)));
        return buildCommand(RD, variableId, data, true);
    }

    public byte[] buildWriteCommand(byte variableId, byte[] data) {
        log.info("Return buildWriteCommand body " + MyUtilities.bytesToHex(buildCommand(WR, variableId, data, true)));
        return buildCommand(WR, variableId, data, true);
    }

    private byte[] buildCommand(byte cmd, byte variableId, byte[] data, boolean isHostSend) {
        byte[] payload = new byte[9];
        payload[0] = variableId;
        if (data != null) {
            if (data.length > 8) {
                throw new IllegalArgumentException("Data exceeds 8 bytes for payload");
            }
            System.arraycopy(data, 0, payload, 1, data.length);
        }
        byte[] frame = new byte[13];
        frame[0] = START;
        frame[1] = cmd;
        System.arraycopy(payload, 0, frame, 2, 9);
        frame[11] = DLE;
        frame[12] = EOF;
        byte[] cs = calculateChecksum(frame, isHostSend);
        log.info("Return buildCommand body " + MyUtilities.bytesToHex(concatenate(frame, cs)));
        return concatenate(frame, cs);
    }

    private byte[] calculateChecksum(byte[] data, boolean isHostSend) {
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xFF;
        }
        if (isHostSend) {
            // Для отправки: представляем 16-битную сумму как 4 байта (каждый полубайт становится байтом)
            byte[] checksumBytes = new byte[4];
            checksumBytes[0] = (byte) ((sum >> 12) & 0x0F);
            checksumBytes[1] = (byte) ((sum >> 8) & 0x0F);
            checksumBytes[2] = (byte) ((sum >> 4) & 0x0F);
            checksumBytes[3] = (byte) (sum & 0x0F);
            return checksumBytes;
        } else {
            // Для приема: возвращаем 2 байта (старший и младший) в big-endian порядке
            return new byte[] { (byte) (sum >> 8), (byte) (sum & 0xFF) };
        }
    }

    private AnswerValues parseSimpleDataResponse(byte[] response) {
            AnswerValues answerValues = new AnswerValues(4); // concentration, temperature, humidity, absorbance

            if (response.length < 23) {
                log.warn("BELEAD: Слишком короткий ответ для SimpleDataResponse. Длина: " + response.length);
                return null;
            }

            if (response[0] != START || response[1] != DAT) {
                log.warn("BELEAD: Некорректный заголовок пакета (START DAT) в SimpleDataResponse");
                return null;
            }

            if (!validateChecksum(response, false)) {
                log.warn("BELEAD: Ошибка контрольной суммы в SimpleDataResponse");
            }

            int dataLength = Byte.toUnsignedInt(response[2]);
            if (dataLength != 16) {
                log.warn("BELEAD: Неожиданная длина данных в SimpleDataResponse: " + dataLength);
                return null;
            }

            // Извлекаем 16 байт данных (4 float значения)
            byte[] data = Arrays.copyOfRange(response, 3, 3 + dataLength);

            // Парсим концентрацию (байты 0-3)
            float concentration = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4))
                    .order(ByteOrder.LITTLE_ENDIAN).getFloat();
            answerValues.addValue(concentration, "Концентрация (ppm)");

            // Парсим температуру (байты 4-7)
            float temperature = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8))
                    .order(ByteOrder.LITTLE_ENDIAN).getFloat();
            answerValues.addValue(temperature, "Температура (°C)");

            // Парсим влажность (байты 8-11)
            float humidity = ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 12))
                    .order(ByteOrder.LITTLE_ENDIAN).getFloat();
            answerValues.addValue(humidity, "Влажность (% RH)");

            // Парсим поглощение (байты 12-15)
            float absorbance = ByteBuffer.wrap(Arrays.copyOfRange(data, 12, 16))
                    .order(ByteOrder.LITTLE_ENDIAN).getFloat();
            answerValues.addValue(absorbance, "Поглощение");

            return answerValues;
    }

    private AnswerValues parseDataResponse(byte[] response) {
        // Assuming similar structure to Dynament; adjust based on actual Table 1 if available
        AnswerValues answerValues = new AnswerValues(7); // version, status, concentration, temperature, detector_signal, reference_signal, absorbance

        if (response.length < 27) {
            log.warn("BELEAD: Слишком короткий ответ DataResponse");
            return null;
        }

        if (response[0] != START || response[1] != DAT) {
            log.warn("BELEAD: Некорректный заголовок пакета (START DAT) DataResponse");
            return null;
        }

        if (!validateChecksum(response, false)) {
            log.warn("BELEAD: Ошибка контрольной суммы DataResponse");
        }

        // Assuming no length byte, payload starts at index 2, length variable
        // But to match Dynament, assume length at [2], payload from 3
        int dataLength = Byte.toUnsignedInt(response[2]);
        byte[] payload = Arrays.copyOfRange(response, 3, 3 + dataLength);

        if (payload.length < dataLength) {
            log.warn("BELEAD: Несоответствие длины данных");
            return null;
        }

        if (payload.length < 20) {
            log.warn("BELEAD: Недостаточная длина данных для парсинга DataResponse");
            return null;
        }

        String version = Byte.toUnsignedInt(payload[0]) + "." + Byte.toUnsignedInt(payload[1]);
        answerValues.addValue(Double.parseDouble(version), "Версия прошивки");

        byte[] statusBytes = {payload[2], payload[3]};
        String status = parseStatusFlags(statusBytes);
        answerValues.addValue(0.0, status);

        float concentration = ByteBuffer.wrap(Arrays.copyOfRange(payload, 4, 8)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        answerValues.addValue(concentration, "Концентрация (ppm)");

        float temperature = ByteBuffer.wrap(Arrays.copyOfRange(payload, 8, 12)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        answerValues.addValue(temperature, "Температура");

        int detectorSignal = ByteBuffer.wrap(Arrays.copyOfRange(payload, 12, 14)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        answerValues.addValue(detectorSignal, "Detector Signal");

        int referenceSignal = ByteBuffer.wrap(Arrays.copyOfRange(payload, 14, 16)).order(ByteOrder.LITTLE_ENDIAN).getShort() & 0xFFFF;
        answerValues.addValue(referenceSignal, "Reference Signal");

        float absorbance = ByteBuffer.wrap(Arrays.copyOfRange(payload, 16, 20)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        answerValues.addValue(absorbance, "Absorbance");

        // If humidity is included, add here if known

        return answerValues;
    }

    private AnswerValues parseAckNakResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1); // success/failure

        if (response.length == 0) {
            log.warn("BELEAD: Пустой ответ от устройства");
            answerValues.addValue(-1.0, "Ошибка: Пустой ответ");
            return answerValues;
        }

        if (response[0] != START) {
            log.warn("BELEAD: Отсутствует START в начале ответа");
            answerValues.addValue(-1.0, "Ошибка: Некорректный заголовок");
            return answerValues;
        }

        byte responseType = response[1];
        if (responseType == ACK) {
            answerValues.addValue(1.0, "Успех");
        } else if (responseType == NAK) {
            int errorCode = (response.length > 2) ? Byte.toUnsignedInt(response[2]) : -1;
            answerValues.addValue(-1.0, "Ошибка: NAK, код " + errorCode);
        } else {
            answerValues.addValue(-1.0, "Ошибка: Неожиданный тип ответа 0x" + Integer.toHexString(Byte.toUnsignedInt(responseType)));
        }

        return answerValues;
    }

    private String parseStatusFlags(byte[] statusBytes) {
        int statusWord = ((Byte.toUnsignedInt(statusBytes[0]) << 8) | Byte.toUnsignedInt(statusBytes[1]));
        StringBuilder activeFlags = new StringBuilder();
        for (Map.Entry<Integer, String> entry : statusFlags.entrySet()) {
            if ((statusWord & entry.getKey()) != 0) {
                if (activeFlags.length() > 0) {
                    activeFlags.append(", ");
                }
                activeFlags.append(entry.getValue());
            }
        }
        return activeFlags.length() > 0 ? activeFlags.toString() : "No active flags";
    }

    private boolean validateChecksum(byte[] response, boolean isHostSend) {
        int checkLength = isHostSend ? 4 : 2;
        if (response.length < checkLength) {
            log.info("Для проверки передана неполная посылка. Ожидалось не менее " + checkLength + " принято " + response.length);
            return false;
        }
        long calculated = 0;
        for (int i = 0; i < response.length - checkLength; i++) {
            calculated += Byte.toUnsignedLong(response[i]);
        }
        long received = 0;
        for (int i = 0; i < checkLength; i++) {
            received = (received << 8) | Byte.toUnsignedLong(response[response.length - checkLength + i]);
        }
        if (isHostSend) {
            calculated &= 0xFFFFFFFFL;
        } else {
            calculated &= 0xFFFF;
            received &= 0xFFFF;
        }
        log.info("received crc: " + received + " calculated crc: " + calculated);
        return calculated == received;
    }

    private byte[] concatenate(byte[]... arrays) {
        int totalLength = Arrays.stream(arrays).mapToInt(arr -> arr.length).sum();
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] array : arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }
}