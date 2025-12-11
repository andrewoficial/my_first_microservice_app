package org.example.device;

import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.ParityList;
import org.example.services.comPort.StopBitsList;

/**
 * Interface that indicates a device is capable of configuring default COM port connection parameters.
 * Implementing classes should provide default settings for serial communication parameters
 * such as baud rate, data bits, parity, and stop bits.
 *
 * <p>This interface is typically implemented by device drivers or communication modules
 * that require specific serial port configurations to operate correctly.</p>
 *
 * <p><b>Usage Example: someDevice.getDefaultDataBit</b></p>
 *
 * @see BaudRatesList
 * @see DataBitsList
 * @see ParityList
 * @see StopBitsList
 */
public interface ProtocolComPort {

    /**
     * Returns the default data bits setting for the device.
     * Data bits define the number of bits in each character transmitted.
     *
     * @return default data bits configuration for serial communication
     * @see DataBitsList
     */
    DataBitsList getDefaultDataBit();

    /**
     * Returns the default parity setting for the device.
     * Parity is an error-checking mechanism used in serial communication.
     *
     * @return default parity configuration for serial communication
     * @see ParityList
     */
    ParityList getDefaultParity();

    /**
     * Returns the default baud rate for the device.
     * Baud rate defines the speed of data transmission in bits per second.
     *
     * @return default baud rate configuration for serial communication
     * @see BaudRatesList
     */
    BaudRatesList getDefaultBaudRate();

    /**
     * Returns the default stop bits setting for the device.
     * Stop bits indicate the end of a character transmission.
     *
     * @return default stop bits configuration for serial communication
     * @see StopBitsList
     */
    StopBitsList getDefaultStopBit();
}