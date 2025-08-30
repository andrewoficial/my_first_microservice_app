package org.example.device.command;

// Enum для типа команды (data format)
public enum CommandType {
    ASCII,  // String-based
    BINARY, // Raw byte[]
    JSON,   // JSON parse (e.g., with Gson)
    OTHER   // Extensible
}