package org.example.gui.devices.boto.emulation;

/**
 * Модель термокамеры BOTO (эмуляция).
 * <p>
 * Температура и влажность плавно выходят на свои уставки (ramp).
 * При выключенном состоянии температура падает, влажность — к комнатному значению.
 */
public class BotoEmulator {

    private static final double ROOM_TEMP_C = 20.0;         // комната при выключении
    private static final double ROOM_HUM_PCT = 40.0;
    private static final double OFF_DRIFT_C_PER_SEC = 0.2 / 60.0; // медленно, как у Testa
    private static final double OFF_HUM_PER_SEC = 0.2 / 60.0;

    private volatile double currentTempC = 20.0;
    private volatile double setpointC = 25.0;
    private volatile boolean on = false;
    private volatile double rampRateCPerSec = 2.0;

    private volatile double currentHumidity = 40.0;
    private volatile double humiditySetpoint = 50.0;
    private volatile double humidityRampPerSec = 1.0;

    // Найденные по штатной программе регистры (см. readRegister в BotoModbusResponder).
    private volatile double tempMaxLimitC = 90.0;   // рег 39 (raw = C*10)
    private volatile double humiMaxLimitPct = 98.0; // рег 41 (raw = %*10)
    private volatile int timeSetRaw = 0;            // рег 101 (raw; GUI /100)
    private volatile double tempRatePerMin = 0.5;   // рег 102 (raw = *10)
    private volatile double humiRatePerMin = 0.5;   // рег 103 (raw = *10)

    private volatile long runTimeMs = 0;            // накопленное время работы (рег 32/33/34)

    // Часы: SYSTEM TIME (рег 92..97: Y M D H Min S) и Reserve time (рег 151..156).
    private volatile long sysTimeMs = System.currentTimeMillis();
    private volatile long reserveTimeMs = System.currentTimeMillis() - 3_600_000L; // на 1 час назад

    // Тест/программа (см. readRegister). Поля по умолчанию = "нейтральное состояние".
    private volatile int programNumber = 1;      // 62
    private volatile int segTotal = 1;           // 70
    private volatile int segCurrent = 1;         // 71
    private volatile int cycleCurrent = 1;       // 72
    private volatile int cycleTotal = 1;         // 73
    private volatile int rmnHours = 0;           // 74
    private volatile int rmnMinutes = 0;         // 75
    private volatile int rmnSeconds = 0;         // 76
    private volatile double progWaitTempC = 0;   // 111 (raw = C*10)
    private volatile double progWaitHumPct = 0;  // 112 (raw = %*10)

    // Нижние пределы (рег 38 темп — знаковый /10, рег 40 влага /10).
    private volatile double tempMinLimitC = -40.0;  // рег 38
    private volatile double humiMinLimitPct = 0.0;  // рег 40

    /**
     * Продвижение имитации.
     */
    public synchronized void advance(double dtSeconds) {
        double d = Math.min(dtSeconds, 0.5);

        long ms = (long) (d * 1000);
        sysTimeMs += ms;
        reserveTimeMs += ms;

        if (on) {
            runTimeMs += ms;
            // температура к уставке
            currentTempC = rampTowards(currentTempC, setpointC, rampRateCPerSec, d);
            // влажность к уставке
            currentHumidity = clamp(rampTowards(currentHumidity, humiditySetpoint,
                    humidityRampPerSec, d), 0, 100);
        } else {
            // выключено: медленный дрейф к «комнате» (медленнее, чем у Testa)
            currentTempC = clamp(rampTowards(currentTempC, ROOM_TEMP_C, OFF_DRIFT_C_PER_SEC, d), 0, 400);
            currentHumidity = clamp(rampTowards(currentHumidity, ROOM_HUM_PCT, OFF_HUM_PER_SEC, d), 0, 100);
        }
    }

