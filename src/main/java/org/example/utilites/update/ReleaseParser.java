package org.example.utilites.update;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Парсер для GitHub-style Releases API (и его эмуляций).
 * Ожидает объекты с полями:
 *   - tag_name
 *   - body (или notes)
 *   - assets[0].browser_download_url
 *   - assets[0].name (опционально)
 */
public final class ReleaseParser {

    private ReleaseParser() {}

    /**
     * Парсит массив релизов (обычно ответ на /releases).
     */
    public static List<Release> parseReleases(JSONArray array) {
        if (array == null) {
            return Collections.emptyList();
        }
        List<Release> result = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.optJSONObject(i);
            if (obj != null) {
                Release release = parseSingle(obj);
                if (!release.version.isEmpty()) {
                    result.add(release);
                }
            }
        }
        return result;
    }

    /**
     * Парсит один релиз (объект из массива или ответ /latest).
     */
    public static Release parseSingle(JSONObject obj) {
        if (obj == null) {
            return new Release("", "", "");
        }

        String version = obj.optString("tag_name", obj.optString("version", ""));
        String notes = obj.optString("body", obj.optString("notes", ""));

        String downloadUrl = "";
        String fileName = "";

        JSONArray assets = obj.optJSONArray("assets");
        if (assets != null && assets.length() > 0) {
            JSONObject first = assets.optJSONObject(0);
            if (first != null) {
                downloadUrl = first.optString("browser_download_url", first.optString("download_url", ""));
                fileName = first.optString("name", "");
            }
        }

        // Если downloadUrl пустой, попробуем другие распространённые поля
        if (downloadUrl.isEmpty()) {
            downloadUrl = obj.optString("browser_download_url", obj.optString("download_url", ""));
        }

        return new Release(version, notes, downloadUrl);
    }
}
