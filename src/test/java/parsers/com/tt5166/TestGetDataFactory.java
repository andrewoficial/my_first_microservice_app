package parsers.com.tt5166;

import org.example.device.protTt5166.TT5166CommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestGetDataFactory {

    private final TestPacketFactory factory = new TestPacketFactory();
    private final TT5166CommandRegistry reg = new TT5166CommandRegistry();

    @Test
    void testGetData_ok() {
        byte[] payload = new byte[]{
                0x0B, (byte) 0xDE,
                0x02, (byte) 0x94,
                0x03, (byte) 0xE8,
                0x01, (byte) 0xF4,
                0x02, (byte) 0x58,
                0x02, (byte) 0xBC
        };

        byte[] frame = factory.okFrame((byte) 1, (byte) 3, payload);

        factory.assertOk(frame, reg::parseDataResponse, v -> {
            assertEquals(30.38, v.getValues()[0]);
            assertEquals(66.0,   v.getValues()[1]);
            assertEquals(100.0,  v.getValues()[2]);
            return true;
        });
    }

    @Test
    void testGetData_badCrc() {

        byte[] payload = new byte[]{
                0x0B, (byte) 0xDE,
                0x02, (byte) 0x94,
                0x03, (byte) 0xE8,
                0x01, (byte) 0xF4,
                0x02, (byte) 0x58,
                0x02, (byte) 0xBC
        };

        byte[] badFrame = factory.badCrcFrame((byte)1, (byte)3, payload);

        factory.assertFail(badFrame, reg::parseDataResponse);
    }

    @Test
    void testGetData_wrongFunction() {
        byte[] payload = new byte[12];
        byte[] frame = factory.wrongHeaderFrame((byte)1, (byte)4, payload);

        factory.assertFail(frame, reg::parseDataResponse);
    }

    @Test
    void testGetData_wrongByteCount() {
        byte[] payload = new byte[]{0x00, 0x01}; // только 1 регистр

        byte[] frame = factory.okFrame((byte)1, (byte)3, payload);

        factory.assertFail(frame, reg::parseDataResponse);
    }

    @Test
    void testFault_ok() {

        // faultCode 17 = "power due to phase…"
        byte[] payload = new byte[]{0x00, 0x11};

        byte[] frame = factory.okFrame((byte)1, (byte)3, payload);

        factory.assertOk(frame, reg::parseFaultResponse, v -> {
            assertTrue(v.getUnits()[0].contains("phase"));
            return true;
        });
    }

    @Test
    void testAck_ok() {
        byte[] frame = reg.buildForceCoil((byte)1, 0x0000, true);

        factory.assertOk(frame, reg::parseAckResponse, v -> {
            assertEquals(1.0, v.getValues()[0]);
            return true;
        });
    }

}
