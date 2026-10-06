package org.example.device.protAcu10fd;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Кадры ACU10-FD: чтение/запись holding-регистров и float в порядке CDAB.
 * Транспорт (COM-порт) сюда не входит.
 */
public final class Acu10fdFrames {

    private Acu10fdFrames() {
    }

    public static byte[] readHoldingRegisters(byte address, int register, int registerCount) {
        int regHigh = (register >> 8) & 0xFF;
        int regLow = register & 0xFF;
        int countHigh = (registerCount >> 8) & 0xFF;
        int countLow = registerCount & 0xFF;
        ByteBuffer buf = ByteBuffer.allocate(6)
                .put(address)
                .put((byte) 0x03)
                .put((byte) regHigh)
                .put((byte) regLow)
                .put((byte) countHigh)
                .put((byte) countLow);
        return addCrc(buf.array());
    }

    public static byte[] writeFloatCdab(byte address, int register, float value) {
        byte[] floatBytes = ByteBuffer.allocate(4)
                .order(ByteOrder.BIG_ENDIAN)
                .putFloat(value)
                .array();
        byte[] swappedFloatBytes = new byte[4];
        swappedFloatBytes[0] = floatBytes[2];
        swappedFloatBytes[1] = floatBytes[3];
        swappedFloatBytes[2] = floatBytes[0];
        swappedFloatBytes[3] = floatBytes[1];
        ByteBuffer buf = ByteBuffer.allocate(11)
                .order(ByteOrder.BIG_ENDIAN)
                .put(address)
                .put((byte) 0x10)
                .putShort((short) register)
                .putShort((short) 2)
                .put((byte) 4)
                .put(swappedFloatBytes);
        return addCrc(buf.array());
    }

    public static float parseFloatCdab(byte[] response, int offset) {
        if (response.length < offset + 4) {
            throw new IllegalArgumentException("Invalid response length");
        }
        byte[] cdab = Arrays.copyOfRange(response, offset, offset + 4);
        byte[] abcd = {cdab[2], cdab[3], cdab[0], cdab[1]};
        return ByteBuffer.wrap(abcd).order(ByteOrder.BIG_ENDIAN).getFloat();
    }

    public static byte[] addCrc(byte[] data) {
        int crc = crc16(data);
        byte[] result = Arrays.copyOf(data, data.length + 2);
        result[result.length - 2] = (byte) (crc & 0xFF);
        result[result.length - 1] = (byte) ((crc >> 8) & 0xFF);
        return result;
    }

    public static boolean checkCrc(byte[] data) {
        if (data.length < 3) {
            return false;
        }
        byte[] withoutCrc = Arrays.copyOf(data, data.length - 2);
        int calculatedCrc = crc16(withoutCrc);
        int receivedCrc = (data[data.length - 1] & 0xFF) << 8 | (data[data.length - 2] & 0xFF);
        return calculatedCrc == receivedCrc;
    }

    public static int crc16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return crc;
    }
}
