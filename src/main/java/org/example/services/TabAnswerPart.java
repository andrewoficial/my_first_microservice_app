package org.example.services;

import com.fazecast.jSerialComm.SerialPort;

public class TabAnswerPart {
    private String answerPart = "";
    private  Integer position = 0;

    public TabAnswerPart(String answerPart, Integer position) {
        this.answerPart = answerPart;
        this.position = position;
    }

    public String getAnswerPart() {
        return answerPart;
    }

    public void setAnswerPart(String answerPart) {
        this.answerPart = answerPart;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }
}
