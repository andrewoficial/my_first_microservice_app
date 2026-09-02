package parsers.com.esp.kantser;

import org.example.device.command.SingleCommand;
import org.example.device.protEspKantserBleEmu.ESP_KANTSER_BLE_EMU;
import org.example.device.protEspKantserBleEmu.EspKantserBleEmuCommandRegistry;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the ESP_KANTSER_BLE_EMU command registry:
 * command coverage, argument builders and response parsers.
 */
@DisplayName("ESP_KANTSER_BLE_EMU registry tests")
class EspKantserBleEmuCommandRegistryTest {

    private static final String[] ALL_COMMANDS = {
            "HELP", "GDUI?", "SREV?", "BLST?", "ADST?",
            "SCH1", "SCH2", "SCH3", "SCH4", "STER",
            "ADVE", "ADVD", "REBT",
            "LRBC", "LSBA", "LLBA", "CMMD", "SMAC", "GMAC"
    };

    private EspKantserBleEmuCommandRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new EspKantserBleEmuCommandRegistry();
    }

    @Test
    @DisplayName("All documented commands are registered")
    void allCommandsRegistered() {
        for (String name : ALL_COMMANDS) {
            assertThat(registry.getCommandList().getCommand(name))
                    .as("command %s is registered", name)
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("BLE GET parameters reachable over serial are registered")
    void bleParamsRegistered() {
        String[] ble = {
                "VE?", "GS?", "DT?", "BD?", "AA0?", "AA7?", "AW0?", "AW7?",
                "APON?", "APOFF?", "BAON?", "BAOFF?", "BLON?", "BLOFF?", "BF?", "LD?",
                "LB?", "GB?", "LWM?", "LC?", "LWJ?", "LWD?", "LWA?", "LWd?",
                "LWS?", "LWN?", "LWr?", "LWj?", "LWs?", "LWB?", "LL?", "LR?",
                "LV?", "GF?", "GA?", "Gs?", "IO?", "BW?", "Ll?", "Bf?",
                "BM?", "BZ?", "AR?", "IT?", "IS?", "IC?", "ID?", "IH?",
                "QA?", "QC?", "QD?", "QE?", "QF?", "QG?", "QH?"
        };
        for (String name : ble) {
            assertThat(registry.getCommandList().getCommand(name))
                    .as("BLE param %s is registered", name)
                    .isNotNull();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"HELP", "GDUI?", "SREV?", "BLST?", "ADST?", "ADVE", "ADVD", "REBT",
            "LRBC", "LSBA", "LLBA", "CMMD", "GMAC"})
    @DisplayName("No-arg commands build the bare command bytes")
    void noArgCommandsBuild(String name) {
        SingleCommand cmd = registry.getCommandList().getCommand(name);
        assertThat(cmd).isNotNull();
        assertThat(new String(cmd.build(java.util.Map.of()), StandardCharsets.US_ASCII))
                .isEqualTo(name);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SCH1", "SCH2", "SCH3", "SCH4", "STER"})
    @DisplayName("Numeric set commands append the value")
    void numericSetCommandsBuild(String name) {
        SingleCommand cmd = registry.getCommandList().getCommand(name);
        assertThat(cmd).isNotNull();
        String built = new String(cmd.build(java.util.Map.of("value", 42.0)), StandardCharsets.US_ASCII);
        assertThat(built).isEqualTo(name + " 42");
        // whole numbers are printed without fraction
        assertThat(new String(cmd.build(java.util.Map.of("value", 10.5)), StandardCharsets.US_ASCII))
                .isEqualTo(name + " 10.5");
    }

    @Test
    @DisplayName("SMAC appends the MAC argument and validates it")
    void smacBuildsAndValidates() {
        SingleCommand cmd = registry.getCommandList().getCommand("SMAC");
        assertThat(cmd).isNotNull();
        String built = new String(cmd.build(java.util.Map.of("mac", "A4:CF:12:34:56:78")),
                StandardCharsets.US_ASCII);
        assertThat(built).isEqualTo("SMAC A4:CF:12:34:56:78");
        assertThat(cmd.getArguments().get(0).validate("AA-BB-CC-DD-EE-FF")).isTrue();
        assertThat(cmd.getArguments().get(0).validate("not-a-mac")).isFalse();
    }

    @Test
    @DisplayName("HELP response is parsed as text with Ok flag")
    void helpResponseParsed() {
        AnswerValues av = registry.getCommandList().getCommand("HELP")
                .getResult("HELP, GDUI?, SREV?, BLST?...\r\n".getBytes(StandardCharsets.US_ASCII));
        assertThat(av).isNotNull();
        assertThat(av.getUnits()[0]).contains("HELP, GDUI?");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BLE Status:\n  Connected: YES\n  Advertising: YES\n  Scanning: NO",
            "BLE Status:\n  Connected: NO\n  Advertising: NO\n  Scanning: YES"})
    @DisplayName("BLST? parses Connected/Advertising/Scanning")
    void bleStatusParsed(String response) {
        AnswerValues av = registry.getCommandList().getCommand("BLST?")
                .getResult(response.getBytes(StandardCharsets.US_ASCII));
        assertThat(av).isNotNull();
        assertThat(av.getValues()).hasSize(3);
        boolean connected = response.contains("Connected: YES");
        boolean advertising = response.contains("Advertising: YES");
        boolean scanning = response.contains("Scanning: YES");
        assertThat(av.getValues()[0]).isEqualTo(connected ? 1.0 : 0.0);
        assertThat(av.getValues()[1]).isEqualTo(advertising ? 1.0 : 0.0);
        assertThat(av.getValues()[2]).isEqualTo(scanning ? 1.0 : 0.0);
    }

    @Test
    @DisplayName("CMMD 'NOT SET' falls back to text parsing")
    void cmmdNotSetParsed() {
        AnswerValues av = registry.getCommandList().getCommand("CMMD")
                .getResult("NOT SET".getBytes(StandardCharsets.US_ASCII));
        assertThat(av).isNotNull();
        assertThat(av.getUnits()[0]).contains("NOT SET");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Master MAC: A4:CF:12:34:56:78", "CMMD AA:BB:CC:DD:EE:FF"})
    @DisplayName("MAC responses extract the MAC address")
    void macParsed(String response) {
        AnswerValues av = registry.getCommandList().getCommand("CMMD")
                .getResult(response.getBytes(StandardCharsets.US_ASCII));
        assertThat(av).isNotNull();
        assertThat(av.getUnits()[0].trim()).containsPattern("^[0-9A-Fa-f]{2}([:-])[0-9A-Fa-f]{2}(\\1[0-9A-Fa-f]{2}){4}$");
    }

    @Test
    @DisplayName("OK responses flag value 1")
    void okResponseFlagsOne() {
        AnswerValues av = registry.getCommandList().getCommand("ADVE")
                .getResult("Ok".getBytes(StandardCharsets.US_ASCII));
        assertThat(av).isNotNull();
        assertThat(av.getValues()[0]).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Device resolves query alias without '?' and arg commands")
    void deviceResolvesAliasAndArgs() {
        ESP_KANTSER_BLE_EMU device = new ESP_KANTSER_BLE_EMU();
        device.setCmdToSend("VE");
        assertThat(device.isKnownCommand()).as("VE alias for VE?").isTrue();
        device.setCmdToSend("VE?");
        assertThat(device.isKnownCommand()).isTrue();
        device.setCmdToSend("SCH1 42");
        assertThat(device.isKnownCommand()).isTrue();
        assertThat(device.getExpectedBytes()).isEqualTo(20);
        device.setCmdToSend("UNKNOWN");
        assertThat(device.isKnownCommand()).isFalse();
    }
}
