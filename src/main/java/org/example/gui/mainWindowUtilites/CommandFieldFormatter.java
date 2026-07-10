package org.example.gui.mainWindowUtilites;

import org.example.device.TemplatedAscii;
import org.example.utilites.MyUtilities;

import java.nio.charset.StandardCharsets;

/**
 * Решает, в каком виде положить построенную команду в текстовое поле терминала.
 *
 * <p>Ключевое различие:
 * <ul>
 *   <li>{@link TemplatedAscii}-устройства (например Mipex2) отправляют на шину
 *       именно ASCII-содержимое текстового поля. Значит туда должна попасть
 *       читаемая команда ("ZERO2", "CALB 0225"), а не её hex-представление.</li>
 *   <li>NonAscii (бинарные, напр. Modbus) отправляют отдельный rawCommand,
 *       а текстовое поле служит лишь для отображения — там hex.</li>
 * </ul>
 *
 * Ошибка регрессии заключалась в том, что hex подставлялся всем без разбора,
 * из-за чего TemplatedAscii-приборы получали на шину literёнал hex-строки.
 */
public final class CommandFieldFormatter {

    private CommandFieldFormatter() {
    }

    /**
     * @param commandBytes    байты, построенные билдером команды
     * @param isTemplatedAscii true, если устройство реализует {@link TemplatedAscii}
     * @return строка для текстового поля терминала
     */
    public static String toFieldText(byte[] commandBytes, boolean isTemplatedAscii) {
        if (commandBytes == null) {
            return "";
        }
        if (isTemplatedAscii) {
            return new String(commandBytes, StandardCharsets.US_ASCII);
        }
        return MyUtilities.bytesToHexString(commandBytes);
    }
}
