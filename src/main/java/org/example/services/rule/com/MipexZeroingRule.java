package org.example.services.rule.com;

import org.example.device.SomeDevice;

import java.io.Serializable;
import java.util.Map;

public class MipexZeroingRule implements ComRule {
    @Override
    public String generateCommand() {
        return "ZERO2";
    }

    @Override
    public void processResponse(byte[] response) {

    }

    @Override
    public void reset() {

    }

    @Override
    public String getRuleId() {
        return "";
    }

    @Override
    public RuleType getRuleType() {
        return null;
    }


    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public void updateState() {

    }

    @Override
    public long getNextPoolDelay() {
        return 0;
    }

    @Override
    public boolean isActiveRule() {
        return false;
    }

    @Override
    public SomeDevice getTargetDevice() {
        return null;
    }

    @Override
    public void setWaitingForAnswer(boolean state) {

    }

    @Override
    public boolean isWaitingForAnswer() {
        return false;
    }

    @Override
    public boolean isTimeForAction() {
        return false;
    }
}
