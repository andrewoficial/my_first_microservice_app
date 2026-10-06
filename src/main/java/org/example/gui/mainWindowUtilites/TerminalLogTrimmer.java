package org.example.gui.mainWindowUtilites;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Срезает журнал терминала по границе записи.
 * Запись начинается со штампа {@code yyyy.MM.dd HH:mm:ss.SSS}.
 */
public final class TerminalLogTrimmer {

    public static final int OLDEST_RECORDS_TO_DROP = 20;
    private static final Pattern STAMP = Pattern.compile(
            "(?m)^\\d{4}\\.\\d{2}\\.\\d{2} \\d{2}:\\d{2}:\\d{2}\\.\\d{3}");

    private TerminalLogTrimmer() {
    }

    /**
     * Удаляет {@code recordsToDrop} самых старых записей со штампом.
     * Текст до первого штампа уходит вместе с ними.
     * Если штампов меньше, чем нужно оставить одну последнюю запись, остаётся она.
     * Без единого штампа текст очищается целиком: границы записи нет.
     */
    public static String dropOldest(String text, int recordsToDrop) {
        if (text == null || text.isEmpty() || recordsToDrop <= 0) {
            return text == null ? "" : text;
        }
        List<Integer> starts = stampStarts(text);
        if (starts.isEmpty()) {
            return "";
        }
        int cutIndex = recordsToDrop;
        if (cutIndex >= starts.size()) {
            cutIndex = starts.size() - 1;
        }
        if (cutIndex <= 0) {
            return text;
        }
        return text.substring(starts.get(cutIndex));
    }

    /**
     * Пока текст вместе с новой порцией длиннее {@code maxLength}, снимает пачки старых записей.
     */
    public static String fit(String text, int incomingLength, int maxLength, int recordsToDrop) {
        String current = text == null ? "" : text;
        int guard = 0;
        while (current.length() + incomingLength > maxLength && !current.isEmpty() && guard < 1000) {
            String next = dropOldest(current, recordsToDrop);
            if (next.length() >= current.length()) {
                return "";
            }
            current = next;
            guard++;
        }
        return current;
    }

    private static List<Integer> stampStarts(String text) {
        List<Integer> starts = new ArrayList<>();
        Matcher matcher = STAMP.matcher(text);
        while (matcher.find()) {
            starts.add(matcher.start());
        }
        return starts;
    }
}
