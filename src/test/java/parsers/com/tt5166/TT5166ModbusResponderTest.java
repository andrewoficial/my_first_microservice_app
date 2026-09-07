package parsers.com.tt5166;

import org.example.gui.devices.tt5166.emulation.TT5166Emulator;
import org.example.gui.devices.tt5166.emulation.TT5166ModbusResponder;
import org.example.gui.devices.tt5166.emulation.TT5166ModbusUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

public class TT5166ModbusResponderTest {

    private TT5166Emulator emulator;
    private TT5166ModbusResponder responder;

    @BeforeEach
    void setUp() {
        emulator = new TT5166Emulator();
        responder = new TT5166ModbusResponder(emulator, 1);
    }

    /** Запрос: [addr][func][reg(2)][value/qty(2)] + CRC */
    private byte[] request(int addr, int func, int reg, int value) {
        ByteBuffer buf = ByteBuffer.allocate(8);
        buf.order(ByteOrder.BIG_ENDIAN);
        buf.put((byte) addr);
        buf.put((byte) func);
        buf.putShort((short) reg);
        buf.putShort((short) value);
        return TT5166ModbusUtil.appendCrc(buf.array());
    }

    private int readReg(byte[] resp, int idx) {
        return ((resp[3 + 2 * idx] & 0xFF) << 8) | (resp[4 + 2 * idx] & 0xFF);
    }

    @Test
    void testReadRegisters_valuesAndScale() {
        emulator.setOn(true);
        emulator.setCurrentTempC(30.25);
        emulator.setSetpointC(40.0);
        emulator.setCurrentHumidity(55.5);
        emulator.setHumiditySetpoint(60.0);

        byte[] resp = responder.processRequest(request(1, 0x03, 0x0000, 6));

        assertNotNull(resp);
        assertEquals(17, resp.length);
        assertEquals(0x03, resp[1] & 0xFF);
        assertEquals(12, resp[2] & 0xFF);
        assertTrue(TT5166ModbusUtil.validateCrc(resp));

        assertEquals(3025, readReg(resp, 0));  // temp PV ×100
        assertEquals(400, readReg(resp, 1));   // temp SV ×10
        assertEquals(555, readReg(resp, 3));   // hum PV ×10
        assertEquals(600, readReg(resp, 4));   // hum SV ×10
    }

    @Test
    void testReadRegister_stateAndFault() {
        emulator.setOn(true);
        emulator.setProgramMode(true);
        emulator.setFaultCode(17);

        byte[] resp = responder.processRequest(request(1, 0x03, 0x0018, 2));
        assertNotNull(resp);
        assertEquals(1, readReg(resp, 0));
        assertEquals(1, readReg(resp, 1));

        byte[] fault = responder.processRequest(request(1, 0x03, 0x001B, 1));
        assertNotNull(fault);
        assertEquals(17, readReg(fault, 0));
    }

    @Test
    void testWriteRegister_setpointApplied() {
        byte[] resp = responder.processRequest(request(1, 0x06, 0x0026, 255)); // 25.5 °C

        assertNotNull(resp);
        assertEquals(8, resp.length);
        assertEquals(0x06, resp[1] & 0xFF);
        assertTrue(TT5166ModbusUtil.validateCrc(resp));
        assertEquals(25.5, emulator.getSetpointC(), 0.001);
    }

    @Test
    void testWriteHumidityApplied() {
        responder.processRequest(request(1, 0x06, 0x0027, 620)); // 62.0 %
        assertEquals(62.0, emulator.getHumiditySetpoint(), 0.001);
    }

    @Test
    void testForceCoilStart() {
        byte[] resp = responder.processRequest(request(1, 0x05, 0x0000, 0xFF00));

        assertNotNull(resp);
        assertEquals(0x05, resp[1] & 0xFF);
        assertTrue(emulator.isOn());
    }

    @Test
    void testForceCoilStop() {
        emulator.setOn(true);
        responder.processRequest(request(1, 0x05, 0x0001, 0xFF00));
        assertFalse(emulator.isOn());
    }

    @Test
    void testUnknownFunction_exceptionResponse() {
        byte[] resp = responder.processRequest(request(1, 0x10, 0x0000, 0x0001));

        assertNotNull(resp);
        assertEquals(5, resp.length);
        assertEquals(0x90, resp[1] & 0xFF);
        assertEquals(0x01, resp[2] & 0xFF);
        assertTrue(TT5166ModbusUtil.validateCrc(resp));
    }

    @Test
    void testBadCrc_null() {
        byte[] req = request(1, 0x03, 0x0000, 1);
        req[req.length - 2] ^= 0x55;
        assertNull(responder.processRequest(req));
    }

    @Test
    void testWrongSlaveId_null() {
        assertNull(responder.processRequest(request(2, 0x03, 0x0000, 1)));
    }

    @Test
    void testManualRegisterOverrides() {
        responder.setManualRegister(0x0050, 1234);
        byte[] resp = responder.processRequest(request(1, 0x03, 0x0050, 1));
        assertNotNull(resp);
        assertEquals(1234, readReg(resp, 0));
    }
}