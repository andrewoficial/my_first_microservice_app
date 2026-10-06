package org.example.device.protErstevakMtp4d;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.nio.charset.StandardCharsets;

/**
 * Команды Erstevak MTP4D (протокол Thyracont RS485 V1).
 * <p>
 * Кадр: {@code <3 цифры адреса><команда><данные><checksum>\r}.
 * На реальном приборе команда чтения идёт с завершающим {@code ^} (например {@code 001M^}),
 * без него прибор не отвечает.
 */
@Slf4j
public class ErstevakMtp4dCommandRegistry extends DeviceCommandRegistry {

    private static final int EXPECTED_BYTES = 5000;

    @Override
    protected void initCommands() {
        commandList.addCommand(createPressureCommand());
        commandList.addCommand(createTypeCommand());
        commandList.addCommand(createSetpointCommand("S1^", "S1^ - уставка 1 (давление)"));
        commandList.addCommand(createSetpointCommand("S2^", "S2^ - уставка 2 (давление)"));
        commandList.addCommand(createCalibrationCommand("C1^", "C1^ - калибровочный коэффициент 1"));
        commandList.addCommand(createCalibrationCommand("C2^", "C2^ - калибровочный коэффициент 2"));
        commandList.addCommand(createPenningStateCommand());
        commandList.addCommand(createPenningSyncCommand());
    }

    private SingleCommand createPressureCommand() {
        return new SingleCommand(
                "M^",
                "M^ - запрос давления у датчика. 001M^ - запрос давления у прибора с адресом 001",
                response -> parseEncodedValue(response, 'M', " mbar"),
                EXPECTED_BYTES
        );
    }

    private SingleCommand createTypeCommand() {
        return new SingleCommand(
                "T^",
                "T^ - запрос модели/типа датчика",
                this::parseType,
                EXPECTED_BYTES
        );
    }

    private SingleCommand createSetpointCommand(String name, String description) {
        return new SingleCommand(
                name,
                description,
                response -> parseEncodedValue(response, 'S', " mbar"),
                EXPECTED_BYTES
        );
    }

    private SingleCommand createCalibrationCommand(String name, String description) {
        return new SingleCommand(
                name,
                description,
                response -> parseCalibration(response, 'C'),
                EXPECTED_BYTES
        );
    }

    private SingleCommand createPenningStateCommand() {
        return new SingleCommand(
                "I^",
                "I^ - состояние газоразрядного (Penning) датчика",
                response -> parseState(response, 'I'),
                EXPECTED_BYTES
        );
    }

    private SingleCommand createPenningSyncCommand() {
        return new SingleCommand(
                "W^",
                "W^ - синхронизация газоразрядного (Penning) датчика",
                response -> parseState(response, 'W'),
                EXPECTED_BYTES
        );
    }

    /**
     * 6-значное значение: 5 цифр мантиссы + 1 цифра степени, {@code value * 10^degree / 10000}.
     * Формула проверена на реальном приборе (пример {@code 001M960022Q} -> 960.02 mbar).
     */
    private AnswerValues parseEncodedValue(byte[] response, char command, String unit) {
        String rsp = toText(response);
        int digits = dataStart(rsp, command, 6);
        if (digits < 0) {
            log.warn("Erstevak MTP4D: некорректный ответ '{}'", rsp);
            return null;
        }
        try {
            int mantissa = Integer.parseInt(rsp.substring(digits, digits + 5));
            int degree = Integer.parseInt(rsp.substring(digits + 5, digits + 6));
            double value = (mantissa * Math.pow(10, degree)) / 10000.0;
            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(value, unit);
            return answerValues;
        } catch (NumberFormatException e) {
            log.warn("Erstevak MTP4D: не удалось разобрать значение '{}'", rsp);
            return null;
        }
    }

    /** Калибровочный коэффициент: 6 цифр, значение = целое / 100. */
    private AnswerValues parseCalibration(byte[] response, char command) {
        String rsp = toText(response);
        int digits = dataStart(rsp, command, 6);
        if (digits < 0) {
            log.warn("Erstevak MTP4D: некорректный ответ '{}'", rsp);
            return null;
        }
        try {
            double value = Integer.parseInt(rsp.substring(digits, digits + 6)) / 100.0;
            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(value, "");
            return answerValues;
        } catch (NumberFormatException e) {
            log.warn("Erstevak MTP4D: не удалось разобрать калибровку '{}'", rsp);
            return null;
        }
    }

    /** Состояние: 6 цифр, обычно 0/1. */
    private AnswerValues parseState(byte[] response, char command) {
        String rsp = toText(response);
        int digits = dataStart(rsp, command, 6);
        if (digits < 0) {
            log.warn("Erstevak MTP4D: некорректный ответ '{}'", rsp);
            return null;
        }
        try {
            int state = Integer.parseInt(rsp.substring(digits, digits + 6));
            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(state, "");
            return answerValues;
        } catch (NumberFormatException e) {
            log.warn("Erstevak MTP4D: не удалось разобрать состояние '{}'", rsp);
            return null;
        }
    }

    /** Модель прибора: текст после команды (без завершающего checksum и CR/LF). */
    private AnswerValues parseType(byte[] response) {
        String rsp = toText(response);
        int idx = rsp.indexOf('T');
        if (idx < 0 || idx + 1 >= rsp.length()) {
            log.warn("Erstevak MTP4D: некорректный ответ '{}'", rsp);
            return null;
        }
        int end = rsp.length();
        for (int i = idx + 1; i < rsp.length(); i++) {
            char ch = rsp.charAt(i);
            if (ch == '\r' || ch == '\n') {
                end = i > idx + 1 ? i - 1 : i; // отбрасываем checksum перед терминатором
                break;
            }
        }
        String model = rsp.substring(idx + 1, end).trim();
        if (model.isEmpty()) {
            return null;
        }
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(0.0, model);
        return answerValues;
    }

    /**
     * Индекс первого символа данных (после команды) либо -1, если ответ короче ожидаемого.
     */
    private int dataStart(String response, char command, int dataLength) {
        if (response == null) {
            return -1;
        }
        int idx = response.indexOf(command);
        if (idx < 0 || response.length() < idx + 1 + dataLength) {
            return -1;
        }
        return idx + 1;
    }

    private String toText(byte[] response) {
        return response == null ? "" : new String(response, StandardCharsets.US_ASCII);
    }
}
