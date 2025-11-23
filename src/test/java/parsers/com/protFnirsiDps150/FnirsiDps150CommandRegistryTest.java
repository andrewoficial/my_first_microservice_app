package parsers.com.protFnirsiDps150;

import org.apache.log4j.Logger;
import org.example.device.command.SingleCommand;
import org.example.device.protFnirsiDps150.FnirsiDps150CommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static org.junit.jupiter.api.Assertions.*;

public class FnirsiDps150CommandRegistryTest {

    private FnirsiDps150CommandRegistry registry;
    private Method parseFloatMethod;
    private Method parseByteMethod;
    private Method validateChecksumMethod;

    @BeforeEach
    public void setUp() throws NoSuchMethodException {
        registry = new FnirsiDps150CommandRegistry();
        // Получаем приватные методы через рефлексию
        parseFloatMethod = FnirsiDps150CommandRegistry.class.getDeclaredMethod("parseFloatResponse", byte[].class);
        parseFloatMethod.setAccessible(true);

        parseByteMethod = FnirsiDps150CommandRegistry.class.getDeclaredMethod("parseByteResponse", byte[].class);
        parseByteMethod.setAccessible(true);

        validateChecksumMethod = FnirsiDps150CommandRegistry.class.getDeclaredMethod("validateChecksum", byte[].class, boolean.class);
        validateChecksumMethod.setAccessible(true);
    }

    @Test
    public void testParseFloatResponseValid() throws Exception {
        // Тестовый ответ: start=0xF0, cmd=0xA1, type=0x03 (VOUT_MEAS), len=4, data= float 12.34 (little-endian), cs=correct
        float testValue = 12.34f;
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(testValue);
        byte[] data = bb.array();
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, data[0], data[1], data[2], data[3] };
        byte cs = registry.calculateChecksum(response, 2); // Расчёт cs
        byte[] fullResponse = new byte[response.length + 1];
        System.arraycopy(response, 0, fullResponse, 0, response.length);
        fullResponse[response.length] = cs;

        AnswerValues result = (AnswerValues) parseFloatMethod.invoke(registry, fullResponse);

