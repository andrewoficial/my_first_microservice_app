package org.example.device.protEdwardsD397;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;
import java.util.regex.Pattern;

/**
 * Реестр команд для Edwards TIC (D397).
 * Поддержка протокола из serial-communications-manual-D397-30-880.
 *
 * Основные Object ID (из Table 1 manual):
 * 902 - TIC Status
 * 904 - Turbo Pump (статус, on/off, setups)
 * 905-909 - Turbo speed/power/normal/standby/cycle
 * 910-912 - Backing pump
 * 913-936 - Gauges 1..6 (в т.ч. 6-gauge версии)
 * 916-918,937-939 - Relays
 * 919,920 - Temperatures
 * 921-924 - Analogue, Vent, Heater, Air cooler
 * 940 - Gauge Values (оптимизированный запрос всех датчиков)
 */
@Slf4j
public class EdwardsD397CommandRegistry extends DeviceCommandRegistry {

    private static final Pattern RESPONSE_PREFIX = Pattern.compile("^[=#\\*][CVS]\\s*(\\d{3,5})");
    private static final Pattern MULTIDROP_PREFIX = Pattern.compile("^#\\d{2}:\\d{2}");

    public EdwardsD397CommandRegistry() {
        initCommands();
    }

    @Override
    protected void initCommands() {
        // === СИСТЕМНЫЙ СТАТУС ===
        commandList.addCommand(create902StatusCmd());
        commandList.addCommand(create902InfoCmd());

        // === ТУРБОНАСОС ===
        commandList.addCommand(create904StatusCmd());
        commandList.addCommand(create904OnOffCmd(true));
        commandList.addCommand(create904OnOffCmd(false));
        commandList.addCommand(create904SetupSlaveCmd());
        commandList.addCommand(create904StartDelayCmd());

        // === ТУРБО СКОРОСТЬ / МОЩНОСТЬ ===
        commandList.addCommand(create905SpeedCmd());
        commandList.addCommand(create906PowerCmd());
        commandList.addCommand(create907NormalCmd());
        commandList.addCommand(create908StandbyCmd());
        commandList.addCommand(create908SetStandbyCmd(true));
        commandList.addCommand(create908SetStandbyCmd(false));
        commandList.addCommand(create909CycleTimeCmd());

        // === БЭКИНГ ===
        commandList.addCommand(create910StatusCmd());
        commandList.addCommand(create910OnOffCmd(true));
        commandList.addCommand(create910OnOffCmd(false));
        commandList.addCommand(create910SequenceCmd());

        // === ДАТЧИКИ ДАВЛЕНИЯ (GAUGES) ===
        commandList.addCommand(createGaugeStatusCmd(913));
        commandList.addCommand(createGaugeStatusCmd(914));
        commandList.addCommand(createGaugeStatusCmd(915));
        commandList.addCommand(createGaugeStatusCmd(934));
        commandList.addCommand(createGaugeStatusCmd(935));
        commandList.addCommand(createGaugeStatusCmd(936));

        commandList.addCommand(create940AllGaugesCmd());

        commandList.addCommand(createGaugeOnOffCmd(913, true));
        commandList.addCommand(createGaugeOnOffCmd(913, false));
        commandList.addCommand(createGaugeZeroCmd(913));
        commandList.addCommand(createGaugeCalCmd(913));
        commandList.addCommand(createGaugeDegasCmd(913));
        commandList.addCommand(createGaugeNewIdCmd(913));

        // === РЕЛЕ ===
        commandList.addCommand(createRelayStatusCmd(916));
        commandList.addCommand(createRelayStatusCmd(917));
        commandList.addCommand(createRelayStatusCmd(918));
        commandList.addCommand(createRelayOnOffCmd(916, true));
        commandList.addCommand(createRelayOnOffCmd(916, false));

        // === ТЕМПЕРАТУРЫ ===
        commandList.addCommand(create919PsTempCmd());
        commandList.addCommand(create920InternalTempCmd());

        // === ДРУГИЕ ВАЖНЫЕ ===
        commandList.addCommand(create921AnalogueOutCmd());
        commandList.addCommand(create922VentValveCmd());
        commandList.addCommand(create923HeaterCmd());
        commandList.addCommand(create924AirCoolerCmd());
    }

    // ==================== Фабричные методы команд ====================

    private SingleCommand create902StatusCmd() {
        return new SingleCommand("?V00902", "?V00902 - Системный статус TIC", this::parse902Status, 200);
    }

    private SingleCommand create902InfoCmd() {
        return new SingleCommand("?S00902", "?S00902 - Информация о системе", this::parse902Info, 100);
    }

    private SingleCommand create904StatusCmd() {
        return new SingleCommand("?V00904", "?V00904 - Статус турбонасоса", this::parseSimpleStatus, 100);
    }

