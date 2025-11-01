package org.example.services;

import lombok.Getter;
import lombok.Setter;
import org.example.device.SomeDevice;
import org.example.utilites.MyUtilities;
import org.example.utilites.properties.MyProperties;

import java.security.PrivateKey;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeviceAnswer {
    private MyProperties prop = MyProperties.getInstance();

    @Setter @Getter
    private SomeDevice deviceType;
    @Setter @Getter
    private LocalDateTime requestSendTime;
    @Setter @Getter
    private LocalDateTime answerReceivedTime;
    @Setter @Getter
    private String requestSendString;
    @Setter @Getter
    private String answerReceivedString;

    @Setter @Getter
    private byte[] answerReceivedBytes;
    @Setter @Getter
    private byte[] answerSendBytes;

    @Setter @Getter
    private String answerReceivedStringCSV;

    @Setter @Getter
    private AnswerValues answerReceivedValues;
    @Setter @Getter
    private Integer clientId;
    public DeviceAnswer(LocalDateTime requestSendTime,
                        String requestSendString,
                        Integer clientId,
                        MyProperties prop) {

        this.requestSendTime = requestSendTime;
        this.requestSendString = requestSendString;
        this.clientId = clientId;
        this.prop = prop;
    }

    public DeviceAnswer(LocalDateTime requestSendTime,
                        String requestSendString,
                        Integer clientId) {

        this.requestSendTime = requestSendTime;
        this.requestSendString = requestSendString;
        this.clientId = clientId;
    }

    public void changeTabNum(Integer num){
        this.clientId = num;
    }
    public Integer getFieldCount (){
        if(answerReceivedValues != null){
            return answerReceivedValues.getCounter(); //ToDo recheck it
        }
        return 0;
    }
    @Override
    public String toString(){
        String formattedString = answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER);
        return formattedString + "\t" + "className" + "\t" + answerReceivedString + "\n";

    }

    public String toStringCSV(){
        StringBuilder formattedString = new StringBuilder();
        if(answerReceivedString == null){
            return "NULL STRING FOR CSV";
        }
        if(prop.isCsvLogOutputASCII()){
            formattedString.append(requestSendTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getCsvLogSeparator());
            formattedString.append(requestSendString);
            formattedString.append("\n");
        }
        if(prop.isCsvLogInputASCII()){
            formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getCsvLogSeparator());
            formattedString.append(answerReceivedString.trim());
            formattedString.append("\n");
        }
        if(prop.isCsvLogInputParsed()){
            formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getCsvLogSeparator());
            if(answerReceivedValues != null) {
                for (int i = 0; i < answerReceivedValues.getCounter(); i++) {
                    if (answerReceivedValues.getValues() != null) {
                        formattedString.append(MyUtilities.changeToNeedSeparator(answerReceivedValues.getValues()[i]));
                        formattedString.append(prop.getCsvLogSeparator());
                        formattedString.append(answerReceivedValues.getUnits()[i]);
                        formattedString.append(prop.getCsvLogSeparator());
                    } else {
                        formattedString.append("nullValue");
                        formattedString.append(prop.getCsvLogSeparator());
                        formattedString.append("nullUnits");
                        formattedString.append(prop.getCsvLogSeparator());
                    }
                }
            }
            formattedString.append("\n");
        }
        //Плохо помню зачем обрезать два символа
        if(formattedString.toString().length() < 3){
            return formattedString.toString();
        }else {
            return formattedString.toString().substring(0,formattedString.toString().length()-2);
        }
    }

    public String toStringDBG(){
        StringBuilder formattedString = new StringBuilder();
        if(answerReceivedString == null){
            answerReceivedString = "NULL STRING";
        }
        if(prop.isDbgLogOutputASCII()){
            formattedString.append(requestSendTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            formattedString.append(requestSendString);
            formattedString.append("\n");
        }

        if(prop.isDbgLogOutputHEX()){
            formattedString.append(requestSendTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            if(answerSendBytes != null && answerSendBytes.length > 0) {
                formattedString.append(arrayToHexArray(answerSendBytes));
            }else{
                formattedString.append(stringToHexArray(requestSendString));
            }
            formattedString.append("\n");
        }
        if(prop.isDbgLogInputHEX()){
            formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            if(answerReceivedBytes != null && answerReceivedBytes.length > 0) {
                formattedString.append(arrayToHexArray(answerReceivedBytes));
            }else{
                formattedString.append(stringToHexArray(answerReceivedString));
            }
            formattedString.append("\n");
        }
        if(prop.isDbgLogInputASCII()){
            formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            formattedString.append(answerReceivedString.trim());
            formattedString.append("\n");
        }
        if(prop.isDbgLogInputParsed()){
            formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            if(answerReceivedValues != null) {
                for (int i = 0; i < answerReceivedValues.getCounter(); i++) {
                    if (answerReceivedValues.getValues() != null) {
                        formattedString.append(MyUtilities.changeToNeedSeparator(answerReceivedValues.getValues()[i]));
                        formattedString.append(prop.getDbgLogSeparator());
                        formattedString.append(answerReceivedValues.getUnits()[i]);
                        formattedString.append(prop.getDbgLogSeparator());
                    } else {
                        formattedString.append("nullValue");
                        formattedString.append(prop.getDbgLogSeparator());
                        formattedString.append("nullUnits");
                        formattedString.append(prop.getDbgLogSeparator());
                    }
                }
            }else{
                formattedString.append("nullValuesReceived");
                formattedString.append(prop.getDbgLogSeparator());
                formattedString.append("nullValuesReceived");
                formattedString.append(prop.getDbgLogSeparator());
            }
            formattedString.append("\n");
        }
        //Плохо помню зачем обрезать два символа
        if(formattedString.toString().length() < 3){
            return formattedString.toString();
        }else {
            return formattedString.toString().substring(0,formattedString.toString().length()-2);
        }

    }

    public String stringToHexArray(String input) {
        if(input == null){
            return new StringBuilder("{}").toString();
        }
        StringBuilder hexArray = new StringBuilder("{");
        for (int i = 0; i < input.length(); i++) {
            hexArray.append((int) input.charAt(i));
            if (i < input.length() - 1) {
                hexArray.append(" ");
            }
        }
        hexArray.append("}");
        return hexArray.toString();
    }

    public String arrayToHexArray(byte[] input) {
        if(input == null){
            return new StringBuilder("{}").toString();
        }
        StringBuilder hexArray = new StringBuilder("{");
        for (int i = 0; i < input.length; i++) {
            hexArray.append(String.format("%02X", input[i] & 0xFF));
            if (i < input.length - 1) {
                hexArray.append(" ");
            }
            hexArray.append(" ");
        }
        hexArray.append("} ");
        return hexArray.toString();
    }
}
