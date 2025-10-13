package org.example.gui.mgstest.model.answer;

import lombok.Data;

@Data
public class GetAllSettingsModel {
    private boolean loaded = false;
    
    // Первые 20 параметров (предположительно short)
    private short ledAlarmPWM;
    private short vibroPWM;
    private short ledAlarmTime;
    private short ledAlarmSlowTime;
    private short alarmBeepOffTimeOut;
    private short alarmTimeOut;
    private short ledRedSCRPWM;
    private short freezeDeltaTemper;
    private short freezeDeltaTime;
    private short vrefWarmUpTime;
    private short battLow;
    private short logState;
    private short pressureState;
    private short lifeTimeWeek;
    private short freezeStatusMask;
    private short freezeLimit;
    private short coefVolToLEL;
    private short logTimeOut;
    private short logAlarmTimeOut;
    private short ch4BufferTerm;
    
    // Следующие параметры
    private short ch4BufferTime;
    private float coefH2SppmToMg;
    private float coefCOppmToMg;
    private float coefCHEMppmToMg;
    private short nfcTimeOutDetectSeconds;
    private short nfcTimeOutWaitMinutes;
    private short sensorsUnits;
    private short o2Chem;
    private short sensorsPrecisions;
    private short skipSelfTest;
    private short sensorsAutoZero;
    private short altScreenTime;
    private short rssiLow;
    private short snrLow;
    private short alarmType;
    private short lostSec;
    private short lostPackets;
    private short o2Sim;
    private short coSim;
    private short h2sSim;
    private short ch4Sim;
    private short screenPosition;
    private short o2Scalepoint;
    private short coScalepoint;
    private short h2sScalepoint;
    private short ch4Scalepoint;
    private short options;
    private short weekToScale;
    private short transportAlarmOffMin;
    private short unfreeze;
}