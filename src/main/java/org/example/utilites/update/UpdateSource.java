package org.example.utilites.update;

/**
 * Описание одного источника обновлений.
 * listUrl должен возвращать массив релизов в GitHub-формате (или эмуляцию).
 */
public final class UpdateSource {
    public final String name;
    public final String listUrl;   // URL списка релизов (с ?per_page=100 желательно)

    public UpdateSource(String name, String listUrl) {
        this.name = name != null ? name : "Unknown";
        this.listUrl = listUrl != null ? listUrl : "";
    }

    public boolean isValid() {
        return !listUrl.isEmpty();
    }
}
