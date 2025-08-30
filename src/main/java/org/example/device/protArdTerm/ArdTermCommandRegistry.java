package org.example.device.protArdTerm;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import static org.example.utilites.MyUtilities.*;

public class ArdTermCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(ArdTermCommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
        // Добавление других команд
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "F",
                "F - запрос измеренного значения температуры и давления. ",
                this::parseFResponse,
                73
        );
    }

    private AnswerValues parseFResponse(byte[] response) {
        AnswerValues answerValues = null;
        answerValues = null;
        boolean messagIsValid = false;
        if( ! checkStructureForF(response)){
            log.info("Не пройдена проверка по признакам длинны, первого символа, табуляций и последнего символа");
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                //sb.append((char) b);
                sb.append("[");
                sb.append(b);
                sb.append("], ");
            }
            log.warn("Рассматривал массив длинною " + response.length + " " + sb.toString());
            return null;
        }else{
            messagIsValid = true;
        }
        if (response[70] != calculateCRCforF(response)) {
            log.info("ERROR CRC for F");
            log.info("Expected CRC F " + calculateCRCforF(response) + " received " + response[70] + " ARD_TERM");
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                //sb.append((char) b);
                sb.append("[");
                sb.append(b);
                sb.append("], ");
            }
            log.warn("Рассматривал массив длинною " + response.length + " " + sb.toString());
            return null;

        }

        double outputState = -1.0;
        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            sb.append((char) b);
        }
        sb.replace(0, 1, "");
        //log.info("F answer " + sb.toString());
        StringBuilder tmpSb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                tmpSb.append(sb.charAt(i));
            }
        }

        boolean isOk = true;
        double termOne = 0;
        double pressOne = 0;
        double termTwo = 0;
        int serialNum = 0;
        if(tmpSb.length() == 5){
            //System.out.print("termOne " + tmpSb);
            //System.out.println(" is Correct");
            try {
                termOne = Double.parseDouble(tmpSb.toString()) / 100;
                isOk = true;
            }catch (NumberFormatException e){
                log.warn("NumberFormatException" +  e.getMessage());
                isOk = false;
            }
        }else {
            isOk = false;
        }

        if(isOk){
            tmpSb.setLength(0);
            for (int i = 6; i < 12; i++) {
                if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                    tmpSb.append(sb.charAt(i));
                }
            }
        }

        if(tmpSb.length() == 5){
            //System.out.print("pressOne " + tmpSb);
            //System.out.println(" is Correct");
            try {
                pressOne = Double.parseDouble(tmpSb.toString()) / 10;
                isOk = true;
            }catch (NumberFormatException e){
                log.warn("NumberFormatException" +  e.getMessage());
                isOk = false;
            }
        }else{
            //System.out.print("pressOne " + tmpSb);
            //System.out.println(" is wrong");
            isOk = false;
        }

        if(isOk){
            tmpSb.setLength(0);
            for (int i = 12; i < 17; i++) {
                if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                    tmpSb.append(sb.charAt(i));
                }
            }
        }
        if(tmpSb.length() == 5){
            try {
                termTwo = Double.parseDouble(tmpSb.toString()) / 100;
                isOk = true;
            }catch (NumberFormatException e){
                log.warn("NumberFormatException" +  e.getMessage());
                isOk = false;
            }

        }else{

            isOk = false;
        }


        if(isOk){
            tmpSb.setLength(0);
            for (int i = 60; i < 68; i++) {
                if(sb.charAt(i) > 47 && sb.charAt(i) < 58) {
                    tmpSb.append(sb.charAt(i));
                }
            }
        }
        if(true){
            try {
                serialNum = Integer.parseInt(tmpSb.toString());
                isOk = true;
            }catch (NumberFormatException e){
                log.warn("NumberFormatException" +  e.getMessage() + " " + tmpSb.toString());
                isOk = false;
            }

        }else{

            isOk = false;
        }

        if(isOk) {
            answerValues = new AnswerValues(4);
            answerValues.addValue(termOne, "C");
            answerValues.addValue(pressOne, "mmRg");
            answerValues.addValue(termTwo, "C");
            answerValues.addValue(serialNum, "S.N.");
            return answerValues;
        }else{
            //System.out.println("Answer doesnt contain dot " + sb.toString());
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