package org.example.device.protFnirsiDps150;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.example.utilites.MyUtilities.getAnyNumber;

public class FnirsiDps150CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(FnirsiDps150CommandRegistry.class);

    // Тип команды
    private static final byte START_SEND 	= (byte) 0xF1;
    private static final byte START_RECV 	= (byte) 0xF0;
    private static final byte CMD_GET 		= (byte) 0xA1;
    private static final byte CMD_SET 		= (byte) 0xB1;
    private static final byte SPECIAL_SET 	= (byte) 0xC1;

    // Параметр команды
    public static final byte TYPE_BRIGHTNESS 	= (byte) 0xD6; // Brightness 10-14
    public static final byte TYPE_DEV_OUTPUT	= (byte) 0xDB; // Output on/off 0/1
    public static final byte TYPE_DEV_MODEL 	= (byte) 0xDE; // "DPS-150"
    public static final byte TYPE_HW_VERSION 	= (byte) 0xDF; // "V1.0"

    public static final byte TYPE_SW_VERSION 	= (byte) 0xE0; // "V1.1"
    public static final byte TYPE_DEV_STATUS 	= (byte) 0xE1; // "1/0"
    public static final byte TYPE_VOUT_LIMIT 	= (byte) 0xE2; // "5.23"
    public static final byte TYPE_IOUT_LIMIT 	= (byte) 0xE3; // "1.53"

    public static final byte TYPE_DEV_VIN 	= (byte) 0xC0; // "5.45"
    public static final byte TYPE_DEV_VOUT  = (byte) 0xC1; // "5.25"
    public static final byte TYPE_DEV_IOUT  = (byte) 0xC2; // "1.25"
    public static final byte TYPE_POWER     = (byte) 0xC3; // 12 bytes, (float MeasuredVolts; float MeasuredCurrent; float MeasuredWatts)
    public static final byte TYPE_TEMP 		= (byte) 0xC4; // Temperature ~24C

    //Специальные типы
    public static final byte TYPE_DUMP 		= (byte) 0xFF; // Big settings dump
    public static final byte TYPE_WAKE_SET	= (byte) 0x00; // Big settings dump

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetModelCommand());
        commandList.addCommand(createGetSwVersionCommand());
        commandList.addCommand(createGetHwVersionCommand());
        commandList.addCommand(createGetVinCommand());
        commandList.addCommand(createGetLimitVoutCommand());
        commandList.addCommand(createGetLimitCurrentCommand());
        commandList.addCommand(createGetPowerCommand());
        commandList.addCommand(createGetTempCommand());
        commandList.addCommand(createGetOutputCommand());
        commandList.addCommand(createGetBrightnessCommand());
        commandList.addCommand(createGetDumpCommand());
        commandList.addCommand(createSetVoutCommand());
        commandList.addCommand(createSetIoutCommand());
        commandList.addCommand(createSetOutputCommand());
        commandList.addCommand(createSetBrightnessCommand());
        commandList.addCommand(createWakeUpCommand());
    }

    private SingleCommand createWakeUpCommand() {
        byte[] baseBody = buildCommand(SPECIAL_SET, TYPE_WAKE_SET, new byte[]{(byte) 1}); // По умолчанию включение

        SingleCommand command = new SingleCommand(
                "wakeUp",
                "wakeUp [0/1] - Включ. 1 / Выключ. 0",
                "wakeUp",
                baseBody,
                args -> {
                    // Преобразуем любой числовой тип в byte
                    Object value = args.getOrDefault("value", (byte) 1);
                    byte byteValue;
                    if (value instanceof Number) {
                        byteValue = ((Number) value).byteValue();
                    } else {
                        throw new IllegalArgumentException("Значение должно быть числом (0 или 1)");
                    }
                    return buildCommand(SPECIAL_SET, TYPE_WAKE_SET, new byte[]{byteValue});
                },
                this::parseStringResponse, // Или другой парсер, если нужно
                6, // Длина зависит от вашего протокола
                CommandType.BINARY
        );

        command.addArgument(new ArgumentDescriptor(
                "value",
                Number.class,
                (byte) 1,
                val -> {
                    if (val instanceof Number) {
                        byte b = ((Number) val).byteValue();
                        return b == 0 || b == 1;
                    }
                    return false;
                }
        ));

        return command;
    }

    private SingleCommand createGetModelCommand() {
        byte[] baseBody = buildReadCommand(TYPE_DEV_MODEL);
        return new SingleCommand(
                "getModel",
                "getModel - Получить модель устройства",
                "getModel",
                baseBody,
                args -> baseBody,
                this::parseStringResponse,
                13, // len=7 + header+cs
                CommandType.BINARY
        );
    }

    private SingleCommand createGetSwVersionCommand() {
        byte[] baseBody = buildReadCommand(TYPE_SW_VERSION);
        return new SingleCommand(
                "getSwVersion",
                "getSwVersion - Получить версию ПО",
                "getSwVersion",
                baseBody,
                args -> baseBody,
                this::parseStringResponse,
                10, // len=4
                CommandType.BINARY
        );
    }

    private SingleCommand createGetHwVersionCommand() {
        byte[] baseBody = buildReadCommand(TYPE_HW_VERSION);
        return new SingleCommand(
                "getHwVersion",
                "getHwVersion - Получить версию аппаратной части",
                "getHwVersion",
                baseBody,
                args -> baseBody,
                this::parseStringResponse,
                10,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetVinCommand() {
        byte[] baseBody = buildReadCommand(TYPE_DEV_VIN);
        return new SingleCommand(
                "getVin",
                "getVin - Получить входное напряжение",
                "getVin",
                baseBody,
                args -> baseBody,
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetLimitVoutCommand() {
        byte[] baseBody = buildReadCommand(TYPE_VOUT_LIMIT);
        return new SingleCommand(
                "getLimitVout",
                "getLimitVout - Получить максимальное допустимое напряжение",
                "getLimitVout",
                baseBody,
                args -> baseBody,
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetLimitCurrentCommand() {
        byte[] baseBody = buildReadCommand(TYPE_IOUT_LIMIT);
        return new SingleCommand(
                "getLimitCurrent",
                "getLimitCurrent - Получить максимально допустимый ток",
                "getLimitCurrent",
                baseBody,
                args -> baseBody,
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetPowerCommand() {
        byte[] baseBody = buildReadCommand(TYPE_POWER);
        return new SingleCommand(
                "getPower",
                "getPower - Получить данные о мощности (12 байт)",
                "getPower",
                baseBody,
                args -> baseBody,
                this::parsePowerResponse, // Custom для 12 байт
                17, // header +12 +cs
                CommandType.BINARY
        );
    }

    private SingleCommand createGetTempCommand() {
        byte[] baseBody = buildReadCommand(TYPE_TEMP);
        return new SingleCommand(
                "getTemp",
                "getTemp - Получить температуру",
                "getTemp",
                baseBody,
                args -> baseBody,
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetOutputCommand() {
        byte[] baseBody = buildReadCommand(TYPE_DEV_OUTPUT);
        return new SingleCommand(
                "getOutput",
                "getOutput - Получить статус выхода (0/1)",
                "getOutput",
                baseBody,
                args -> baseBody,
                this::parseByteResponse,
                6,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetBrightnessCommand() {
        byte[] baseBody = buildReadCommand(TYPE_BRIGHTNESS);
        return new SingleCommand(
                "getBrightness",
                "getBrightness - Получить яркость (1-14)",
                "getBrightness",
                baseBody,
                args -> baseBody,
                this::parseByteResponse,
                6,
                CommandType.BINARY
        );
    }

    private SingleCommand createGetDumpCommand() {
        byte[] baseBody = buildReadCommand(TYPE_DUMP);
        return new SingleCommand(
                "getDump",
                "getDump - Получить дамп настроек",
                "getDump",
                baseBody,
                args -> baseBody,
                this::parseDumpResponse, // Custom для большого дампа
                144, // len=0x8B + header + cs
                CommandType.BINARY
        );
    }

    private SingleCommand createSetVoutCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_DEV_VOUT, new byte[4]);
        SingleCommand command = new SingleCommand(
                "setVout",
                "setVout [value] - Установить напряжение",
                "setVout",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
                    byte[] valueBytes = bb.array();
                    return buildWriteCommand(TYPE_DEV_VOUT, valueBytes);
                },
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> (Float) val >= 0 && (Float) val <= 30.0f // Предполагаемый max
        ));
        return command;
    }

    private SingleCommand createSetIoutCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_DEV_IOUT, new byte[4]);
        SingleCommand command = new SingleCommand(
                "setIout",
                "setIout [value] - Установить ток",
                "setIout",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
                    byte[] valueBytes = bb.array();
                    return buildWriteCommand(TYPE_DEV_IOUT, valueBytes);
                },
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> (Float) val >= 0 && (Float) val <= 5.0f // Предполагаемый max
        ));
        return command;
    }

    private SingleCommand createSetOutputCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_DEV_OUTPUT, new byte[1]);
        SingleCommand command = new SingleCommand(
                "setOutput",
                "setOutput [0/1] - Установить статус выхода",
                "setOutput",
                baseBody,
                args -> {
                    // Преобразуем любой числовой тип в byte
                    Object value = args.getOrDefault("value", (byte) 0);
                    byte byteValue;
                    if (value instanceof Number) {
                        byteValue = ((Number) value).byteValue();
                    } else {
                        throw new IllegalArgumentException("Значение выхода должно быть числом (0 или 1)");
                    }
                    byte[] valueBytes = {byteValue};
                    return buildWriteCommand(TYPE_DEV_OUTPUT, valueBytes);
                },
                this::parseByteResponse,
                6,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Number.class, // Разрешаем любой числовой тип
                (byte) 0,
                val -> {
                    if (val instanceof Number) {
                        byte b = ((Number) val).byteValue();
                        return b == 0 || b == 1;
                    }
                    return false;
                }
        ));
        return command;
    }

    private SingleCommand createSetBrightnessCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_BRIGHTNESS, new byte[1]);
        SingleCommand command = new SingleCommand(
                "setBrightness",
                "setBrightness [value] - Установить яркость (1-14)",
                "setBrightness",
                baseBody,
                args -> {
                    // Преобразуем любой числовой тип в byte
                    Object value = args.getOrDefault("value", (byte) 10);
                    byte byteValue;
                    if (value instanceof Number) {
                        byteValue = ((Number) value).byteValue();
                    } else {
                        throw new IllegalArgumentException("Яркость должна быть числом");
                    }
                    byte[] valueBytes = {byteValue};
                    return buildWriteCommand(TYPE_BRIGHTNESS, valueBytes);
                },
                this::parseByteResponse,
                6,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Number.class, // Разрешаем любой числовой тип
                (byte) 10,
                val -> {
                    if (val instanceof Number) {
                        byte b = ((Number) val).byteValue();
                        return b >= 0 && b <= 14;
                    }
                    return false;
                }
        ));
        return command;
    }

    public byte[] buildReadCommand(byte type) {
        byte[] data = new byte[0]; // Для get data часто 1 байт 0, но в логах len=01, data=00
        byte[] base = new byte[]{0x00}; // По логам get имеет len=01 data=00
        return buildCommand(CMD_GET, type, base);
    }

    public byte[] buildWriteCommand(byte type, byte[] data) {
        return buildCommand(CMD_SET, type, data);
    }

    private byte[] buildCommand(byte cmd, byte type, byte[] data) {
        byte length = (byte) (data != null ? data.length : 0);
        byte[] frameWithoutCs = new byte[4 + length];
        frameWithoutCs[0] = START_SEND;
        frameWithoutCs[1] = cmd;
        frameWithoutCs[2] = type;
        frameWithoutCs[3] = length;
        if (data != null) {
            System.arraycopy(data, 0, frameWithoutCs, 4, length);
        }
        byte cs = calculateChecksum(frameWithoutCs);
        byte[] frame = new byte[frameWithoutCs.length + 1];
        System.arraycopy(frameWithoutCs, 0, frame, 0, frameWithoutCs.length);
        frame[frameWithoutCs.length] = cs;
        log.info("Built command: " + MyUtilities.bytesToHex(frame));
        return frame;
    }

    public byte calculateChecksum(byte[] data) {
        int sum = 0;
        for (int i = 2; i < data.length; i++) { // От type (2) до конца data
            sum += (data[i] & 0xFF);
        }
        return (byte) (sum % 256);
    }

    public boolean validateChecksum(byte[] response) {
        if (response.length < 5) {
            return false;
        }
        byte receivedCs = response[response.length - 1];
        byte calculatedCs = calculateChecksum(response); // Теперь calc от 0, но sum от 2
        int sum = 0;
        for (int i = 2; i < response.length - 1; i++) {
            sum += (response[i] & 0xFF);
        }
        calculatedCs = (byte) (sum % 256);
        return receivedCs == calculatedCs;
    }

    private AnswerValues parseFloatResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);
        if (response.length != 9) {
            log.warn("FNIRSI_DPS150: Wrong response length for float: " + response.length);
            return null;
        }
        if (response[0] != START_RECV || response[1] != CMD_GET) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("FNIRSI_DPS150: Checksum error in response");
            return null;
        }
        byte length = response[3];
        if (length != 4) {
            log.warn("FNIRSI_DPS150: Unexpected data length for float: " + length);
            return null;
        }
        byte[] data = Arrays.copyOfRange(response, 4, 8);
        float value = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        answerValues.addValue(value, "value");
        return answerValues;
    }

    private AnswerValues parseByteResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);
        if (response.length != 6) {
            log.warn("FNIRSI_DPS150: Wrong response length for byte: " + response.length);
            return null;
        }
        if (response[0] != START_RECV || response[1] != CMD_GET) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("FNIRSI_DPS150: Checksum error in response");
            return null;
        }
        byte length = response[3];
        if (length != 1) {
            log.warn("FNIRSI_DPS150: Unexpected data length for byte: " + length);
            return null;
        }
        byte value = response[4];
        answerValues.addValue(value, "status");
        return answerValues;
    }

    private AnswerValues parseStringResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);
        if (response.length < 6) {
            log.warn("FNIRSI_DPS150: Wrong response length for string");
            return null;
        }
        if (response[0] != START_RECV || response[1] != CMD_GET) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("FNIRSI_DPS150: Checksum error in response");
            return null;
        }
        byte length = response[3];
        if (length == 0) {
            log.warn("FNIRSI_DPS150: Zero data length for string");
            return null;
        }
        byte[] data = Arrays.copyOfRange(response, 4, 4 + length);
        String value;
        try {
            value = new String(data, StandardCharsets.US_ASCII);
        } catch (Exception e) {
            log.warn("FNIRSI_DPS150: Error decoding string: " + e.getMessage());
            return null;
        }
        Double number = getAnyNumber(value);
        answerValues.addValue(number, value);
        return answerValues;
    }

    private AnswerValues parsePowerResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(3); // Предполагаем 3 float в 12 байтах
        //ToDo recheck it!!!!!
        if (response.length != 17) {
            log.warn("FNIRSI_DPS150: Wrong response length for power: " + response.length);
            return null;
        }
        if (response[0] != START_RECV || response[1] != CMD_GET) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("FNIRSI_DPS150: Checksum error in response");
            return null;
        }
        byte length = response[3];
        if (length != 12) {
            log.warn("FNIRSI_DPS150: Unexpected data length for power: " + length);
            return null;
        }
        byte[] data = Arrays.copyOfRange(response, 4, 16);
        // Разбить на 3 float?
        float val1 = ByteBuffer.wrap(Arrays.copyOfRange(data, 0, 4)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float val2 = ByteBuffer.wrap(Arrays.copyOfRange(data, 4, 8)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        float val3 = ByteBuffer.wrap(Arrays.copyOfRange(data, 8, 12)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        answerValues.addValue(val1, "power1");
        answerValues.addValue(val2, "power2");
        answerValues.addValue(val3, "power3");
        return answerValues;
    }

    private AnswerValues parseDumpResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);
        if (response.length != 144) {
            log.warn("FNIRSI_DPS150: Wrong response length for dump: " + response.length);
            return null;
        }
        if (response[0] != START_RECV || response[1] != CMD_GET) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("FNIRSI_DPS150: Checksum error in response");
            return null;
        }
        byte length = response[3];
        if (length != (byte)0x8B) {
            log.warn("FNIRSI_DPS150: Unexpected data length for dump: " + length);
            return null;
        }
        byte[] data = Arrays.copyOfRange(response, 4, 4 + length);
        // Возвращаем как hex string или парсим, если известно
        String hexDump = MyUtilities.bytesToHex(data);
        answerValues.addValue(-1, hexDump);
        return answerValues;
    }
}