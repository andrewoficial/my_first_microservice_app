package parsers.com.cubic;

import org.apache.log4j.Logger;
import org.example.device.protCubic.CubicCommandRegistry;
import org.example.services.AnswerValues;

public class TestGasPropertyParser {
    private static final org.apache.log4j.Logger log = Logger.getLogger(TestGasPropertyParser.class);
    
    public static void main(String[] args) {
        // Создаем экземпляр CubicCommandRegistry
        CubicCommandRegistry registry = new CubicCommandRegistry();
        
        // Тестовые данные для SJH-5B (метан 0-5% об.)
        // Ожидаемые значения: range=5.0, decimals=2, unit=2 (vol%)
        byte[] testResponse = new byte[] {
            (byte) 0x16, // ACK
            (byte) 0x08, // LB
            (byte) 0x0D, // CMD
            (byte) 0x01, // DF1
            (byte) 0xF4, // DF2
            (byte) 0x02, // DF3 (decimals)
            (byte) 0x00, // DF4 (reserved)
            (byte) 0x02, // DF5 (unit)
            (byte) 0x00, // DF6 (reserved)
            (byte) 0x00, // DF7 (reserved)
            (byte) 0xDC  // CS
        };
        
        log.info("=== ТЕСТИРОВАНИЕ parseGasPropertyResponse ===");
        
        try {
            // Используем рефлексию для вызова private метода
            java.lang.reflect.Method method = CubicCommandRegistry.class.getDeclaredMethod("parseGasPropertyResponse", byte[].class);
            method.setAccessible(true);
            
            AnswerValues result = (AnswerValues) method.invoke(registry, testResponse);
            
            if (result != null) {
                log.info("=== РЕЗУЛЬТАТЫ ТЕСТА ===");
                double[] results = result.getValues();
                log.info("Range: " + results[0]);
                log.info("Decimals: " + results[1]);
                log.info("Unit: " + results[2] + " ( )");
                
                // Проверяем ожидаемые значения
                double range = results[0];
                double decimals = results[1];
                double unit = results[2];
                
                boolean rangeOk = Math.abs(range - 5.0) < 0.001;
                boolean decimalsOk = decimals == 2.0;
                boolean unitOk = unit == 2.0;
                
                log.info("=== ПРОВЕРКА ===");
                log.info("Range (5.0): " + (rangeOk ? "ПРОЙДЕНО" : "НЕ ПРОЙДЕНО"));
                log.info("Decimals (2): " + (decimalsOk ? "ПРОЙДЕНО" : "НЕ ПРОЙДЕНО"));
                log.info("Unit (2): " + (unitOk ? "ПРОЙДЕНО" : "НЕ ПРОЙДЕНО"));
                
                if (rangeOk && decimalsOk && unitOk) {
                    log.info("=== ВСЕ ТЕСТЫ ПРОЙДЕНЫ УСПЕШНО ===");
                } else {
                    log.info("=== НЕКОТОРЫЕ ТЕСТЫ НЕ ПРОЙДЕНЫ ===");
                }
            } else {
                log.error("Метод вернул null");
            }
            
        } catch (Exception e) {
            log.error("Ошибка при вызове метода: " + e.getMessage(), e);
        }
    }
}