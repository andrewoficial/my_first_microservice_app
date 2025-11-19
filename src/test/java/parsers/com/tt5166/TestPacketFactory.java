package parsers.com.tt5166;

import org.example.device.protTt5166.TT5166CommandRegistry;
import java.util.function.Function;

public class TestPacketFactory {

    private final TT5166CommandRegistry reg = new TT5166CommandRegistry();

    /**
     * Создаёт Modbus RTU фрейм с корректным CRC.
     */
    public byte[] okFrame(byte addr, byte func, byte[] payload) {
        byte[] resp = new byte[3 + payload.length + 2];
        resp[0] = addr;
        resp[1] = func;
        resp[2] = (byte) payload.length;

        System.arraycopy(payload, 0, resp, 3, payload.length);

        int crc = reg.calculateModbusCRC(resp, 0, resp.length - 2);
        resp[resp.length - 2] = (byte) (crc & 0xFF);
        resp[resp.length - 1] = (byte) ((crc >> 8) & 0xFF);

        return resp;
    }

    /**
     * Создаёт фрейм с битым CRC.
     */
    public byte[] badCrcFrame(byte addr, byte func, byte[] payload) {
        byte[] resp = okFrame(addr, func, payload);
        resp[resp.length - 2] ^= 0x55; // ломаем CRC
        return resp;
    }

    /**
     * Создаёт фрейм с неверным Modbus заголовком (например wrong func).
     */
    public byte[] wrongHeaderFrame(byte addr, byte badFunc, byte[] payload) {
        return okFrame(addr, badFunc, payload);
    }

    /**
     * Универсальный тест-раннер.
     */
    public <T> void assertOk(byte[] frame,
                             Function<byte[], T> parser,
                             Function<T, Boolean> assertion) {

        T res = parser.apply(frame);
        if (res == null)
            throw new AssertionError("Parser returned null");

        if (!assertion.apply(res))
            throw new AssertionError("Assertion failed: " + res);
    }

    /**
     * Тест, который должен провалиться (например, CRC error).
     */
    public <T> void assertFail(byte[] frame,
                               Function<byte[], T> parser) {

        T res = parser.apply(frame);
        if (res != null)
            throw new AssertionError("Expected parser to return null or fail, but got: " + res);
    }
}
