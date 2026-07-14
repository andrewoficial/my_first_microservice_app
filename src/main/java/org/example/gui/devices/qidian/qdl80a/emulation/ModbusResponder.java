package org.example.gui.devices.qidian.qdl80a.emulation;

import lombok.extern.slf4j.Slf4j;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import static org.example.utilites.LittleEndianUtils.bytesToHex;

@Slf4j
public class ModbusResponder {

    private volatile int address = 1;
    private volatile int baudRateCode = 3; // 3 = 9600
    private volatile int unitCode = 1;     // 1 = кПа
    private volatile int decimalPoints = 1;
    private volatile int zeroOffset = 0;
    private volatile int pv = 0;
    private volatile int parity = 0;       // 0=None, 1=Odd, 2=Even  (регистр 0x0025)

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
        registers.put(0x0025, (short) parity);
        // 0x0016 (float) — не хранится в карте, вычисляется динамически
    }

    // ================== Вспомогательные методы ==================

    /**
     * Возвращает текущее значение PV в виде float с учётом decimalPoints.
     * Это значение используется для регистра 0x0016 (IEEE754 float, ABCD).
     */
    private float getCurrentFloatValue() {
        int dp = decimalPoints;
        if (dp < 0 || dp > 4) dp = 0;
        return pv / (float) Math.pow(10, dp);
    }

    // ================== Основной метод обработки запроса ==================

    public synchronized byte[] processRequest(byte[] request) {
        log.info(">> " + bytesToHex(request) + " (" + request.length + " байт)");
        if (request.length < 8) {
            log.warn("Короткий запрос: " + request.length + " < 8");
            return null;
        }

        if (!validateCRC(request)) {
            log.warn("CRC ошибка: " + bytesToHex(request));
            return null;
        }

        int slaveAddr = request[0] & 0xFF;
        if (slaveAddr != address && slaveAddr != 0) {
            log.warn("Адрес " + slaveAddr + " != " + address);
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
                log.warn("Неподдерживаемая функция: 0x" + String.format("%02X", function));
                return buildExceptionResponse(slaveAddr, function, 0x01);
        }
    }

    // ================== Чтение (0x03 / 0x04) ==================

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

            // Специальная обработка float-регистра 0x0016 (и 0x0017)
            if (regAddr == 0x0016 || regAddr == 0x0017) {
                float fval = getCurrentFloatValue();
                int bits = Float.floatToIntBits(fval);
                if (regAddr == 0x0016) {
                    response.putShort((short) (bits >>> 16)); // high word (AB)
                } else {
                    response.putShort((short) (bits & 0xFFFF)); // low word (CD)
                }
                continue;
            }

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

    // ================== Запись одного регистра (0x06) ==================

    private byte[] handleWriteSingle(byte[] request) {
        int regAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int value = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);

        switch (regAddr) {
            case 0x0000:
                if (value < 1 || value > 255) return buildExceptionResponse(request[0], request[1], 0x03);
                address = value;
                registers.put(regAddr, (short) value);
                break;

            case 0x0001: // Baud rate (0..7 по документу)
                if (value < 0 || value > 7) return buildExceptionResponse(request[0], request[1], 0x03);
                baudRateCode = value;
                registers.put(regAddr, (short) value);
                break;

            case 0x000C:
                zeroOffset = value;
                registers.put(regAddr, (short) value);
                break;

            case 0x0025: // Parity / Serial check bit (0=None, 1=Odd, 2=Even)
                if (value < 0 || value > 2) return buildExceptionResponse(request[0], request[1], 0x03);
                parity = value;
                registers.put(regAddr, (short) value);
                break;

            case 0x000F:
                // Save to user area — просто эхо (как требует документ)
                break;

            case 0x0010:
                // Factory reset
                address = 1;
                baudRateCode = 3;
                unitCode = 1;
                decimalPoints = 1;
                zeroOffset = 0;
                pv = 0;
                parity = 0;
                initRegisters();
                break;

            default:
                // Всё остальное (в т.ч. 0x0002, 0x0003, 0x0004, 0x0005, 0x0006, 0x0016) — illegal address
                // Согласно документу (стр.5): пользователи могут менять только 4 параметра
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

    // ================== Запись нескольких регистров (0x10) ==================

    private byte[] handleWriteMultiple(byte[] request) {
        int startAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
        int byteCount = request[6] & 0xFF;

        if (byteCount != 2 * quantity) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }

        for (int i = 0; i < quantity; i++) {
            int regAddr = startAddr + i;
            int value = ((request[7 + 2 * i] & 0xFF) << 8) | (request[8 + 2 * i] & 0xFF);

            if (regAddr == 0x0000) {
                if (value < 1 || value > 255) return buildExceptionResponse(request[0], request[1], 0x03);
                address = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x0001) {
                if (value < 0 || value > 7) return buildExceptionResponse(request[0], request[1], 0x03);
                baudRateCode = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x000C) {
                zeroOffset = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x0025) {
                if (value < 0 || value > 2) return buildExceptionResponse(request[0], request[1], 0x03);
                parity = value;
                registers.put(regAddr, (short) value);
            } else if (regAddr == 0x000F || regAddr == 0x0010) {
                // игнорируем (save/restore обрабатываются отдельно)
            } else {
                // 0x0002, 0x0003, 0x0004, 0x0005, 0x0006 и всё остальное — illegal address
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

    // ================== Вспомогательные методы ==================

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
        short recvCRC = (short) ((frame[len - 1] & 0xFF) << 8 | (frame[len - 2] & 0xFF));
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

    // ================== Методы для управления из GUI ==================

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

    public synchronized void setParity(int p) {
        if (p < 0 || p > 2) throw new IllegalArgumentException("Parity 0=None, 1=Odd, 2=Even");
        parity = p;
        registers.put(0x0025, (short) p);
    }

    // Getters
    public synchronized int getAddress() { return address; }
    public synchronized int getBaudRateCode() { return baudRateCode; }
    public synchronized int getUnitCode() { return unitCode; }
    public synchronized int getDecimalPoints() { return decimalPoints; }
    public synchronized int getZeroOffset() { return zeroOffset; }
    public synchronized int getParity() { return parity; }

    public synchronized String getBaudRateString() {
        switch (baudRateCode) {
            case 0: return "1200";
            case 1: return "2400";
            case 2: return "4800";
            case 3: return "9600";
            case 4: return "19200";
            case 5: return "38400";
            case 6: return "57600";
            case 7: return "115200";
            default: return "unknown";
        }
    }

    public synchronized String getParityString() {
        switch (parity) {
            case 0: return "None (N)";
            case 1: return "Odd (O)";
            case 2: return "Even (E)";
            default: return "unknown";
        }
    }
}
