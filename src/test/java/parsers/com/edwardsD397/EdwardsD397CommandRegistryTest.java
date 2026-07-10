package parsers.com.edwardsD397;

import org.apache.log4j.Logger;
import org.example.device.protEdwardsD397.EdwardsD397CommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class EdwardsD397CommandRegistryTest {

    private static final Logger log = Logger.getLogger(EdwardsD397CommandRegistryTest.class);

    private EdwardsD397CommandRegistry registry;
    private Method parseGaugeMethod;

    @BeforeEach
    public void setUp() throws NoSuchMethodException {
        registry = new EdwardsD397CommandRegistry();
        parseGaugeMethod = EdwardsD397CommandRegistry.class.getDeclaredMethod("parseGaugeResponse", byte[].class, int.class);
        parseGaugeMethod.setAccessible(true);
    }

    private AnswerValues invokeParse(byte[] response, int objId) throws Exception {
        return (AnswerValues) parseGaugeMethod.invoke(registry, response, objId);
    }

    @Test
    public void testParseResponseWithMultiDropPrefix() throws Exception {
        // #01:01=V913 5.0691e+01;59;11;0;0\r
        // После cleanResponse: 5.0691e+01;59;11;0;0
        // values: [5.0691e+01, 59, 11, 0, 0]
        byte[] response = "#01:01=V913 5.0691e+01;59;11;0;0\r".getBytes();

        AnswerValues result = invokeParse(response, 913);

        assertNotNull(result);
        assertEquals(5, result.getValues().length);

        assertEquals(5.0691e+01, result.getValues()[0], 0.0001); // value
        assertEquals(59, result.getValues()[1], 0.0001);         // units
        assertEquals(11, result.getValues()[2], 0.0001);         // state
        assertEquals(0, result.getValues()[3], 0.0001);          // alert
        assertEquals(0, result.getValues()[4], 0.0001);          // priority
    }

    @Test
    public void testParseResponseWithoutPrefix() throws Exception {
        // =V913 2.4965e-02;59;11;0;0\r
        byte[] response = "=V913 2.4965e-02;59;11;0;0\r".getBytes();

        AnswerValues result = invokeParse(response, 913);

        assertNotNull(result);
        assertEquals(5, result.getValues().length);

        assertEquals(2.4965e-02, result.getValues()[0], 0.0001);
        assertEquals(59, result.getValues()[1], 0.0001);
        assertEquals(11, result.getValues()[2], 0.0001);
        assertEquals(0, result.getValues()[3], 0.0001);
        assertEquals(0, result.getValues()[4], 0.0001);
    }

    @Test
    public void testParseResponseForV914() throws Exception {
        // =V914 1.23e-03;66;5;10;1\r
        byte[] response = "=V914 1.23e-03;66;5;10;1\r".getBytes();

        AnswerValues result = invokeParse(response, 914);

        assertNotNull(result);
        assertEquals(5, result.getValues().length);

        assertEquals(1.23e-03, result.getValues()[0], 0.0001);
        assertEquals(66, result.getValues()[1], 0.0001);
        assertEquals(5, result.getValues()[2], 0.0001);
        assertEquals(10, result.getValues()[3], 0.0001);
        assertEquals(1, result.getValues()[4], 0.0001);
    }

    @Test
    public void testParseResponseWithoutOptionalFields() throws Exception {
        // =V915 9.9000e+09;59;0\r — без alert и priority
        byte[] response = "=V915 9.9000e+09;59;0\r".getBytes();

        AnswerValues result = invokeParse(response, 915);

        assertNotNull(result);
        assertEquals(5, result.getValues().length); // массив всегда минимум 5

        assertEquals(9.9000e+09, result.getValues()[0], 0.0001);
        assertEquals(59, result.getValues()[1], 0.0001);
        assertEquals(0, result.getValues()[2], 0.0001);
        assertEquals(0.0, result.getValues()[3], 0.0001); // alert — нет данных, default
        assertEquals(0.0, result.getValues()[4], 0.0001); // priority — нет данных, default
    }

    @Test
    public void testParseWithUnknownCommand() throws Exception {
        // =V999 1.0;59;11;0;0\r — команда 999 не распознаётся,
        // но cleanResponse всё равно снимет префикс, вернёт данные.
        // Парсер не валидирует Object ID, поэтому ответ будет распарсен.
        byte[] response = "=V999 1.0;59;11;0;0\r".getBytes();

        AnswerValues result = invokeParse(response, 999);

        assertNotNull(result);
        assertEquals(5, result.getValues().length);
        assertEquals(1.0, result.getValues()[0], 0.0001);
    }

    @Test
    public void testParseWithWhitespace() throws Exception {
        // =V913  4.56e+00 ;59 ;11 \r — пробелы вокруг значений
        byte[] response = "=V913  4.56e+00 ;59 ;11 \r".getBytes();

        AnswerValues result = invokeParse(response, 913);

        assertNotNull(result);
        // после cleanResponse: 4.56e+00;59;11 → 3 части
        assertEquals(5, result.getValues().length);
        assertEquals(4.56, result.getValues()[0], 0.0001);
        assertEquals(59, result.getValues()[1], 0.0001);
        assertEquals(11, result.getValues()[2], 0.0001);
    }
}
