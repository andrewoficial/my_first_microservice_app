package org.example.device.protTt5166;

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
import java.util.HashMap;
import java.util.Map;

public class TT5166CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(TT5166CommandRegistry.class);

    private final Map<Integer, String> faultMessages = new HashMap<>();

    public TT5166CommandRegistry() {
        initFaultMessages();
    }

    private void initFaultMessages() {
        faultMessages.put(1, "CM1 compressor overcurrent trip.");
        faultMessages.put(2, "CM1 compressor overpressure protection tripping.");
        faultMessages.put(3, "CM1 compressor oil pressure anomaly.");
        faultMessages.put(4, "CM2 compressor overcurrent trip.");
        faultMessages.put(5, "CM2 compressor overpressure protection tripping.");
        faultMessages.put(6, "CM2 compressor oil pressure anomaly.");
        faultMessages.put(7, "CM3 compressor overcurrent trip.");
        faultMessages.put(8, "CM3 compressor overpressure protection tripping.");
        faultMessages.put(9, "storage tank water. （Delayed alarm）");
        faultMessages.put(10, "storage tank water. （Alarm immediately for water shortage）");
        faultMessages.put(11, "CM3 compressor oil pressure anomaly.");
        faultMessages.put(12, "CM4 compressor overcurrent trip.");
        faultMessages.put(13, "CM4 compressor overpressure protection tripping.");
        faultMessages.put(14, "CM4 compressor oil pressure anomaly.");
        faultMessages.put(15, "empty burning heating tube protection.");
        faultMessages.put(16, "empty tube humidification burn protection.");
        faultMessages.put(17, "power due to phase or reverse phase.");
        faultMessages.put(18, "the temperature inside the over-temperature protection.");
        faultMessages.put(19, "Alarm B.");
        faultMessages.put(20, "Alarm C.");
        faultMessages.put(21, "box fan operation exception.");
        faultMessages.put(22, "abnormal cooling towers.");
        faultMessages.put(23, "dry-bulb SCR failure.");
        faultMessages.put(24, "wet bulb SCR failure.");
        faultMessages.put(25, "temperature sensor error.");
        faultMessages.put(26, "Humidity sensor error.");
        faultMessages.put(27, "inside the temperature gauge anomaly.");
        faultMessages.put(28, "blower flow。");
        faultMessages.put(29, "condenser fan flow。");
        faultMessages.put(30, "filter differential pressure。");
        faultMessages.put(31, "supplying pressure difference。");
        faultMessages.put(32, "first compressor low pressure switch protection。");
        faultMessages.put(33, "second compressor low pressure switch protection。");
        faultMessages.put(34, "Dehumidifier fault protection。");
        faultMessages.put(35, "Equipment have not closed the door alarm");
        faultMessages.put(36, "The box body the emergency stop switch alarm");
        faultMessages.put(37, "The emergency stop switch alarm");
        faultMessages.put(38, "Circulating fan flow alarm");
        faultMessages.put(39, "One exhaust fan alarm");
        faultMessages.put(40, "Two exhaust fan alarm");
        faultMessages.put(41, "CM1 compressor heat。");
        faultMessages.put(42, "CM2 compressor heat。");
        faultMessages.put(43, "VVVF ERR");
        faultMessages.put(44, "CIRCULATION FAN ERR");
        faultMessages.put(45, "AIR DUCT ERR");
        faultMessages.put(46, "ALM1 TEMP OR HUMIDITY OUT.");
        faultMessages.put(47, "ALM2 TEMP OR HUMIDITY OUT。");
        faultMessages.put(48, "ALM3 TEMP OR HUMIDITY OUT。");
        faultMessages.put(49, "Alarm of low pressure");
        faultMessages.put(50, "Alarm of cool cycle no water");
        faultMessages.put(51, "INFO1（Custom alarm）");
        faultMessages.put(52, "INFO2（Custom alarm）");
        faultMessages.put(53, "INFO3（Custom alarm）");
        faultMessages.put(54, "INFO4（Custom alarm）");
        faultMessages.put(55, "INFO5（Custom alarm）");
        faultMessages.put(56, "INF200（Extension module custom alarm）");
        faultMessages.put(57, "INF201（Extension module custom alarm）");
        faultMessages.put(58, "INF202（Extension module custom alarm）");
        faultMessages.put(59, "INF203（Extension module custom alarm）");
        faultMessages.put(60, "INF204（Extension module custom alarm）");
        faultMessages.put(61, "INF205（Extension module custom alarm）");
        faultMessages.put(62, "INF206（Extension module custom alarm）");
        faultMessages.put(63, "INF207（Extension module custom alarm）");
        faultMessages.put(64, "Smoke alarm");
        faultMessages.put(65, "A TEMP OVER");
        faultMessages.put(66, "B TEMP OVER");
        faultMessages.put(67, "C TEMP OVER");
        faultMessages.put(68, "D TEMP OVER");
        faultMessages.put(69, "E TEMP OVER");
        faultMessages.put(70, "F TEMP OVER");
        faultMessages.put(71, "G TEMP OVER");
        faultMessages.put(72, "H TEMP OVER");
        faultMessages.put(73, "Main circuit over T shutdown");
        faultMessages.put(74, "Sample over T shutdown");
        faultMessages.put(75, "404 low pressure over T shutdown");
        faultMessages.put(76, "404 high pressure over T shutdown");
        faultMessages.put(77, "23 low pressure overt T shutdown");
        faultMessages.put(78, "23 high pressure overt T shutdown");
        faultMessages.put(79, "CM1 OUT TEMP OVER");
        faultMessages.put(80, "CM2 OUT TEMP OVER");
        faultMessages.put(81, "water high");
        faultMessages.put(82, "Multi temperature over temperature shutdown");
        faultMessages.put(83, "Prompt for too long low temperature time");
        faultMessages.put(84, "Shutdown due to too long low temperature time");
        faultMessages.put(85, "The heating rate is too slow");
    }

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetDataCommand());
        commandList.addCommand(createGetFaultCommand());
        commandList.addCommand(createStartCommand());
        commandList.addCommand(createStopCommand());
        commandList.addCommand(createSetConstTempCommand());
        commandList.addCommand(createSetConstHumCommand());

        commandList.addCommand(createGetStateCommand());
        commandList.addCommand(createGetProgramTimeCommand());
        commandList.addCommand(createGetAdditionalChannelsCommand());
        commandList.addCommand(createSetTempGradientCommand());
        commandList.addCommand(createSetHumGradientCommand());
    }

    private SingleCommand createGetDataCommand() {
        byte[] baseBody = buildReadRegisters((byte) 0x01, 0x0000, 6);
        SingleCommand command = new SingleCommand(
                "getData",
                "getData - Получить данные температуры и влажности",
                "getData",
                baseBody,
                args -> baseBody, // No args
                this::parseDataResponse,
                3 + 12 + 2, // addr func bytecount data(12) crc(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetFaultCommand() {
        byte[] baseBody = buildReadRegisters((byte) 0x01, 0x001B, 1);
        SingleCommand command = new SingleCommand(
                "getFault",
                "getFault - Получить информацию об ошибке",
                "getFault",
                baseBody,
                args -> baseBody,
                this::parseFaultResponse,
                3 + 2 + 2,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createStartCommand() {
        byte[] baseBody = buildForceCoil((byte) 0x01, 0x0000, true);
        SingleCommand command = new SingleCommand(
                "start",
                "start - Запустить устройство",
                "start",
                baseBody,
                args -> baseBody,
                this::parseAckResponse,
                8,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createStopCommand() {
        byte[] baseBody = buildForceCoil((byte) 0x01, 0x0001, true);
        SingleCommand command = new SingleCommand(
                "stop",
                "stop - Остановить устройство",
                "stop",
                baseBody,
                args -> baseBody,
                this::parseAckResponse,
                8,
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetConstTempCommand() {
        byte[] baseBody = buildWriteRegister((byte) 0x01, 0x0026, (short) 0);
        SingleCommand command = new SingleCommand(
                "setConstTemp",
                "setConstTemp [value] - Установить постоянную температуру",
                "setConstTemp",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    short sval = (short) (value * 10);
                    return buildWriteRegister((byte) 0x01, 0x0026, sval);
                },
                this::parseAckResponse,
                8,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> true // Add validation if needed
        ));
        return command;
    }

    private SingleCommand createSetConstHumCommand() {
        byte[] baseBody = buildWriteRegister((byte) 0x01, 0x0027, (short) 0);
        SingleCommand command = new SingleCommand(
                "setConstHum",
                "setConstHum [value] - Установить постоянную влажность",
                "setConstHum",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    short sval = (short) (value * 10);
                    return buildWriteRegister((byte) 0x01, 0x0027, sval);
                },
                this::parseAckResponse,
                8,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> true // Add validation if needed
        ));
        return command;
    }

    private SingleCommand createGetStateCommand() {
        byte[] baseBody = buildReadRegisters((byte) 0x01, 0x0018, 2);
        SingleCommand command = new SingleCommand(
                "getState",
                "getState - Получить состояние работы и режим",
                "getState",
                baseBody,
                args -> baseBody,
                this::parseStateResponse,
                3 + 4 + 2, // addr func bytecount data(4) crc(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetProgramTimeCommand() {
        byte[] baseBody = buildReadRegisters((byte) 0x01, 0x0006, 3);
        SingleCommand command = new SingleCommand(
                "getProgramTime",
                "getProgramTime - Получить время выполнения программы",
                "getProgramTime",
                baseBody,
                args -> baseBody,
                this::parseProgramTimeResponse,
                3 + 6 + 2, // addr func bytecount data(6) crc(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createGetAdditionalChannelsCommand() {
        byte[] baseBody = buildReadRegisters((byte) 0x01, 0x0024, 3);
        SingleCommand command = new SingleCommand(
                "getAdditionalChannels",
                "getAdditionalChannels - Получить температуры дополнительных каналов",
                "getAdditionalChannels",
                baseBody,
                args -> baseBody,
                this::parseAdditionalChannelsResponse,
                3 + 6 + 2, // addr func bytecount data(6) crc(2)
                CommandType.BINARY
        );
        return command;
    }

    private SingleCommand createSetTempGradientCommand() {
        byte[] baseBody = buildWriteRegister((byte) 0x01, 0x0064, (short) 0);
        SingleCommand command = new SingleCommand(
                "setTempGradient",
                "setTempGradient [value] - Установить градиент температуры",
                "setTempGradient",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    short sval = (short) (value * 10);
                    return buildWriteRegister((byte) 0x01, 0x0064, sval);
                },
                this::parseAckResponse,
                8,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> true
        ));
        return command;
    }

    private SingleCommand createSetHumGradientCommand() {
        byte[] baseBody = buildWriteRegister((byte) 0x01, 0x0065, (short) 0);
        SingleCommand command = new SingleCommand(
                "setHumGradient",
                "setHumGradient [value] - Установить градиент влажности",
                "setHumGradient",
                baseBody,
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    short sval = (short) (value * 10);
                    return buildWriteRegister((byte) 0x01, 0x0065, sval);
                },
                this::parseAckResponse,
                8,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                0.0f,
                val -> true
        ));
        return command;
    }

    public byte[] buildReadRegisters(byte deviceAddr, int startReg, int numRegs) {
        byte[] frame = new byte[8];
        frame[0] = deviceAddr;
        frame[1] = 0x03;
        frame[2] = (byte) (startReg >> 8);
        frame[3] = (byte) (startReg & 0xFF);
        frame[4] = (byte) (numRegs >> 8);
        frame[5] = (byte) (numRegs & 0xFF);
        int crc = calculateModbusCRC(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF); // low byte
        frame[7] = (byte) (crc >> 8); // high byte
        log.info("Built read registers command: " + MyUtilities.bytesToHex(frame));
        return frame;
    }

    public byte[] buildForceCoil(byte deviceAddr, int coilAddr, boolean on) {
        byte[] frame = new byte[8];
        frame[0] = deviceAddr;
        frame[1] = 0x05;
        frame[2] = (byte) (coilAddr >> 8);
        frame[3] = (byte) (coilAddr & 0xFF);
        frame[4] = (byte) (on ? 0xFF : 0x00);
        frame[5] = 0x00;
        int crc = calculateModbusCRC(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) (crc >> 8);
        log.info("Built force coil command: " + MyUtilities.bytesToHex(frame));
        return frame;
    }

    public byte[] buildWriteRegister(byte deviceAddr, int regAddr, short value) {
        byte[] frame = new byte[8];
        frame[0] = deviceAddr;
        frame[1] = 0x06;
        frame[2] = (byte) (regAddr >> 8);
        frame[3] = (byte) (regAddr & 0xFF);
        frame[4] = (byte) (value >> 8);
        frame[5] = (byte) (value & 0xFF);
        int crc = calculateModbusCRC(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) (crc >> 8);
        log.info("Built write register command: " + MyUtilities.bytesToHex(frame));
        return frame;
    }

    public int calculateModbusCRC(byte[] buf, int off, int len) {
        int crc = 0xFFFF;
        for (int pos = off; pos < off + len; pos++) {
            crc ^= (int) buf[pos] & 0xFF;
            for (int i = 8; i != 0; i--) {
                if ((crc & 0x0001) != 0) {
                    crc >>= 1;
                    crc ^= 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }

    public boolean validateChecksum(byte[] response) {
        int len = response.length;
        if (len < 4) {
            log.warn("TT5166: Response too short for CRC check");
            return false;
        }
        int calcCrc = calculateModbusCRC(response, 0, len - 2);
        int recCrc = ((response[len - 1] & 0xFF) << 8) | (response[len - 2] & 0xFF);
        log.info("Received CRC: " + recCrc + " Calculated CRC: " + calcCrc);
        return calcCrc == recCrc;
    }

    public AnswerValues parseDataResponse(byte[] response) {
        if (response.length < 3 + 12 + 2) {
            log.warn("TT5166: Response too short for getData");
            return null;
        }
        if (response[0] != 0x01 || response[1] != 0x03) {
            log.warn("TT5166: Invalid header for getData response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("TT5166: CRC error in getData response");
            return null;
        }
        if (response[2] != 12) {
            log.warn("TT5166: Unexpected byte count in getData: " + response[2]);
            return null;
        }
        byte[] data = Arrays.copyOfRange(response, 3, 3 + 12);
        AnswerValues answerValues = new AnswerValues(6);

        // Temp PV /100
        short tempPvRaw = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double tempPv = tempPvRaw / 100.0;
        answerValues.addValue(tempPv, "Температура PV (°C)");

        // Temp SV /10
        short tempSvRaw = ByteBuffer.wrap(data, 2, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double tempSv = tempSvRaw / 10.0;
        answerValues.addValue(tempSv, "Температура SV (°C)");

        // Temp Output /10
        short tempOutRaw = ByteBuffer.wrap(data, 4, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double tempOut = tempOutRaw / 10.0;
        answerValues.addValue(tempOut, "Выход температуры (%)");

        // Hum PV /10 (assuming)
        short humPvRaw = ByteBuffer.wrap(data, 6, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double humPv = humPvRaw / 10.0;
        answerValues.addValue(humPv, "Влажность PV (% RH)");

        // Hum SV /10
        short humSvRaw = ByteBuffer.wrap(data, 8, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double humSv = humSvRaw / 10.0;
        answerValues.addValue(humSv, "Влажность SV (% RH)");

        // Hum Output /10
        short humOutRaw = ByteBuffer.wrap(data, 10, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double humOut = humOutRaw / 10.0;
        answerValues.addValue(humOut, "Выход влажности (%)");

        return answerValues;
    }

    public AnswerValues parseFaultResponse(byte[] response) {
        if (response.length < 3 + 2 + 2) {
            log.warn("TT5166: Response too short for getFault");
            return null;
        }
        if (response[0] != 0x01 || response[1] != 0x03) {
            log.warn("TT5166: Invalid header for getFault response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("TT5166: CRC error in getFault response");
            return null;
        }
        if (response[2] != 2) {
            log.warn("TT5166: Unexpected byte count in getFault: " + response[2]);
            return null;
        }
        byte[] data = Arrays.copyOfRange(response, 3, 5);
        short faultCode = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN).getShort();
        String message = faultMessages.getOrDefault((int) faultCode, "Unknown fault code: " + faultCode);
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(faultCode, message);
        return answerValues;
    }

    public AnswerValues parseAckResponse(byte[] response) {
        AnswerValues answerValues = new AnswerValues(1);
        if (response.length < 8) {
            log.warn("TT5166: Response too short for ACK");
            answerValues.addValue(-1.0, "Ошибка: Слишком короткий ответ");
            return answerValues;
        }
        if (response[0] != 0x01) {
            log.warn("TT5166: Invalid address in response");
            answerValues.addValue(-1.0, "Ошибка: Некорректный адрес");
            return answerValues;
        }
        byte func = response[1];
        if (func == 0x05 || func == 0x06) {
            if (!validateChecksum(response)) {
                log.warn("TT5166: CRC error in ACK response");
                answerValues.addValue(-1.0, "Ошибка: Некорректная CRC");
                return answerValues;
            }
            answerValues.addValue(1.0, "Успех");
        } else if (func == 0x85 || func == 0x86) {
            byte errorCode = response[2];
            answerValues.addValue(-1.0, "Ошибка: код " + errorCode);
        } else {
            answerValues.addValue(-1.0, "Ошибка: Неожиданный тип ответа 0x" + Integer.toHexString(Byte.toUnsignedInt(func)));
        }
        return answerValues;
    }

    private AnswerValues parseStateResponse(byte[] response) {
        if (response.length < 3 + 4 + 2) {
            log.warn("TT5166: Response too short for getState");
            return null;
        }
        if (response[0] != 0x01 || response[1] != 0x03) {
            log.warn("TT5166: Invalid header for getState response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("TT5166: CRC error in getState response");
            return null;
        }
        if (response[2] != 4) {
            log.warn("TT5166: Unexpected byte count in getState: " + response[2]);
            return null;
        }

        byte[] data = Arrays.copyOfRange(response, 3, 7);
        AnswerValues answerValues = new AnswerValues(2);

        // Run/Stop state (H0018)
        short runStateRaw = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        String runState = (runStateRaw & 0x0001) == 1 ? "Running" : "Stopped";
        answerValues.addValue((double) runStateRaw, "Состояние работы: " + runState);

        // Fixed/Program state (H0019)
        short modeStateRaw = ByteBuffer.wrap(data, 2, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        String modeState = (modeStateRaw & 0x0001) == 1 ? "Program mode" : "Fixed mode";
        answerValues.addValue((double) modeStateRaw, "Режим работы: " + modeState);

        return answerValues;
    }

    private AnswerValues parseProgramTimeResponse(byte[] response) {
        if (response.length < 3 + 6 + 2) {
            log.warn("TT5166: Response too short for getProgramTime");
            return null;
        }
        if (response[0] != 0x01 || response[1] != 0x03) {
            log.warn("TT5166: Invalid header for getProgramTime response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("TT5166: CRC error in getProgramTime response");
            return null;
        }
        if (response[2] != 6) {
            log.warn("TT5166: Unexpected byte count in getProgramTime: " + response[2]);
            return null;
        }

        byte[] data = Arrays.copyOfRange(response, 3, 9);
        AnswerValues answerValues = new AnswerValues(2);

        // Program Run to time H (H0006)
        short timeH = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        // Program Run to time M (H0008)
        short timeM = ByteBuffer.wrap(data, 4, 2).order(ByteOrder.BIG_ENDIAN).getShort();

        answerValues.addValue((double) timeH, "Время выполнения программы (часы)");
        answerValues.addValue((double) timeM, "Время выполнения программы (минуты)");

        return answerValues;
    }

    private AnswerValues parseAdditionalChannelsResponse(byte[] response) {
        if (response.length < 3 + 6 + 2) {
            log.warn("TT5166: Response too short for getAdditionalChannels");
            return null;
        }
        if (response[0] != 0x01 || response[1] != 0x03) {
            log.warn("TT5166: Invalid header for getAdditionalChannels response");
            return null;
        }
        if (!validateChecksum(response)) {
            log.warn("TT5166: CRC error in getAdditionalChannels response");
            return null;
        }
        if (response[2] != 6) {
            log.warn("TT5166: Unexpected byte count in getAdditionalChannels: " + response[2]);
            return null;
        }

        byte[] data = Arrays.copyOfRange(response, 3, 9);
        AnswerValues answerValues = new AnswerValues(3);

        // Channel 2 temperature (H0024)
        short channel2Temp = ByteBuffer.wrap(data, 0, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double channel2TempValue = channel2Temp / 10.0;
        answerValues.addValue(channel2TempValue, "Канал 2 температура (°C)");

        // Channel 3 temperature (H0025)
        short channel3Temp = ByteBuffer.wrap(data, 2, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double channel3TempValue = channel3Temp / 10.0;
        answerValues.addValue(channel3TempValue, "Канал 3 температура (°C)");

        // Channel 4 temperature (H002B)
        short channel4Temp = ByteBuffer.wrap(data, 4, 2).order(ByteOrder.BIG_ENDIAN).getShort();
        double channel4TempValue = channel4Temp / 10.0;
        answerValues.addValue(channel4TempValue, "Канал 4 температура (°C)");

        return answerValues;
    }
}