    private static double rampTowards(double cur, double target, double rate, double dt) {
        double diff = target - cur;
        if (Math.abs(diff) < rate * dt) return target;
        return cur + Math.signum(diff) * rate * dt;
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public synchronized double getCurrentTempC() { return currentTempC; }
    public synchronized double getSetpointC() { return setpointC; }
    public synchronized double getCurrentHumidity() { return currentHumidity; }
    public synchronized double getHumiditySetpoint() { return humiditySetpoint; }
    public synchronized boolean isOn() { return on; }
    public synchronized double getRampRateCPerSec() { return rampRateCPerSec; }

    public synchronized void setCurrentTempC(double t) { currentTempC = t; }
    public synchronized void setSetpointC(double t) { setpointC = Math.max(0, Math.min(400, t)); }
    public synchronized void setCurrentHumidity(double h) { currentHumidity = clamp(h, 0, 100); }
    public synchronized void setHumiditySetpoint(double h) { humiditySetpoint = clamp(h, 0, 100); }
    public synchronized void setOn(boolean on) { this.on = on; }
    public synchronized void setRampRateCPerSec(double rate) { this.rampRateCPerSec = Math.max(0.1, rate); }

    public synchronized double getTempMaxLimitC() { return tempMaxLimitC; }
    public synchronized void setTempMaxLimitC(double v) { tempMaxLimitC = Math.max(0, Math.min(400, v)); }
    public synchronized double getHumiMaxLimitPct() { return humiMaxLimitPct; }
    public synchronized void setHumiMaxLimitPct(double v) { humiMaxLimitPct = clamp(v, 0, 100); }
    public synchronized int getTimeSetRaw() { return timeSetRaw; }
    public synchronized void setTimeSetRaw(int v) { timeSetRaw = Math.max(0, Math.min(0xFFFF, v)); }
    public synchronized double getTempRatePerMin() { return tempRatePerMin; }
    public synchronized void setTempRatePerMin(double v) { tempRatePerMin = Math.max(0, v); }
    public synchronized double getHumiRatePerMin() { return humiRatePerMin; }
    public synchronized void setHumiRatePerMin(double v) { humiRatePerMin = Math.max(0, v); }

    public synchronized long getRunTimeMs() { return runTimeMs; }
    public synchronized void resetRunTime() { runTimeMs = 0; }
    public synchronized int getRunHours() { return (int) (runTimeMs / 3_600_000); }
    public synchronized int getRunMinutes() { return (int) ((runTimeMs / 60_000) % 60); }
    public synchronized int getRunSeconds() { return (int) ((runTimeMs / 1000) % 60); }

    /** Компоненты часов [год, мес, день, час, мин, сек] по epochMs. */
    private static int[] clockParts(long epochMs) {
        java.time.LocalDateTime lt =
                java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMs),
                        java.time.ZoneId.systemDefault());
        return new int[]{lt.getYear(), lt.getMonthValue(), lt.getDayOfMonth(),
                lt.getHour(), lt.getMinute(), lt.getSecond()};
    }

    /** [Y,M,D,H,Min,S] системных часов (SYSTEM TIME). */
    public synchronized int[] getSysTime() { return clockParts(sysTimeMs); }
    /** [Y,M,D,H,Min,S] резервных часов (Reserve time). */
    public synchronized int[] getReserveTime() { return clockParts(reserveTimeMs); }

    public synchronized void setSysTimeNow() { sysTimeMs = System.currentTimeMillis(); }
    public synchronized void setReserveTimeNow() { reserveTimeMs = System.currentTimeMillis(); }
    public synchronized void addSysTimeHours(double h) { sysTimeMs += (long) (h * 3_600_000); }
    public synchronized void addReserveTimeHours(double h) { reserveTimeMs += (long) (h * 3_600_000); }
    /** Установить системные часы по компонентам [Y,M,D,H,Min,S]. */
    public synchronized void setSysTime(int y, int mo, int d, int h, int mi, int s) {
        sysTimeMs = toEpochMs(y, mo, d, h, mi, s);
    }
    /** Установить резервные часы по компонентам. */
    public synchronized void setReserveTime(int y, int mo, int d, int h, int mi, int s) {
        reserveTimeMs = toEpochMs(y, mo, d, h, mi, s);
    }

    private static long toEpochMs(int y, int mo, int d, int h, int mi, int s) {
        return java.time.LocalDateTime.of(y, mo, d, h, mi, s)
                .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    public synchronized int getProgramNumber() { return programNumber; }
    public synchronized void setProgramNumber(int v) { programNumber = Math.max(0, v); }
    public synchronized int getSegTotal() { return segTotal; }
    public synchronized void setSegTotal(int v) { segTotal = Math.max(1, v); }
    public synchronized int getSegCurrent() { return segCurrent; }
    public synchronized void setSegCurrent(int v) { segCurrent = Math.max(0, v); }
    public synchronized int getCycleCurrent() { return cycleCurrent; }
    public synchronized void setCycleCurrent(int v) { cycleCurrent = Math.max(0, v); }
    public synchronized int getCycleTotal() { return cycleTotal; }
    public synchronized void setCycleTotal(int v) { cycleTotal = Math.max(1, v); }
    public synchronized int getRmnHours() { return rmnHours; }
    public synchronized void setRmnTime(int h, int m, int s) {
        rmnHours = Math.max(0, h); rmnMinutes = Math.max(0, Math.min(59, m)); rmnSeconds = Math.max(0, Math.min(59, s));
    }
    public synchronized int[] getRmnTime() { return new int[]{rmnHours, rmnMinutes, rmnSeconds}; }
    public synchronized double getProgWaitTempC() { return progWaitTempC; }
    public synchronized void setProgWaitTempC(double v) { progWaitTempC = Math.max(0, v); }
    public synchronized double getProgWaitHumPct() { return progWaitHumPct; }
    public synchronized void setProgWaitHumPct(double v) { progWaitHumPct = Math.max(0, Math.min(100, v)); }
    public synchronized double getTempMinLimitC() { return tempMinLimitC; }
    public synchronized void setTempMinLimitC(double v) { tempMinLimitC = Math.max(-200, Math.min(400, v)); }
    public synchronized double getHumiMinLimitPct() { return humiMinLimitPct; }
    public synchronized void setHumiMinLimitPct(double v) { humiMinLimitPct = clamp(v, 0, 100); }
}
