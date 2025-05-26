package org.example.device.protGpsTest;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;

import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.loggers.GPS_Loger;


public class GpsTestCommandRegistry extends DeviceCommandRegistry {
    private final Logger log = Logger.getLogger(GpsTestCommandRegistry.class);
    private static final int FROM_RAK = 0;
    private static final int FROM_EBYTE = 1;
    private GPS_Loger gpsLogerRak = new GPS_Loger("66_RAK", 5);
    private GPS_Loger gpsLogerEbyte = new GPS_Loger("66_EBYTE", 5);
    @Override
    protected void initCommands() {
        commandList.addCommand(createRakByEvtCmd());
        commandList.addCommand(createRakByTimeCmd());
        commandList.addCommand(createEbyteByByteCmd());
        // Добавление других команд
    }

    private SingleCommand createRakByEvtCmd() {
        return new SingleCommand(
                "EVT:RXP2P",
                "CEIVE TIME - не команда а часть автоматически принимаемых данных от модуля RAK",
                this::parseRakEvent,
                5000
        );
    }

    private SingleCommand createRakByTimeCmd() {
        return new SingleCommand(
                "CEIVE TIME",
                "CEIVE TIME - не команда а часть автоматически принимаемых данных от модуля RAK",
                this::parseRakEvent,
                5000
        );
    }

    private SingleCommand createEbyteByByteCmd() {
        return new SingleCommand(
                "BYTE:",
                "BYTE: - не команда а часть автоматически принимаемых данных от модуля EBYTE",
                this::parseEbyteEvent,
                5000
        );
    }

    private SingleCommand createUnknownCmd() {
        return new SingleCommand(
                "UNKNOWN",
                "UNKNOWN - неизвестная команда",
                this::parseUnknownEvent,
                5000
        );
    }


