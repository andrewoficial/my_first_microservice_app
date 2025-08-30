package org.example.device.protErstevakMtp4d;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;

import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.util.Arrays;




public class ErstevakMtp4dCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(ErstevakMtp4dCommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createMcmd());
        // Добавление других команд
    }

    private SingleCommand createMcmd() {
        return new SingleCommand(
                "M^",
                "M^ - запрос давления у датчика. 001M^ - запрос температуры у первого прибора в линии",
                this::parseMcmd,
                5000
        );
    }



    private AnswerValues parseMcmd(byte[] response) {//"CRDG? 1" - direct
        AnswerValues answerValues = null;
        //log.info("Proceed M^ direct");

        String example = "001M960022Q\n";
        //String example = "001M495820Z\r\n";
        if (response.length >= example.length() && response.length < (example.length() + 5)) {
            if (response[3] == 'M') {
                Double value, degree;
                StringBuilder sb = new StringBuilder();
                for (byte b : response) {
                    sb.append((char) b);
                }
                String rsp = sb.toString();
                //System.out.println("Asw value " + rsp);
                //log.debug("Parse " + rsp);
                try {
                    int firstPart = rsp.indexOf("M") + 1;
                    //System.out.println(firstPart);
                    value = (double) Integer.parseInt(rsp.substring(firstPart, firstPart + 5));
                    degree = (double) Integer.parseInt(rsp.substring(firstPart + 5, firstPart + 6));
                } catch (NumberFormatException e) {
                    value = 0.0;
                    degree = 0.0;
                }

                value = (value * (double) Math.pow(10, degree));
                value /= 10000.0;
                //System.out.println("Parser result " + value);
                //log.info("Parser result " + value);
                answerValues = new AnswerValues(1);
                answerValues.addValue(value, " unit");
                return answerValues;
            } else {
                //System.out.println("Wrong M position  " + Arrays.toString(response));
                log.warn("Wrong M position  " + Arrays.toString(response));
                return null;
            }
        } else {
            //System.out.println("Wrong answer length " + response.length);
            log.warn("Wrong answer length " + response.length);
        }
        return answerValues;

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