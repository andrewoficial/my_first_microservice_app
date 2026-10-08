package org.example.utilites.update;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Проверка/загрузка обновлений лаунчера ({@code launcher.exe}).
 * <p>
 * Лаунчер написан на Pascal и публикуется отдельным релизом в GitHub-репозитории
 * {@code andrewoficial/elephant-monitor-launcher}. Текущая установленная версия
 * определяется по SHA-256 файла {@code launcher.exe}: GitHub отдаёт дайджест ассета,
 * поэтому сравнение хешей надёжнее разбора версий (теги бывают нечисловыми, напр. {@code publish}).
 * <p>
 * Файл лаунчера ищется в типовых местах:
 * <ol>
 *   <li>рядом с запущенным jar (или классами при разработке);</li>
 *   <li>в рабочей директории процесса;</li>
 *   <li>в типовой папке установки {@code C:\ElephantMonitor}.</li>
 * </ol>
 */
@Slf4j
public class LauncherUpdater {

    public static final String GITHUB_LIST =
            "https://api.github.com/repos/andrewoficial/elephant-monitor-launcher/releases?per_page=20";

    public static final String LAUNCHER_FILE_NAME = "launcher.exe";

    private static final String[] TYPICAL_DIRS = {"C:\\ElephantMonitor"};

    /** Описание доступного релиза лаунчера. */
    public static final class LauncherReleaseInfo {
        public final String version;
        public final String notes;
        public final String downloadUrl;
        public final String fileName;
        public final String digest;

        public LauncherReleaseInfo(String version, String notes, String downloadUrl,
                                   String fileName, String digest) {
            this.version = version != null ? version : "";
            this.notes = notes != null ? notes : "";
            this.downloadUrl = downloadUrl != null ? downloadUrl : "";
            this.fileName = fileName != null && !fileName.isBlank() ? fileName : LAUNCHER_FILE_NAME;
            this.digest = digest != null ? digest : "";
        }

        public boolean hasDownload() {
            return !downloadUrl.isBlank();
        }
    }

    /** Результат проверки обновления лаунчера. */
    public static final class CheckResult {
        public final boolean updateAvailable;
        public final boolean failed;
        public final String error;
        public final Path localLauncher;
        public final String localHash;
        public final LauncherReleaseInfo latest;

        private CheckResult(boolean updateAvailable, boolean failed, String error,
                            Path localLauncher, String localHash, LauncherReleaseInfo latest) {
            this.updateAvailable = updateAvailable;
            this.failed = failed;
            this.error = error;
            this.localLauncher = localLauncher;
            this.localHash = localHash;
            this.latest = latest;
        }

        static CheckResult failed(String error) {
            return new CheckResult(false, true, error, null, "", null);
        }
    }

    /**
     * Ищет установленный {@code launcher.exe} в типовых местах.
     *
     * @return путь к найденному лаунчеру либо {@code null}.
     */
    public Path findInstalledLauncher() {
        List<Path> candidates = new ArrayList<>();

        try {
            URL location = LauncherUpdater.class.getProtectionDomain().getCodeSource().getLocation();
            Path appPath = Paths.get(location.toURI());
            Path dir = Files.isDirectory(appPath) ? appPath : appPath.getParent();
            if (dir != null) {
                candidates.add(dir.resolve(LAUNCHER_FILE_NAME));
            }
        } catch (Exception ignored) {
            // во время разработки/на экзотических classloader location может быть недоступна
        }

        candidates.add(Paths.get(System.getProperty("user.dir", ".")).resolve(LAUNCHER_FILE_NAME));
        for (String typical : TYPICAL_DIRS) {
            candidates.add(Paths.get(typical, LAUNCHER_FILE_NAME));
        }

        for (Path candidate : candidates) {
            try {
                if (candidate != null && Files.isRegularFile(candidate)) {
                    return candidate.toAbsolutePath();
                }
            } catch (Exception ignored) {
                // пробуем следующий вариант
            }
        }
        return null;
    }

    /**
     * Тихая проверка: сравнивает локальный {@code launcher.exe} с последним релизом.
     * Никогда не бросает исключений — ошибки упакованы в {@link CheckResult#failed}.
     */
    public CheckResult checkForUpdate() {
        Path local = findInstalledLauncher();
        if (local == null) {
            return CheckResult.failed("Файл " + LAUNCHER_FILE_NAME + " не найден рядом с программой");
        }

        try {
            LauncherReleaseInfo latest = fetchLatest();
            if (latest == null) {
                return CheckResult.failed("В репозитории лаунчера нет подходящих релизов");
            }

            String localHash = sha256(local);
            // Дайджеста нет — сравнить не можем, считаем что обновления нет (тихо).
            boolean updateAvailable = !latest.digest.isBlank()
                    && !normalizeDigest(latest.digest).equalsIgnoreCase(localHash);

            return new CheckResult(updateAvailable, false, null, local, localHash, latest);
        } catch (Exception ex) {
            log.debug("Проверка обновления лаунчера не удалась: {}", ex.toString());
            return CheckResult.failed(ex.getMessage());
        }
    }

