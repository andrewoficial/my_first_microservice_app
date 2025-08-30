package org.example.device.protDynament;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandBuilder;
import org.example.device.command.CommandParser;
import org.example.device.command.CommandType;
import org.example.services.AnswerValues;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.function.Predicate;

public class DynamentCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(DynamentCommandRegistry.class);

    private static final byte DLE = 0x10;
    private static final byte RD = 0x13;
    private static final byte WR = 0x15;
    private static final byte ACK = 0x16;
    private static final byte NAK = 0x19;
    private static final byte DAT = 0x1A;
    private static final byte EOF = 0x1F;
    private static final byte WP1 = (byte) 0xE5;
    private static final byte WP2 = (byte) 0xA2;

    private final Map<Integer, String> statusFlags = new HashMap<>();

    public DynamentCommandRegistry() {
        initStatusFlags();
    }

    private void initStatusFlags() {
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
        commandList.addCommand(createGetConcCommand());
        commandList.addCommand(createGetVersionCommand());
        commandList.addCommand(createSetZeroCommand());
        commandList.addCommand(createSetConcCommand());
        // Removed searchBaudrateCommand since it's handled in Dynament.java
    }

    private SingleCommand createGetConcCommand() {
        byte[] baseBody = buildReadCommand(0x01); // Pre-build with checksum: 10 13 01 10 1F cs1 cs2
        SingleCommand command = new SingleCommand(
                "getConc",
                "getConc - Получить концентрацию газа",
                "getConc",
                baseBody,
                args -> baseBody, // No args, return pre-built command
                this::parseLiveDataResponse,
                27,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetVersionCommand() {
        byte[] baseBody = buildReadCommand(0x00); // 10 13 00 10 1F cs1 cs2
        SingleCommand command = new SingleCommand(
                "getVersion",
                "getVersion - Получить версию конфигурации",
                "getVersion",
                baseBody,
                args -> baseBody,
                this::parseConfigDataResponse,
                7,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetZeroCommand() {
        byte[] baseBody = buildWriteCommand(0x02, true); // 10 15 E5 A2 02 10 1F cs1 cs2
        SingleCommand command = new SingleCommand(
                "setZero",
                "setZero - Установить калибровку нуля",
                "setZero",
                baseBody,
                args -> {
                    byte[] datFrame = buildDataFrame(new byte[0]); // Empty data frame
                    return concatenate(baseBody, datFrame); // Combine WR + DAT
                },
                this::parseAckNakResponse,
                6,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetConcCommand() {
        byte[] baseBody = buildWriteCommand(0x03, true); // 10 15 E5 A2 03 10 1F cs1 cs2
        SingleCommand command = new SingleCommand(
                "setConc",
                "setConc [value] - Установить калибровку span",
                "setConc",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value);
                    byte[] valueBytes = bb.array();
                    byte[] datFrame = buildDataFrame(valueBytes); // DAT frame with float
                    return concatenate(baseBody, datFrame); // Combine WR + DAT
                },
                this::parseAckNakResponse,
                6,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> (Float) val >= 0 // Basic validation
        ));
        return command;
    }

    // Build methods (unchanged from your code)
    public byte[] buildReadCommand(int variableId) {
        byte[] frame = {DLE, RD, (byte) variableId, DLE, EOF};
        byte[] cs = calculateChecksum(frame);
        byte[] command = new byte[frame.length + cs.length];
        System.arraycopy(frame, 0, command, 0, frame.length);
        System.arraycopy(cs, 0, command, frame.length, cs.length);
        return command;
    }

    public byte[] buildWriteCommand(int variableId, boolean withPassword) {
        List<Byte> frameList = new ArrayList<>();
        frameList.add(DLE);
        frameList.add(WR);
        if (withPassword) {
            frameList.add(WP1);
            frameList.add(WP2);
        }
        frameList.add((byte) variableId);
        frameList.add(DLE);
        frameList.add(EOF);
        byte[] frame = new byte[frameList.size()];
        for (int i = 0; i < frameList.size(); i++) {
            frame[i] = frameList.get(i);
        }
        byte[] cs = calculateChecksum(frame);
        byte[] command = new byte[frame.length + cs.length];
        System.arraycopy(frame, 0, command, 0, frame.length);
        System.arraycopy(cs, 0, command, frame.length, cs.length);
        return command;
    }

    public byte[] buildDataFrame(byte[] data) {
        if (data == null) {
            data = new byte[0];
        }
        byte length = (byte) data.length;
        byte[] frame = new byte[3 + data.length + 2];
        frame[0] = DLE;
        frame[1] = DAT;
        frame[2] = length;
        System.arraycopy(data, 0, frame, 3, data.length);
        frame[3 + data.length] = DLE;
        frame[3 + data.length + 1] = EOF;
        byte[] cs = calculateChecksum(frame);
        byte[] fullFrame = new byte[frame.length + cs.length];
        System.arraycopy(frame, 0, fullFrame, 0, frame.length);
        System.arraycopy(cs, 0, fullFrame, frame.length, cs.length);
        return fullFrame;
    }

    public byte[] calculateChecksum(byte[] data) {
        int checksum = 0;
        for (byte b : data) {
            checksum += Byte.toUnsignedInt(b);
        }
        checksum &= 0xFFFF;
        return new byte[]{(byte) ((checksum >> 8) & 0xFF), (byte) (checksum & 0xFF)};
    }

    // Parse methods (unchanged from your code)
    private AnswerValues parseLiveDataResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(7); // version, status, concentration, temperature, detector_signal, reference_signal, absorbance

        if (response.length < 27) {
            log.warn("DYNAMENT: Слишком короткий ответ LiveDataResponse");
            return null;
        }

        if (response[0] != DLE || response[1] != DAT) {
            log.warn("DYNAMENT: Некорректный заголовок пакета (DLE DAT) LiveDataResponse");
            return null;
        }

        if (!validateChecksum(response)) {
            log.warn("DYNAMENT: Ошибка контрольной суммы LiveDataResponse");
            return null;
        }

        int dataLength = Byte.toUnsignedInt(response[2]);
        byte[] payload = Arrays.copyOfRange(response, 3, response.length - 4);

        if (payload.length != dataLength) {
            log.warn("DYNAMENT: Несоответствие длины данных: ожидалось {"+dataLength+"}, получено {"+payload.length+"} LiveDataResponse");
            return null;
        }

        if (payload.length < 20) {
            log.warn("DYNAMENT: Недостаточная длина данных для парсинга LiveDataResponse");
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

        return answerValues;
    }

    private AnswerValues parseConfigDataResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1); // version

        if (response.length < 7) {
            log.warn("DYNAMENT: Слишком короткий ответ для версии");
            return null;
        }

        if (!validateChecksum(response)) {
            log.warn("DYNAMENT: Ошибка контрольной суммы для версии");
            //return null;
        }

        String version = Byte.toUnsignedInt(response[3]) + "." + Byte.toUnsignedInt(response[4]);
        answerValues.addValue(Double.parseDouble(version), "Config Version");

        return answerValues;
    }

    private AnswerValues parseAckNakResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1); // success/failure

        if (response.length == 0) {
            log.warn("DYNAMENT: Пустой ответ от устройства");
            answerValues.addValue(-1.0, "Ошибка: Пустой ответ");
            return answerValues;
        }

        if (response[0] != DLE) {
            log.warn("DYNAMENT: Отсутствует DLE в начале ответа");
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

    private boolean validateChecksum(byte[] response) {
        if (response.length < 4) {
            return false;
        }
        int calculated = 0;
        for (int i = 0; i < response.length - 2; i++) {
            calculated += Byte.toUnsignedInt(response[i]);
        }
        calculated &= 0xFFFF;
        int received = (Byte.toUnsignedInt(response[response.length - 2]) << 8) | Byte.toUnsignedInt(response[response.length - 1]);
        return calculated == received;
    }

    // Helper to concatenate frames for multi-frame commands (e.g., WR + DAT)
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