package org.example.device.spbstu.mcps;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;

import java.util.Arrays;

public class SPbSTuMcpsCommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(SPbSTuMcpsCommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createGetModeCommand());
        commandList.addCommand(createGetInputCommand());
        commandList.addCommand(createGetOutputCommand());
        commandList.addCommand(createGetAllInputsCommand());
        commandList.addCommand(createGetAllOutputsCommand());
        commandList.addCommand(createSetOutputCommand());
    }

    private SingleCommand createGetModeCommand() {
        return new SingleCommand(
                "getMode",
                "Чтение режима работы (ручное / автоматическое )",
                "getMode",
                null,
                args -> "@RDMD".getBytes(),
                this::parseGetModeResponse,
                20,
                CommandType.ASCII
        );
    }

    private SingleCommand createGetInputCommand() {
        SingleCommand command = new SingleCommand(
                "getInput",
                "Чтение входов IN [01-15]",
                "getInput",
                null,
                args -> {
                    Integer ch = Math.round((Float) args.get("channel"));
                    return String.format("@RI%02d", ch).getBytes();
                },
                this::parseGetInputResponse,
                15,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "channel",
                Float.class,
                1,
                val -> ((Float) val) >= 1 && ((Float) val) <= 15
        ));
        return command;
    }

    private SingleCommand createGetOutputCommand() {
        SingleCommand command = new SingleCommand(
                "getOutput",
                "Чтение выходов OUT [01-15]",
                "getOutput",
                null,
                args -> {
                    // Адаптер для старых команд
                    if (args.containsKey("channel")) {
                        Float channelFloat = (Float) args.get("channel");
                        Integer channelInteger =  Math.round(channelFloat);
                        return String.format("@RO%02d", channelInteger).getBytes();
                    }
                    return "@RO01".getBytes(); // значение по умолчанию
                },
                this::parseGetOutputResponse,
                15,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "channel",
                Float.class,
                1,
                val -> ((Float) val) >= 1 && ((Float) val) <= 15
        ));
        return command;
    }

    private SingleCommand createGetAllInputsCommand() {
        return new SingleCommand(
                "getAllInputs",
                "Чтение порта IN",
                "getAllInputs",
                null,
                args -> "@RPIN".getBytes(),
                this::parseGetAllInputsResponse,
                25,
                CommandType.ASCII
        );
    }

    private SingleCommand createGetAllOutputsCommand() {
        return new SingleCommand(
                "getAllOutputs",
                "Чтение порта OUT",
                "getAllOutputs",
                null,
                args -> "@RPOU".getBytes(),
                this::parseGetAllOutputsResponse,
                25,
                CommandType.ASCII
        );
    }

    private SingleCommand createSetOutputCommand() {
        SingleCommand command = new SingleCommand(
                "setOutput",
                "Запись выходов OUT [01-15] [0/1] [,time 0-65535 if 1]",
                "setOutput",
                null,
                args -> {
                    Integer ch = Math.round((Float) args.get("channel"));
                    Integer b = Math.round((Float) args.get("b(0/1)"));
                    Integer time = Math.round((Float)  args.get("time"));
                    if (b == 0) {
                        return String.format("@WR%02d %d", ch, b).getBytes();
                    } else {
                        return String.format("@WR%02d %d,%d", ch, b, time).getBytes();
                    }
                },
                this::parseSetOutputResponse,
                15,
                CommandType.ASCII
        );
        command.addArgument(new ArgumentDescriptor(
                "channel",
                Float.class,
                1,
                val -> ((Float) val) >= 1 && ((Float) val) <= 15
        ));
        command.addArgument(new ArgumentDescriptor(
                "b",
                Float.class,
                0,
                val -> ((Float) val) == 0 || ((Float) val) == 1
        ));
        command.addArgument(new ArgumentDescriptor(
                "time",
                Float.class,
                0,
                val -> ((Float) val) >= 0 && ((Float) val) <= 65535
        ));
        return command;
    }


    private AnswerValues parseGetModeResponse(byte[] response) {
        String s;
        try {
            s = new String(response).trim();
        } catch (Exception e) {
            log.warn("Failed to parse response to string");
            return null;
        }
        if (s.startsWith("@RAMD ")) {
            String mode = s.substring(6);
            double val = "Auto".equals(mode) ? 1.0 : 0.0;
            AnswerValues av = new AnswerValues(1);
            av.addValue(val, mode);
            return av;
        } else {
            log.info("Unexpected response for getMode: " + s);
            return null;
        }
    }

    private AnswerValues parseGetInputResponse(byte[] response) {
        String s;
        try {
            s = new String(response).trim();
        } catch (Exception e) {
            log.warn("Failed to parse response to string");
            return null;
        }
        if (s.startsWith("@RA") && s.length() >= 7 && s.charAt(5) == ' ') {
            String b = s.substring(6);
            if ("0".equals(b) || "1".equals(b)) {
                double val = Double.parseDouble(b);
                AnswerValues av = new AnswerValues(1);
                av.addValue(val, b);
                return av;
            }
        }
        log.info("Unexpected response for getInput: " + s);
        return null;
    }

    private AnswerValues parseGetOutputResponse(byte[] response) {
        String s;
        try {
            s = new String(response).trim();
        } catch (Exception e) {
            log.warn("Failed to parse response to string");
            return null;
        }
        if (s.startsWith("@RA") && s.length() >= 7 && s.charAt(5) == ' ') {
            String b = s.substring(6);
            if ("0".equals(b) || "1".equals(b)) {
                double val = Double.parseDouble(b);
                AnswerValues av = new AnswerValues(1);
                av.addValue(val, b);
                return av;
            }
        }
        log.info("Unexpected response for getOutput: " + s);
        return null;
    }

    private AnswerValues parseGetAllInputsResponse(byte[] response) {
        String s;
        try {
            s = new String(response).trim();
        } catch (Exception e) {
            log.warn("Failed to parse response to string");
            return null;
        }
        if (s.startsWith("@RAIN ") && s.length() >= 21) {
            String bits = s.substring(6, 21);
            if (bits.matches("[01]{15}")) {
                AnswerValues av = new AnswerValues(1);
                av.addValue(0.0, bits);
                return av;
            }
        }
        log.info("Unexpected response for getAllInputs: " + s);
        return null;
    }

    private AnswerValues parseGetAllOutputsResponse(byte[] response) {
        String s;
        try {
            s = new String(response).trim();
        } catch (Exception e) {
            log.warn("Failed to parse response to string");
            return null;
        }
        if (s.startsWith("@RAOU ") && s.length() >= 21) {
            String bits = s.substring(6, 21);
            if (bits.matches("[01]{15}")) {
                AnswerValues av = new AnswerValues(1);
                av.addValue(0.0, bits);
                return av;
            }
        }
        log.info("Unexpected response for getAllOutputs: " + s);
        return null;
    }

    private AnswerValues parseSetOutputResponse(byte[] response) {
        String s;
        try {
            s = new String(response).trim();
        } catch (Exception e) {
            log.warn("Failed to parse response to string");
            return null;
        }
        if (s.endsWith(" OK") && s.startsWith("@WR")) {
            AnswerValues av = new AnswerValues(1);
            av.addValue(1.0, "OK");
            return av;
        } else {
            log.info("Unexpected response for setOutput: " + s);
            return null;
        }
    }
}