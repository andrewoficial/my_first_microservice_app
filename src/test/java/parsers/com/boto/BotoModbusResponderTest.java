package parsers.com.boto;

import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.devices.boto.emulation.BotoEmulator;
import org.example.gui.devices.boto.emulation.BotoModbusResponder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Проверка новых кандидатов-регистров B-TH-800F:
 * 18 — поддержка влаги, 19 — подсветка, 20 — флаги ошибок.
 */
public class BotoModbusResponderTest {

    private BotoEmulator emulator;
    private BotoModbusResponder responder;

    @BeforeEach
    void setUp() {
        emulator = new BotoEmulator();
        responder = new BotoModbusResponder(emulator, 1, 10, 60, 63, 10);
    }

    private int readReg(int reg) {
        byte[] req = BotoModbusUtil.buildReadHoldingRequest(1, reg, 1);
        byte[] resp = responder.processRequest(req);
        int[] v = BotoModbusUtil.parseReadResponse(resp);
        assertNotNull(v);
        return v[0];
    }

    private void writeReg(int reg, int value) {
        byte[] req = BotoModbusUtil.buildWriteSingleRequest(1, reg, value);
        byte[] resp = responder.processRequest(req);
        assertNotNull(resp);
        // Эхо 0x06
        assertEquals(0x06, resp[1] & 0xFF);
        assertEquals(reg, ((resp[2] & 0xFF) << 8) | (resp[3] & 0xFF));
        assertEquals(value, ((resp[4] & 0xFF) << 8) | (resp[5] & 0xFF));
    }

    @Test
    void testHumidityEnableWriteAndRead() {
        assertEquals(0, readReg(BotoModbusResponder.REG_HUMI_EN)); // по умолчанию выкл, как реальная камера
        writeReg(BotoModbusResponder.REG_HUMI_EN, 1);
        assertEquals(1, readReg(BotoModbusResponder.REG_HUMI_EN));
        assertTrue(emulator.isHumidityEnabled());
    }

    @Test
    void testRealCameraReferenceBlock() {
        // Эталонный блок реальной камеры B-TH-800F (рег 10..49), снятый с неё:
        // R12/R16 — зеркала уставок, R30 = 1, R35/R38..R42 — константы.
        emulator.setCurrentTempC(24.9);
        emulator.setSetpointC(24.0);
        emulator.setCurrentHumidity(72.8);
        emulator.setHumiditySetpoint(55.0);
        emulator.setOn(false);

        byte[] req = BotoModbusUtil.buildReadHoldingRequest(1, 10, 40);
        byte[] resp = responder.processRequest(req);
        int[] v = BotoModbusUtil.parseReadResponse(resp);
        assertNotNull(v);
        assertEquals(40, v.length);

        assertEquals(249, v[0]);            // R10 TEMP_PV (24.9°C)
        assertEquals(240, v[1]);            // R11 TEMP_SP
        assertEquals(v[1], v[2]);           // R12 = зеркало TEMP_SP
        assertEquals(728, v[4]);            // R14 HUMI_PV (72.8%)
        assertEquals(550, v[5]);            // R15 HUMI_SP
        assertEquals(v[5], v[6]);           // R16 = зеркало HUMI_SP
        assertEquals(0, v[8]);              // R18
        assertEquals(0, v[9]);              // R19 (лампа выкл — камера не работает)
        assertEquals(0, v[10]);             // R20 ошибки
        assertEquals(1, v[20]);             // R30 = 1 всегда
        assertEquals(0, v[21]);             // R31 = режим (выкл)
        assertEquals(41, v[25]);            // R35
        assertEquals(509, v[28]);           // R38
        assertEquals(4614, v[29]);          // R39
        assertEquals(3584, v[30]);          // R40
        assertEquals(3, v[31]);             // R41
        assertEquals(0xE800, v[32]);        // R42
    }

    @Test
    void testLightWriteAndRead() {
        assertEquals(0, readReg(BotoModbusResponder.REG_LIGHT));   // по умолчанию выкл
        writeReg(BotoModbusResponder.REG_LIGHT, 1);
        assertEquals(1, readReg(BotoModbusResponder.REG_LIGHT));
        assertTrue(emulator.isLightOn());
    }

    @Test
    void testErrorFlagsRead() {
        emulator.setErrorFlag(2, true);
        emulator.setErrorFlag(5, true);
        assertEquals(0x24, readReg(BotoModbusResponder.REG_ERROR_FLAGS));
        emulator.setErrorFlag(2, false);
        assertEquals(0x20, readReg(BotoModbusResponder.REG_ERROR_FLAGS));
    }

    @Test
    void testBlockReadContainsNewRegisters() {
        // Блок 10..31: рег 18 (инд.8) — влага, 19 (инд.9) — подсветка, 20 (инд.10) — ошибки.
        emulator.setHumidityEnabled(false);
        emulator.setLightOn(true);
        emulator.setErrorFlags(0x81);

        byte[] req = BotoModbusUtil.buildReadHoldingRequest(1, 10, 22);
        byte[] resp = responder.processRequest(req);
        int[] v = BotoModbusUtil.parseReadResponse(resp);
        assertNotNull(v);
        assertEquals(22, v.length);

        int base = 10;
        assertEquals(0, v[BotoModbusResponder.REG_HUMI_EN - base]);
        assertEquals(1, v[BotoModbusResponder.REG_LIGHT - base]);
        assertEquals(0x81, v[BotoModbusResponder.REG_ERROR_FLAGS - base]);
    }

    @Test
    void testHumidityMaintainedEvenWhenSupportOff() {
        // Реальная камера поддерживает влагу при работе независимо от «рег 18».
        emulator.setCurrentHumidity(80.0);
        emulator.setHumiditySetpoint(50.0);
        emulator.setOn(true);
        emulator.setHumidityEnabled(false);          // запись рег 18 — эффекта не даёт
        emulator.advance(0.5);
        assertEquals(79.5, emulator.getCurrentHumidity(), 0.001);
    }

    @Test
    void testLightFollowsRunState() {
        emulator.setOn(true);
        assertTrue(emulator.isLightOn());
        assertEquals(1, readReg(BotoModbusResponder.REG_LIGHT));
        emulator.setOn(false);
        assertFalse(emulator.isLightOn());
        assertEquals(0, readReg(BotoModbusResponder.REG_LIGHT));
    }

    @Test
    void testHumidityEnabledRampsToSetpoint() {
        emulator.setCurrentHumidity(80.0);
        emulator.setHumiditySetpoint(50.0);
        emulator.setOn(true);
        emulator.setHumidityEnabled(true);
        emulator.advance(0.5);
        // При включённой поддержке влага движется к уставке (1.0 %/сек)
        assertEquals(79.5, emulator.getCurrentHumidity(), 0.001);
    }
}