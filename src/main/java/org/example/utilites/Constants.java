package org.example.utilites;

public interface Constants {

    public interface HidCommunication {
        int HID_PACKET_SIZE = 64;
        int READ_TIMEOUT_MS = 15;
        int MULTIGASSENSE_TARGET_PRODUCT_ID = 53456;
        int MIKROSENSE_TARGET_PRODUCT_ID = 22356;
        byte PADDING_CC = (byte) 0xCC;
        byte PADDING_00 = (byte) 0x00;
    }

    public interface SerialCommunication {
        int READ_TIMEOUT_MS = 15;
        byte PADDING_CC = (byte) 0xCC;
        byte PADDING_00 = (byte) 0x00;
    }

    public interface TcpCommunication {
        int READ_TIMEOUT_MS = 15;
        byte PADDING_CC = (byte) 0xCC;
        byte PADDING_00 = (byte) 0x00;
    }

    enum SupportedHidDeviceType{
        MULTIGASSENSE, MIKROSENSE, UNKNOWN
    }


    interface Gui{
        interface Windows{
            int DEVICE_NAME_LIMIT = 5;
        }
    }

}
