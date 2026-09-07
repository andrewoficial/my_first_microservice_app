package parsers.com.tt5166;

import org.example.device.protTt5166.TT5166CommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class TT5166CommandRegistryTest {

    private final TestPacketFactory factory = new TestPacketFactory();
    private TT5166CommandRegistry reg;

    private Method parseStateMethod;
    private Method parseProgramTimeMethod;
    private Method parseAdditionalChannelsMethod;

    @BeforeEach
    public void setUp() throws NoSuchMethodException {
        reg = new TT5166CommandRegistry();
        parseStateMethod = TT5166CommandRegistry.class.getDeclaredMethod("parseStateResponse", byte[].class);
        parseStateMethod.setAccessible(true);
        parseProgramTimeMethod = TT5166CommandRegistry.class.getDeclaredMethod("parseProgramTimeResponse", byte[].class);
        parseProgramTimeMethod.setAccessible(true);
        parseAdditionalChannelsMethod = TT5166CommandRegistry.class.getDeclaredMethod("parseAdditionalChannelsResponse", byte[].class);
        parseAdditionalChannelsMethod.setAccessible(true);
    }

    private AnswerValues invoke(Method m, byte[] response) throws InvocationTargetException, IllegalAccessException {
        return (AnswerValues) m.invoke(reg, (Object) response);
    }

    // ---------- Построители кадров ----------

    @Test
    void testBuildReadRegisters_layout() {
        byte[] frame = reg.buildReadRegisters((byte) 0x01, 0x0006, 3);

        assertEquals(8, frame.length);
        assertEquals(0x01, frame[0] & 0xFF);
        assertEquals(0x03, frame[1] & 0xFF);
        assertEquals(0x00, frame[2] & 0xFF);
        assertEquals(0x06, frame[3] & 0xFF);
        assertEquals(0x00, frame[4] & 0xFF);
        assertEquals(0x03, frame[5] & 0xFF);
        assertTrue(reg.validateChecksum(frame));
    }

    @Test
    void testBuildReadRegisters_knownCrcVector() {
        byte[] frame = reg.buildReadRegisters((byte) 0x01, 0x0000, 10);
        // Канонический вектор Modbus: 01 03 00 00 00 0A -> CRC 0xCDC5
        assertEquals(0xC5, frame[6] & 0xFF);
        assertEquals(0xCD, frame[7] & 0xFF);
        assertEquals(0xCDC5, reg.calculateModbusCRC(frame, 0, 6));
    }

    @Test
    void testBuildForceCoil_on() {
        byte[] frame = reg.buildForceCoil((byte) 0x01, 0x0001, true);

        assertEquals(0x05, frame[1] & 0xFF);
        assertEquals(0x00, frame[2] & 0xFF);
        assertEquals(0x01, frame[3] & 0xFF);
        assertEquals(0xFF, frame[4] & 0xFF);
        assertEquals(0x00, frame[5] & 0xFF);
        assertTrue(reg.validateChecksum(frame));
    }

    @Test
    void testBuildForceCoil_off() {
        byte[] frame = reg.buildForceCoil((byte) 0x01, 0x0000, false);

        assertEquals(0x00, frame[4] & 0xFF);
        assertTrue(reg.validateChecksum(frame));
    }

    @Test
    void testBuildWriteRegister_value() {
        byte[] frame = reg.buildWriteRegister((byte) 0x01, 0x0026, (short) (25.5 * 10));

        assertEquals(0x06, frame[1] & 0xFF);
        assertEquals(0x00, frame[2] & 0xFF);
        assertEquals(0x26, frame[3] & 0xFF);
        assertEquals(0x00, frame[4] & 0xFF);
        assertEquals(0xFF, frame[5] & 0xFF); // 255
        assertTrue(reg.validateChecksum(frame));
    }

    // ---------- CRC и проверка контрольной суммы ----------

    @Test
    void testCalculateModbusCRC_offsetLength() {
        byte[] frame = reg.buildReadRegisters((byte) 0x03, 0x0100, 1);
        int crc = reg.calculateModbusCRC(frame, 0, 6);
        // CRC должен зависеть только от 6 служебных байт, не от CRC-поля
        assertEquals(crc, reg.calculateModbusCRC(frame, 0, frame.length - 2));
        assertNotEquals(0, crc);
    }

    @Test
    void testValidateChecksum_ok() {
        byte[] frame = reg.buildForceCoil((byte) 0x01, 0x0000, true);
        assertTrue(reg.validateChecksum(frame));
    }

    @Test
    void testValidateChecksum_corrupted() {
        byte[] frame = reg.buildForceCoil((byte) 0x01, 0x0000, true);
        frame[5] ^= 0x01;
        assertFalse(reg.validateChecksum(frame));
    }

    @Test
    void testValidateChecksum_tooShort() {
        assertFalse(reg.validateChecksum(new byte[]{0x01, 0x03, 0x02}));
        assertFalse(reg.validateChecksum(new byte[0]));
    }

    // ---------- parseDataResponse ----------

    @Test
    void testParseData_fullValues() {
        byte[] payload = new byte[]{
                0x0B, (byte) 0xDE, // temp PV 0x0BDE = 3038 -> 30.38
                0x02, (byte) 0x94, // temp SV 0x0294 = 660  -> 66.0
                0x03, (byte) 0xE8, // temp Out 0x03E8 = 1000 -> 100.0
                0x01, (byte) 0xF4, // hum PV 0x01F4 = 500   -> 50.0
                0x02, (byte) 0x58, // hum SV 0x0258 = 600   -> 60.0
                0x02, (byte) 0xBC  // hum Out 0x02BC = 700  -> 70.0
        };

        byte[] frame = factory.okFrame((byte) 1, (byte) 3, payload);

        factory.assertOk(frame, reg::parseDataResponse, v -> {
            assertEquals(6, v.getValues().length);
            assertEquals(30.38, v.getValues()[0], 0.0001);
            assertEquals(66.0, v.getValues()[1], 0.0001);
            assertEquals(100.0, v.getValues()[2], 0.0001);
            assertEquals(50.0, v.getValues()[3], 0.0001);
            assertEquals(60.0, v.getValues()[4], 0.0001);
            assertEquals(70.0, v.getValues()[5], 0.0001);
            assertEquals("Температура PV (°C)", v.getUnits()[0]);
            assertEquals("Влажность SV (% RH)", v.getUnits()[4]);
            return true;
        });
    }

    @Test
    void testParseData_negativeTemperature() {
        byte[] payload = new byte[]{
                (byte) 0xFE, 0x0C, // -500 -> -5.0
                0x01, (byte) 0xF4,
                0x00, 0x00,
                0x00, (byte) 0x32,
                0x00, (byte) 0x32,
                0x00, 0x00
        };

        byte[] frame = factory.okFrame((byte) 1, (byte) 3, payload);

        factory.assertOk(frame, reg::parseDataResponse, v -> {
            assertEquals(-5.0, v.getValues()[0], 0.0001);
            assertEquals(5.0, v.getValues()[3], 0.0001); // 0x0032 = 50 / 10
            return true;
        });
    }

    @Test
    void testParseData_tooShort() {
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x01});
        assertNull(reg.parseDataResponse(frame));
        assertNull(reg.parseDataResponse(new byte[]{0x01, 0x03}));
    }

    @Test
    void testParseData_wrongHeader() {
        byte[] frame = factory.wrongHeaderFrame((byte) 2, (byte) 3, new byte[12]);
        assertNull(reg.parseDataResponse(frame));
    }

    @Test
    void testParseData_badCrc() {
        byte[] frame = factory.badCrcFrame((byte) 1, (byte) 3, new byte[12]);
        assertNull(reg.parseDataResponse(frame));
    }

    @Test
    void testParseData_wrongByteCount() {
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[10]);
        assertNull(reg.parseDataResponse(frame));
    }

    // ---------- parseFaultResponse ----------

    @Test
    void testParseFault_knownCode() {
        // 85 = "The heating rate is too slow"
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x55});

        factory.assertOk(frame, reg::parseFaultResponse, v -> {
            assertEquals(1, v.getValues().length);
            assertEquals(85.0, v.getValues()[0], 0.0001);
            assertTrue(v.getUnits()[0].contains("heating rate"));
            return true;
        });
    }

    @Test
    void testParseFault_compressorCode() {
        // 4 = "CM2 compressor overcurrent trip"
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x04});

        factory.assertOk(frame, reg::parseFaultResponse, v -> {
            assertEquals(4.0, v.getValues()[0], 0.0001);
            assertTrue(v.getUnits()[0].contains("CM2"));
            return true;
        });
    }

    @Test
    void testParseFault_unknownCode() {
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x03, (byte) 0xE7}); // 999

        factory.assertOk(frame, reg::parseFaultResponse, v -> {
            assertEquals(999.0, v.getValues()[0], 0.0001);
            assertTrue(v.getUnits()[0].contains("Unknown fault code"));
            return true;
        });
    }

    @Test
    void testParseFault_errors() {
        byte[] badCrc = factory.badCrcFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x01});
        assertNull(reg.parseFaultResponse(badCrc));
        assertNull(reg.parseFaultResponse(factory.okFrame((byte) 1, (byte) 4, new byte[]{0x00, 0x01})));
        assertNull(reg.parseFaultResponse(factory.okFrame((byte) 1, (byte) 3, new byte[4])));
        assertNull(reg.parseFaultResponse(new byte[]{0x01, 0x03}));
    }

    // ---------- parseAckResponse ----------

    private byte[] ackFrame(byte func, byte[] body) {
        byte[] frame = new byte[6 + body.length];
        frame[0] = 0x01;
        frame[1] = func;
        System.arraycopy(body, 0, frame, 2, body.length);
        int crc = reg.calculateModbusCRC(frame, 0, 6);
        frame[6] = (byte) (crc & 0xFF);
        frame[7] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    private byte[] coilAck(boolean on) {
        return ackFrame((byte) 0x05, new byte[]{0x00, 0x00, on ? (byte) 0xFF : 0x00, 0x00});
    }

    private byte[] writeRegAck(short value) {
        return ackFrame((byte) 0x06, new byte[]{0x00, 0x26, (byte) (value >> 8), (byte) (value & 0xFF)});
    }

    @Test
    void testParseAck_coilSuccess() {
        AnswerValues res = reg.parseAckResponse(coilAck(true));
        assertEquals(1.0, res.getValues()[0], 0.0001);
        assertEquals("Успех", res.getUnits()[0]);
    }

    @Test
    void testParseAck_writeRegisterSuccess() {
        AnswerValues res = reg.parseAckResponse(writeRegAck((short) 255));
        assertEquals(1.0, res.getValues()[0], 0.0001);
        assertEquals("Успех", res.getUnits()[0]);
    }

    @Test
    void testParseAck_coilOff() {
        AnswerValues res = reg.parseAckResponse(coilAck(false));
        assertEquals(1.0, res.getValues()[0], 0.0001);
    }

    private byte[] exceptionFrame(int func, int errorCode) {
        byte[] frame = new byte[]{0x01, (byte) func, (byte) errorCode, 0x00, 0x00};
        int crc = reg.calculateModbusCRC(frame, 0, 3);
        frame[3] = (byte) (crc & 0xFF);
        frame[4] = (byte) ((crc >> 8) & 0xFF);
        return frame;
    }

    @Test
    void testParseAck_exceptionResponse() {
        // Реальный Modbus exception-ответ: [addr][func+0x80][code][crcLo][crcHi]
        AnswerValues res = reg.parseAckResponse(exceptionFrame(0x85, 0x02));
        assertEquals(-1.0, res.getValues()[0], 0.0001);
        assertTrue(res.getUnits()[0].contains("код 2"));
    }

    @Test
    void testParseAck_exceptionWriteRegister() {
        AnswerValues res = reg.parseAckResponse(exceptionFrame(0x86, 0x03));
        assertEquals(-1.0, res.getValues()[0], 0.0001);
        assertTrue(res.getUnits()[0].contains("код 3"));
    }

    @Test
    void testParseAck_unexpectedFunction() {
        byte[] frame = ackFrame((byte) 0x04, new byte[]{0x00, 0x00, 0x00, 0x00});
        AnswerValues res = reg.parseAckResponse(frame);
        assertEquals(-1.0, res.getValues()[0], 0.0001);
        assertTrue(res.getUnits()[0].contains("Неожиданный тип ответа"));
    }

    @Test
    void testParseAck_wrongAddress() {
        byte[] frame = {0x02, 0x05, 0x00, 0x00, (byte) 0xFF, 0x00, 0x00, 0x00};
        AnswerValues res = reg.parseAckResponse(frame);
        assertEquals(-1.0, res.getValues()[0], 0.0001);
        assertTrue(res.getUnits()[0].contains("адрес"));
    }

    @Test
    void testParseAck_tooShort() {
        AnswerValues res = reg.parseAckResponse(new byte[]{0x01, 0x05, 0x00});
        assertEquals(-1.0, res.getValues()[0], 0.0001);
        assertTrue(res.getUnits()[0].contains("короткий"));
    }

    @Test
    void testParseAck_badCrc() {
        byte[] frame = coilAck(true);
        frame[6] ^= 0x55;
        AnswerValues res = reg.parseAckResponse(frame);
        assertEquals(-1.0, res.getValues()[0], 0.0001);
        assertTrue(res.getUnits()[0].contains("CRC"));
    }

    // ---------- parseStateResponse (приватный, через рефлексию) ----------

    @Test
    void testParseState_runningProgramMode() throws Exception {
        // runState = 0x0001 (bit0 = 1 -> Running), mode = 0x0001 -> Program mode
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x01, 0x00, 0x01});

        AnswerValues res = invoke(parseStateMethod, frame);
        assertNotNull(res);
        assertEquals(2, res.getValues().length);
        assertTrue(res.getUnits()[0].contains("Running"));
        assertTrue(res.getUnits()[1].contains("Program mode"));
    }

    @Test
    void testParseState_stoppedFixedMode() throws Exception {
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x02, (byte) 0xFF, 0x00});

        AnswerValues res = invoke(parseStateMethod, frame);
        assertNotNull(res);
        assertTrue(res.getUnits()[0].contains("Stopped"));
        assertTrue(res.getUnits()[1].contains("Fixed mode"));
    }

    @Test
    void testParseState_singleRegister() throws Exception {
        // Только регистр H0018 — быстрый вариант ответа
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x01});

        AnswerValues res = invoke(parseStateMethod, frame);
        assertNotNull(res);
        assertEquals(1.0, res.getValues()[0], 0.0001); // raw H0018 = 0x0001
        assertTrue(res.getUnits()[0].contains("Running"));
        assertNull(res.getUnits()[1]); // второй регистр не запрашивался
    }

    @Test
    void testParseState_errors() throws Exception {
        assertNull(invoke(parseStateMethod, factory.badCrcFrame((byte) 1, (byte) 3, new byte[]{0x00, 0x01, 0x00, 0x01})));
        assertNull(invoke(parseStateMethod, factory.okFrame((byte) 1, (byte) 4, new byte[]{0x00, 0x01, 0x00, 0x01})));
        assertNull(invoke(parseStateMethod, factory.okFrame((byte) 1, (byte) 3, new byte[6])));
        assertNull(invoke(parseStateMethod, new byte[]{0x01, 0x03}));
    }

    // ---------- parseProgramTimeResponse (приватный) ----------

    @Test
    void testParseProgramTime_hoursAndMinutes() throws Exception {
        // H0006 = 0x000A (10ч), H0007 не используется, H0008 = 0x002D (45мин)
        byte[] frame = factory.okFrame((byte) 1, (byte) 3,
                new byte[]{0x00, 0x0A, 0x00, 0x00, 0x00, 0x2D});

        AnswerValues res = invoke(parseProgramTimeMethod, frame);
        assertNotNull(res);
        assertEquals(2, res.getValues().length);
        assertEquals(10.0, res.getValues()[0], 0.0001);
        assertEquals(45.0, res.getValues()[1], 0.0001);
        assertTrue(res.getUnits()[0].contains("часы"));
        assertTrue(res.getUnits()[1].contains("минуты"));
    }

    @Test
    void testParseProgramTime_zero() throws Exception {
        byte[] frame = factory.okFrame((byte) 1, (byte) 3, new byte[6]);

        AnswerValues res = invoke(parseProgramTimeMethod, frame);
        assertNotNull(res);
        assertEquals(0.0, res.getValues()[0], 0.0001);
        assertEquals(0.0, res.getValues()[1], 0.0001);
    }

    @Test
    void testParseProgramTime_errors() throws Exception {
        assertNull(invoke(parseProgramTimeMethod, factory.badCrcFrame((byte) 1, (byte) 3, new byte[6])));
        assertNull(invoke(parseProgramTimeMethod, factory.okFrame((byte) 1, (byte) 3, new byte[4])));
        assertNull(invoke(parseProgramTimeMethod, new byte[]{0x01, 0x03, 0x06}));
    }

    // ---------- parseAdditionalChannelsResponse (приватный) ----------

    @Test
    void testParseAdditionalChannels_values() throws Exception {
        // Канал2 = 0x012C = 300 -> 30.0, Канал3 = 0x00C8 = 200 -> 20.0, Канал4 = 0x00FA = 250 -> 25.0
        byte[] frame = factory.okFrame((byte) 1, (byte) 3,
                new byte[]{0x01, 0x2C, 0x00, (byte) 0xC8, 0x00, (byte) 0xFA});

        AnswerValues res = invoke(parseAdditionalChannelsMethod, frame);
        assertNotNull(res);
        assertEquals(3, res.getValues().length);
        assertEquals(30.0, res.getValues()[0], 0.0001);
        assertEquals(20.0, res.getValues()[1], 0.0001);
        assertEquals(25.0, res.getValues()[2], 0.0001);
        assertTrue(res.getUnits()[0].contains("Канал 2"));
    }

    @Test
    void testParseAdditionalChannels_errors() throws Exception {
        assertNull(invoke(parseAdditionalChannelsMethod, factory.badCrcFrame((byte) 1, (byte) 3, new byte[6])));
        assertNull(invoke(parseAdditionalChannelsMethod, factory.okFrame((byte) 1, (byte) 3, new byte[4])));
        assertNull(invoke(parseAdditionalChannelsMethod, factory.okFrame((byte) 1, (byte) 4, new byte[6])));
    }
}