package org.example.device;

/**
 * Устройство с настраиваемым сетевым адресом (slave address, обычно 1-247).
 * Используется GUI, чтобы поле префикса трактовать как адрес устройства.
 */
public interface SlaveAddressable {

    void setSlaveAddress(int address);
}
