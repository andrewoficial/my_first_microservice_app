package org.example.device.protAcu10fd;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Последние ответы прибора. Одна строка: {@code ЧЧ:ММ:СС: текст}.
 * GUI и консоль используют один и тот же журнал.
 */
public final class AcuStatusLog {

    public static final int CAPACITY = 15;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Clock clock;
    private final Deque<String> lines = new ArrayDeque<>();

    public AcuStatusLog() {
        this(Clock.systemDefaultZone());
    }

    public AcuStatusLog(Clock clock) {
        this.clock = clock;
    }

    public synchronized String append(String answer) {
        String text = answer == null ? "" : answer.replace('\r', ' ').replace('\n', ' ').trim();
        String stamp = LocalDateTime.now(clock).format(TIME);
        lines.addLast(stamp + ": " + text);
        while (lines.size() > CAPACITY) {
            lines.removeFirst();
        }
        return render();
    }

    public synchronized void clear() {
        lines.clear();
    }

    public synchronized List<String> lines() {
        return new ArrayList<>(lines);
    }

    public synchronized String render() {
        return String.join("\n", lines);
    }
}
