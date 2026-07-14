package org.example.device.protDemo;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;
import java.util.Arrays;

@Slf4j
public class DemoProtocolCommandRegistry extends DeviceCommandRegistry {
    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "AM_I_DEMO",
                "AM_I_DEMO - заглушка для тестирования парсера ",
                this::parseFResponse,
                1
        );
    }

    private AnswerValues parseFResponse(byte[] response) {
        log.warn("Получен ответ от демо-протокола: " + Arrays.toString(response));
        return null;
    }
}