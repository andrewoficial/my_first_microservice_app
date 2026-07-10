package org.example.gui.devices.edvards.d39730880.emulation;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Эмулятор Edwards TIC (D397).
 * Хранит внутреннее состояние и генерирует ответы на ASCII-команды.
 */
public class EdwardsResponder {

    private volatile int turboState = 0;           // 0=off, 4=running
    private volatile int turboAlert = 0;
    private volatile int turboPriority = 0;

    private volatile double[] gaugeValues = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
    private volatile int[] gaugeStates = {0, 0, 0, 0, 0, 0};
    private volatile int[] gaugeAlerts = {0, 0, 0, 0, 0, 0};
    private volatile int[] gaugePriorities = {0, 0, 0, 0, 0, 0};

    private volatile double psTemp = 35.0;
    private volatile double internalTemp = 32.0;

    private final Map<String, String> lastSetups = new ConcurrentHashMap<>();

    public EdwardsResponder() {
        gaugeStates[0] = 11;
        gaugeValues[0] = 1.234e-3;
    }

    public synchronized String processCommand(String cmd) {
        cmd = cmd.trim();
        if (cmd.isEmpty()) return null;

        if (cmd.startsWith("#")) {
            int colon = cmd.indexOf(':');
            if (colon > 0) cmd = cmd.substring(colon + 1).trim();
        }

        if (cmd.startsWith("?V")) return handleQueryValue(cmd);
        if (cmd.startsWith("?S")) return handleQuerySetup(cmd);
        if (cmd.startsWith("IC") || cmd.startsWith("!C")) return handleCommand(cmd);
        if (cmd.startsWith("!S")) return handleSetSetup(cmd);

        return "*C000 2\r";
    }

    private String handleQueryValue(String cmd) {
        if (cmd.contains("902")) return "=V902 4;4;11;0;0;0;0;0;0;0\r";
        if (cmd.contains("904")) return String.format("=V904 %d;%d;%d\r", turboState, turboAlert, turboPriority);
        if (cmd.contains("913")) return String.format("=V913 %.4e;59;%d;%d;%d\r", gaugeValues[0], gaugeStates[0], gaugeAlerts[0], gaugePriorities[0]);
        if (cmd.contains("914")) return String.format("=V914 %.4e;59;%d;%d;%d\r", gaugeValues[1], gaugeStates[1], gaugeAlerts[1], gaugePriorities[1]);
        if (cmd.contains("915")) return String.format("=V915 %.4e;59;%d;%d;%d\r", gaugeValues[2], gaugeStates[2], gaugeAlerts[2], gaugePriorities[2]);
        if (cmd.contains("940")) {
            StringBuilder sb = new StringBuilder("=V940 ");
            for (int i = 0; i < 3; i++) if (gaugeStates[i] > 0) sb.append(String.format("%d;%.4e;", i + 1, gaugeValues[i]));
            sb.append("\r");
            return sb.toString();
        }
        if (cmd.contains("919")) return String.format("=V919 %.1f;0;0\r", psTemp);
        if (cmd.contains("920")) return String.format("=V920 %.1f;0;0\r", internalTemp);
        return "=V000 0;0;0\r";
    }

    private String handleQuerySetup(String cmd) {
        if (cmd.contains("904 4")) return "=S904 913;59;1.0e-3;5.0e-3;1\r";
        return "=S000 0\r";
    }

    private String handleCommand(String cmd) {
        if (cmd.contains("904 1")) { turboState = 4; return "*C904 0\r"; }
        if (cmd.contains("904 0")) { turboState = 0; return "*C904 0\r"; }
        if (cmd.contains("913 1")) { gaugeStates[0] = 11; return "*C913 0\r"; }
        if (cmd.contains("913 0")) { gaugeStates[0] = 5; return "*C913 0\r"; }
        return "*C000 0\r";
    }

    private String handleSetSetup(String cmd) {
        lastSetups.put(cmd, "OK");
        return "*S000 0\r";
    }

    // === Методы для GUI ===
    public synchronized void setGaugeValue(int gaugeIndex, double value) {
        if (gaugeIndex >= 0 && gaugeIndex < gaugeValues.length) gaugeValues[gaugeIndex] = value;
    }

    public synchronized void setTurboState(int state) { this.turboState = state; }
    public synchronized void setTemperature(double ps, double internal) { this.psTemp = ps; this.internalTemp = internal; }

    public synchronized int getTurboState() { return turboState; }
    public synchronized double getGaugeValue(int idx) { return (idx >= 0 && idx < gaugeValues.length) ? gaugeValues[idx] : 0.0; }
}