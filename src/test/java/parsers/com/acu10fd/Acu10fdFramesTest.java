package parsers.com.acu10fd;

import org.example.device.protAcu10fd.Acu10fdFrames;
import org.example.device.protAcu10fd.AcuStatusLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ACU10-FD frames and status log")
class Acu10fdFramesTest {

    @Test
    @DisplayName("Read frame matches the register layout used on the device")
    void readInstantFlowFrame() {
        byte[] frame = Acu10fdFrames.readHoldingRegisters((byte) 0x01, 0x0010, 2);
        assertThat(frame).containsExactly(
                (byte) 0x01, (byte) 0x03, (byte) 0x00, (byte) 0x10, (byte) 0x00, (byte) 0x02,
                (byte) 0xC5, (byte) 0xCE
        );
        assertThat(Acu10fdFrames.checkCrc(frame)).isTrue();
    }

    @Test
    @DisplayName("Write frame stores float as CDAB and keeps a valid CRC")
    void writeCoefficientFrame() {
        byte[] frame = Acu10fdFrames.writeFloatCdab((byte) 0x01, 0x0072, 1.0f);
        assertThat(frame).startsWith(
                (byte) 0x01, (byte) 0x10, (byte) 0x00, (byte) 0x72, (byte) 0x00, (byte) 0x02, (byte) 0x04
        );
        assertThat(frame).containsSequence((byte) 0x00, (byte) 0x00, (byte) 0x3F, (byte) 0x80);
        assertThat(Acu10fdFrames.checkCrc(frame)).isTrue();
        assertThat(frame).hasSize(13);
    }

    @Test
    @DisplayName("CDAB payload is decoded as IEEE-754 float")
    void parseFloat() {
        byte[] response = new byte[]{
                0x01, 0x03, 0x04,
                0x00, 0x00, 0x3F, (byte) 0x80,
                0x00, 0x00
        };
        assertThat(Acu10fdFrames.parseFloatCdab(response, 3)).isEqualTo(1.0f);
        assertThatThrownBy(() -> Acu10fdFrames.parseFloatCdab(new byte[]{0x01}, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Status log keeps the last 15 lines as HH:mm:ss: answer")
    void statusLogCapacityAndClear() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-06T15:04:05Z"), ZoneId.of("UTC"));
        AcuStatusLog log = new AcuStatusLog(clock);
        for (int i = 1; i <= 16; i++) {
            log.append("ответ " + i);
        }
        assertThat(log.lines()).hasSize(AcuStatusLog.CAPACITY);
        assertThat(log.lines().get(0)).isEqualTo("15:04:05: ответ 2");
        assertThat(log.lines().get(14)).isEqualTo("15:04:05: ответ 16");
        assertThat(log.render()).doesNotContain("ответ 1\n").contains("ответ 2");

        log.append("строка\nс переносом");
        assertThat(log.lines().get(14)).isEqualTo("15:04:05: строка с переносом");

        log.clear();
        assertThat(log.render()).isEmpty();
    }
}
