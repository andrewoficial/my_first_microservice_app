package org.example.utilites.update;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GitFlicReleaseClient {

    private static final String ORIGIN = "https://gitflic.ru";
    private static final String API_ORIGIN = "https://api.gitflic.ru";
    private static final String TOKEN = "1d691a01-16a4-45df-b9c3-b9f24c6a8bfb";

    private static final Pattern LIST_RELEASE_LINK = Pattern.compile(
            "href=\"((?:https://gitflic\\.ru)?/project/([^\"]+)/release/([0-9a-fA-F\\-]{36}))\"[^>]*>\\s*([^<]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DETAIL_JAR_LINK = Pattern.compile(
            "href=\"((?:https://gitflic\\.ru)?/project/[^\"]+/release/[0-9a-fA-F\\-]{36}/[0-9a-fA-F\\-]{36}/download)\"[^>]*>\\s*([^<]*\\.jar[^<]*)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern MARKDOWN_BODY = Pattern.compile(
            "<div[^>]*class=\"[^\"]*markdown-body[^\"]*\"[^>]*>(.*?)</div>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern VERSION_IN_TEXT = Pattern.compile(
            "v?(\\d+\\.\\d+\\.\\d+(?:-\\w+)?)",
            Pattern.CASE_INSENSITIVE);

    private GitFlicReleaseClient() {}

    public static boolean isGitFlicUrl(String url) {
        if (url == null || url.isBlank()) return false;
        String u = url.toLowerCase();
        return u.contains("gitflic.ru");
    }

    /**
     * Нормализует URL к API-эндпоинту списка релизов.
     */
    public static String toListUrl(String input) {
        if (input == null || input.isBlank()) return "";
        String u = input.trim();

        // api.gitflic.ru/.../release/latest → .../release
        if (u.contains("/release/latest")) {
            u = u.replace("/release/latest", "/release");
        }

        // Конвертируем web UI URL → API URL
        if (u.contains("gitflic.ru/project/") && !u.contains("api.gitflic.ru")) {
            u = u.replaceAll("(?i)https?://gitflic\\.ru", API_ORIGIN);
        }

        // /release/{uuid} → /release
        u = u.replaceAll("(?i)(/project/[^?#]+/release)/[0-9a-fA-F\\-]{36}.*", "$1");

        // убрать хвостовой /file/.../download и т.п.
        if (u.matches("(?i).*/project/.+/release$") || u.contains("/release?")) {
            // ok
        } else if (u.matches("(?i).*/project/.+/release/.*")) {
            u = u.replaceAll("(?i)(/project/[^?#]+/release).*", "$1");
        }

        // убрать query-параметры (API не нужно)
        if (u.contains(API_ORIGIN) && u.contains("?")) {
            u = u.replaceAll("\\?.*", "");
        }

        return u;
    }

    public static List<Release> fetchReleases(String listUrl) throws Exception {
        String url = toListUrl(listUrl);
        String body = httpGet(url);

        if (body == null || body.isBlank()) {
            throw new IOException("GitFlic API вернул пустой ответ: " + url);
        }

        String trimmed = body.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return parseApiJson(trimmed);
        }
        return fetchFromHtmlList(url, body);
    }

    private static List<Release> fetchFromHtmlList(String listUrl, String listHtml) throws Exception {
        Map<String, ReleaseStub> stubs = new LinkedHashMap<>();
        Matcher m = LIST_RELEASE_LINK.matcher(listHtml);
        while (m.find()) {
            String pathOrUrl = m.group(1);
            String releaseUuid = m.group(3);
            String title = m.group(4).trim();
            String version = extractVersion(title);
            if (version.isEmpty()) continue;
            if (stubs.containsKey(releaseUuid)) continue;

            String detailUrl = toAbsolute(pathOrUrl);
            stubs.put(releaseUuid, new ReleaseStub(version, detailUrl));
        }

        if (stubs.isEmpty()) {
            throw new IOException("GitFlic: на странице релизов не найдено ни одного релиза: " + listUrl);
        }

        List<Release> result = new ArrayList<>(stubs.size());
        int i = 0;
        for (ReleaseStub stub : stubs.values()) {
            // Полные детали (jar + notes) — для всех, но с разумным лимитом сетевых запросов
            if (i < 30) {
                result.add(enrichFromDetail(stub));
            } else {
                result.add(new Release(stub.version, "", "", ""));
            }
            i++;
        }
        return result;
    }

    private static Release enrichFromDetail(ReleaseStub stub) {
        try {
            String html = httpGet(stub.detailUrl);
            String downloadUrl = "";
            String fileName = "";
            Matcher jar = DETAIL_JAR_LINK.matcher(html);
            // предпочитаем Elephant-Monitor-*.jar, иначе первый .jar
            while (jar.find()) {
                String href = toAbsolute(jar.group(1).trim());
                String name = jar.group(2).trim();
                if (downloadUrl.isEmpty() || name.toLowerCase().contains("elephant-monitor")) {
                    downloadUrl = href;
                    fileName = name;
                    if (name.toLowerCase().contains("elephant-monitor")) break;
                }
            }
            String notes = extractNotes(html);
            return new Release(stub.version, notes, downloadUrl, fileName);
        } catch (Exception e) {
            return new Release(stub.version, "", "", "");
        }
    }

    private static String stripHtml(String html) {
        if (html == null || html.isBlank()) return "";
        String s = html;
        s = s.replaceAll("(?i)<br\\s*/?>", "\n");
        s = s.replaceAll("(?i)</p>", "\n");
        s = s.replaceAll("(?i)</li>", "\n");
        s = s.replaceAll("<[^>]+>", "");
        s = s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
        return s.replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String extractNotes(String html) {
        Matcher body = MARKDOWN_BODY.matcher(html);
        if (!body.find()) return "";
        return stripHtml(body.group(1));
    }

    /** Парсит JSON ответа GitFlic API (list или single object). */
    static List<Release> parseApiJson(String json) {
        org.json.JSONArray array;
        String t = json.trim();
        if (t.startsWith("[")) {
            array = new org.json.JSONArray(t);
        } else {
            org.json.JSONObject root = new org.json.JSONObject(t);
            if (root.has("_embedded")) {
                org.json.JSONObject emb = root.getJSONObject("_embedded");
                if (emb.has("releaseTagModelList")) {
                    array = emb.getJSONArray("releaseTagModelList");
                } else {
                    array = new org.json.JSONArray();
                    array.put(root);
                }
            } else if (root.has("tagName") || root.has("tag_name")) {
                array = new org.json.JSONArray();
                array.put(root);
            } else {
                array = new org.json.JSONArray();
            }
        }

        List<Release> out = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) {
            org.json.JSONObject obj = array.optJSONObject(i);
            if (obj == null) continue;
            String version = obj.optString("tagName", obj.optString("tag_name", ""));
            String notes = stripHtml(obj.optString("description", obj.optString("body", "")));
            String downloadUrl = "";
            String fileName = "";
            org.json.JSONArray files = obj.optJSONArray("attachmentFiles");
            if (files != null) {
                for (int j = 0; j < files.length(); j++) {
                    org.json.JSONObject f = files.optJSONObject(j);
                    if (f == null) continue;
                    String name = f.optString("name", "");
                    String link = f.optString("link", "");
                    if (link.isEmpty()) continue;
                    if (downloadUrl.isEmpty() || name.toLowerCase().endsWith(".jar")) {
                        downloadUrl = link;
                        fileName = name;
                        if (name.toLowerCase().endsWith(".jar")) break;
                    }
                }
            }
            if (!version.isEmpty()) {
                // убираем ведущую v для единообразия с ReleaseParser
                if (version.startsWith("v") || version.startsWith("V")) {
                    version = version.substring(1);
                }
                out.add(new Release(version, notes, downloadUrl, fileName));
            }
        }
        return out;
    }

    private static String extractVersion(String title) {
        if (title == null) return "";
        Matcher m = VERSION_IN_TEXT.matcher(title.trim());
        if (m.find()) {
            return m.group(1);
        }
        return "";
    }

    private static String toAbsolute(String pathOrUrl) {
        if (pathOrUrl == null || pathOrUrl.isBlank()) return "";
        if (pathOrUrl.startsWith("http://") || pathOrUrl.startsWith("https://")) {
            return pathOrUrl;
        }
        if (pathOrUrl.startsWith("/")) {
            return ORIGIN + pathOrUrl;
        }
        return ORIGIN + "/" + pathOrUrl;
    }

    public static void applyAuth(HttpURLConnection con) {
        con.setRequestProperty("Authorization", "token " + TOKEN);
    }

    private static String httpGet(String url) throws IOException {
        HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
        con.setRequestMethod("GET");
        con.setConnectTimeout(8000);
        con.setReadTimeout(15000);
        con.setInstanceFollowRedirects(true);
        con.setRequestProperty("User-Agent", "ElephantMonitor-Updater/1.0");
        con.setRequestProperty("Accept", "text/html,application/json,*/*");
        applyAuth(con);

        int code = con.getResponseCode();
        if (code != 200) {
            throw new IOException("HTTP " + code + " from " + url);
        }
        try (BufferedReader r = new BufferedReader(
                new InputStreamReader(con.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } finally {
            con.disconnect();
        }
    }

    private static final class ReleaseStub {
        final String version;
        final String detailUrl;

        ReleaseStub(String version, String detailUrl) {
            this.version = version;
            this.detailUrl = detailUrl;
        }
    }
}
