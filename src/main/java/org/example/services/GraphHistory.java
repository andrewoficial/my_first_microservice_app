package org.example.services;

import java.util.ArrayList;
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

    synchronized void forEachPoint(Consumer<GraphPoint> consumer) {
        for (GraphPoint[] chunk : merged) {
            for (GraphPoint point : chunk) {
                consumer.accept(point);
            }
        }
        for (GraphPoint point : tail) {
            consumer.accept(point);
        }
    }
}
