package org.example.device.lora;

import org.apache.log4j.Logger;

import java.util.Optional;

public class LoraMeshMessage {
    private static final Logger log = Logger.getLogger(LoraMeshMessage.class);

    // Типы пакетов
    public static final int LORA_INVALID = 0;
    public static final int LORA_DIRECT = 1;
    public static final int LORA_FORWARD = 2;
    public static final int LORA_BROADCAST = 3;
    public static final int LORA_NODEMAP = 4;
    public static final int LORA_MAP_REQ = 5;
    public static final int LORA_ACK = 6;

    private int format; // 0..1 bit
    private int type;   // 2..7 bits
    private long dest;  // 4 bytes, unsigned
    private long from;  // 4 bytes
    private long orig;  // 4 bytes
    private int msgId;  // 1 byte
    private int hops;   // 1 byte
    private int checksum; // 1 byte
    private byte[] data;  // Полезная нагрузка

    private LoraMeshMessage(int format, int type, long dest, long from, long orig, int msgId, int hops, int checksum, byte[] data) {
        this.format = format;
        this.type = type;
        this.dest = dest;
        this.from = from;
        this.orig = orig;
        this.msgId = msgId;
        this.hops = hops;
        this.checksum = checksum;
        this.data = data;
    }

    public static Optional<LoraMeshMessage> parse(byte[] msg) {
        if (msg == null || msg.length < 1) {
            log.error("Invalid LoRa Mesh message: empty or null");
            return Optional.empty();
        }

        try {
            int offset = 0;

            // Type & Format (1 byte)
            byte typeFormat = msg[offset++];
            int format = typeFormat & 0x03;
            int type = (typeFormat >> 2) & 0x3F;

            if (type == LORA_INVALID) {
                log.warn("Invalid LoRa type: " + type);
                return Optional.empty();
            }

            long dest = 0;
            long from = 0;
            long orig = 0;
            int msgId = 0;
            int hops = 0;
            int checksum = 0;
            byte[] data = new byte[0];

            switch (type) {
                case LORA_DIRECT:
                case LORA_FORWARD:
                case LORA_BROADCAST:
                    if (msg.length < 16) {
                        throw new IllegalArgumentException("Message too short for type " + type);
                    }
                    dest = bytesToLong(msg, offset, 4);
                    offset += 4;
                    from = bytesToLong(msg, offset, 4);
                    offset += 4;
                    orig = bytesToLong(msg, offset, 4);
                    offset += 4;
                    msgId = msg[offset++] & 0xFF;
                    hops = msg[offset++] & 0xFF;
                    data = new byte[msg.length - offset - 1];
                    System.arraycopy(msg, offset, data, 0, data.length);
                    checksum = msg[msg.length - 1] & 0xFF;
                    break;
                case LORA_MAP_REQ:
                    if (msg.length < 12) {
                        throw new IllegalArgumentException("Message too short for MAP_REQ");
                    }
                    dest = bytesToLong(msg, offset, 4);
                    offset += 4;
                    from = bytesToLong(msg, offset, 4);
                    offset += 4;
                    msgId = msg[offset++] & 0xFF;
                    hops = msg[offset++] & 0xFF;
                    checksum = msg[offset++] & 0xFF;
                    data = new byte[0];
                    orig = 0;
                    break;
                case LORA_NODEMAP:
                    if (msg.length < 10) {
                        throw new IllegalArgumentException("Message too short for NODEMAP");
                    }
                    dest = bytesToLong(msg, offset, 4);
                    offset += 4;
                    from = bytesToLong(msg, offset, 4);
                    offset += 4;
                    data = new byte[msg.length - offset - 1];
                    System.arraycopy(msg, offset, data, 0, data.length);
                    checksum = msg[msg.length - 1] & 0xFF;
                    orig = 0;
                    msgId = 0;
                    hops = 0;
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported type: " + type);
            }

            log.info("Parsed LoRa Mesh message: Type=" + type + ", Format=" + format);
            return Optional.of(new LoraMeshMessage(format, type, dest, from, orig, msgId, hops, checksum, data));
        } catch (Exception e) {
            log.error("Error parsing LoRa Mesh message", e);
            return Optional.empty();
        }
    }

    private static long bytesToLong(byte[] bytes, int offset, int length) {
        long val = 0;
        for (int i = 0; i < length; i++) {
            val |= ((long) (bytes[offset + i] & 0xFF)) << (i * 8); // little-endian
        }
        return val;
    }

    public int getFormat() {
        return format;
    }

    public int getType() {
        return type;
    }

    public long getDest() {
        return dest;
    }

    public long getFrom() {
        return from;
    }

    public long getOrig() {
        return orig;
    }

    public int getMsgId() {
        return msgId;
    }

    public int getHops() {
        return hops;
    }

    public int getChecksum() {
        return checksum;
    }

    public byte[] getData() {
        return data;
    }
}