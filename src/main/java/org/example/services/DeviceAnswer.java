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
}
