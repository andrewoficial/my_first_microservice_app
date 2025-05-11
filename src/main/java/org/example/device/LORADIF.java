//package org.example.device;
//
//import com.fazecast.jSerialComm.SerialPort;
//import lombok.Setter;
//import org.apache.log4j.Logger;
//import org.example.services.AnswerValues;
//import org.example.utilites.MyUtilities;
//
//import java.nio.ByteBuffer;
//import java.nio.CharBuffer;
//import java.nio.charset.Charset;
//import java.nio.charset.CharsetDecoder;
//import java.nio.charset.CoderResult;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.List;
//import java.util.function.Function;
//
//public class LORADIF implements SomeDevice {
//    private volatile boolean bisy = false;
//    private static final Logger log = Logger.getLogger(LORADIF.class);
//    private final SerialPort comPort;
//    private byte [ ] lastAnswerBytes;
//    private StringBuilder lastAnswer = new StringBuilder();
//    private final StringBuilder emulatedAnswer = new StringBuilder();
//    private final boolean knownCommand = false;
//    private volatile boolean hasAnswer = false;
//    private  volatile boolean hasValue = false;
//    @Setter
//    private byte [] strEndian = {13, 10};//CR, LF
//    private int received = 0;
//    private final long millisLimit = 110;
//    private final long repeatWaitTime = 50;
//    private final long millisPrev = System.currentTimeMillis();
//    private final static Charset charset = Charset.forName("Cp1251");
//    private static final CharsetDecoder decoder = charset.newDecoder();
//    private AnswerValues answerValues = new AnswerValues(0);
//    private boolean needRemoveDataListener = true;
//    String cmdToSend;
//    Integer TabForAnswer;
//    Integer rssi = 0;
//    Integer snr = 0;
//    public LORADIF(SerialPort port){
//        System.out.println("Создан объект протокола LoraDifferent");
//        this.comPort = port;
//        this.enable();
//    }
//    public LORADIF(){
//        System.out.println("Создан объект протокола LoraDifferent эмуляция");
//        this.comPort = null;
//    }
//    @Override
//    public Integer getTabForAnswer(){
//        return TabForAnswer;
//    }
//    @Override
//    public void setCmdToSend(String str) {
//        cmdToSend = str;
//    }
//
//    @Override
//    public byte[] getStrEndian() {
//        return this.strEndian;
//    }
//
//    @Override
//    public SerialPort getComPort() {
//        return this.comPort;
//    }
//
//    @Override
//    public boolean isKnownCommand() {
//        return knownCommand;
//    }
//
//    @Override
//    public int getReceivedCounter() {
//        return received;
//    }
//
//    @Override
//    public void setReceivedCounter(int cnt) {
//        this.received = cnt;
//    }
//
//    @Override
//    public long getMillisPrev() {
//        return millisPrev;
//    }
//
//    @Override
//    public long getMillisLimit() {
//        return millisLimit;
//    }
//
//    @Override
//    public int getMillisReadLimit() {
//        return 350;
//    }
//
//    @Override
//    public int getMillisWriteLimit() {
//        return 350;
//    }
//
//    @Override
//    public long getRepeatWaitTime() {
//        return repeatWaitTime;
//    }
//
//
//    @Override
//    public void setLastAnswer(byte [] ans) {
//        lastAnswerBytes = ans;
//    }
//
//    private DeviceCommandListClass commands = new DeviceCommandListClass();
//
//    public boolean enable() {
//        return true;
//    }
//
//    public void setReceived(String answer){
//        this.received = answer.length();
//        this.parseData();
//    }
//
//    @Override
//    public void parseData() {
//        System.out.println("Start parse... LORADIF");
//        if (lastAnswerBytes.length > 0) {
//            System.out.println("Run Parser: " + lastAnswerBytes.length);
//            answerValues = null;
//            String inputString = new String(lastAnswerBytes);
//            inputString = inputString.replaceAll("[\\r\\n+]", "").trim();
//
//            if (inputString.length() > 20 && inputString.length() < 150) {
//                if (inputString.contains("EVT:RXP2P") || inputString.contains("CEIVE TIME")) {
//                    parseRak(inputString);
//                } else if (inputString.contains("BYTE:")) {
//                    parseEbyte(inputString);
//                } else {
//                    //parseUnknown(inputString);
//                }
//            }
//
//            lastAnswer.setLength(0);
//            if (answerValues != null) {
//                hasAnswer = true;
//                for (int i = 0; i < answerValues.getValues().length; i++) {
//                    lastAnswer.append(answerValues.getValues()[i])
//                            .append(" ")
//                            .append(answerValues.getUnits()[i])
//                            .append("  ");
//                }
//                System.out.println("ok..");
//            } else {
//                appendRawDataToAnswer();
//                System.out.println("unkn..");
//                hasAnswer = true;
//            }
//        } else {
//            System.out.println("empty..");
//            hasAnswer = false;
//        }
//    }
//
//    private void parseRak(String inputString) {
//        String[] lines = inputString.split("EV");
//        rssi = 0;
//        snr = 0;
//        for (String line : lines) {
//            if (line.isEmpty()) continue;
//
//            if (line.contains("T:RXP2P")) {
//                parseSignalParams(line);
//            } else if (line.startsWith("T:")) {
//                parsePayloadData(line, "_RAK");
//            }
//        }
//    }
//
//    private void parseEbyte(String inputString) {
//        String payload = inputString.replace("EBYTE:", "").trim();
//        rssi = 0;
//        snr = 0;
//        parsePayloadData(payload, "_EBYTE");
//    }
//
//    private void parseSignalParams(String line) {
//        String[] parts = line.split(", ");
//        if (parts.length < 3) {
//            System.out.println("Invalid RXP2P format");
//            return;
//        }
//
//        try {
//            // Обрабатываем RSSI
//            String rssiStr = parts[1].replace("RSSI", "").trim();
//            rssi = Integer.parseInt(rssiStr.split(" ")[0]);
//
//            // Обрабатываем SNR
//            String snrStr = parts[2].replace("SNR", "").trim();
//            snr = Integer.parseInt(snrStr.split(" ")[0]);
//
//        } catch (Exception e) {
//            System.out.println("Error parsing signal params: " + e.getMessage());
//        }finally {
//            System.out.println("rssi "+ rssi + " snr" + snr);
//        }
//    }
//
//    private void parsePayloadData(String dataLine, String suffix) {
//        // Ищем начало payload после префиксов
//        int payloadStart = dataLine.indexOf("EVT:") + 4;
//        if (payloadStart < 4) payloadStart = 0;
//
//        String payload = dataLine.substring(payloadStart);
//        if (payload.length() < 34) {
//            System.out.println("Invalid payload length: " + payload.length());
//            return;
//        }
//        payload = payload.replaceAll("EVT:", "");
//        payload = payload.replaceAll(":","");
//        payload = payload.replaceAll("T","");
//        payload = payload.trim();
//
//        // Берем данные и CRC со смещением
//        System.out.println(" PayLoad" + payload);
//        String dataPart = payload.substring(0, 28);
//        String crcPart = payload.substring(28, 32); // Смещение CRC на 2 символа
//
//        try {
//            int receivedCRC = Integer.parseInt(crcPart, 16);
//            System.out.println(dataPart);
//            System.out.println(crcPart);
//            int calculatedCRC = MyUtilities.calculateCRC16(dataPart.getBytes());
//            if (calculatedCRC != receivedCRC) {
//                System.out.println("CRC mismatch ca");
//                return;
//            }
//            System.out.println("CRC OK" + " transfered RSSI" + rssi + " transfered SNR" + snr + "dataPart" + dataPart);
//            answerValues = new AnswerValues(5);
//            answerValues.addValue(rssi, "RSSI");
//            answerValues.addValue(snr, "SNR");
//
//            String ident = IGM_10LORA_P2P.parsePayload(dataPart, answerValues) + suffix;
//            //TabForAnswer = AnswerStorage.getTabByIdent(ident);
//
//        } catch (NumberFormatException e) {
//            System.out.println("Invalid CRC format: " + e.getMessage());
//        }
//    }
//    private void appendRawDataToAnswer() {
//        for (byte b : lastAnswerBytes) {
//            lastAnswer.append((char) b);
//        }
//    }
//    private void showReceived(){
//        lastAnswer.setLength(0);
//        hasAnswer = true;
//        hasValue = false;
//        for (int i = 0; i < lastAnswerBytes.length; i++) {
//            lastAnswer.append( (char) lastAnswerBytes[i]);
//        }
//    }
//
//
//    public void sendData(String data, byte [] strEndian, SerialPort comPort, boolean knownCommand, int buffClearTimeLimit, SomeDevice device){
//
//        setCmdToSend(data);
//        comPort.flushDataListener();
//        //log.info("  Выполнено flushDataListener ");
//
//        if(needRemoveDataListener) {
//            comPort.removeDataListener();
//        }
//        //log.info("  Выполнено removeDataListener ");
//        setCmdToSend(data);
//        byte[] buffer = new byte[data.length() + strEndian.length];
//        for (int i = 0; i < data.length(); i++) {
//            buffer[i] = (byte) data.charAt(i);
//        }
//        //buffer [data.length()] = 13;//CR
//        System.arraycopy(strEndian, 0, buffer, data.length() , strEndian.length);
//
//        if(log.isInfoEnabled()){
//            //System.out.println("Логирование ответа в файл...");
//            StringBuilder sb = new StringBuilder();
//            for (byte b : buffer) {
//                sb.append((char)b);
//            }
//            log.info("Parse request ASCII [" + sb.toString().trim() + "] ");
//            log.info("Parse request HEX [" + buffer.toString() + "] ");
//            sb = null;
//        }
//        comPort.flushIOBuffers();
//        log.info("  Выполнено flushIOBuffers и теперь bytesAvailable " + comPort.bytesAvailable());
//        comPort.writeBytes(buffer, buffer.length);
//        log.info("  Завершена отправка данных");
//
//    }
//
//    private static String parsePayload(String dataPart, AnswerValues ans) {
//        // Longitude
//        long longitude = Long.parseLong(dataPart.substring(0, 8), 16);
//        if ((longitude & 0x80000000) != 0) {
//            longitude = -(longitude & 0x7FFFFFFF);
//        }
//        double longitudeValue = longitude / 100000.0;
//        ans.addValue(longitudeValue, "LON");
//        //System.out.println(" LON: " + longitudeValue);
//
//        // Latitude
//        long latitude = Long.parseLong(dataPart.substring(8, 16), 16);
//        System.out.println(" LAT STR: " + dataPart.substring(8, 16));
//        System.out.println(" LAT LONG: " + latitude);
//        if ((latitude & 0x80000000) != 0) {
//            latitude = -(latitude & 0x7FFFFFFF);
//        }
//        double latitudeValue = latitude / 100000.0;
//        ans.addValue(latitudeValue, "LAT");
//        //System.out.println(" LAT: " + latitudeValue);
//
//        // Accuracy X
//        //int accuracyX = Integer.parseInt(dataPart.substring(16, 18), 16);
//        //ans.addValue(accuracyX, "prX");
//        //System.out.println("AccuracyX: " + accuracyX);
//
//        // Accuracy Z
//        //int accuracyZ = Integer.parseInt(dataPart.substring(18, 20), 16);
//        //ans.addValue(accuracyZ, "prZ");
//        //System.out.println("AccuracyZ: " + accuracyZ);
//
//        // Accuracy Y
//        //int accuracyY = Integer.parseInt(dataPart.substring(20, 22), 16);
//        //ans.addValue(accuracyY, "prY");
//        //System.out.println("AccuracyY: " + accuracyY);
//
//        // Active Satellites
//        int activeSatellites = Integer.parseInt(dataPart.substring(16, 18), 16);
//        ans.addValue(activeSatellites, "act Sat");
//        //System.out.println("ActiveSatellites: " + activeSatellites);
//
//        // Observed Satellites
//        //int observedSatellites = Integer.parseInt(dataPart.substring(24, 26), 16);
//        //ans.addValue(observedSatellites, "obs Sat");
//        //System.out.println("ObservedSatellites: " + observedSatellites);
//
//        // Current Message Number
//        int currentMessageNumber = Integer.parseInt(dataPart.substring(18, 20), 16);
//        ans.addValue(currentMessageNumber, "msg num");
//        //System.out.println("CurrentMessageNumber: " + currentMessageNumber);
//
//        // Total Messages
//        int totalMessages = Integer.parseInt(dataPart.substring(20, 22), 16);
//        ans.addValue(totalMessages, "tot msg");
//        //System.out.println("TotalMessages: " + totalMessages);
//
//        // Serial Number
//        int serialNumber = Integer.parseInt(dataPart.substring(22, 26), 16);
//        ans.addValue(serialNumber, "s.n.");
//        //System.out.println("SerialNumber: " + serialNumber);
//
//        // Battery Level
//        String tmp = dataPart.substring(26, 30);
//        //System.out.println("HEX battery " + tmp);
//        double batteryLevel = 0;
//        try{
//            batteryLevel = Integer.parseInt(tmp,16);
//        } catch (NumberFormatException e) {
//            System.out.println(e.getMessage());
//            //throw new RuntimeException(e);
//        }
//        batteryLevel /= 100;
//        ans.addValue(batteryLevel, "batt, V");
//        //System.out.println("BatteryLevel: " + batteryLevel);
//
//        return String.valueOf(serialNumber);
//    }
//
//    private static void parseFlags(int flags) {
//        boolean limit1 = (flags & (1 << 0)) != 0;
//        boolean limit2 = (flags & (1 << 1)) != 0;
//        boolean imitation = (flags & (1 << 2)) != 0;
//        boolean eepromError = (flags & (1 << 3)) != 0;
//        boolean deviceError = (flags & (1 << 8)) != 0;
//        boolean rangeError = (flags & (1 << 9)) != 0;
//        boolean opticalError = (flags & (1 << 10)) != 0;
//        boolean snrError = (flags & (1 << 11)) != 0;
//        boolean startReason = (flags & (1 << 13)) != 0;
//        boolean dataUrtError = (flags & (1 << 14)) != 0;
//        boolean specialMode = (flags & (1 << 15)) != 0;
//        boolean relay2Error = (flags & (1 << 16)) != 0;
//        boolean currentOutFixed = (flags & (1 << 24)) != 0;
//        boolean manual = (flags & (1 << 25)) != 0;
//        boolean locked = (flags & (1 << 26)) != 0;
//        boolean powerError = (flags & (1 << 27)) != 0;
//        boolean magnet = (flags & (1 << 29)) != 0;
//        boolean relay1Error = (flags & (1 << 31)) != 0;
//
//        System.out.println("Flags:");
//        System.out.println("  LIMIT1: " + limit1);
//        System.out.println("  LIMIT2: " + limit2);
//        System.out.println("  IMITATION: " + imitation);
//        System.out.println("  EEPROM_ERROR: " + eepromError);
//        System.out.println("  Device Error: " + deviceError);
//        System.out.println("  Range Error: " + rangeError);
//        System.out.println("  Optical Error: " + opticalError);
//        System.out.println("  SNR Error: " + snrError);
//        System.out.println("  Start Reason: " + startReason);
//        System.out.println("  Data URT Error: " + dataUrtError);
//        System.out.println("  Special Mode: " + specialMode);
//        System.out.println("  Relay 2 Error: " + relay2Error);
//        System.out.println("  Current Out Fixed: " + currentOutFixed);
//        System.out.println("  Manual: " + manual);
//        System.out.println("  Locked: " + locked);
//        System.out.println("  Power Error: " + powerError);
//        System.out.println("  Magnet: " + magnet);
//        System.out.println("  Relay 1 Error: " + relay1Error);
//    }
//
//    public boolean isBusy(){
//        return bisy;
//    }
//
//    public String getAnswer(){
//        if(hasAnswer) {
//
//            hasAnswer = false;
//            return lastAnswer.toString();
//        }else {
//            return null;
//        }
//    }
//
//    public boolean hasAnswer(){
//        //System.out.println("return flag " + hasAnswer);
//        return hasAnswer;
//    }
//
//    public boolean hasValue(){
//        return false;
//    }
//
//    public AnswerValues getValues(){
//        return answerValues;
//    }
//
//
//
//
//
//    private enum CommandList{
//
//
//        TERM("TERM?", (response) -> {
//            // Ваш алгоритм проверки для TERM?
//
//            System.out.print("Start TERM length ");
//            System.out.println(response.length);
//            AnswerValues answerValues = null;
//            if(response.length > 5 && response.length < 10){
//                ArrayList <Byte> cleanAnswer= new ArrayList<>();
//                for (byte responseByte : response) {
//                    if(responseByte > 32 && responseByte < 127)
//                        cleanAnswer.add(responseByte);
//                    //System.out.print(HexFormat.of().toHexDigits(lastAnswerByte) + " ");
//                }
//
//                //System.out.println();
//                response = new byte[cleanAnswer.size()];
//                StringBuilder sb = new StringBuilder();
//                for (byte aByte : cleanAnswer) {
//                    sb.append((char) aByte);
//                }
//
//                try{
//                    Double answer = Double.parseDouble(sb.toString());
//                    answer /= 100;
//                    if(answer > 80 || answer < -80){
//                        throw new NumberFormatException("Wrong number");
//                    }else {
//                        answerValues = new AnswerValues(1);
//                        answerValues.addValue(answer, "°С");
//                        System.out.println("degree " + answer);
//                    }
//
//                }catch (NumberFormatException ignored){
//
//                }
//            }
//            //System.out.println("Result " + anAr[0]);
//            return answerValues;
//        }),
//        FF("F", (response) -> {
//            // Ваш алгоритм проверки для F
//            return null;
//        }),
//        SREV("SREV?", (response) -> {
//            // Ваш алгоритм проверки для SREV?
//            return null;
//        }),
//        ALMH("ALMH?", (response) -> {
//            // Ваш алгоритм проверки для SRAL?
//            CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
//            ByteBuffer byteBuffer = ByteBuffer.wrap(response);
//            CoderResult result = decoder.decode(byteBuffer, charBuffer, true);
//            if (result.isError()) {
//                System.out.println("Error during decoding: " + result.toString());
//            }else{
//                return null;
//            }
//            charBuffer.flip();
//
//            StringBuilder sb = new StringBuilder();
//            sb.append(charBuffer);
//            AnswerValues an = new AnswerValues(1);
//            an.addValue(0.0, sb.toString());
//            charBuffer.clear();
//            sb.setLength(0);
//            return an;
//        });
//
//        private final String name;
//        private final Function<byte [], AnswerValues> parseFunction;
//        private static final List<String> VALUES;
//
//        static {
//            VALUES = new ArrayList<>();
//            for (CommandList someEnum : CommandList.values()) {
//                VALUES.add(someEnum.name);
//            }
//        }
//
//        CommandList(String name, Function<byte [], AnswerValues> parseFunction) {
//            this.name = name;
//            this.parseFunction = parseFunction;
//        }
//
//        public String getValue() {
//            return name;
//        }
//
//        public static List<String> getValues() {
//            return Collections.unmodifiableList(VALUES);
//        }
//
//        public static String getLikeArray(int number) {
//            List<String> values = CommandList.getValues();
//            return values.get(number);
//        }
//
//        public AnswerValues parseAnswer(byte [] response) {
//            return parseFunction.apply(response);
//        }
//
//        public static CommandList getCommandByName(String name) {
//            for (CommandList command : CommandList.values()) {
//                if (command.name.equals(name)) {
//                    return command;
//                }
//            }
//            return null;
//        }
//    }
//}
