/*
Реализация протокола обмена данными с прибором (в зависимости от прибора)
ToDo
Брать соединение из коллекции открытых ком-портов (коллекции пока нету)

 */
package org.example.device;

import com.fazecast.jSerialComm.SerialPortEvent;

public interface SomeDevice {
    void enable();

    void sendData(String data);

    boolean hasAnswer();
    String getAnswer();

    boolean hasValue();
    String getValue();
}
