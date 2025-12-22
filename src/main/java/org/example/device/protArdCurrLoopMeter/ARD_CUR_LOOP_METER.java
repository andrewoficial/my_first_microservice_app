package org.example.device.protArdCurrLoopMeter;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.example.device.DeviceCommandListClass;
import org.example.device.SomeDevice;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.apache.log4j.Logger;
import org.example.services.comPort.*;

public class ARD_CUR_LOOP_METER implements SomeDevice {


    private static final Logger log = Logger.getLogger(ARD_CUR_LOOP_METER.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;

    private final DeviceCommandListClass commands;
    private final ArdCurrLoopMeterCommandRegistry commandRegistry;

    private volatile byte [] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "ARD_CUR_LOOP_METER";

    public ARD_CUR_LOOP_METER(){
        log.info("Создан объект протокола ARD_CUR_LOOP_METER эмуляция");
        this.comPort = null;
        this.commandRegistry = new ArdCurrLoopMeterCommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public ARD_CUR_LOOP_METER(SerialPort port){
        log.info("Создан объект протокола ARD_CUR_LOOP_METER");
        this.comPort = port;
        this.commandRegistry = new ArdCurrLoopMeterCommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(DataBitsList.B8);
        comParameters.setParity(ParityList.P_EV);
        comParameters.setBaudRate(BaudRatesList.B9600);
        comParameters.setStopBits(StopBitsList.S1);
        comParameters.setStringEndian(StringEndianList.CR);
        comParameters.setMillisLimit(1200);
        comParameters.setRepeatWaitTime(300);
        this.enable();
    }


    @Override
    public DeviceCommandListClass getCommandListClass() {
        return this.commands;
    }

    @Override
    public void setCmdToSend(String str) {
        if(str == null || str.isEmpty()){
            expectedBytes = 500;
            cmdToSend = null;
        }else{
            //Получает количесвто одидаемых байт
            expectedBytes = commands.getExpectedBytes(str); //ToDo распространить на сотальные девайсы
            cmdToSend = str;
        }
    }

    @Override
    public int getExpectedBytes(){
        return expectedBytes;
    }

    @Override
    public byte[] getStrEndian() {
        return this.comParameters.getStringEndian().getBytes();
    }

    @Override
    public SerialPort getComPort() {
        return this.comPort;
    }

    @Override
    public boolean isKnownCommand() {
        return  commands.isKnownCommand(cmdToSend);
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
    }

    @Override
    public long getMillisLimit() {
        return comParameters.getMillisLimit();
    }

    @Override
    public int getMillisReadLimit() {
        return comParameters.getMillisReadLimit();
    }

    @Override
    public int getMillisWriteLimit() {
        return comParameters.getMillisWriteLimit();
    }

    @Override
    public long getRepeatWaitTime() {
        return comParameters.getRepeatWaitTime();
    }

    @Override
    public void setLastAnswer(byte[] sb) {
        lastAnswerBytes = sb;
    }

    public boolean enable() {
        return true;
    }




    @Override
    public void parseData() {

        if (lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            lastAnswer.setLength(0);
            if (commands.isKnownCommand(cmdToSend)) {
                answerValues = commands.getCommand(cmdToSend).getResult(lastAnswerBytes); //Получение значений в ответе
                if(answerValues != null){
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(String.valueOf(answerValues.getValues()[i]).replace(".", ","));
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                }else{
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.info("ARD_CUR_LOOP_METER Cant create answers obj (error in answer)");
                }
            }else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                log.info("DVK_4RD Cant create answers obj (unknown command)");
            }
        }else{
            log.info("DVK_4RD empty received");
        }
        //parseMMESU(lastAnswerBytes);
    }

    public String getAnswer(){
        if(hasAnswer()) {
            received = 0;
            lastAnswerBytes = null;
            return lastAnswer.toString();
        }else {
            return null;
        }
    }

    public boolean hasAnswer(){
        return lastAnswerBytes != null && lastAnswerBytes.length > 0;
    }

    @Override
    public boolean hasValue(){
        return answerValues != null;
    }

    public AnswerValues getValues(){
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



}
