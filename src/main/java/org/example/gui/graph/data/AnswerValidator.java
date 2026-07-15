package org.example.gui.graph.data;

import lombok.extern.slf4j.Slf4j;
import org.example.services.DeviceAnswer;

@Slf4j
public class AnswerValidator {
    private final AnswerLoader al = new AnswerLoader();
    public boolean isCorrectAnswerValue(DeviceAnswer answer, int tab, int typicalFieldCapacity, int currentFieldCapacity) {
        if (answer == null) {
            log.debug("Answer is null");
            return false;
        }

        if (answer.getAnswerReceivedValues() == null) {
            log.info("AnswerReceivedValues is null");
            return false;
        }

        if (answer.getAnswerReceivedValues().getValues() == null) {
            log.info("AnswerReceivedValues array of values is null");
            return false;
        }
        if(currentFieldCapacity == 0){
            log.warn("Передан typicalFieldCapacity == 0");
            currentFieldCapacity = answer.getAnswerReceivedValues().getValues().length;
        }

        if(typicalFieldCapacity == 0){
            log.warn("Передан currentFieldCapacity == 0");
            typicalFieldCapacity = al.getUnitsArrayForSelectedClientOrTab(tab).size();
        }

        if (currentFieldCapacity != typicalFieldCapacity) {
            log.info("typicalFieldCapacity not equal to currentFieldCapacity " + currentFieldCapacity + " != " + typicalFieldCapacity);
            return false;
        }
        return true;
    }
}
