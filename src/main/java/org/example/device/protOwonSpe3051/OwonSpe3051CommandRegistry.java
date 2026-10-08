package org.example.device.protOwonSpe3051;


import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Slf4j
public class OwonSpe3051CommandRegistry extends DeviceCommandRegistry {

    /** Предел напряжения по умолчанию для серии SPE (совпадает с правилами OwonVolt*Rule). */
    private static final double MAX_VOLTAGE = 24.0;
    /** Предел тока по умолчанию для серии SPE. */
    private static final double MAX_CURRENT = 5.0;

    @Override
    protected void initCommands() {
        // --- IEEE 488.2 Common Commands ---
        commandList.addCommand(createIdentityCmd());
        commandList.addCommand(createResetCmd());

        // --- Измерения ---
        commandList.addCommand(createMeasVoltCmd());
        commandList.addCommand(createMeasCurrCmd());
        commandList.addCommand(createMeasPowCmd());

        // --- Выход ---
        commandList.addCommand(createGetOutputCmd());
        commandList.addCommand(createSetOutputCmd());

        // --- Уставки напряжения и тока ---
        commandList.addCommand(createGetVoltCmd());
        commandList.addCommand(createSetVoltCmd());
        commandList.addCommand(createGetCurrCmd());
        commandList.addCommand(createSetCurrCmd());

        // --- Пределы OVP / OCP ---
        commandList.addCommand(createGetVoltLimitCmd());
        commandList.addCommand(createSetVoltLimitCmd());
        commandList.addCommand(createGetCurrLimitCmd());
        commandList.addCommand(createSetCurrLimitCmd());
    }

    // =====================================================================
    // IEEE 488.2
    // =====================================================================

    /**
     * {@code *IDN?} — идентификатор прибора.
     * Формат ответа: {@code OWON,<model>,<serial number>,FV:X.XX.XX}.
     */
    private SingleCommand createIdentityCmd() {
        return new SingleCommand(
                "*IDN?",
                "*IDN? - запрос идентификатора прибора.",
                this::parseIdentity,
                64
        );
    }

    /**
     * {@code *RST} — сброс прибора. Ответ мануалом не описан.
     */
    private SingleCommand createResetCmd() {
        return new SingleCommand(
                "*RST",
                "*RST - сброс прибора к заводским настройкам.",
                "*RST",
                "*RST".getBytes(StandardCharsets.US_ASCII),
                args -> "*RST".getBytes(StandardCharsets.US_ASCII),
                response -> null,
                1,
                CommandType.ASCII
        );
    }

    // =====================================================================
    // Измерения
    // =====================================================================

    /** {@code MEASure:VOLTage?} — измеренное напряжение. */
    private SingleCommand createMeasVoltCmd() {
        return new SingleCommand(
                "MEAS:VOLT?",
                "MEAS:VOLT? - запрос измеренного значения напряжения.",
                response -> parseValue(response, "V", "MEAS:VOLT?"),
                16
        );
    }

    /** {@code MEASure:CURRent?} — измеренный ток. */
    private SingleCommand createMeasCurrCmd() {
        return new SingleCommand(
                "MEAS:CURR?",
                "MEAS:CURR? - запрос измеренного значения силы тока.",
                response -> parseValue(response, "A", "MEAS:CURR?"),
                16
        );
    }

    /** {@code MEASure:POWer?} — измеренная мощность. */
    private SingleCommand createMeasPowCmd() {
        return new SingleCommand(
                "MEAS:POW?",
                "MEAS:POW? - запрос измеренного значения мощности.",
                response -> parseValue(response, "W", "MEAS:POW?"),
                16
        );
    }

    // =====================================================================
    // Выход
    // =====================================================================

    /** {@code OUTPut?} — состояние выхода ({@code 1}/0 либо {@code ON}/{@code OFF}). */
    private SingleCommand createGetOutputCmd() {
        return new SingleCommand(
                "OUTPut?",
                "OUTPut? - запрос состояния выхода (1/0, ON/OFF).",
                this::parseBool,
                16
        );
    }

