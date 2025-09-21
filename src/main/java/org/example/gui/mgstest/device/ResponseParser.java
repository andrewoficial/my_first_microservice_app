package org.example.gui.mgstest.device;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ResponseParser {
    
    /**
     * Извлекает все ASCII-строки из бинарных данных.
     * Строка должна состоять из печатных ASCII-символов (0x20-0x7E) и заканчиваться 0x0D.
     */
    public static List<String> extractTextResponses(byte[] data) {
        List<String> result = new ArrayList<>();
        StringBuilder currentString = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            
            // Если байт в диапазоне печатных ASCII-символов
            if (b >= 0x20 && b <= 0x7E) {
                if (!inString) {
                    inString = true;
                }
                currentString.append((char) b);
            }
            // Если найден конец строки (CR)
            else if (b == 0x0D && inString) {
                result.add(currentString.toString());
                currentString.setLength(0);
                inString = false;
            }
            // Любой другой байт прерывает текущую строку
            else if (inString) {
                currentString.setLength(0);
                inString = false;
            }
        }
        
        // Добавляем последнюю строку, если она не завершилась CR
        if (currentString.length() > 0) {
            result.add(currentString.toString());
        }
        
        return result;
    }
    
    /**
     * Альтернативный метод: ищет определенные известные шаблоны ответов
     */
    public static String parseMipexResponse(byte[] data) {
        // Преобразуем весь массив в строку для поиска
        String fullText = new String(data, StandardCharsets.US_ASCII);
        
        // Ищем известные шаблоны ответов
        if (fullText.contains("NOTINST")) {
            return "NOT INSTALLED";
        } else if (fullText.contains("CALB")) {
            return "CALIBRATION SUCCESS";
        } else if (fullText.contains("ERROR")) {
            return "ERROR DETECTED";
        }
        
        // Если ничего не найдено, возвращаем сырые данные
        return "Unknown response: " + fullText.replaceAll("[^\\x20-\\x7E]", ".");
    }
}