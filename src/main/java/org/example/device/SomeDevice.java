package org.example.device;

import com.fazecast.jSerialComm.SerialPortEvent;

public interface SomeDevice {
    void enable();

    void sendData(String data);

    String getAnswer();

    boolean hasAnswer();


}
