package parsers.com.edwardsD397;

import org.apache.log4j.Logger;
import org.example.device.protEdwardsD397.EdwardsD397CommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class EdwardsD397CommandRegistryTest {

    private EdwardsD397CommandRegistry registry;
    private Method parseMethod;

    @BeforeEach
    public void setUp() throws NoSuchMethodException {
        registry = new EdwardsD397CommandRegistry();
        // Получаем приватный метод через рефлексию
        parseMethod = EdwardsD397CommandRegistry.class.getDeclaredMethod("parse913AskCmd", byte[].class);
        parseMethod.setAccessible(true);
    }

    @Test
    public void testParseResponseWithMultiDropPrefix() throws Exception {
        // Тестовый ответ с multi-drop префиксом: #01:01=V913 5.0691e+01;59;11;0;0\r
        // Ожидаем: value=5.0691e+01, units=59, state=11, alert=0, priority=0
        byte[] response = "#01:01=V913 5.0691e+01;59;11;0;0\r".getBytes();

        AnswerValues result = (AnswerValues) parseMethod.invoke(registry, response);

        assertNotNull(result);
        assertEquals(5, result.getValues().length); // 5 значений: value+units, stateCode+stateName, alertCode+alertName, priorityCode+priorityName

        // Проверяем значение давления
        assertEquals(5.0691e+01, result.getValues()[0], 0.0001);
        // assertEquals(EdwardsUnits.fromCode(59).getSymbol(), result.getDescription(0)); // Pascals

        // Проверяем состояние
        assertEquals(11, result.getValues()[1]);
        // assertEquals(EdwardsState.fromCode(11).getName(), result.getDescription(1)); // Ожидаем "On"

        // Alert
        assertEquals(0, result.getValues()[2]);
        // assertEquals(EdwardsAlert.fromCode(0).getName(), result.getDescription(2)); // "No Alert"

        // Priority
        assertEquals(0, result.getValues()[3]);
        // assertEquals(EdwardsPriority.fromCode(0).getName(), result.getDescription(3)); // "OK"
    }

    @Test
    public void testParseResponseWithoutPrefix() throws Exception {
        // Тестовый ответ без префикса: =V913 2.4965e-02;59;11;0;0\r
        // Ожидаем: value=2.4965e-02, units=59, state=11, alert=0, priority=0
        byte[] response = "=V913 2.4965e-02;59;11;0;0\r".getBytes();

        AnswerValues result = (AnswerValues) parseMethod.invoke(registry, response);

        assertNotNull(result);
        assertEquals(5, result.getValues().length); // 5 значений

        // Проверяем значение давления
        assertEquals(2.4965e-02, result.getValues()[0], 0.0001);
        // assertEquals(EdwardsUnits.fromCode(59).getSymbol(), result.getDescription(0)); // Pascals

        // Проверяем состояние
        assertEquals(11, result.getValues()[1]);
        // assertEquals(EdwardsState.fromCode(11).getName(), result.getDescription(1)); // "On"

        // Alert
        assertEquals(0, result.getValues()[2]);
        // assertEquals(EdwardsAlert.fromCode(0).getName(), result.getDescription(2)); // "No Alert"

        // Priority
        assertEquals(0, result.getValues()[3]);
        // assertEquals(EdwardsPriority.fromCode(0).getName(), result.getDescription(3)); // "OK"
    }

    @Test
    public void testParseResponseForV914() throws Exception {
        // Тестовый ответ для V914 (аналогично, парсер общий): =V914 1.23e-03;66;5;10;1\r
        // Ожидаем: value=1.23e-03, units=66 (voltage), state=5 (Off), alert=10 (Over Range), priority=1 (warning)
        byte[] response = "=V914 1.23e-03;66;5;10;1\r".getBytes();

        AnswerValues result = (AnswerValues) parseMethod.invoke(registry, response);

        assertNotNull(result);
        assertEquals(5, result.getValues().length);

        assertEquals(1.23e-03, result.getValues()[0], 0.0001);
        // assertEquals(EdwardsUnits.fromCode(66).getSymbol(), result.getDescription(0)); // Voltage

        assertEquals(5, result.getValues()[1]);
        // assertEquals(EdwardsState.fromCode(5).getName(), result.getDescription(1)); // "Off"

        assertEquals(10, result.getValues()[2]);
        // assertEquals(EdwardsAlert.fromCode(10).getName(), result.getDescription(2)); // "Over Range"

        assertEquals(1, result.getValues()[3]);
        // assertEquals(EdwardsPriority.fromCode(1).getName(), result.getDescription(3)); // "warning"
    }

    @Test
    public void testParseResponseForV915WithoutOptionalFields() throws Exception {
        // Тестовый ответ без опциональных alert и priority: =V915 9.9000e+09;59;0\r
        // Ожидаем: value=9.9000e+09, units=59, state=0
        byte[] response = "=V915 9.9000e+09;59;0\r".getBytes();

        AnswerValues result = (AnswerValues) parseMethod.invoke(registry, response);

        assertNotNull(result);
        assertEquals(3, result.getValues().length); // Только 3 значения

        assertEquals(9.9000e+09, result.getValues()[0], 0.0001);
        // assertEquals(EdwardsUnits.fromCode(59).getSymbol(), result.getDescription(0));

        assertEquals(0, result.getValues()[1]);
        // assertEquals(EdwardsState.fromCode(0).getName(), result.getDescription(1)); // "Gauge Not connected"
    }

    @Test
    public void testParseInvalidCommandInResponse() throws Exception {
        // Тестовый ответ с неверной командой: =V999 1.0;59;11;0;0\r
        byte[] response = "=V999 1.0;59;11;0;0\r".getBytes();

        AnswerValues result = (AnswerValues) parseMethod.invoke(registry, response);

        assertNull(result); // Ожидаем null из-за IllegalArgumentException "Command not found"
    }

    @Test
    public void testParseResponseWithWhitespaceAndCarriageReturn() throws Exception {
        // Тестовый ответ с лишними пробелами: =V913  4.56e+00 ;59 ;11 \r
        byte[] response = "=V913  4.56e+00 ;59 ;11 \r".getBytes();

        AnswerValues result = (AnswerValues) parseMethod.invoke(registry, response);

        assertNull(result);
    }
}