package org.example.device.protArdMipexEmu;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.SpringContextHolder;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.example.utilites.MyUtilities.*;

/**
 * Command registry for ARD_MIPEX_EMU (Arduino Mipex / multi-mode emulator).
 * Protocol reference: {@code mip_emu.md}.
 * <p>
 * Commands listed under «Список команд, доступных для имитации» are NOT registered here
 * (they are emulated sensor-side protocol, not control-panel commands).
 */
@Slf4j
public class ArdMipexEmuCommandRegistry extends DeviceCommandRegistry {

    private static final int F_RESPONSE_MIN_LENGTH = 71;

    /** Fixed-width F fields (Mipex II layout, mode FMOD 0002). */
    private static final int[][] F_FIELDS = {
            {1,  5, 1},   // Term (raw; display may apply poly)
            {7,  5, 1},   // St
            {13, 5, 1},   // Us
            {19, 5, 1},   // Uref
            {25, 5, 1},   // Stz0
            {31, 5, 1},   // Stz
            {37, 5, 1},   // Stzkt
            {43, 5, 1},   // C0
            {49, 5, 1},   // C1
            {55, 5, 1},   // Status
            {61, 8, 1},   // SerialNumber
    };

    private static final String[] F_UNITS = {
            " Term", " St", " Us", " Uref", " Stz0", " Stz", " Stzkt",
            " C0", " C1", " st", " SN"
    };

    private static final int SERIAL_NUMBER_INDEX = 10;

