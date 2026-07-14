package org.example.device.protQdl80a;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Реестр команд для QDL80A.
 * Реализует формирование Modbus RTU запросов и парсинг ответов.
 */
@Slf4j
public class Qdl80aCommandRegistry extends DeviceCommandRegistry {

    // Полином CRC-16/Modbus
    private static final int CRC_POLYNOMIAL = 0xA001;

    public Qdl80aCommandRegistry() {
        initCommands();
    }

    @Override
    protected void initCommands() {
        commandList.addCommand(createReadMeasurementCommand());
        commandList.addCommand(createReadFloatMeasurementCommand());
        commandList.addCommand(createReadAddressCommand());
        commandList.addCommand(createWriteAddressCommand());
        commandList.addCommand(createReadBaudRateCommand());
        commandList.addCommand(createWriteBaudRateCommand());
        commandList.addCommand(createReadUnitCommand());
        commandList.addCommand(createWriteUnitCommand());
        commandList.addCommand(createReadDecimalPointsCommand());
        commandList.addCommand(createWriteZeroOffsetCommand());
        commandList.addCommand(createSaveParametersCommand());
        commandList.addCommand(createRestoreFactoryCommand());
    }

    // ============================================================
    //  Построение Modbus RTU запросов
    // ============================================================

    /**
     * Запрос на чтение регистров (функция 0x03 или 0x04)
     */
    public byte[] buildReadRequest(byte slaveAddr, byte function, short startAddr, short quantity) {
        ByteBuffer buf = ByteBuffer.allocate(6);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(slaveAddr);
        buf.put(function);
        buf.putShort(startAddr);
        buf.putShort(quantity);
        short crc = calculateCRC(buf.array());
        return ByteBuffer.allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(buf.array())
                .putShort(crc)
                .array();
    }

    /**
     * Запрос на запись одного регистра (функция 0x06)
     */
    public byte[] buildWriteSingleRegisterRequest(byte slaveAddr, short regAddr, short value) {
        ByteBuffer buf = ByteBuffer.allocate(6);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put(slaveAddr);
        buf.put((byte)0x06);
        buf.putShort(regAddr);
        buf.putShort(value);
        byte[] withoutCRC = buf.array();
        short crc = calculateCRC(withoutCRC);
        return ByteBuffer.allocate(6 + 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(withoutCRC)
                .putShort(crc)
                .array();
    }

    // ============================================================
    //  CRC16 (Modbus)
    // ============================================================

    public static short calculateCRC(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ CRC_POLYNOMIAL;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    public static boolean validateCRC(byte[] response) {
        if (response.length < 4) {
            log.warn("Слишком короткий ответ для проверки CRC: " + MyUtilities.bytesToHexString(response));
            return false;
        }
        int len = response.length;
        byte[] data = Arrays.copyOfRange(response, 0, len - 2);
        short receivedCRC = (short)((response[len-1] & 0xFF) << 8 | (response[len-2] & 0xFF));
        short calcCRC = calculateCRC(data);
        if (calcCRC != receivedCRC) {
            log.warn("CRC mismatch: received=" + String.format("%04X", receivedCRC) +
                    ", calculated=" + String.format("%04X", calcCRC) +
                    ", data=" + MyUtilities.bytesToHexString(data));
            return false;
        }
        return true;
    }

    // ============================================================
    //  Парсеры ответов
    // ============================================================

    public AnswerValues parseReadResponse(byte[] response, int numberOfRegisters, String paramName) {
        if (!validateCRC(response)) {
            log.warn("CRC ошибка");
            return null;
        }

        // Проверка на исключение Modbus (функция >= 0x80)
        if ((response[1] & 0x80) != 0) {
            byte exceptionCode = response[2];
            AnswerValues err = new AnswerValues(1);
            err.addValue(-1.0, "Ошибка Modbus, код " + exceptionCode);
            return err;
        }

        if (response.length < 3 + 2 * numberOfRegisters + 2) {
            log.warn("Недостаточная длина ответа");
            return null;
        }

        byte byteCount = response[2];
        if (byteCount != 2 * numberOfRegisters) {
            log.warn("Несоответствие количества байт данных");
            return null;
        }

        AnswerValues result = new AnswerValues(1);

        // Извлекаем значение (старший байт первым)
        int value = 0;
        for (int i = 0; i < numberOfRegisters; i++) {
            int high = response[3 + 2*i] & 0xFF;
            int low  = response[4 + 2*i] & 0xFF;
            value = (value << 16) | (high << 8) | low;
        }

        // Если это float (2 регистра) — интерпретируем как IEEE754
        if (numberOfRegisters == 2 && paramName.contains("Float")) {
            float fVal = Float.intBitsToFloat(value);
            result.addValue((double)fVal, paramName);
        } else {
            // Для 1 регистра — знаковое целое
            short sVal = (short)(value & 0xFFFF);
            result.addValue((double)sVal, paramName);
        }
        return result;
    }

    private AnswerValues parseWriteResponse(byte[] response) {
        if (!validateCRC(response)) {
            log.warn("CRC ошибка в ответе на запись");
            return null;
        }

        if ((response[1] & 0x80) != 0) {
            byte exceptionCode = response[2];
            AnswerValues err = new AnswerValues(1);
            err.addValue(-1.0, "Ошибка записи, код " + exceptionCode);
            return err;
        }

        AnswerValues ok = new AnswerValues(1);
        ok.addValue(1.0, "Запись выполнена успешно");
        return ok;
    }

    // ============================================================
    //  Создание команд
    // ============================================================

    private SingleCommand createReadMeasurementCommand() {
        byte[] baseBody = buildReadRequest((byte)0x01, (byte)0x03, (short)0x0004, (short)1);
        return new SingleCommand(
                "readMeasurement",
                "readMeasurement - чтение значения PV (регистр H:4)",
                "readMeasurement",
                baseBody,
                args -> baseBody,
                resp -> parseReadResponse(resp, 1, "Измеренное значение (целое)"),
                7,
                CommandType.BINARY
        );
    }

    private SingleCommand createReadFloatMeasurementCommand() {
        byte[] baseBody = buildReadRequest((byte)0x01, (byte)0x03, (short)0x0016, (short)2);
        return new SingleCommand(
                "readFloatMeasurement",
                "readFloatMeasurement - чтение значения в формате float (IEEE754)",
                "readFloatMeasurement",
                baseBody,
                args -> baseBody,
                resp -> parseReadResponse(resp, 2, "Измеренное значение (float)"),
                9,
                CommandType.BINARY
        );
    }

    private SingleCommand createReadAddressCommand() {
        byte[] baseBody = buildReadRequest((byte)0x01, (byte)0x03, (short)0x0000, (short)1);
        return new SingleCommand(
                "readAddress",
                "readAddress - чтение сетевого адреса (H:0)",
                "readAddress",
                baseBody,
                args -> baseBody,
                resp -> parseReadResponse(resp, 1, "Адрес устройства"),
                7,
                CommandType.BINARY
        );
    }

    private SingleCommand createWriteAddressCommand() {
        byte[] baseBody = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "writeAddress",
                "writeAddress [1-247] - запись нового адреса",
                "writeAddress",
                baseBody,
                args -> baseBody,
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("address", Integer.class, 1, val -> (Integer)val >= 1 && (Integer)val <= 247));
        return cmd;
    }

    private SingleCommand createReadBaudRateCommand() {
        byte[] baseBody = buildReadRequest((byte)0x01, (byte)0x03, (short)0x0001, (short)1);
        return new SingleCommand(
                "readBaudRate",
                "readBaudRate - чтение кода скорости (H:1)",
                "readBaudRate",
                baseBody,
                args -> baseBody,
                resp -> parseReadResponse(resp, 1, "Код скорости"),
                7,
                CommandType.BINARY
        );
    }

    private SingleCommand createWriteBaudRateCommand() {
        byte[] baseBody = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "writeBaudRate",
                "writeBaudRate [0-5] - запись кода скорости (0=1200,1=2400,2=4800,3=9600,4=19200,5=38400)",
                "writeBaudRate",
                baseBody,
                args -> baseBody,
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("baudCode", Integer.class, 3, val -> (Integer)val >= 0 && (Integer)val <= 5));
        return cmd;
    }

