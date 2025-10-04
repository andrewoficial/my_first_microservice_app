package org.example.utilites;

public interface Constants {

    public interface HidCommunication {
        int HID_PACKET_SIZE = 64;
        int READ_TIMEOUT_MS = 15;
        byte PADDING_CC = (byte) 0xCC;
        byte PADDING_00 = (byte) 0x00;
    }
}