    private static final Pattern TOGGLE_TF = Pattern.compile(
            "(ShowUnknownCommands|ShowKnownCommands|ShowSendetAnswer)\\s+(T|F)",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void initCommands() {
        // Measurements
        commandList.addCommand(createFCommand());
        commandList.addCommand(ascii("F?", "F? — текущий режим имитации", 20, this::parseModeOrText));
        commandList.addCommand(ascii("CCS", "CCS — концентрация (ASCII / как F)", 80, this::parseCcsResponse));
        commandList.addCommand(createAtCommand());
        commandList.addCommand(ascii("LOG", "LOG — вкл/выкл авто-отправку F", 80, this::parseOkOrText));
        commandList.addCommand(ascii("CONC?", "CONC? — текущая концентрация", 40, this::parseOkOrText));
        commandList.addCommand(ascii("CONST?", "CONST? — параметры устройства", 200, this::parseOkOrText));
        commandList.addCommand(ascii("ID", "ID — ID оптического сенсора", 40, this::parseOkOrText));
        commandList.addCommand(ascii("TERM?", "TERM? — температура (заглушка)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("MMES", "MMES — разовое измерение A4/A5", 80, this::parseOkOrText));
        commandList.addCommand(ascii("GMCV", "GMCV — текущее значение MCV", 20, this::parseNumericEcho));

        // Mode / simulation
        commandList.addCommand(createArg4Command("FMOD", "FMOD 0002 — режим имитатора", 2));
        commandList.addCommand(createArg4Command("CMOD", "CMOD 0006 — вкл/выкл задачу", 6));
        commandList.addCommand(createArg4Command("GMOD", "GMOD 0006 — статус задачи", 6));
        commandList.addCommand(createTmodCommand());
        commandList.addCommand(createArg4Command("SAPR", "SAPR 0001 — точки усреднения ×100", 1));
        commandList.addCommand(createArg4Command("SDAC", "SDAC 1234 — ЦАП / база имитации (0–500)", 100));
        commandList.addCommand(createArg4Command("SMCV", "SMCV 0044 — предел имитации (1–9998)", 44));
        commandList.addCommand(createArg4Command("KALB", "KALB 0020 — принуд. C0/C1 в F", 20));
        commandList.addCommand(createArg4Command("SSTAT", "SSTAT 0031 — статус в F/CCS/@/$", 31));
        commandList.addCommand(createArg4Command("TERM", "TERM xxxx — установка температуры", 2500));
        commandList.addCommand(createArg4Command("CDAC", "CDAC 0000/0001 — форма сигнала (не реализ.)", 0));

        // System
        commandList.addCommand(ascii("SREV?", "SREV? — версия ПО", 40, this::parseSoftwareRev));
        commandList.addCommand(ascii("SRAL?", "SRAL? — серийный номер", 20, this::parseSerial));
        commandList.addCommand(ascii("%**", "%** — запрос адреса", 10, this::parseAddress));
        commandList.addCommand(createS085Command());
        commandList.addCommand(createBangCommand());

        // UART / bridge / debug
        commandList.addCommand(ascii("URTS1", "URTS1 — показ UART1", 40, this::parseUrtsResponse));
        commandList.addCommand(ascii("URTS2", "URTS2 — показ UART2", 40, this::parseUrtsResponse));
        commandList.addCommand(ascii("URTS3", "URTS3 — показ UART3", 40, this::parseUrtsResponse));
        commandList.addCommand(ascii("RSND", "RSND — мост UART1↔UART2", 40, this::parseOkOrText));
        commandList.addCommand(ascii("URT0", "URT0 — тест F → UART0", 40, this::parseOkOrText));
        commandList.addCommand(ascii("URT1", "URT1 — тест F → UART1", 40, this::parseOkOrText));
        commandList.addCommand(ascii("URT2", "URT2 — тест F → UART2", 40, this::parseOkOrText));
        commandList.addCommand(ascii("URT3", "URT3 — тест F → UART3", 40, this::parseOkOrText));
        commandList.addCommand(ascii("PING", "PING — целостность UART1↔UART2", 80, this::parseOkOrText));
        commandList.addCommand(ascii("OSLT", "OSLT — нагрузочный тест UART", 200, this::parseOkOrText));
        commandList.addCommand(ascii("SUCU", "SUCU — показ неизвестных команд", 40, this::parseToggleText));
        commandList.addCommand(ascii("SKCU", "SKCU — показ известных команд", 40, this::parseToggleText));
        commandList.addCommand(ascii("SRKC", "SRKC — показ ответов на команды", 40, this::parseToggleText));
    }

    private SingleCommand ascii(String name, String description, int expectedBytes,
                                java.util.function.Function<byte[], AnswerValues> parser) {
        return new SingleCommand(name, description, parser, expectedBytes);
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "F",
                "F → Term St Us Uref Stz0 Stz Stzkt C0 C1 Status Serial CRC",
                this::parseFResponse,
                72
        );
    }

    /**
     * {@code @} — binary C1: [C1H][C1L][CR], big-endian unsigned 16-bit.
     */
    private SingleCommand createAtCommand() {
        return new SingleCommand(
                "@",
                "@ — концентрация C1 (2 байта BE + CR)",
                this::parseAtResponse,
                3
        );
    }

    private SingleCommand createArg4Command(String name, String description, int defaultValue) {
        SingleCommand command = new SingleCommand(
                name,
                description,
                name,
                String.format(Locale.US, "%s %04d", name, defaultValue).getBytes(StandardCharsets.US_ASCII),
                args -> {
                    int n = ((Number) args.getOrDefault("value", defaultValue)).intValue();
                    return String.format(Locale.US, "%s %04d", name, Math.max(0, Math.min(9999, n)))
                            .getBytes(StandardCharsets.US_ASCII);
                },
                this::parseOkOrText,
                40,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Integer.class, defaultValue, val -> ((Number) val).intValue() >= 0));
        return command;
    }

    /** TMOD xxYY — period (sec 01–99) + task number. */
    private SingleCommand createTmodCommand() {
        SingleCommand command = new SingleCommand(
                "TMOD",
                "TMOD 0305 — период (xx сек) + номер задачи (YY)",
                "TMOD",
                "TMOD 0305".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    int n = ((Number) args.getOrDefault("value", 305)).intValue();
                    n = Math.max(0, Math.min(9999, n));
                    return String.format(Locale.US, "TMOD %04d", n).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseOkOrText,
                40,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Integer.class, 305, val -> ((Number) val).intValue() >= 0));
        return command;
    }

