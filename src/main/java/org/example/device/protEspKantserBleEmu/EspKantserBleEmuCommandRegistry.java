package org.example.device.protEspKantserBleEmu;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command registry for ESP_KANTSER_BLE_EMU (ESP32 Kantser BLE simulator).
 * Link params: 115200 8N1, CR.
 * <p>
 * The firmware echoes the command ("\n> CMD ARGS\n") before printing the reply.
 * All BLE GET parameters (VE?, GS?, AA0?…QH?) are also reachable over serial;
 * sending any of them with an argument stores the value and answers "OK".
 */
@Slf4j
public class EspKantserBleEmuCommandRegistry extends DeviceCommandRegistry {

    private static final Pattern MAC_PATTERN = Pattern.compile(
            "[0-9A-Fa-f]{2}([:-])[0-9A-Fa-f]{2}(\\1[0-9A-Fa-f]{2}){4}");
    private static final Pattern CONNECTED_PATTERN = Pattern.compile(
            "Connected\\s*:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);
    private static final Pattern ADVERTISING_PATTERN = Pattern.compile(
            "Advertising\\s*:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCANNING_PATTERN = Pattern.compile(
            "Scanning\\s*:\\s*(YES|NO)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OK_PATTERN = Pattern.compile(
            "\\b(OK|Ok)\\b", Pattern.CASE_INSENSITIVE);

    /** BLE GET/SET parameters reachable over serial, in firmware order. */
    private static final String[][] BLE_PARAMS = {
            {"VE?",  "1.079", "Firmware version"},
            {"GS?",  "58:2A:BD:7C:88:D6", "MAC address"},
            {"DT?",  "20", "Device type"},
            {"BD?",  "20", "Baud rate"},
            {"BO?",  "15", "BO param"},
            {"BB?",  "50", "BB param"},
            {"Bb?",  "0", "Bb param"},
            {"Bs?",  "78", "Bs param"},
            {"AA0?", "112233445566", "AA0 param"},
            {"AA1?", "AA0000000002", "AA1 param"},
            {"AA2?", "AA0000000003", "AA2 param"},
            {"AA3?", "AA0000000004", "AA3 param"},
            {"AA4?", "AA0000000005", "AA4 param"},
            {"AA5?", "AA0000000006", "AA5 param"},
            {"AA6?", "AA0000000007", "AA6 param"},
            {"AA7?", "AA0000000008", "AA7 param"},
            {"AW0?", "223344556677", "AW0 param"},
            {"AW1?", "AB0000000002", "AW1 param"},
            {"AW2?", "AB0000000003", "AW2 param"},
            {"AW3?", "AB0000000004", "AW3 param"},
            {"AW4?", "AB0000000005", "AW4 param"},
            {"AW5?", "AB0000000006", "AW5 param"},
            {"AW6?", "AB0000000007", "AW6 param"},
            {"AW7?", "AB0000000008", "AW7 param"},
            {"APON?",  "150", "APON param"},
            {"APOFF?", "150", "APOFF param"},
            {"BAON?",  "200", "BAON param"},
            {"BAOFF?", "200", "BAOFF param"},
            {"BLON?",  "100", "BLON param"},
            {"BLOFF?", "100", "BLOFF param"},
            {"BF?",   "2000", "BF param"},
            {"LD?",   "30", "LD param"},
            {"LB?",   "57600", "LB param"},
            {"GB?",   "57600", "GB param"},
            {"LWM?",  "2", "LWM param"},
            {"LC?",   "NARUTO", "LC param"},
            {"LWJ?",  "0123456789ABCDEF0123456789ABCDEF", "LWJ param"},
            {"LWD?",  "0123456789ABCDEF", "LWD param"},
            {"LWA?",  "0123456789ABCDEF", "LWA param"},
            {"LWd?",  "12345678", "LWd param"},
            {"LWS?",  "0123456789ABCDEF0123456789ABCDEF", "LWS param"},
            {"LWN?",  "0123456789ABCDEF0123456789ABCDEF", "LWN param"},
            {"LWr?",  "5", "LWr param"},
            {"LWj?",  "3", "LWj param"},
            {"LWs?",  "2", "LWs param"},
            {"LWB?",  "2", "LWB param"},
            {"LL?",   "OK", "LL param"},
            {"LR?",   "12", "LR param"},
            {"LV?",   "RAK811 V3.0.0", "LV param"},
            {"GF?",   "0", "GF param"},
            {"GA?",   "2", "GA param"},
            {"Gs?",   "1", "Gs param"},
            {"IO?",   "42", "IO param"},
            {"BW?",   "5", "BW param"},
            {"Ll?",   "0", "Ll param"},
            {"Bf?",   "1000", "Bf param"},
            {"BM?",   "100", "BM param"},
            {"BZ?",   "0", "BZ param"},
            {"AR?",   "888", "AR param"},
            {"IT?",   "MULTIGAS", "IT param"},
            {"IS?",   "IGM-TEST-001", "IS param"},
            {"IC?",   "Test Device", "IC param"},
            {"ID?",   "2025-06-01", "ID param"},
            {"IH?",   "2.1", "IH param"},
            {"QA?",   "5", "QA param"},
            {"QC?",   "1", "QC param"},
            {"QD?",   "50", "QD param"},
            {"QE?",   "80", "QE param"},
            {"QF?",   "8", "QF param"},
            {"QG?",   "12450", "QG param"},
            {"QH?",   "1", "QH param"},
    };

    @Override
    protected void initCommands() {
        // Терминальные / справочные
        commandList.addCommand(createQuery("HELP", "HELP — список команд", 4000, this::parseText));
        commandList.addCommand(createQuery("GDUI?", "GDUI? — описание устройства", 80, this::parseText));
        commandList.addCommand(createQuery("SREV?", "SREV? — версия симулятора", 40, this::parseText));
        commandList.addCommand(createQuery("BLST?", "BLST? — статус BLE (Connected/Adv/Scanning)", 100, this::parseBleStatus));
        commandList.addCommand(createQuery("ADST?", "ADST? — текущий adv-пакет", 150, this::parseText));

        // Установка параметров имитации
        commandList.addCommand(createNumericSet("SCH1", "SCH1 — концентрация CH1", 0));
        commandList.addCommand(createNumericSet("SCH2", "SCH2 — концентрация CH2", 0));
        commandList.addCommand(createNumericSet("SCH3", "SCH3 — концентрация CH3", 0));
        commandList.addCommand(createNumericSet("SCH4", "SCH4 — концентрация CH4", 0));
        commandList.addCommand(createNumericSet("STER", "STER — смещение температуры", 0));

        // Управление
        commandList.addCommand(createQuery("ADVE", "ADVE — реклама BLE ON", 20, this::parseText));
        commandList.addCommand(createQuery("ADVD", "ADVD — реклама BLE OFF", 20, this::parseText));
        commandList.addCommand(createQuery("REBT", "REBT — перезагрузка устройства", 100, this::parseText));

        // Служебные / MAC
        commandList.addCommand(createQuery("LRBC", "LRBC — последняя принятая BLE команда", 120, this::parseText));
        commandList.addCommand(createQuery("LSBA", "LSBA — последний отправленный BLE ответ (hex)", 120, this::parseText));
        commandList.addCommand(createQuery("LLBA", "LLBA — последняя принятая BLE adv", 120, this::parseText));
        commandList.addCommand(createQuery("CMMD", "CMMD — MAC мастера", 40, this::parseMac));
        commandList.addCommand(createMacSet());
        commandList.addCommand(createQuery("GMAC", "GMAC — MAC устройства", 40, this::parseMac));

        // BLE GET/SET параметры, доступные и через serial
        for (String[] p : BLE_PARAMS) {
            commandList.addCommand(createQuery(p[0],
                    p[0] + " — " + p[2] + " (default: " + p[1] + ")", 40, this::parseText));
        }
    }

    private SingleCommand createQuery(String name, String description, int expectedBytes,
                                      java.util.function.Function<byte[], AnswerValues> parser) {
        return new SingleCommand(name, description, parser, expectedBytes);
    }

    private SingleCommand createNumericSet(String name, String description, int defaultValue) {
        SingleCommand command = new SingleCommand(
                name,
                description,
                name,
                (name + " 0").getBytes(StandardCharsets.US_ASCII),
                args -> {
                    double v = ((Number) args.getOrDefault("value", defaultValue)).doubleValue();
                    return (name + " " + formatNumber(v)).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseText,
                20,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Double.class, defaultValue, val -> ((Number) val).doubleValue() >= 0));
        return command;
    }

    private SingleCommand createMacSet() {
        SingleCommand command = new SingleCommand(
                "SMAC",
                "SMAC A4:CF:12:34:56:78 — задать MAC мастера",
                "SMAC",
                "SMAC 00:00:00:00:00:00".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    String mac = String.valueOf(args.getOrDefault("mac", "00:00:00:00:00:00"));
                    return ("SMAC " + mac).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseText,
                20,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "mac", String.class, "00:00:00:00:00:00",
                val -> MAC_PATTERN.matcher(String.valueOf(val)).find()));
        return command;
    }

