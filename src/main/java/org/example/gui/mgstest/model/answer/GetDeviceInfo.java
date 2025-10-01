package org.example.gui.mgstest.model.answer;

import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
public class GetDeviceInfo {
    private String cpuId;
    private int serialNumber;
    private int swMin;
    private int swMaj;
    private int hwMin;
    private int hwMaj;
    private int time;
    private boolean beepEnabled;
    private boolean vibroEnabled;
    private boolean alarmEnabled;
    private boolean daylightSaving;
    private int logTimeout;
    private byte replaceCount;
    private byte enabledHW;
    private byte verControl;
    private String softwareVer;
    private String hardwareVer;
    private boolean loaded;

    // Вспомогательные методы для форматированного вывода
    public Date getTimeAsDate() {
        if (time > 0 && time < 0xFFFFFFFFL) {
            return new Date(time * 1000L);
        }
        return null;
    }

    @Override
    public String toString() {
        return "DeviceInfo{" +
                "cpuId='" + cpuId + '\'' +
                ", serialNumber=" + serialNumber +
                ", softwareVer='" + softwareVer + '\'' +
                ", hardwareVer='" + hardwareVer + '\'' +
                ", time=" + time + " (" + getTimeAsDate() + ")" +
                ", beepEnabled=" + beepEnabled +
                ", vibroEnabled=" + vibroEnabled +
                ", alarmEnabled=" + alarmEnabled +
                ", loaded=" + loaded +
                '}';
    }
}