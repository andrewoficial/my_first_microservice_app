package org.example.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

final class GraphHistory {

    private static final int FULL_RES_SAMPLES = 18000;
    private static final int DECIMATE_BATCH = 2000;
    private static final int DECIMATE_OUT = 1000;

    private final ArrayList<GraphPoint[]> merged = new ArrayList<>();
    private final ArrayList<GraphPoint> tail = new ArrayList<>();
    private int totalSampleCount;

    synchronized void add(GraphPoint point) {
        tail.add(point);
        totalSampleCount++;
        decimateWhileNeeded();
    }

    private void decimateWhileNeeded() {
        while (totalSampleCount > FULL_RES_SAMPLES && tail.size() >= DECIMATE_BATCH) {
            GraphPoint[] chunk = new GraphPoint[DECIMATE_OUT];
            for (int i = 0; i < DECIMATE_OUT; i++) {
                chunk[i] = GraphPoint.mergePair(tail.get(i * 2), tail.get(i * 2 + 1));
            }
            merged.add(chunk);
            tail.subList(0, DECIMATE_BATCH).clear();
            totalSampleCount -= (DECIMATE_BATCH - DECIMATE_OUT);
        }
    }

    synchronized void clear() {
        merged.clear();
        tail.clear();
        totalSampleCount = 0;
    }

    synchronized int getTotalSampleCount() {
        return totalSampleCount;
    }

    void forEachPoint(Consumer<GraphPoint> consumer) {
        List<GraphPoint[]> mergedSnapshot;
        List<GraphPoint> tailSnapshot;
        synchronized (this) {
            mergedSnapshot = List.copyOf(merged);
            tailSnapshot = List.copyOf(tail);
        }
        for (GraphPoint[] chunk : mergedSnapshot) {
            for (GraphPoint point : chunk) {
                consumer.accept(point);
            }
        }
        for (GraphPoint point : tailSnapshot) {
            consumer.accept(point);
        }
    }

    /**
     * Точки новее {@code sinceExclusive}. Если их больше {@code limit}, остаются самые новые.
     */
    java.util.List<GraphSample> pointsAfter(long sinceExclusive, int limit) {
        java.util.List<GraphSample> found = new java.util.ArrayList<>();
        forEachPoint(point -> {
            if (point.getEpochMilli() <= sinceExclusive) {
                return;
            }
            double[] values = new double[point.getFieldCount()];
            for (int i = 0; i < values.length; i++) {
                Double value = point.getValue(i);
                values[i] = value == null ? Double.NaN : value;
            }
            found.add(new GraphSample(point.getEpochMilli(), values));
        });
        if (limit > 0 && found.size() > limit) {
            return java.util.List.copyOf(found.subList(found.size() - limit, found.size()));
        }
        return found;
    }
}
