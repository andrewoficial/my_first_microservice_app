package org.example.device;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class GPS_Test implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(GPS_Test.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes;
    private StringBuilder lastAnswer = new StringBuilder();
    private final StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13, 10};//CR, LF
    private int received = 0;
    private final long millisLimit = 110;
    private final long repeatWaitTime = 50;
    private final long millisPrev = System.currentTimeMillis();
    private final static Charset charset = Charset.forName("Cp1251");
    private static final CharsetDecoder decoder = charset.newDecoder();
    private AnswerValues answerValues = new AnswerValues(0);
    private boolean needRemoveDataListener = true;
    String cmdToSend;
    Integer TabForAnswer;

    public GPS_Test(SerialPort port){
        System.out.println("Создан объект протокола GPS-Test P2P");
        this.comPort = port;
        this.enable();
    }
    public GPS_Test(){
        System.out.println("Создан объект протокола GPS_Test эмуляция");
        this.comPort = null;
    }
    @Override
    public Integer getTabForAnswer(){
        return TabForAnswer;
    }
    @Override
    public void setCmdToSend(String str) {
        cmdToSend = str;
    }

    @Override
    public byte[] getStrEndian() {
        return this.strEndian;
    }

    @Override
    public SerialPort getComPort() {
        return this.comPort;
    }

    @Override
    public boolean isKnownCommand() {
        return knownCommand;
    }

    @Override
    public int getReceivedCounter() {
        return received;
    }

    @Override
    public void setReceivedCounter(int cnt) {
        this.received = cnt;
    }

    @Override
    public long getMillisPrev() {
        return millisPrev;
    }

    @Override
    public long getMillisLimit() {
        return millisLimit;
    }

    @Override
    public int getMillisReadLimit() {
        return 350;
    }

    @Override
    public int getMillisWriteLimit() {
        return 350;
    }

    @Override
    public long getRepeatWaitTime() {
        return repeatWaitTime;
    }


    @Override
    public void setLastAnswer(byte [] ans) {
        lastAnswerBytes = ans;
    }

    @Override
    public StringBuilder getEmulatedAnswer() {
        return this.emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb) {
        emulatedAnswer.setLength(0);
        emulatedAnswer.append(sb);
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }

    private CommandListClass commands = new CommandListClass();

    public boolean enable() {
       return true;
    }

    public void setReceived(String answer){
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        System.out.println("Start parse... GPS_Test");
        if(lastAnswerBytes.length > 0) {
            System.out.println("Run Parser: " + lastAnswerBytes.length);
            answerValues = null;
                String inputString = new String(lastAnswerBytes);
                inputString = inputString.replace("\r", "");
                inputString = inputString.replace("\n", "");
                inputString = inputString.replace("+", "");
                inputString = inputString.trim();

                if(inputString.length() > 20 && inputString.length() < 150){
                    if(inputString.contains("EVT:RXP2P") || inputString.contains("CEIVE TIME")){
                        parseRak(inputString);
                    } else if (inputString.contains("BYTE:")) {
                        parseEbyte(inputString);
                    } else{
                        parseUnknown(inputString);
                    }
            }

            lastAnswer.setLength(0);
            if(answerValues != null){
                hasAnswer = true;
                for (int i = 0; i < answerValues.getValues().length; i++) {
                    lastAnswer.append(answerValues.getValues()[i]);
                    lastAnswer.append(" ");
                    lastAnswer.append(answerValues.getUnits()[i]);
                    lastAnswer.append("  ");
                }
                System.out.println("ok..");

            }else{
                for (byte lastAnswerByte : lastAnswerBytes) {
                    //System.out.print(lastAnswerByte);
                    //System.out.print(" ");
                    lastAnswer.append((char) lastAnswerByte);
                }
                System.out.println("unkn..");
                hasAnswer = true;
            }
        }else {
            System.out.println("empty..");
            hasAnswer = false;
        }


    }

    private void parseRak(String inputString){
        if(! inputString.contains("RAK:")){
            System.out.println("Send AT+PRECV=65535");
            boolean flipFlopBusy  = this.isBusy();
            needRemoveDataListener = false;
            this.setBusy(false);
            this.sendData("AT+PRECV=65535", strEndian, this.comPort, true, 5, this);
            this.setBusy(flipFlopBusy);
            byte [] tmpLastAnswer = lastAnswerBytes;
            this.receiveData(this);
            needRemoveDataListener = true;
            lastAnswerBytes = tmpLastAnswer;
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
                    showReceived();
                    return;
                }

                String rssiPart = parts[1].trim();
                String snrPart = parts[2].trim();

                if (!rssiPart.startsWith("RSSI") || !snrPart.startsWith("SNR")) {
                    System.out.println("        Invalid format RSSI or SNR not found");
                    showReceived();
                    return;
                }
                System.out.println("        rssiPart: " + rssiPart.split(" ")[1]);
                System.out.println("        snrPart: " + snrPart.split(" ")[1]);
                try {
                    rssi = Integer.parseInt(rssiPart.split(" ")[1]);
                }catch (NumberFormatException e){
                    System.out.println("         Exception:" + e.getMessage());
                    showReceived();
                }

                try {
                    snr = Integer.parseInt(snrPart.split(" ")[1]);
                }catch (NumberFormatException e){
                    System.out.println("         Exception:" + e.getMessage());
                    showReceived();
                }

                //System.out.println("        RSSI: " + rssi);
                //System.out.println("        SNR: " + snr);
            } else if (line.startsWith("T:")) {
                System.out.println("    Found T:");
                String payload = line.substring(line.indexOf("T:")+2);
                if (payload.length() < 34) {
                    System.out.println("        Payload length is incorrect wrong Length payload");
                    showReceived();
                    return;
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
                    TabForAnswer = AnswerStorage.getTabByIdent(ident);
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
                    showReceived();
                }
            }else{
                System.out.println("        Scip unknown str");
                //showReceived();
                //return;
            }
        }
    }

    private void showReceived(){
        lastAnswer.setLength(0);
        hasAnswer = true;
        hasValue = false;
        for (int i = 0; i < lastAnswerBytes.length; i++) {
            lastAnswer.append( (char) lastAnswerBytes[i]);
        }
    }
    private void parseEbyte(String inputString){
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
            TabForAnswer = AnswerStorage.getTabByIdent(ident);
            System.out.println("        Will be out in " + TabForAnswer + " serial is " + ident);
        } else {
            System.out.print("        CRC check failed for RAK");
            System.out.print("calculate: " + MyUtilities.calculateCRC16_GPS(dataPart.getBytes()));
            System.out.println(" receive: " + Integer.parseInt(crcPart, 16));
        }
    }

    private void parseUnknown(String inputString){

    }
    @Override
    public void sendData(String data, byte [] strEndian, SerialPort comPort, boolean knownCommand, int buffClearTimeLimit, SomeDevice device){

        setCmdToSend(data);
        comPort.flushDataListener();
        //log.info("  Выполнено flushDataListener ");

        if(needRemoveDataListener) {
            comPort.removeDataListener();
        }
        //log.info("  Выполнено removeDataListener ");
        setCmdToSend(data);
        byte[] buffer = new byte[data.length() + strEndian.length];
        for (int i = 0; i < data.length(); i++) {
            buffer[i] = (byte) data.charAt(i);
        }
        //buffer [data.length()] = 13;//CR
        System.arraycopy(strEndian, 0, buffer, data.length() , strEndian.length);

        if(log.isInfoEnabled()){
            //System.out.println("Логирование ответа в файл...");
            StringBuilder sb = new StringBuilder();
            for (byte b : buffer) {
                sb.append((char)b);
            }
            log.info("Parse request ASCII [" + sb.toString().trim() + "] ");
            log.info("Parse request HEX [" + buffer.toString() + "] ");
            sb = null;
        }
        comPort.flushIOBuffers();
        log.info("  Выполнено flushIOBuffers и теперь bytesAvailable " + comPort.bytesAvailable());
        comPort.writeBytes(buffer, buffer.length);
        log.info("  Завершена отправка данных");
        device.setBusy(false);
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

    private static void parseFlags(int flags) {
        boolean limit1 = (flags & (1 << 0)) != 0;
        boolean limit2 = (flags & (1 << 1)) != 0;
        boolean imitation = (flags & (1 << 2)) != 0;
        boolean eepromError = (flags & (1 << 3)) != 0;
        boolean deviceError = (flags & (1 << 8)) != 0;
        boolean rangeError = (flags & (1 << 9)) != 0;
        boolean opticalError = (flags & (1 << 10)) != 0;
        boolean snrError = (flags & (1 << 11)) != 0;
        boolean startReason = (flags & (1 << 13)) != 0;
        boolean dataUrtError = (flags & (1 << 14)) != 0;
        boolean specialMode = (flags & (1 << 15)) != 0;
        boolean relay2Error = (flags & (1 << 16)) != 0;
        boolean currentOutFixed = (flags & (1 << 24)) != 0;
        boolean manual = (flags & (1 << 25)) != 0;
        boolean locked = (flags & (1 << 26)) != 0;
        boolean powerError = (flags & (1 << 27)) != 0;
        boolean magnet = (flags & (1 << 29)) != 0;
        boolean relay1Error = (flags & (1 << 31)) != 0;

        System.out.println("Flags:");
        System.out.println("  LIMIT1: " + limit1);
        System.out.println("  LIMIT2: " + limit2);
        System.out.println("  IMITATION: " + imitation);
        System.out.println("  EEPROM_ERROR: " + eepromError);
        System.out.println("  Device Error: " + deviceError);
        System.out.println("  Range Error: " + rangeError);
        System.out.println("  Optical Error: " + opticalError);
        System.out.println("  SNR Error: " + snrError);
        System.out.println("  Start Reason: " + startReason);
        System.out.println("  Data URT Error: " + dataUrtError);
        System.out.println("  Special Mode: " + specialMode);
        System.out.println("  Relay 2 Error: " + relay2Error);
        System.out.println("  Current Out Fixed: " + currentOutFixed);
        System.out.println("  Manual: " + manual);
        System.out.println("  Locked: " + locked);
        System.out.println("  Power Error: " + powerError);
        System.out.println("  Magnet: " + magnet);
        System.out.println("  Relay 1 Error: " + relay1Error);
    }

    public boolean isBusy(){
        return bisy;
    }

    @Override
    public void setBusy(boolean busy){
        this.bisy = busy;
    }
    public String getAnswer(){
        if(hasAnswer) {

            hasAnswer = false;
            return lastAnswer.toString();
        }else {
            return null;
        }
    }

    public boolean hasAnswer(){
        //System.out.println("return flag " + hasAnswer);
        return hasAnswer;
    }

    public boolean hasValue(){
        return false;
    }

    public AnswerValues getValues(){
        return answerValues;
    }





    private enum CommandList{


        TERM("TERM?", (response) -> {
            // Ваш алгоритм проверки для TERM?

            System.out.print("Start TERM length ");
            System.out.println(response.length);
            AnswerValues answerValues = null;
            if(response.length > 5 && response.length < 10){
                ArrayList <Byte> cleanAnswer= new ArrayList<>();
                for (byte responseByte : response) {
                    if(responseByte > 32 && responseByte < 127)
                        cleanAnswer.add(responseByte);
                    //System.out.print(HexFormat.of().toHexDigits(lastAnswerByte) + " ");
                }

                //System.out.println();
                response = new byte[cleanAnswer.size()];
                StringBuilder sb = new StringBuilder();
                for (byte aByte : cleanAnswer) {
                    sb.append((char) aByte);
                }

                try{
                    Double answer = Double.parseDouble(sb.toString());
                    answer /= 100;
                    if(answer > 80 || answer < -80){
                        throw new NumberFormatException("Wrong number");
                    }else {
                        answerValues = new AnswerValues(1);
                        answerValues.addValue(answer, "°С");
                        System.out.println("degree " + answer);
                    }

                }catch (NumberFormatException ignored){

                }
            }
            //System.out.println("Result " + anAr[0]);
            return answerValues;
        }),
        FF("F", (response) -> {
            // Ваш алгоритм проверки для F
            return null;
        }),
        SREV("SREV?", (response) -> {
            // Ваш алгоритм проверки для SREV?
            return null;
        }),
        ALMH("ALMH?", (response) -> {
            // Ваш алгоритм проверки для SRAL?
            CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
            ByteBuffer byteBuffer = ByteBuffer.wrap(response);
            CoderResult result = decoder.decode(byteBuffer, charBuffer, true);
            if (result.isError()) {
                System.out.println("Error during decoding: " + result.toString());
            }else{
                return null;
            }
            charBuffer.flip();

            StringBuilder sb = new StringBuilder();
            sb.append(charBuffer);
            AnswerValues an = new AnswerValues(1);
            an.addValue(0.0, sb.toString());
            charBuffer.clear();
            sb.setLength(0);
            return an;
        });

        private final String name;
        private final Function<byte [], AnswerValues> parseFunction;
        private static final List<String> VALUES;

        static {
            VALUES = new ArrayList<>();
            for (CommandList someEnum : CommandList.values()) {
                VALUES.add(someEnum.name);
            }
        }

        CommandList(String name, Function<byte [], AnswerValues> parseFunction) {
            this.name = name;
            this.parseFunction = parseFunction;
        }

        public String getValue() {
            return name;
        }

        public static List<String> getValues() {
            return Collections.unmodifiableList(VALUES);
        }

        public static String getLikeArray(int number) {
            List<String> values = CommandList.getValues();
            return values.get(number);
        }

        public AnswerValues parseAnswer(byte [] response) {
            return parseFunction.apply(response);
        }

        public static CommandList getCommandByName(String name) {
            for (CommandList command : CommandList.values()) {
                if (command.name.equals(name)) {
                    return command;
                }
            }
            return null;
        }
    }
}
