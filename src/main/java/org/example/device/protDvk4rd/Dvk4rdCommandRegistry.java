package org.example.device.protDvk4rd;

import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;

import java.util.Arrays;

import static org.example.utilites.MyUtilities.*;
import static org.example.utilites.MyUtilities.isCorrectNumberF;

public class Dvk4rdCommandRegistry extends DeviceCommandRegistry {
    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
        // Добавление других команд
    }
    
    private SingleCommand createFCommand() {
        return new SingleCommand(
            "F", 
            "F - Основная команда опроса", 
            this::parseFResponse, 
            72
        );
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
            answerValues = new AnswerValues(10);
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
                    value = (value*value*-0.0000006)-(value*0.0249)+122.08;
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

            subResponse = Arrays.copyOfRange(response, 37, 42);  //Сила тока шунт один после полинома
            if (isCorrectNumberF(subResponse)) {
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