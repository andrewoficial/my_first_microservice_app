package org.example.device.protDemo;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;
import org.example.device.protArdFeeBrdMeter.ARD_FEE_BRD_METER;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;

import java.util.Arrays;

import static org.example.utilites.MyUtilities.*;
import static org.example.utilites.MyUtilities.isCorrectNumberF;

public class DemoProtocolCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(DemoProtocolCommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
        // Добавление других команд
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

    // Вынесенные методы для повторного использования
    private double parseSubResponse(byte[] subResponse) {
        // Общая логика преобразования байтов в число
        return 0.0;
    }

    private boolean validateCrc(byte[] response) {
        // Логика проверки CRC
        return false;
    }
}