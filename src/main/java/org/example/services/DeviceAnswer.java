package org.example.services;

import lombok.Getter;
import lombok.Setter;
import org.example.device.SomeDevice;
import org.example.utilites.MyUtilities;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DeviceAnswer {
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
            return answerReceivedValues.getCounter() + 1; //ToDo recheck it
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
        formattedString.append(answerReceivedTime.format(MyUtilities.CUSTOM_FORMATTER_CSV));
        formattedString.append(MyUtilities.separator);
        for (int i = 0; i < answerReceivedValues.getCounter(); i++) {
            if(answerReceivedValues.getValues() != null){
                formattedString.append(MyUtilities.changeToNeedSeparator(answerReceivedValues.getValues()[i]));
                formattedString.append(MyUtilities.separator);
                formattedString.append(answerReceivedValues.getUnits()[i]);
                formattedString.append(MyUtilities.separator);
            }else{
                formattedString.append("nullValue");
                formattedString.append(MyUtilities.separator);
                formattedString.append("nullUnits");
                formattedString.append(MyUtilities.separator);
            }
        }
        formattedString.append("\n");
        return formattedString.toString();
    }
}
