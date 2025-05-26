package org.example.services.loggers;

import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;

public class GPS_Loger {
    private final String fileName;
    private File logFile;
    private Long dateTimeLastWrite = System.currentTimeMillis();
    private final ArrayList<String> stringsBuffer = new ArrayList<>();
    private final int dev_ident;
    private final String deviceName;
    private boolean isFileInitialized = false;

    public GPS_Loger(String name, int dev_ident) {
        this.deviceName = name;
        this.dev_ident = dev_ident;
        this.fileName = "GPS_" + name + ".js";

        // Создание директории и файла
        try {
            File logsDir = new File("logs");
            if (!logsDir.exists()) {
                logsDir.mkdirs();
            }
            logFile = new File("logs/" + fileName);
            if (!logFile.exists()) {
                if (logFile.createNewFile()) {
                    // Записываем шапку при создании файла
                    initializeFile();
                    isFileInitialized = true;
                }
            } else {
                isFileInitialized = true; // Файл уже существует
            }
        } catch (IOException e) {
            System.err.println("Ошибка создания файла: " + e.getMessage());
        }
    }

    private void initializeFile() throws IOException {
        try (FileWriter fw = new FileWriter(logFile, false);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write("var data_" + deviceName + " = [];\n");
        }
    }

    public void writeLine(AnswerValues answer) {
        if (answer == null || answer.getValues() == null || answer.getUnits() == null || answer.getValues().length < 2) {
            System.err.println("Некорректные данные AnswerValues");
            return;
        }

        // Формируем строку для записи: [lon, lat, "1"]
        StringBuilder line = new StringBuilder();
        double lon = answer.getValues()[3]; // LON
        double lat = answer.getValues()[2]; // LAT
        line.append("[")
                .append(lon)
                .append(", ")
                .append(lat)
                .append(", \"1\"]");

        // Буферизация
        if ((System.currentTimeMillis() - dateTimeLastWrite) < 300L) {
            stringsBuffer.add(line.toString());
        } else {
            dateTimeLastWrite = System.currentTimeMillis();
            stringsBuffer.add(line.toString());
            appendToFile();
        }
    }

    private void appendToFile() {
        if (stringsBuffer.isEmpty()) {
            return;
        }

        try {
            // Читаем текущее содержимое файла
            StringBuilder fileContent = new StringBuilder();
            if (logFile.exists()) {
                try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                }
            }

            // Модифицируем содержимое: добавляем новые записи в массив
            String content = fileContent.toString();
            int arrayStart = content.indexOf("[");
            int arrayEnd = content.lastIndexOf("]");
            if (arrayStart == -1 || arrayEnd == -1) {
                // Если массив поврежден, перезаписываем шапку
                initializeFile();
                content = "var data_" + deviceName + " = [];\n";
                arrayStart = content.indexOf("[");
                arrayEnd = content.lastIndexOf("]");
            }

            StringBuilder newContent = new StringBuilder();
            newContent.append(content, 0, arrayEnd); // До конца массива

            // Добавляем новые записи
            if (arrayEnd > arrayStart + 1) {
                newContent.append(", "); // Если массив не пустой, добавляем запятую
            }
            newContent.append(String.join(", ", stringsBuffer));
            newContent.append("];\n"); // Закрываем массив

            // Записываем обновленное содержимое
            try (FileWriter fw = new FileWriter(logFile, false);
                 BufferedWriter bw = new BufferedWriter(fw)) {
                bw.write(newContent.toString());
            }

            stringsBuffer.clear();
        } catch (IOException e) {
            System.err.println("Ошибка записи в файл: " + e.getMessage());
        }
    }
}
