package org.example.device.protIgm10.ascii;


import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;

import org.example.device.command.SingleCommand;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.ArrayList;
import java.util.Arrays;

import static org.example.utilites.MyUtilities.isCorrectNumberF;
import static org.example.utilites.MyUtilities.isCorrectNumberFExceptMinus;


public class Igm10AsciiCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(Igm10AsciiCommandRegistry.class);
    private final static Charset charset = Charset.forName("Cp1251");
    private static final CharsetDecoder decoder = charset.newDecoder();

    @Override
    protected void initCommands() {
        commandList.addCommand(createFCmd());
        commandList.addCommand(createAlmhGuestionCmd());
        commandList.addCommand(createTermQuestionCmd());

        // Добавление других команд
    }

    private SingleCommand createFCmd() {
        return new SingleCommand(
                "F",
                "F - Основная команда опроса",
                this::parseFCmd,
                73
        );
    }

    private SingleCommand createAlmhGuestionCmd() {
        return new SingleCommand(
                "ALMH?",
                "ALMH? - запрос текущего измеряемого газа.",
                this::parseAlmhGuestionCmd,
                85
        );
    }

    private SingleCommand createTermQuestionCmd() {
        return new SingleCommand(
                "TERM?",
                "TERM? - запрос температуры.",
                this::parseTermQuestionCmd,
                5000
        );
    }


    private AnswerValues parseFCmd(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed F ");
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
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (0)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " °C(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (1)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 1(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (2)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 2(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (3)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 3(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (4)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 4(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (5)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 5(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (6)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 6(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (7)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 7(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (8)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 8(ERR)");
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
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (9)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 9(ERR)");
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
                    if(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)) != null){
                        answerValues.setDirection(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)));
                    }


                    String devIdent = String.valueOf(serialNumber);
                    serialNumber = isNegative ? - serialNumber : serialNumber; // Применяем знак
                    answerValues.addValue(serialNumber, " SN");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " SN (ERR)");
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (serialNumber) 61, 69 " + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " SN (ERR)");
                return null;
            }
            value = 0.0;



            System.out.println("Answer size: " + answerValues.getCounter());
            return answerValues;
        } else {

            log.warn("Wrong answer length " + response.length);
            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                sb.append((char) b).append(" ");
            }
            log.info("Wrong answer: " + sb.toString());
            return null;
        }
    }


    private AnswerValues parseAlmhGuestionCmd(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed ALMH? ");
        //String example = "5.229";
        CharBuffer charBuffer = CharBuffer.allocate(512);  // Предполагаемый максимальный размер
        ByteBuffer byteBuffer = ByteBuffer.wrap(response);
        CoderResult result = decoder.decode(byteBuffer, charBuffer, true);
        if (result.isError()) {
            log.warn("Error during decoding: " + result.toString());
            return null;
        }
        log.info("Decode result: " + result.toString());
        charBuffer.flip();

        StringBuilder sb = new StringBuilder();
        sb.append(charBuffer);
        AnswerValues an = new AnswerValues(1);
        an.addValue(0.0, sb.toString());
        charBuffer.clear();
        sb.setLength(0);
        return an;
    }

    private AnswerValues parseTermQuestionCmd(byte[] response) {//
        AnswerValues answerValues = null;
        log.info("Proceed TERM? ");
        if(response.length > 5 && response.length < 10){
            ArrayList<Byte> cleanAnswer= new ArrayList<>();
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
                    log.warn("Wrong number size" + answer);
                    throw new NumberFormatException("Wrong number");
                }else {
                    answerValues = new AnswerValues(1);
                    answerValues.addValue(answer, "°С");
                    log.info("degree " + answer);
                }

            }catch (NumberFormatException ignored){
                log.warn("Wrong number format" + sb.toString());
            }
        }
        //System.out.println("Result " + anAr[0]);
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