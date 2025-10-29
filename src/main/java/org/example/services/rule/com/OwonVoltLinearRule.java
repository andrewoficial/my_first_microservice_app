package org.example.services.rule.com;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.SomeDevice;
import org.example.device.protOwonSpe3051.OWON_SPE3051;
import org.example.services.connectionPool.ComDataCollector;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class OwonVoltLinearRule implements ComRule {
    private final static Logger log = Logger.getLogger(OwonVoltLinearRule.class); // Объект логера
    @Getter
    private final String ruleId;

    private final float startVoltage;
    @Getter
    private final float endVoltage;
    private final float step;
    private final long delayMs;
    private float currentVoltage;
    private boolean isWaitingForAnswer;
    private long millisPrev;
    private final String ruleDescription;
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
        log.info("Created rule with id: " + ruleId);
    }
    
    @Override
    public String generateCommand() {
        log.info("Run cmd generation ");
        StringBuilder sb = new StringBuilder();
        sb.append("VOLT ");
        log.info("current voltage " + currentVoltage);
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


        //log.info("delayMs: " + delayMs);
        //log.info("delta: " + delta);
        //log.info("now: " + now);
        //log.info("Is time for action: " + (delta > delayMs));

        boolean result = delta > delayMs;
        if(result){
            millisPrev = System.currentTimeMillis();
        }

        return result;
    }

}