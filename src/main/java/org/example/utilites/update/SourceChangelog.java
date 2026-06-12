package org.example.utilites.update;

import java.util.Collections;
import java.util.List;

/**
 * Результат проверки одного источника: список всех релизов, которые новее текущей версии.
 * Используется для отображения полного "что нового" по промежуточным версиям.
 */
public final class SourceChangelog {
    public final String sourceName;
    public final List<Release> newerReleases; // от самой новой к более старым (как обычно отдаёт GitHub)
    public final String error;

    public SourceChangelog(String sourceName, List<Release> newerReleases, String error) {
        this.sourceName = sourceName != null ? sourceName : "Unknown";
        this.newerReleases = newerReleases != null ? newerReleases : Collections.emptyList();
        this.error = error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }

    public boolean hasNewer() {
        return !hasError() && !newerReleases.isEmpty();
    }

    public String getLatestVersion() {
        return hasNewer() ? newerReleases.get(0).version : "0.0.0";
    }

    /** Склеенные заметки всех промежуточных версий с заголовками. Удобно для вывода в UI. */
    public String getCombinedNotes() {
        if (newerReleases.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (Release r : newerReleases) {
            sb.append("=== Версия ").append(r.version).append(" ===\n");
            String n = r.notes.trim();
            if (!n.isEmpty()) {
                sb.append(n).append("\n\n");
            }
        }
        return sb.toString().trim();
    }
}
