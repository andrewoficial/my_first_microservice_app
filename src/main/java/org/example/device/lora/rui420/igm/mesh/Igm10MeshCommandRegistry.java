package org.example.device.lora.rui420.igm.mesh;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

public class Igm10MeshCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(Igm10MeshCommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createTestCmd());
    }

    private SingleCommand createTestCmd() {
        return new SingleCommand(
                "AT+PRECV=9999",
                "AT+PRECV=9999 - Перевод в режим приема",
                this::parseTestCmd,
                5000
        );
    }

    private AnswerValues parseTestCmd(byte[] response) {
        log.info("AT+PRECV=9999");

        try {
            String responseStr = new String(response).trim();
            // Простой парсер: предполагаем, что ответ - это строка с любым содержимым
            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(0, responseStr); // Просто добавляем весь ответ как значение с единицей ""
            return answerValues;
        } catch (Exception e) {
            log.error("Unexpected error while parsing test response: " + new String(response) + e);
            return null;
        }
    }
}