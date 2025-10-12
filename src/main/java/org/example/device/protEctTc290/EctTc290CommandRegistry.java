package org.example.device.protEctTc290;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;

import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.util.Arrays;

import static org.example.utilites.MyUtilities.isCorrectNumber;


public class EctTc290CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(EctTc290CommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createCrdgDirect());
        commandList.addCommand(createCrdgBroadcast());
        commandList.addCommand(createKrdgDirect());
        commandList.addCommand(createKrdgBroadcast());
        commandList.addCommand(createSrdgDirect());
        commandList.addCommand(createSrdgBroadcast());
        // Добавление других команд
    }

    private SingleCommand createCrdgDirect() {
        return new SingleCommand(
                "CRDG? ",
                "CRDG? 1 - запрос температуры у первого датчика. CRDG? 10 - запрос температуры у десятого датчика",
                this::parseCrdgDirect,
                5000
        );
    }

    private SingleCommand createKrdgDirect() {
        return new SingleCommand(
                "KRDG? ",
                "KRDG? 1 - запрос температуры у первого датчика. KRDG? 10 - запрос температуры у десятого датчика",
                this::parseKrdgDirect,
                5000
        );
    }

    private SingleCommand createSrdgDirect() {
        return new SingleCommand(
                "SRDG? ",
                "SRDG? 1 - запрос температуры у первого датчика. SRDG? 10 - запрос температуры у десятого датчика",
                this::parseKrdgDirect,
                5000
        );
    }

    private SingleCommand createCrdgBroadcast() {
        return new SingleCommand(
                "CRDG?",
                "CRDG? - без аргументов. Опрос у всех сенсоров (Градусы Цельсия)",
                this::parseCrdgBroadcast,
                5000
        );
    }

    private SingleCommand createKrdgBroadcast() {
        return new SingleCommand(
                "KRDG?",
                "KRDG? - без аргументов. Опрос у всех сенсоров (Градусы Кельвина)",
                this::parseKrdgBroadcast,
                5000
        );
    }

    private SingleCommand createSrdgBroadcast() {
        return new SingleCommand(
                "SRDG?",
                "SRDG? - без аргументов. Опрос у всех сенсоров",
                this::parseSrdgBroadcast,
                5000
        );
    }

    private AnswerValues parseCrdgDirect(byte[] response) {//"CRDG? 1" - direct
        AnswerValues answerValues = null;
        log.info("Proceed CRDG direct");
        String example = "29.1899";
        if(response.length >= 7 ){

            if(isCorrectNumber(response)) {
                Double value;
                StringBuilder sb = new StringBuilder();
                for (byte b : response) {
                    sb.append((char)b);
                }
                String rsp = sb.toString();
                rsp = rsp.trim();
                rsp = rsp.replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                boolean success = false;
                try{
                    value = (double) Double.parseDouble(rsp);
                    success = true;
                }catch (NumberFormatException e){
                    log.warn("Exception " + e.getMessage());
                    value = 0.0;
                    success = false;
                }
                if(success){
                    answerValues = new AnswerValues(1);
                    answerValues.addValue(value, " °C");
                }else{
                    answerValues = null;
                }
                return answerValues;
            }else {
                log.warn("Wrong POINT position  " + Arrays.toString(response));
            }
        }else {
            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b).append(" ");
            }
            log.warn(sb.toString());
        }
        return answerValues;

    }

    private AnswerValues parseKrdgDirect(byte[] response) {//"KRDG? 1" - direct
        AnswerValues answerValues = null;
        log.info("Proceed KRDG direct");
        String example = "35629.9";
        if(response.length >= 27 ){

            if(isCorrectNumber(response)) {
                Double value;
                StringBuilder sb = new StringBuilder();
                for (byte b : response) {
                    sb.append((char)b);
                }
                String rsp = sb.toString();
                rsp = rsp.trim();
                rsp = rsp.replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                boolean success = false;
                try{
                    value = (double) Double.parseDouble(rsp);
                    success = true;
                }catch (NumberFormatException e){
                    log.warn("Exception " + e.getMessage());
                    value = 0.0;
                    success = false;
                }
                if(success){
                    answerValues = new AnswerValues(1);
                    answerValues.addValue(value, " °K");
                }else{
                    answerValues = null;
                }
                return answerValues;
            }else {
                log.warn("Wrong POINT position  " + Arrays.toString(response));
            }
        }else {
            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b).append(" ");
            }
            log.warn(sb.toString());
        }
        return answerValues;

    }

    private AnswerValues parseSrdgDirect(byte[] response) {//"SRDG? 1" - direct
        AnswerValues answerValues = null;
        log.info("Proceed SRDG direct");

        if(response.length >= 37 ){

            if(isCorrectNumber(response)) {
                Double value;
                StringBuilder sb = new StringBuilder();
                for (byte b : response) {
                    sb.append((char)b);
                }
                String rsp = sb.toString();
                rsp = rsp.trim();
                rsp = rsp.replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                boolean success = false;
                try{
                    value = (double) Double.parseDouble(rsp);
                    success = true;
                }catch (NumberFormatException e){
                    log.warn("Exception " + e.getMessage());
                    value = 0.0;
                    success = false;
                }
                if(success){
                    answerValues = new AnswerValues(1);
                    answerValues.addValue(value, " Units");
                }else{
                    answerValues = null;
                }
                return answerValues;
            }else {
                log.warn("Wrong POINT position  " + Arrays.toString(response));
            }
        }else {
            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b).append(" ");
            }
            log.warn(sb.toString());
        }
        return answerValues;

    }

    private AnswerValues parseKrdgBroadcast(byte[] response) {//"KRDG?" - broadcast
        AnswerValues answerValues = null;
        log.info("Proceed KRDG broadcast");
        answerValues = null;
        if(response.length >= 170 ){
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b);
            }
            String rsp = sb.toString();


            rsp = rsp.trim();
            String [] strValues = rsp.split(",");
            answerValues = new AnswerValues(10);
            for (int i = 0; i < strValues.length; i++) {
                double value = 0.0;
                strValues[i] = strValues[i].replace(",", "");
                strValues[i] = strValues[i].trim();
                strValues[i] = strValues[i].replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                boolean success = false;
                try{
                    success = true;
                    value = Double.parseDouble(strValues[i]);
                }catch (NumberFormatException e){
                    success = false;
                    log.warn("Exception " + e.getMessage());
                    //Past cleaner here
                    //Throw exception


                }
                if(success){
                    answerValues.addValue(value, " °K");
                }else{
                    //throw new ParseException("Exception message", "Exception message");
                    answerValues = null;
                }
            }
        }else {
            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b).append(" ");
            }
            log.warn(sb.toString());
        }
        return answerValues;

    }

    private AnswerValues parseSrdgBroadcast(byte[] response) {//"SRDG?" - broadcast
        AnswerValues answerValues = null;
        log.info("Proceed SRDG broadcast");
        answerValues = null;
        if(response.length >= 200 ){
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b);
            }
            String rsp = sb.toString();


            rsp = rsp.trim();
            String [] strValues = rsp.split(",");
            answerValues = new AnswerValues(10);
            for (int i = 0; i < strValues.length; i++) {
                double value = 0.0;
                strValues[i] = strValues[i].replace(",", "");
                strValues[i] = strValues[i].trim();
                strValues[i] = strValues[i].replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                boolean success = false;
                try{
                    success = true;
                    value = Double.parseDouble(strValues[i]);
                }catch (NumberFormatException e){
                    success = false;
                    log.warn("Exception " + e.getMessage());
                    //Past cleaner here
                    //Throw exception


                }
                if(success){
                    answerValues.addValue(value, " Units");
                }else{
                    //throw new ParseException("Exception message", "Exception message");
                    answerValues = null;
                }
            }
        }else {
            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b).append(" ");
            }
            log.warn(sb.toString());
        }
        return answerValues;

    }

    private AnswerValues parseCrdgBroadcast(byte[] response) {//"CRDG?" - broadcast
        AnswerValues answerValues = null;
        log.info("Proceed CRDG broadcast");
        answerValues = null;
        String example = "0.000000,0.000000,21.736603,0.000000,0.000000,0.000000,0.000000,0.000000,0.000000,0.000000";
        if(response.length >= 85 ){
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b);
            }
            String rsp = sb.toString();


            rsp = rsp.trim();
            String [] strValues = rsp.split(",");
            answerValues = new AnswerValues(10);
            for (int i = 0; i < strValues.length; i++) {
                double value = 0.0;
                strValues[i] = strValues[i].replace(",", "");
                strValues[i] = strValues[i].trim();
                strValues[i] = strValues[i].replaceAll("[^0-9.,-]", ""); // удалится все кроме цифр и указанных знаков
                boolean success = false;
                try{
                    success = true;
                    value = Double.parseDouble(strValues[i]);
                }catch (NumberFormatException e){
                    success = false;
                    log.warn("Exception " + e.getMessage());
                    //Past cleaner here
                    //Throw exception


                }
                if(success){
                    answerValues.addValue(value, " °C");
                }else{
                    //throw new ParseException("Exception message", "Exception message");
                    answerValues = null;
                }
            }
        }else {
            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char)b).append(" ");
            }
            log.warn(sb.toString());
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