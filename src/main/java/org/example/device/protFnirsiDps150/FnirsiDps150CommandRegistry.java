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
import java.util.Arrays;
import java.util.Map;

public class FnirsiDps150CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(FnirsiDps150CommandRegistry.class);

    private static final byte START_SEND = (byte) 0xF1;
    private static final byte START_RECV = (byte) 0xF0;
    private static final byte CMD_GET = (byte) 0xA1;
    private static final byte CMD_SET = (byte) 0xB1;

    // Speculative type codes; these are assumed based on typical power supply parameters.
    // You may need to reverse-engineer or adjust based on actual device behavior.
    public static final byte TYPE_OUTPUT = (byte) 0x00; // Output on/off (byte: 0/1)
    public static final byte TYPE_VOUT_SET = (byte) 0x01; // Set voltage (float)
    public static final byte TYPE_IOUT_SET = (byte) 0x02; // Set current (float)
    public static final byte TYPE_VOUT_MEAS = (byte) 0x03; // Measured voltage (float)
    public static final byte TYPE_IOUT_MEAS = (byte) 0x04; // Measured current (float)
    public static final byte TYPE_POWER = (byte) 0x05; // Power (float)
    public static final byte TYPE_TEMP = (byte) 0x06; // Temperature (float)

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetVoutCommand());
        commandList.addCommand(createGetIoutCommand());
        commandList.addCommand(createGetPowerCommand());
        commandList.addCommand(createGetTempCommand());
        commandList.addCommand(createGetOutputCommand());
        commandList.addCommand(createSetVoutCommand());
        commandList.addCommand(createSetIoutCommand());
        commandList.addCommand(createSetOutputCommand());
        // Add more commands as needed, e.g., for protection settings if discovered
    }

    private SingleCommand createGetVoutCommand() {
        byte[] baseBody = buildReadCommand(TYPE_VOUT_MEAS);
        return new SingleCommand(
                "getVout",
                "getVout - Получить измеренное напряжение",
                "getVout",
                baseBody,
                args -> baseBody,
                this::parseFloatResponse,
                9, // Expected response length for float (start+cmd+type+len+data4+cs = 9)
                CommandType.BINARY
        );
    }

    private SingleCommand createGetIoutCommand() {
        byte[] baseBody = buildReadCommand(TYPE_IOUT_MEAS);
        return new SingleCommand(
                "getIout",
                "getIout - Получить измеренный ток",
                "getIout",
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
                "getPower - Получить мощность",
                "getPower",
                baseBody,
                args -> baseBody,
                this::parseFloatResponse,
                9,
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
        byte[] baseBody = buildReadCommand(TYPE_OUTPUT);
        return new SingleCommand(
                "getOutput",
                "getOutput - Получить статус выхода (on/off)",
                "getOutput",
                baseBody,
                args -> baseBody,
                this::parseByteResponse,
                6, // Expected for byte (start+cmd+type+len+data1+cs = 6)
                CommandType.BINARY
        );
    }

    private SingleCommand createSetVoutCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_VOUT_SET, new byte[4]); // Placeholder
        SingleCommand command = new SingleCommand(
                "setVout",
                "setVout [value] - Установить напряжение",
                "setVout",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
                    byte[] valueBytes = bb.array();
                    return buildWriteCommand(TYPE_VOUT_SET, valueBytes);
                },
                this::parseFloatResponse, // Assume response returns the set value
                9,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> (Float) val >= 0 && (Float) val <= 30.0f // Assumed max 30V
        ));
        return command;
    }

    private SingleCommand createSetIoutCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_IOUT_SET, new byte[4]); // Placeholder
        SingleCommand command = new SingleCommand(
                "setIout",
                "setIout [value] - Установить ток",
                "setIout",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
                    byte[] valueBytes = bb.array();
                    return buildWriteCommand(TYPE_IOUT_SET, valueBytes);
                },
                this::parseFloatResponse,
                9,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> (Float) val >= 0 && (Float) val <= 5.0f // Assumed max 5A
        ));
        return command;
    }

    private SingleCommand createSetOutputCommand() {
        byte[] baseBody = buildWriteCommand(TYPE_OUTPUT, new byte[1]); // Placeholder
        SingleCommand command = new SingleCommand(
                "setOutput",
                "setOutput [0/1] - Установить статус выхода (off/on)",
                "setOutput",
                baseBody,
                args -> {
                    Byte value = (Byte) args.getOrDefault("value", (byte) 0);
                    byte[] valueBytes = {value};
                    return buildWriteCommand(TYPE_OUTPUT, valueBytes);
                },
                this::parseByteResponse,
                6,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Byte.class,
                (byte) 0,
                val -> ((Byte) val == 0 || (Byte) val == 1)
        ));
        return command;
    }

    public byte[] buildReadCommand(byte type) {
        byte[] data = new byte[0];
        return buildCommand(CMD_GET, type, data);
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
        byte cs = calculateChecksum(frameWithoutCs, 2); // Start checksum from byte 2
        byte[] frame = new byte[frameWithoutCs.length + 1];
        System.arraycopy(frameWithoutCs, 0, frame, 0, frameWithoutCs.length);
        frame[frameWithoutCs.length] = cs;
        log.info("Built command: " + MyUtilities.bytesToHex(frame));
        return frame;
    }

    public byte calculateChecksum(byte[] data, int startIndex) {
        int sum = 0;
        for (int i = startIndex; i < data.length; i++) {
            sum += (data[i] & 0xFF);
        }
        return (byte) (sum % 256);
    }

    public boolean validateChecksum(byte[] response, boolean isRecv) {
        if (response.length < 5) {
            return false;
        }
        byte receivedCs = response[response.length - 1];
        byte calculatedCs = calculateChecksum(response, 2); // Checksum over bytes 2 to length-2
        return receivedCs == calculatedCs;
    }

    private AnswerValues parseFloatResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);
        if (response.length != 9) {
            log.warn("FNIRSI_DPS150: Wrong response length for float: " + response.length);
            return null;
        }
        if (response[0] != START_RECV || (response[1] != CMD_GET && response[1] != CMD_SET)) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response, true)) {
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
        if (response[0] != START_RECV || (response[1] != CMD_GET && response[1] != CMD_SET)) {
            log.warn("FNIRSI_DPS150: Invalid header in response");
            return null;
        }
        if (!validateChecksum(response, true)) {
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
}