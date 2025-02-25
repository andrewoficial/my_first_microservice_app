package org.example.device;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
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

import static org.example.utilites.MyUtilities.*;
import static org.example.utilites.MyUtilities.isCorrectNumberF;

public class IGM_10 implements SomeDevice {
    private volatile boolean busy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte [ ] lastAnswerBytes;
    private StringBuilder lastAnswer = new StringBuilder();
    private final StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private  volatile boolean hasValue = false;
    @Setter
    private byte [] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 750;
    private final long repeatWaitTime = 200;
    private final long millisPrev = System.currentTimeMillis();
    private final static Charset charset = Charset.forName("Cp1251");
    private static final CharsetDecoder decoder = charset.newDecoder();
    private AnswerValues answerValues = null;
    String cmdToSend;
    private Integer TabForAnswer;
    private String devIdent = "IGM_10_ASCII";
    private int expectedBytes = 0;
    private CommandListClass commands = new CommandListClass();

    public IGM_10(SerialPort port){
        System.out.println("Создан объект протокола ИГМ-10");
        this.comPort = port;
        this.enable();
    }

    public IGM_10(){
        System.out.println("Создан объект протокола ИГМ-10 эмуляция");
        this.comPort = null;
    }
    @Override
    public void setCmdToSend(String str) {
        //Получает количесвто одидаемых байт душная история
        expectedBytes = commands.getExpectedBytes(str); //ToDo распространить на сотальные девайсы
        cmdToSend = str;
    }

    @Override
    public int getExpectedBytes(){
        return expectedBytes;
    }

    @Override
    public Integer getTabForAnswer() {
        return null;
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
        return 700;
    }

