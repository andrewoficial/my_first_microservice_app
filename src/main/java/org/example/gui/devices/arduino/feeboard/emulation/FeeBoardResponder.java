package org.example.gui.devices.arduino.feeboard.emulation;

import org.example.utilites.MyUtilities;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Минимальный имитатор ARD_FEE_BRD_METER (ccm_fee.md).
 * Задаются: температура, ток, влажность, давление, питание потребителя.
 */
public class FeeBoardResponder {

    private volatile double temperatureC = 25.3;
    private volatile double currentMa = 0.15;
    private volatile double humidityPct = 40.0;
    private volatile double pressureMmHg = 759.4;
    private volatile double consumerVolts = 3.30;
    private volatile boolean feePowerOn = false;
    private volatile boolean logMode = false;
    private volatile int logAproxK1 = 1;
    private volatile int logAproxK2 = 1;
    private volatile int address = 2;
    private volatile String serial = "08500012";
    private volatile String swRev = "SW:2.19 07.11.2023";

    public synchronized String processCommand(String raw) {
        if (raw == null) {
            return null;
        }
        String cmd = raw.trim();
        if (cmd.isEmpty()) {
            return null;
        }

        String upper = cmd.toUpperCase(Locale.ROOT);

        if (upper.equals("F")) {
            return buildFResponse();
        }
        if (upper.equals("MMESU") || upper.equals("LOGO") || upper.equals("LOG")) {
            if (upper.equals("LOG")) {
                logMode = !logMode;
            }
            return buildMmesuLine();
        }
        if (upper.equals("CONC?")) {
            return String.format(Locale.US,
                    "FEE:0\tCUR:%.3f\tTER:%.2f\tHUD:%.0f\tPRS:%.1f\tPWR:%.2f",
                    currentMa, temperatureC, humidityPct, pressureMmHg, consumerVolts);
        }
        if (upper.equals("FPWR?")) {
            return "feePWR:\t" + (feePowerOn ? "ON" : "OFF");
        }
        if (upper.equals("FPWR")) {
            feePowerOn = !feePowerOn;
            return "feePWR:\t" + (feePowerOn ? "ON" : "OFF");
        }
        if (upper.equals("SENSON")) {
            return "Ok";
        }
        if (upper.equals("SENSOFF")) {
            return "Ok";
        }
        if (upper.equals("SREV?")) {
            return swRev;
        }
        if (upper.equals("SRAL?")) {
            return serial;
        }
        if (upper.equals("%**")) {
            return String.format("!%02d", address);
        }
        if (upper.equals("GCOEF")) {
            return buildGcoef();
        }
        if (upper.equals("REBOOT")) {
            return "Ok\r*********************************\r*\tDevice: CurMeter\t*\r*\tS/N: " + serial
                    + "\t\t*\r*\tAdres: " + String.format("%02d", address)
                    + "\t\t*\r*\t" + swRev + "\t*\r*********************************\rBME280 on def";
        }
        if (upper.startsWith("SLAS")) {
            logAproxK1 = parseTrailingInt(cmd, logAproxK1);
            return String.valueOf(logAproxK1);
        }
        if (upper.startsWith("SDAS")) {
            logAproxK2 = parseTrailingInt(cmd, logAproxK2);
            return String.valueOf(logAproxK2);
        }
        if (upper.equals("SPOLY0") || upper.equals("SPOLY1") || upper.equals("SVOLT0")
                || upper.equals("STRGLV") || upper.equals("SV2AMP") || upper.equals("SCABD")
                || upper.equals("URTMOD")) {
            return "Ok";
        }

        return "ERR";
    }

    private String buildMmesuLine() {
        // One_V Two_V Thre_V Cur_One Cur_Two FPWR C_One_Poly C_Two_Poly Term Pres Hydm CurrRes
        double oneV = currentMa / 18.34 / 1000.0; // rough inverse of VLT_ToAmperN_K1
        double twoV = currentMa / 3.6 / 1000.0;
        double curOne = currentMa * 0.9;
        double curTwo = currentMa * 0.1;
        return String.format(Locale.US,
                "%.3f\tV\t%.3f\tV\t%.3f\tV\t%.3f\tmA\t%.3f\tmA\t%s\t%.3f\tmA\t%.3f\tmA\t%.2f\tdeg C\t%.1f\tmm Hg\t%.0f\t%%\t%.2f\tmA",
                oneV, twoV, consumerVolts, curOne, curTwo,
                feePowerOn ? "ON" : "OFF",
                curOne, curTwo,
                temperatureC, pressureMmHg, humidityPct, currentMa);
    }

