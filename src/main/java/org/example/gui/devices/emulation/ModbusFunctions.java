package org.example.gui.devices.emulation;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Справочник кодов функций Modbus и разбор PDU для трассировки.
 *
 * <p>Используется эмуляторами в режиме «сниффера»: по первому байту после адреса
 * печатается человекочитаемое имя функции, а по остальным байтам — расшифровка полей
 * (адрес/количество регистров, значение, CRC-статус). Нужен, чтобы при брутфорсе
 * протокола не гадать по голым шестнадцатеричным байтам.
 *
 * <p>Разбор намеренно «мягкий»: неизвестные коды не считаются ошибкой, а возвращаются
 * как {@link #UNKNOWN} с десятичным значением — чтобы в логе всегда было видно значение.
 */
public final class ModbusFunctions {

    public static final String UNKNOWN = "неизвестная функция";

    private static final Map<Integer, String> NAMES = new LinkedHashMap<>();
    private static final Map<Integer, String> EXCEPTIONS = new LinkedHashMap<>();

    static {
        NAMES.put(0x01, "ReadCoils");
        NAMES.put(0x02, "ReadDiscreteInputs");
        NAMES.put(0x03, "ReadHoldingRegs");
        NAMES.put(0x04, "ReadInputRegs");
        NAMES.put(0x05, "WriteSingleCoil");
        NAMES.put(0x06, "WriteSingleReg");
        NAMES.put(0x07, "ReadExceptionStatus");
        NAMES.put(0x08, "Diagnostics");
        NAMES.put(0x0B, "GetCommEventCounter");
        NAMES.put(0x0C, "GetCommEventLog");
        NAMES.put(0x0D, "ReportSlaveId");
        NAMES.put(0x0E, "GetCommEventCounter/Mbus");
        NAMES.put(0x0F, "WriteMultipleCoils");
        NAMES.put(0x10, "WriteMultipleRegs");
        NAMES.put(0x11, "ReportSlaveBusChar");
        NAMES.put(0x14, "ReadFifoQueue");
        NAMES.put(0x17, "ReportServerId");
        NAMES.put(0x18, "ReadFileRecord");
        NAMES.put(0x1B, "ReadFifoQueue");
        NAMES.put(0x1C, "ReadFileRecord(alt)");
        NAMES.put(0x20, "ReadFileRecordEnc");
        NAMES.put(0x2B, "ReadDeviceIdentification(0x0E)");
        NAMES.put(0x2F, "ReadDeviceIdentification(0x00)");

        EXCEPTIONS.put(0x01, "IllegalFunction");
        EXCEPTIONS.put(0x02, "IllegalDataAddress");
        EXCEPTIONS.put(0x03, "IllegalDataValue");
        EXCEPTIONS.put(0x04, "SlaveDeviceFailure");
        EXCEPTIONS.put(0x05, "Acknowledge");
        EXCEPTIONS.put(0x06, "SlaveDeviceBusy");
        EXCEPTIONS.put(0x08, "MemoryParityError");
        EXCEPTIONS.put(0x0A, "GatewayPathUnavailable");
        EXCEPTIONS.put(0x0B, "GatewayTargetFailed");
    }

    private ModbusFunctions() {
    }

    /** Имя функции по коду; для неизвестных — {@code "неизвестная функция (0x1F)"}. */
    public static String name(int func) {
        int f = func & 0xFF;
        if ((f & 0x80) != 0) {
            String exc = exceptionName(f & 0x7F);
            return "EXCEPTION " + exc;
        }
        String n = NAMES.get(f);
        if (n != null) {
            return n;
        }
        if (f >= 0x41 && f <= 0x48) {
            return "VendorSpecific(" + f + ")";
        }
        return UNKNOWN + " (" + EmulatorTrace.hexByte(f) + ")";
    }

    /** Имя кода исключения Modbus. */
    public static String exceptionName(int code) {
        String n = EXCEPTIONS.get(code & 0xFF);
        return n != null ? n : "Exception(" + EmulatorTrace.hexByte(code) + ")";
    }

    /** Известна ли функция (нужно, чтобы решить, отвечать или молчать). */
    public static boolean isKnown(int func) {
        return NAMES.containsKey(func & 0xFF);
    }

    /**
     * Расшифровка PDU (без адресаslave и без CRC) для журнала.
     *
     * @param pdu данные кадра начиная с байта функции
     * @return человекочитаемое описание полей; никогда не {@code null}
     */
    public static String describePdu(byte[] pdu) {
        if (pdu == null || pdu.length < 1) {
            return "пустой PDU";
        }
        int func = pdu[0] & 0xFF;
        if ((func & 0x80) != 0) {
            int code = pdu.length > 1 ? pdu[1] & 0xFF : 0;
            return name(func) + " code=" + code + " (" + exceptionName(code) + ")";
        }
        switch (func & 0x7F) {
            case 0x01:
            case 0x02: {
                if (pdu.length < 5) return "запрос обрезан (" + pdu.length + " B)";
                int addr = u16(pdu, 1);
                int qty = u16(pdu, 3);
                return "addr=" + addr + " (0x" + String.format("%04X", addr) + ") qty=" + qty;
            }
            case 0x03:
            case 0x04: {
                if (pdu.length < 5) return "запрос обрезан (" + pdu.length + " B)";
                int addr = u16(pdu, 1);
                int qty = u16(pdu, 3);
                return "reg=" + addr + " (0x" + String.format("%04X", addr) + ") qty=" + qty;
            }
            case 0x05:
            case 0x06: {
                if (pdu.length < 5) return "запрос обрезан (" + pdu.length + " B)";
                int addr = u16(pdu, 1);
                int val = u16(pdu, 3);
                String extra = (func & 0x7F) == 0x05
                        ? (val == 0xFF00 ? " (ON)" : val == 0x0000 ? " (OFF)" : "")
                        : "";
                return "reg=" + addr + " (0x" + String.format("%04X", addr) + ") value=" + val
                        + " (0x" + String.format("%04X", val) + ")" + extra;
            }
            case 0x0F:
            case 0x10: {
                if (pdu.length < 6) return "запрос обрезан (" + pdu.length + " B)";
                int addr = u16(pdu, 1);
                int qty = u16(pdu, 3);
                int byteCount = pdu.length > 5 ? pdu[5] & 0xFF : 0;
                StringBuilder sb = new StringBuilder();
                sb.append("reg=").append(addr).append(" (0x").append(String.format("%04X", addr))
                        .append(") qty=").append(qty).append(" byteCount=").append(byteCount);
                if (pdu.length > 6) {
                    sb.append(" data=[");
                    int max = Math.min(pdu.length - 6, 8);
                    for (int i = 0; i < max; i += 2) {
                        if (i > 0) sb.append(", ");
                        int hi = pdu[6 + i] & 0xFF;
                        int lo = (7 + i < pdu.length) ? pdu[7 + i] & 0xFF : 0;
                        sb.append(String.format("0x%04X", (hi << 8) | lo));
                    }
                    if (pdu.length - 6 > max) sb.append(", …");
                    sb.append(']');
                }
                return sb.toString();
            }
            case 0x0D:
            case 0x11:
            case 0x17: {
                return "payload=" + Math.max(0, pdu.length - 1) + " B";
            }
            case 0x2B:
            case 0x2F: {
                if (pdu.length < 4) return "запрос обрезан (" + pdu.length + " B)";
                int objType = pdu[1] & 0xFF;
                int objId = pdu[2] & 0xFF;
                return "MEI=0x0E objType=" + objType + " objId=" + objId;
            }
            default: {
                return "payload=" + Math.max(0, pdu.length - 1) + " B: " + EmulatorTrace.hex(pdu, 1, pdu.length - 1);
            }
        }
    }

    /**
     * Ожидаемая длина PDU запроса (от байта функции до конца, без CRC) для функций
     * с фиксированным размером. Для переменных — {@code -1}.
     */
    public static int fixedRequestPduLength(int func) {
        return switch (func & 0x7F) {
            case 0x01, 0x02, 0x03, 0x04, 0x05, 0x06 -> 5;
            case 0x0B, 0x0C, 0x11 -> 2;
            case 0x08 -> 4;
            default -> -1;
        };
    }

    private static int u16(byte[] d, int off) {
        return ((d[off] & 0xFF) << 8) | (d[off + 1] & 0xFF);
    }
}