    private SingleCommand create904OnOffCmd(boolean on) {
        int state = on ? 1 : 0;
        String name = "IC904 " + state;
        return new SingleCommand(name, name + " - " + (on ? "Включить" : "Выключить") + " турбонасос",
                this::parseSimpleControlResponse, 100);
    }

    private SingleCommand create904SetupSlaveCmd() {
        return new SingleCommand("?S00904 4", "?S00904 4 - Slave setup турбонасоса", this::parseTurboSlaveSetup, 150);
    }

    private SingleCommand create904StartDelayCmd() {
        return new SingleCommand("?S00904 21", "?S00904 21 - Задержка старта турбонасоса", this::parseSimpleSetup, 100);
    }

    private SingleCommand create905SpeedCmd() {
        return new SingleCommand("?V00905", "?V00905 - Скорость турбонасоса (%)", this::parseSimpleValue, 100);
    }

    private SingleCommand create906PowerCmd() {
        return new SingleCommand("?V00906", "?V00906 - Мощность турбонасоса (Вт)", this::parseSimpleValue, 100);
    }

    private SingleCommand create907NormalCmd() {
        return new SingleCommand("?V00907", "?V00907 - На нормальной скорости", this::parseSimpleValue, 100);
    }

    private SingleCommand create908StandbyCmd() {
        return new SingleCommand("?V00908", "?V00908 - Статус standby", this::parseSimpleValue, 100);
    }

    private SingleCommand create908SetStandbyCmd(boolean enable) {
        int val = enable ? 1 : 0;
        return new SingleCommand("IC908 " + val, "IC908 " + val + " - Установить standby", this::parseSimpleControlResponse, 100);
    }

    private SingleCommand create909CycleTimeCmd() {
        return new SingleCommand("?V00909", "?V00909 - Наработка турбонасоса (часы)", this::parseSimpleValue, 100);
    }

    private SingleCommand create910StatusCmd() {
        return new SingleCommand("?V00910", "?V00910 - Статус backing pump", this::parseSimpleStatus, 100);
    }

    private SingleCommand create910OnOffCmd(boolean on) {
        int state = on ? 1 : 0;
        return new SingleCommand("IC910 " + state, "IC910 " + state + " - Вкл/выкл backing", this::parseSimpleControlResponse, 100);
    }

    private SingleCommand create910SequenceCmd() {
        return new SingleCommand("?S00910 70", "?S00910 70 - Последовательность backing", this::parseSimpleSetup, 100);
    }

    private SingleCommand createGaugeStatusCmd(int objId) {
        String cmd = String.format("?V00%03d", objId);
        return new SingleCommand(cmd, cmd + " - Gauge " + (objId - 912), resp -> parseGaugeResponse(resp, objId), 150);
    }

    private SingleCommand create940AllGaugesCmd() {
        return new SingleCommand("?V00940", "?V00940 - Все значения датчиков (рекомендуется)", this::parse940Response, 300);
    }

    private SingleCommand createGaugeOnOffCmd(int objId, boolean on) {
        int code = on ? 1 : 0;
        String name = String.format("IC%03d %d", objId, code);
        return new SingleCommand(name, name + " - Вкл/выкл gauge", this::parseSimpleControlResponse, 100);
    }

    private SingleCommand createGaugeZeroCmd(int objId) {
        return new SingleCommand(String.format("IC%03d 3", objId), "Zero gauge", this::parseSimpleControlResponse, 200);
    }

    private SingleCommand createGaugeCalCmd(int objId) {
        return new SingleCommand(String.format("IC%03d 4", objId), "Calibrate gauge", this::parseSimpleControlResponse, 5000);
    }

    private SingleCommand createGaugeDegasCmd(int objId) {
        return new SingleCommand(String.format("IC%03d 5", objId), "Degas gauge", this::parseSimpleControlResponse, 300);
    }

    private SingleCommand createGaugeNewIdCmd(int objId) {
        return new SingleCommand(String.format("IC%03d 2", objId), "New gauge ID", this::parseSimpleControlResponse, 200);
    }

    private SingleCommand createRelayStatusCmd(int objId) {
        String cmd = String.format("?V00%03d", objId);
        return new SingleCommand(cmd, cmd + " - Relay " + (objId - 915), this::parseSimpleStatus, 100);
    }

    private SingleCommand createRelayOnOffCmd(int objId, boolean on) {
        int state = on ? 1 : 0;
        return new SingleCommand(String.format("IC%03d %d", objId, state), "Вкл/выкл реле", this::parseSimpleControlResponse, 100);
    }

    private SingleCommand create919PsTempCmd() {
        return new SingleCommand("?V00919", "?V00919 - Температура блока питания", this::parseSimpleValue, 100);
    }

    private SingleCommand create920InternalTempCmd() {
        return new SingleCommand("?V00920", "?V00920 - Внутренняя температура TIC", this::parseSimpleValue, 100);
    }

