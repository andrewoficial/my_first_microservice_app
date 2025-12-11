package org.example.device.protIgm11.modbus;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

public class Igm11ModbusCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(Igm11ModbusCommandRegistry.class);

    private int slaveAddress = 1;

    private final Map<Integer, String> statusFlags = new HashMap<>();

    public void setSlaveAddress(int address) {
        this.slaveAddress = address;
    }

    public Igm11ModbusCommandRegistry() {
        initStatusFlags();
    }

    private void initStatusFlags() {
        // From register 5 bits
        statusFlags.put(0x0001, "Ошибка (Ош)");
        statusFlags.put(0x0002, "Порог 1 (П1)");
        statusFlags.put(0x0004, "Порог 2 (П2)");
        statusFlags.put(0x0008, "Диапазон (Диап)");
        statusFlags.put(0x0010, "Старт (Старт)");
        statusFlags.put(0x0020, "Блокировка (Блк)");
        statusFlags.put(0x0040, "Ток фиксирован (ТФ)");
        statusFlags.put(0x0080, "Специальный режим (СР)");
        statusFlags.put(0x0100, "Деблокировка (Деблк)");
        statusFlags.put(0x0200, "Имитация (Им)");
        statusFlags.put(0x0400, "Реле 1 (Р1)");
        statusFlags.put(0x0800, "Реле 2 (Р2)");
        statusFlags.put(0x1000, "Реле диагностики (РД)");
        statusFlags.put(0x2000, "Пониженное потребление (ПП)");
    }

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetConcCommand());
        commandList.addCommand(createGetVersionCommand());
        commandList.addCommand(createGetSerialCommand());
        commandList.addCommand(createGetGasPropertyCommand());
        commandList.addCommand(createSetZeroCommand());
        commandList.addCommand(createSetConcCommand());
        commandList.addCommand(createResetFactoryCommand());
    }

    private SingleCommand createGetConcCommand() {
        byte[] baseBody = buildReadHolding(6, 1);
        SingleCommand command = new SingleCommand(
                "getConc",
                "getConc - Get gas concentration",
                "getConc",
                baseBody,
                args -> buildReadHolding(6, 1),
                this::parseMeasurementResponse,
                7, // Addr 03 ByteCount(2) Data(2) CRC(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetVersionCommand() {
        byte[] baseBody = buildReadHolding(16, 1);
        SingleCommand command = new SingleCommand(
                "getVersion",
                "getVersion - Get software version",
                "getVersion",
                baseBody,
                args -> buildReadHolding(16, 1),
                this::parseVersionResponse,
                7,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetSerialCommand() {
        byte[] baseBody = buildReadHolding(1, 2);
        SingleCommand command = new SingleCommand(
                "getSerial",
                "getSerial - Get serial number",
                "getSerial",
                baseBody,
                args -> buildReadHolding(1, 2),
                this::parseSerialResponse,
                9, // Addr 03 04 Data(4) CRC(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetGasPropertyCommand() {
        byte[] baseBody = buildReadHolding(3, 1); // Gas type
        SingleCommand command = new SingleCommand(
                "getGasProperty",
                "getGasProperty - Get gas measurement property",
                "getGasProperty",
                baseBody,
                args -> buildReadHolding(3, 1), // For simplicity, get gas type; range separate if needed
                this::parseGasPropertyResponse,
                7,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetZeroCommand() {
        byte[] baseBody = buildWriteSingle(6, 0xAAAA);
        SingleCommand command = new SingleCommand(
                "setZero",
                "setZero - Set zero calibration",
                "setZero",
                baseBody,
                args -> buildWriteSingle(6, 0xAAAA),
                this::parseWriteResponse,
                8, // Addr 06 Reg(2) Val(2) CRC(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetConcCommand() {
        byte[] baseBody = buildWriteSingle(6, 0);
        SingleCommand command = new SingleCommand(
                "setConc",
                "setConc [value] - Set concentration calibration",
                "setConc",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    int intValue = Math.round(value * 10); // Assume *10 for %НКПР
                    return buildWriteSingle(6, intValue);
                },
                this::parseWriteResponse,
                8,
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

    private SingleCommand createResetFactoryCommand() {
        byte[] baseBody = buildWriteSingle(6, 0xBBBB);
        SingleCommand command = new SingleCommand(
                "resetFactory",
                "resetFactory - Reset to factory calibration",
                "resetFactory",
                baseBody,
                args -> buildWriteSingle(6, 0xBBBB),
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
        return command;
    }

    public byte[] buildReadHolding(int reg, int count) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x03);
        buffer.putShort((short) reg);
        buffer.putShort((short) count);
        byte[] data = buffer.array();
        byte[] crc = calculateCRC(data);
        byte[] frame = new byte[data.length + 2];
        System.arraycopy(data, 0, frame, 0, data.length);
        System.arraycopy(crc, 0, frame, data.length, 2);
        return frame;
    }

    public byte[] buildWriteSingle(int reg, int value) {
        ByteBuffer buffer = ByteBuffer.allocate(6);
        buffer.order(ByteOrder.BIG_ENDIAN);
        buffer.put((byte) slaveAddress);
        buffer.put((byte) 0x06);
        buffer.putShort((short) reg);
        buffer.putShort((short) value);
        byte[] data = buffer.array();
        byte[] crc = calculateCRC(data);
        byte[] frame = new byte[data.length + 2];
        System.arraycopy(data, 0, frame, 0, data.length);
        System.arraycopy(crc, 0, frame, data.length, 2);
        return frame;
    }

    public byte[] calculateCRC(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= Byte.toUnsignedInt(b);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort((short) crc);
        return buffer.array();
    }

    private boolean validateCRC(byte[] response) {
        if (response.length < 4) return false;
        byte[] receivedCrc = {response[response.length - 2], response[response.length - 1]};
        byte[] calculatedCrc = calculateCRC(Arrays.copyOf(response, response.length - 2));
        return Arrays.equals(receivedCrc, calculatedCrc);
    }

    private AnswerValues parseMeasurementResponse(byte[] response) {
        if (response.length < 7 || response[0] != (byte) slaveAddress || response[1] != 0x03 || response[2] != 0x02) {
            log.warn("IGM11: Invalid measurement response");
            return null;
        }
        if (!validateCRC(response)) {
            log.warn("IGM11: CRC error in measurement response");
        }
        AnswerValues answerValues = new AnswerValues(2); // conc, status

        int conc = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
        double concentration = conc / 10.0; // Assume %НКПР *10
        answerValues.addValue(concentration, "Concentration");

        // Status from another register? For now, placeholder
        String status = "OK";
        answerValues.addValue(0.0, status);

        return answerValues;
    }

    private AnswerValues parseVersionResponse(byte[] response) {
        if (response.length < 7 || response[0] != (byte) slaveAddress || response[1] != 0x03) {
            log.warn("IGM11: Invalid version response");
            return null;
        }
        if (!validateCRC(response)) {
            log.warn("IGM11: CRC error in version response");
        }
        int ver = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
        double versionNum = ver / 100.0; // e.g. 0x0102 -> 1.02
        String versionStr = String.format("%.2f", versionNum);

        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(versionNum, versionStr);
        return answerValues;
    }

    private AnswerValues parseSerialResponse(byte[] response) {
        if (response.length < 9 || response[0] != (byte) slaveAddress || response[1] != 0x03 || response[2] != 0x04) {
            log.warn("IGM11: Invalid serial response");
            return null;
        }
        if (!validateCRC(response)) {
            log.warn("IGM11: CRC error in serial response");
        }
        long serialHigh = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
        long serialLow = ((response[5] & 0xFF) << 8) | (response[6] & 0xFF);
        long serial = (serialHigh << 16) | serialLow;
        String serialStr = String.format("%08d", serial);

        double numericValue = Double.parseDouble(serialStr);

        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(numericValue, serialStr);
        return answerValues;
    }

    private AnswerValues parseGasPropertyResponse(byte[] response) {
        if (response.length < 7 || response[0] != (byte) slaveAddress || response[1] != 0x03) {
            log.warn("IGM11: Invalid gas property response");
            return null;
        }
        if (!validateCRC(response)) {
            log.warn("IGM11: CRC error in gas property response");
        }
        int gasCode = ((response[3] & 0xFF) << 8) | (response[4] & 0xFF);
        // For range, would need another call, but for now assume
        double range = 100.0; // Placeholder
        double decimals = 1.0; // Assume /10
        String unitStr = getIgm11GasName(gasCode) + " %НКПР"; // Example

        AnswerValues answerValues = new AnswerValues(3);
        answerValues.addValue(range, "Range");
        answerValues.addValue(decimals, "Decimals");
        answerValues.addValue((double) gasCode, unitStr);
        return answerValues;
    }

    private String getIgm11GasName(int code) {
        // From table D.2
        Map<Integer, String> gasMap = new HashMap<>();
        gasMap.put(1, "Метан");
        gasMap.put(2, "Пропан");
        // Add others...
        return gasMap.getOrDefault(code, "Unknown");
    }

    private AnswerValues parseWriteResponse(byte[] response) {
        if (response.length != 8 || response[0] != (byte) slaveAddress || response[1] != 0x06) {
            log.warn("IGM11: Invalid write response");
            return null;
        }
        if (!validateCRC(response)) {
            log.warn("IGM11: CRC error in write response");
        }
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(1.0, "Success");
        return answerValues;
    }

    private String parseStatusFlags(int status) {
        StringBuilder activeFlags = new StringBuilder();
        for (Map.Entry<Integer, String> entry : statusFlags.entrySet()) {
            if ((status & entry.getKey()) != 0) {
                if (activeFlags.length() > 0) {
                    activeFlags.append(", ");
                }
                activeFlags.append(entry.getValue());
            }
        }
        return activeFlags.length() > 0 ? activeFlags.toString() : "No active flags";
    }
}