    private static String formatNumber(double v) {
        if (Math.rint(v) == v && Math.abs(v) < 1e15) {
            return String.format(Locale.US, "%.0f", v);
        }
        return String.format(Locale.US, "%s", v);
    }

    // ─── parsers ───────────────────────────────────────────────────────────

    private AnswerValues parseText(byte[] response) {
        String text = toText(response).trim();
        if (text.isEmpty()) {
            return null;
        }
        AnswerValues av = new AnswerValues(1);
        boolean ok = OK_PATTERN.matcher(text).find();
        av.addValue(ok ? 1.0 : 0.0, " " + (text.length() > 80 ? text.substring(0, 80) + "…" : text));
        return av;
    }

    private AnswerValues parseBleStatus(byte[] response) {
        String text = toText(response);
        Matcher connected = CONNECTED_PATTERN.matcher(text);
        Matcher advertising = ADVERTISING_PATTERN.matcher(text);
        Matcher scanning = SCANNING_PATTERN.matcher(text);
        boolean hasConnected = connected.find();
        boolean hasAdv = advertising.find();
        boolean hasScan = scanning.find();
        if (hasConnected || hasAdv || hasScan) {
            AnswerValues av = new AnswerValues(3);
            av.addValue(hasConnected && "YES".equalsIgnoreCase(connected.group(1)) ? 1.0 : 0.0, " Connected");
            av.addValue(hasAdv && "YES".equalsIgnoreCase(advertising.group(1)) ? 1.0 : 0.0, " Advertising");
            av.addValue(hasScan && "YES".equalsIgnoreCase(scanning.group(1)) ? 1.0 : 0.0, " Scanning");
            return av;
        }
        return parseText(response);
    }

    private AnswerValues parseMac(byte[] response) {
        String text = toText(response).trim();
        Matcher m = MAC_PATTERN.matcher(text);
        if (m.find()) {
            AnswerValues av = new AnswerValues(1);
            av.addValue(1.0, " " + m.group(0));
            return av;
        }
        return parseText(response);
    }

    private static String toText(byte[] response) {
        if (response == null || response.length == 0) {
            return "";
        }
        int end = response.length;
        while (end > 0 && (response[end - 1] == '\r' || response[end - 1] == '\n' || response[end - 1] == 0)) {
            end--;
        }
        return new String(response, 0, end, StandardCharsets.US_ASCII);
    }
}
