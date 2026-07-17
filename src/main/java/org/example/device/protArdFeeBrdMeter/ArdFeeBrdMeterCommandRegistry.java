package org.example.device.protArdFeeBrdMeter;

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
 * Command registry for ARD_FEE_BRD_METER (CCM Fee Board / CurMeter).
 * Protocol reference: {@code ccm_fee.md}.
 */
@Slf4j
public class ArdFeeBrdMeterCommandRegistry extends DeviceCommandRegistry {

    private static final int F_RESPONSE_MIN_LENGTH = 71;

    private static final int[][] F_FIELDS = {
            {1,  5, 100},   // TermBM    °C
            {7,  5,  10},   // PresBM    mmHg
            {13, 5,   1},   // hydmBM    %
            {19, 5, 100},   // thre_V    V (pwr)
            {25, 5, 1000},  // cur_One   A
            {31, 5, 1000},  // cur_Two   A
            {37, 5, 1000},  // c_One_Poly A
            {43, 5, 1000},  // c_Two_Poly A
            {49, 5, 1000},  // currResF  A
            {55, 5,    1},  // stat      st
            {61, 8,    1},  // serialNumber SN
    };

    private static final String[] F_UNITS = {
            " °C", " mmHg", " %", " V (pwr)", " A", " A", " A", " A", " A", " st", " SN"
    };

    private static final int SERIAL_NUMBER_INDEX = 10;