    /**
     * {@code OUTPut {0|1|ON|OFF}} — включение/выключение выхода.
     */
    private SingleCommand createSetOutputCmd() {
        SingleCommand cmd = new SingleCommand(
                "OUTPut",
                "OUTPut <0|1|ON|OFF> - управление выходом.",
                "OUTPut",
                "OUTPut ".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    Object v = args.get("value");
                    boolean on;
                    if (v instanceof Boolean b) {
                        on = b;
                    } else if (v instanceof Number n) {
                        on = n.doubleValue() != 0;
                    } else if (v != null) {
                        String s = String.valueOf(v).trim().toUpperCase(Locale.US);
                        on = s.equals("ON") || s.equals("1") || s.equals("TRUE");
                    } else {
                        on = false;
                    }
                    return ("OUTPut " + (on ? "ON" : "OFF")).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseBool,
                16,
                CommandType.ASCII
        );
        cmd.addArgument(new ArgumentDescriptor(
                "value",
                String.class,
                "OFF",
                o -> o instanceof String s && (s.equalsIgnoreCase("ON")
                        || s.equalsIgnoreCase("OFF")
                        || s.equals("1") || s.equals("0"))
        ));
        return cmd;
    }

    // =====================================================================
    // Уставка напряжения
    // =====================================================================

    /** {@code VOLTage?} — заданное (уставка), а не измеренное напряжение. */
    private SingleCommand createGetVoltCmd() {
        return new SingleCommand(
                "VOLT?",
                "VOLT? - запрос заданного напряжения (уставка).",
                response -> parseValue(response, "V", "VOLT?"),
                16
        );
    }

    /**
     * {@code VOLTage <value>} — установка напряжения (0–24 В).
     * Формат совпадает с {@code OwonVoltLinearRule} и {@code OwonVoltSinusRule}.
     */
    private SingleCommand createSetVoltCmd() {
        return createFloatSetter("VOLT", "VOLT <value> - установка заданного напряжения (0–24 В).",
                "VOLT", "V", 0, MAX_VOLTAGE);
    }

    // =====================================================================
    // Уставка тока
    // =====================================================================

    /** {@code CURRent?} — заданный (уставка), а не измеренный ток. */
    private SingleCommand createGetCurrCmd() {
        return new SingleCommand(
                "CURR?",
                "CURR? - запрос заданного тока (уставка).",
                response -> parseValue(response, "A", "CURR?"),
                16
        );
    }

    /** {@code CURRent <value>} — установка тока (0–5 А). */
    private SingleCommand createSetCurrCmd() {
        return createFloatSetter("CURR", "CURR <value> - установка заданного тока (0–5 А).",
                "CURR", "A", 0, MAX_CURRENT);
    }

    // =====================================================================
    // Предел напряжения (OVP)
    // =====================================================================

    /** {@code VOLTage:LIMit?} — уставка защиты по напряжению. */
    private SingleCommand createGetVoltLimitCmd() {
        return new SingleCommand(
                "VOLT:LIM?",
                "VOLT:LIM? - запрос предела напряжения (OVP).",
                response -> parseValue(response, "V", "VOLT:LIM?"),
                16
        );
    }

    /** {@code VOLTage:LIMit <value>} — установка предела напряжения (OVP). */
    private SingleCommand createSetVoltLimitCmd() {
        return createFloatSetter("VOLT:LIM", "VOLT:LIM <value> - установка предела напряжения (OVP).",
                "VOLT:LIM", "V", 0, MAX_VOLTAGE);
    }

    // =====================================================================
    // Предел тока (OCP)
    // =====================================================================

