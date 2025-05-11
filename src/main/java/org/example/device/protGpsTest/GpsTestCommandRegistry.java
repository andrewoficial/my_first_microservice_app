package org.example.device.protGpsTest;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;

import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.util.Arrays;




public class GpsTestCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(GpsTestCommandRegistry.class);

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


    private AnswerValues parseRakEvent(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed Rak Event ");
        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            sb.append((char) b);
        }
        String inputString = sb.toString();

        if(! inputString.contains("RAK:")){
            log.warn("Try send AT+PRECV=65535");

            //this.sendData("AT+PRECV=65535", strEndian, this.comPort, true, 5, this);
            // ToDo вместо этого просто отправлять в класс, курирующий порт

            //Такой метод не сработает, потому что теряется ответ. Нужен нормльный монитор с генерацией ответов
//            answerValues = new AnswerValues(1);
//            answerValues.setDirection(-55);
//            answerValues.addValue(-55, "AT+PRECV=65535");
//            return answerValues;
            //this.receiveData(this);
            // ToDo просто вешать слушатель на порт и проверять ожидаемую команду

        }

        //comPort.addDataListener(serialPortDataListener);

        String[] lines = inputString.split("EV");
//                        System.out.println("    Array Size = " + lines.length);
//                        for (String line : lines) {
//                            System.out.println(line);
//                        }
        int rssi = 0;
        int snr = 0;
        answerValues = null;
        for (String line : lines) {
            System.out.println("Run iter");
            if(line == null || line.isEmpty()){
                System.out.println("    Scip empty str");
                continue;
            }
            if (line.contains("T:RXP2P")) {
                System.out.println("    Found T:RXP2P");
                String[] parts = line.split(",");
                if (parts.length < 3) {
                    System.out.println("        Invalid format RXP2P not found");
                    return null;
                }

                String rssiPart = parts[1].trim();
                String snrPart = parts[2].trim();

                if (!rssiPart.startsWith("RSSI") || !snrPart.startsWith("SNR")) {
                    System.out.println("        Invalid format RSSI or SNR not found");
                    return null;
                }
                System.out.println("        rssiPart: " + rssiPart.split(" ")[1]);
                System.out.println("        snrPart: " + snrPart.split(" ")[1]);
                try {
                    rssi = Integer.parseInt(rssiPart.split(" ")[1]);
                }catch (NumberFormatException e){
                    System.out.println("         Exception:" + e.getMessage());
                    return null;
                }

                try {
                    snr = Integer.parseInt(snrPart.split(" ")[1]);
                }catch (NumberFormatException e){
                    System.out.println("         Exception:" + e.getMessage());
                    return null;
                }

                //System.out.println("        RSSI: " + rssi);
                //System.out.println("        SNR: " + snr);
            } else if (line.startsWith("T:")) {
                System.out.println("    Found T:");
                String payload = line.substring(line.indexOf("T:")+2);
                if (payload.length() < 34) {
                    System.out.println("        Payload length is incorrect wrong Length payload");
                    return null;
                }

                String dataPart = payload.substring(0, 30);
                //System.out.println("      MSG: " + dataPart);
                String crcPart = payload.substring(30, 34);
                //System.out.println("      CRC: " + crcPart);
                answerValues = new AnswerValues(4);
                answerValues.addValue(rssi, "RSSI");
                answerValues.addValue(snr, "SNR");
                System.out.println("      MSG: [ " + dataPart + "]");

                System.out.println("      CRC: [ " + crcPart + "]" );
                Integer calculatedCRC16Gps = MyUtilities.calculateCRC16_GPS(dataPart.getBytes());
                int crcReceived = 0;
                try{
                    crcReceived = Integer.parseInt(crcPart, 16);
                }catch (NumberFormatException e){
                    //Ignore
                }
                System.out.println("      crcString: " + crcPart + " calculatedCRC16Gps: " + calculatedCRC16Gps + " crcReceived: " + crcReceived);
                if (calculatedCRC16Gps == crcReceived) {
                    System.out.println("        CRC correct");
                    answerValues = new AnswerValues(11);
                    System.out.println("        Run proceed " + inputString + " length " + inputString.length());


                                /*
                                    ToDo
                                        Идентефикаор канала
                                 */
                    String ident = parsePayload(dataPart, answerValues);
                    ident = ident + "_RAK";
                    answerValues.setDirection(AnswerStorage.getTabByIdent(ident));

                    //System.out.println("        Will be out in " + TabForAnswer + " serial is " + ident);
                } else {
                    System.out.print("        CRC check failed for RAK");
                    System.out.print("calculate: " + MyUtilities.calculateCRC16_GPS(dataPart.getBytes()));
                    int crcReceivedInt = 0;
                    try{
                        crcReceivedInt = Integer.parseInt(crcPart, 16);
                    }catch (NumberFormatException e){
                        System.out.println(e.getMessage());
                    }

                    System.out.println(" receive: " + crcReceivedInt);
                }
            }else{
                System.out.println("        Scip unknown str");
                //showReceived();
                //return;
            }
        }
        return answerValues;

    }


    private AnswerValues parseEbyteEvent(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed Ebyte event");
        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            sb.append((char) b);
        }
        String inputString = sb.toString();
        String payload = inputString.replace("EBYTE:", "");
        payload = payload.trim();
        String dataPart = payload.substring(0, 30);
        //System.out.println("      MSG: " + dataPart);
        String crcPart = payload.substring(30, 34);
        //System.out.println("      CRC: " + crcPart);
        answerValues = new AnswerValues(4);
        System.out.println("      MSG: " + dataPart);
        System.out.println("      CRC: " + crcPart);
        if (MyUtilities.checkCRC16(dataPart, crcPart)) {
            //System.out.println("        CRC OK");
            answerValues = new AnswerValues(11);
            System.out.println("Run proceed " + inputString + " length " + inputString.length());


                                /*
                                    ToDo
                                        Идентефикаор канала
                                 */
            String ident = parsePayload(dataPart, answerValues);
            ident = ident + "_EBYTE";
            answerValues.setDirection(AnswerStorage.getTabByIdent(ident));

            System.out.println("        Will be out in " + AnswerStorage.getTabByIdent(ident) + " serial is " + ident);
        } else {
            System.out.print("        CRC check failed for RAK");
            System.out.print("calculate: " + MyUtilities.calculateCRC16_GPS(dataPart.getBytes()));
            System.out.println(" receive: " + Integer.parseInt(crcPart, 16));
        }

        return answerValues;
    }

    private AnswerValues parseUnknownEvent(byte[] response) {//
        log.warn("Parser for unknown event is not implemented yet");
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

    private static String parsePayload(String dataPart, AnswerValues ans) {
        // Longitude
        long longitude = Long.parseLong(dataPart.substring(0, 8), 16);
        if ((longitude & 0x80000000) != 0) {
            longitude = -(longitude & 0x7FFFFFFF);
        }
        double longitudeValue = longitude / 100000.0;
        ans.addValue(longitudeValue, "LON");
        //System.out.println(" LON: " + longitudeValue);

        // Latitude
        long latitude = Long.parseLong(dataPart.substring(8, 16), 16);
        System.out.println(" LAT STR: " + dataPart.substring(8, 16));
        System.out.println(" LAT LONG: " + latitude);
        if ((latitude & 0x80000000) != 0) {
            latitude = -(latitude & 0x7FFFFFFF);
        }
        double latitudeValue = latitude / 100000.0;
        ans.addValue(latitudeValue, "LAT");
        //System.out.println(" LAT: " + latitudeValue);

        // Accuracy X
        //int accuracyX = Integer.parseInt(dataPart.substring(16, 18), 16);
        //ans.addValue(accuracyX, "prX");
        //System.out.println("AccuracyX: " + accuracyX);

        // Accuracy Z
        //int accuracyZ = Integer.parseInt(dataPart.substring(18, 20), 16);
        //ans.addValue(accuracyZ, "prZ");
        //System.out.println("AccuracyZ: " + accuracyZ);

        // Accuracy Y
        //int accuracyY = Integer.parseInt(dataPart.substring(20, 22), 16);
        //ans.addValue(accuracyY, "prY");
        //System.out.println("AccuracyY: " + accuracyY);

        // Active Satellites
        int activeSatellites = Integer.parseInt(dataPart.substring(16, 18), 16);
        ans.addValue(activeSatellites, "act Sat");
        //System.out.println("ActiveSatellites: " + activeSatellites);

        // Observed Satellites
        //int observedSatellites = Integer.parseInt(dataPart.substring(24, 26), 16);
        //ans.addValue(observedSatellites, "obs Sat");
        //System.out.println("ObservedSatellites: " + observedSatellites);

        // Current Message Number
        int currentMessageNumber = Integer.parseInt(dataPart.substring(18, 20), 16);
        ans.addValue(currentMessageNumber, "msg num");
        //System.out.println("CurrentMessageNumber: " + currentMessageNumber);

        // Total Messages
        int totalMessages = Integer.parseInt(dataPart.substring(20, 22), 16);
        ans.addValue(totalMessages, "tot msg");
        //System.out.println("TotalMessages: " + totalMessages);

        // Serial Number
        int serialNumber = Integer.parseInt(dataPart.substring(22, 26), 16);
        ans.addValue(serialNumber, "s.n.");
        //System.out.println("SerialNumber: " + serialNumber);

        // Battery Level
        String tmp = dataPart.substring(26, 30);
        //System.out.println("HEX battery " + tmp);
        double batteryLevel = 0;
        try{
            batteryLevel = Integer.parseInt(tmp,16);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            //throw new RuntimeException(e);
        }
        batteryLevel /= 100;
        ans.addValue(batteryLevel, "batt, V");
        //System.out.println("BatteryLevel: " + batteryLevel);

        return String.valueOf(serialNumber);
    }

}