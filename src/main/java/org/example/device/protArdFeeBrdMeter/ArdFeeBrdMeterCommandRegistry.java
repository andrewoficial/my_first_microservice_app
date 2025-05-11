package org.example.device.protArdFeeBrdMeter;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.SingleCommand;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.connectionPool.ComDataCollector;

import java.util.Arrays;

import static org.example.utilites.MyUtilities.*;
import static org.example.utilites.MyUtilities.isCorrectNumberF;

public class ArdFeeBrdMeterCommandRegistry extends DeviceCommandRegistry {
    private final static Logger log = Logger.getLogger(ArdFeeBrdMeterCommandRegistry.class); // Объект логера

    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
        // Добавление других команд
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "F",
                "F -> TermBM PresBM HydmBM Thre_V Cur_One Cur_Two C_One_Poly C_Two_Poly CurrResF Stat SerialNumber CRC",
                this::parseFResponse,
                72
        );
    }

    private AnswerValues parseFResponse(byte[] response) {
        AnswerValues answerValues = null;
//"F" - direct
        answerValues = null;
        //System.out.println("Proceed CRDG direct");
        String example = "02750\t07518\t00023\t00323\t08614\t01695\t06353\t03314\t03314\t00001\t08500012\t0x0D";

        if (response.length >= 68) {
            if (response[70] != calculateCRCforF(response)) {
                System.out.println("ERROR CRC for F");
                System.out.println("Expected CRC " + calculateCRCforF(response) + " received " + response[70]);
            }
            double termBM = 0.0;
            double presBM = 0.0;
            double hydmBM = 0.0;
            double thre_V = 0.0;
            double cur_One = 0.0;
            double cur_Two = 0.0;
            double c_One_Poly = 0.0;
            double c_Two_Poly = 0.0;
            double currResF = 0.0;
            double stat = 0.0;
            double serialNumber = 0.0;
            answerValues = new AnswerValues(11);
            byte subResponse[] = Arrays.copyOfRange(response, 1, 6);
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
                        termBM = termBM * 10 + (b - '0');
                    } else {
                        termBM = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    termBM /= 100.0; // Сдвигаем запятую на два знака влево
                    termBM = isNegative ? -termBM : termBM; // Применяем знак
                    answerValues.addValue(termBM, " °C");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " °C(ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position " + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " °C(ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 7, 12);
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
                        presBM = presBM * 10 + (b - '0');
                    } else {
                        presBM = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    presBM /= 10.0; // Сдвигаем запятую на два знака влево
                    presBM = isNegative ? -presBM : presBM; // Применяем знак
                    answerValues.addValue(presBM, " mmHg");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " mmHg(ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (press)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " mmHg(ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 13, 18);
            if (isCorrectNumberF(subResponse)) {
                boolean isNegative = false;
                boolean success = true;

                // Преобразуем байты ASCII-чисел вручную
                for (int i = 0; i < subResponse.length; i++) {
                    byte b = subResponse[i];

                    if (b == '-' && i == 0) {
                        isNegative = true;
                        continue;
                    }                                        // Проверяем, что символ – цифра
                    if (b >= '0' && b <= '9') {
                        hydmBM = hydmBM * 10 + (b - '0');
                    } else {
                        hydmBM = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    //hydmBM /= 10.0; // Сдвигаем запятую на два знака влево
                    hydmBM = isNegative ? -hydmBM : hydmBM; // Применяем знак
                    answerValues.addValue(hydmBM, " %");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " %(ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (hydmBM)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " %(ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 19, 24);  //Напряжение питания
            if (isCorrectNumberF(subResponse)) {
                boolean isNegative = false;
                boolean success = true;

                // Преобразуем байты ASCII-чисел вручную
                for (int i = 0; i < subResponse.length; i++) {
                    byte b = subResponse[i];

                    if (b == '-' && i == 0) {
                        isNegative = true;
                        continue;
                    }                                        // Проверяем, что символ – цифра
                    if (b >= '0' && b <= '9') {
                        thre_V = thre_V * 10 + (b - '0');
                    } else {
                        thre_V = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    thre_V /= 100.0; // Сдвигаем запятую на два знака влево
                    thre_V = isNegative ? -thre_V : thre_V; // Применяем знак
                    answerValues.addValue(thre_V, " V (pwr)");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " V (pwr) (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (thre_V)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " V (pwr) (ERR)");
            }


            subResponse = Arrays.copyOfRange(response, 25, 30);  //Сила тока шунт один до полинома
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
                        cur_One = cur_One * 10 + (b - '0');
                    } else {
                        cur_One = 0.0;
                        success = false;
                        break;
                    }

                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    cur_One /= 1000.0; // Сдвигаем запятую на два знака влево
                    cur_One = isNegative ? -cur_One : cur_One; // Применяем знак
                    answerValues.addValue(cur_One, " A");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " A (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (cur_One)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " A (ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 31, 36);  //Сила тока шунт два до полинома
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
                        cur_Two = cur_Two * 10 + (b - '0');
                    } else {
                        cur_Two = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    cur_Two /= 1000.0; // Сдвигаем запятую на два знака влево
                    cur_Two = isNegative ? -cur_Two : cur_Two; // Применяем знак
                    answerValues.addValue(cur_Two, " A");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " A (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (cur_Two)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " A (ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 37, 42);  //Сила тока шунт один после полинома
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
                        c_One_Poly = c_One_Poly * 10 + (b - '0');
                    } else {
                        c_One_Poly = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    c_One_Poly /= 1000.0; // Сдвигаем запятую на два знака влево
                    c_One_Poly = isNegative ? -c_One_Poly : c_One_Poly; // Применяем знак
                    answerValues.addValue(c_One_Poly, " A");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " A (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (c_One_Poly)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " A (ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 43, 48);  //Сила тока шунт два после полинома
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
                        c_Two_Poly = c_Two_Poly * 10 + (b - '0');
                    } else {
                        c_Two_Poly = 0.0;
                        success = false;
                        break;
                    }
                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    c_Two_Poly /= 1000.0; // Сдвигаем запятую на два знака влево
                    c_Two_Poly = isNegative ? -c_Two_Poly : c_Two_Poly; // Применяем знак
                    answerValues.addValue(c_Two_Poly, " A");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " A (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (c_Two_Poly)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " A (ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 49, 54);  //Сила тока выбранный результат (ардуиной)
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
                        currResF = currResF * 10 + (b - '0');
                    } else {
                        currResF = 0.0;
                        success = false;
                        break;
                    }

                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    currResF /= 1000.0; // Сдвигаем запятую на два знака влево
                    currResF = isNegative ? -currResF : currResF; // Применяем знак
                    answerValues.addValue(currResF, " A");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " A (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (currResF)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " A (ERR)");
            }

            subResponse = Arrays.copyOfRange(response, 55, 60);  //Статус
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
                        stat = stat * 10 + (b - '0');
                    } else {
                        stat = 0.0;
                        success = false;
                        break;
                    }

                }

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    //stat /= 1000.0; // Сдвигаем запятую на два знака влево
                    stat = isNegative ? - stat : stat; // Применяем знак
                    answerValues.addValue(stat, " st");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " st (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (stat)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " st (ERR)");
            }

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

                // Применяем десятичный порядок для получения числа в формате 123.45
                if (success) {
                    //stat /= 1000.0; // Сдвигаем запятую на два знака влево
                    if(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)) != null){
                        answerValues.setDirection(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)));
                    }

                    serialNumber = isNegative ? - serialNumber : serialNumber; // Применяем знак
                    answerValues.addValue(serialNumber, " SN");
                    //System.out.println(termBM);
                } else {
                    answerValues.addValue(-88.88, " SN (ERR)");
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position (serialNumber)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " SN (ERR)");
            }




            return answerValues;
        } else {
            System.out.println("Wrong answer length " + response.length);
            for (byte b : response) {
                System.out.print(b + " ");
            }
            System.out.println();
        }
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
}