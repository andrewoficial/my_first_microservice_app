//package org.example.device;
//
//import com.fazecast.jSerialComm.SerialPort;
//import lombok.Setter;
//import org.apache.log4j.Logger;
//import org.example.services.AnswerStorage;
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
//public class IGM_10LORA_P2P implements SomeDevice {
//    private volatile boolean busy = false;
//    private static final Logger log = Logger.getLogger(IGM_10LORA_P2P.class);
//    private final SerialPort comPort;
//    private byte [ ] lastAnswerBytes;
//    private StringBuilder lastAnswer = new StringBuilder();
//    private final StringBuilder emulatedAnswer = new StringBuilder();
//    private final boolean knownCommand = false;
//    private volatile boolean hasAnswer = false;
//
//    @Setter
//    private byte [] strEndian = {13, 10};//CR, LF
//    private int received = 0;
//    private final long millisLimit = 30;
//    private final long repeatWaitTime = 10;
//    private final long millisPrev = System.currentTimeMillis();
//    private final static Charset charset = Charset.forName("Cp1251");
//    private static final CharsetDecoder decoder = charset.newDecoder();
//    private AnswerValues answerValues = new AnswerValues(0);
//    private boolean needRemoveDataListener = true;
//    String cmdToSend;
//    Integer TabForAnswer;
//
//    public IGM_10LORA_P2P(SerialPort port){
//        System.out.println("Создан объект протокола ИГМ-10 LoRa P2P");
//        this.comPort = port;
//        this.enable();
//    }
//    public IGM_10LORA_P2P(){
//        System.out.println("Создан объект протокола IGM_10LORA_P2P эмуляция");
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
//    public boolean enable() {
//        setReceiveMode();
//        return false;
//    }
//
//
//    public void setReceived(String answer){
//        this.received = answer.length();
//        this.parseData();
//    }
//
//    @Override
//    public void parseData() {
//        //System.out.println("Start parse... IGM10 LoRa P2P");
//            if(lastAnswerBytes.length > 0) {
//                        String inputString = new String(lastAnswerBytes);
//                        inputString = inputString.replace("\r", "");
//                        inputString = inputString.replace("\n", "");
//                        inputString = inputString.replace("+", "");
//                        String[] lines = inputString.split("EV");
//                        if(inputString.contains("EVT:RXP2P") || inputString.contains("CEIVE TIME")) {
//                            setReceiveMode();
//                        }
//
//
//                        int rssi = 0;
//                        int snr = 0;
//                        answerValues = null;
//                        for (String line : lines) {
//                            if (line.startsWith("T:RXP2P")) {
//                                String[] parts = line.split(",");
//                                if (parts.length < 3) {
//                                    showReceived();
//                                    System.out.println("        Invalid format RXP2P not found");
//                                    return;
//                                }
//
//                                String rssiPart = parts[1].trim();
//                                String snrPart = parts[2].trim();
//
//                                if (!rssiPart.startsWith("RSSI") || !snrPart.startsWith("SNR")) {
//                                    showReceived();
//                                    System.out.println("        Invalid format RSSI or SNR not found");
//                                    return;
//                                }
//
//                                try {
//                                    rssi = Integer.parseInt(rssiPart.split(" ")[1]);
//                                }catch (NumberFormatException e){
//                                    showReceived();
//                                    System.out.println("         Exception:" + e.getMessage());
//                                }
//
//                                try {
//                                    snr = Integer.parseInt(snrPart.split(" ")[1]);
//                                }catch (NumberFormatException e){
//                                    showReceived();
//                                    System.out.println("         Exception:" + e.getMessage());
//                                }
//
//                                //System.out.println("        RSSI: " + rssi);
//                                //System.out.println("        SNR: " + snr);
//                            } else if (line.startsWith("T:")) {
//                                //System.out.println("    Found T:");
//                                String payload = line.substring(line.indexOf("T:")+2);
//                                if (payload.length() < 30) {
//                                    System.out.println("        Payload length is incorrect wrong Length payload");
//                                    showReceived();
//                                    return;
//                                }
//
//                                String dataPart = payload.substring(0, 28);
//                                //System.out.println("      MSG: " + dataPart);
//                                String crcPart = payload.substring(28, 32);
//                                //System.out.println("      CRC: " + crcPart);
//                                answerValues = new AnswerValues(4);
//                                answerValues.addValue(rssi, "RSSI");
//                                answerValues.addValue(snr, "SNR");
//                                if (MyUtilities.checkCRC16(dataPart, crcPart)) {
//                                    String ident = parsePayload(dataPart, answerValues);
//                                    TabForAnswer = AnswerStorage.getTabByIdent(ident);
//                                    //System.out.println("        Will be out in " + TabForAnswer + " serial is " + ident);
//                                } else {
//                                    System.out.println("        CRC check failed");
//                                }
//                            }
//                        }
//
//                lastAnswer.setLength(0);
//                if(answerValues != null){
//                    hasAnswer = true;
//                    for (int i = 0; i < answerValues.getValues().length; i++) {
//                        lastAnswer.append(answerValues.getValues()[i]);
//                        lastAnswer.append(" ");
//                        lastAnswer.append(answerValues.getUnits()[i]);
//                        lastAnswer.append("  ");
//                    }
//                    //System.out.println("done correct...");
//
//                }else{
//                    showReceived();
//                    hasAnswer = true;
//                }
//            }else {
//                showReceived();
//                //System.out.println("done empty...");
//                hasAnswer = false;
//            }
//
//
//    }
//
//    private void setReceiveMode(){
//        //System.out.println("Send AT+PRECV=65535");
//        boolean flipFlopBusy  = this.isBusy();
//        byte [] tmpLastAnswer = lastAnswerBytes;
//
//        needRemoveDataListener = false;
//
//        //this.sendData("AT+PRECV=65535", strEndian, this.comPort, true, 5, this);
//        //ToDo вместо этого просто засылать в класс, курирующий сериал порт
//        //this.receiveData(this);
//        //ToDo вместо этого просто вешать слушателя и ловить ответ
//
//        needRemoveDataListener = true;
//
//
//        //StringBuilder sb = new StringBuilder();
//        //for (int i = 0; i < lastAnswerBytes.length; i++) {
//        //    sb.append( (char) lastAnswerBytes[i]);
//        //}
//        //System.out.println("Answer:" + sb.toString());
//
//        lastAnswerBytes = tmpLastAnswer;
//    }
//    private void showReceived(){
//        lastAnswer.setLength(0);
//        hasAnswer = true;
//        for (int i = 0; i < lastAnswerBytes.length; i++) {
//            lastAnswer.append( (char) lastAnswerBytes[i]);
//        }
//    }
//
//    //Метод парсинга посылки после проверки СRC сделан публичным и статическим....
//    public static String parsePayload(String dataPart, AnswerValues ans) {
//        int temperature = 0, gasNumber = 0, measurementResult = 0, units = 0, flags = 0, serialNumber = 0;
//
//        try {
//            temperature = parseHex(dataPart, 0, 4);
//            if (temperature > 32767) temperature -= 65536;
//            ans.addValue(temperature / 100.0, "°C"); //TODO ВНЁС ПРАВКУ В СПЕШКЕ!
//        } catch (IllegalArgumentException e) {
//            System.err.println("Temperature error: " + e.getMessage());
//        }
//
//        try {
//            gasNumber = parseHex(dataPart, 4, 6);
//        } catch (IllegalArgumentException e) {
//            System.err.println("Gas number error: " + e.getMessage());
//        }
//
//        try {
//            measurementResult = parseHex(dataPart, 6, 10);
//            ans.addValue(measurementResult / 10.0, " GAS: " + gasNumber);
//        } catch (IllegalArgumentException e) {
//            System.err.println("Measurement error: " + e.getMessage());
//        }
//
//        try {
//            units = parseHex(dataPart, 10, 12);
//        } catch (IllegalArgumentException e) {
//            System.err.println("Units error: " + e.getMessage());
//        }
//
//        try {
//            flags = parseHex(dataPart, 12, 20);
//        } catch (IllegalArgumentException e) {
//            System.err.println("Flags error: " + e.getMessage());
//        }
//
//        try {
//            serialNumber = parseHex(dataPart, 20, 28);
//            ans.addValue(units, "UNITS:" + units + " SERIAL:" + serialNumber);
//        } catch (IllegalArgumentException e) {
//            System.err.println("Serial error: " + e.getMessage());
//            return null;
//        }
//
//        return String.valueOf(serialNumber);
//    }
//
//    private static int parseHex(String data, int start, int end) throws IllegalArgumentException {
//        try {
//            return Integer.parseInt(data.substring(start, end), 16);
//        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
//            throw new IllegalArgumentException("Invalid hex at [" + start + "-" + end + "]: " +
//                    data.substring(start, Math.min(end, data.length())));
//        }
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
//
//    public boolean isBusy(){
//        return busy;
//    }
//
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
