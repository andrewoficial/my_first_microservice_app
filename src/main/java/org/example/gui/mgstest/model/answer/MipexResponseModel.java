package org.example.gui.mgstest.model.answer;

public class MipexResponseModel {
    public long time;
    public String text;

    public MipexResponseModel(long time, String text) {
        this.time = time;
        this.text = text;
    }

    @Override
    public String toString() {
        return "MipexResponse{time=" + time + ", text='" + text + "'}";
    }
}