package org.example.gui.mgstest.device;

import java.util.ArrayList;
import java.util.List;

public class AdvancedResponseParser {
    
    /**
     * Извлекает все ASCII-строки из бинарных данных.
     * Строка должна состоять из печатных ASCII-символов (0x20-0x7E) длиной не менее 3 символов.
     */
    public static List<String> extractAllTextResponses(byte[] data) {
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
            // Если найден конец строки (CR) или любой другой непечатный символ
            else if (inString) {
                // Сохраняем строку только если она достаточно длинная
                if (currentString.length() >= 3) {
                    result.add(currentString.toString());
                }
                currentString.setLength(0);
                inString = false;
            }
        }
        
        // Добавляем последнюю строку, если она не завершилась и достаточно длинная
        if (inString && currentString.length() >= 3) {
            result.add(currentString.toString());
        }
        
        return result;
    }
    
    /**
     * Альтернативный метод: ищет строки, ограниченные специальными байтами
     */
    public static List<String> extractDelimitedText(byte[] data, byte startByte, byte endByte) {
        List<String> result = new ArrayList<>();
        boolean inString = false;
        StringBuilder currentString = new StringBuilder();

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];
            
            if (b == startByte) {
                inString = true;
                currentString.setLength(0);
            } else if (b == endByte && inString) {
                inString = false;
                if (currentString.length() >= 3) {
                    result.add(currentString.toString());
                }
            } else if (inString && b >= 0x20 && b <= 0x7E) {
                currentString.append((char) b);
            } else if (inString) {
                // Непечатный символ внутри строки - прерываем текущую строку
                inString = false;
                if (currentString.length() >= 3) {
                    result.add(currentString.toString());
                }
            }
        }
        
        return result;
    }
}