    @Override
    public int getMillisWriteLimit() {
        return 400;
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

    public boolean enable() throws RuntimeException{
            return true;
    }


    public void setReceived(String answer){
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        //System.out.println("IGM_10_ASCII run parse");
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) { //ToDo Распространить эту проверку
            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                if(answerValues != null){
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        if(i == 0){
                            double ans = answerValues.getValues()[i] * 100;
                            ans = Math.round(ans) / 100.0;
                            lastAnswer.append(ans);
                            lastAnswer.append("\t");
                        }else {
                            lastAnswer.append(Math.round(answerValues.getValues()[i]));
                            lastAnswer.append("\t");
                        }
                    }
                }else{
                    System.out.println("IGM-10-ASCII Can`t create answers obj (error in answer)");

                }

            }else {
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                System.out.println("IGM-10-ASCII Cant create answers obj (unknown command)");
            }
        }else{
            System.out.println("IGM-10-ASCII empty received");
        }

    }


    public boolean isBusy(){
        return busy;
    }

    @Override
    public void setBusy(boolean busy){
        this.busy = busy;
    }
    public String getAnswer(){
        if(lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            received = 0;
            lastAnswerBytes = null;
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
        return this.answerValues;
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

    {
        commands.addCommand(
                new SingleCommand(
                        "F", "F - Основная команда опроса", //72 байта
                        (response) -> {
                            answerValues = null;
                            String example = "02750\t07518\t00023\t00323\t08614\t01695\t06353\t03314\t03314\t00001\t08500012\t0x0D";
                            System.out.println("Got string (array) [" + Arrays.toString(response) + "]");
                            //System.out.println("Got string (string) [" + MyUtilities.byteArrayToString(response) + "] ");

                            if (response.length >= 72) {
                                if (response[72] != 13) {
                                    System.out.println("Не найден CR");
                                    //resetAnswerValues();
                                    //return null;

                                }
                                double value = 0.0;
                                double serialNumber = 0.0;
                                answerValues = new AnswerValues(11);
                                byte subResponse[] = Arrays.copyOfRange(response, 1, 5);
                                if (isCorrectNumberF(subResponse)) {
                                    boolean success = true;

                                    // Преобразуем байты ASCII-чисел вручную
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }

                                    // Применяем десятичный порядок для получения числа в формате 123.45
                                    if (success) {
                                        value = (value*value*-0.0000006)-(value*0.0249)+122.08;
                                        answerValues.addValue(value, " °C");
                                        //System.out.println(termBM);
                                    } else {
                                        answerValues.addValue(-88.88, " °C(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("Wrong isCorrectNumberF position (0)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " °C(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 7, 12);
                                if (isCorrectNumberF(subResponse)) {
                                    boolean success = true;
                                    // Преобразуем байты ASCII-чисел вручную
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        // Проверяем, что символ – цифра
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }

                                    // Применяем десятичный порядок для получения числа в формате 123.45
                                    if (success) {
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 1(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("Wrong isCorrectNumberF position (1)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 1(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 13, 18);
                                if (isCorrectNumberF(subResponse)) {
                                    boolean success = true;
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }
                                    if (success) {
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 2(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (2)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 2(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 19, 24);  //Напряжение питания
                                if (isCorrectNumberF(subResponse)) {
                                    boolean success = true;
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }
                                    if (success) {
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 3(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (3)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 3(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 25, 30);  //Сила тока шунт один до полинома
                                if (isCorrectNumberF(subResponse)) {
                                    boolean success = true;
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }
                                    if (success) {
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 4(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (4)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 4(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 31, 36);  // Сила тока шунт два до полинома
                                byte[]  subResponse_short = Arrays.copyOfRange(response, 32, 36);  // Сила тока шунт два до полинома
                                if (isCorrectNumberF(subResponse_short)) {
                                    boolean success = true;
                                    int sign = 1; // По умолчанию знак положительный
                                    int startIndex = 0; // Индекс, с которого начинаем обработку цифр

                                    // Проверяем первый байт на наличие знака минус
                                    if (subResponse.length > 0 && subResponse[0] == '-') {
                                        sign = -1; // Устанавливаем знак минус
                                        startIndex = 1; // Начинаем обработку со второго байта
                                    }

                                    for (int i = startIndex; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }

                                    if (success) {
                                        value *= sign; // Умножаем значение на знак
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 5(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (5)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 5(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 37, 42);  // Сила тока шунт два до полинома
                                subResponse_short = Arrays.copyOfRange(response, 38, 42);  // Сила тока шунт два до полинома
                                if (isCorrectNumberF(subResponse_short)) {
                                    boolean success = true;
                                    int sign = 1; // По умолчанию знак положительный
                                    int startIndex = 0; // Индекс, с которого начинаем обработку цифр

                                    // Проверяем первый байт на наличие знака минус
                                    if (subResponse.length > 0 && subResponse[0] == '-') {
                                        sign = -1; // Устанавливаем знак минус
                                        startIndex = 1; // Начинаем обработку со второго байта
                                    }

                                    for (int i = startIndex; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }

                                    if (success) {
                                        value *= sign; // Умножаем значение на знак
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 6(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (6)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 6(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 43, 48);  //Сила тока шунт два после полинома
                                if (isCorrectNumberFExceptMinus(subResponse)) {
                                    boolean success = true;
                                    boolean isNegative = false;

                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (i == 0 && b == '-' ) {
                                            isNegative = true;
                                            continue;
                                        }

                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        }
                                    }
                                    if (success) {
                                        value = isNegative ? -value : value; // Применяем знак
                                        answerValues.addValue(value, " Units");
                                    } else {
                                        answerValues.addValue(-88.88, " 7(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (7)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 7(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 49, 54);  //Сила тока выбранный результат (ардуиной)
                                if (isCorrectNumberFExceptMinus(subResponse)) {
                                    boolean success = true;
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        }
                                    }
                                    if (success) {
                                        answerValues.addValue(value, " C0");
                                    } else {
                                        answerValues.addValue(-88.88, " 8(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (8)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 8(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 55, 60);  //Статус
                                if (isCorrectNumberF(subResponse)) {
                                    boolean success = true;
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b >= '0' && b <= '9') {
                                            value = value * 10 + (b - '0');
                                        } else {
                                            success = false;
                                            break;
                                        }
                                    }
                                    if (success) {
                                        answerValues.addValue(value, " C1");
                                    } else {
                                        answerValues.addValue(-88.88, " 9(ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("isCorrectNumberF found error in position (9)" + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " 9(ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;

                                subResponse = Arrays.copyOfRange(response, 61, 69);  //Серийный номер
                                if (isCorrectNumberF(subResponse)) {
                                    boolean isNegative = false;
                                    boolean success = true;
                                    // Преобразуем байты ASCII-чисел вручную
                                    for (int i = 0; i < subResponse.length; i++) {
                                        byte b = subResponse[i];
                                        if (b == '-' && i == 0) {
                                            isNegative = true;
                                            continue;
                                        }
                                        // Проверяем, что символ – цифра
                                        if (b >= '0' && b <= '9') {
                                            serialNumber = serialNumber * 10 + (b - '0');
                                        } else {
                                            serialNumber = 0.0;
                                            success = false;
                                            break;
                                        }

                                    }
                                    value = 0.0;

                                    // Применяем десятичный порядок для получения числа в формате 123.45
                                    if (success) {
                                        //stat /= 1000.0; // Сдвигаем запятую на два знака влево
                                        TabForAnswer = AnswerStorage.getTabByIdent(String.valueOf(serialNumber));
                                        devIdent = String.valueOf(serialNumber);
                                        serialNumber = isNegative ? - serialNumber : serialNumber; // Применяем знак
                                        answerValues.addValue(serialNumber, " SN");
                                        //System.out.println(termBM);
                                    } else {
                                        answerValues.addValue(-88.88, " SN (ERR)");
                                        resetAnswerValues();
                                        return null;
                                    }
                                } else {
                                    System.out.println("Wrong isCorrectNumberF position (serialNumber) 61, 69 " + Arrays.toString(subResponse));
                                    answerValues.addValue(-99.99, " SN (ERR)");
                                    resetAnswerValues();
                                    return null;
                                }
                                value = 0.0;



                                System.out.println("Answer size: " + answerValues.getCounter());
                                return answerValues;
                            } else {
                                System.out.println("Wrong answer length " + response.length);
                                for (byte b : response) {
                                    System.out.print(b + " ");
                                }
                                System.out.println();
                                resetAnswerValues();
                                return null;
                            }
                        }, 73)
        );
    }

    public void resetAnswerValues() {
        answerValues = null;
        lastAnswerBytes = null;
        lastAnswer.setLength(0);

    }
}
