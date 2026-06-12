package org.example.utilites.update;

/**
 * Минимальная модель одного релиза (версия из tag_name, текст изменений, прямая ссылка на скачивание).
 * Используется для всех источников, которые эмулируют GitHub Releases API.
 */
public final class Release {
    public final String version;
    public final String notes;
    public final String downloadUrl;

    public Release(String version, String notes, String downloadUrl) {
        this.version = version != null ? version : "";
        this.notes = notes != null ? notes : "";
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
    }

    public boolean hasDownload() {
        return !downloadUrl.isEmpty();
    }
}
