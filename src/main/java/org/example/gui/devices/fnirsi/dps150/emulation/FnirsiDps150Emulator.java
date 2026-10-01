package org.example.gui.devices.fnirsi.dps150.emulation;

import org.example.device.protFnirsiDps150.FnirsiDps150CommandRegistry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Чистая модель эмулируемого источника питания FNIRSI DPS150.
 *
 * <p>Принимает бинарные кадры {@code F1 ...} и строит ответы {@code F0 A1 ...}
 * ровно по тем же типам и единицам, что и {@link FnirsiDps150CommandRegistry}.
 */
public class FnirsiDps150Emulator {

    private static final byte START_SEND = (byte) 0xF1;
    private static final byte START_RECV = (byte) 0xF0;
    private static final byte CMD_GET = (byte) 0xA1;
    private static final byte CMD_SET = (byte) 0xB1;
    private static final byte CMD_SPECIAL = (byte) 0xC1;

    private volatile float voutSet = 0f;
    private volatile float ioutSet = 0f;
    private volatile int output = 0;
    private volatile int brightness = 10;

    private volatile float vin = 24.0f;
    private volatile float temp = 25.0f;

    private final float voutLimit = 30.0f;
    private final float ioutLimit = 5.0f;
    private final String modelName = "DPS-150";
    private final String swVersion = "V1.10";
    private final String hwVersion = "V1.0";

    private volatile float measuredVout = 0f;
    private volatile float measuredIout = 0f;

    public float getVoutSet() {
        return voutSet;
    }

    public void setVoutSet(float value) {
        this.voutSet = Math.max(0, Math.min(voutLimit, value));
    }

    public float getIoutSet() {
        return ioutSet;
    }

    public void setIoutSet(float value) {
        this.ioutSet = Math.max(0, Math.min(ioutLimit, value));
    }

    public int getOutput() {
        return output;
    }

    public void setOutput(int value) {
        this.output = value != 0 ? 1 : 0;
    }

    public int getBrightness() {
        return brightness;
    }

    public void setBrightness(int value) {
        this.brightness = Math.max(0, Math.min(14, value));
    }

    public float getVin() {
        return vin;
    }

    public void setVin(float value) {
        this.vin = value;
    }

    public float getMeasuredVout() {
        return measuredVout;
    }

    public float getMeasuredIout() {
        return measuredIout;
    }

    /** Шаг симуляции: измеренные значения догоняют уставки при включённом выходе. */
    public void advance() {
        float targetV = output == 1 ? voutSet : 0f;
        float targetI = output == 1 ? ioutSet : 0f;
        measuredVout = step(measuredVout, targetV, 1.5f);
        measuredIout = step(measuredIout, targetI, 0.3f);
        temp = step(temp, 24.0f + measuredVout * 0.05f + measuredIout * 0.4f, 0.2f);
    }

    private static float step(float current, float target, float maxDelta) {
        float delta = target - current;
        if (Math.abs(delta) <= maxDelta) {
            return target;
        }
        return current + Math.signum(delta) * maxDelta;
    }

    /**
     * Обработать запрос. Возвращает кадр ответа либо {@code null}, если кадр
     * не распознан.
     */
    public byte[] handle(byte[] request) {
        if (request == null || request.length < 5 || request[0] != START_SEND) {
            return null;
        }
        byte cmd = request[1];
        byte type = request[2];
        int len = request[3] & 0xFF;
        if (request.length < 4 + len + 1) {
            return null;
        }
        byte[] data = new byte[len];
        System.arraycopy(request, 4, data, 0, len);

        advance();

        if (cmd == CMD_GET) {
            return response(type);
        }
        if (cmd == CMD_SET) {
            apply(type, data);
            return response(type);
        }
        if (cmd == CMD_SPECIAL) {
            return frame(type, new byte[]{1});
        }
        return null;
    }

    private void apply(byte type, byte[] data) {
        if (type == FnirsiDps150CommandRegistry.TYPE_DEV_VOUT && data.length >= 4) {
            setVoutSet(readFloat(data, 0));
        } else if (type == FnirsiDps150CommandRegistry.TYPE_DEV_IOUT && data.length >= 4) {
            setIoutSet(readFloat(data, 0));
        } else if (type == FnirsiDps150CommandRegistry.TYPE_DEV_OUTPUT && data.length >= 1) {
            setOutput(data[0]);
        } else if (type == FnirsiDps150CommandRegistry.TYPE_BRIGHTNESS && data.length >= 1) {
            setBrightness(data[0]);
        }
    }

    private byte[] response(byte type) {
        switch (type) {
            case FnirsiDps150CommandRegistry.TYPE_DEV_MODEL:
                return frame(type, modelName.getBytes(StandardCharsets.US_ASCII));
            case FnirsiDps150CommandRegistry.TYPE_SW_VERSION:
                return frame(type, swVersion.getBytes(StandardCharsets.US_ASCII));
            case FnirsiDps150CommandRegistry.TYPE_HW_VERSION:
                return frame(type, hwVersion.getBytes(StandardCharsets.US_ASCII));
            case FnirsiDps150CommandRegistry.TYPE_DEV_VIN:
                return frame(type, floatLe(vin));
            case FnirsiDps150CommandRegistry.TYPE_VOUT_LIMIT:
                return frame(type, floatLe(voutLimit));
            case FnirsiDps150CommandRegistry.TYPE_IOUT_LIMIT:
                return frame(type, floatLe(ioutLimit));
            case FnirsiDps150CommandRegistry.TYPE_TEMP:
                return frame(type, floatLe(temp));
            case FnirsiDps150CommandRegistry.TYPE_POWER:
                return frame(type, powerData());
            case FnirsiDps150CommandRegistry.TYPE_DEV_OUTPUT:
                return frame(type, new byte[]{(byte) output});
            case FnirsiDps150CommandRegistry.TYPE_BRIGHTNESS:
                return frame(type, new byte[]{(byte) brightness});
            case FnirsiDps150CommandRegistry.TYPE_DEV_VOUT:
                return frame(type, floatLe(voutSet));
            case FnirsiDps150CommandRegistry.TYPE_DEV_IOUT:
                return frame(type, floatLe(ioutSet));
            case FnirsiDps150CommandRegistry.TYPE_DUMP:
                return frame(type, dumpData());
            default:
                return null;
        }
    }

    private byte[] powerData() {
        ByteBuffer buffer = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(measuredVout);
        buffer.putFloat(measuredIout);
        buffer.putFloat(measuredVout * measuredIout);
        return buffer.array();
    }

    private byte[] dumpData() {
        byte[] dump = new byte[0x8B];
        ByteBuffer buffer = ByteBuffer.wrap(dump).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(voutSet);
        buffer.putFloat(ioutSet);
        buffer.putFloat(voutLimit);
        buffer.putFloat(ioutLimit);
        dump[16] = (byte) output;
        dump[17] = (byte) brightness;
        return dump;
    }

    private byte[] frame(byte type, byte[] data) {
        int len = data == null ? 0 : data.length;
        byte[] f = new byte[4 + len + 1];
        f[0] = START_RECV;
        f[1] = CMD_GET;
        f[2] = type;
        f[3] = (byte) len;
        if (len > 0) {
            System.arraycopy(data, 0, f, 4, len);
        }
        int sum = 0;
        for (int i = 2; i < 4 + len; i++) {
            sum += f[i] & 0xFF;
        }
        f[4 + len] = (byte) (sum % 256);
        return f;
    }

    private static byte[] floatLe(float value) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value).array();
    }

    private static float readFloat(byte[] data, int offset) {
        return ByteBuffer.wrap(data, offset, 4).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }
}
