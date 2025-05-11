
package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import org.apache.log4j.Logger;
import org.example.services.AnswerValues;

public interface SomeDevice {

    static final Logger log = Logger.getLogger(SomeDevice.class);

    long millisPrev = 1000;

    @Deprecated
    DeviceCommandListClass commandList = null;


    byte [] getStrEndian ();

    SerialPort getComPort ();

    boolean isKnownCommand();

    int getReceivedCounter();

    void setReceivedCounter(int cnt);


    long getMillisLimit();

    int getMillisReadLimit();

    int getMillisWriteLimit();

    long getRepeatWaitTime();

    void setLastAnswer(byte [] ans);

    boolean enable();

    String cmdToSend = "";

    void setCmdToSend(String str);

    DeviceCommandListClass commands = null;

    default DeviceCommandListClass getCommandListClass(){
        return this.commands;
    }

    void parseData();

    boolean hasAnswer();

    String getAnswer();

    boolean hasValue();

    AnswerValues getValues();

    default int getExpectedBytes() {
        // Возвращает ожидаемое количество байт.
        return 600;
    }

    default long getMillisPrev() {
        return millisPrev;
    }
}
