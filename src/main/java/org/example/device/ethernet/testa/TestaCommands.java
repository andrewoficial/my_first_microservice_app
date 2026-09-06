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

    // ─── Опкоды кнопочных команд (23 45 34 21) ─────────────────────────────
    public static final int OP_STOP = 0x64;
    public static final int OP_LIGHT = 0x66;
    public static final int OP_PING = 0x67;
    public static final int OP_STATE = 0x6A;

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

    /**
     * 12-байтная кнопочная команда {@code 23 45 34 21 | op | (мусор 5..7) | param uint32 LE}.
     */
    public static byte[] buildButtonCommand(int opcode, int param) {
        byte[] f = new byte[12];
        f[0] = 0x23;
        f[1] = 0x45;
        f[2] = 0x34;
        f[3] = 0x21;
        f[4] = (byte) opcode;
        f[8] = (byte) (param & 0xFF);
        f[9] = (byte) ((param >>> 8) & 0xFF);
        f[10] = (byte) ((param >>> 16) & 0xFF);
        f[11] = (byte) ((param >>> 24) & 0xFF);
        return f;
    }

    /**
     * 32-байтный кадр «запуск статического поддержания» (02 33 88 66).
     * Влажность в байтах 28..31 (float LE), скорость в 16..19 (0 = максимум), байт 5 = влажность активна.
     */
    public static byte[] buildStartFrame(double tempDeg, double rampDegPerMin,
                                         double humiditySet, boolean humidityEnabled) {
        byte[] f = new byte[SET_FRAME_LEN];
        f[0] = 0x02;
        f[1] = 0x33;
        f[2] = (byte) 0x88;
        f[3] = 0x66;
        f[5] = (byte) (humidityEnabled ? 1 : 0);
        putFloatLE(f, 12, tempDeg);
        putFloatLE(f, 16, rampDegPerMin > 0 ? rampDegPerMin : 0.0);
        f[20] = 0x33;
        putFloatLE(f, 28, humiditySet);
        return f;
    }

    private static void putFloatLE(byte[] f, int off, double v) {
        int bits = Float.floatToRawIntBits((float) v);
        f[off] = (byte) (bits & 0xFF);
        f[off + 1] = (byte) ((bits >>> 8) & 0xFF);
        f[off + 2] = (byte) ((bits >>> 16) & 0xFF);
        f[off + 3] = (byte) ((bits >>> 24) & 0xFF);
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

    private static short shortLE(byte[] d, int off) {
        return (short) ((d[off] & 0xFF) | ((d[off + 1] & 0xFF) << 8));
    }

    private static double shortLE100(byte[] d, int off) {
        return shortLE(d, off) / 100.0;
    }

    /** Управление температурой, % (байты 8..9). */
    public static double parseTempControl(byte[] data) {
        return shortLE100(data, 8);
    }

    /** Влажность текущая, %RH (12..13). */
    public static double parseHumidityActual(byte[] data) {
        return shortLE100(data, 12);
    }

    /** Влажность заданная, %RH (14..15). */
    public static double parseHumiditySetpoint(byte[] data) {
        return shortLE100(data, 14);
    }

    /** Управление влажностью, % (16..17). */
    public static double parseHumidityControl(byte[] data) {
        return shortLE100(data, 16);
    }

    /** Температура парогенератора, °C (26..27). */
    public static double parseSteamGenerator(byte[] data) {
        return shortLE100(data, 26);
    }

    /** DWORD флагов (байты 20..23). */
    public static long parseFlags(byte[] data) {
        return (data[20] & 0xFFL) | ((data[21] & 0xFFL) << 8)
                | ((data[22] & 0xFFL) << 16) | ((data[23] & 0xFFL) << 24);
    }

    /** Бит подсветки камеры (байт 21, бит 7). */
    public static boolean parseLight(byte[] data) {
        return (data[21] & 0x80) != 0;
    }

    /** Байт аварий (22). */
    public static int parseAlarms(byte[] data) {
        return data[22] & 0xFF;
    }

    // ─── Сборка датаграммы статуса (для эмулятора) ─────────────────────────

    /**
     * 40-байтная датаграмма статуса камеры для эмулятора.
     * Заголовок {@code 11 22 00 00}; actual/setpoint — знаковые short (round(°C*100)) LE.
     */
    public static byte[] buildStatusDatagram(double actualDeg, double setpointDeg) {
        return buildStatusDatagram(actualDeg, setpointDeg, null);
    }

    /**
     * То же, что {@link #buildStatusDatagram(double, double)}, но позволяет заполнить
     * «сырые» байты 8..39 кадра (не документированные поля) произвольными значениями,
     * чтобы можно было наблюдать, как сторонняя (штатная) программа интерпретирует эти
     * смещения. {@code extraBytes} кладётся начиная с байта 8 (обрезается до 32 байт).
     */
    public static byte[] buildStatusDatagram(double actualDeg, double setpointDeg, byte[] extraBytes) {
        return buildStatusDatagram(actualDeg, setpointDeg, Double.NaN, Double.NaN, extraBytes);
    }

    /**
     * Собирает статус и, если влажность задана (не NaN), кладёт текущую влажность (12..13) и
     * заданную (14..15) — чтобы влажность жила/менялась динамически, а не из «сырого» буфера.
     */
    public static byte[] buildStatusDatagram(double actualDeg, double setpointDeg,
                                             double humCurDeg, double humSetDeg, byte[] extraBytes) {
        byte[] f = new byte[GET_DATAGRAM_LEN];
        f[0] = 0x11;
        f[1] = 0x22;
        short a = (short) Math.round(actualDeg * 100);
        short s = (short) Math.round(setpointDeg * 100);
        f[4] = (byte) (a & 0xFF);
        f[5] = (byte) ((a >>> 8) & 0xFF);
        f[6] = (byte) (s & 0xFF);
        f[7] = (byte) ((s >>> 8) & 0xFF);
        if (extraBytes != null) {
            int n = Math.min(extraBytes.length, GET_DATAGRAM_LEN - 8);
            System.arraycopy(extraBytes, 0, f, 8, n);
        }
        if (!Double.isNaN(humCurDeg)) {
            short hc = (short) Math.round(humCurDeg * 100);
            short hs = Double.isNaN(humSetDeg) ? hc : (short) Math.round(humSetDeg * 100);
            f[12] = (byte) (hc & 0xFF);
            f[13] = (byte) ((hc >>> 8) & 0xFF);
            f[14] = (byte) (hs & 0xFF);
            f[15] = (byte) ((hs >>> 8) & 0xFF);
        }
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
