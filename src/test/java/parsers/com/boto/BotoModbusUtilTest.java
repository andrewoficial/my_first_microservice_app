package parsers.com.boto;

import org.example.device.protBoto.BotoModbusUtil;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверка разбора Modbus-кадра блока реального времени термокамеры BOTO 800.
 * Ключевой момент: ответ 0x03 не содержит адреса начального регистра — значения
 * восстанавливаются по смещениям внутри блока (рег 10..31).
 */
public class BotoModbusUtilTest {

    private static final int TEMP_REG = 10;
    private static final int MODE_REG = 31;

    /// Собирает ответ вида: [slave] 03 <byteCount> <data...> <crc(2)>.
    private static byte[] readResponse(int[] registers) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(0x01);
        out.write(0x03);
        out.write(registers.length * 2);
        for (int v : registers) {
            out.write((v >> 8) & 0xFF);
            out.write(v & 0xFF);
        }
        return BotoModbusUtil.appendCrc(out.toByteArray());
    }

    @Test
    void testKnownRequestCrcVector() {
        // Канонический кадр из реального обмена: 01 03 00 0A 00 01 A4 08
        byte[] frame = BotoModbusUtil.buildReadHoldingRequest(1, 10, 1);
        assertEquals("01 03 00 0A 00 01 A4 08", BotoModbusUtil.bytesToHex(frame));
    }

    @Test
    void testKnownResponseCrcVector() {
        // Реальный ответ камеры: 01 03 02 00 83 F9 E5 (рег 10 = 0x0083 = 131 -> 13.1°C)
        byte[] frame = readResponse(new int[]{0x0083});
        assertEquals("01 03 02 00 83 F9 E5", BotoModbusUtil.bytesToHex(frame));
        assertTrue(BotoModbusUtil.validateCrc(frame));
    }

    @Test
    void testBlockReadLayout() {
        // Запрос блока реального времени: рег 10..31 (22 регистра)
        int quantity = MODE_REG - TEMP_REG + 1;
        assertEquals(22, quantity);
        byte[] frame = BotoModbusUtil.buildReadHoldingRequest(1, TEMP_REG, quantity);
        assertEquals("01 03 00 0A 00 16 E4 06", BotoModbusUtil.bytesToHex(frame));
        // Запрос 0x03 всегда фиксированной длины 8 байт (адрес, функция, рег, колич., CRC).
        assertEquals(8, BotoModbusUtil.expectedRequestFrameLength(frame));
    }

    @Test
    void testBlockParseOffsets() {
        // Блок реального времени (рег 10..31):
        // 10=0x0083 (13.1°C), 11=0x00FA (25.0°C), 12=0, 13=0, 14=0x02BC (70.0%), 15=0x01F4 (50.0%),
        // 16..30 = 0, 31=1 (режим ВКЛ).
        int[] regs = new int[22];
        regs[0] = 0x0083;
        regs[1] = 0x00FA;
        regs[4] = 0x02BC;
        regs[5] = 0x01F4;
        regs[MODE_REG - TEMP_REG] = 1;

        byte[] frame = readResponse(regs);
        assertEquals(3 + 22 * 2 + 2, frame.length);
        assertEquals(3 + 22 * 2 + 2, BotoModbusUtil.expectedFrameLength(frame));

        int[] values = BotoModbusUtil.parseReadResponse(frame);
        assertNotNull(values);
        assertEquals(22, values.length);

        int scale = 10;
        assertEquals(13.1, values[0] / (double) scale, 0.001);   // TEMP_PV
        assertEquals(25.0, values[1] / (double) scale, 0.001);   // TEMP_SP
        assertEquals(70.0, values[4] / (double) scale, 0.001);   // HUMI_PV
        assertEquals(50.0, values[5] / (double) scale, 0.001);   // HUMI_SP
        assertEquals(1, values[MODE_REG - TEMP_REG]);            // реж. рег 31
    }

    @Test
    void testSingleReadParse() {
        // Одиночное чтение рег 10 (как в реальном обмене): значение 0x0083
        byte[] frame = readResponse(new int[]{0x0083});
        int[] values = BotoModbusUtil.parseReadResponse(frame);
        assertNotNull(values);
        assertEquals(1, values.length);
        assertEquals(0x0083, values[0]);
    }

    @Test
    void testBlockParseBadCrc() {
        int[] regs = new int[22];
        byte[] frame = readResponse(regs);
        frame[frame.length - 1] ^= 0x01;
        assertFalse(BotoModbusUtil.validateCrc(frame));
        assertNull(BotoModbusUtil.parseReadResponse(frame));
    }

    @Test
    void testWriteSingleEchoParse() {
        // Эхо 0x06 на запись уставки в рег 60: [01 06 00 3C 00 FA CRC]
        byte[] body = new byte[]{0x01, 0x06, 0x00, 0x3C, 0x00, (byte) 0xFA};
        byte[] frame = BotoModbusUtil.appendCrc(body);
        int[] parsed = BotoModbusUtil.parseWriteResponse(frame);
        assertNotNull(parsed);
        assertEquals(60, parsed[0]);
        assertEquals(0x00FA, parsed[1]);
    }
}