package org.example.device;

import java.util.Arrays;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Setter;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;

import static org.example.utilites.MyUtilities.*;


public class DVK_4RD  implements SomeDevice  {
    private volatile boolean busy = false;
    private volatile byte [ ] lastAnswerBytes = new byte[1];
    private final SerialPort comPort;
    private StringBuilder lastAnswer = new StringBuilder();
    private StringBuilder emulatedAnswer = null;
    @Setter
    private byte [] strEndian = {13};//CR
    private int received = 0;
    private final long millisLimit = 600;
    private final long repeatWaitTime = 500;
    private final long millisPrev = System.currentTimeMillis();
    private AnswerValues answerValues = null;
    private String cmdToSend;
    private Integer TabForAnswer;
    private String devIdent = "DVK_4RD";
    private int expectedBytes = 0;

    public DVK_4RD(SerialPort port){
        this.comPort = port;
        this.enable();
    }

    public DVK_4RD(){
        this.comPort = null;
    }

    @Override
    public void setCmdToSend(String str) {
        //Получает количесвто одидаемых байт душная история
        expectedBytes = commands.getExpectedBytes(str); //ToDo распространить на сотальные девайсы
        cmdToSend = str;
    }

    @Override
    public Integer getTabForAnswer() {
        return null;
    }

    @Override
    public int getExpectedBytes(){
        return expectedBytes;
    }


    @Override
    public boolean isBusy(){
        return busy;
    }

    @Override
    public void setBusy(boolean busy){
        this.busy = busy;
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
        return true;
    }

    @Override
    public int getReceivedCounter() {
        return received;
    }

    @Override
    public void setReceivedCounter(int cnt) {
        this.received = cnt;
    }



    public void setReceived(String answer){
        lastAnswerBytes = answer.getBytes();
        this.received = lastAnswerBytes.length;
        //this.parseData();
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
    public void setLastAnswer(byte[] sb) {
        lastAnswerBytes = sb;
    }

    @Override
    public StringBuilder getEmulatedAnswer() {
        return this.emulatedAnswer;
    }

    @Override
    public void setEmulatedAnswer(StringBuilder sb) {
        this.emulatedAnswer = sb;
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
            comPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 10, 5);
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

    @Override
    public void parseData() {
        //System.out.println("DVK_4RD run parse");
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
                    System.out.println("DVK_4RD Cant create answers obj (error in answer)");

                }

            }else {
                for (int i = 0; i < lastAnswerBytes.length; i++) {
                    lastAnswer.append( (char) lastAnswerBytes[i]);
                }
                System.out.println("DVK_4RD Cant create answers obj (unknown command)");
            }
        }else{
            System.out.println("DVK_4RDDVK_4RD empty received");
        }
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
        return true;
    }

    @Override
    public boolean hasValue(){
        return true;
    }

    public AnswerValues getValues(){
        return this.answerValues;
    }

    {
        commands.addCommand(
                new SingleCommand(
                        "F", "F - Основная команда опроса", //72 байта
                        (response) -> {
                        answerValues = null;
                        String example = "02750\t07518\t00023\t00323\t08614\t01695\t06353\t03314\t03314\t00001\t08500012\t0x0D";

        if (response.length >= 68) {
            if (response[70] != calculateCRCforF(response)) {
                System.out.println("ERROR CRC for F");
                System.out.println("Expected CRC " + calculateCRCforF(response) + " received " + response[70]);
                resetAnswerValues();
                return null;

            }
            double value = 0.0;
            double serialNumber = 0.0;
            answerValues = new AnswerValues(11);
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
                    resetAnswerValues();
                    return null;
                }
            } else {
                System.out.println("Wrong isCorrectNumberF position " + Arrays.toString(subResponse));
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
                    answerValues.addValue(value, " 1");
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
                    answerValues.addValue(value, " 2");
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
                    answerValues.addValue(value, " 3");
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
                    answerValues.addValue(value, " 4");
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
                    answerValues.addValue(value, " 5");
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
                    answerValues.addValue(value, " 7");
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
                    answerValues.addValue(value, " 8");
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
                    answerValues.addValue(value, " 9");
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
                System.out.println("Wrong isCorrectNumberF position (serialNumber)" + Arrays.toString(subResponse));
                answerValues.addValue(-99.99, " SN (ERR)");
                resetAnswerValues();
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
            resetAnswerValues();
            return null;
        }
    }, 72)
            );
    }

    public void resetAnswerValues() {
        answerValues = null;
        lastAnswerBytes = null;
        lastAnswer.setLength(0);

    }
}
