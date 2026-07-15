package org.example.services;

import org.jfree.data.time.Millisecond;

import java.time.ZoneId;
import java.util.Date;

public final class GraphPoint {

    private final long epochMilli;
    private final double[] values;

    public GraphPoint(long epochMilli, double[] values) {
        this.epochMilli = epochMilli;
        this.values = values;
    }

    public static GraphPoint from(DeviceAnswer answer) {
        int fieldCount = answer.getFieldCount();
        double[] vals = new double[fieldCount];
        AnswerValues av = answer.getAnswerReceivedValues();
        double[] avValues = av.getValues();
        for (int i = 0; i < fieldCount; i++) {
            vals[i] = avValues[i];
        }
        long millis = answer.getAnswerReceivedTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return new GraphPoint(millis, vals);
    }

    static GraphPoint mergePair(GraphPoint a, GraphPoint b) {
        long time = (a.epochMilli + b.epochMilli) / 2;
        double[] merged = new double[a.values.length];
        for (int i = 0; i < merged.length; i++) {
            merged[i] = (a.values[i] + b.values[i]) / 2.0;
        }
        return new GraphPoint(time, merged);
    }

    public long getEpochMilli() {
        return epochMilli;
    }

    public Double getValue(int fieldIndex) {
        if (fieldIndex < 0 || fieldIndex >= values.length) return null;
        return values[fieldIndex];
    }

    public int getFieldCount() {
        return values.length;
    }

    public Millisecond toJFreeMillisecond() {
        return new Millisecond(new Date(epochMilli));
    }
}
