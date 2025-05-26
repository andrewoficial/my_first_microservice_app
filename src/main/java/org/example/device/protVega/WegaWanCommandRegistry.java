package org.example.device.protVega;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;

import org.example.services.AnswerValues;

import java.util.Arrays;




public class WegaWanCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(WegaWanCommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createMeasCurrCmd());
        commandList.addCommand(createMeasVoltCmd());
        commandList.addCommand(createGetVoltCmd());
        // Добавление других команд
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