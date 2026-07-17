package org.example.utilites;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.utilites.update.*;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Координатор обновлений программы.
 * <p>
 * Источники:
 * <ol>
 *   <li>Встроенный GitHub ({@code GITHUB_OLD} / при необходимости {@code GITHUB_NEW} — не удалять)</li>
 *   <li>Встроенный GitFlic (зеркало релизов)</li>
 *   <li>{@code updateSourceUrl} из конфига — опциональный источник заказчика
 *       (локальная сеть / enterprise mirror при отсутствии интернета).
 *       Пока ожидается GitHub-compatible JSON; в будущем — выбор парсера
 *       (gitLike / jsonLike / …).</li>
 * </ol>
 * GitHub — Releases API; GitFlic — API с токеном.
 */
@Slf4j
public class ProgramUpdater {

    // Встроенные источники. GITHUB_NEW можно заполнить позже — не удалять и не заменять.
    private static final String GITHUB_OLD_LIST = "https://api.github.com/repos/andrewoficial/my_first_microservice_app/releases?per_page=100";
    private static final String GITHUB_NEW_LIST = ""; // заполни /releases?per_page=100 когда будет новый реп

    /** Встроенное зеркало релизов на GitFlic. */
    private static final String GITFLIC_LIST =
            "https://gitflic.ru/project/andrewkantser/elephant-monitor/release?sort=TIME&direction=DESC";

    private static final Path UPDATE_DIR = Paths.get("").toAbsolutePath();

    // Для обратной совместимости со старым getInfo()
    private String lastNotes = "";

    @Getter
    private boolean busy = false;
    @Getter
    private volatile int updatePercents = 0;

    /**
     * Собирает активные источники: встроенные (GitHub, GitFlic) + опциональный из конфига.
     * {@code updateSourceUrl} — только для LAN/enterprise mirror заказчика; пустой = не используется.
     * Дубликаты URL не добавляются.
     */
    public java.util.List<UpdateSource> getActiveSources() {
        java.util.List<UpdateSource> sources = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();

        if (!GITHUB_OLD_LIST.isBlank()) {
            addSource(sources, seen, "GitHub", GITHUB_OLD_LIST);
        }
        if (!GITHUB_NEW_LIST.isBlank()) {
            addSource(sources, seen, "Новый GitHub", GITHUB_NEW_LIST);
        }
        if (!GITFLIC_LIST.isBlank()) {
            addSource(sources, seen, "GitFlic", GITFLIC_LIST);
        }

        // Локальный / enterprise источник заказчика (не подставляем дефолт — это осознанная настройка)
        try {
            String fromConfig = org.example.utilites.properties.MyProperties.getInstance() != null
                    ? org.example.utilites.properties.MyProperties.getInstance().getUpdateSourceUrl()
                    : "";
            if (fromConfig != null && !fromConfig.isBlank()) {
                String listUrl = toListUrl(fromConfig.trim());
                // Имя: GitFlic, если вдруг указали gitflic; иначе «Локальный» (LAN mirror)
                String name = GitFlicReleaseClient.isGitFlicUrl(listUrl) ? "GitFlic (конфиг)" : "Локальный";
                addSource(sources, seen, name, listUrl);
            }
        } catch (Exception ignored) {
            // MyProperties может быть ещё не готов (некоторые тесты и т.д.)
        }

        return sources;
    }

    private void addSource(java.util.List<UpdateSource> sources, java.util.Set<String> seen,
                           String name, String url) {
        if (url == null || url.isBlank()) return;
        String key = url.trim().toLowerCase();
        // для сравнения без query-шума
        int q = key.indexOf('?');
        if (q >= 0) key = key.substring(0, q);
        if (!seen.add(key)) return;
        sources.add(new UpdateSource(name, url.trim()));
    }

