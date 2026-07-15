package org.example.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class GraphDataRepository {

    private static final GraphDataRepository INSTANCE = new GraphDataRepository();

    private final ConcurrentHashMap<Integer, GraphHistory> histories = new ConcurrentHashMap<>();

    public static GraphDataRepository getInstance() {
        return INSTANCE;
    }

    public void addData(Integer tabId, DeviceAnswer answer) {
        if (tabId == null || answer == null) return;
        if (answer.getFieldCount() == 0) return;

        GraphHistory history = histories.computeIfAbsent(tabId, k -> new GraphHistory());
        history.add(GraphPoint.from(answer));
    }

    public void forEachHistoryPoint(Integer tabId, Consumer<GraphPoint> consumer) {
        if (tabId == null || consumer == null) return;
        GraphHistory history = histories.get(tabId);
        if (history != null) {
            history.forEachPoint(consumer);
        }
    }

    public int getTotalSampleCount(Integer tabId) {
        if (tabId == null) return 0;
        GraphHistory history = histories.get(tabId);
        return history != null ? history.getTotalSampleCount() : 0;
    }

    public void clear(Integer tabId) {
        if (tabId == null) return;
        GraphHistory history = histories.get(tabId);
        if (history != null) {
            history.clear();
        }
    }
}