    private static final Pattern CONC_PATTERN = Pattern.compile(
            "FEE:([\\d.+-]+)\\s+CUR:([\\d.+-]+)\\s+TER:([\\d.+-]+)\\s+HUD:([\\d.+-]+)\\s+PRS:([\\d.+-]+)\\s+PWR:([\\d.+-]+)",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern FPWR_PATTERN = Pattern.compile(
            "feePWR:\\s*(ON|OFF)", Pattern.CASE_INSENSITIVE);

    @Override
    protected void initCommands() {
        // Binary/fixed-width measurement
        commandList.addCommand(createFCommand());

        // Measurements (ASCII text)
        commandList.addCommand(ascii("MMESU", "MMESU — быстрые показания", 200, this::parseMmesuLike));
        commandList.addCommand(ascii("LOGO", "LOGO — лог один раз", 220, this::parseMmesuLike));
        commandList.addCommand(ascii("LOG", "LOG — вкл/выкл авто-лог", 220, this::parseMmesuLike));
        commandList.addCommand(ascii("CONC?", "CONC? — основные показатели", 80, this::parseConcResponse));

        // Power
        commandList.addCommand(ascii("FPWR?", "FPWR? — статус питания потребителя", 40, this::parseFpwrResponse));
        commandList.addCommand(ascii("FPWR", "FPWR — вкл/выкл питание потребителя", 40, this::parseFpwrResponse));
        commandList.addCommand(ascii("SENSON", "SENSON — мост плата↔потребитель ON", 20, this::parseOkOrText));
        commandList.addCommand(ascii("SENSOFF", "SENSOFF — мост плата↔потребитель OFF", 20, this::parseOkOrText));

        // System
        commandList.addCommand(ascii("GCOEF", "GCOEF — коэффициенты / системные настройки", 800, this::parseOkOrText));
        commandList.addCommand(ascii("REBOOT", "REBOOT — программная перезагрузка", 300, this::parseOkOrText));
        commandList.addCommand(ascii("SREV?", "SREV? — версия ПО", 40, this::parseSoftwareRev));
        commandList.addCommand(ascii("SRAL?", "SRAL? — серийный номер", 20, this::parseSerial));
        commandList.addCommand(ascii("%**", "%** — запрос адреса", 10, this::parseAddress));

        // Averaging steps (with argument)
        commandList.addCommand(createSlasCommand());
        commandList.addCommand(createSdasCommand());

        // Multi-step calibration entry points (device answers Ok then waits for values)
        commandList.addCommand(ascii("SPOLY0", "SPOLY0 — задать полином CH0 (далее 6 коэфф.)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("SPOLY1", "SPOLY1 — задать полином CH1 (далее 6 коэфф.)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("SVOLT0", "SVOLT0 — нули АЦП (далее 3 значения)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("STRGLV", "STRGLV — trigger level (далее 1 значение)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("SV2AMP", "SV2AMP — V→A коэффициенты (далее 2 значения)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("SCABD", "SCABD — дата калибровки (далее ДД ММ ГГГГ)", 20, this::parseOkOrText));
        commandList.addCommand(ascii("URTMOD", "URTMOD — скорость UART (далее baud)", 20, this::parseOkOrText));
    }

    private SingleCommand ascii(String name, String description, int expectedBytes,
                                java.util.function.Function<byte[], AnswerValues> parser) {
        return new SingleCommand(name, description, parser, expectedBytes);
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "F",
                "F → TermBM PresBM HydmBM Thre_V Cur_One Cur_Two C_One_Poly C_Two_Poly CurrResF Stat SerialNumber CRC",
                this::parseFResponse,
                72
        );
    }

    private SingleCommand createSlasCommand() {
        SingleCommand command = new SingleCommand(
                "SLAS",
                "SLAS 0010 — усреднение для LOG/LOGO (LogAproxStep_K1)",
                "SLAS",
                "SLAS 0010".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    int n = ((Number) args.getOrDefault("value", 10)).intValue();
                    return String.format(Locale.US, "SLAS %04d", Math.max(0, Math.min(9999, n)))
                            .getBytes(StandardCharsets.US_ASCII);
                },
                this::parseNumericEcho,
                16,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Integer.class, 10, val -> ((Number) val).intValue() >= 0));
        return command;
    }

    private SingleCommand createSdasCommand() {
        SingleCommand command = new SingleCommand(
                "SDAS",
                "SDAS 0010 — усреднение для MMESU/фона (LogAproxStep_K2)",
                "SDAS",
                "SDAS 0010".getBytes(StandardCharsets.US_ASCII),
                args -> {
                    int n = ((Number) args.getOrDefault("value", 10)).intValue();
                    return String.format(Locale.US, "SDAS %04d", Math.max(0, Math.min(9999, n)))
                            .getBytes(StandardCharsets.US_ASCII);
                },
                this::parseNumericEcho,
                16,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "value", Integer.class, 10, val -> ((Number) val).intValue() >= 0));
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

    /**
     * MMESU / LOGO / LOG — tab-separated values with units, e.g.
     * {@code 0.011\tV\t-0.001\tV\t...}
     */
    private AnswerValues parseMmesuLike(byte[] response) {
        String text = toText(response).trim();
        if (text.isEmpty()) {
            return null;
        }
        // Strip optional leading "Ok " / "OK "
        if (text.regionMatches(true, 0, "OK ", 0, 3)) {
            text = text.substring(3).trim();
        }

        String[] tokens = text.split("[\\t ]+");
        // Expect pairs: value unit; FPWR field is bare ON/OFF without a unit token
        java.util.List<Double> vals = new java.util.ArrayList<>();
        java.util.List<String> units = new java.util.ArrayList<>();
        for (int i = 0; i < tokens.length; ) {
            String tok = tokens[i];
            if ("ON".equalsIgnoreCase(tok) || "OFF".equalsIgnoreCase(tok)) {
                vals.add("ON".equalsIgnoreCase(tok) ? 1.0 : 0.0);
                units.add(" FPWR");
                i++;
                continue;
            }
            if (i + 1 >= tokens.length) {
                break;
            }
            try {
                vals.add(Double.parseDouble(tok.replace(',', '.')));
                units.add(" " + tokens[i + 1]);
                i += 2;
            } catch (NumberFormatException ex) {
                i++;
            }
        }
        if (vals.isEmpty()) {
            return parseOkOrText(response);
        }
        AnswerValues av = new AnswerValues(vals.size());
        for (int i = 0; i < vals.size(); i++) {
            av.addValue(vals.get(i), units.get(i));
        }
        return av;
    }

    private AnswerValues parseConcResponse(byte[] response) {
        String text = toText(response).trim();
        Matcher m = CONC_PATTERN.matcher(text);
        if (!m.find()) {
            log.warn("CONC? parse failed: {}", text);
            return null;
        }
        AnswerValues av = new AnswerValues(6);
        av.addValue(Double.parseDouble(m.group(1)), " FEE");
        av.addValue(Double.parseDouble(m.group(2)), " CUR mA");
        av.addValue(Double.parseDouble(m.group(3)), " TER °C");
        av.addValue(Double.parseDouble(m.group(4)), " HUD %");
        av.addValue(Double.parseDouble(m.group(5)), " PRS");
        av.addValue(Double.parseDouble(m.group(6)), " PWR V");
        return av;
    }

    private AnswerValues parseFpwrResponse(byte[] response) {
        String text = toText(response).trim();
        Matcher m = FPWR_PATTERN.matcher(text);
        if (!m.find()) {
            return parseOkOrText(response);
        }
        AnswerValues av = new AnswerValues(1);
        av.addValue("ON".equalsIgnoreCase(m.group(1)) ? 1.0 : 0.0, " feePWR");
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
        // keep as opaque: store hash of version string length for graph, unit holds text
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
        // !02
        String digits = text.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return parseOkOrText(response);
        }
        AnswerValues av = new AnswerValues(1);
        av.addValue(Double.parseDouble(digits), " addr");
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
        // Drop trailing CR/LF and non-printables at ends
        int end = response.length;
        while (end > 0 && (response[end - 1] == '\r' || response[end - 1] == '\n' || response[end - 1] == 0)) {
            end--;
        }
        return new String(response, 0, end, StandardCharsets.US_ASCII);
    }
}
