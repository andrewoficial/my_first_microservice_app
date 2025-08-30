package org.example.device.command;

import org.example.services.AnswerValues;

import java.util.Map;

// Interface для генерации команды (build full package)
@FunctionalInterface
public interface CommandBuilder {
    byte[] build(Map<String, Object> args);  // Input: parsed args from UI, Output: ready byte[] (with checksum etc.)
}

