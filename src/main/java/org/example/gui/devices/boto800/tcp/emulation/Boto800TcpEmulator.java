package org.example.gui.devices.boto800.tcp.emulation;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Модель термокамеры BOTO-800 для TCP-эмулятора (сниффер протокола).
 *
 * <p><b>Идентификация модели.</b> Перехваченный по Ethernet обмен — это Modbus TCP
 * (MBAP + тот же PDU, что в RTU, но без CRC), и карта регистров совпадает с
 * протоколом {@link org.example.device.protBoto.BotoProtocol#BOTO_800_MODBUS}
 * камеры <b>B-TH-800 F</b> (FW {@code v4.2.4}, программа {@code UApp_boxed.exe}):
 * {@code REG_TEMP=10, REG_SET_TEMP=60, REG_SET_MOD=63, SCALE=10}.
 *
 * <p><b>Не путать с China Modbus</b> — это протокол другой камеры,
 * {@link org.example.device.protBoto.BotoProtocol#CHINA_MODBUS_120}
 * (B-TH-120 E, FW {@code V3.4.2}): {@code 12/100/105, SCALE=100}, Ethernet нет.
 * Разграничение: {@code boto_cameras.md}, карта: {@code boto_800_register_map.md}:
 * блок реального времени 10..49, уставки 60/61/63, часы 92..97 и 151..156,
 * служебные блоки 0x1FAC / 0x22C4 / 0x2304 / 0x157E.
 *
 * <p><b>Зачем «всеядность».</b> Модель намеренно отвечает на любой адрес: любой
 * прочитанный адрес возвращает значение, а любой записанный попадает в разрежённое
 * зеркало {@link #writtenRegisters()}. Это позволяет брутфорсить карту регистров
 * настоящей камеры через эмулятор и тут же видеть, что прибор реально пишет.
 *
 * <p>Служебные блоки (0x1FAC, 0x22C4, 0x2304, 0x157E) в штатном режиме отдают 0 —
 * иначе штатная программа получает абсурдные пределы и ломает GUI (см. документ).
 *
 * <p>Все методы потокобезопасны: тик симуляции идёт из таймера Swing, регистры пишет
 * TCP-поток обработки кадров.
 */
public class Boto800TcpEmulator {

    private static final double EPS = 0.02;

    /** Масштаб BOTO 800: значение = °C(%) × 10. */
    public static final int SCALE = 10;

    // ─── блок реального времени D0010…D0059 ─────────────────────────────────
    // Источник: официальный документ «680通信协议V1.0», лист «基本运行信息».

    /** 10 (D0010) — 温度测试值: PV температуры, ×10. −300 = датчик отключён/вне диапазона. */
    public static final int REG_TEMP_PV = 10;
    /** 11 (D0011) — 温度设定值 NSP: текущая уставка температуры, ×10. */
    public static final int REG_TEMP_SP = 11;
    /** 12 (D0012) — 温度目标设定值 TSP: целевая уставка температуры, ×10. */
    public static final int REG_TEMP_TSP = 12;
    /** 13 (D0013) — 温度出力: выход регулятора температуры, ×10. */
    public static final int REG_TEMP_MV = 13;
    /** 14 (D0014) — 湿度测试值: PV влажности (канал 2), ×10. −300 = датчик отключён. */
    public static final int REG_HUMI_PV = 14;
    /** 15 (D0015) — 湿度设定值 NSP: текущая уставка влажности, ×10. */
    public static final int REG_HUMI_SP = 15;
    /** 16 (D0016) — 湿度目标设定值 TSP: целевая уставка влажности, ×10. */
    public static final int REG_HUMI_TSP = 16;
    /** 17 (D0017) — 湿度出力: выход регулятора влажности, ×10. */
    public static final int REG_HUMI_MV = 17;
    /** 18 (D0018) — 湿球温度测试值: PV влажножарной температуры, ×10. */
    public static final int REG_WETBULB_PV = 18;
    /** 19 (D0019) — 湿球温度设定值 NSP: уставка влажножарной температуры, ×10. */
    public static final int REG_WETBULB_SP = 19;
    /** 20 (D0020) — 湿球温度设定值 TSP: целевая влажножарная температура, ×10. */
    public static final int REG_WETBULB_TSP = 20;
    /** 30 (D0030) — 运行模式: 0 = 程式 (программа), 1 = 定值 (фикс. значение). */
    public static final int REG_OP_MODE = 30;
    /** 31 (D0031) — 运行状态: 0 = 停止 (стоп), 1 = 运行 (работа), 2 = 暂停 (пауза). */
    public static final int REG_RUN = 31;
    /** 32/33/34 (D0032..D0034) — накопленное время работы: часы / минуты / секунды. */
    public static final int REG_RUN_H = 32;
    public static final int REG_RUN_M = 33;
    public static final int REG_RUN_S = 34;
    /** 35 (D0035) — 运行程式号: номер исполняемой программы. */
    public static final int REG_RUN_PROGRAM_NO = 35;
    /** 36 (D0036) — 运行段号: номер исполняемого сегмента. */
    public static final int REG_RUN_SEG_NO = 36;
    /** 37 (D0037) — 运行PID组: используемая группа ПИД. */
    public static final int REG_PID_GROUP = 37;
    /** 38 (D0038) — 设定温度范围低: нижняя граница ввода температуры, знаковый ×10. */
    public static final int REG_TEMP_MIN = 38;
    /** 39 (D0039) — 设定温度范围高: верхняя граница ввода температуры, ×10. */
    public static final int REG_TEMP_MAX = 39;
    /** 40 (D0040) — 设定湿度范围低: нижняя граница ввода влажности, ×10. */
    public static final int REG_HUMI_MIN = 40;
    /** 41 (D0041) — 设定湿度范围高: верхняя граница ввода влажности, ×10. */
    public static final int REG_HUMI_MAX = 41;
    /** 42 (D0042) — DI状态: биты 0..15 = DI1..DI16. */
    public static final int REG_DI_1_16 = 42;
    /** 43 (D0043) — 温湿度/单温标志: 0 = 温湿度 (T+RH), 1 = 单温度 (только T). */
    public static final int REG_TH_MODE = 43;
    /** 44 (D0044) — 程式标志: 0 = программа не включена, 1 = включена. */
    public static final int REG_PROGRAM_FLAG = 44;
    /** 45 (D0045) — DI状态: биты 0..15 = DI17..DI32. */
    public static final int REG_DI_17_32 = 45;
    /** 46 (D0046) — 照明按键: подсветка, 0 = OFF / 1 = ON, R/W. */
    public static final int REG_LIGHT = 46;
    /** 47 (D0047) — 按键1, R/W. */
    public static final int REG_BUTTON_1 = 47;
    /** 48 (D0048) — 按键2, R/W. */
    public static final int REG_BUTTON_2 = 48;
    /** 49 (D0049) — 监控通道数量: число каналов мониторинга, 0..10, R/W. */
    public static final int REG_CHANNEL_COUNT = 49;
    /** 50..59 (D0050..D0059) — PV температуры каналов 3..12, ×10. */
    public static final int REG_CHANNEL_3 = 50;
    public static final int REG_CHANNEL_12 = 59;

    // ─── регистры записи (команды) ─────────────────────────────────────────

    /** 60 (D0060) — 定值温度设定值: уставка температуры фикс. режима, ×10, R/W. */
    public static final int REG_SET_TEMP = 60;
    /** 61 (D0061) — 定值湿度设定值: уставка влажности фикс. режима, ×10, R/W. */
    public static final int REG_SET_HUMI = 61;
    /** 62 (D0062) — 设定运行程式号: задать номер программы, R/W. */
    public static final int REG_PROGRAM = 62;
    /** 63 (D0063) — 设定运行状态: 0 = стоп, 1 = работа, 2 = пауза, R/W. */
    public static final int REG_SET_MODE = 63;

    /** 70..73 (D0070..D0073) — 总段号 / 运行段号 / 程式循环次数 / 程式总循环. */
    public static final int REG_SEG_TOTAL = 70;
    public static final int REG_SEG_CURRENT = 71;
    public static final int REG_CYCLE_CURRENT = 72;
    public static final int REG_CYCLE_TOTAL = 73;
    /** 74..76 (D0074..D0076) — 段剩余时间 (остаток сегмента): часы / минуты / секунды. */
    public static final int REG_RMN_H = 74;
    public static final int REG_RMN_M = 75;
    public static final int REG_RMN_S = 76;
    /** 77 (D0077) — 保持状态 On Hold: 0 = не удерживать, 1 = удерживать, R/W. */
    public static final int REG_ON_HOLD = 77;
    /** 78 (D0078) — 跳段 Skip: 1 = пропустить сегмент (только запись). */
    public static final int REG_SKIP = 78;

    /** 90 (D0090) — 运行方式: 0 = 程式 (программа), 1 = 定值 (фикс.), R/W. */
    public static final int REG_OPERATION_MODE = 90;
    /** 91 (D0091) — 停电方式: 0 = стоп, 1 = холодный старт, 2 = горячий старт, R/W. */
    public static final int REG_POWER_OUTAGE = 91;
    /** 92..97 (D0092..D0097) — 当前时间: год, месяц, день, час, минута, секунда, R/W. */
    public static final int REG_SYS_YEAR = 92;
    /** 100 (D0100) — 计时设定: включение отсчёта времени, 0/1, R/W. */
    public static final int REG_TIMING_ON = 100;
    /** 101 (D0101) — 运行时间 (H.M): время шага, две цифры после запятой. */
    public static final int REG_TIME_SET = 101;
    /** 102/103 (D0102/D0103) — 温度斜率 / 湿度斜率: скорость изменения, ×10, R/W. */
    public static final int REG_RATE_TEMP = 102;
    public static final int REG_RATE_HUMI = 103;
    /** 105 (D0105) — 待机设定: включение режима ожидания, 0/1, R/W. */
    public static final int REG_STANDBY_ON = 105;
    /** 106/107 (D0106/D0107) — 温度区域 / 湿度区域: зоны температуры/влажности, ×10, R/W. */
    public static final int REG_TEMP_ZONE = 106;
    public static final int REG_HUMI_ZONE = 107;
    /** 110 (D0110) — 待机设定: включение режима ожидания, 0/1, R/W. */
    public static final int REG_STANDBY_ON_2 = 110;
    /** 111 (D0111) — 温度区域: зона температуры режима ожидания, ×10, R/W. */
    public static final int REG_STANDBY_TEMP_ZONE = 111;
    /** 112 (D0112) — 湿度区域: зона влажности режима ожидания, ×10, R/W. */
    public static final int REG_STANDBY_HUMI_ZONE = 112;
    /** 113 (D0113) — 待机时间: время ожидания, две цифры после запятой, R/W. */
    public static final int REG_STANDBY_TIME = 113;
    /** 150 (D0150) — 预约设定: вкл/выкл预约 (постановки по расписанию), 0/1, R/W. */
    public static final int REG_APPOINTMENT_ON = 150;
    /** 151..156 (D0151..D0156) — 预约时间: год, месяц, день, час, минута, секунда, R/W. */
    public static final int REG_APPOINT_YEAR = 151;

    /** 200 (D0200) — 设置程式号: выбрать программу 1..10 (только запись). */
    public static final int REG_SELECT_PROGRAM = 200;
    /** 201 (D0201) — 全部循环: общее число циклов 0..99, 0 = бесконечно, R/W. */
    public static final int REG_CYCLE_ALL = 201;
    /** 202 (D0202) — 连接程式号: номер связанной программы 0..10, R/W. */
    public static final int REG_LINK_PROGRAM = 202;
    /** 220..249 (D0220..D0249) — 程式名称: имена программ, 20 символов UTF-8, R/W. */
    public static final int REG_PROGRAM_NAME_BASE = 220;
    public static final int REG_PROGRAM_NAME_LAST = 249;
    /**
     * 250 + 5·(n−1) (D0250…) — сегменты программы: T, RH, время, TS1, TS2 (все ×10, R/W).
     * Перед обращением к сегментам обязательно записать номер программы в {@link #REG_SELECT_PROGRAM}.
     */
    public static final int REG_SEGMENT_BASE = 250;
    public static final int REG_SEGMENT_STRIDE = 5;
    /** 1000 (D1000) — 设置程式号: селектор программы; писать первым при работе с программой. */
    public static final int REG_PROGRAM_SELECTOR = 1000;

    // ─── служебные блоки (в штатном режиме — нули) ────────────────────────

    /** 8108 (0x1FAC) — программа пишет 0/1 при подключении (флаг/сброс команды). */
    public static final int REG_CMD_1FAC = 0x1FAC;
    /** 8900 (0x22C4) — блок из 64 регистров, читается на старте и по 3 в цикле. */
    public static final int REG_BLOCK_22C4 = 0x22C4;
    /** 8964 (0x2304) — блок из 26 регистров, читается один раз при старте. */
    public static final int REG_BLOCK_2304 = 0x2304;
    /** 5498 (0x157E) — блок из 16 регистров. */
    public static final int REG_BLOCK_157E = 0x157E;

    private volatile double setpoint = 25.0;
    private volatile double actual = 25.0;
    private volatile boolean powerOn = false;

    private volatile double humiSetpoint = 50.0;
    private volatile double humiActual = 50.0;
    /** 43 (D0043) — 0 = 温湿度 (T+RH), 1 = 单温度 (только температура). */
    private volatile boolean singleTempMode = false;
    /** 44 (D0044) — включена ли программа. */
    private volatile boolean programEnabled = false;
    /** 46 (D0046) — 照明按键, подсветка. */
    private volatile boolean lightOn = false;
    /** 42 (D0042) — биты 0..15 = DI1..DI16. */
    private volatile int di1to16 = 0;
    /** 45 (D0045) — биты 0..15 = DI17..DI32. */
    private volatile int di17to32 = 0;
    /** 47/48 (D0047/D0048) — 按键1 / 按键2. */
    private volatile boolean button1 = false;
    private volatile boolean button2 = false;
    /** 49 (D0049) — число каналов мониторинга, 0..10. */
    private volatile int channelCount = 2;
    /** 18/19/20 (D0018..D0020) — влажножарная температура: PV / NSP / TSP, °C. */
    private volatile double wetBulbActual = 20.0;
    private volatile double wetBulbSp = 20.0;
    private volatile double wetBulbTsp = 20.0;
    /** 35/36/37 (D0035..D0037) — программа / сегмент / группа ПИД. */
    private volatile int runProgramNo = 1;
    private volatile int runSegmentNo = 1;
    private volatile int pidGroup = 1;

    private volatile double tempMaxLimitC = 90.0;
    private volatile double tempMinLimitC = -40.0;
    private volatile double humiMaxLimitPct = 98.0;
    private volatile double humiMinLimitPct = 0.0;

    private volatile int timeSetRaw = 0;
    private volatile double tempRatePerMin = 5.0;
    private volatile double humiRatePerMin = 5.0;

    private volatile int programNumber = 1;
    private volatile int segTotal = 1;
    private volatile int segCurrent = 1;
    private volatile int cycleCurrent = 1;
    private volatile int cycleTotal = 1;
    private volatile int rmnHours = 0;
    private volatile int rmnMinutes = 0;
    private volatile int rmnSeconds = 0;
    /** 77 (D0077) — 保持状态, удержание сегмента. */
    private volatile boolean onHold = false;
    /** 90 (D0090) / 91 (D0091) — режим работы и поведение при пропадании питания. */
    private volatile int operationMode = 1;
    private volatile int powerOutageMode = 0;
    /** 100 (D0100) / 105,110 (D0105,D0110) / 113 (D0113) — таймер и режим ожидания. */
    private volatile boolean timingOn = false;
    private volatile boolean standbyOn = false;
    private volatile int standbyTimeRaw = 0;
    /** 106/107 (D0106/D0107) и 111/112 (D0111/D0112) — зоны температуры и влажности. */
    private volatile double tempZoneC = 25.0;
    private volatile double humiZonePct = 50.0;
    /** 150 (D0150) — 预约 (постановка по расписанию). */
    private volatile boolean appointmentOn = false;
    /** 200/201/202 (D0200..D0202) / 1000 (D1000) — выбор программы, циклы, связь. */
    private volatile int selectedProgram = 1;
    private volatile int cycleAll = 1;
    private volatile int linkProgram = 0;

    /** Слово состояния из блока 8900 — подкручивается для изучения реакции GUI. */
    private volatile int statusWord = 0x0000;

    private volatile long sysTimeMs = System.currentTimeMillis();
    private volatile long reserveTimeMs = System.currentTimeMillis() - 3_600_000L;
    private volatile long runTimeMs = 0;

    private volatile double rampRateDegPerMin = 5.0;
    private volatile double overshootDeg = 2.0;
    private volatile double fluctuationAmpDeg = 0.3;
    private volatile double fluctuationPeriodSec = 20.0;

    private volatile double minDeg = -40.0;
    private volatile double maxDeg = 400.0;

    private transient double stageTarget = Double.NaN;
    private transient boolean stageFirst = false;
    private transient double ripplePhase = 0.0;

    /** Зеркало всех адресов, куда прибор писал (ключ — адрес Modbus). */
    private final ConcurrentHashMap<Integer, Integer> written = new ConcurrentHashMap<>();

    /** «Обучающие» регистры: автоответ на адреса, которых нет в карте (для брутфорса). */
    private volatile boolean learnEnabled = true;
    private volatile int learnDefault = 0;

    // ─── динамика ─────────────────────────────────────────────────────────

    /** Тик симуляции: сдвигает фактическую температуру и влажность на dt (сек). */
    public synchronized void advance(double dtSec) {
        if (dtSec <= 0) {
            return;
        }
        long ms = (long) (dtSec * 1000);
        sysTimeMs += ms;
        reserveTimeMs += ms;

        if (powerOn) {
            runTimeMs += ms;
            humiActual = clampHumi(rampTowards(humiActual, singleTempMode ? humiActual : humiSetpoint,
                    humiRatePerMin / 60.0, dtSec));
        } else {
            humiActual = clampHumi(rampTowards(humiActual, 40.0, 0.2 / 60.0, dtSec));
        }

        double dir = Double.compare(setpoint, actual);
        double dist = Math.abs(setpoint - actual);

        if (dist <= EPS) {
            actual = setpoint;
            stageTarget = Double.NaN;
        } else if (Double.isNaN(stageTarget) || Math.signum(stageTarget - actual) != dir) {
            beginApproach(dir);
        }

        if (!Double.isNaN(stageTarget)) {
            double maxStep = (rampRateDegPerMin / 60.0) * dtSec;
            double step = Math.signum(stageTarget - actual) * Math.min(maxStep, Math.abs(stageTarget - actual));
            actual += step;
            if (Math.abs(stageTarget - actual) <= EPS) {
                actual = stageTarget;
                if (stageFirst) {
                    stageFirst = false;
                    stageTarget = setpoint;
                } else {
                    stageTarget = Double.NaN;
                    actual = setpoint;
                }
            }
        }

        actual = clamp(actual);
        ripplePhase = (ripplePhase + 2 * Math.PI * dtSec / fluctuationPeriodSec) % (2 * Math.PI);
    }

    private static double rampTowards(double cur, double target, double ratePerSec, double dtSec) {
        double maxStep = ratePerSec * dtSec;
        double diff = target - cur;
        if (Math.abs(diff) <= maxStep) {
            return target;
        }
        return cur + Math.signum(diff) * maxStep;
    }

    private void beginApproach(double dir) {
        if (overshootDeg <= EPS) {
            stageFirst = false;
            stageTarget = setpoint;
            return;
        }
        stageFirst = true;
        stageTarget = clamp(setpoint + dir * overshootDeg);
    }

    /** «Измеренная» температура с флуктуацией на полке. */
    public synchronized double getMeasuredTemperature() {
        return clamp(actual + fluctuationAmpDeg * Math.sin(ripplePhase));
    }

    public synchronized double getSetpoint() {
        return setpoint;
    }

    public synchronized boolean isPowerOn() {
        return powerOn;
    }

    public synchronized void setSetpoint(double t) {
        double lo = Math.max(minDeg, tempMinLimitC);
        double hi = Math.min(maxDeg, tempMaxLimitC);
        setpoint = hi < lo ? t : Math.max(lo, Math.min(hi, t));
        stageTarget = Double.NaN;
    }

    public synchronized void setPowerOn(boolean on) {
        powerOn = on;
        lightOn = on;
    }

    public synchronized double getHumidity() {
        return humiActual;
    }

    public synchronized double getHumiditySetpoint() {
        return humiSetpoint;
    }

    public synchronized void setHumiditySetpoint(double h) {
        humiSetpoint = clampHumi(h);
    }

    public synchronized boolean isHumidityEnabled() {
        return !singleTempMode;
    }

    /** D0043: {@code true} = 单温度 (только температура, влажность выключена). */
    public synchronized void setHumidityEnabled(boolean en) {
        singleTempMode = !en;
    }

    public synchronized boolean isLightOn() {
        return lightOn;
    }

    /** D0046 (46) — 照明按键, подсветка. */
    public synchronized void setLightOn(boolean on) {
        lightOn = on;
    }

    public synchronized void setDiState(int di1to16bits, int di17to32bits) {
        di1to16 = di1to16bits & 0xFFFF;
        di17to32 = di17to32bits & 0xFFFF;
    }

    /** D0035/D0036/D0037 — исполняемая программа, сегмент, группа ПИД. */
    public synchronized void setRunProgram(int programNo, int segmentNo, int pid) {
        runProgramNo = programNo;
        runSegmentNo = segmentNo;
        pidGroup = pid;
    }

    public synchronized void setProgramEnabled(boolean enabled) {
        programEnabled = enabled;
    }

    /** 18/19/20 (D0018..D0020) — влажножарная температура: PV / NSP / TSP. */
    public synchronized void setWetBulb(double actual, double sp, double tsp) {
        wetBulbActual = actual;
        wetBulbSp = sp;
        wetBulbTsp = tsp;
    }

    public synchronized int getStatusWord() {
        return statusWord;
    }

    public synchronized void setStatusWord(int w) {
        statusWord = w & 0xFFFF;
    }

    public synchronized int getRunHours() {
        return (int) (runTimeMs / 3_600_000);
    }

    public synchronized int getRunMinutes() {
        return (int) ((runTimeMs / 60_000) % 60);
    }

    public synchronized int getRunSeconds() {
        return (int) ((runTimeMs / 1000) % 60);
    }

    private static int[] clockParts(long epochMs) {
        LocalDateTime lt = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.systemDefault());
        return new int[]{lt.getYear(), lt.getMonthValue(), lt.getDayOfMonth(),
                lt.getHour(), lt.getMinute(), lt.getSecond()};
    }

    public synchronized int[] getSysTime() {
        return clockParts(sysTimeMs);
    }

    public synchronized int[] getReserveTime() {
        return clockParts(reserveTimeMs);
    }

    public synchronized void setSysTimeNow() {
        sysTimeMs = System.currentTimeMillis();
    }

    public synchronized void setReserveTimeNow() {
        reserveTimeMs = System.currentTimeMillis();
    }

    // ─── регистры ─────────────────────────────────────────────────────────

    /**
     * Значение holding/input-регистра по адресу.
     *
     * <p>Порядок: подтверждённая карта BOTO 800 → зеркало записанных адресов →
     * «обучающее» значение по умолчанию → 0.
     */
    public int readRegister(int address) {
        int addr = address & 0xFFFF;
        Integer known = knownRegister(addr);
        if (known != null) {
            return known;
        }
        Integer mirrored = written.get(addr);
        if (mirrored != null) {
            return mirrored;
        }
        return learnEnabled ? learnDefault : 0;
    }

    /** Признак «регистр известен по карте или служебный» — для подсветки в логе. */
    public boolean isKnownRegister(int address) {
        return knownRegister(address & 0xFFFF) != null;
    }

    /**
     * Карта известных регистров BOTO 800.
     *
     * @return значение или {@code null}, если адрес не описан
     */
    private synchronized Integer knownRegister(int addr) {
        int run = powerOn ? 1 : 0;
        double temp = getMeasuredTemperature();

        return switch (addr) {
            case REG_TEMP_PV -> scaled(temp);
            case REG_TEMP_SP, REG_SET_TEMP -> scaled(setpoint);
            case REG_TEMP_TSP -> scaled(setpoint);          // TSP = целевая уставка
            case REG_TEMP_MV -> scaled(mv(setpoint, temp));
            case REG_HUMI_PV -> scaled(humiActual);
            case REG_HUMI_SP, REG_SET_HUMI -> scaled(humiSetpoint);
            case REG_HUMI_TSP -> scaled(humiSetpoint);
            case REG_HUMI_MV -> scaled(mv(humiSetpoint, humiActual));
            case REG_WETBULB_PV -> scaled(wetBulbActual);
            case REG_WETBULB_SP -> scaled(wetBulbSp);
            case REG_WETBULB_TSP -> scaled(wetBulbTsp);
            case REG_OP_MODE -> operationMode;
            case REG_RUN -> run;
            case REG_RUN_H -> getRunHours();
            case REG_RUN_M -> getRunMinutes();
            case REG_RUN_S -> getRunSeconds();
            case REG_RUN_PROGRAM_NO -> runProgramNo;
            case REG_RUN_SEG_NO -> runSegmentNo;
            case REG_PID_GROUP -> pidGroup;
            case REG_TEMP_MIN -> (int) Math.round(tempMinLimitC * SCALE) & 0xFFFF;
            case REG_TEMP_MAX -> scaled(tempMaxLimitC);
            case REG_HUMI_MIN -> scaled(humiMinLimitPct);
            case REG_HUMI_MAX -> scaled(humiMaxLimitPct);
            case REG_DI_1_16 -> di1to16;
            case REG_TH_MODE -> singleTempMode ? 1 : 0;
            case REG_PROGRAM_FLAG -> programEnabled ? 1 : 0;
            case REG_DI_17_32 -> di17to32;
            case REG_LIGHT -> lightOn ? 1 : 0;
            case REG_BUTTON_1 -> button1 ? 1 : 0;
            case REG_BUTTON_2 -> button2 ? 1 : 0;
            case REG_CHANNEL_COUNT -> channelCount;
            case REG_SET_MODE -> run;                      // 0/1/2 — заданный статус
            case REG_PROGRAM -> programNumber;
            case REG_SEG_TOTAL -> segTotal;
            case REG_SEG_CURRENT -> segCurrent;
            case REG_CYCLE_CURRENT -> cycleCurrent;
            case REG_CYCLE_TOTAL -> cycleTotal;
            case REG_RMN_H -> rmnHours;
            case REG_RMN_M -> rmnMinutes;
            case REG_RMN_S -> rmnSeconds;
            case REG_ON_HOLD -> onHold ? 1 : 0;
            case REG_OPERATION_MODE -> operationMode;
            case REG_POWER_OUTAGE -> powerOutageMode;
            case REG_TIMING_ON -> timingOn ? 1 : 0;
            case REG_TIME_SET -> timeSetRaw;
            case REG_RATE_TEMP -> scaled(tempRatePerMin);
            case REG_RATE_HUMI -> scaled(humiRatePerMin);
            case REG_STANDBY_ON, REG_STANDBY_ON_2 -> standbyOn ? 1 : 0;
            case REG_TEMP_ZONE, REG_STANDBY_TEMP_ZONE -> scaled(tempZoneC);
            case REG_HUMI_ZONE, REG_STANDBY_HUMI_ZONE -> scaled(humiZonePct);
            case REG_STANDBY_TIME -> standbyTimeRaw;
            case REG_APPOINTMENT_ON -> appointmentOn ? 1 : 0;
            case REG_SELECT_PROGRAM, REG_PROGRAM_SELECTOR -> selectedProgram;
            case REG_CYCLE_ALL -> cycleAll;
            case REG_LINK_PROGRAM -> linkProgram;
            default -> serviceRegister(addr);
        };
    }

    /** Служебные блоки и сегменты: в штатном режиме нули, кроме настраиваемого слова 8900. */
    private Integer serviceRegister(int addr) {
        if (addr == REG_BLOCK_22C4) {
            return statusWord;   // слово состояния, которое прибор читает в каждом цикле
        }
        if (inBlock(addr, REG_BLOCK_22C4, 64)
                || inBlock(addr, REG_BLOCK_2304, 26)
                || inBlock(addr, REG_BLOCK_157E, 16)
                || addr == REG_SKIP
                || inBlock(addr, REG_PROGRAM_NAME_BASE,
                        REG_PROGRAM_NAME_LAST - REG_PROGRAM_NAME_BASE + 1)) {
            return 0;
        }
        if (inBlock(addr, REG_CHANNEL_3, REG_CHANNEL_12 - REG_CHANNEL_3 + 1)) {
            // −300 для канала 3, −301 для канала 4, …, −309 для канала 12
            return (-300 - (addr - REG_CHANNEL_3)) & 0xFFFF;
        }
        if (addr >= REG_SEGMENT_BASE
                && addr < REG_SEGMENT_BASE + REG_SEGMENT_STRIDE * 100) {
            int idx = (addr - REG_SEGMENT_BASE) % REG_SEGMENT_STRIDE;
            int segmentNo = (addr - REG_SEGMENT_BASE) / REG_SEGMENT_STRIDE + 1;
            return idx == 0 ? scaled(setpoint + segmentNo) : 0;   // температура сегмента
        }
        return null;
    }

    private static boolean inBlock(int addr, int base, int len) {
        return addr >= base && addr < base + len;
    }

    /** Степень коррекции 0..100 по рассогласованию. */
    private double mv(double set, double meas) {
        if (!powerOn) {
            return 0;
        }
        return clampPct(Math.abs(set - meas) / 10.0 * 100.0);
    }

    private int scaled(double v) {
        return (int) Math.round(v * SCALE) & 0xFFFF;
    }

    /** Запись значения: обновляет модель для известных регистров и запоминает адрес. */
    public void writeRegister(int address, int value) {
        int addr = address & 0xFFFF;
        int val = value & 0xFFFF;
        written.put(addr, val);
        applyWrite(addr, val);
    }

    private void applyWrite(int addr, int val) {
        switch (addr) {
            case REG_SET_TEMP, REG_TEMP_SP, REG_TEMP_TSP -> setSetpoint(rawToC(val));
            case REG_SET_HUMI, REG_HUMI_SP, REG_HUMI_TSP -> setHumiditySetpoint(rawToC(val));
            case REG_SET_MODE, REG_RUN -> setPowerOn(val == 1);
            case REG_TEMP_PV -> actual = rawToC(val);
            case REG_HUMI_PV -> humiActual = rawToC(val);
            case REG_WETBULB_PV -> wetBulbActual = rawToC(val);
            case REG_WETBULB_SP, REG_WETBULB_TSP -> {
                wetBulbSp = rawToC(val);
                wetBulbTsp = wetBulbSp;
            }
            case REG_OP_MODE, REG_OPERATION_MODE -> operationMode = val;
            case REG_TH_MODE -> singleTempMode = val == 1;
            case REG_PROGRAM_FLAG -> programEnabled = val == 1;
            case REG_DI_1_16 -> di1to16 = val;
            case REG_DI_17_32 -> di17to32 = val;
            case REG_LIGHT -> setLightOn(val == 1);
            case REG_BUTTON_1 -> button1 = val == 1;
            case REG_BUTTON_2 -> button2 = val == 1;
            case REG_CHANNEL_COUNT -> channelCount = Math.min(10, val);
            case REG_TEMP_MIN -> tempMinLimitC = rawToC((short) val);
            case REG_TEMP_MAX -> tempMaxLimitC = rawToC(val);
            case REG_HUMI_MIN -> humiMinLimitPct = rawToC(val);
            case REG_HUMI_MAX -> humiMaxLimitPct = rawToC(val);
            case REG_PROGRAM -> programNumber = val;
            case REG_SEG_TOTAL -> segTotal = Math.max(1, val);
            case REG_SEG_CURRENT -> segCurrent = val;
            case REG_CYCLE_CURRENT -> cycleCurrent = val;
            case REG_CYCLE_TOTAL -> cycleTotal = Math.max(1, val);
            case REG_RMN_H -> rmnHours = val;
            case REG_RMN_M -> rmnMinutes = Math.min(59, val);
            case REG_RMN_S -> rmnSeconds = Math.min(59, val);
            case REG_ON_HOLD -> onHold = val == 1;
            case REG_POWER_OUTAGE -> powerOutageMode = val;
            case REG_TIMING_ON -> timingOn = val == 1;
            case REG_TIME_SET -> timeSetRaw = val;
            case REG_RATE_TEMP -> tempRatePerMin = rawToC(val);
            case REG_RATE_HUMI -> humiRatePerMin = rawToC(val);
            case REG_STANDBY_ON, REG_STANDBY_ON_2 -> standbyOn = val == 1;
            case REG_TEMP_ZONE, REG_STANDBY_TEMP_ZONE -> tempZoneC = rawToC(val);
            case REG_HUMI_ZONE, REG_STANDBY_HUMI_ZONE -> humiZonePct = rawToC(val);
            case REG_STANDBY_TIME -> standbyTimeRaw = val;
            case REG_APPOINTMENT_ON -> appointmentOn = val == 1;
            case REG_SELECT_PROGRAM, REG_PROGRAM_SELECTOR -> selectedProgram = val;
            case REG_CYCLE_ALL -> cycleAll = val;
            case REG_LINK_PROGRAM -> linkProgram = val;
            default -> {
                if (addr == REG_BLOCK_22C4) {
                    statusWord = val;
                }
            }
        }
    }

    private static double rawToC(int raw) {
        return raw / (double) SCALE;
    }

    /** Значение катушки (coils/discrete inputs): 0xFF00 = включено. */
    public int readCoil(int address) {
        int addr = address & 0xFFFF;
        if (addr == 0 || addr == REG_RUN || addr == REG_SET_MODE) {
            return powerOn ? 0xFF00 : 0x0000;
        }
        return written.getOrDefault(addr, 0);
    }

    public void writeCoil(int address, int value) {
        writeRegister(address, (value & 0xFF00) == 0xFF00 ? 1 : 0);
    }

    /** Снимок зеркала записанных адресов (по возрастанию) — для дампа в лог. */
    public Map<Integer, Integer> writtenRegisters() {
        return new TreeMap<>(written);
    }

    /** Сбросить зеркало записанных адресов (кнопка в панели). */
    public void clearWritten() {
        written.clear();
    }

    /** Статистика зеркала: адрес → значение (для строки статуса). */
    public String writtenSummary() {
        Map<Integer, Integer> snap = writtenRegisters();
        if (snap.isEmpty()) {
            return "неизвестных адресов не было";
        }
        StringBuilder sb = new StringBuilder(snap.size() * 12);
        int i = 0;
        for (Map.Entry<Integer, Integer> e : snap.entrySet()) {
            if (i++ > 0) {
                sb.append(", ");
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
            if (i >= 12) {
                sb.append(", … (+").append(snap.size() - 12).append(')');
                break;
            }
        }
        return sb.toString();
    }

    /** Список адресов, которые прибор уже опрашивал (для подсказки в панели). */
    public List<Integer> touchedAddresses() {
        List<Integer> keys = new ArrayList<>(written.keySet());
        keys.sort(Integer::compareTo);
        return keys;
    }

    public void setLearnEnabled(boolean v) {
        this.learnEnabled = v;
    }

    public boolean isLearnEnabled() {
        return learnEnabled;
    }

    public void setLearnDefault(int v) {
        this.learnDefault = v & 0xFFFF;
    }

    public int getLearnDefault() {
        return learnDefault;
    }

    // ─── настройки ────────────────────────────────────────────────────────

    public void setRampRateDegPerMin(double v) {
        if (v > 0) {
            this.rampRateDegPerMin = v;
        }
    }

    public void setOvershootDeg(double v) {
        this.overshootDeg = Math.max(0, v);
    }

    public void setFluctuationAmpDeg(double v) {
        this.fluctuationAmpDeg = Math.max(0, v);
    }

    public void setFluctuationPeriodSec(double v) {
        if (v > 0) {
            this.fluctuationPeriodSec = v;
        }
    }

    public void setRange(double min, double max) {
        this.minDeg = min;
        this.maxDeg = max;
    }

    public double getRampRateDegPerMin() {
        return rampRateDegPerMin;
    }

    public double getOvershootDeg() {
        return overshootDeg;
    }

    public double getFluctuationAmpDeg() {
        return fluctuationAmpDeg;
    }

    public double getFluctuationPeriodSec() {
        return fluctuationPeriodSec;
    }

    /** Сброс модели в исходное состояние. */
    public void reset() {
        written.clear();
        synchronized (this) {
            setpoint = 25.0;
            actual = setpoint;
            powerOn = false;
            humiSetpoint = 50.0;
            humiActual = humiSetpoint;
            singleTempMode = false;
            lightOn = false;
            di1to16 = 0;
            di17to32 = 0;
            programEnabled = false;
            statusWord = 0;
            runTimeMs = 0;
            stageTarget = Double.NaN;
            stageFirst = false;
            ripplePhase = 0.0;
            sysTimeMs = System.currentTimeMillis();
        }
    }

    /**
     * Карта известных регистров для подсказки в GUI.
     *
     * <p>Имена — по официальному документу «680通信协议V1.0», лист «基本运行信息».
     */
    public static Map<String, String> knownRegisterMap() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("10", "D0010 温度测试值 — PV температуры, ×10 (−300 = датчик отключён)");
        m.put("11", "D0011 温度设定值 NSP — уставка температуры, ×10");
        m.put("12", "D0012 温度目标设定值 TSP — целевая уставка, ×10");
        m.put("13", "D0013 温度出力 — выход регулятора, ×10");
        m.put("14", "D0014 湿度测试值 — PV влажности, ×10");
        m.put("15", "D0015 湿度设定值 NSP — уставка влажности, ×10");
        m.put("16", "D0016 湿度目标设定值 TSP — целевая уставка, ×10");
        m.put("17", "D0017 湿度出力 — выход регулятора, ×10");
        m.put("18-20", "D0018..D0020 湿球温度 — PV / NSP / TSP, ×10");
        m.put("30", "D0030 运行模式 — 0 = 程式 (программа), 1 = 定值 (фикс.)");
        m.put("31", "D0031 运行状态 — 0 = стоп, 1 = работа, 2 = пауза");
        m.put("32-34", "D0032..D0034 运行时间 — наработка: ч/м/с");
        m.put("35-37", "D0035..D0037 运行程式号 / 段号 / PID组");
        m.put("38-41", "D0038..D0041 设定范围低/高 — пределы ввода T (38/39) и RH (40/41)");
        m.put("42", "D0042 DI状态 — биты 0..15 = DI1..DI16");
        m.put("43", "D0043 温湿度/单温 — 0 = T+RH, 1 = только T");
        m.put("44", "D0044 程式标志 — 0 = программа выкл., 1 = вкл.");
        m.put("45", "D0045 DI状态 — биты 0..15 = DI17..DI32");
        m.put("46-48", "D0046..D0048 照明按键 / 按键1 / 按键2, R/W");
        m.put("49", "D0049 监控通道数量 — 0..10, R/W");
        m.put("50-59", "D0050..D0059 通道3..12 温度测试值, ×10");
        m.put("60", "D0060 定值温度设定值 — уставка температуры, ×10, R/W");
        m.put("61", "D0061 定值湿度设定值 — уставка влажности, ×10, R/W");
        m.put("62", "D0062 设定运行程式号 — задать программу, R/W");
        m.put("63", "D0063 设定运行状态 — 0 = стоп, 1 = работа, 2 = пауза, R/W");
        m.put("70-73", "D0070..D0073 总段号 / 运行段号 / 循环次数 / 总循环");
        m.put("74-76", "D0074..D0076 段剩余时间 — остаток сегмента: ч/м/с");
        m.put("77", "D0077 保持状态 — 0 = не удерживать, 1 = удерживать, R/W");
        m.put("78", "D0078 跳段 — 1 = пропустить сегмент (только запись)");
        m.put("90-91", "D0090 运行方式 / D0091 停电方式, R/W");
        m.put("92-97", "D0092..D0097 当前时间 — год, месяц, день, час, мин, сек, R/W");
        m.put("100-103", "D0100 计时设定 / D0101 运行时间 H.M / D0102,103 斜率");
        m.put("105-107", "D0105 待机设定 / D0106,107 温度区域, 湿度区域, R/W");
        m.put("110-113", "D0110 待机设定 / D0111,112 区域 / D0113 待机时间, R/W");
        m.put("150", "D0150 预约设定 — вкл/выкл постановки по расписанию, R/W");
        m.put("151-156", "D0151..D0156 预约时间 — год, месяц, день, час, мин, сек, R/W");
        m.put("200-202", "D0200 设置程式号 (1..10) / D0201 全部循环 / D0202 连接程式号");
        m.put("220-249", "D0220..D0249 程式名称 — имена программ, 20 символов UTF-8");
        m.put("250+", "D0250+(n−1)·5 — сегменты программы: T, RH, время, TS1, TS2");
        m.put("1000", "D1000 设置程式号 — писать первым перед сегментами");
        m.put("5498", "служебный блок 0x157E × 16 → 0 (не документирован)");
        m.put("8108", "0x1FAC — пишется 0/1 при подключении (не документирован)");
        m.put("8900", "0x22C4 × 64 → 0; 8900..8902 = слово состояния (настраивается)");
        m.put("8964", "0x2304 × 26 → 0 (не документирован)");
        return m;
    }

    private double clamp(double v) {
        return Math.max(minDeg, Math.min(maxDeg, v));
    }

    private static double clampHumi(double v) {
        return clampPct(v);
    }

    private static double clampPct(double v) {
        return Math.max(0, Math.min(100, v));
    }
}
