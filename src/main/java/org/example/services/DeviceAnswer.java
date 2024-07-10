package org.example.services;

import lombok.Getter;
import lombok.Setter;
import org.example.device.SomeDevice;

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
        return answerReceivedValues.getCounter();
    }
    @Override
    public String toString(){
        //String className = deviceType.getClass().toString().replace("class org.example.device.", "");
        DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedString = answerReceivedTime.format(CUSTOM_FORMATTER);
        return formattedString + "\t" + "className" + "\t" + answerReceivedString + "\n";
    }
}
