package org.example.services.rule.com;

import lombok.Getter;
import org.example.device.SomeDevice;
import org.example.device.protOwonSpe3051.OWON_SPE3051;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class OwonVoltLinearRule implements ComRule, Serializable {
    @Getter
    private final String ruleId;

    private final float startVoltage;
    private final float endVoltage;
    private final float step;
    private final long delayMs;
    private float currentVoltage;
    private boolean isWaitingForAnswer;
    private long millisPrev;
    private String ruleDescription;
    public OwonVoltLinearRule(
            @RuleParameter(name = "Start Voltage", type = "FLOAT", min = 0, max = 24, description = "Initial voltage in volts")
            float start,

            @RuleParameter(name = "End Voltage", type = "FLOAT", min = 0, max = 24, description = "Final voltage in volts")
            float end,

            @RuleParameter(name = "Step", type = "FLOAT", min = 0.01, max = 100, description = "Voltage step in volts")
            float step,

            @RuleParameter(name = "Interval (ms)", type = "INT", min = 0, max = 10000, description = "Delay between voltage changes in ms")
            long delay,

            @RuleParameter(name = "Rule description", type = "STRING")
            String ruleDescription
    ) {

        this.startVoltage = start;
        this.endVoltage = end;
        this.step = step;
        this.delayMs = delay;
        this.currentVoltage = start;
        this.ruleDescription = ruleDescription;
        millisPrev = System.currentTimeMillis() - (delayMs * 100);
        ruleId = UUID.randomUUID().toString();
        System.out.println("Created rule with id: " + ruleId);
    }
    
    @Override
    public String generateCommand() {
        System.out.println("Run cmd generation ");
        StringBuilder sb = new StringBuilder();
        sb.append("VOLT ");
        System.out.println("current voltage " + currentVoltage);
        sb.append(String.format("%.2f", currentVoltage));
        byte [] strEnd = getTargetDevice().getStrEndian();
        for (byte b : strEnd) {
            sb.append(b);
        }
        currentVoltage += step;
        if (currentVoltage > endVoltage) currentVoltage = startVoltage;
        //sb.replace(sb.indexOf(","), sb.indexOf(",") + 1, ".");


        return sb.toString().replace(",", ".");
    }

    @Override
    public void processResponse(byte[] response) {
        // Обработка ответа в данном правиле не требуется
    }

    @Override
    public void reset() {

    }

    @Override
    public String getRuleId() {
        return this.ruleId;
    }

    @Override
    public RuleType getRuleType() {
        return null;
    }



    @Override
    public String getDescription() {
        return this.ruleDescription;
    }

    @Override
    public void updateState() {

    }

    @Override
    public long getNextPoolDelay() {
        return this.delayMs;
    }

    @Override
    public boolean isActiveRule() {
        return false;
    }

    @Override
    public SomeDevice getTargetDevice() {
        return new OWON_SPE3051();
    }

    @Override
    public void setWaitingForAnswer(boolean state) {
        this.isWaitingForAnswer = state;
    }

    @Override
    public boolean isWaitingForAnswer() {
        return this.isWaitingForAnswer;
    }

    @Override
    public boolean isTimeForAction() {
        long now = System.currentTimeMillis();
        long delta = now - millisPrev;


        System.out.println("delayMs: " + delayMs);
        System.out.println("delta: " + delta);
        System.out.println("now: " + now);
        System.out.println("Is time for action: " + (delta > delayMs));

        boolean result = delta > delayMs;
        if(result){
            millisPrev = System.currentTimeMillis();
        }

        return result;
    }

    public float getEndVoltage(){
        return endVoltage;
    }
    // Остальные методы интерфейса...
}