package org.example.gui.mgstest.transport;

public class MipexResponse {
    public long time;
    public String text;

    public MipexResponse(long time, String text) {
        this.time = time;
        this.text = text;
    }

    @Override
    public String toString() {
        return "MipexResponse{time=" + time + ", text='" + text + "'}";
    }
}