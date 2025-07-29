package org.example.gui.graph.data;

import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

public class AnswerLoader {

    public ArrayList<String> getUnitsArrayForTab(int tab){
        ArrayList<String> unitsInAnswer = new ArrayList<>();
        int index = AnswerStorage.getAnswersForGraph(tab).size() - 1;
        index = Math.max(0, index);
        DeviceAnswer selectedAnswer = AnswerStorage.getAnswersForGraph(tab).get(index);


        if (Objects.equals(selectedAnswer.getClientId(), tab)) {
            if (tab == 0 && selectedAnswer.getFieldCount() == 0) {
                unitsInAnswer.add(" ");
                return unitsInAnswer;
            }
        }

        if (selectedAnswer.getFieldCount() > 0) {
            AnswerValues values = selectedAnswer.getAnswerReceivedValues();
            unitsInAnswer.addAll(Arrays.asList(values.getUnits()));
        }

        return unitsInAnswer;
    }
}
