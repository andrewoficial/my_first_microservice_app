package org.example.device.command;

import org.example.services.AnswerValues;

// Interface для парсинга ответа
@FunctionalInterface
public interface CommandParser {
    AnswerValues parse(final byte[] response);  // As your current Function
}
