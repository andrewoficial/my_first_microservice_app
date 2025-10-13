package org.example.gui.mgstest.parser.answer;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.GetAllSettingsModel;
import org.example.gui.mgstest.util.CrcValidator;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class GetAllSettingsParser {
    private static final Logger log = Logger.getLogger(GetAllSettingsParser.class);

    public static GetAllSettingsModel parse(byte[] data) {
        validateDataLength(data);

        // Проверяем CRC для payload с 27 по 104 байт
        if (!CrcValidator.checkCrc(data, 27, 105, 105)) {
            throw new IllegalArgumentException("CRC validation failed for GetAllSettings");
        }

        GetAllSettingsModel settings = new GetAllSettingsModel();
        settings.setLoaded(false);

        try {
            ByteBuffer buffer = ByteBuffer.wrap(data, 27, 78); // 78 байт payload
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            log.info("Начинаем парсинг настроек. Доступно байт: " + buffer.remaining());

            short value;

            log.info("Чтение LED_Alarm_PWM...");
            value = buffer.getShort();
            log.info("LED_Alarm_PWM: " + value);
            settings.setLedAlarmPWM(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Vibro_PWM...");
            value = buffer.getShort();
            log.info("Vibro_PWM: " + value);
            settings.setVibroPWM(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Led_Alarm_Time...");
            value = buffer.getShort();
            log.info("Led_Alarm_Time: " + value);
            settings.setLedAlarmTime(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Led_AlarmSlow_Time...");
            value = buffer.getShort();
            log.info("Led_AlarmSlow_Time: " + value);
            settings.setLedAlarmSlowTime(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Alarm_BeepOff_TimeOut...");
            value = buffer.getShort();
            log.info("Alarm_BeepOff_TimeOut: " + value);
            settings.setAlarmBeepOffTimeOut(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Alarm_TimeOut...");
            value = buffer.getShort();
            log.info("Alarm_TimeOut: " + value);
            settings.setAlarmTimeOut(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение LedRedSCR_PWM...");
            value = buffer.getShort();
            log.info("LedRedSCR_PWM: " + value);
            settings.setLedRedSCRPWM(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение FreezeDeltaTemper...");
            value = buffer.getShort();
            log.info("FreezeDeltaTemper: " + value);
            settings.setFreezeDeltaTemper(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение FreezeDeltaTime...");
            value = buffer.getShort();
            log.info("FreezeDeltaTime: " + value);
            settings.setFreezeDeltaTime(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Vref_WarmUp_Time...");
            value = buffer.getShort();
            log.info("Vref_WarmUp_Time: " + value);
            settings.setVrefWarmUpTime(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4_Buffer_Term...");
            value = (short) (buffer.get() & 0xFF);
            log.info("CH4_Buffer_Term: " + value);
            settings.setCh4BufferTerm(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4_Buffer_Time...");
            value = (short) (buffer.get() & 0xFF);
            log.info("CH4_Buffer_Time: " + value);
            settings.setCh4BufferTime(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CoefH2SppmToMg (short)...");
            value = buffer.getShort();
            log.info("CoefH2SppmToMg: " + value);
            settings.setCoefH2SppmToMg(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CoefCOppmToMg (short)...");
            value = buffer.getShort();
            log.info("CoefCOppmToMg: " + value);
            settings.setCoefCOppmToMg(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение NFCTimeOutDetectSeconds...");
            value = (short) (buffer.get() & 0xFF);
            log.info("NFCTimeOutDetectSeconds: " + value);
            settings.setNfcTimeOutDetectSeconds(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение NFCTimeOutWaitMinutes...");
            value = (short) (buffer.get() & 0xFF);
            log.info("NFCTimeOutWaitMinutes: " + value);
            settings.setNfcTimeOutWaitMinutes(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение BattLow...");
            value = (short) (buffer.get() & 0xFF);
            log.info("BattLow: " + value);
            settings.setBattLow(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Log_State...");
            value = (short) (buffer.get() & 0xFF);
            log.info("Log_State: " + value);
            settings.setLogState(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Pressure_State...");
            value = (short) (buffer.get() & 0xFF);
            log.info("Pressure_State: " + value);
            settings.setPressureState(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Life_Time_Week...");//31...32
            value = (short) (buffer.get() & 0xFF);
            log.info("Life_Time_Week: " + value);
            settings.setLifeTimeWeek(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение FreezeStatusMask...");//32...36
            value = (short) (buffer.get() & 0xFF);
            value = (short) (buffer.get() & 0xFF);
            value = (short) (buffer.get() & 0xFF);
            value = (short) (buffer.get() & 0xFF);
            log.info("FreezeStatusMask: " + value);
            settings.setFreezeStatusMask(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CoefVolToLEL...");//36...38
            value = buffer.getShort();
            log.info("CoefVolToLEL: " + value);
            settings.setCoefVolToLEL(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение LogTimeOut...");//38...39
            value = (short) (buffer.get() & 0xFF);
            log.info("LogTimeOut: " + value);
            settings.setLogTimeOut(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение LogAlarmTimeOut...");//39...40
            value = (short) (buffer.get() & 0xFF);
            log.info("LogAlarmTimeOut: " + value);
            settings.setLogAlarmTimeOut(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение SensorsUnits...");//40...41
            value = (short) (buffer.get() & 0xFF);
            log.info("SensorsUnits: " + value);
            settings.setSensorsUnits(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Skip SNSRef_ADC...");//41...43
            value = buffer.getShort(); // SNSRef_ADC (0 в данных, no set in model)
            log.info("SNSRef_ADC skipped (skipped)" + value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2Chem...");//43...44
            value = (short) (buffer.get() & 0xFF);
            log.info("O2Chem: " + value);
            settings.setO2Chem(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CoefCHEMppmToMg (short)...");//44...46
            value = buffer.getShort();
            log.info("CoefCHEMppmToMg: " + value);
            settings.setCoefCHEMppmToMg(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение SensorsPrecisions...");//46...47
            value = (short) (buffer.get() & 0xFF);
            log.info("SensorsPrecisions: " + value);
            settings.setSensorsPrecisions(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение SkipSelfTest...");//47...48
            value = (short) (buffer.get() & 0xFF);
            log.info("SkipSelfTest: " + value);
            settings.setSkipSelfTest(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение SensorsAutoZero...");//48...49
            value = (short) (buffer.get() & 0xFF);
            log.info("SensorsAutoZero: " + value);
            settings.setSensorsAutoZero(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение AltScreenTime...");//49...50
            value = (short) (buffer.get() & 0xFF);
            log.info("AltScreenTime: " + value);
            settings.setAltScreenTime(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение RssiLow...");//50...52
            value = buffer.getShort();
            log.info("RssiLow: " + value);
            settings.setRssiLow(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение SnrLow...");//52...53
            value = (short) (buffer.get() & 0xFF);
            log.info("SnrLow: " + value);
            settings.setSnrLow(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение AlarmType...");//53...54
            value = (short) (buffer.get() & 0xFF);
            log.info("AlarmType: " + value);
            settings.setAlarmType(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение LostSec...");//54...56
            value = buffer.getShort();
            log.info("LostSec: " + value);
            settings.setLostSec(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение LostPackets...");//56...57
            value = (short) (buffer.get() & 0xFF);
            log.info("LostPackets: " + value);
            settings.setLostPackets(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2 sim...");//57...59
            value = buffer.getShort();
            log.info("O2 sim: " + value);
            settings.setO2Sim(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO sim...");//59...61
            value = buffer.getShort();
            log.info("CO sim: " + value);
            settings.setCoSim(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S sim...");//61...63
            value = buffer.getShort();
            log.info("H2S sim: " + value);
            settings.setH2sSim(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4 sim...");//63...65
            value = buffer.getShort();
            log.info("CH4 sim: " + value);
            settings.setCh4Sim(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение ScreenPosition...");//65...66 (O2ChemScrPos)
            value = (short) (buffer.get() & 0xFF);
            log.info("ScreenPosition: " + value);
            settings.setScreenPosition(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение O2 scalepoint...");//66...68
            value = buffer.getShort();
            log.info("O2 scalepoint: " + value);
            settings.setO2Scalepoint(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CO scalepoint...");//68...70
            value = buffer.getShort();
            log.info("CO scalepoint: " + value);
            settings.setCoScalepoint(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение H2S scalepoint...");//70...72
            value = buffer.getShort();
            log.info("H2S scalepoint: " + value);
            settings.setH2sScalepoint(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение CH4 scalepoint...");//72...74
            value = buffer.getShort();
            log.info("CH4 scalepoint: " + value);
            settings.setCh4Scalepoint(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Options...");//74...75
            value = (short) (buffer.get() & 0xFF);
            log.info("Options: " + value);
            settings.setOptions(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение WeekToScale...");//75...76
            value = (short) (buffer.get() & 0xFF);
            log.info("WeekToScale: " + value);
            settings.setWeekToScale(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение TransportAlarmOffMin...");//76...77
            value = (short) (buffer.get() & 0xFF);
            log.info("TransportAlarmOffMin: " + value);
            settings.setTransportAlarmOffMin(value);
            log.info("Осталось байт: " + buffer.remaining());

            log.info("Чтение Unfreeze...");//77...78
            value = (short) (buffer.get() & 0xFF);
            log.info("Unfreeze: " + value);
            settings.setUnfreeze(value);
            log.info("Осталось байт: " + buffer.remaining());

            settings.setLoaded(true);
            log.info("GetAllSettings parsed successfully");

        } catch (Exception e) {
            log.error("Error parsing GetAllSettings: " + e.getMessage(), e);
            throw new IllegalArgumentException("Failed to parse GetAllSettings: " + e.getMessage(), e);
        }

        return settings;
    }

    private static void validateDataLength(byte[] data) {
        if (data.length < 108) { // 27 + 78 + 4 = 109, но индексы с 0
            throw new IllegalArgumentException("Data too short, expected at least 108 bytes, got " + data.length);
        }
    }
}