package org.example.device.protSimpleHex;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.ArgumentDescriptor;
import org.example.device.command.CommandType;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

@Slf4j
public class SimpleHexCommandRegistry extends DeviceCommandRegistry {

    public SimpleHexCommandRegistry() {
        initCommands();
    }

    @Override
    protected void initCommands() {
        commandList.addCommand(createSendHexCommand());
    }

    private SingleCommand createSendHexCommand() {
        byte[] baseBody = new byte[0];
        SingleCommand cmd = new SingleCommand(
                "sendHex",
                "sendHex [hex string] - отправить hex-последовательность (например: 00 11 22 FF)",
                "sendHex",
                baseBody,
                args -> {
                    String hexStr = (String) args.get("hex");
                    if (hexStr == null || hexStr.isEmpty()) {
                        throw new IllegalArgumentException("Hex string is empty");
                    }
                    return MyUtilities.hexStringToBytes(hexStr);
                },
                this::parseResponse,
                500,
                CommandType.BINARY
        );
        cmd.addArgument(new ArgumentDescriptor("hex", String.class, "", null));
        return cmd;
    }

    private AnswerValues parseResponse(byte[] response) {
        AnswerValues result = new AnswerValues(1);
        result.addValue(0.0, MyUtilities.bytesToHexString(response));
        return result;
    }
}