    /** {@code CURRent:LIMit?} — уставка защиты по току. */
    private SingleCommand createGetCurrLimitCmd() {
        return new SingleCommand(
                "CURR:LIM?",
                "CURR:LIM? - запрос предела тока (OCP).",
                response -> parseValue(response, "A", "CURR:LIM?"),
                16
        );
    }

    /** {@code CURRent:LIMit <value>} — установка предела тока (OCP). */
    private SingleCommand createSetCurrLimitCmd() {
        return createFloatSetter("CURR:LIM", "CURR:LIM <value> - установка предела тока (OCP).",
                "CURR:LIM", "A", 0, MAX_CURRENT);
    }

    // =====================================================================
    // Общие помощники
    // =====================================================================

    /**
     * Создаёт команду вида {@code <token> <число>} с валидацией диапазона
     * {@code [min, max]} и разбором числового ответа прибора.
     */
    private SingleCommand createFloatSetter(String mapKey, String description, String token,
                                            String unit, double min, double max) {
        SingleCommand cmd = new SingleCommand(
                mapKey,
                description,
                mapKey,
                (token + " ").getBytes(StandardCharsets.US_ASCII),
                args -> {
                    Object v = args.get("value");
                    double value;
                    if (v instanceof Number n) {
                        value = n.doubleValue();
                    } else if (v != null) {
                        value = Double.parseDouble(String.valueOf(v).replace(',', '.'));
                    } else {
                        value = 0;
                    }
                    value = Math.max(min, Math.min(max, value));
                    return String.format(Locale.US, "%s %.2f", token, value)
                            .getBytes(StandardCharsets.US_ASCII);
                },
                response -> parseValue(response, unit, mapKey),
                16,
                CommandType.ASCII
        );
        cmd.addArgument(new ArgumentDescriptor(
                "value",
                Double.class,
                min,
                o -> o instanceof Number n && n.doubleValue() >= min && n.doubleValue() <= max
        ));
        return cmd;
    }

    /** Разбор ASCII-ответа в одно число (например {@code 1.000}). */
    private AnswerValues parseValue(byte[] response, String unit, String commandName) {
        String text = textOf(response);
        if (text.isEmpty()) {
            log.warn("OWON {}: пустой ответ", commandName);
            return null;
        }
        try {
            double value = Double.parseDouble(text.replace(',', '.'));
            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(value, unit);
            return answerValues;
        } catch (NumberFormatException e) {
            log.warn("OWON {}: не удалось разобрать ответ '{}'", commandName, text);
            return null;
        }
    }

    /** Разбор логического ответа ({@code 1}/0 либо {@code ON}/{@code OFF}). */
    private AnswerValues parseBool(byte[] response) {
        String text = textOf(response).toUpperCase(Locale.US);
        if (text.isEmpty()) {
            return null;
        }
        double state;
        if (text.equals("ON") || text.equals("1")) {
            state = 1;
        } else if (text.equals("OFF") || text.equals("0")) {
            state = 0;
        } else {
            try {
                state = Double.parseDouble(text) != 0 ? 1 : 0;
            } catch (NumberFormatException e) {
                log.warn("OWON: не удалось разобрать логический ответ '{}'", text);
                return null;
            }
        }
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(state, "bool");
        return answerValues;
    }

    /**
     * Разбор {@code *IDN?}: {@code OWON,<model>,<serial>,FV:X.XX.XX}.
     * Строковые поля сохраняются в units (значение 0), чтобы их было видно в ответе.
     */
    private AnswerValues parseIdentity(byte[] response) {
        String text = textOf(response);
        if (text.isEmpty()) {
            return null;
        }
        String[] parts = text.split(",");
        AnswerValues answerValues = new AnswerValues(Math.max(1, parts.length));
        for (String part : parts) {
            answerValues.addValue(0, part.trim());
        }
        return answerValues;
    }

    private static String textOf(byte[] response) {
        if (response == null || response.length == 0) {
            return "";
        }
        return new String(response, StandardCharsets.US_ASCII).trim();
    }
}
