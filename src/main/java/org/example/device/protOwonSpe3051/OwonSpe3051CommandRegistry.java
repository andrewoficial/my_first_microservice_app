package org.example.device.protOwonSpe3051;


import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

@Slf4j
public class OwonSpe3051CommandRegistry extends DeviceCommandRegistry {
    @Override
    protected void initCommands() {
        commandList.addCommand(createMeasCurrCmd());
        commandList.addCommand(createMeasVoltCmd());
        commandList.addCommand(createGetVoltCmd());
        commandList.addCommand(createSetVoltCmd());
    }

    private SingleCommand createMeasCurrCmd() {
        return new SingleCommand(
                "MEAS:CURR?",
                "MEAS:CURR? - запрос измеренного значения силы тока.",
                this::parseMeasCurrCmd,
                5000
        );
    }

    private SingleCommand createMeasVoltCmd() {
        return new SingleCommand(
                "MEAS:VOLT?",
                "MEAS:VOLT? - запрос измеренного значения напряжения.",
                this::parseMeasVoltCmd,
                5000
        );
    }

    private SingleCommand createGetVoltCmd() {
        return new SingleCommand(
                "OUTPut?",
                "OUTPut? - запрос заданного напряжения.",
                this::parseGetVoltCmd,
                5000
        );
    }

    /**
     * Команда установки напряжения: {@code VOLT <значение><CR>} (0–24 В).
     * Например {@code VOLT 5.00}. Формат совпадает с {@code OwonVoltLinearRule}
     * и {@code OwonVoltSinusRule}, чтобы установка была единообразной.
     */
    private SingleCommand createSetVoltCmd() {
        SingleCommand cmd = new SingleCommand(
                "VOLT",
                "VOLT <value> - установка заданного напряжения (0–24 В).",
                "VOLT",
                "VOLT ".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    Object v = args.get("value");
                    double volt;
                    if (v instanceof Number n) {
                        volt = n.doubleValue();
                    } else if (v != null) {
                        volt = Double.parseDouble(String.valueOf(v).replace(',', '.'));
                    } else {
                        volt = 0;
                    }
                    volt = Math.max(0, Math.min(24, volt));
                    return String.format(Locale.US, "VOLT %.2f", volt).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseVoltAnswer,
                8,
                CommandType.ASCII
        );
        cmd.addArgument(new ArgumentDescriptor(
                "value",
                Double.class,
                0.0,
                o -> o instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() <= 24
        ));
        return cmd;
    }

    /**
     * Парсер ответа на установку напряжения: прибор возвращает установленное значение,
     * либо корректный числовой ответ. Если в ответе нет числа — не знаем (вернём null).
     */
    private AnswerValues parseVoltAnswer(byte[] response) {
        if (response == null || response.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            sb.append((char) b);
        }
        String text = sb.toString().trim();
        try {
            double v = Double.parseDouble(text.replace(',', '.'));
            AnswerValues answerValues = new AnswerValues(1);
            answerValues.addValue(v, "V");
            return answerValues;
        } catch (NumberFormatException e) {
            log.warn("OWON VOLT: не удалось разобрать ответ '{}'", text);
            return null;
        }
    }


    private AnswerValues parseMeasCurrCmd(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed MEAS:CURR? ");
        //String example = "5.229";
        if (response.length > 1 && response.length < 8) {

            double outputState = -1.0;
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char) b);
            }
            if(sb.toString().contains(".")) {
                try {
                    outputState = Double.parseDouble(sb.toString());
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    outputState = -2;
                }


                answerValues = new AnswerValues(1);
                answerValues.addValue(outputState, "A");
                return answerValues;
            }else{
                //System.out.println("Answer doesnt contain dot " + sb.toString());
                log.warn("Answer doesnt contain dot " + sb.toString());
            }
        } else {
            //System.out.println("Wrong answer length " + response.length);
            log.warn("Wrong answer length " + response.length);
        }
        return null;
    }


    private AnswerValues parseMeasVoltCmd(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed MEAS:VOLT? ");
        //String example = "5.229";
        if (response.length > 1 && response.length < 8) {

            double outputState = -1.0;
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char) b);
            }
            if(sb.toString().contains(".")) {
                try {
                    outputState = Double.parseDouble(sb.toString());
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    outputState = -2;
                }
                answerValues = new AnswerValues(1);
                answerValues.addValue(outputState, "V");
                return answerValues;
            }else{
                //System.out.println("Answer doesnt contain dot " + sb.toString());
                log.warn("Answer doesnt contain dot " + sb.toString());
            }
        } else {
            //System.out.println("Wrong answer length " + response.length);
            log.warn("Wrong answer length " + response.length);
        }
        return null;
    }

    private AnswerValues parseGetVoltCmd(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed OUTPut ");
        //String example = "ON\n";
        if (response.length > 1 && response.length < 6) {
            if (response[0] == 'O') {
                int outputState = -1;
                StringBuilder sb = new StringBuilder();
                for (byte b : response) {
                    sb.append((char) b);
                }
                String rsp = sb.toString();
                //log.debug("Parse " + rsp);
                try {
                    if(sb.toString().contains("OFF")){
                        log.debug("OOOOOOOFFFFFFFFF");
                        outputState = 0;
                    }else if(sb.toString().contains("ON")){
                        log.info("OOOONNNN");
                        outputState = 1;
                    }else{
                        log.warn("???" + sb.toString());
                        outputState = -2;
                    }
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    log.warn(e.getMessage());
                    outputState = -3;
                }


                answerValues = new AnswerValues(1);
                answerValues.addValue(outputState, "bool");
                return answerValues;
            } else {
                log.warn("Wrong 'O' position  ");
                return null;
            }
        } else {
            log.warn("Wrong answer length " + response.length);
        }
        return null;
    }
    
}