package org.example.gui.mgstest.transport.cmd;

import org.example.services.comPort.StringEndianList;

public interface CommandModel {
    byte getCommandNumber();
    byte[] getArguments(); // Raw args before CRC
    byte[] getAnswerOffsets();
    void addArgument(byte arg);
    void addArgument(int arg); // Adds as 4 bytes little-endian
    void addArgument(float arg); // Adds as 4 bytes float little-endian
    void addArgument(long arg);
    void addArgument(String text, StringEndianList endian);


    void addArguments(float[] args); // Adds multiple floats
}