package parsers.com.bkm4;

import org.example.device.command.SingleCommand;
import org.example.device.protBkm4.BKM4_DEVICE;
import org.example.device.protBkm4.Bkm4CommandRegistry;
import org.example.gui.devices.bkm4.emulation.Bkm4Emulator;
import org.example.gui.devices.bkm4.emulation.Bkm4Responder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BKM-4 emulator + parser tests")
class Bkm4ProtocolTest {

    private Bkm4Emulator emulator;
    private Bkm4Responder responder;
    private Bkm4CommandRegistry registry;
    private BKM4_DEVICE device;

    @BeforeEach
    void setUp() {
        emulator = new Bkm4Emulator();
        responder = new Bkm4Responder(emulator);
        registry = new Bkm4CommandRegistry();
        device = new BKM4_DEVICE();
    }

    @Test
    @DisplayName("Emulator answers queries with zero-padded frames")
    void emulatorQueries() {
        assertThat(responder.processCommand("&A?")).isEqualTo("@A0");
        assertThat(responder.processCommand("&V?")).isEqualTo("@V0");
        assertThat(responder.processCommand("&S?")).isEqualTo("@S0000");
        assertThat(responder.processCommand("&F?")).isEqualTo("@F0000");
        assertThat(responder.processCommand("&G?")).isEqualTo("@G0");
    }

    @Test
    @DisplayName("Emulator applies set commands and echoes padded values")
    void emulatorSetCommands() {
        assertThat(responder.processCommand("&A1")).isEqualTo("@A1");
        assertThat(responder.processCommand("&V3")).isEqualTo("@V3");
        assertThat(responder.processCommand("&G1")).isEqualTo("@G1");
        assertThat(responder.processCommand("&S1000")).isEqualTo("@S1000");
        assertThat(responder.processCommand("&S0500")).isEqualTo("@S0500");
        assertThat(responder.processCommand("&S?")).isEqualTo("@S0500");
    }

    @Test
    @DisplayName("Emulator rejects out-of-range and malformed commands")
    void emulatorErrors() {
        assertThat(responder.processCommand("&A2")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("&V5")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("&G2")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("&S3001")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("&F123")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("&X?")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("junk")).isEqualTo("@ERROR");
        assertThat(responder.processCommand("")).isEqualTo("@ERROR");
        assertThat(responder.processCommand(null)).isEqualTo("@ERROR");
    }

    @Test
    @DisplayName("Flow needs an open valve, generation and a setpoint")
    void flowRequiresValve() {
        emulator.setGeneration(1);
        emulator.setSetpointMlMin(1000);
        for (int i = 0; i < 60; i++) {
            emulator.advance(0.5);
        }
        assertThat(emulator.getCurrentFlowMlMin()).isLessThan(5.0);
    }

    @Test
    @DisplayName("Flow ramps up to the setpoint like a real device")
    void flowRampsToSetpoint() {
        emulator.setValve(2);
        emulator.setSetpointMlMin(1000);
        emulator.setGeneration(1);
        for (int i = 0; i < 120; i++) {
            emulator.advance(0.5);
        }
        assertThat(emulator.getCurrentFlowMlMin()).isBetween(995.0, 1005.0);

        String answer = responder.processCommand("&F?");
        assertThat(answer).matches("@F\\d{4}");
        assertThat(Integer.parseInt(answer.substring(2))).isBetween(995, 1005);
    }

    @Test
    @DisplayName("Registry commands are reachable by protocol letter")
    void registryKeys() {
        for (String key : new String[]{"A", "V", "S", "F", "G"}) {
            assertThat(registry.getCommandList().getCommand(key))
                    .as("command %s", key)
                    .isNotNull();
        }
        SingleCommand setpoint = registry.getCommandList().getCommand("S");
        assertThat(new String(setpoint.build(Map.of("value", 500)), StandardCharsets.US_ASCII))
                .isEqualTo("&S0500");
        assertThat(new String(setpoint.build(Map.of()), StandardCharsets.US_ASCII))
                .isEqualTo("&S?");
    }

    @Test
    @DisplayName("Device resolves, parses padded answers and flags @ERROR")
    void deviceParsesAnswers() {
        device.setCmdToSend("&A?");
        assertThat(device.isKnownCommand()).isTrue();
        device.setCmdToSend("&S1500");
        assertThat(device.isKnownCommand()).isTrue();
        device.setCmdToSend("&F?");
        assertThat(device.isKnownCommand()).isTrue();

        device.setCmdToSend("&F?");
        device.setReceived("@F0500\r");
        device.parseData();
        assertThat(device.hasValue()).isTrue();
        assertThat(device.getValues().getValues()[0]).isEqualTo(500.0);

        device.setCmdToSend("&S?");
        device.setReceived("@S0050\r");
        device.parseData();
        assertThat(device.hasValue()).isTrue();
        assertThat(device.getValues().getValues()[0]).isEqualTo(50.0);

        device.setCmdToSend("&A?");
        device.setReceived("@ERROR\r");
        device.parseData();
        assertThat(device.hasValue()).isFalse();
    }

    @Test
    @DisplayName("Unknown command is not resolved")
    void unknownCommand() {
        device.setCmdToSend("&Z?");
        assertThat(device.isKnownCommand()).isFalse();
    }
}
