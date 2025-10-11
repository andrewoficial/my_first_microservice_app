package org.example.gui.mgstest.transport.cmd;

public interface CommandModel {
    byte getCommandNumber();
    byte[] getArguments(); // Raw args before CRC
    void addArgument(byte arg);
    void addArgument(int arg); // Adds as 4 bytes little-endian
    void addArgument(float arg); // Adds as 4 bytes float little-endian
    void addArgument(long arg);


    void addArguments(float[] args); // Adds multiple floats
}