package org.example.device.protCubic;

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

public class CubicCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(CubicCommandRegistry.class);

    private static final byte IP = 0x11;
    private static final byte ACK = 0x16;
    private static final byte NAK = 0x06;

    private final Map<Integer, String> statusFlags = new HashMap<>();

    public CubicCommandRegistry() {
        initStatusFlags();
    }

    private void initStatusFlags() {
        statusFlags.put(0x01, "Warming-up");
        statusFlags.put(0x02, "Malfunction");
        statusFlags.put(0x04, "Out of range");
        // 0x08 reserved
        statusFlags.put(0x10, "No calibration");
        statusFlags.put(0x20, "High humidity");
        statusFlags.put(0x40, "Reference channel over limit");
        statusFlags.put(0x80, "Measurement channel over limit");
    }

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetConcCommand());
        commandList.addCommand(createGetVersionCommand());
        commandList.addCommand(createSetZeroCommand());
        commandList.addCommand(createSetConcCommand());
        commandList.addCommand(createGetSerialCommand());
        commandList.addCommand(createGetGasPropertyCommand());
        commandList.addCommand(createResetFactoryCommand());
    }

    private SingleCommand createGetConcCommand() {
        byte[] baseBody = buildCommand(0x01, new byte[0]);
        SingleCommand command = new SingleCommand(
                "getConc",
                "getConc - Get gas concentration",
                "getConc",
                baseBody,
                args -> baseBody, // No args
                this::parseMeasurementResponse,
                9, // ACK LB CMD DF1 DF2 ST1 ST2 CS, LB=0x05, total ~9
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetVersionCommand() {
        byte[] baseBody = buildCommand(0x1E, new byte[0]);
        SingleCommand command = new SingleCommand(
                "getVersion",
                "getVersion - Get software version",
                "getVersion",
                baseBody,
                args -> baseBody,
                this::parseVersionResponse,
                20, // Variable length
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetSerialCommand() {
        byte[] baseBody = buildCommand(0x1F, new byte[0]);
        SingleCommand command = new SingleCommand(
                "getSerial",
                "getSerial - Get serial number",
                "getSerial",
                baseBody,
                args -> baseBody,
                this::parseSerialResponse,
                14, // ACK 0B 1F SN1H SN1L ... SN5H SN5L CS
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetGasPropertyCommand() {
        byte[] baseBody = buildCommand(0x0D, new byte[0]);
        SingleCommand command = new SingleCommand(
                "getGasProperty",
                "getGasProperty - Get gas measurement property",
                "getGasProperty",
                baseBody,
                args -> baseBody,
                this::parseGasPropertyResponse,
                11, // ACK 08 0D DF1..DF7 CS
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetZeroCommand() {
        byte[] baseBody = buildCommand(0x4B, new byte[]{0x00, 0x00, 0x00}); // 00 DF1=0 DF2=0
        SingleCommand command = new SingleCommand(
                "setZero",
                "setZero - Set zero calibration",
                "setZero",
                baseBody,
                args -> buildCommand(0x4B, new byte[]{0x00, 0x00, 0x00}),
                this::parseAckNakResponse,
                5, // ACK 01 4B CS
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetConcCommand() {
        SingleCommand command = new SingleCommand(
                "setConc",
                "setConc [value] - Set concentration calibration",
                "setConc",
                null, // Dynamic
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    // Assume n=0 for ppm, adjust if needed. User should know resolution.
                    // For simplicity, assume value is in base units, e.g., ppm
                    int intValue = Math.round(value);
                    byte df1 = (byte) ((intValue >> 8) & 0xFF);
                    byte df2 = (byte) (intValue & 0xFF);
                    return buildCommand(0x03, new byte[]{df1, df2});
                },
                this::parseAckNakResponse,
                5, // ACK 01 03 CS
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
        byte[] baseBody = buildCommand(0x4D, new byte[]{0x00});
        SingleCommand command = new SingleCommand(
                "resetFactory",
                "resetFactory - Reset to factory calibration",
                "resetFactory",
                baseBody,
                args -> baseBody,
                this::parseAckNakResponse,
                5, // ACK 01 4D CS
                CommandType.BINARY
        );
        return command;
    }

    public byte[] buildCommand(int cmdCode, byte[] df) {
        int lb = 1 + df.length; // CMD + DF
        byte[] frame = new byte[2 + lb + 1]; // IP LB CMD DF CS
        frame[0] = IP;
        frame[1] = (byte) lb;
        frame[2] = (byte) cmdCode;
        System.arraycopy(df, 0, frame, 3, df.length);
        frame[frame.length - 1] = calculateChecksum(Arrays.copyOf(frame, frame.length - 1));
        return frame;
    }

    public byte calculateChecksum(byte[] data) {
        log.info("Run calculate checksum for array " + MyUtilities.bytesToHex(data));
        int sum = 0;
        for (byte b : data) {
            sum += Byte.toUnsignedInt(b);
        }
        return (byte) ((0x100 - (sum & 0xFF)) & 0xFF);
    }

    private boolean validateChecksum(byte[] response) {

        if (response.length < 2) {
            log.info("response.length < 2. Actual length:" + response.length);
            return false;
        }
        byte receivedCs = response[response.length - 1];
        byte calculatedCs = calculateChecksum(Arrays.copyOf(response, response.length - 1));
        log.info("receivedCs:" + receivedCs + " calculatedCs:" + calculatedCs);
        return receivedCs == calculatedCs;
    }

    private AnswerValues parseMeasurementResponse(byte[] response) {
        if (response.length < 8 || response[0] != ACK || response[1] != 0x05 || response[2] != 0x01) {
            log.warn("CUBIC: Invalid measurement response");
            if(response.length <  8)
                log.info("response.length < 8" + response.length);

            if(response[0] != ACK)
                log.info("response[0] != ACK");

            if(response[1] != 0x05)
                log.info("response[1] != 0x05");

            if(response[2] != 0x01)
                log.info("response[2] != 0x01");

            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("CUBIC: Checksum error in measurement response");
            // return null;
        }
        AnswerValues answerValues = new AnswerValues(2); // conc, status

        int df1 = Byte.toUnsignedInt(response[3]);
        int df2 = Byte.toUnsignedInt(response[4]);
        double concentration = (df1 * 256 + df2); // / 10^n, but n unknown, assume n=0 for now or query separately
        // For example, if ppm n=0, if % n=2
        // User can adjust in code or add param
        answerValues.addValue(concentration, "Concentration");

        byte st1 = response[5];
        // byte st2 = response[6]; reserved
        String status = parseStatusFlags(st1);
        answerValues.addValue(0.0, status);

        return answerValues;
    }

    private AnswerValues parseVersionResponse(byte[] response) {
        if (response[0] != ACK || response[2] != 0x1E || !validateChecksum(response)) {
            log.warn("CUBIC: Invalid version response");
            return null;
        }
        int lb = Byte.toUnsignedInt(response[1]);
        if (response.length != lb + 3) return null; // ACK LB ... CS
        StringBuilder version = new StringBuilder();
        for (int i = 3; i < response.length - 1; i++) {
            version.append((char) response[i]);
        }
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(0.0, version.toString());
        return answerValues;
    }

    private AnswerValues parseSerialResponse(byte[] response) {
        if (response.length < 14 || response[0] != ACK || response[1] != 0x0B || response[2] != 0x1F || !validateChecksum(response)) {
            log.warn("CUBIC: Invalid serial response");
            return null;
        }
        StringBuilder serial = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int sn = ByteBuffer.wrap(new byte[]{response[3 + i*2], response[4 + i*2]}).order(ByteOrder.BIG_ENDIAN).getShort();
            serial.append(String.format("%04d", sn));
        }
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(0.0, serial.toString());
        return answerValues;
    }

    private AnswerValues parseGasPropertyResponse(byte[] response) {
        if (response.length < 11 || response[0] != ACK || response[1] != 0x08 || response[2] != 0x0D || !validateChecksum(response)) {
            log.warn("CUBIC: Invalid gas property response");
            return null;
        }
        int df1 = Byte.toUnsignedInt(response[3]);
        int df2 = Byte.toUnsignedInt(response[4]);
        int decimals = Byte.toUnsignedInt(response[5]);
        // df4 reserved
        int unit = Byte.toUnsignedInt(response[7]); // 0 ppm, 2 vol%
        // df6 df7 reserved
        double range = (df1 * 256 + df2) / Math.pow(10, decimals);
        String unitStr = unit == 0 ? "ppm" : (unit == 2 ? "vol%" : "unknown");
        AnswerValues answerValues = new AnswerValues(3);
        answerValues.addValue(range, "Range");
        answerValues.addValue((double) decimals, "Decimals");
        answerValues.addValue(0.0, unitStr);
        return answerValues;
    }

    private AnswerValues parseAckNakResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);

        if (response.length == 0) {
            log.warn("CUBIC: Empty response");
            answerValues.addValue(-1.0, "Error: Empty response");
            return answerValues;
        }

        byte responseType = response[0];
        if (responseType == ACK) {
            if (!validateChecksum(response)) {
                log.warn("CUBIC: Checksum error in ACK");
            }
            answerValues.addValue(1.0, "Success");
        } else if (responseType == NAK) {
            if (response.length < 5 || response[1] != 0x02) {
                log.warn("CUBIC: Invalid NAK response");
            }
            int errorCode = Byte.toUnsignedInt(response[3]);
            answerValues.addValue(-1.0, "Error: NAK, code " + errorCode);
        } else {
            answerValues.addValue(-1.0, "Error: Unexpected response type 0x" + Integer.toHexString(Byte.toUnsignedInt(responseType)));
        }

        return answerValues;
    }

    private String parseStatusFlags(byte statusByte) {
        int status = Byte.toUnsignedInt(statusByte);
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