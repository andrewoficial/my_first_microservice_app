package org.example.utilites.update;

/**
 * Лёгкий результат для "только последняя доступная версия по источнику".
 * Используется checkAllSources() для быстрой проверки.
 */
public final class UpdateSourceResult {
    public final String sourceName;
    public final String version;
    public final String notes;
    public final String downloadUrl;
    public final String error;

    public UpdateSourceResult(String sourceName, String version, String notes, String downloadUrl, String error) {
        this.sourceName = sourceName;
        this.version = version != null ? version : "0.0.0";
        this.notes = notes != null ? notes : "";
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
        this.error = error;
    }

    public boolean hasError() {
        return error != null && !error.isEmpty();
    }
}
