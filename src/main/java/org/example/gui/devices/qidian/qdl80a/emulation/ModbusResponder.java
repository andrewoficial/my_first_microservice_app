package org.example.gui.devices.qidian.qdl80a.emulation;

import org.apache.log4j.Logger;
import org.example.gui.devices.qidian.qdl80a.util.DevicesSearch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.example.utilites.LittleEndianUtils.bytesToHex;

public class ModbusResponder {
    private static final Logger logger = Logger.getLogger(ModbusResponder.class);
    private volatile int address = 1;
    private volatile int baudRateCode = 3; // 3 = 9600
    private volatile int unitCode = 1;     // 1 = кПа
    private volatile int decimalPoints = 1;
    private volatile int zeroOffset = 0;
    private volatile int pv = 0;

    // Используем ConcurrentHashMap для потокобезопасного доступа
    private final Map<Integer, Short> registers = new ConcurrentHashMap<>();

    public ModbusResponder() {
        initRegisters();
    }

    private void initRegisters() {
        registers.put(0x0000, (short) address);
        registers.put(0x0001, (short) baudRateCode);
        registers.put(0x0002, (short) unitCode);
        registers.put(0x0003, (short) decimalPoints);
        registers.put(0x0004, (short) pv);
        registers.put(0x0005, (short) 0);
        registers.put(0x0006, (short) 0);
        registers.put(0x000C, (short) zeroOffset);
    }

    // Основной метод обработки запроса (синхронизирован для атомарности)
    public synchronized byte[] processRequest(byte[] request) {
        logger.info(">> " + bytesToHex(request) + " (" + request.length + " байт)");
        if (request.length < 8) {
            logger.warn("Короткий запрос: " + request.length + " < 8");
            return null;
        }

        if (!validateCRC(request)) {
            logger.warn("CRC ошибка: " + bytesToHex(request));
            return null;
        }

        int slaveAddr = request[0] & 0xFF;
        if (slaveAddr != address && slaveAddr != 0) {
            logger.warn("Адрес " + slaveAddr + " != " + address);
            return null;
        }

        int function = request[1] & 0xFF;

        switch (function) {
            case 0x03:
            case 0x04:
                return handleRead(request);
            case 0x06:
                return handleWriteSingle(request);
            case 0x10:
                return handleWriteMultiple(request);
            default:
                logger.warn("Неподдерживаемая функция: 0x" + String.format("%02X", function));
                return buildExceptionResponse(slaveAddr, function, 0x01);
        }
    }

    private byte[] handleRead(byte[] request) {
        int startAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);

