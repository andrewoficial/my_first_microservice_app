package org.example.gui.mgstest.exception;

import lombok.extern.slf4j.Slf4j;
import org.hid4java.HidDevice;
import java.util.ArrayList;

@Slf4j
public class MessageDoesNotDeliveredToHidDevice extends RuntimeException{

    public MessageDoesNotDeliveredToHidDevice(ArrayList<byte []> acceptableAnswerPatterns, int receiveLimit, int resendLimit, String comment, HidDevice device){
        log.warn("Sending message to device failed. " +
                "acceptableAnswerPatterns: " + acceptableAnswerPatterns +
                " receive repeat limit: " + receiveLimit +
                " send command repeat limit: " + resendLimit +
                " device path " + device.getPath() +
                " is closed " + device.isClosed() +
                " last error " + device.getLastErrorMessage() +
                " commentary [" + comment + "]");
    }
}
