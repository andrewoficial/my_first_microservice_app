package org.example.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GraphDataRepository {

    private static final GraphDataRepository INSTANCE = new GraphDataRepository();

    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, GraphHistory>> histories = new ConcurrentHashMap<>();

    public static GraphDataRepository getInstance() {
        return INSTANCE;
    }

    public void addData(Integer tabId, String command, DeviceAnswer answer) {
        if (tabId == null || command == null || answer == null) return;
        if (answer.getFieldCount() == 0) return;

        histories.computeIfAbsent(tabId, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(command, k -> new GraphHistory())
                .add(GraphPoint.from(answer));
    }

    public void forEachHistoryPoint(Integer tabId, String command, Consumer<GraphPoint> consumer) {
        if (tabId == null || command == null || consumer == null) return;
        ConcurrentHashMap<String, GraphHistory> cmdHistories = histories.get(tabId);
        if (cmdHistories == null) return;
        GraphHistory history = cmdHistories.get(command);
        if (history != null) {
            history.forEachPoint(consumer);
        }
    }

    public int getTotalSampleCount(Integer tabId, String command) {
        if (tabId == null || command == null) return 0;
        ConcurrentHashMap<String, GraphHistory> cmdHistories = histories.get(tabId);
        if (cmdHistories == null) return 0;
        GraphHistory history = cmdHistories.get(command);
        return history != null ? history.getTotalSampleCount() : 0;
    }

    public void clear(Integer tabId) {
        if (tabId == null) return;
        ConcurrentHashMap<String, GraphHistory> cmdHistories = histories.remove(tabId);
        if (cmdHistories != null) {
            cmdHistories.values().forEach(GraphHistory::clear);
        }
    }

    public void clearCommand(Integer tabId, String command) {
        if (tabId == null || command == null) return;
        ConcurrentHashMap<String, GraphHistory> cmdHistories = histories.get(tabId);
        if (cmdHistories != null) {
            GraphHistory history = cmdHistories.remove(command);
            if (history != null) {
                history.clear();
            }
        }
    }

    public void clearAll() {
        histories.values().forEach(cmdHistories ->
                cmdHistories.values().forEach(GraphHistory::clear));
        histories.clear();
    }
}
