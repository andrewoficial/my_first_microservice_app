package org.example.device.protBoto;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

/**
 * Реестр команд термокамеры BOTO 1000 / China Modbus (Modbus RTU, 9600 8N1).
 * Регистры: 12 (текущая T ×100), 100 (уставка T ×100), 105 (вкл/выкл).
 */
@Slf4j
public class Boto1000CommandRegistry extends DeviceCommandRegistry {

    public static final int REG_TEMP = 12;
    public static final int REG_SET_TEMP = 100;
    public static final int REG_SET_MOD = 105;
    public static final int TEMP_SCALE = 100;

    @Override
    protected void initCommands() {
        commandList.addCommand(createReadTempCmd());
        commandList.addCommand(createWriteSetTempCmd());
        commandList.addCommand(createWriteModCmd());
    }

    private SingleCommand createReadTempCmd() {
        byte[] body = BotoModbusUtil.buildReadHoldingRequest(1, REG_TEMP, 1);
        return new SingleCommand(
                "readTemp",
                "readTemp - чтение текущей температуры (регистр 12, ×100)",
                "readTemp",
                body,
                args -> body,
                resp -> parseTemp(resp, "Текущая T", TEMP_SCALE),
                7,
                CommandType.BINARY
        );
    }

    private SingleCommand createWriteSetTempCmd() {
        byte[] empty = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "writeSetTemp",
                "writeSetTemp <значение> - установка температуры (регистр 100, ×100, °C)",
                "writeSetTemp",
                empty,
                args -> {
                    Object v = args.get("value");
                    double temp = v instanceof Number n ? n.doubleValue() : 0;
                    int raw = (int) Math.round(temp * TEMP_SCALE);
                    return BotoModbusUtil.buildWriteSingleRequest(1, REG_SET_TEMP, raw);
                },
                resp -> {
                    int[] parsed = BotoModbusUtil.parseWriteResponse(resp);
                    if (parsed != null) {
                        AnswerValues av = new AnswerValues(1);
                        av.addValue(parsed[1] / (double) TEMP_SCALE, "°C");
                        return av;
                    }
                    return null;
                },
                8,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("value", Double.class, 25.0,
                v -> v instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() <= 400));
        return cmd;
    }

    private SingleCommand createWriteModCmd() {
        byte[] empty = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "writeMod",
                "writeMod <0|1> - включение/выключение (регистр 105, 1=вкл, 0=выкл)",
                "writeMod",
                empty,
                args -> {
                    Object v = args.get("value");
                    int on = v instanceof Number n ? (n.intValue() == 1 ? 1 : 0) : 0;
                    return BotoModbusUtil.buildWriteSingleRequest(1, REG_SET_MOD, on);
                },
                resp -> {
                    int[] parsed = BotoModbusUtil.parseWriteResponse(resp);
                    if (parsed != null) {
                        AnswerValues av = new AnswerValues(1);
                        av.addValue((double) parsed[1], parsed[1] == 1 ? "ВКЛ" : "ВЫКЛ");
                        return av;
                    }
                    return null;
                },
                8,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("value", Integer.class, 1,
                v -> v instanceof Number n && (n.intValue() == 0 || n.intValue() == 1)));
        return cmd;
    }

    private static AnswerValues parseTemp(byte[] response, String label, int scale) {
        int[] values = BotoModbusUtil.parseReadResponse(response);
        if (values != null && values.length > 0) {
            AnswerValues av = new AnswerValues(1);
            av.addValue(values[0] / (double) scale, "°C");
            return av;
        }
        return null;
    }
}
