package org.example.gui.mgstest.exception;

import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;
import org.hid4java.HidDevice;

import java.util.ArrayList;

public class MessageDoesNotDeliveredToHidDevice extends RuntimeException{

    private final Logger log = Logger.getLogger(MessageDoesNotDeliveredToHidDevice.class);
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
