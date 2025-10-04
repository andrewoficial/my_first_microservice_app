package org.example.gui.mgstest.transport.hid;

import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;
import java.util.Arrays;
import static org.example.utilites.Constants.HidCommunication.*;


public class SomeHidController implements HidCommunicator {
    private final Logger log = Logger.getLogger(SomeHidController.class);

    /**
     * Sends data to HID device with zero padding
     * @param device - target HID device
     * @param data - byte array data (1 to 64 bytes). Will be padded with 00 to 64 bytes.
     */
    public void simpleSend(HidDevice device, byte[] data) {
        sendWithPadding(device, data, PADDING_00);
    }

    /**
     * Sends data to HID device with CC padding
     * @param device - target HID device
     * @param data - byte array data (1 to 64 bytes). Will be padded with CC to 64 bytes.
     */
    public void simpleSendInitial(HidDevice device, byte[] data) {
        sendWithPadding(device, data, PADDING_CC);
    }

    /**
     * Reads response from HID device
     * @param device - source HID device
     * @return response data as byte array
     */
    public byte[] readResponse(HidDevice device) {
        return basicRead(device);
    }

    private void sendWithPadding(HidDevice device, byte[] data, byte padding) {
        byte reportId = data[0];
        byte[] message = generateMessageConfigDropFirst(data, padding);
        basicSend(device, message, reportId, data, padding);
    }

    private byte[] basicRead(HidDevice device) {
        byte[] buffer = new byte[HID_PACKET_SIZE];
        int bytesRead = device.read(buffer, READ_TIMEOUT_MS);
        if (bytesRead < 0) {
            log.error("Error reading from device. Last error: " + device.getLastErrorMessage());
            logDeviceInfo(device);
        }
        return buffer;
    }

    private void basicSend(HidDevice device, byte[] message, byte reportId, byte[] originalData, byte padding) {
        int bytesSent = device.write(message, message.length, reportId);
        String paddingHex = String.format("%02X", padding);
        log.info("Payload: " + MyUtilities.bytesToHex(originalData) + " rest filled with " + paddingHex + " up to " + bytesSent + " bytes");
        if (bytesSent < 1) {
            log.error("Error writing to device. Last error: " + device.getLastErrorMessage());
            logDeviceInfo(device);
        }
    }

    private void logDeviceInfo(HidDevice device) {
        log.error("Device info - Usage: " + device.getUsage() +
                ", ProductId: " + device.getProductId() +
                ", Manufacturer: " + device.getManufacturer() +
                ", IsOpen: " + device.isOpen() +
                ", Path: " + device.getPath());
    }

    public void printArrayLikeDeviceMonitor(byte[] data) {
        StringBuilder singleLine = new StringBuilder();
        StringBuilder formattedLines = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            int unsignedValue = data[i] & 0xFF;
            String hexByte = String.format("%02X ", unsignedValue);

            singleLine.append(hexByte);
            formattedLines.append(hexByte);

            if ((i + 1) % 16 == 0) {
                formattedLines.append("\n");
            }
        }

        System.out.println(singleLine);
        System.out.println(formattedLines);
    }

    private byte[] generateMessageConfigDropFirst(byte[] data, byte padding) {
        if (data.length > HID_PACKET_SIZE) {
            log.warn("Message too big. Truncating to " + HID_PACKET_SIZE + " bytes");
        }

        byte[] message = new byte[HID_PACKET_SIZE];
        Arrays.fill(message, padding);

        int bytesToCopy = Math.min(HID_PACKET_SIZE, data.length) - 1; // -1 to skip reportId
        if (bytesToCopy > 0) {
            System.arraycopy(data, 1, message, 0, bytesToCopy);
        }

        return message;
    }
}