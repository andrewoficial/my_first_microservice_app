package org.example.device;

import java.util.zip.CRC32;
import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.utilites.CommandListClass;
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

public class IGM_10LORA_P2P implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(IGM_10LORA_P2P.class);
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
    private final long millisLimit = 500000;
    private final long repeatGetAnswerTimeDelay = 1;
    private final int buffClearTimeLimit = 5;
    private final int repetCounterLimit = 200;
    private final long millisPrev = System.currentTimeMillis();
    private final static Charset charset = Charset.forName("Cp1251");
    private static final CharsetDecoder decoder = charset.newDecoder();
    private AnswerValues answerValues = new AnswerValues(0);
    String cmdToSend;
    Integer TabForAnswer;

    public IGM_10LORA_P2P(SerialPort port){
        System.out.println("Создан объект протокола ИГМ-10 LoRa P2P");
        this.comPort = port;
        this.enable();
    }
    public IGM_10LORA_P2P(){
        System.out.println("Создан объект протокола IGM_10LORA_P2P эмуляция");
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
    public long getRepeatGetAnswerTimeDelay() {
        return repeatGetAnswerTimeDelay;
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
    public int getBuffClearTimeLimit() {
        return this.buffClearTimeLimit;
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {
        this.hasAnswer = hasAnswer;
    }

    private CommandListClass commands = new CommandListClass();
    public void enable() {
        comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 65, 65);
        System.out.println("Порт открыт, задержки выставлены IGM_10LORA_P2P");
    }

    @Override
    public int getRepetCounterLimit() {
        return repetCounterLimit;
    }

    public void setReceived(String answer){
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        System.out.println("Start parse... IGM10 LoRa P2P");
            if(lastAnswerBytes.length > 0) {
                //System.out.println("Run Parser: ");
                    CommandList cmd = CommandList.getCommandByName(cmdToSend);
                    //System.out.println("    cmdToSend: " + cmdToSend);
                    //lastAnswer = new StringBuilder(lastAnswer.toString().replaceAll("\\p{C}", "?"));
                    if (cmd != null) {
                        answerValues = cmd.parseAnswer(lastAnswerBytes);

                    }

                    if (answerValues == null || cmd == null){
                        //System.out.println("    answerValues == null || cmd == null ");
                        //System.out.println("    answerValues: " + answerValues);
                        //for (byte lastAnswerByte : lastAnswerBytes) {
                            //System.out.print(lastAnswerByte);  System.out.print(" ");
                        //}
                        //System.out.println();

                        String inputString = new String(lastAnswerBytes);
                        inputString = inputString.replace("\r", "");
                        inputString = inputString.replace("\n", "");
                        inputString = inputString.replace("+", "");
                        String[] lines = inputString.split("EV");
//                        System.out.println("    Array Size = " + lines.length);
//                        for (String line : lines) {
//                            System.out.println(line);
//                        }
                        int rssi = 0;
                        int snr = 0;
                        answerValues = null;
                        for (String line : lines) {
                            //System.out.println("Run iter");
                            if (line.startsWith("T:RXP2P")) {
                                //System.out.println("    Found T:RXP2P");
                                String[] parts = line.split(",");
                                if (parts.length < 3) {
                                    //System.out.println("        Invalid format RXP2P not found");
                                    return;
                                }

                                String rssiPart = parts[1].trim();
                                String snrPart = parts[2].trim();

                                if (!rssiPart.startsWith("RSSI") || !snrPart.startsWith("SNR")) {
                                    System.out.println("        Invalid format RSSI or SNR not found");
                                    return;
                                }
                                //System.out.println("        rssiPart: " + rssiPart.split(" ")[1]);
                                //System.out.println("        snrPart: " + snrPart.split(" ")[1]);
                                try {
                                    rssi = Integer.parseInt(rssiPart.split(" ")[1]);
                                }catch (NumberFormatException e){
                                    System.out.println("         Exception:" + e.getMessage());
                                }

                                try {
                                    snr = Integer.parseInt(snrPart.split(" ")[1]);
                                }catch (NumberFormatException e){
                                    System.out.println("         Exception:" + e.getMessage());
                                }

                                //System.out.println("        RSSI: " + rssi);
                                //System.out.println("        SNR: " + snr);
                            } else if (line.startsWith("T:")) {
                                //System.out.println("    Found T:");
                                String payload = line.substring(line.indexOf("T:")+2);
                                if (payload.length() < 30) {
                                    System.out.println("        Payload length is incorrect wrong Length payload");
                                    return;
                                }

                                String dataPart = payload.substring(0, 28);
                                //System.out.println("      MSG: " + dataPart);
                                String crcPart = payload.substring(28, 32);
                                //System.out.println("      CRC: " + crcPart);
                                answerValues = new AnswerValues(4);
                                answerValues.addValue(rssi, "RSSI");
                                answerValues.addValue(snr, "SNR");
                                if (MyUtilities.checkCRC16(dataPart, crcPart)) {
                                    //System.out.println("        CRC OK");
                                    String ident = parsePayload(dataPart, answerValues);
                                    TabForAnswer = AnswerStorage.getTabByIdent(ident);
                                    //System.out.println("        Will be out in " + TabForAnswer + " serial is " + ident);
                                } else {
                                    System.out.println("        CRC check failed");
                                }
                            }
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
                    //System.out.println("done correct...");

                }else{
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        System.out.print(lastAnswerByte);
                        System.out.print(" ");
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    System.out.println("done unknown...");
                    hasAnswer = true;
                }
            }else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    System.out.print(lastAnswerByte);
                    System.out.print(" ");
                    lastAnswer.append((char) lastAnswerByte);
                }
                System.out.println("done empty...");
                hasAnswer = false;
            }


    }

    private static String parsePayload(String dataPart, AnswerValues ans) {
        // Temperature
        int temperature = Integer.parseInt(dataPart.substring(0, 4), 16);
        if (temperature > 32767) {
            temperature -= 65536;
        }
        double temperatureInCelsius = temperature / 10.0;
        //System.out.println("Temperature: " + temperatureInCelsius + "°C");
        ans.addValue(temperatureInCelsius, "°C");
        // Gas number
        int gasNumber = Integer.parseInt(dataPart.substring(4, 6), 16);
        //System.out.println("Gas Number: " + gasNumber);

        // Measurement result
        int measurementResult = Integer.parseInt(dataPart.substring(6, 10), 16);
        double resultValue = measurementResult / 10.0;
        //System.out.println("Measurement Result: " + resultValue);

        // Units
        int units = Integer.parseInt(dataPart.substring(10, 12), 16);
        //System.out.println("Units: " + units);

        // Flags
        int flags = Integer.parseInt(dataPart.substring(12, 20), 16);
        //parseFlags(flags);

        // Serial number
        int serialNumber = Integer.parseInt(dataPart.substring(20, 28), 16);
        //System.out.println("Serial Number: " + serialNumber);
        ans.addValue(resultValue, " GAS: " + gasNumber + " UNITS:" + units + " SERIAL:" + serialNumber);
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

    @Override
    public boolean isBisy(){
        return bisy;
    }

    @Override
    public void setBisy(boolean bisy){
        this.bisy = bisy;
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
