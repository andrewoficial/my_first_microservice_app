package org.example.utilites;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import org.example.Main;
import org.json.JSONObject;

import static org.springframework.messaging.simp.stomp.StompHeaderAccessor.getContentLength;

public class ProgramUpdater {

    private static final String API_URL = "https://api.github.com/repos/andrewoficial/my_first_microservice_app/releases/latest";
    private static final String UPDATE_FILE_PATH = new File("◘").getAbsolutePath().replaceAll("◘", "");

    public String getLatestVersion() {
        try {
            // Создаем URL и открываем соединение
            URL url = new URL(API_URL);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");

            // Устанавливаем таймауты
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            // Проверяем HTTP-статус
            int status = con.getResponseCode();
            if (status != 200) {
                System.out.println("GitHub API returned status: " + status);
                return "0.0.0";
            }

            // Читаем ответ
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder response = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            // Парсим JSON
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("tag_name"); // Метка версии в поле "tag_name"
        } catch (Exception e) {
            System.err.println("Error while fetching version: " + e.getMessage());
            return "0.0.0";
        }
    }

    public boolean isAvailableNewVersion(String foundVersion, String currentVersion) {
        // Отладочное сообщение
        System.out.println("Проверка доступности новой версии.");
        System.out.println("Текущая версия: " + currentVersion);
        System.out.println("Найденная версия: " + foundVersion);

        // Проверка на null
        if (currentVersion == null || foundVersion == null) {
            System.out.println("Отладка: Одна из строк версии null.");
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
            System.out.println("Отладка: Формат версии неверный.");
            return false;
        }

        // Разбиение строк на основные части
        String[] currentParts = currentVersion.split("[-.]");
        String[] foundParts = foundVersion.split("[-.]");

        // Отладочное сообщение
        System.out.println("Разделенная текущая версия: " + String.join(", ", currentParts));
        System.out.println("Разделенная найденная версия: " + String.join(", ", foundParts));

        // Сравнение по числовым частям версии
        for (int i = 0; i < 3; i++) {
            int currentNum = Integer.parseInt(currentParts[i]);
            int foundNum = Integer.parseInt(foundParts[i]);

            if (foundNum > currentNum) {
                System.out.println("Найдена новая версия.");
                return true;
            } else if (foundNum < currentNum) {
                System.out.println("Новая версия недоступна, текущая версия новее.");
                return false;
            }
        }

        // Если основные версии равны, проверяем суффиксы (если они есть)
        if (currentParts.length > 3 || foundParts.length > 3) {
            String currentSuffix = (currentParts.length > 3) ? currentParts[3] : "";
            String foundSuffix = (foundParts.length > 3) ? foundParts[3] : "";

            // Отладочное сообщение
            System.out.println("Суффиксы: текущий - " + currentSuffix + ", найденный - " + foundSuffix);

            if (!currentSuffix.isEmpty() && foundSuffix.isEmpty()) {
                System.out.println("Найдена стабильная версия.");
                return true;
            } else if (foundSuffix.compareTo(currentSuffix) < 0) {
                System.out.println("Найдена новая версия без суффикса.");
                return true;
            }
        }

        System.out.println("Версии совпадают.");
        return false;
    }

    public void downloadUpdate() throws IOException, InterruptedException {
        // Отладочное сообщение
        System.out.println("Загрузка новой версии.");

        // Создание HTTP-клиента
        HttpClient client = HttpClient.newHttpClient();

        // Создание HTTP-запроса
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .GET()
                .build();

        // Выполнение HTTP-запроса      
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());



        // Проверка статуса ответа
        if (response.statusCode() != 200) {
            System.out.println("Отладка: Ошибка при загрузке новой версии.");
            return;
        }

        // Отладочное сообщение
        System.out.println("Ответ от сервера: " + response.body());

        // Парсинг JSON-ответа
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        String downloadUrl = jsonObject.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
        String fileName = jsonObject.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();

        // Отладочное сообщение
        System.out.println("Ссылка на загрузку: " + downloadUrl);
        System.out.println("Имя файла: " + fileName);

        // Создание директории для сохранения файла
        Files.createDirectories(Paths.get(UPDATE_FILE_PATH));

        // Путь для сохранения файла
        String filePath = UPDATE_FILE_PATH + fileName;

        // Загрузка файла
        try (InputStream is = new URL(downloadUrl).openStream()) {
            long totalBytes = getContentLength(downloadUrl);
            long downloadedBytes = 0;
            byte[] buffer = new byte[8192];
            int bytesRead;
            long lastReportTime = System.currentTimeMillis();
            try (var fos = Files.newOutputStream(Paths.get(filePath))) {
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                    downloadedBytes += bytesRead;

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastReportTime >= 300) { // Каждые 300 мс
                        System.out.printf("Загружено: %.2f МБ / %.2f МБ%n",
                                downloadedBytes / 1_048_576.0, totalBytes / 1_048_576.0);
                        lastReportTime = currentTime;
                    }
                }
            }

            System.out.println("Файл успешно загружен: " + filePath);
        } catch (IOException e) {
            System.out.println("Ошибка при загрузке файла: " + e.getMessage());
        }
    }

    private long getContentLength(String downloadUrl) throws IOException {
        URL url = new URL(downloadUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("HEAD");
        long contentLength = connection.getContentLengthLong();
        connection.disconnect();
        return contentLength;
    }


}