    /** Первый (самый свежий, т.к. API отдаёт DESC) релиз с ассетом {@code launcher.exe}. */
    private LauncherReleaseInfo fetchLatest() throws Exception {
        JSONArray releases = fetchArray(GITHUB_LIST);
        for (int i = 0; i < releases.length(); i++) {
            JSONObject release = releases.optJSONObject(i);
            if (release == null || release.optBoolean("draft", false)) {
                continue;
            }
            JSONArray assets = release.optJSONArray("assets");
            if (assets == null) {
                continue;
            }
            for (int j = 0; j < assets.length(); j++) {
                JSONObject asset = assets.optJSONObject(j);
                if (asset == null) {
                    continue;
                }
                String name = asset.optString("name", "");
                if (!LAUNCHER_FILE_NAME.equalsIgnoreCase(name)) {
                    continue;
                }
                String version = release.optString("name", "");
                if (version.isBlank()) {
                    version = release.optString("tag_name", "");
                }
                String url = asset.optString("browser_download_url", "");
                if (url.isBlank()) {
                    continue;
                }
                return new LauncherReleaseInfo(version, release.optString("body", ""),
                        url, name, asset.optString("digest", ""));
            }
        }
        return null;
    }

    private JSONArray fetchArray(String listUrl) throws Exception {
        HttpURLConnection con = (HttpURLConnection) new URL(listUrl).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(6000);
        con.setReadTimeout(10000);
        con.setRequestProperty("Accept", "application/vnd.github+json");
        con.setRequestProperty("User-Agent", "ElephantMonitor-LauncherUpdater/1.0");

        try {
            if (con.getResponseCode() != 200) {
                throw new IOException("HTTP " + con.getResponseCode() + " from " + listUrl);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                return new JSONArray(sb.toString());
            }
        } finally {
            con.disconnect();
        }
    }

    /**
     * Загрузка файла лаунчера во временный файл рядом с целевым. <b>Не</b> подменяет
     * работающий {@code launcher.exe}: сначала качает в {@code launcher.exe.download},
     * затем пытается заменить оригинал. Если оригинал занят — новый файл остаётся
     * как {@code launcher.exe.download}, а в ошибку кладётся путь.
     *
     * @return путь к загруженному файлу (обычно целевой {@code launcher.exe}).
     */
    public Path download(LauncherReleaseInfo release, Path targetLauncher) throws IOException {
        if (release == null || !release.hasDownload()) {
            throw new IOException("Пустая ссылка на скачивание лаунчера");
        }
        if (targetLauncher == null) {
            throw new IOException("Не найден установленный " + LAUNCHER_FILE_NAME);
        }

        Path targetDir = targetLauncher.toAbsolutePath().getParent();
        if (targetDir == null) {
            throw new IOException("Не удалось определить папку для загрузки лаунчера");
        }
        Files.createDirectories(targetDir);

        Path temp = targetDir.resolve(LAUNCHER_FILE_NAME + ".download");
        log.info("Загрузка лаунчера: {} -> {}", release.downloadUrl, temp);

        HttpURLConnection con = (HttpURLConnection) new URL(release.downloadUrl).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(10000);
        con.setReadTimeout(120000);
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("User-Agent", "ElephantMonitor-LauncherUpdater/1.0");

        try {
            if (con.getResponseCode() / 100 != 2) {
                throw new IOException("HTTP " + con.getResponseCode() + " при скачивании лаунчера");
            }
            try (InputStream in = con.getInputStream()) {
                Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            con.disconnect();
        }

        try {
            Files.move(temp, targetLauncher, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            log.info("Лаунчер обновлён: {}", targetLauncher);
            return targetLauncher;
        } catch (IOException locked) {
            log.warn("Не удалось заменить {} (возможно, запущен): {}", targetLauncher, locked.getMessage());
            return temp;
        }
    }

    private static String normalizeDigest(String digest) {
        String d = digest.trim().toLowerCase(Locale.ROOT);
        int colon = d.indexOf(':');
        return colon >= 0 ? d.substring(colon + 1) : d;
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                md.update(buffer, 0, read);
            }
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte b : md.digest()) {
            sb.append(Character.forDigit((b >> 4) & 0xF, 16));
            sb.append(Character.forDigit(b & 0xF, 16));
        }
        return sb.toString();
    }
}
