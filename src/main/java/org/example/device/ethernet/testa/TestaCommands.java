package org.example.device.ethernet.testa;

/**
 * Кодек климатической камеры Testa (UDP).
 *
 * <p>Зеркалит C++ {@code CTestaTCamera} / Java {@code TestaChamber}.
 * <ul>
 *   <li>Отправка команды установки температуры — на порт назначения (по умолчанию
 *       1300), датаграмма 32 байта, температура = 32-бит float IEEE-754 (little-endian)
 *       в байтах 12..15, заголовок {@code 02 33 88 66}.</li>
 *   <li>Приём статуса — на локальном порту 1200, датаграмма 40 байт, заголовок
 *       {@code 11 22 00 00}; текущая температура (байты 4..5) и уставка (байты 6..7) —
 *       знаковые 16-бит целые (short), делённые на 100.</li>
 * </ul>
 */
public final class TestaCommands {

    public static final int DEFAULT_REMOTE_PORT = 1300;  // порт приёма команды у камеры
    public static final int LOCAL_RECEIVE_PORT = 1200;   // локальный порт приёма статуса

    private static final int SET_FRAME_LEN = 32;
    private static final int GET_DATAGRAM_LEN = 40;

    private TestaCommands() {
    }

    // ─── Сборка кадра установки температуры (SetT, 32 байта) ───────────────

    /**
     * 32-байтная UDP-датаграмма установки температуры.
     * Заголовок {@code 02 33 88 66}, float °C (IEEE-754, little-endian) в байтах 12..15.
     */
    public static byte[] buildSetTemperatureFrame(float temperature) {
        byte[] f = new byte[SET_FRAME_LEN];
        f[0] = 0x02;
        f[1] = 0x33;
        f[2] = (byte) 0x88;
        f[3] = 0x66;
        int bits = Float.floatToRawIntBits(temperature);
        f[12] = (byte) (bits & 0xFF);
        f[13] = (byte) ((bits >>> 8) & 0xFF);
        f[14] = (byte) ((bits >>> 16) & 0xFF);
        f[15] = (byte) ((bits >>> 24) & 0xFF);
        return f;
    }

    // ─── Проверка/разбор датаграммы статуса (GetT, 40 байт) ────────────────

    /** Ожидаемая длина датаграммы статуса камеры. */
    public static int getStatusDatagramLength() {
        return GET_DATAGRAM_LEN;
    }

    /**
     * true, если датаграмма — статус камеры (длина 40, заголовок {@code 11 22 00 00}).
     */
    public static boolean isTemperatureDatagram(byte[] data) {
        if (data == null || data.length != GET_DATAGRAM_LEN) {
            return false;
        }
        return (data[0] & 0xFF) == 0x11 && (data[1] & 0xFF) == 0x22
                && data[2] == 0 && data[3] == 0;
    }

    /** Текущая температура, °C: знаковый short (байты 4..5 LE) / 100. */
    public static double parseActual(byte[] data) {
        short w = (short) ((data[4] & 0xFF) | ((data[5] & 0xFF) << 8));
        return w / 100.0;
    }

    /** Уставка из датаграммы, °C: знаковый short (байты 6..7 LE) / 100. */
    public static double parseSetpoint(byte[] data) {
        short w = (short) ((data[6] & 0xFF) | ((data[7] & 0xFF) << 8));
        return w / 100.0;
    }

    // ─── Сборка датаграммы статуса (для эмулятора) ─────────────────────────

    /**
     * 40-байтная датаграмма статуса камеры для эмулятора.
     * Заголовок {@code 11 22 00 00}; actual/setpoint — знаковые short (round(°C*100)) LE.
     */
    public static byte[] buildStatusDatagram(double actualDeg, double setpointDeg) {
        byte[] f = new byte[GET_DATAGRAM_LEN];
        f[0] = 0x11;
        f[1] = 0x22;
        short a = (short) Math.round(actualDeg * 100);
        short s = (short) Math.round(setpointDeg * 100);
        f[4] = (byte) (a & 0xFF);
        f[5] = (byte) ((a >>> 8) & 0xFF);
        f[6] = (byte) (s & 0xFF);
        f[7] = (byte) ((s >>> 8) & 0xFF);
        return f;
    }

    public static String toHex(byte[] data) {
        if (data == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(data.length * 3);
        for (byte b : data) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }
}
