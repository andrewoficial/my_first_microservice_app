package org.example.device;

/**
 * Functional interface indicating that a device operates with bitwise commands (e.g., Modbus).
 * This interface is designed for devices that communicate using raw binary data
 * rather than ASCII-encoded commands, typically in industrial protocols like Modbus RTU.
 *
 * <p>Implementing this interface allows a device to receive raw byte arrays
 * representing low-level protocol frames, enabling direct control over the 
 * communication layer for custom or proprietary protocols.</p>
 *
 */
@FunctionalInterface
public interface NonAscii {

    /**
     * Sends a raw binary command to the device.
     * The command is represented as a byte array containing the complete protocol frame,
     * including device address, function code, data payload, and CRC/LRC checksum if applicable.
     *
     * <p><b>Implementation Note:</b>
     * The implementing class is responsible for ensuring the command format matches
     * the device's protocol specifications. No encoding or transformation is applied
     * to the byte array before transmission.</p>
     *
     * @param cmd the raw byte array representing the complete command frame.
     *            Must not be {@code null} and should follow the device-specific protocol.
     * @throws IllegalArgumentException if the command format is invalid
     * @throws IllegalStateException if the device is not ready to receive commands
     */
    void setRawCommand(byte[] cmd);
}