    private AnswerValues parseRakEvent(byte[] response) {
        //log.info("Proceed Rak Event ");
        AnswerValues answerValues = null;
        int rssi = 0;
        int snr = 0;
        String inputString = byteArrayToString(response);
        if(inputString == null){
            log.error("Прервана обработка сообщения для RAK из-за его неконсистентности");
            return null;
        }

        // ToDo реализовать эту отправку
        if(! inputString.contains("RAK:")){
            log.warn("NEED send AT+PRECV=65535");
            //sendData("AT+PRECV=65535", strEndian, this.comPort, true, 5, this);
        }

        //log.info("Для RAK начат разбор строчек");
        String[] lines = inputString.split("EV");
        for (String line : lines) {
            //System.out.println("Run iter");
            if(line == null || line.isEmpty()){
                continue;
            }
            if (line.contains("T:RXP2P")) {
                //log.info("Нашел T:RXP2P");
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    log.error("Invalid format RXP2P not found");
                    return null;
                }

                String rssiPart = parts[1].trim();
                String snrPart = parts[2].trim();

                if (!rssiPart.startsWith("RSSI") || !snrPart.startsWith("SNR")) {
                    log.error("Invalid format RSSI not found");
                    return null;
                }

                try {
                    rssi = Integer.parseInt(rssiPart.split(" ")[1]);
                }catch (NumberFormatException e){
                    log.error("rssi Exception"+ e.getMessage());
                    rssi = 0;
                }

                try {
                    String snrStr = snrPart.split(" ")[1].trim();
                    snrStr = snrStr.replaceAll("[^-0-9]", "").replaceAll("(?<=.)-", "");
                    snr = Integer.parseInt(snrStr);
                }catch (NumberFormatException e){
                    log.error("snr Exception"+ e.getMessage());
                    snr = 0;
                }

            } else if (line.startsWith("T:")) {
                String payload = line.substring(line.indexOf("T:")+2);
                //System.out.println("Payload RAK [" + payload + "]");


                LoraMyGpsMessage loraMyGpsMessage = null;
                try{
                    loraMyGpsMessage = new LoraMyGpsMessage(payload);
                }catch (IllegalArgumentException e){
                    log.error("Ошибка при обработке сообщения от RAK " + e.getMessage() + " обработка ответа прервана!");
                    return null;
                }
                answerValues = new AnswerValues(15);
                answerValues.addValue(rssi, "RSSI");
                answerValues.addValue(snr, "SNR");
                parsePayloadMyGpsMessage(loraMyGpsMessage, answerValues, FROM_RAK);
            }else{
                //System.out.println("        Scip unknown str");
            }
        }
        return answerValues;

    }


    private AnswerValues parseEbyteEvent(byte[] response) {//
        //log.info("Proceed Ebyte event");
        AnswerValues answerValues = null;
        LoraMyGpsMessage loraMyGpsMessage = null;

        String inputString = byteArrayToString(response);
        if(inputString == null){
            log.error("Прервана обработка сообщения для RAK из-за его неконсистентности");
            return null;
        }
        inputString = inputString.replace("EBYTE:", "");
        inputString = inputString.trim();

        try{
            loraMyGpsMessage = new LoraMyGpsMessage(inputString);
        }catch (IllegalArgumentException e){
            log.error("Ошибка при обработке сообщения от EBYTE " + e.getMessage() + " обработка ответа прервана!");
            return null;
        }
        answerValues = new AnswerValues(15);
        answerValues.addValue(0, "RSSI");
        answerValues.addValue(0, "SNR");
        parsePayloadMyGpsMessage(loraMyGpsMessage, answerValues, FROM_EBYTE);

        return answerValues;
    }

    private AnswerValues parseUnknownEvent(byte[] response) {//
        log.warn("Parser for unknown event is not implemented yet");
        return null;
    }


    private String byteArrayToString(byte [] toString){
        if(toString == null){
            log.error("Попытка работы с null массивом сообщения MyGpsMessage");
            return null;
        }
        if(toString.length < 1){
            log.error("Попытка работы с коротким сообщением MyGpsMessage");
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (byte b : toString) {
            sb.append((char) b);
        }
        return sb.toString();
    }



    public void parsePayloadMyGpsMessage(LoraMyGpsMessage receivedLoraMessage, AnswerValues ans, int fromDevice) {
        ans.addValue(receivedLoraMessage.parseLongitude(), "LON");//0..8

        ans.addValue(receivedLoraMessage.parseLatitude(), "LAT"); //8..16

        ans.addValue(receivedLoraMessage.parseActiveSatellites(), "act Sat"); //16..18

        ans.addValue(receivedLoraMessage.parseMessageNumber(), "msg num"); //18..20

        ans.addValue(receivedLoraMessage.parseTotalMessages(), "tot msg"); //20..22

        ans.addValue(receivedLoraMessage.parseSerialNumber(), "s.n."); //22..26

        ans.addValue(receivedLoraMessage.parseBatteryVoltage(), "bat, V"); //26..30

        if(fromDevice == FROM_RAK){
            ans.addValue(66, "RAK");
            gpsLogerRak.writeLine(ans);
            //ans.setDirection(AnswerStorage.getTabByIdent(receivedLoraMessage.parseSerialNumber() + "_RAK"));
            //log.info("Присвоен id клиента " + AnswerStorage.getTabByIdent(receivedLoraMessage.parseSerialNumber() + "_RAK") + " по идентефикатору " + receivedLoraMessage.parseSerialNumber() + "_RAK");
        }else if(fromDevice == FROM_EBYTE){
            ans.addValue(66, "EBYTE");
            gpsLogerEbyte.writeLine(ans);
            //ans.setDirection(AnswerStorage.getTabByIdent(receivedLoraMessage.parseSerialNumber() + "_EBYTE"));
            //log.info("Присвоен id клиента " + AnswerStorage.getTabByIdent(receivedLoraMessage.parseSerialNumber() + "_RAK") + " по идентефикатору " + receivedLoraMessage.parseSerialNumber() + "_EBYTE");
        }else{
            ans.addValue(ans.getDirection(), "ERROR_TYPE");
        }


    }


}