        assertNotNull(result);
        assertEquals(1, result.getValues().length);
        assertEquals(testValue, result.getValues()[0], 0.0001);
        // assertEquals("value", result.getDescription(0)); // Если descriptions реализованы
    }

    @Test
    public void testParseFloatResponseWrongLength() throws Exception {
        // Тестовый ответ с неверной длиной (не 9 байт)
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, 0x00, 0x00 }; // Короткий

        AnswerValues result = (AnswerValues) parseFloatMethod.invoke(registry, response);

        assertNull(result); // Ожидаем null
    }

    @Test
    public void testParseFloatResponseInvalidHeader() throws Exception {
        // Тестовый ответ с неверным start или cmd
        byte[] response = new byte[]{ (byte)0xFF, (byte)0xA1, (byte)0x03, (byte)0x04, 0x00, 0x00, 0x00, 0x00, 0x00 }; // Неверный start

        AnswerValues result = (AnswerValues) parseFloatMethod.invoke(registry, response);

        assertNull(result); // Ожидаем null
    }

    @Test
    public void testParseFloatResponseChecksumError() throws Exception {
        // Тестовый ответ с неверной cs
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, 0x00, 0x00, 0x00, 0x00, (byte)0xFF }; // Неверная cs

        AnswerValues result = (AnswerValues) parseFloatMethod.invoke(registry, response);

        assertNull(result); // Ожидаем null
    }

    @Test
    public void testParseByteResponseValid() throws Exception {
        // Тестовый ответ: start=0xF0, cmd=0xA1, type=0x00 (OUTPUT), len=1, data=1 (on), cs=correct
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x00, (byte)0x01, (byte)0x01 };
        byte cs = registry.calculateChecksum(response, 2);
        byte[] fullResponse = new byte[response.length + 1];
        System.arraycopy(response, 0, fullResponse, 0, response.length);
        fullResponse[response.length] = cs;

        AnswerValues result = (AnswerValues) parseByteMethod.invoke(registry, fullResponse);

        assertNotNull(result);
        assertEquals(1, result.getValues().length);
        assertEquals(1.0, result.getValues()[0], 0.0001); // Как byte 1
        // assertEquals("status", result.getDescription(0));
    }

    @Test
    public void testParseByteResponseWrongLength() throws Exception {
        // Тестовый ответ с неверной длиной (не 6 байт)
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x00, (byte)0x01, (byte)0x01 }; // Без cs

        AnswerValues result = (AnswerValues) parseByteMethod.invoke(registry, response);

        assertNull(result);
    }

    @Test
    public void testParseByteResponseInvalidDataLength() throws Exception {
        // Тестовый ответ с len !=1 для byte
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x00, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x00 }; // len=2, cs неверный

        AnswerValues result = (AnswerValues) parseByteMethod.invoke(registry, response);

        assertNull(result);
    }

    @Test
    public void testValidateChecksumValid() throws Exception {
        // Валидный ответ для float
        float testValue = 5.0f;
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(testValue);
        byte[] data = bb.array();
        byte[] responseWithoutCs = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, data[0], data[1], data[2], data[3] };
        byte cs = registry.calculateChecksum(responseWithoutCs, 2);
        byte[] fullResponse = new byte[responseWithoutCs.length + 1];
        System.arraycopy(responseWithoutCs, 0, fullResponse, 0, responseWithoutCs.length);
        fullResponse[fullResponse.length - 1] = cs;

        boolean result = (boolean) validateChecksumMethod.invoke(registry, fullResponse, true);

        assertTrue(result);
    }

    @Test
    public void testValidateChecksumInvalid() throws Exception {
        // Неверная cs
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, 0x00, 0x00, 0x00, 0x00, (byte)0xFF };

        boolean result = (boolean) validateChecksumMethod.invoke(registry, response, true);

        assertFalse(result);
    }

    @Test
    public void testValidateChecksumShortResponse() throws Exception {
        // Короткий ответ (<5 байт)
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03 };

        boolean result = (boolean) validateChecksumMethod.invoke(registry, response, true);

        assertFalse(result);
    }

    // Дополнительные тесты для edge-кейсов

    @Test
    public void testParseFloatResponseZeroValue() throws Exception {
        // Float = 0.0
        byte[] data = new byte[]{0x00, 0x00, 0x00, 0x00};
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, data[0], data[1], data[2], data[3] };
        byte cs = registry.calculateChecksum(response, 2);
        byte[] fullResponse = new byte[response.length + 1];
        System.arraycopy(response, 0, fullResponse, 0, response.length);
        fullResponse[response.length] = cs;

        AnswerValues result = (AnswerValues) parseFloatMethod.invoke(registry, fullResponse);

        assertNotNull(result);
        assertEquals(0.0, result.getValues()[0], 0.0001);
    }

    @Test
    public void testParseByteResponseOff() throws Exception {
        // Byte = 0 (off)
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x00, (byte)0x01, (byte)0x00 };
        byte cs = registry.calculateChecksum(response, 2);
        byte[] fullResponse = new byte[response.length + 1];
        System.arraycopy(response, 0, fullResponse, 0, response.length);
        fullResponse[response.length] = cs;

        AnswerValues result = (AnswerValues) parseByteMethod.invoke(registry, fullResponse);

        assertNotNull(result);
        assertEquals(0.0, result.getValues()[0], 0.0001);
    }

    @Test
    public void testParseFloatResponseNegativeValue() throws Exception {
        // Отрицательный float (хотя для power supply маловероятно, но тест)
        float testValue = -1.23f;
        ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(testValue);
        byte[] data = bb.array();
        byte[] response = new byte[]{ (byte)0xF0, (byte)0xA1, (byte)0x03, (byte)0x04, data[0], data[1], data[2], data[3] };
        byte cs = registry.calculateChecksum(response, 2);
        byte[] fullResponse = new byte[response.length + 1];
        System.arraycopy(response, 0, fullResponse, 0, response.length);
        fullResponse[response.length] = cs;

        AnswerValues result = (AnswerValues) parseFloatMethod.invoke(registry, fullResponse);

        assertNotNull(result);
        assertEquals(testValue, result.getValues()[0], 0.0001);
    }

    @Test
    public void testParseEmptyResponse() throws Exception {
        // Пустой ответ
        byte[] response = new byte[0];

        AnswerValues resultFloat = (AnswerValues) parseFloatMethod.invoke(registry, response);
        AnswerValues resultByte = (AnswerValues) parseByteMethod.invoke(registry, response);

        assertNull(resultFloat);
        assertNull(resultByte);
    }
}