    private SingleCommand createS085Command() {
        SingleCommand command = new SingleCommand(
                "S085",
                "S085xxxxx — смена серийного номера",
                "S085",
                "S08500000".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    int n = ((Number) args.getOrDefault("value", 0)).intValue();
                    n = Math.max(0, Math.min(99999, n));
                    return String.format(Locale.US, "S085%05d", n).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseOkOrText,
                40,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Integer.class, 0, val -> ((Number) val).intValue() >= 0));
        return command;
    }

    private SingleCommand createBangCommand() {
        SingleCommand command = new SingleCommand(
                "!",
                "!xxYY — смена сетевого адреса (xx=текущий, YY=новый)",
                "!",
                "!0203".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    int n = ((Number) args.getOrDefault("value", 203)).intValue();
                    n = Math.max(0, Math.min(9999, n));
                    return String.format(Locale.US, "!%04d", n).getBytes(StandardCharsets.US_ASCII);
                },
                this::parseAddress,
                20,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Integer.class, 203, val -> ((Number) val).intValue() >= 0));
        return command;
    }

    // ─── parsers ───────────────────────────────────────────────────────────

    private AnswerValues parseFResponse(byte[] response) {
        if (response.length < F_RESPONSE_MIN_LENGTH) {
            log.warn("F response too short: {} bytes (expected >= {})", response.length, F_RESPONSE_MIN_LENGTH);
            return null;
        }

        byte expectedCrc = calculateCRCforF(response);
        if (response[70] != expectedCrc) {
            log.warn("CRC mismatch for F: expected 0x{} got 0x{}",
                    String.format("%02X", expectedCrc), String.format("%02X", response[70]));
            return null;
        }

        AnswerValues answerValues = new AnswerValues(F_FIELDS.length);

        for (int i = 0; i < F_FIELDS.length; i++) {
            int offset = F_FIELDS[i][0];
            int length = F_FIELDS[i][1];
            double divisor = F_FIELDS[i][2];
            String unit = F_UNITS[i];

            Double val = parseAsciiFieldChecked(response, offset, length, divisor);
            if (val == null) {
                log.warn("Invalid F field at offset {}: {}", offset, new String(response, offset, length));
                return null;
            }
            // Term: Mipex II polynomial (same as protMipex2)
            if (i == 0) {
                double raw = val;
                val = (raw * raw * -0.00000002) - (raw * 0.0412) + 93.116;
                unit = " °C";
            }
            answerValues.addValue(val, unit);
        }

        double serialNumber = answerValues.getValues()[SERIAL_NUMBER_INDEX];
        String snStr = String.valueOf((long) serialNumber);
        AnswerStorage as = SpringContextHolder.getBean(AnswerStorage.class);
        Integer tab = as != null ? as.getTabByIdent(snStr) : null;
        if (tab != null) {
            answerValues.setDirection(tab);
        }

        return answerValues;
    }

    private AnswerValues parseModeOrText(byte[] response) {
        // F? may return a name ("MIPEX\tII") or a numeric code ("0002")
        String text = toText(response).trim()
                .replace('\t', ' ')
                .replaceAll(" +", " ")
                .trim();
        if (text.isEmpty()) {
            return null;
        }
        // Prefer short pure-numeric answers as mode id
        if (text.matches("\\d{1,4}")) {
            try {
                AnswerValues av = new AnswerValues(1);
                av.addValue(Double.parseDouble(text), " mode");
                return av;
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        // Named mode: store 1.0 with the name in the unit field for UI display
        AnswerValues av = new AnswerValues(1);
        String compact = text.toUpperCase(Locale.ROOT).replace(" ", "").replace("-", "");
        double code = 0;
        if (compact.contains("MIPEX14")) {
            code = 3;
        } else if (compact.contains("MIPEXII") || compact.contains("MIPEX2")) {
            code = 2;
        }
        av.addValue(code, " " + text);
        return av;
    }

    private AnswerValues parseNumericEcho(byte[] response) {
        String text = toText(response).trim();
        try {
            double v = Double.parseDouble(text.replaceAll("[^0-9.+-].*", "").trim());
            AnswerValues av = new AnswerValues(1);
            av.addValue(v, "");
            return av;
        } catch (Exception e) {
            return parseOkOrText(response);
        }
    }

    private AnswerValues parseSoftwareRev(byte[] response) {
        String text = toText(response).trim();
        AnswerValues av = new AnswerValues(1);
        av.addValue(text.length(), " " + text);
        return av;
    }

    private AnswerValues parseSerial(byte[] response) {
        String text = toText(response).trim().replaceAll("[^0-9]", "");
        if (text.isEmpty()) {
            return null;
        }
        AnswerValues av = new AnswerValues(1);
        av.addValue(Double.parseDouble(text), " SN");
        return av;
    }

    private AnswerValues parseAddress(byte[] response) {
        String text = toText(response).trim();
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return parseOkOrText(response);
        }
        AnswerValues av = new AnswerValues(1);
        av.addValue(Double.parseDouble(digits), " addr");
        return av;
    }

    private AnswerValues parseToggleText(byte[] response) {
        String text = toText(response).trim();
        Matcher m = TOGGLE_TF.matcher(text);
        if (m.find()) {
            AnswerValues av = new AnswerValues(1);
            av.addValue("T".equalsIgnoreCase(m.group(2)) ? 1.0 : 0.0, " " + m.group(1));
            return av;
        }
        return parseOkOrText(response);
    }

    /**
     * URTS: {@code Events on UART1 will be showed:F} / {@code ...:T}
     */
    private AnswerValues parseUrtsResponse(byte[] response) {
        String text = toText(response).trim();
        Matcher m = Pattern.compile("showed\\s*:\\s*([TF])", Pattern.CASE_INSENSITIVE).matcher(text);
        if (m.find()) {
            AnswerValues av = new AnswerValues(1);
            boolean on = "T".equalsIgnoreCase(m.group(1));
            av.addValue(on ? 1.0 : 0.0, on ? " ON" : " OFF");
            return av;
        }
        return parseOkOrText(response);
    }

    /**
     * CCS often mirrors F layout; if not, try tab/space-separated fields and take C1-like token.
     */
    private AnswerValues parseCcsResponse(byte[] response) {
        AnswerValues f = parseFResponse(response);
        if (f != null) {
            return f;
        }
        String text = toText(response).trim();
        if (text.isEmpty()) {
            return null;
        }
        // Prefer last 5-digit concentration-like token
        Matcher m = Pattern.compile("(\\d{4,5})").matcher(text);
        String last = null;
        while (m.find()) {
            last = m.group(1);
        }
        if (last != null) {
            AnswerValues av = new AnswerValues(1);
            av.addValue(Double.parseDouble(last), " C1");
            return av;
        }
        return parseOkOrText(response);
    }

    /**
     * Binary {@code @}: two-byte big-endian concentration, optional trailing CR.
     */
    private AnswerValues parseAtResponse(byte[] response) {
        if (response == null || response.length < 2) {
            return null;
        }
        int len = response.length;
        if (response[len - 1] == 0x0D || response[len - 1] == 0x0A) {
            len--;
        }
        if (len < 2) {
            return null;
        }
        int conc = ((response[0] & 0xFF) << 8) | (response[1] & 0xFF);
        AnswerValues av = new AnswerValues(1);
        av.addValue(conc, " C1 (@)");
        return av;
    }

    private AnswerValues parseOkOrText(byte[] response) {
        String text = toText(response).trim();
        if (text.isEmpty()) {
            return null;
        }
        AnswerValues av = new AnswerValues(1);
        boolean ok = text.toUpperCase(Locale.ROOT).startsWith("OK")
                || text.equalsIgnoreCase("Ok");
        av.addValue(ok ? 1.0 : 0.0, " " + (text.length() > 80 ? text.substring(0, 80) + "…" : text));
        return av;
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
