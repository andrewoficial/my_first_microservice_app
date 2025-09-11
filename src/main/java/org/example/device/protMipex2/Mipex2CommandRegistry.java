package org.example.device.protMipex2;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.device.protMipex2.parsers.ZERO2Parser;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;

import java.util.Arrays;

import static org.example.utilites.MyUtilities.*;

public class Mipex2CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(Mipex2CommandRegistry.class);
    private final ZERO2Parser fuluParser = new ZERO2Parser();
    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
        commandList.addCommand(createFuluCommand());
        commandList.addCommand(createSetConcCommand());
        // Добавление других команд
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
            "F",
            "Получение текущих значенией",
            this::parseFResponse,
            74
        );
    }

    private SingleCommand createFuluCommand() {
        return new SingleCommand(
                "ZERO2",
                "Установка нуля",
                this::parseFULUResponse,
                50
        );
    }


    private AnswerValues parseFULUResponse(byte[] response){
        return this.fuluParser.parseZero2Response(response);
    }

    private SingleCommand createSetConcCommand() {
        byte[] baseBody = "CALB 0000".getBytes();
        ZERO2Parser parser = new ZERO2Parser();
        SingleCommand command = new SingleCommand(
                "CALB ",
                "CALB 0225 - Установка 2.25 VOL",
                "setConc",
                baseBody, // Dynamic
                args -> {
                    Float value = (Float) args.getOrDefault("value", 0.0f);
                    // Добавляем префикс "CALB "
                    return buildCommand("CALB", value).getBytes();
                },
                null,
                60,
                CommandType.BINARY
        );
        command.addArgument(new ArgumentDescriptor(
                "value",
                Float.class,
                2.2f,
                val -> (Float) val >= 0
        ));
        return command;
    }

    public String buildCommand(String body, double value) {
        // Умножаем на 100 и преобразуем в целое число
        int intValue = (int) Math.round(value * 100);

        // Форматируем в 4-значное число с ведущими нулями
        String formatted = String.format("%04d", intValue);

        // Добавляем префикс "CALB "
        return body + " " + formatted;
    }
    private AnswerValues parseFResponse(byte[] response) {
        AnswerValues answerValues = null;
        String example = "02750\t07518\t00023\t00323\t08614\t01695\t06353\t03314\t03314\t00001\t08500012\t0x0D";

        if (response.length >= 68) {
            if (response[70] != calculateCRCforF(response)) {
                System.out.println("ERROR CRC for F");
                System.out.println("Expected CRC " + calculateCRCforF(response) + " received " + response[70]);
                return null;

            }
            double value = 0.0;
            double serialNumber = 0.0;
            answerValues = new AnswerValues(11);
            //===================================================1...6==============Term================================
            byte subResponse[] = Arrays.copyOfRange(response, 1, 6);
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
                    //=-0,00000002*B3^2 - 0,0412*B3 + 93,116
                    value = (value*value*-0.00000002)-(value*0.0412)+93.116;
                    answerValues.addValue(value, " °C");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " °C(ERR)");
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position " + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " °C(ERR)");
                return null;
            }

            //===================================================7...12==============St=================================
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
                    answerValues.addValue(value, " St");
                } else {
                    answerValues.addValue(-88.88, " 1(ERR)");
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (1)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 1(ERR)");
                return null;
            }
            //===================================================13...18==============Us================================
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
                    answerValues.addValue(value, " Us");
                } else {
                    answerValues.addValue(-88.88, " 2(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (2)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 2(ERR)");
                return null;
            }
            //===================================================19...24==============Uref==============================
            value = 0.0;

            subResponse = Arrays.copyOfRange(response, 19, 24);
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
                    answerValues.addValue(value, " Uref");
                } else {
                    answerValues.addValue(-88.88, " 3(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (3)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 3(ERR)");
                return null;
            }
            //===================================================25...30==============Stz0==============================
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
                    answerValues.addValue(value, " Stz0");
                } else {
                    answerValues.addValue(-88.88, " 4(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (4)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 4(ERR)");
                return null;
            }
            //===================================================31...36==============Stz===============================
            value = 0.0;

            subResponse = Arrays.copyOfRange(response, 31, 36);  //Сила тока шунт два до полинома
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
                    answerValues.addValue(value, " Stz");
                } else {
                    answerValues.addValue(-88.88, " 5(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (5)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 5(ERR)");
                return null;
            }
            //===================================================37...42==============Stzkt=============================
            value = 0.0;

            subResponse = Arrays.copyOfRange(response, 37, 42);  //Сила тока шунт один после полинома
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
                    answerValues.addValue(value, " Stzkt");
                } else {
                    answerValues.addValue(-88.88, " 6(ERR)");
                    return null;
                }

            } else {
                System.out.println("isCorrectNumberF found error in position (6)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 6(ERR)");
                return null;
            }
            //===================================================43...48==============C0================================
            value = 0.0;

            subResponse = Arrays.copyOfRange(response, 43, 48);  //Сила тока шунт два после полинома
            if (isCorrectNumberFExceptMinus(subResponse)) {
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
                    answerValues.addValue(value, " Конц. заводск. калибровка (C0)");
                } else {
                    answerValues.addValue(-88.88, " 7(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (7)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 7(ERR)");
                return null;
            }
            //===================================================49...54==============C1================================
            value = 0.0;

            subResponse = Arrays.copyOfRange(response, 49, 54);  //Сила тока выбранный результат (ардуиной)
            if (isCorrectNumberFExceptMinus(subResponse)) {
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
                    answerValues.addValue(value, " Конц. пользов. калибровка (C1)");
                } else {
                    answerValues.addValue(-88.88, " 7(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (8)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 8(ERR)");
                return null;
            }
            //===================================================55...60==============C1================================
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
                    answerValues.addValue(value, " Статус");
                } else {
                    answerValues.addValue(-88.88, " 9(ERR)");
                    return null;
                }
            } else {
                System.out.println("isCorrectNumberF found error in position (9)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " 9(ERR)");
                return null;
            }
            //===================================================61...69==============SN================================
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



                    serialNumber = isNegative ? - serialNumber : serialNumber; // Применяем знак
                    answerValues.addValue(serialNumber, " SN");
                    if(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)) != null){
                        answerValues.setDirection(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)));
                    }
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " SN (ERR)");
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (serialNumber)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " SN (ERR)");
                return null;
            }
            value = 0.0;
            return answerValues;
        } else {
            System.out.println("Wrong answer length " + response.length);
            for (byte b : response) {
                System.out.print(b + " ");
            }
            System.out.println();
            return null;
        }
    }





}