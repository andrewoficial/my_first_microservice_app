package org.example.device;

import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.nio.charset.Charset;

import static org.example.utilites.MyUtilities.*;

public class ARD_FEE_BRD_METER implements SomeDevice {
    private volatile boolean bisy = false;
    private static final Logger log = Logger.getLogger(IGM_10.class);
    private final SerialPort comPort;
    private byte[] lastAnswerBytes;
    private final StringBuilder lastAnswer = new StringBuilder();
    private StringBuilder emulatedAnswer = new StringBuilder();
    private final boolean knownCommand = false;
    private volatile boolean hasAnswer = false;
    private volatile boolean hasValue = false;
    @Setter
    private byte[] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 450;
    private final long repeatWaitTime = 100;
    private final long millisPrev = System.currentTimeMillis();
    private final Charset charset = Charset.forName("Cp1251");
    private static AnswerValues answerValues = new AnswerValues(0);
    private String cmdToSend;
    private Integer TabForAnswer;
    private String devIdent = "ARD_FEE_BRD_METER";

    public ARD_FEE_BRD_METER(SerialPort port) {
        log.info("Создан объект протокола ARD_FEE_BRD_METER");
        this.comPort = port;

        this.enable();
    }

    public ARD_FEE_BRD_METER() {
        System.out.println("Создан объект протокола ARD_FEE_BRD_METER эмуляция");
        this.comPort = null;
    }

    @Override
    public void setCmdToSend(String str) {
        cmdToSend = str;
    }

    @Override
    public Integer getTabForAnswer() {
        return null;
    }


    public boolean isBusy() {
        return bisy;
    }

    @Override
    public void setBusy(boolean busy) {
        this.bisy = busy;
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
        received = cnt;
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
    public long getRepeatWaitTime() {
        return repeatWaitTime;
    }

    @Override
    public void setLastAnswer(byte[] ans) {
//        for (byte an : ans) {
//            System.out.print(an);
//        }
        lastAnswerBytes = ans;
    }

    @Override
    public StringBuilder getEmulatedAnswer() {
        return emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb) {
        emulatedAnswer = sb;
    }

    @Override
    public void setHasAnswer(boolean hasAnswer) {

    }

    private CommandListClass commands = new CommandListClass();

    @Override
    public CommandListClass getCommandListClass() {
        return this.commands;
    }

    public boolean enable() {
        if(! comPort.isOpen()){
            comPort.openPort();
            comPort.flushDataListener();
            comPort.removeDataListener();
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 15, 10);
            if(comPort.isOpen()){
                log.info("Порт открыт, задержки выставлены");
                return true;
            }else {
                throw new RuntimeException("Cant open COM-Port");
            }

        }else{
            log.info("Порт был открыт ранее");
            return true;
        }
    }

    public String getForSend() {
        return cmdToSend;
    }

    public void setReceived(String answer) {
        this.received = answer.length();
        this.parseData();
    }

    @Override
    public void parseData() {
        //System.out.println("ARD_FEE_BRD_METER run parse");
        if (lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                
                String str = new String(lastAnswerBytes, charset);
                
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes);
                hasAnswer = true;
                if (answerValues == null) {
                    System.out.println("ARD_FEE_BRD_METER done known command. Result NULL.");
                    return;
                }
                for (int i = 0; i < answerValues.getValues().length; i++) {
                    lastAnswer.append(String.valueOf(answerValues.getValues()[i]).replace(".", ","));
                    lastAnswer.append(" ");
                    lastAnswer.append(answerValues.getUnits()[i]);
                    lastAnswer.append("  ");
                }
                System.out.println("ident " + devIdent);
                //System.out.println("ARD_FEE_BRD_METER done correct...[" + lastAnswer.toString() + "]...");
            } else {
                hasAnswer = true;

                parseMMESU(lastAnswerBytes);
                for (int i = 0; i < answerValues.getValues().length; i++) {
                    lastAnswer.append(String.valueOf(answerValues.getValues()[i]).replace(".", ","));
                    lastAnswer.append(" ");
                    lastAnswer.append(answerValues.getUnits()[i]);
                    lastAnswer.append("  ");
                }
            }


        } else {
            System.out.println("ARD_FEE_BRD_METER empty received");
        }
    }



    public String getAnswer() {

        hasAnswer = false;
        return lastAnswer.toString();
    }

    public boolean hasAnswer() {
        //System.out.println("return flag " + hasAnswer);
        return hasAnswer;
    }

    public boolean hasValue() {
        return hasValue;
    }

    private void saveAnswerValue() {
        answerValues = new AnswerValues(1);

        double ans = Double.parseDouble(lastAnswer.toString());
        answerValues.addValue(ans, "deg");

    }

    public AnswerValues getValues() {
        return this.answerValues;
    }

    private void parseMMESU(byte[] response) {
// Преобразование byte[] в строку
        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            sb.append((char) b);
        }

// Разделение строки по символу табуляции
        String[] parts = sb.toString().split("\t");

// Проверка количества элементов, чтобы избежать ошибок при доступе
        if (parts.length >= 13) { // Убедимся, что данные пришли полностью
            answerValues = new AnswerValues(13);
            try {
                // Преобразование и обработка значений
                double vltToAmperN0 = Double.parseDouble(parts[0]);
                double vltToAmperN1 = Double.parseDouble(parts[2]);
                double vltConsumer = Double.parseDouble(parts[4]);
                double cur0 = Double.parseDouble(parts[6]);
                double cur1 = Double.parseDouble(parts[8]);
                String gainStat = parts[10]; // `gainStat` - строка или другой тип данных?
                double cur0Corr = Double.parseDouble(parts[11]);
                double cur1Corr = Double.parseDouble(parts[13]);
                double termBMF = Double.parseDouble(parts[15]);
                double presBMF = Double.parseDouble(parts[17]);
                double hydmBM = Double.parseDouble(parts[19]);
                double currRes = Double.parseDouble(parts[21]);

                double gainStatus = - 1.0;
                if(gainStat.equals("OFF")) gainStatus = 0.0;
                if(gainStat.equals("ON")) gainStatus = 1.0;

                // Добавление значений в `answerValues` (или другая обработка)
                answerValues.addValue(vltToAmperN0, " V");
                answerValues.addValue(vltToAmperN1, " V");
                answerValues.addValue(vltConsumer, " V");
                answerValues.addValue(cur0, " mA");
                answerValues.addValue(cur1, " mA");
                answerValues.addValue(gainStatus, "");
                answerValues.addValue(cur0Corr, " mA");
                answerValues.addValue(cur1Corr, " mA");
                answerValues.addValue(termBMF, " °C");
                answerValues.addValue(presBMF, " mm Hg");
                answerValues.addValue(hydmBM, " %");
                answerValues.addValue(currRes, " mA");

                System.out.println("Data parsed successfully!");

            } catch (NumberFormatException e) {
                System.out.println("Error parsing number: " + e.getMessage());
                answerValues.addValue(-88.88, "ERR");
            }
        } else {
            System.out.println("Incomplete response data");
            answerValues.addValue(-99.99, "ERR");
        }

    }

    {
        commands.addCommand(
                new SingleCommand(
                        "F", "F -> TermBM PresBM HydmBM Thre_V Cur_One Cur_Two C_One_Poly C_Two_Poly CurrResF Stat SerialNumber CRC",
                        (response) -> { //"F" - direct
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
                                        TabForAnswer = AnswerStorage.getTabByIdent(String.valueOf(serialNumber));
                                        devIdent = String.valueOf(serialNumber);
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
                        }, 72)
        );

    }

}