    /** Нормализует URL списка релизов (GitHub API или GitFlic). */
    private String toListUrl(String input) {
        if (input == null || input.isBlank()) return "";
        if (GitFlicReleaseClient.isGitFlicUrl(input)) {
            return GitFlicReleaseClient.toListUrl(input);
        }
        String u = input.trim();
        if (u.contains("/releases/latest")) {
            u = u.replace("/releases/latest", "/releases");
        } else if (u.endsWith("/latest")) {
            u = u.substring(0, u.length() - 7) + "/releases";
        }
        if (!u.contains("per_page=")) {
            u += (u.contains("?") ? "&" : "?") + "per_page=100";
        }
        return u;
    }

    /** Загрузка и разбор релизов с учётом типа источника. */
    private java.util.List<Release> loadReleases(String listUrl) throws Exception {
        if (GitFlicReleaseClient.isGitFlicUrl(listUrl)) {
            return GitFlicReleaseClient.fetchReleases(listUrl);
        }
        JSONArray arr = fetchReleases(listUrl);
        return ReleaseParser.parseReleases(arr);
    }

    /** Лёгкий fetch одного релиза (для простых случаев). */
    private JSONObject fetchJson(String url) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(5000);
        con.setReadTimeout(8000);

        if (con.getResponseCode() != 200) {
            throw new IOException("HTTP " + con.getResponseCode());
        }

