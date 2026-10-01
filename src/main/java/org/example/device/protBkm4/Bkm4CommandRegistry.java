package org.example.device.protBkm4;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Command registry for БКМ-4 (блок коммутации, RS-232C, 9600 8N1, CR).
 * Protocol reference: {@code bkm4.md}.
 * <p>
 * Команды — ASCII, начинаются с {@code &}, ответы — с {@code @}, завершаются CR.
 * Все команды возвращают число, которое парсер извлекает из ответа.
 */
@Slf4j
public class Bkm4CommandRegistry extends DeviceCommandRegistry {

    @Override
    protected void initCommands() {
        // Режим работы: &A? / &A0 / &A1 → @A0 / @A1
        commandList.addCommand(createOneDigitCommand("A", "&A? / &A0 (ручн.) / &A1 (внешн.) — режим работы", "Режим", 0, 1, 5));
        // Клапаны: &V? / &V0..&V4 → @V0..@V4
        commandList.addCommand(createOneDigitCommand("V", "&V? / &V0 (все выкл) / &V1..&V4 — газовый клапан", "Клапан", 0, 4, 5));
        // Расход (уставка): &S? / &Sxxxx (0..3000) → @Sxxxx
        commandList.addCommand(createFlowCommand("S", "&S? / &Sxxxx (0..3000) — уставка расхода", "мл/мин", 8));
        // Фактический расход: &F? → @Fxxxx
        commandList.addCommand(createFlowCommand("F", "&F? — текущий (фактический) расход", "мл/мин", 8));
        // Генерация потока: &G? / &G0 / &G1 → @G0 / @G1
        commandList.addCommand(createOneDigitCommand("G", "&G? / &G0 (выкл) / &G1 (вкл) — генерация потока", "Генерация", 0, 1, 5));
    }

    /**
     * Команда с одним параметром-цифрой: режим A (0..1), клапан V (0..4),
     * генерация G (0..1). Принимает запрос {@code &X?} и установку {@code &Xn}.
     */
    private SingleCommand createOneDigitCommand(String letter, String description,
                                                String unit, int min, int max, int expectedBytes) {
        String base = "&" + letter;
        return new SingleCommand(
                letter,
                description,
                letter,
                (base + "?").getBytes(StandardCharsets.US_ASCII),
                args -> {
                    Object v = args.get("value");
                    if (v == null) {
                        return (base + "?").getBytes(StandardCharsets.US_ASCII);
                    }
                    int n = ((Number) v).intValue();
                    return (base + Math.max(min, Math.min(max, n))).getBytes(StandardCharsets.US_ASCII);
                },
                bytes -> parseNumericAnswer(bytes, unit),
                expectedBytes,
                CommandType.ASCII);
    }

    /**
     * Команда расхода (0..3000 мл/мин): уставка S и факт F.
     * Принимает запрос {@code &X?} и установку {@code &Xxxxx} (4 разряда).
     */
    private SingleCommand createFlowCommand(String letter, String description,
                                            String unit, int expectedBytes) {
        String base = "&" + letter;
        return new SingleCommand(
                letter,
                description,
                letter,
                (base + "?").getBytes(StandardCharsets.US_ASCII),
                args -> {
                    Object v = args.get("value");
                    if (v == null) {
                        return (base + "?").getBytes(StandardCharsets.US_ASCII);
                    }
                    int n = ((Number) v).intValue();
                    n = Math.max(0, Math.min(3000, n));
                    return (base + String.format(Locale.US, "%04d", n)).getBytes(StandardCharsets.US_ASCII);
                },
                bytes -> parseNumericAnswer(bytes, unit),
                expectedBytes,
                CommandType.ASCII);
    }

    /**
     * Извлекает из ответа {@code @X<число><CR>} числовое значение.
     * Обрабатывает и целые (расход), и дробные (фактический расход с флуктуацией).
     */
    private static AnswerValues parseNumericAnswer(byte[] response, String unit) {
        if (response == null || response.length == 0) {
            return null;
        }
        String text = new String(response, StandardCharsets.US_ASCII).trim();
        if (text.toUpperCase(Locale.ROOT).contains("ERROR")) {
            log.info("BKM-4: получен @ERROR: {}", text);
            return null;
        }
        // Оставляем только цифры, минус и десятичную точку
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isDigit(c) || c == '-' || c == '.' || c == ',') {
                digits.append(c);
            }
        }
        if (digits.length() == 0) {
            return null;
        }
        try {
            double value = Double.parseDouble(digits.toString().replace(',', '.'));
            AnswerValues av = new AnswerValues(1);
            av.addValue(value, unit);
            return av;
        } catch (NumberFormatException e) {
            log.warn("BKM-4: не удалось разобрать ответ '{}'", text);
            return null;
        }
    }
}
