package org.example.gui.mgstest.gui.names;

import java.util.ArrayList;

public class SettingNames {
    private final ArrayList<String> settingNames;

    public SettingNames() {
        settingNames = new ArrayList<>();
        
        // Добавляем все имена настроек из списка
        settingNames.add("LED_Alarm_PWM");
        settingNames.add("Vibro_PWM");
        settingNames.add("Led_Alarm_Time");
        settingNames.add("Led_AlarmSlow_Time");
        settingNames.add("Alarm_BeepOff_TimeOut");
        settingNames.add("Alarm_TimeOut");
        settingNames.add("LedRedSCR_PWM");
        settingNames.add("FreezeDeltaTemper");
        settingNames.add("FreezeDeltaTime");
        settingNames.add("Vref_WarmUp_Time");
        settingNames.add("BattLow");
        settingNames.add("Log_State");
        settingNames.add("Pressure_State");
        settingNames.add("Life_Time_Week");
        settingNames.add("FreezeStatusMask");
        settingNames.add("FreezeLimit,млн-1");
        settingNames.add("CoefVolToLEL");
        settingNames.add("LogTimeOut");
        settingNames.add("LogAlarmTimeOut");
        settingNames.add("CH4_Buffer_Term");
        settingNames.add("CH4_Buffer_Time");
        settingNames.add("CoefH2SppmToMg");
        settingNames.add("CoefCOppmToMg");
        settingNames.add("CoefCHEMppmToMg");
        settingNames.add("NFCTimeOutDetectSeconds");
        settingNames.add("NFCTimeOutWaitMinutes");
        settingNames.add("SensorsUnits");
        settingNames.add("O2Chem");
        settingNames.add("SensorsPrecisions");
        settingNames.add("SkipSelfTest");
        settingNames.add("SensorsAutoZero");
        settingNames.add("AltScreenTime, s");
        settingNames.add("RssiLow");
        settingNames.add("SnrLow");
        settingNames.add("AlarmType");
        settingNames.add("LostSec");
        settingNames.add("LostPackets");
        settingNames.add("O2 sim");
        settingNames.add("CO sim");
        settingNames.add("H2S sim");
        settingNames.add("CH4 sim");
        settingNames.add("ScreenPosition");
        settingNames.add("O2 scalepoint");
        settingNames.add("CO scalepoint");
        settingNames.add("H2S scalepoint");
        settingNames.add("CH4 scalepoint");
        settingNames.add("Options");
        settingNames.add("WeekToScale");
        settingNames.add("TransportAlarmOffMin");
        settingNames.add("Unfreeze");
    }

    public ArrayList<String> getSettingNames() {
        return settingNames;
    }
}