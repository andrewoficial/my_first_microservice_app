package org.example.device.protDynament;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandBuilder;
import org.example.device.command.CommandParser;
import org.example.device.command.CommandType;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
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
        byte[] baseBody = buildReadCommand(0x06); // 10 13 00 10 1F cs1 cs2
        SingleCommand command = new SingleCommand(
                "getVersion",
                "getVersion - Получить версию конфигурации",
                "getVersion",
                baseBody,
                args -> baseBody,
                this::parseLiveDataResponseShort,
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
            //return null;
        }



        int dataLength = Byte.toUnsignedInt(response[2]);
        int payloadEnd = 3 + dataLength + 2; // 3 - заголовок, 2 - CRC
        if (payloadEnd > response.length) {
            log.warn("DYNAMENT: Несоответствие длины данных: ожидалось " + payloadEnd + ", получено " + response.length);
            return null;
        }

        byte[] payload = Arrays.copyOfRange(response, 3, response.length - 4);

        if (payload.length < dataLength) {
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

    private AnswerValues parseLiveDataResponseShort(byte[] rawResponse) {
        AnswerValues answerValues = new AnswerValues(3); // version
        log.info("Массив на входе: " + MyUtilities.bytesToHex(rawResponse));
        log.info("Размер массива на входе: " + rawResponse.length);
        byte[] frame = extractFrame(rawResponse);
        if (frame == null) return null;
        log.info("Выделенный frame: " + MyUtilities.bytesToHex(frame));
        log.info("Размер выделенного frame: " + frame.length);

        byte[] response = unescapeFrame(frame);
        log.info("Востановленный массив: " + MyUtilities.bytesToHex(response));
        log.info("Размер востановленного массива: " + response.length);
        //byte[] response = unescapeResponse(rawResponse);
        //byte[] response = (rawResponse);


        // Минимальная длина ответа: DLE + DAT + len + 8 data + DLE + EOF + checksum(2)
        if (response.length < 10) {
            log.warn("DYNAMENT: Слишком короткий ответ. " + response.length);
            return null;
        }

        if (!validateChecksum(rawResponse)) {
            log.warn("DYNAMENT: Ошибка контрольной суммы");
            // return null; // можно не прерывать, если хочется отладить
        }

        // Проверяем сигнатуру
        if (response[0] != 0x10 || response[1] != 0x1A) {
            log.warn("DYNAMENT: Неверный заголовок ответа");
            log.info("Ожидался на нулевой позиции " + 0x10 + " был найден " + response[0]);
            log.info("Ожидался на первой позиции " + 0x1A + " был найден " + response[1]);
            return null;
        }

        int dataLen = response[2] & 0xFF;

        if (dataLen != 0x08) {
            log.warn("DYNAMENT: Неожиданный размер блока данных: " + dataLen);
            return null;
        }else{
            log.info("Найден пакет данных размером:" + dataLen);
        }

        // смещение данных: байты после dataLen
        int dataOffset = 3;

        // Version: 2 байта (младший-старший)
        int version = ((response[dataOffset + 1] & 0xFF) << 8) | (response[dataOffset] & 0xFF);
        answerValues.addValue((double) version, "Version");

        // Status flags: 2 байта
        int statusFlags = ((response[dataOffset + 3] & 0xFF) << 8) | (response[dataOffset + 2] & 0xFF);
        answerValues.addValue((double) statusFlags, "Status Flags");

        // Gas reading: 4 байта float (IEEE754, little-endian!)
        int gasIntBits =
                (response[dataOffset + 4] & 0xFF) |
                        ((response[dataOffset + 5] & 0xFF) << 8) |
                        ((response[dataOffset + 6] & 0xFF) << 16) |
                        ((response[dataOffset + 7] & 0xFF) << 24);
        float gasReading = Float.intBitsToFloat(gasIntBits);
        answerValues.addValue((double) gasReading, "Gas Reading");

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

    /**
     * Проверка контрольной суммы Dynament.
     *
     * Алгоритм:
     *  - Берём все байты кадра начиная с первого DLE (0x10)
     *    и до EOF (0x1F) включительно.
     *  - Складываем их как беззнаковые байты.
     *  - Результат берём по модулю 0x10000 (16 бит).
     *  - Сравниваем с двумя байтами в конце кадра
     *    (high byte + low byte).
     */
    private boolean validateChecksum(byte[] frame) {
        if (frame == null || frame.length < 6) { // min: DLE, type, len, DLE, EOF, CRC(2)
            log.warn("validateChecksum: слишком короткий кадр: " + (frame == null ? 0 : frame.length));
            return false;
        }

        log.info("CRC: исходный кадр: [" + MyUtilities.bytesToHex(frame) + "], len=" + frame.length);

        // 1) Принятый CRC (Big Endian: High, Low)
        int rxHigh = frame[frame.length - 2] & 0xFF;
        int rxLow  = frame[frame.length - 1] & 0xFF;
        int rxCrc  = (rxHigh << 8) | rxLow;

        // 2) Наш расчёт: сумма всех байт кадра, кроме двух последних (CRC_H, CRC_L)
        int sum = 0;
        for (int i = 0; i < frame.length - 2; i++) {
            sum += (frame[i] & 0xFF);
        }
        sum &= 0xFFFF;

        log.info(String.format("CRC: received=0x%04X (%d), calculated=0x%04X (%d)", rxCrc, rxCrc, sum, sum));

        // Доп. диагностика: разложим сумму на "базу" + "газ" для пакетов live data (формат 10 1A 08 ... 10 1F CRC CRC)
        if (frame.length >= 15 && frame[0] == 0x10 && frame[1] == 0x1A && (frame[2] & 0xFF) == 0x08) {
            // Индексы данных в таком кадре фиксированы:
            // [0]=DLE [1]=DAT [2]=LEN(08) [3..4]=version [5..6]=flags [7..10]=gas(4 bytes) [11]=DLE [12]=EOF [13]=CRC_H [14]=CRC_L
            int base = 0;
            for (int i = 0; i <= 12; i++) { // до EOF включительно
                if (i >= 7 && i <= 10) continue; // пропускаем 4 байта газа
                base += (frame[i] & 0xFF);
            }
            base &= 0xFFFF;

            int gasB0 = frame[7] & 0xFF;
            int gasB1 = frame[8] & 0xFF;
            int gasB2 = frame[9] & 0xFF;
            int gasB3 = frame[10] & 0xFF;
            int gasSum = (gasB0 + gasB1 + gasB2 + gasB3) & 0xFFFF;

            int calcCheck = (base + gasSum) & 0xFFFF;

            log.info(String.format("CRC diag (live): base=0x%04X (%d), gasBytes=[%02X %02X %02X %02X] gasSum=0x%04X (%d), base+gas=0x%04X (%d)",
                    base, base, gasB0, gasB1, gasB2, gasB3, gasSum, gasSum, calcCheck, calcCheck));
            log.info(String.format("Разность received - calculated = 0x%04X (%d)", (rxCrc - sum) & 0xFFFF, (rxCrc - sum)));
        }

        return rxCrc == sum;
    }

    private byte[] extractFrame(byte[] response) {
        // Ищем первый DLE и последний DLE EOF
        int start = -1, end = -1;
        for (int i = 0; i < response.length - 1; i++) {
            if (response[i] == DLE && response[i + 1] == 0x1A) {
                start = i;
            }
            if (response[i] == DLE && response[i + 1] == EOF) {
                end = i;
                break;
            }
        }
        if (start == -1 || end == -1) {
            log.warn("DYNAMENT: не удалось найти границы кадра");
            return null;
        }

        // Берём всё от DLE ... EOF + 2 байта CRC
        int frameLen = (end + 4) - start;
        byte[] frame = Arrays.copyOfRange(response, start, start + frameLen);
        log.info("Extracted frame: " + MyUtilities.bytesToHex(frame));
        return frame;
    }

    private byte[] unescapeFrame(byte[] frame) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        for (int i = 0; i < frame.length; i++) {
            if (frame[i] == DLE && i + 1 < frame.length) {
                if (frame[i + 1] == DLE) {
                    baos.write(DLE);
                    i++; // пропускаем второй
                    continue;
                }
            }
            baos.write(frame[i]);
        }

        byte[] unescaped = baos.toByteArray();
        log.info("Unescaped frame: " + MyUtilities.bytesToHex(unescaped));
        return unescaped;
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