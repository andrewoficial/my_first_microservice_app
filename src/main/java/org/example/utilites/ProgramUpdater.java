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
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.Main;
import org.example.services.AnswerStorage;
import org.json.JSONObject;

import static org.springframework.messaging.simp.stomp.StompHeaderAccessor.getContentLength;

public class ProgramUpdater {

    private static final String API_URL = "https://api.github.com/repos/andrewoficial/my_first_microservice_app/releases/latest";
    private static final String UPDATE_FILE_PATH = new File("◘").getAbsolutePath().replaceAll("◘", "");
    private static final Logger log = Logger.getLogger(ProgramUpdater.class);
    private String whatsNews = "Запрос не был произведён";

    @Getter
    private boolean busy = false;
    @Getter
    private volatile int updatePercents = 0;
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
                log.warn("GitHub API returned status: " + status);
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
            whatsNews = jsonResponse.getString("body"); // Строка с описанием "что нового" (уже содержит r n)
            System.out.println(whatsNews);
            return jsonResponse.getString("tag_name"); // Метка версии в поле "tag_name"
        } catch (Exception e) {
            log.warn("Error while fetching version: " + e.getMessage());
            return "0.0.0";
        }
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

    public String getInfo() throws IOException {
        return whatsNews;
    }

    public void downloadUpdate() throws IOException, InterruptedException {
        updatePercents = 0;

        // Отладочное сообщение
        log.info("Загрузка новой версии.");
        this.busy = true;
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
            log.warn("Ошибка при загрузке новой версии.");
            this.busy = false;
            return;
        }

        // Отладочное сообщение
        //System.out.println("Ответ от сервера: " + response.body());

        // Парсинг JSON-ответа
        JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
        String downloadUrl = jsonObject.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("browser_download_url").getAsString();
        String fileName = jsonObject.get("assets").getAsJsonArray().get(0).getAsJsonObject().get("name").getAsString();

        // Отладочное сообщение
        //System.out.println("Ссылка на загрузку: " + downloadUrl);
        //System.out.println("Имя файла: " + fileName);

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
                    if (currentTime - lastReportTime >= 150) { // Каждые 300 мс


                        System.out.printf("Загружено: %.2f МБ / %.2f МБ%n",
                                downloadedBytes / 1_048_576.0, totalBytes / 1_048_576.0);

                        updatePercents = (int) ((downloadedBytes / (double) totalBytes) * 100);
                        System.out.printf("Прогресс: %d%%%n", updatePercents);

                        lastReportTime = currentTime;
                    }

                }
            }

            log.info("Файл успешно загружен: " + filePath);
        } catch (IOException e) {
            updatePercents = 0;
            log.info("Ошибка при загрузке файла: " + e.getMessage());
        }finally {
            busy = false;
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

