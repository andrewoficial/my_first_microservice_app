package org.example.web.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
public class FilePathService {
    public String getFileContent(String fileName) throws IOException {
        File logFile = new File("logs/" + fileName);
        //File logFile = new File("logs/" + "GPS_66_RAK.js");

        // Создаем файл, если его нет
        if (!logFile.exists()) {
            logFile.getParentFile().mkdirs();
            logFile.createNewFile();
        }

        // Читаем содержимое файла
        String content;
        try {
            content = new String(Files.readAllBytes(Paths.get(logFile.getAbsolutePath())));
        } catch (IOException e) {
            // Если чтение не удалось, возвращаем дефолтные данные
            content = "error in Java";
        }

        return content;
    }
}