package org.example.device.ethernet.binder;

/**
 * Кодек и реестр команд климатической камеры Binder (Modbus-подобные кадры + CRC-16/Modbus).
 *
 * <p>Протокол описан в {@code doc/protocols/binder.md} и {@code doc/protocols/modbus-crc16.md}.
 * Каждый кадр начинается с {@code slaveId}, затем код функции Modbus; в конце — CRC-16
 * (полином {@code 0xA001}), младший байт вперёд. CRC считается по кадру БЕЗ двух
 * собственных байтов CRC.
 *
 * <p>Реализация автономная (без привязки к COM/фреймворку устройств): используется
 * и панелью управления (TCP-клиент), и эмулятором (TCP-сервер), а в дальнейшем —
 * как основа для глобального протокола {@code SomeDevice}.
 */
public final class BinderCommandRegistry {

    // Адреса регистров Modbus
    private static final int REG_SET_TEMP = 0x1581;      // float °C, write multiple (0x10)
    private static final int REG_GET_TEMP = 0x11A9;      // float °C, read holding (0x03)
    private static final int REG_HUMIDITY = 0x158B;      // write single (0x06)

    private BinderCommandRegistry() {
    }

    // ─── CRC-16/Modbus ────────────────────────────────────────────────────

    /**
     * CRC-16/Modbus по кадру {@code data[0..len)} (без двух байтов CRC).
     * Регистр инициализируется 0xFFFF, полином (реверсный) 0xA001.
     */
    public static int crc16Modbus(byte[] data, int len) {
        int crc = 0xFFFF;
        for (int i = 0; i < len; i++) {
            crc ^= data[i] & 0xFF;
            for (int b = 0; b < 8; b++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ 0xA001;
                } else {
                    crc >>>= 1;
                }
            }
        }
        return crc & 0xFFFF;
    }

    /** Возвращает true, если хвостовые 2 байта {@code data} содержат верную CRC. */
    public static boolean checkCrc(byte[] data, int lenWithoutCrc) {
        if (data == null || data.length < lenWithoutCrc + 2) {
            return false;
        }
        int received = (data[lenWithoutCrc] & 0xFF) | ((data[lenWithoutCrc + 1] & 0xFF) << 8);
        return received == crc16Modbus(data, lenWithoutCrc);
    }

    // ─── Сборка кадров запроса ────────────────────────────────────────────

    /**
     * SetT — функция 0x10 (Write Multiple Registers), регистр 0x1581, 2 регистра.
     * Значение — 32-битный float °C (IEEE-754); байты передаются b1,b0,b3,b2 (little-endian).
     *
     * @return кадр длиной 13 байт (включая CRC)
     */
    public static byte[] buildSetTemperatureFrame(int slaveId, float temperature) {
        int bits = Float.floatToRawIntBits(temperature);
        byte b0 = (byte) (bits & 0xFF);
        byte b1 = (byte) ((bits >>> 8) & 0xFF);
        byte b2 = (byte) ((bits >>> 16) & 0xFF);
        byte b3 = (byte) ((bits >>> 24) & 0xFF);

        byte[] f = new byte[13];
        f[0] = (byte) slaveId;
        f[1] = 0x10;
        f[2] = (byte) (REG_SET_TEMP >> 8);
        f[3] = (byte) REG_SET_TEMP;
        f[4] = 0x00;
        f[5] = 0x02;
        f[6] = 0x04;
        f[7] = b1;
        f[8] = b0;
        f[9] = b3;
        f[10] = b2;
        appendCrc(f, 11);
        return f;
    }

    /**
     * GetT — функция 0x03 (Read Holding Registers), регистр 0x11A9, 2 регистра.
     *
     * @return кадр длиной 8 байт (включая CRC)
     */
    public static byte[] buildGetTemperatureFrame(int slaveId) {
        byte[] f = new byte[8];
        f[0] = (byte) slaveId;
        f[1] = 0x03;
        f[2] = (byte) (REG_GET_TEMP >> 8);
        f[3] = (byte) REG_GET_TEMP;
        f[4] = 0x00;
        f[5] = 0x02;
        appendCrc(f, 6);
        return f;
    }

    /**
     * SetHC — функция 0x06 (Write Single Register), регистр 0x158B.
     *
     * @return кадр длиной 8 байт (включая CRC)
     */
    public static byte[] buildSetHumidityFrame(int slaveId, boolean on) {
        byte[] f = new byte[8];
        f[0] = (byte) slaveId;
        f[1] = 0x06;
        f[2] = (byte) (REG_HUMIDITY >> 8);
        f[3] = (byte) REG_HUMIDITY;
        f[4] = 0x00;
        f[5] = on ? (byte) 0x81 : (byte) 0x00;
        appendCrc(f, 6);
        return f;
    }

    // ─── Разбор ответов ───────────────────────────────────────────────────

    /** Ожидаемая длина ответа на GetT. */
    public static int getTemperatureReplyLength() {
        return 9;
    }

    /**
     * Разбор ответа на GetT (9 байт). Возвращает температуру °C или {@code null},
     * если адрес/функция/длина/CRC не совпадают.
     */
    public static Float parseGetTemperature(byte[] reply, int slaveId) {
        if (reply == null || reply.length != 9) {
            return null;
        }
        if ((reply[0] & 0xFF) != slaveId || (reply[1] & 0xFF) != 0x03) {
            return null;
        }
        if (!checkCrc(reply, 7)) {
            return null;
        }
        int bits = (reply[4] & 0xFF) | ((reply[3] & 0xFF) << 8)
                | ((reply[6] & 0xFF) << 16) | ((reply[5] & 0xFF) << 24);
        return Float.intBitsToFloat(bits);
    }

    /**
     * Проверка эхо-ответа на write (0x10 SetT или 0x06 SetHC).
     * Допустимая длина 8 байт, совпадают slaveId, функция и старший/младший байты регистра.
     */
    public static boolean isWriteAck(byte[] reply, int slaveId, int funcCode, int register, int byteLen) {
        if (reply == null || reply.length != byteLen) {
            return false;
        }
        if ((reply[0] & 0xFF) != slaveId || (reply[1] & 0xFF) != funcCode) {
            return false;
        }
        if ((reply[2] & 0xFF) != (register >> 8) || (reply[3] & 0xFF) != register) {
            return false;
        }
        return checkCrc(reply, byteLen - 2);
    }

    /** Возвращает true, если эхо соответствует ответу на SetT (0x10). */
    public static boolean isSetTemperatureAck(byte[] reply, int slaveId) {
        return isWriteAck(reply, slaveId, 0x10, REG_SET_TEMP, 8);
    }

    /** Возвращает true, если эхо соответствует ответу на SetHC (0x06). */
    public static boolean isSetHumidityAck(byte[] reply, int slaveId) {
        return isWriteAck(reply, slaveId, 0x06, REG_HUMIDITY, 8);
    }

    private static void appendCrc(byte[] f, int lenWithoutCrc) {
        int crc = crc16Modbus(f, lenWithoutCrc);
        f[lenWithoutCrc] = (byte) (crc & 0xFF);
        f[lenWithoutCrc + 1] = (byte) ((crc >>> 8) & 0xFF);
    }
}
