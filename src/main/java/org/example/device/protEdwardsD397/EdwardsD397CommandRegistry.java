package org.example.device.protEdwardsD397;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;

import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;



public class EdwardsD397CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(EdwardsD397CommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(create913AskCmd());
        commandList.addCommand(create914AskCmd());
        commandList.addCommand(create915AskCmd());
        // Добавление других команд
    }

// ===== ДАТЧИКИ ДАВЛЕНИЯ (GAUGES) =====

    private SingleCommand create913AskCmd() {
        return new SingleCommand(
                "?V00913",
                "?V00913 - Запрос давления с датчика 1 (Gauge 1)",
                this::parse913AskCmd,
                5000
        );
    }

    private SingleCommand create914AskCmd() {
        return new SingleCommand(
                "?V00914",
                "?V00914 - Запрос давления с датчика 2 (Gauge 2)",
                this::parse913AskCmd, // Используем тот же парсер, т.к. формат ответа идентичен
                5000
        );
    }

    private SingleCommand create915AskCmd() {
        return new SingleCommand(
                "?V00915",
                "?V00915 - Запрос давления с датчика 3 (Gauge 3)",
                this::parse913AskCmd, // Используем тот же парсер, т.к. формат ответа идентичен
                5000
        );
    }
/*
// ===== ТУРБОНАСОС (TURBO PUMP) =====

    private SingleCommand create904AskCmd() {
        return new SingleCommand(
                "?V00904",
                "?V00904 - Запрос состояния турбонасоса",
                this::parse904AskCmd,
                5000
        );
    }

    private SingleCommand create904ControlCmd(boolean turnOn) {
        int state = turnOn ? 1 : 0;
        String action = turnOn ? "Включение" : "Выключение";
        return new SingleCommand(
                "IC904 " + state,
                "IC904 " + state + " - " + action + " турбонасоса",
                this::parse904ControlCmd,
                5000
        );
    }

// ===== СКОРОСТЬ ТУРБОНАСОСА (TURBO SPEED) =====

    private SingleCommand create905AskCmd() {
        return new SingleCommand(
                "?V00905",
                "?V00905 - Запрос скорости турбонасоса (%)",
                this::parse905AskCmd,
                5000
        );
    }

// ===== МОЩНОСТЬ ТУРБОНАСОСА (TURBO POWER) =====

    private SingleCommand create906AskCmd() {
        return new SingleCommand(
                "?V00906",
                "?V00906 - Запрос мощности турбонасоса (Вт)",
                this::parse906AskCmd,
                5000
        );
    }

// ===== СИСТЕМНЫЙ СТАТУС (SYSTEM STATUS) =====

    private SingleCommand create902AskCmd() {
        return new SingleCommand(
                "?V00902",
                "?V00902 - Запрос системного статуса TIC",
                this::parse902AskCmd,
                5000
        );
    }

    private SingleCommand create902InfoCmd() {
        return new SingleCommand(
                "?S00902",
                "?S00902 - Запрос системной информации (версия, серийный номер)",
                this::parse902InfoCmd,
                5000
        );
    }

// ===== РЕЛЕ (RELAYS) =====

    private SingleCommand create916AskCmd() {
        return new SingleCommand(
                "?V00916",
                "?V00916 - Запрос состояния реле 1",
                this::parse916AskCmd,
                5000
        );
    }

    private SingleCommand create916ControlCmd(boolean turnOn) {
        int state = turnOn ? 1 : 0;
        String action = turnOn ? "Включение" : "Выключение";
        return new SingleCommand(
                "IC916 " + state,
                "IC916 " + state + " - " + action + " реле 1",
                this::parse916ControlCmd,
                5000
        );
    }

// ===== ТЕМПЕРАТУРЫ =====

    private SingleCommand create919AskCmd() {
        return new SingleCommand(
                "?V00919",
                "?V00919 - Запрос температуры блока питания",
                this::parse919AskCmd,
                5000
        );
    }

    private SingleCommand create920AskCmd() {
        return new SingleCommand(
                "?V00920",
                "?V00920 - Запрос внутренней температуры TIC",
                this::parse920AskCmd,
                5000
        );
    }

// ===== ВСЕ ЗНАЧЕНИЯ ДАТЧИКОВ (ОПТИМИЗИРОВАННЫЙ ЗАПРОС) =====

    private SingleCommand create940AskCmd() {
        return new SingleCommand(
                "?V00940",
                "?V00940 - Запрос всех значений датчиков за один запрос",
                this::parse940AskCmd,
                5000
        );
    }
    */



    private AnswerValues parse913AskCmd(byte[] response) {

        log.info("Proceed ?V00913 or ?V00914 or ?V00915  direct");

        try {
            // Парсим сырые данные
            ParsedResponse parsed = parseEdwardsResponse(response);

            // Создаем объект ответа
            AnswerValues answerValues = new AnswerValues(parsed.parts.length);

            // Парсим основные значения
            double value = Double.parseDouble(parsed.parts[0]);
            log.info("Parsed value: " + value);

            // Единицы измерения
            EdwardsUnits units = EdwardsUnits.fromCode(Integer.parseInt(parsed.parts[1]));
            answerValues.addValue(value, units.getSymbol());

            // Состояние датчика
            int stateCode = Integer.parseInt(parsed.parts[2]);
            String stateName = extractGaugeStateName(stateCode);
            answerValues.addValue(stateCode, stateName);

            // Alert (если есть)
            if (parsed.parts.length > 3) {
                EdwardsAlert alert = EdwardsAlert.fromCode(Integer.parseInt(parsed.parts[3]));
                answerValues.addValue(Integer.parseInt(parsed.parts[3]), alert.getName());
            }

            // Priority (если есть)
            if (parsed.parts.length > 4) {
                EdwardsPriority priority = EdwardsPriority.fromCode(Integer.parseInt(parsed.parts[4]));
                answerValues.addValue(Integer.parseInt(parsed.parts[4]), priority.getName());
            }

            return answerValues;
        } catch (NumberFormatException e) {
            log.warn("Number format error in response: " + new String(response) + e);
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to parse response: " + e.getMessage());
            return null;
        } catch (Exception e) {
            log.error("Unexpected error while parsing response: " + new String(response)  + e);
            return null;
        }
    }

    /**
     * Универсальный парсер ответов Edwards
     * Обрабатывает форматы: =V913, *V913, #dd:ss=V913 и т.д.
     */
    private ParsedResponse parseEdwardsResponse(byte[] response) {
        if (response == null || response.length == 0) {
            throw new IllegalArgumentException("Empty response");
        }

        String responseStr = new String(response).trim();
        log.debug("Parsing response: " + responseStr);
        boolean containOnOfCommand = false;
        String [] commandsPattern = {"V913", "V914", "V915"};
        String expectedCommand = null;
        for (String command : commandsPattern) {
            if(responseStr.contains(command)) {
                containOnOfCommand = true;
                expectedCommand = command;
                break;
            }
        }
        if(!containOnOfCommand) {
            log.warn("Not contain =V913 or =V914 or =V915 in response: " + responseStr);
            throw new IllegalArgumentException("Not contain ?V00913 or ?V00914 or ?V00915 in response: " + responseStr);
        }
        // Ищем позицию команды в ответе
        int commandPosition = findCommandPosition(response, expectedCommand);
        if (commandPosition == -1) {
            throw new IllegalArgumentException("Command " + expectedCommand + " not found in: " + responseStr);
        }

        // Извлекаем данные после команды
        String dataPart = extractDataAfterCommand(responseStr, commandPosition, expectedCommand);

        // Разделяем на части
        String[] parts = dataPart.split(";");

        // Проверяем минимальное количество частей
        if (parts.length < 3) {
            throw new IllegalArgumentException(
                    String.format("Insufficient data parts: expected at least 3, got %d in: %s",
                            parts.length, dataPart)
            );
        }

        // Проверяем, что все обязательные части могут быть преобразованы в числа
        validateNumericParts(parts, 3);

        return new ParsedResponse(parts, dataPart);
    }

    /**
     * Находит позицию команды в массиве байтов
     */
    private int findCommandPosition(byte[] response, String expectedCommand) {
        String responseStr = new String(response);
        return responseStr.indexOf(expectedCommand);
    }

    /**
     * Извлекает данные после команды
     */
    private String extractDataAfterCommand(String responseStr, int commandPosition, String expectedCommand) {
        int dataStart = commandPosition + expectedCommand.length();

        // Пропускаем пробелы после команды
        while (dataStart < responseStr.length() && Character.isWhitespace(responseStr.charAt(dataStart))) {
            dataStart++;
        }

        if (dataStart >= responseStr.length()) {
            throw new IllegalArgumentException("No data after command: " + responseStr);
        }

        String data = responseStr.substring(dataStart);

        // Удаляем возможный carriage return в конце
        if (data.endsWith("\r")) {
            data = data.substring(0, data.length() - 1);
        }

        return data.trim();
    }

    /**
     * Извлекает имя состояния датчика
     */
    private String extractGaugeStateName(int stateCode) {
        return EdwardsState.fromCode(stateCode).stream()
                .filter(EdwardsState::isGaugeState)
                .findFirst()
                .map(EdwardsState::getName)
                .orElse("NotParsed");
    }

    /**
     * Проверяет, что указанные части могут быть преобразованы в числа
     */
    private void validateNumericParts(String[] parts, int minPartsToCheck) {
        int partsToCheck = Math.min(minPartsToCheck, parts.length);

        for (int i = 0; i < partsToCheck; i++) {
            try {
                Double.parseDouble(parts[i]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        String.format("Invalid numeric format at position %d: '%s'", i, parts[i]), e
                );
            }
        }
    }

    /**
     * Вспомогательный класс для хранения распарсенных данных
     */
    private static class ParsedResponse {
        final String[] parts;
        final String rawData;

        ParsedResponse(String[] parts, String rawData) {
            this.parts = parts;
            this.rawData = rawData;
        }
    }



    /**
     * Шаблонный метод для парсинга различных ответов
     */
    private AnswerValues parseEdwardsResponseWithTemplate(byte[] response,  ResponseParser parser) {
        try {
            ParsedResponse parsed = parseEdwardsResponse(response);
            AnswerValues answer = new AnswerValues(parsed.parts.length);
            parser.parse(parsed.parts, answer);
            return answer;
        } catch (Exception e) {
            log.warn("Failed to parse {} response: "  + e.getMessage());
            return null;
        }
    }

    @FunctionalInterface
    private interface ResponseParser {
        void parse(String[] parts, AnswerValues answer) throws Exception;
    }
}