package org.example.gui.devices.emulation;

import org.example.device.protBoto.BotoModbusUtil;

import java.util.Arrays;

/**
 * Определение границы кадра в потоке Modbus (для эмуляторов-снифферов).
 *
 * <p>Протокол BOTO по Ethernet на момент разработки не документирован, поэтому граница
 * кадра ищется эвристически, в порядке убывания уверенности:
 * <ol>
 *   <li><b>RTU с верной CRC-16</b> — длина по коду функции + проверка контрольной суммы;</li>
 *   <li><b>Modbus/TCP</b> — MBAP-заголовок ({@code protocolId == 0x0000}), длина
 *       {@code 6 + length} из байт 4..5;</li>
 *   <li><b>перебор по CRC-16</b> — берётся минимальный префикс буфера с верной CRC;
 *       это спасает, когда код функции нестандартный и длину из таблицы угадать нельзя;</li>
 *   <li><b>длина по коду функции</b> — последний вариант, если CRC ещё не пришла.</li>
 * </ol>
 *
 * <p>Все методы принимают общий буфер соединения и смещение — вызывающий сам отрезает
 * обработанные кадры.
 */
public final class ModbusFrameGuesser {

    public static final int MAX_ADU = 300;

    private ModbusFrameGuesser() {
    }

    /**
     * Длина очередного кадра.
     *
     * @return длина ADU в байтах или {@code 0}, если данных недостаточно/формат не распознан
     */
    public static int frameLength(byte[] d, int off, int avail) {
        if (d == null || avail < 2 || off < 0 || off + avail > d.length) {
            return 0;
        }
        int mbap = mbapLength(d, off, avail);
        int rtu = rtuLength(d, off, avail);
        if (rtu > 0 && crcOk(d, off, rtu)) {
            return rtu;
        }
        if (mbap > 0) {
            return mbap;
        }
        int probe = crcProbeLength(d, off, avail);
        if (probe > 0) {
            return probe;
        }
        return rtu;
    }

    /** Длина Modbus/TCP ADU по MBAP-заголовку или {@code 0}, если это не MBAP. */
    public static int mbapLength(byte[] d, int off, int avail) {
        if (avail < 6) {
            return 0;
        }
        int protocolId = ((d[off + 2] & 0xFF) << 8) | (d[off + 3] & 0xFF);
        if (protocolId != 0x0000) {
            return 0;
        }
        int length = ((d[off + 4] & 0xFF) << 8) | (d[off + 5] & 0xFF);
        int total = 6 + length;
        return (length < 2 || total > MAX_ADU) ? 0 : total;
    }

    /** Длина Modbus/RTU ADU по коду функции (без проверки CRC) или {@code 0}. */
    public static int rtuLength(byte[] d, int off, int avail) {
        if (avail < 2) {
            return 0;
        }
        int func = d[off + 1] & 0xFF;
        if ((func & 0x80) != 0) {
            return 5;                      // ответ-исключение
        }
        int fixed = ModbusFunctions.fixedRequestPduLength(func);
        if (fixed >= 0) {
            return 1 + fixed + 2;          // slave + PDU + CRC
        }
        if (func == 0x0F || func == 0x10) {
            if (avail < 7) {
                return 0;
            }
            int byteCount = d[off + 6] & 0xFF;
            if (byteCount > 246) {
                return 0;
            }
            return 7 + byteCount + 2;
        }
        return 0;
    }

    /** Минимальная длина префикса с верной CRC-16/Modbus. */
    public static int crcProbeLength(byte[] d, int off, int avail) {
        int max = Math.min(avail, MAX_ADU);
        for (int len = 4; len <= max; len++) {
            if (crcOk(d, off, len)) {
                return len;
            }
        }
        return 0;
    }

    /** Верна ли CRC-16/Modbus в кадре длиной {@code len}. */
    public static boolean crcOk(byte[] d, int off, int len) {
        return d != null && len >= 4 && len <= d.length - off
                && BotoModbusUtil.validateCrc(Arrays.copyOfRange(d, off, off + len));
    }

    /** Похоже ли ADU на Modbus/TCP (MBAP согласован с длиной)? */
    public static boolean isMbap(byte[] adu) {
        if (adu == null || adu.length < 8) {
            return false;
        }
        int protocolId = ((adu[2] & 0xFF) << 8) | (adu[3] & 0xFF);
        if (protocolId != 0) {
            return false;
        }
        int length = ((adu[4] & 0xFF) << 8) | (adu[5] & 0xFF);
        return length + 6 == adu.length;
    }
}
