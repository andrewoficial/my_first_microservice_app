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

import static org.example.utilites.MyUtilities.getCubicUnits;

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
        byte[] baseBody = buildCommand(0x03, new byte[]{0x00, 0x00, 0x00}); // 00 DF1=0 DF2=0
        SingleCommand command = new SingleCommand(
                "setConc",
                "setConc [value] - Set concentration calibration",
                "setConc",
                baseBody, // Dynamic
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
        //log.info("Run calculate checksum for array " + MyUtilities.bytesToHex(data));
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
        log.info("CUBIC: version: " + version);
        double versionNum = extractNumericVersion(version.toString());

        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(versionNum, version.toString());
        return answerValues;
    }

    /**
     * Извлекает числовую версию из строки, содержащей цифры и точки
     * Примеры:
     *   "V2.1.5" -> 2.1
     *   "R1.5.1" -> 1.5
     *   "1.0" -> 1.0
     *   "Version 3.2.1" -> 3.2
     */
    private double extractNumericVersion(String versionStr) {
        if (versionStr == null || versionStr.isEmpty()) {
            return -1.0;
        }

        // Ищем последовательности цифр и точек
        StringBuilder numericPart = new StringBuilder();
        boolean foundDigit = false;
        boolean foundDot = false;

        for (char c : versionStr.toCharArray()) {
            if (Character.isDigit(c)) {
                numericPart.append(c);
                foundDigit = true;
            } else if (c == '.' && foundDigit && !foundDot) {
                // Добавляем только первую точку для мажорной и минорной версии
                numericPart.append(c);
                foundDot = true;
            } else if (foundDigit && !Character.isDigit(c) && c != '.') {
                // Прерываемся при первом нецифровом символе после начала числовой последовательности
                break;
            }
        }

        String result = numericPart.toString();

        // Убедимся, что строка не заканчивается точкой
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }

        // Разделяем на части по точке
        String[] parts = result.split("\\.");

        try {
            if (parts.length == 1) {
                // Только мажорная версия "1" -> 1.0
                return Double.parseDouble(parts[0] + ".0");
            } else if (parts.length >= 2) {
                // Мажорная и минорная версия "1.2" -> 1.2
                // Если минорная версия длинная, берем первые 2 цифры
                String minor = parts[1];
                if (minor.length() > 2) {
                    minor = minor.substring(0, 2);
                }
                return Double.parseDouble(parts[0] + "." + minor);
            }
        } catch (NumberFormatException e) {
            log.info("CUBIC: Cannot parse version number from: " + result);
        }

        return -1.0;
    }

    private AnswerValues parseSerialResponse(byte[] response) {
        // Проверяем минимальную длину и заголовки
        if (response.length < 14 || response[0] != ACK || response[1] != 0x0B || response[2] != 0x1F) {
            log.warn("CUBIC: Invalid serial response - wrong length or header");
            return null;
        }

        // Проверяем контрольную сумму
        if (!validateChecksum(response)) {
            log.warn("CUBIC: Invalid serial response - checksum error");
            return null;
        }

        StringBuilder serial = new StringBuilder();
        int[] snValues = new int[5]; // Массив для хранения всех 5 значений

        // Логируем сырые данные
        log.info("CUBIC: Raw serial data: " + MyUtilities.bytesToHex(Arrays.copyOfRange(response, 3, 13)));

        try {
            // Обрабатываем 5 двухбайтных значений
            for (int i = 0; i < 5; i++) {
                int offset = 3 + i * 2;
                int byte1 = Byte.toUnsignedInt(response[offset]);
                int byte2 = Byte.toUnsignedInt(response[offset + 1]);
                int sn = (byte1 << 8) | byte2;
                snValues[i] = sn; // Сохраняем значение в массив
                serial.append(String.format("%04d", sn));
            }

            String fullSerial = serial.toString();
            log.info("CUBIC: Parsed serial number: " + fullSerial);

            // Формируем double из последних трех чисел со знаком минус
            double numericValue = -1.0;
            if (snValues.length >= 3) {
                // Берем последние три числа и объединяем их
                // Например: [1, 2, 3, 4, 5] -> берем 3, 4, 5 -> формируем -345.0
                StringBuilder lastThreeBuilder = new StringBuilder();
                for (int i = 2; i < 5; i++) {
                    lastThreeBuilder.append(String.format("%04d", snValues[i]));
                }

                try {
                    // Парсим объединенную строку как double и делаем отрицательным
                    String combined = lastThreeBuilder.toString();
                    numericValue = -Double.parseDouble(combined);
                    log.info("CUBIC: Combined last three values: " + combined + " -> " + numericValue);
                } catch (NumberFormatException e) {
                    log.warn("CUBIC: Failed to parse combined serial number part: " + lastThreeBuilder.toString());
                }
            }

            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(numericValue, fullSerial);
            return answerValues;

        } catch (Exception e) {
            log.warn("CUBIC: Error parsing serial number: " + e.getMessage());
            return null;
        }
    }

    private AnswerValues parseGasPropertyResponse(byte[] response) {
        log.info("CUBIC: Starting GasProperty response parsing. Raw data: " + MyUtilities.bytesToHex(response));

        // Более гибкая проверка длины
        if (response.length < 11) {
            log.warn("CUBIC: GasProperty response too short. Expected at least 11 bytes, got: " + response.length);
            return null;
        }

        if (response[0] != ACK) {
            log.warn("CUBIC: Invalid ACK in GasProperty response: 0x" + Integer.toHexString(response[0] & 0xFF));
            return null;
        }

        if (response[1] != 0x08) {
            log.warn("CUBIC: Invalid length byte in GasProperty response: 0x" + Integer.toHexString(response[1] & 0xFF));
            return null;
        }

        if (response[2] != 0x0D) {
            log.warn("CUBIC: Invalid command byte in GasProperty response: 0x" + Integer.toHexString(response[2] & 0xFF));
            return null;
        }

        if (!validateChecksum(response)) {
            log.warn("CUBIC: Checksum error in GasProperty response");
            // Можно продолжить разбор, но залогировать ошибку
        }

        // Подробное логирование всех байтов
//        log.info("CUBIC: GasProperty response structure:");
//        log.info("  ACK: 0x" + Integer.toHexString(response[0] & 0xFF));
//        log.info("  LB: 0x" + Integer.toHexString(response[1] & 0xFF) + " (" + (response[1] & 0xFF) + " bytes)");
//        log.info("  CMD: 0x" + Integer.toHexString(response[2] & 0xFF));
//        log.info("  DF1: 0x" + Integer.toHexString(response[3] & 0xFF) + " (" + (response[3] & 0xFF) + ")");
//        log.info("  DF2: 0x" + Integer.toHexString(response[4] & 0xFF) + " (" + (response[4] & 0xFF) + ")");
//        log.info("  DF3 (decimals): 0x" + Integer.toHexString(response[5] & 0xFF) + " (" + (response[5] & 0xFF) + ")");
//        log.info("  DF4 (reserved): 0x" + Integer.toHexString(response[6] & 0xFF) + " (" + (response[6] & 0xFF) + ")");
//        log.info("  DF5 (unit): 0x" + Integer.toHexString(response[7] & 0xFF) + " (" + (response[7] & 0xFF) + ")");
//        log.info("  DF6 (reserved): 0x" + Integer.toHexString(response[8] & 0xFF) + " (" + (response[8] & 0xFF) + ")");
//        log.info("  DF7 (reserved): 0x" + Integer.toHexString(response[9] & 0xFF) + " (" + (response[9] & 0xFF) + ")");
//        log.info("  CS: 0x" + Integer.toHexString(response[10] & 0xFF) + " (" + (response[10] & 0xFF) + ")");

        // Извлечение данных с проверкой диапазонов
        int df1 = Byte.toUnsignedInt(response[3]);
        int df2 = Byte.toUnsignedInt(response[4]);
        int decimals = Byte.toUnsignedInt(response[5]);
        int df4 = Byte.toUnsignedInt(response[6]); // Зарезервировано, но логируем
        int unit = Byte.toUnsignedInt(response[7]);
        int df6 = Byte.toUnsignedInt(response[8]); // Зарезервировано
        int df7 = Byte.toUnsignedInt(response[9]); // Зарезервировано

        // Проверка корректности decimals
        if (decimals > 5) {
            log.warn("CUBIC: Suspicious decimals value: " + decimals + ". Limiting to 5.");
            decimals = 5;
        }

        // Вычисление диапазона с проверкой переполнения
        long rawRange = (long) df1 * 256L + (long) df2;
        if (rawRange > Integer.MAX_VALUE) {
            log.warn("CUBIC: Range value too large: " + rawRange);
        }

        double range;
        if (decimals == 0) {
            range = rawRange;
        } else {
            double divisor = Math.pow(10, decimals);
            range = rawRange / divisor;
        }

        log.info("CUBIC: Calculated range: " + range + " (raw: " + rawRange + ", decimals: " + decimals + ")");

        // Определение единиц измерения
        String unitStr = getCubicUnits(unit);
        log.info("CUBIC: Unit: " + unitStr);

        // Создание результата
        AnswerValues answerValues = new AnswerValues(3);
        answerValues.addValue(range, "Range");
        answerValues.addValue((double) decimals, "Decimals");
        answerValues.addValue((double) unit, unitStr); // Сохраняем код единицы и строку

        // Дополнительная информация для отладки
        log.info("CUBIC: GasProperty parsing completed:");
        log.info("  Range: " + range + " " + unitStr);
        log.info("  Decimals: " + decimals);
        log.info("  Unit code: " + unit + " (" + unitStr + ")");

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