        try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return new JSONObject(sb.toString());
        }
    }

    /** Основной fetch списка релизов. */
    private JSONArray fetchReleases(String listUrl) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(listUrl).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(6000);
        con.setReadTimeout(10000);

        if (con.getResponseCode() != 200) {
            throw new IOException("HTTP " + con.getResponseCode() + " from " + listUrl);
        }

        try (BufferedReader r = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            return new JSONArray(sb.toString());
        }
    }

    /** Простая проверка "только последняя версия по каждому источнику" (для лёгких случаев). */
    public java.util.List<UpdateSourceResult> checkAllSources() {
        java.util.List<UpdateSourceResult> out = new java.util.ArrayList<>();
        for (UpdateSource src : getActiveSources()) {
            try {
                java.util.List<Release> all = loadReleases(src.listUrl);
                Release latest = all.isEmpty() ? null : all.get(0);
                if (latest == null) {
                    out.add(new UpdateSourceResult(src.name, "0.0.0", "", "", null));
                    continue;
                }
                out.add(new UpdateSourceResult(src.name, latest.version, latest.notes, latest.downloadUrl, null));
            } catch (Exception e) {
                out.add(new UpdateSourceResult(src.name, "0.0.0", "", "", e.getMessage()));
            }
        }
        return out;
    }

    /**
     * Основной метод: возвращает по каждому источнику все релизы, которые новее текущей версии.
     * Используется для красивого вывода полного changelog по промежуточным версиям.
     */
    public java.util.List<SourceChangelog> getChangelogsSince(String currentVersion) {
        java.util.List<SourceChangelog> result = new java.util.ArrayList<>();

        for (UpdateSource src : getActiveSources()) {
            try {
                java.util.List<Release> parsed = loadReleases(src.listUrl);

                java.util.List<Release> newer = new java.util.ArrayList<>();
                for (Release r : parsed) {
                    if (!r.version.isEmpty()) {
                        try {
                            if (isAvailableNewVersion(r.version, currentVersion)) {
                                newer.add(r);
                            }
                        } catch (Exception ignored) {}
                    }
                }

                result.add(new SourceChangelog(src.name, newer, null));
                if (!newer.isEmpty()) {
                    lastNotes = newer.get(0).notes; // для старого getInfo()
                }
            } catch (Exception e) {
                result.add(new SourceChangelog(src.name, java.util.Collections.emptyList(), e.getMessage()));
            }
        }
        return result;
    }

    public boolean isAvailableNewVersion(String foundVersion, String currentVersion) {
        // Отладочное сообщение
        log.info("Проверка доступности новой версии.");
        log.debug("Текущая версия: " + currentVersion);
        log.debug("Найденная версия: " + foundVersion);

        // Проверка на null
        if (currentVersion == null || foundVersion == null) {
            log.debug("Одна из строк версии null.");
            return false;
        }

        // Удаление префикса 'v', если он есть
        if (currentVersion.startsWith("v")) {
            currentVersion = currentVersion.substring(1);
        }
        if (foundVersion.startsWith("v")) {
            foundVersion = foundVersion.substring(1);
        }

        // Проверка на формат (наличие двух точек)
        if (!currentVersion.matches("\\d+\\.\\d+\\.\\d+(-\\w+)?") ||
                !foundVersion.matches("\\d+\\.\\d+\\.\\d+(-\\w+)?")) {
            log.debug("Формат версии неверный.");
            return false;
        }

        // Разбиение строк на основные части
        String[] currentParts = currentVersion.split("[-.]");
        String[] foundParts = foundVersion.split("[-.]");

        // Отладочное сообщение
        log.debug("Разделенная текущая версия: " + String.join(", ", currentParts));
        log.debug("Разделенная найденная версия: " + String.join(", ", foundParts));

        // Сравнение по числовым частям версии
        for (int i = 0; i < 3; i++) {
            int currentNum = Integer.parseInt(currentParts[i]);
            int foundNum = Integer.parseInt(foundParts[i]);

            if (foundNum > currentNum) {
                log.info("Найдена новая версия.");
                return true;
            } else if (foundNum < currentNum) {
                log.info("Новая версия недоступна, текущая версия новее.");
                return false;
            }
        }

        // Если основные версии равны, проверяем суффиксы (если они есть)
        if (currentParts.length > 3 || foundParts.length > 3) {
            String currentSuffix = (currentParts.length > 3) ? currentParts[3] : "";
            String foundSuffix = (foundParts.length > 3) ? foundParts[3] : "";

            // Отладочное сообщение
            log.info("Суффиксы: текущий - " + currentSuffix + ", найденный - " + foundSuffix);

            if (!currentSuffix.isEmpty() && foundSuffix.isEmpty()) {
                log.info("Найдена стабильная версия.");
                return true;
            } else if (foundSuffix.compareTo(currentSuffix) < 0) {
                log.info("Найдена новая версия без суффикса.");
                return true;
            }
        }

        log.info("Версии совпадают.");
        return false;
    }

    public void downloadUpdate() throws IOException, InterruptedException {
        // Берём первый валидный источник как primary
        for (UpdateSource src : getActiveSources()) {
            try {
                java.util.List<Release> parsed = loadReleases(src.listUrl);
                if (!parsed.isEmpty() && parsed.get(0).hasDownload()) {
                    Release r = parsed.get(0);
                    performDownload(r.downloadUrl, r.resolveFileName());
                    return;
                }
            } catch (Exception ignored) {}
        }
        throw new IOException("Не удалось найти подходящий источник для скачивания");
    }

    /**
     * Прямое скачивание по известной ссылке (используется после getChangelogsSince).
     */
    public void downloadFromDirectUrl(String directDownloadUrl, String suggestedFileName) throws IOException, InterruptedException {
        updatePercents = 0;
        this.busy = true;
        try {
            performDownload(directDownloadUrl, suggestedFileName);
        } finally {
            busy = false;
        }
    }

    private void performDownload(String downloadUrl, String suggestedFileName) throws IOException {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            throw new IOException("Пустая ссылка на скачивание");
        }

        String fileName = suggestedFileName;
        if (fileName == null || fileName.isBlank() || "download".equalsIgnoreCase(fileName)) {
            try {
                fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
                int q = fileName.indexOf('?');
                if (q >= 0) fileName = fileName.substring(0, q);
            } catch (Exception e) {
                fileName = "Elephant-Monitor-update.jar";
            }
            if (fileName.isBlank() || "download".equalsIgnoreCase(fileName)) {
                fileName = "Elephant-Monitor-update.jar";
            }
        }

        log.info("Загрузка обновления: " + fileName);
        Path targetDir = UPDATE_DIR;
        Files.createDirectories(targetDir);
        Path target = targetDir.resolve(fileName);

        this.busy = true;
        updatePercents = 0;

        HttpURLConnection downloadCon = (HttpURLConnection) new URL(downloadUrl).openConnection();
        downloadCon.setRequestMethod("GET");
        downloadCon.setConnectTimeout(10000);
        downloadCon.setReadTimeout(120000);
        downloadCon.setInstanceFollowRedirects(true);
        downloadCon.setRequestProperty("User-Agent", "ElephantMonitor-Updater/1.0");
        if (GitFlicReleaseClient.isGitFlicUrl(downloadUrl)) {
            GitFlicReleaseClient.applyAuth(downloadCon);
        }
        // GitFlic отдаёт Content-Disposition; при имени "download" попробуем взять оттуда
        String cd = downloadCon.getHeaderField("Content-Disposition");
        if (cd != null && ("download".equalsIgnoreCase(fileName) || "Elephant-Monitor-update.jar".equals(fileName))) {
            String fromCd = extractFileNameFromContentDisposition(cd);
            if (fromCd != null && !fromCd.isBlank()) {
                fileName = fromCd;
                target = targetDir.resolve(fileName);
            }
        }
        if (downloadCon.getResponseCode() / 100 != 2) {
            int code = downloadCon.getResponseCode();
            downloadCon.disconnect();
            throw new IOException("HTTP " + code + " при скачивании " + downloadUrl);
        }

        try (InputStream is = downloadCon.getInputStream()) {
            long total = downloadCon.getContentLengthLong();
            if (total <= 0) total = getContentLength(downloadUrl);
            long done = 0;
            byte[] buf = new byte[8192];
            int read;
            long lastReport = System.currentTimeMillis();

            try (OutputStream out = Files.newOutputStream(target)) {
                while ((read = is.read(buf)) != -1) {
                    out.write(buf, 0, read);
                    done += read;

                    long now = System.currentTimeMillis();
                    if (now - lastReport >= 150) {
                        updatePercents = total > 0 ? (int) ((done * 100.0) / total) : 0;
                        System.out.printf("Загружено: %.2f / %.2f МБ (%d%%)%n",
                                done / 1048576.0, total / 1048576.0, updatePercents);
                        lastReport = now;
                    }
                }
            }
            log.info("Файл успешно сохранён: " + target);
        } catch (IOException e) {
            updatePercents = 0;
            log.warn("Ошибка загрузки: " + e.getMessage());
            throw e;
        } finally {
            downloadCon.disconnect();
            busy = false;
        }
    }

    private long getContentLength(String url) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("HEAD");
            if (GitFlicReleaseClient.isGitFlicUrl(url)) {
                GitFlicReleaseClient.applyAuth(c);
            }
            long len = c.getContentLengthLong();
            c.disconnect();
            return len > 0 ? len : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // === Legacy / совместимость ===

    public String getLatestVersion() {
        try {
            for (UpdateSource src : getActiveSources()) {
                java.util.List<Release> parsed = loadReleases(src.listUrl);
                if (!parsed.isEmpty()) {
                    lastNotes = parsed.get(0).notes;
                    return parsed.get(0).version;
                }
            }
        } catch (Exception e) {
            log.warn("getLatestVersion failed: " + e.getMessage());
        }
        return "0.0.0";
    }

    public String getInfo() {
        return lastNotes;
    }

    private static String extractFileNameFromContentDisposition(String cd) {
        if (cd == null) return null;
        // filename*=UTF-8''Elephant-Monitor-1.8.49.jar  или filename="..."
        int star = cd.toLowerCase().indexOf("filename*=");
        if (star >= 0) {
            String part = cd.substring(star + "filename*=".length()).trim();
            int semi = part.indexOf(';');
            if (semi >= 0) part = part.substring(0, semi).trim();
            int tick = part.lastIndexOf("''");
            if (tick >= 0) part = part.substring(tick + 2);
            part = part.replace("\"", "").trim();
            if (!part.isEmpty()) return part;
        }
        int fn = cd.toLowerCase().indexOf("filename=");
        if (fn >= 0) {
            String part = cd.substring(fn + "filename=".length()).trim();
            int semi = part.indexOf(';');
            if (semi >= 0) part = part.substring(0, semi).trim();
            part = part.replace("\"", "").trim();
            if (!part.isEmpty()) return part;
        }
        return null;
    }
}

