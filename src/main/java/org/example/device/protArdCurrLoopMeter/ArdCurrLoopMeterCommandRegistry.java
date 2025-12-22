package org.example.device.protArdCurrLoopMeter;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;

import java.util.Arrays;

import static org.example.utilites.MyUtilities.*;
import static org.example.utilites.MyUtilities.isCorrectNumberF;

public class ArdCurrLoopMeterCommandRegistry extends DeviceCommandRegistry {
    private final static Logger log = Logger.getLogger(ArdCurrLoopMeterCommandRegistry.class); // Объект логера

    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
        // Добавление других команд
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "G",
                "G -> TermBM PresBM HydmBM Thre_V Cur_One Cur_Two C_One_Poly C_Two_Poly CurrResF Stat SerialNumber CRC",
                this::parseGResponse,
                72
        );
    }



    private AnswerValues parseGResponse(byte[] response) {
        if(response[0] != (byte)14) {
            log.error("ERROR G. Expected: 14, received: " + response[0]);
            throw new RuntimeException("ERROR G parsing. Wrong zero symbol");
        }
        // Проверка CRC
        if (response[70] != calculateCRCforF(response)) {
            log.error("ERROR CRC for F. Expected: "+ calculateCRCforF(response) +", received: {}" + response[70]);
            throw new RuntimeException("ERROR CRC for G");
        }

        AnswerValues answerValues = new AnswerValues(11);

        parseNumberField(response, 1, 6, 100.0, " °C", answerValues);
        parseNumberField(response, 7, 12, 10.0, " mmHg", answerValues);
        parseNumberField(response, 13, 18, 1.0, " %", answerValues);
        parseNumberField(response, 19, 24, 1.0, " V", answerValues);
        parseNumberField(response, 25, 30, 100.0, " ", answerValues);
        parseNumberField(response, 31, 36, 1.0, " ", answerValues);
        parseNumberField(response, 37, 42, 1.0, " ", answerValues);
        parseNumberField(response, 43, 48, 1.0, " ", answerValues);
        parseNumberField(response, 49, 54, 100.0, " mA", answerValues);
        parseNumberField(response, 55, 60, 1.0, " ", answerValues);
        parseNumberField(response, 61, 66, 1.0, " ", answerValues);

        parseSerialNumberField(response, answerValues);

        return answerValues;
    }

    /**
     * Вспомогательный метод для парсинга числовых полей
     * @param response исходный массив байт
     * @param start начальный индекс (включительно)
     * @param end конечный индекс (исключительно)
     * @param divisor делитель для корректировки десятичной точки
     * @param unit единица измерения для значения
     * @param answerValues объект для сохранения результата
     */
    private void parseNumberField(byte[] response, int start, int end,
                                  double divisor, String unit,
                                  AnswerValues answerValues) {
        byte[] subResponse = Arrays.copyOfRange(response, start, end);

        if (!isCorrectNumberF(subResponse)) {
            log.error("Wrong number format at position " + start+ ": " + Arrays.toString(subResponse));
            throw new RuntimeException("Wrong number format at position " + start);
        }

        double value = convertAsciiBytesToDouble(subResponse, divisor);
        answerValues.addValue(value, unit);
        log.debug("Parsed value: " + value  + unit);
    }

    /**
     * Конвертирует ASCII байты в double с учетом знака и делителя
     */
    private double convertAsciiBytesToDouble(byte[] bytes, double divisor) throws IllegalArgumentException{
        boolean isNegative = false;
        double result = 0.0;

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];

            if (b == '-' && i == 0) {
                isNegative = true;
                continue;
            }

            if (b >= '0' && b <= '9') {
                result = result * 10 + (b - '0');
            } else {
                log.warn("Unexpected character in number: " + (char) b);
                throw new IllegalArgumentException("Unexpected character in number: " + (char) b);
            }
        }

        result /= divisor;
        return isNegative ? -result : result;
    }

    /**
     * Метод для парсинга серийного номера (если формат отличается)
     */
    private void parseSerialNumberField(byte[] response, AnswerValues answerValues) {
        int start = 64;
        int end = 69;

        byte[] serialBytes = Arrays.copyOfRange(response, start, end);
        double serialNumber = convertAsciiBytesToDouble(serialBytes, 1.0);
        answerValues.addValue(serialNumber, " (serial)");

        log.debug("Parsed serial number: " + serialNumber);
    }

    /**
     * Валидация числового поля (упрощенная версия)
     * Можно дополнить более сложной логикой при необходимости
     */
    private boolean isCorrectNumberF(byte[] bytes) {
        if (bytes == null || bytes.length != 5) {
            return false;
        }

        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];

            if (i == 0 && (b == '-' || (b >= '0' && b <= '9'))) {
                continue;
            }

            if (b < '0' || b > '9') {
                return false;
            }
        }

        return true;
    }
}