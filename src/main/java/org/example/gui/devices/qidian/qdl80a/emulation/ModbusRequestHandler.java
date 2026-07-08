package org.example.gui.devices.qidian.qdl80a.emulation;

@FunctionalInterface
public interface ModbusRequestHandler {
    byte[] handleRequest(byte[] request);
}