        if (quantity < 1 || quantity > 125) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }

        ByteBuffer response = ByteBuffer.allocate(3 + 2 * quantity);
        response.order(ByteOrder.BIG_ENDIAN);
        response.put((byte) request[0]);
        response.put((byte) request[1]);
        response.put((byte) (2 * quantity));

        for (int i = 0; i < quantity; i++) {
            int regAddr = startAddr + i;
            Short val = registers.get(regAddr);
            response.putShort(val != null ? val : (short) 0);
        }

        byte[] data = response.array();
        short crc = calculateCRC(data);
        ByteBuffer result = ByteBuffer.allocate(data.length + 2);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.put(data);
        result.putShort(crc);
        return result.array();
    }

    private byte[] handleWriteSingle(byte[] request) {
        int regAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int value = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);

        switch (regAddr) {
            case 0x0000:
                if (value < 1 || value > 255) return buildExceptionResponse(request[0], request[1], 0x03);
                address = value;
                registers.put(regAddr, (short) value);
                break;
            case 0x0001:
                if (value < 0 || value > 5) return buildExceptionResponse(request[0], request[1], 0x03);
                baudRateCode = value;
                registers.put(regAddr, (short) value);
                break;
            case 0x0002:
                unitCode = value;
                registers.put(regAddr, (short) value);
                break;
            case 0x0003:
                if (value < 0 || value > 4) return buildExceptionResponse(request[0], request[1], 0x03);
                decimalPoints = value;
                registers.put(regAddr, (short) value);
                break;
            case 0x0004:
                pv = value;
                registers.put(regAddr, (short) value);
                break;
            case 0x000C:
                zeroOffset = value;
                registers.put(regAddr, (short) value);
                break;
            case 0x000F:
                // Сохранить – эхо, ничего не делаем
                break;
            case 0x0010:
                // Сброс к заводским
                address = 1;
                baudRateCode = 3;
                unitCode = 1;
                decimalPoints = 1;
                zeroOffset = 0;
                pv = 0;
                initRegisters();
                break;
            default:
                return buildExceptionResponse(request[0], request[1], 0x02);
        }

        // Эхо-ответ для 0x06
        ByteBuffer response = ByteBuffer.allocate(6);
        response.order(ByteOrder.BIG_ENDIAN);
        response.put((byte) request[0]);
        response.put((byte) request[1]);
        response.putShort((short) regAddr);
        response.putShort((short) value);
        byte[] data = response.array();
        short crc = calculateCRC(data);
        ByteBuffer result = ByteBuffer.allocate(data.length + 2);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.put(data);
        result.putShort(crc);
        return result.array();
    }

    private byte[] handleWriteMultiple(byte[] request) {
        int startAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
        int byteCount = request[6] & 0xFF;

        if (byteCount != 2 * quantity) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }

        for (int i = 0; i < quantity; i++) {
            int regAddr = startAddr + i;
            int value = ((request[7 + 2*i] & 0xFF) << 8) | (request[8 + 2*i] & 0xFF);

            if (regAddr == 0x0000) {
                if (value < 1 || value > 255) return buildExceptionResponse(request[0], request[1], 0x03);
                address = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x0001) {
                if (value < 0 || value > 5) return buildExceptionResponse(request[0], request[1], 0x03);
                baudRateCode = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x0002) {
                unitCode = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x0003) {
                if (value < 0 || value > 4) return buildExceptionResponse(request[0], request[1], 0x03);
                decimalPoints = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x0004) {
                pv = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x000C) {
                zeroOffset = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x000F || regAddr == 0x0010) {
                // игнорируем
            } else {
                return buildExceptionResponse(request[0], request[1], 0x02);
            }
        }

        // Ответ для 0x10
        ByteBuffer response = ByteBuffer.allocate(6);
        response.order(ByteOrder.BIG_ENDIAN);
        response.put((byte) request[0]);
        response.put((byte) request[1]);
        response.putShort((short) startAddr);
        response.putShort((short) quantity);
        byte[] data = response.array();
        short crc = calculateCRC(data);
        ByteBuffer result = ByteBuffer.allocate(data.length + 2);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.put(data);
        result.putShort(crc);
        return result.array();
    }

    private byte[] buildExceptionResponse(int addr, int func, int code) {
        byte[] data = new byte[3];
        data[0] = (byte) addr;
        data[1] = (byte) (func | 0x80);
        data[2] = (byte) code;
        short crc = calculateCRC(data);
        ByteBuffer result = ByteBuffer.allocate(data.length + 2);
        result.order(ByteOrder.LITTLE_ENDIAN);
        result.put(data);
        result.putShort(crc);
        return result.array();
    }

    private boolean validateCRC(byte[] frame) {
        if (frame.length < 4) return false;
        int len = frame.length;
        byte[] data = new byte[len - 2];
        System.arraycopy(frame, 0, data, 0, len - 2);
        short recvCRC = (short)((frame[len-1] & 0xFF) << 8 | (frame[len-2] & 0xFF));
        short calcCRC = calculateCRC(data);
        return recvCRC == calcCRC;
    }

    private short calculateCRC(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    // ---- Методы для управления из GUI (синхронизированы) ----

    public synchronized void setPv(int value) {
        pv = value;
        registers.put(0x0004, (short) value);
    }

    public synchronized void setUnitCode(int code) {
        unitCode = code;
        registers.put(0x0002, (short) code);
    }

    public synchronized void setDecimalPoints(int dp) {
        decimalPoints = dp;
        registers.put(0x0003, (short) dp);
    }

    public synchronized void setAddress(int newAddress) {
        if (newAddress < 1 || newAddress > 255) throw new IllegalArgumentException("Адрес должен быть 1..255");
        address = newAddress;
        registers.put(0x0000, (short) newAddress);
    }

    public synchronized void setBaudRateCode(int code) {
        if (code < 0 || code > 7) throw new IllegalArgumentException("Код скорости 0..7");
        baudRateCode = code;
        registers.put(0x0001, (short) code);
    }

    public synchronized void setZeroOffset(int offset) {
        if (offset < -32768 || offset > 32767) throw new IllegalArgumentException("Смещение вне диапазона");
        zeroOffset = offset;
        registers.put(0x000C, (short) offset);
    }

    public synchronized int getAddress() { return address; }
    public synchronized int getBaudRateCode() { return baudRateCode; }
    public synchronized int getUnitCode() { return unitCode; }
    public synchronized int getDecimalPoints() { return decimalPoints; }
    public synchronized int getZeroOffset() { return zeroOffset; }

    public synchronized String getBaudRateString() {
        switch (baudRateCode) {
            case 0: return "1200";
            case 1: return "2400";
            case 2: return "4800";
            case 3: return "9600";
            case 4: return "19200";
            case 5: return "38400";
            default: return "unknown";
        }
    }
}