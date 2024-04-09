package org.example.services;

import lombok.Getter;
import lombok.Setter;
import org.example.device.SomeDevice;

import java.time.LocalDateTime;

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
    private String answerReceivedValue;
    @Setter @Getter
    private Integer tabNumber;

    public DeviceAnswer(LocalDateTime requestSendTime,
                        String requestSendString,
                        Integer tabNumber) {

        this.requestSendTime = requestSendTime;
        this.requestSendString = requestSendString;
        this.tabNumber = tabNumber;
    }

    @Override
    public String toString(){
        return deviceType + "\t" + answerReceivedTime + "\t" + answerReceivedString + "\n";
    }
}
