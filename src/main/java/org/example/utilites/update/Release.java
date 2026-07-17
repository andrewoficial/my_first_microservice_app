package org.example.utilites.update;

/**
 * Минимальная модель одного релиза (версия из tag_name, текст изменений, прямая ссылка на скачивание).
 * Используется для GitHub Releases API и GitFlic.
 */
public final class Release {
    public final String version;
    public final String notes;
    public final String downloadUrl;
    /** Имя файла для сохранения (если известно; иначе выводится из URL). */
    public final String fileName;

    public Release(String version, String notes, String downloadUrl) {
        this(version, notes, downloadUrl, "");
    }

    public Release(String version, String notes, String downloadUrl, String fileName) {
        this.version = version != null ? version : "";
        this.notes = notes != null ? notes : "";
        this.downloadUrl = downloadUrl != null ? downloadUrl : "";
        this.fileName = fileName != null ? fileName : "";
    }

    public boolean hasDownload() {
        return !downloadUrl.isEmpty();
    }

    /** Имя файла: явное, либо последний сегмент URL, либо fallback по версии. */
    public String resolveFileName() {
        if (fileName != null && !fileName.isBlank() && !"download".equalsIgnoreCase(fileName)) {
            return fileName.trim();
        }
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            String last = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            int q = last.indexOf('?');
            if (q >= 0) last = last.substring(0, q);
            if (!last.isBlank() && !"download".equalsIgnoreCase(last)) {
                return last;
            }
        }
        if (version != null && !version.isBlank()) {
            return "Elephant-Monitor-" + version + ".jar";
        }
        return "Elephant-Monitor-update.jar";
    }
}
