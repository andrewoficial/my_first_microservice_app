package org.example.gui.graph.data;

import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AnswerLoader {

    private final ConcurrentHashMap<Integer, ArrayList<String>> cache = new ConcurrentHashMap<>();

    public ArrayList<String> getUnitsArrayForSelectedClientOrTab(int tab) {
        return cache.computeIfAbsent(tab, this::loadUnitsForTab);
    }

    private ArrayList<String> loadUnitsForTab(int tab) {
        DeviceAnswer selectedAnswer = AnswerStorage.getLastAnswerForTab(tab);
        if (selectedAnswer == null) {
            return new ArrayList<>();
        }

        if (Objects.equals(selectedAnswer.getClientId(), tab)) {
            if (tab == 0 && selectedAnswer.getFieldCount() == 0) {
                ArrayList<String> single = new ArrayList<>();
                single.add(" ");
                return single;
            }
        }

        ArrayList<String> unitsInAnswer = new ArrayList<>();
        if (selectedAnswer.getFieldCount() > 0) {
            AnswerValues values = selectedAnswer.getAnswerReceivedValues();
            unitsInAnswer.addAll(Arrays.asList(values.getUnits()));
        }

        return unitsInAnswer;
    }

    public void invalidateCache(int tab) {
        cache.remove(tab);
    }

    public void invalidateAll() {
        cache.clear();
    }
}
