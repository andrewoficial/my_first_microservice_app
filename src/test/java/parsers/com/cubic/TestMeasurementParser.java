package parsers.com.cubic;

import org.example.device.protCubic.CubicCommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestMeasurementParser {

    private static Stream<Arguments> provideTestData() {
        return Stream.of(
                // Базовые тесты с нулевой концентрацией
                Arguments.of("Zero concentration, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE4},
                        0.0,
                        "No active flags"
                ),
                Arguments.of("Zero concentration, high humidity flag",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0xC4},
                        0.0,
                        "High humidity"
                ),
                Arguments.of("Zero concentration, all flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xFF, (byte) 0x00, (byte) 0xE5},
                        0.0,
                        "No calibration, High humidity, Reference channel over limit, Measurement channel over limit, Warming-up, Malfunction, Out of range"
                ),

                // Тесты с низкими концентрациями (0-50)
                Arguments.of("Low concentration 25, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x19, (byte) 0x00, (byte) 0x00, (byte) 0xE4},
                        25.0,
                        "No active flags"
                ),
                Arguments.of("Low concentration 25, warming-up flag",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x19, (byte) 0x01, (byte) 0x00, (byte) 0xE5},
                        25.0,
                        "Warming-up"
                ),
                Arguments.of("Low concentration 50, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x32, (byte) 0x00, (byte) 0x00, (byte) 0xE4},
                        50.0,
                        "No active flags"
                ),
                Arguments.of("Low concentration 50, malfunction flag",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x32, (byte) 0x02, (byte) 0x00, (byte) 0xE6},
                        50.0,
                        "Malfunction"
                ),

                // Тесты со средними концентрациями (90-146) - для проверки вашей проблемы
                Arguments.of("Medium concentration 90, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x5A, (byte) 0x00, (byte) 0x00, (byte) 0xE4},
                        90.0,
                        "No active flags"
                ),
                Arguments.of("Medium concentration 90, out of range flag",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x5A, (byte) 0x04, (byte) 0x00, (byte) 0xE8},
                        90.0,
                        "Out of range"
                ),
                Arguments.of("Medium concentration 146, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x92, (byte) 0x00, (byte) 0x00, (byte) 0xE4},
                        146.0,
                        "No active flags"
                ),
                Arguments.of("Medium concentration 146, not calibrated flag",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x92, (byte) 0x10, (byte) 0x00, (byte) 0xF4},
                        146.0,
                        "No calibration"
                ),

                // Тесты с высокими концентрациями (200+)
                Arguments.of("High concentration 200, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0xC8, (byte) 0x00, (byte) 0x00, (byte) 0xE4},
                        200.0,
                        "No active flags"
                ),
                Arguments.of("High concentration 200, reference channel over limit",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0xC8, (byte) 0x40, (byte) 0x00, (byte) 0x24},
                        200.0,
                        "Reference channel over limit"
                ),
                Arguments.of("High concentration 250, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0xFA, (byte) 0x00, (byte) 0x00, (byte) 0xEA},
                        250.0,
                        "No active flags"
                ),
                Arguments.of("High concentration 250, all flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0xFA, (byte) 0xFF, (byte) 0x00, (byte) 0xEB},
                        250.0,
                        "No calibration, High humidity, Reference channel over limit, Measurement channel over limit, Warming-up, Malfunction, Out of range"
                ),

                // Тесты с максимальными концентрациями
                Arguments.of("Max concentration 495, no flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x01, (byte) 0xEF, (byte) 0x00, (byte) 0x00, (byte) 0xF4},
                        495.0,
                        "No active flags"
                ),
                Arguments.of("Max concentration 495, all flags",
                        new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01, (byte) 0x01, (byte) 0xEF, (byte) 0xFF, (byte) 0x00, (byte) 0xF5},
                        495.0,
                        "No calibration, High humidity, Reference channel over limit, Measurement channel over limit, Warming-up, Malfunction, Out of range"
                )
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideTestData")
    public void testParseMeasurementResponse_ValidData(String testName, byte[] response, double expectedConcentration, String expectedStatus) throws Exception {
        System.out.println("\n=== Тест: " + testName + " ===");
        System.out.println("Hex данные: " + bytesToHex(response));

        CubicCommandRegistry registry = new CubicCommandRegistry();

        Method method = CubicCommandRegistry.class.getDeclaredMethod("parseMeasurementResponse", byte[].class);
        method.setAccessible(true);

        AnswerValues result = (AnswerValues) method.invoke(registry, response);

        assertNotNull(result, "Method should not return null for valid data");

        double[] values = result.getValues();
        assertEquals(2, values.length, "Should return 2 values: concentration and status");

        // Проверяем концентрацию
        System.out.println("Ожидаемая концентрация: " + expectedConcentration);
        System.out.println("Фактическая концентрация: " + values[0]);
        assertEquals(expectedConcentration, values[0], 0.001, "Concentration mismatch");

        // Проверяем статус
        String[] units = result.getUnits();
        String actualStatus = units[1];
        System.out.println("Ожидаемый статус: " + expectedStatus);
        System.out.println("Фактический статус: " + actualStatus);
        assertEquals(expectedStatus, actualStatus, "Status string mismatch");

        System.out.println("✓ Тест пройден успешно");
    }

    @Test
    public void testParseMeasurementResponse_InvalidHeader() throws Exception {
        byte[] response = new byte[]{
                (byte) 0x16, (byte) 0x05, (byte) 0x02, // Неверная команда
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE4
        };

        CubicCommandRegistry registry = new CubicCommandRegistry();
        Method method = CubicCommandRegistry.class.getDeclaredMethod("parseMeasurementResponse", byte[].class);
        method.setAccessible(true);

        AnswerValues result = (AnswerValues) method.invoke(registry, response);

        assertNull(result, "Should return null for invalid header");
    }

    @Test
    public void testParseMeasurementResponse_ShortResponse() throws Exception {
        byte[] response = new byte[]{(byte) 0x16, (byte) 0x05, (byte) 0x01}; // Слишком коротко

        CubicCommandRegistry registry = new CubicCommandRegistry();
        Method method = CubicCommandRegistry.class.getDeclaredMethod("parseMeasurementResponse", byte[].class);
        method.setAccessible(true);

        AnswerValues result = (AnswerValues) method.invoke(registry, response);

        assertNull(result, "Should return null for short response");
    }

    @Test
    public void testParseMeasurementResponse_InvalidACK() throws Exception {
        byte[] response = new byte[]{
                (byte) 0x15, // Неверный ACK
                (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE4
        };

        CubicCommandRegistry registry = new CubicCommandRegistry();
        Method method = CubicCommandRegistry.class.getDeclaredMethod("parseMeasurementResponse", byte[].class);
        method.setAccessible(true);

        AnswerValues result = (AnswerValues) method.invoke(registry, response);

        assertNull(result, "Should return null for invalid ACK");
    }

    @Test
    public void testParseMeasurementResponse_InvalidLength() throws Exception {
        byte[] response = new byte[]{
                (byte) 0x16, (byte) 0x06, // Неверная длина
                (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xE4
        };

        CubicCommandRegistry registry = new CubicCommandRegistry();
        Method method = CubicCommandRegistry.class.getDeclaredMethod("parseMeasurementResponse", byte[].class);
        method.setAccessible(true);

        AnswerValues result = (AnswerValues) method.invoke(registry, response);

        assertNull(result, "Should return null for invalid length byte");
    }

    // Вспомогательный метод для преобразования байтов в hex строку
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}