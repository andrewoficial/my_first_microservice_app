package org.example.gui.mgstest.transport;

import org.example.gui.mgstest.transport.cmd.AbstractSendCommand;
import org.example.gui.mgstest.transport.cmd.CommandModel;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.example.gui.mgstest.util.CrcValidator.bytesToHex;
import static org.example.gui.mgstest.util.CrcValidator.calculateCRCBytes;

public class PayloadBuilder {
    private static final byte[] PREFIX_ZERO = {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final byte[] PREFIX_ONE = {(byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01}; // 0x16 = length + 4 (length = PREFIX_TWO[3] ... PREFIX_N[K])
    private static final byte[] PREFIX_TWO = {(byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65}; // 0x12 = length (length = PREFIX_TWO[3] ... PREFIX_N[K])
    private static final byte[] PREFIX_THREE = {(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final byte[] PREFIX_FOUR = {(byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01}; // 0x22 = command.getNumber()
    private static final byte[] PAYLOAD_PREFIX = {(byte) 0x01, (byte) 0x00, (byte) 0x00};

    private static byte[] CRC_PART = {(byte) 0x1B, (byte) 0xDF, (byte) 0x05, (byte) 0xA5};

    public static byte[] build(CommandModel command) {

        byte[] containsArguments = command.getArguments();
        boolean isUart = command instanceof AbstractSendCommand;
        byte first = 0x00;
        byte[] rest = containsArguments;
        byte[] crcInput = containsArguments;

        if (isUart) {
            first = (byte) containsArguments.length;
            crcInput = new byte[1 + containsArguments.length];
            crcInput[0] = first;
            System.arraycopy(containsArguments, 0, crcInput, 1, containsArguments.length);
        }

        CRC_PART = calculateCRCBytes(crcInput);

        // Склеивание containsArguments + CRC_PART
        byte[] payload = new byte[containsArguments.length + CRC_PART.length];
        System.arraycopy(containsArguments, 0, payload, 0, containsArguments.length);
        System.arraycopy(CRC_PART, 0, payload, containsArguments.length, CRC_PART.length);

        int dataLen = payload.length + 13;//13 becourse TWO[3] + THREE [0]THREE [1]THREE [2]THREE [3] + FOUR [0]FOUR [1]FOUR [2]FOUR [3] + PAYLOAD_PREFIX[0]+PAYLOAD_PREFIX[1]+PAYLOAD_PREFIX[2]
        System.out.println("Clear datalen: " + dataLen);
        PREFIX_TWO[0] = (byte) (dataLen);
        PREFIX_ONE[1] = (byte) (dataLen + 4);
        PREFIX_FOUR[1] = command.getCommandNumber();


        int offsetForMipex = 0;
        if (isUart) {
            offsetForMipex = 1;
        }
        ByteBuffer bb = ByteBuffer.allocate(PREFIX_ZERO.length +
                        PREFIX_ONE.length +
                        PREFIX_TWO.length +
                        PREFIX_THREE.length +
                        PREFIX_FOUR.length +
                        PAYLOAD_PREFIX.length +
                        payload.length + 1 +
                        offsetForMipex) // + FE
                .order(ByteOrder.LITTLE_ENDIAN);

        // Assemble fixed prefix with computed values
        bb.put(PREFIX_ZERO);
        System.out.println("Put PREFIX_ZERO: " + bytesToHex(PREFIX_ZERO));

        bb.put(PREFIX_ONE);
        System.out.println("Put PREFIX_ONE: " + bytesToHex(PREFIX_ONE));

        bb.put(PREFIX_TWO);
        System.out.println("Put PREFIX_TWO: " + bytesToHex(PREFIX_TWO));

        bb.put(PREFIX_THREE);
        System.out.println("Put PREFIX_THREE: " + bytesToHex(PREFIX_THREE));

        bb.put(PREFIX_FOUR);
        System.out.println("Put PREFIX_FOUR: " + bytesToHex(PREFIX_FOUR));

        // Payload part
        bb.put(PAYLOAD_PREFIX);
        System.out.println("Put PAYLOAD_PREFIX: " + bytesToHex(PAYLOAD_PREFIX));
        if (isUart) {
            bb.put(first);
            System.out.println("Put first (length for Uart): " + String.format("%02X", first & 0xFF));
        }
        bb.put(payload);
        System.out.println("Put payload: " + bytesToHex(payload));

        bb.put((byte) 0xFE);
        System.out.println("Put FE");

        // Pad remaining with 0x00
        while (bb.hasRemaining()) {
            bb.put((byte) 0x00);
        }
        System.out.println("Padded with 0x00 to length: " + bb.capacity());

        byte[] result = bb.array();
        System.out.println("Final array: " + bytesToHex(result));
        return result;
    }
}