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
    private String answerReceivedStringCSV;

    @Setter @Getter
    private AnswerValues answerReceivedValues;
    @Setter @Getter
    private Integer tabNumber;

    public DeviceAnswer(LocalDateTime requestSendTime,
                        String requestSendString,
                        Integer tabNumber) {

        this.requestSendTime = requestSendTime;
        this.requestSendString = requestSendString;
        this.tabNumber = tabNumber;
    }

    public void changeTabNum(Integer num){
        this.tabNumber = num;
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
            for (int i = 0; i < answerReceivedValues.getCounter(); i++) {
                if(answerReceivedValues.getValues() != null){
                    formattedString.append(MyUtilities.changeToNeedSeparator(answerReceivedValues.getValues()[i]));
                    formattedString.append(prop.getCsvLogSeparator());
                    formattedString.append(answerReceivedValues.getUnits()[i]);
                    formattedString.append(prop.getCsvLogSeparator());
                }else{
                    formattedString.append("nullValue");
                    formattedString.append(prop.getCsvLogSeparator());
                    formattedString.append("nullUnits");
                    formattedString.append(prop.getCsvLogSeparator());
                }
            }
            formattedString.append("\n");
        }
        return formattedString.toString();
    }

    public String toStringDBG(){
        StringBuilder formattedString = new StringBuilder();
        if(prop.isDbgLogOutputASCII()){
            formattedString.append(requestSendTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            formattedString.append(requestSendString);
            formattedString.append("\n");
        }

        if(prop.isDbgLogOutputHEX()){
            formattedString.append(requestSendTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            formattedString.append(stringToHexArray(requestSendString));
            formattedString.append("\n");
        }
        if(prop.isDbgLogInputHEX()){
            formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
            formattedString.append(prop.getDbgLogSeparator());
            formattedString.append(stringToHexArray(answerReceivedString));
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
            for (int i = 0; i < answerReceivedValues.getCounter(); i++) {
                if(answerReceivedValues.getValues() != null){
                    formattedString.append(MyUtilities.changeToNeedSeparator(answerReceivedValues.getValues()[i]));
                    formattedString.append(prop.getDbgLogSeparator());
                    formattedString.append(answerReceivedValues.getUnits()[i]);
                    formattedString.append(prop.getDbgLogSeparator());
                }else{
                    formattedString.append("nullValue");
                    formattedString.append(prop.getDbgLogSeparator());
                    formattedString.append("nullUnits");
                    formattedString.append(prop.getDbgLogSeparator());
                }
            }
            formattedString.append("\n");
        }
        return formattedString.toString();
    }

    public String stringToHexArray(String input) {
        if(input == null){
            return new StringBuilder("{}").toString();
        }
        StringBuilder hexArray = new StringBuilder("{");
        for (int i = 0; i < input.length(); i++) {
            hexArray.append((int) input.charAt(i));
            if (i < input.length() - 1) {
                hexArray.append(", ");
            }
        }
        hexArray.append("}");
        return hexArray.toString();
    }
}