    private SingleCommand create921AnalogueOutCmd() {
        return new SingleCommand("?V00921", "?V00921 - Аналоговый выход", this::parseSimpleValue, 100);
    }

    private SingleCommand create922VentValveCmd() {
        return new SingleCommand("?V00922", "?V00922 - Vent valve", this::parseSimpleStatus, 100);
    }

    private SingleCommand create923HeaterCmd() {
        return new SingleCommand("?V00923", "?V00923 - Heater band", this::parseHeaterResponse, 100);
    }

    private SingleCommand create924AirCoolerCmd() {
        return new SingleCommand("?V00924", "?V00924 - Air cooler", this::parseSimpleStatus, 100);
    }

    // ==================== Парсеры ====================

    private AnswerValues parse902Status(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(parts.length);
        for (int i = 0; i < parts.length; i++) {
            try { av.addValue(Double.parseDouble(parts[i]), "part" + i); }
            catch (Exception e) { av.addValue(0, parts[i]+"raw"); }
        }
        return av;
    }

    private AnswerValues parse902Info(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(4);
        for (int i = 0; i < Math.min(4, parts.length); i++) {
            av.addValue(0, switch (i) { case 0 -> parts[i]+"type";
                case 1 -> parts[i]+"swVer";
                case 2 -> parts[i]+"serial";
                default -> parts[i]+"picVer"; });
        }
        return av;
    }

    private AnswerValues parseSimpleStatus(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(3);
        if (parts.length >= 1) av.addValue(safeParseDouble(parts[0]), "state");
        if (parts.length >= 2) av.addValue(safeParseDouble(parts[1]), "alert");
        if (parts.length >= 3) av.addValue(safeParseDouble(parts[2]), "priority");
        return av;
    }

    private AnswerValues parseSimpleValue(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(1);
        try { av.addValue(Double.parseDouble(parts[0]), " value"); }
        catch (Exception e) { av.addValue(0, s + " raw"); }
        return av;
    }

    private AnswerValues parseSimpleControlResponse(byte[] response) {
        String s = cleanResponse(response);
        AnswerValues av = new AnswerValues(1);
        av.addValue(s.startsWith("*C") || s.startsWith("=C") ? 0.0 : -1.0, s.startsWith("*C") || s.startsWith("=C") ? "OK" : "error");
        return av;
    }

    private AnswerValues parseGaugeResponse(byte[] response, int objId) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(Math.max(5, parts.length));
        if (parts.length > 0) av.addValue(safeParseDouble(parts[0]), "value");
        if (parts.length > 1) av.addValue(safeParseDouble(parts[1]), "units");
        if (parts.length > 2) av.addValue(safeParseDouble(parts[2]), "state");
        if (parts.length > 3) av.addValue(safeParseDouble(parts[3]), "alert");
        if (parts.length > 4) av.addValue(safeParseDouble(parts[4]), "priority");
        return av;
    }

    private AnswerValues parse940Response(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(parts.length / 2);
        for (int i = 0; i + 1 < parts.length; i += 2) {
            av.addValue(safeParseDouble(parts[i + 1]), "gauge_pos_" + (int) safeParseDouble(parts[i]));
        }
        return av;
    }

    private AnswerValues parseTurboSlaveSetup(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(5);
        av.addValue(safeParseDouble(parts[0]), "master");
        av.addValue(safeParseDouble(parts[1]), "units");
        av.addValue(safeParseDouble(parts[2]), "on_sp");
        av.addValue(safeParseDouble(parts[3]), "off_sp");
        av.addValue(safeParseDouble(parts[4]), "enable");
        return av;
    }

    private AnswerValues parseSimpleSetup(byte[] response) {
        AnswerValues av = new AnswerValues(1);
        av.addValue(0, cleanResponse(response) + " data");
        return av;
    }

    private AnswerValues parseHeaterResponse(byte[] response) {
        String s = cleanResponse(response);
        String[] parts = s.split(";");
        AnswerValues av = new AnswerValues(3);
        if (parts.length > 0) av.addValue(safeParseDouble(parts[0]), "time");
        if (parts.length > 1) av.addValue(safeParseDouble(parts[1]), "state");
        return av;
    }

    private String cleanResponse(byte[] response) {
        if (response == null) return "";
        String s = new String(response).trim();
        s = MULTIDROP_PREFIX.matcher(s).replaceFirst("");
        s = RESPONSE_PREFIX.matcher(s).replaceFirst("");
        s = s.replaceAll("^[\\s=\\*VCS]+", "").trim();
        if (s.endsWith("\r")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private double safeParseDouble(String val) {
        try { return Double.parseDouble(val.trim()); }
        catch (Exception e) { return Double.NaN; }
    }

    public SingleCommand getCommand(String cmdString) {
        return commandList.getCommand(cmdString);
    }
}