    private SingleCommand createReadUnitCommand() {
        byte[] baseBody = buildReadRequest((byte)0x01, (byte)0x03, (short)0x0002, (short)1);
        return new SingleCommand(
                "readUnit",
                "readUnit - чтение кода единиц измерения (H:2)",
                "readUnit",
                baseBody,
                args -> baseBody,
                resp -> parseReadResponse(resp, 1, "Код единиц"),
                7,
                CommandType.BINARY
        );
    }

    private SingleCommand createWriteUnitCommand() {
        byte[] baseBody = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "writeUnit",
                "writeUnit [код] - запись кода единиц измерения (20=°C)",
                "writeUnit",
                baseBody,
                args -> baseBody,
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("unitCode", Integer.class, 20, null));
        return cmd;
    }

    private SingleCommand createReadDecimalPointsCommand() {
        byte[] baseBody = buildReadRequest((byte)0x01, (byte)0x03, (short)0x0003, (short)1);
        return new SingleCommand(
                "readDecimalPoints",
                "readDecimalPoints - чтение количества десятичных знаков (H:3)",
                "readDecimalPoints",
                baseBody,
                args -> baseBody,
                resp -> parseReadResponse(resp, 1, "Десятичных знаков"),
                7,
                CommandType.BINARY
        );
    }

    private SingleCommand createWriteZeroOffsetCommand() {
        byte[] baseBody = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "writeZeroOffset",
                "writeZeroOffset [-32768..32767] - запись смещения нуля (H:C)",
                "writeZeroOffset",
                baseBody,
                args -> baseBody,
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("offset", Integer.class, 0, val -> (Integer)val >= -32768 && (Integer)val <= 32767));
        return cmd;
    }

    private SingleCommand createSaveParametersCommand() {
        byte[] baseBody = buildWriteSingleRegisterRequest((byte)0x01, (short)0x000F, (short)0);
        return new SingleCommand(
                "saveParameters",
                "saveParameters - сохранить параметры (H:F, значение 0)",
                "saveParameters",
                baseBody,
                args -> baseBody,
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
    }

    private SingleCommand createRestoreFactoryCommand() {
        byte[] baseBody = buildWriteSingleRegisterRequest((byte)0x01, (short)0x0010, (short)1);
        return new SingleCommand(
                "restoreFactory",
                "restoreFactory - сброс к заводским настройкам (H:10, значение 1)",
                "restoreFactory",
                baseBody,
                args -> baseBody,
                this::parseWriteResponse,
                8,
                CommandType.BINARY
        );
    }
}