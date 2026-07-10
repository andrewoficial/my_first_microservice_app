package parsers.com.mipex;

import org.example.device.command.SingleCommand;
import org.example.device.protMipex2.Mipex2;
import org.example.device.TemplatedAscii;
import org.example.gui.mainWindowUtilites.CommandFieldFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Регрессионный тест: для TemplatedAscii-приборов (Mipex2) команда в текстовое
 * поле терминала должна попадать как читаемый ASCII ("ZERO2", "CALB 0225"),
 * а НЕ как hex-строка. Раньше подставлялся hex всем без разбора, из-за чего
 * прибор получал на шину literёнал вида "5A 45 52 4F 32".
 */
@DisplayName("Mipex2: команда в поле терминала должна быть ASCII, а не hex")
public class Mipex2CommandFieldTest {

    @Test
    @DisplayName("Mipex2 реализует TemplatedAscii")
    void mipex2IsTemplatedAscii() {
        assertTrue(new Mipex2() instanceof TemplatedAscii,
                "Mipex2 обязан быть TemplatedAscii, иначе команда уйдёт в hex-ветку");
    }

    @Test
    @DisplayName("ZERO2 -> в поле кладётся 'ZERO2', не hex")
    void zero2AsAscii() {
        SingleCommand zero2 = findCommand("ZERO2");
        byte[] built = zero2.build(new HashMap<>());

        String field = CommandFieldFormatter.toFieldText(built, true);

        assertEquals("ZERO2", field);
        assertFalse(field.matches("([0-9A-F]{2}\\s?)+"),
                "Поле не должно быть hex-строкой");
    }

    @Test
    @DisplayName("CALB 2.25 -> в поле кладётся 'CALB 0225', не hex")
    void calbAsAscii() {
        SingleCommand calb = findCommand("setConc");
        Map<String, Object> args = new HashMap<>();
        args.put("value", 2.25f);
        byte[] built = calb.build(args);

        String field = CommandFieldFormatter.toFieldText(built, true);

        assertEquals("CALB 0225", field);
    }

    @Test
    @DisplayName("NonAscii-ветка по-прежнему форматирует в hex")
    void nonAsciiStaysHex() {
        byte[] bytes = new byte[]{0x10, 0x13, 0x01};
        String field = CommandFieldFormatter.toFieldText(bytes, false);
        assertEquals("10 13 01", field);
    }

    @Test
    @DisplayName("null -> пустая строка, без NPE")
    void nullSafe() {
        assertEquals("", CommandFieldFormatter.toFieldText(null, true));
        assertEquals("", CommandFieldFormatter.toFieldText(null, false));
    }

    @Test
    @DisplayName("Байты команды ZERO2 действительно ASCII 'ZERO2'")
    void zero2BuilderProducesAsciiBytes() {
        SingleCommand zero2 = findCommand("ZERO2");
        byte[] built = zero2.build(new HashMap<>());
        assertArrayEquals("ZERO2".getBytes(StandardCharsets.US_ASCII), built);
    }

    private SingleCommand findCommand(String mapKey) {
        Mipex2 device = new Mipex2();
        SingleCommand cmd = device.getCommandListClass().getCommandPool().get(mapKey);
        assertNotNull(cmd, "Команда '" + mapKey + "' должна быть в реестре Mipex2");
        return cmd;
    }
}
