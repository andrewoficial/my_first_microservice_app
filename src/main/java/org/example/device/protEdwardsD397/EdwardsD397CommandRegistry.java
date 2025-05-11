package org.example.device.protEdwardsD397;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;

import org.example.services.AnswerValues;

import java.util.Arrays;




public class EdwardsD397CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(EdwardsD397CommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(create913AskCmd());
        // Добавление других команд
    }

    private SingleCommand create913AskCmd() {
        return new SingleCommand(
                "?V00913",
                "?V00913 - Запрос давления",
                this::parse913AskCmd,
                5000
        );
    }



    private AnswerValues parse913AskCmd(byte[] response) {//"CRDG? 1" - direct
        AnswerValues answerValues = null;
        log.info("Proceed ?V00913 direct");

        int startPosition = 1;
        for (int i = startPosition; i < response.length; i++) {
            if(response[i] == 'V'){
                startPosition = i;
                break;
            }
        }
        if (response.length > 11 && response[startPosition] == 'V') {  // Проверка длины и наличия буквы 'V' на позиции 1
            String responseStr = new String(response);

            int firstPart = responseStr.indexOf("V913 ") + 5;
            if (firstPart > 4) {  // Проверка, что "V913 " найдено в строке
                int endPart = responseStr.indexOf(";", firstPart);
                if (endPart > firstPart) {  // Проверка, что найдена первая точка с запятой после числа
                    try {
                        double value = Double.parseDouble(responseStr.substring(firstPart, endPart));
                        log.info("Parsed value: " + value);
                        answerValues = new AnswerValues(1);
                        answerValues.addValue(value, " unit");
                        return answerValues;
                    } catch (NumberFormatException e) {
                        log.warn("Can't parse" + e.getMessage());
                        return null;
                    }
                } else {
                    log.warn("End part not found");
                    return null;
                }
            } else {
                log.warn("Pattern 'V913 ' not found");
                return null;
            }
        } else {
            log.warn("Wrong Length or Missing 'V' at position 1, Length: " + response.length + ", Content: " + Arrays.toString(response));
            return null;
        }

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