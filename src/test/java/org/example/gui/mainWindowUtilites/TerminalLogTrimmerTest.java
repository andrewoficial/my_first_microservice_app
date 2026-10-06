package org.example.gui.mainWindowUtilites;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TerminalLogTrimmerTest {

    @Test
    void dropsTwentyOldestStampedRecordsAndKeepsTheRestWhole() {
        StringBuilder sb = new StringBuilder();
        sb.append("обрывок без штампа\n");
        for (int i = 1; i <= 25; i++) {
            sb.append(String.format("2026.10.06 12:00:%02d.000:\tстрока %d%n", i, i).replace("\r\n", "\n"));
            sb.append("продолжение ").append(i).append('\n');
        }
        String kept = TerminalLogTrimmer.dropOldest(sb.toString(), 20);
        assertThat(kept).startsWith("2026.10.06 12:00:21.000:\tстрока 21\n");
        assertThat(kept).contains("продолжение 21");
        assertThat(kept).doesNotContain("строка 20");
        assertThat(kept).doesNotContain("обрывок");
        assertThat(kept).endsWith("продолжение 25\n");
    }

    @Test
    void keepsTheLastRecordWhenFewerStampsExist() {
        String text = "2026.10.06 12:00:01.000:\tодин\n2026.10.06 12:00:02.000:\tдва\n";
        assertThat(TerminalLogTrimmer.dropOldest(text, 20))
                .isEqualTo("2026.10.06 12:00:02.000:\tдва\n");
    }

    @Test
    void fitRepeatsUntilTheIncomingChunkFits() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 30; i++) {
            sb.append(String.format("2026.10.06 12:00:%02d.000:\t#%d%n", i, i).replace("\r\n", "\n"));
        }
        String text = sb.toString();
        int max = text.length() / 2;
        String kept = TerminalLogTrimmer.fit(text, 50, max, 20);
        assertThat(kept.length() + 50).isLessThanOrEqualTo(max);
        assertThat(kept).startsWith("2026.10.06 12:00:");
        assertThat(kept).contains("#30");
    }
}