    private String buildFResponse() {
        // fixed-width tab-separated digits + CRC at [70]
        // layout matches ArdFeeBrdMeterCommandRegistry.F_FIELDS
        int term = (int) Math.round(temperatureC * 100);
        int pres = (int) Math.round(pressureMmHg * 10);
        int hyd = (int) Math.round(humidityPct);
        int threV = (int) Math.round(consumerVolts * 100);
        int curOne = (int) Math.round(currentMa * 1000); // stored as mA*1000 in field with /1000 → A; use mA*1 for display in A scale of registry
        // Registry divides cur fields by 1000 → treat raw as mA*1000 for milliamp display as A? 
        // F units are " A" with divisor 1000, so raw 150 → 0.15 A if we want 150 mA.
        // User thinks in mA: currentMa=0.15 means 0.15 mA. For demo use currentMa as mA and store *100 so /1000 → mA/10...
        // Simpler: store as integer mA * 100 (CurrResF *100 per docs) for CurrRes, and cur fields similarly *100.
        int curRaw = (int) Math.round(currentMa * 100); // if currentMa is mA
        int stat = feePowerOn ? 1 : 0;

        String body = String.format(Locale.US,
                "\t%05d\t%05d\t%05d\t%05d\t%05d\t%05d\t%05d\t%05d\t%05d\t%05d\t%s",
                clamp5(term), clamp5(pres), clamp5(hyd), clamp5(threV),
                clamp5(curRaw), clamp5(curRaw / 4), clamp5(curRaw), clamp5(curRaw / 4),
                clamp5(curRaw), clamp5(stat), pad8(serial));

        byte[] bytes = new byte[71];
        byte[] src = body.getBytes(StandardCharsets.US_ASCII);
        System.arraycopy(src, 0, bytes, 0, Math.min(src.length, 70));
        // pad rest with spaces if short
        for (int i = src.length; i < 70; i++) {
            bytes[i] = ' ';
        }
        bytes[70] = MyUtilities.calculateCRCforF(bytes);
        return new String(bytes, 0, 71, StandardCharsets.US_ASCII);
    }

    private String buildGcoef() {
        return "14.10.2023\r" +
                "CH0 (Big)\r" +
                "A0_0:\t0.00256319189071655273\r" +
                "A1_0:\t0.73276896476745605468\r" +
                "CH1 (Small)\r" +
                "A0_1:\t0.00106998610496520996\r" +
                "Other\r" +
                "LogAproxStep_K1:\t" + logAproxK1 + "\r" +
                "LogAproxStep_K2:\t" + logAproxK2 + "\r" +
                "VLT_ToAmperN_K1:\t18.34000015258789062500\r" +
                "VLT_ToAmperN_K2:\t3.59999990463256835937";
    }

    private static int parseTrailingInt(String cmd, int def) {
        String[] p = cmd.trim().split("\\s+");
        if (p.length < 2) {
            return def;
        }
        try {
            return Integer.parseInt(p[1].trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int clamp5(int v) {
        if (v < 0) v = 0;
        if (v > 99999) v = 99999;
        return v;
    }

    private static String pad8(String s) {
        if (s == null) s = "00000000";
        if (s.length() >= 8) return s.substring(0, 8);
        return String.format("%8s", s).replace(' ', '0');
    }

    // ─── UI setters / getters ────────────────────────────────────────────

    public synchronized void setTemperatureC(double v) { this.temperatureC = v; }
    public synchronized void setCurrentMa(double v) { this.currentMa = v; }
    public synchronized void setHumidityPct(double v) { this.humidityPct = v; }
    public synchronized void setPressureMmHg(double v) { this.pressureMmHg = v; }
    public synchronized void setConsumerVolts(double v) { this.consumerVolts = v; }
    public synchronized void setFeePowerOn(boolean on) { this.feePowerOn = on; }

    public synchronized double getTemperatureC() { return temperatureC; }
    public synchronized double getCurrentMa() { return currentMa; }
    public synchronized double getHumidityPct() { return humidityPct; }
    public synchronized double getPressureMmHg() { return pressureMmHg; }
    public synchronized double getConsumerVolts() { return consumerVolts; }
    public synchronized boolean isFeePowerOn() { return feePowerOn; }
}
