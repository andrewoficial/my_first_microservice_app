package org.example.gui.mgstest.transport.cmd;

import org.example.services.comPort.StringEndianList;

public interface CommandModel {
    byte getCommandNumber();
    byte[] getArguments();
    byte[] getAnswerOffsets();
    void addArgument(byte arg);  //1 byte....8 bits...little-endian
    void addArgument(short arg); //2 bytes...16 bits..little-endian
    void addArgument(char arg);  //2 bytes...16 bits..little-endian - same as short
    void addArgument(int arg);   //4 bytes...32 bits..little-endian
    void addArgument(float arg); //4 bytes...32 bits..little-endian
    void addArgument(long arg);  //8 bytes...64 bits..little-endian

    void addArgument(String text, StringEndianList endian);
    void addArguments(float[] args);

}