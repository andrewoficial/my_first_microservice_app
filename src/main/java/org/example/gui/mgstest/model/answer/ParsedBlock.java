package org.example.gui.mgstest.model.answer;

import lombok.Getter;
import lombok.Setter;

/**
 * Класс для хранения распарсенного блока
 */
@Getter
@Setter
public class ParsedBlock {
    private final int startIndex;
    private final int length;
    private final byte[] payload;
    private final long receivedCrc;
    private final long calculatedCrc;
    private final int nextIndex;
    private final boolean crcValid;

    public ParsedBlock(int startIndex, int length, byte[] payload,
                       long receivedCrc, long calculatedCrc, int nextIndex) {
        this.startIndex = startIndex;
        this.length = length;
        this.payload = payload;
        this.receivedCrc = receivedCrc;
        this.calculatedCrc = calculatedCrc;
        this.nextIndex = nextIndex;
        this.crcValid = (receivedCrc == calculatedCrc);
    }

    public String getPayloadAsString() {
        return new String(payload, java.nio.charset.StandardCharsets.US_ASCII);
    }

    @Override
    public String toString() {
        return String.format("Block[start=%d, length=%d, crcValid=%b, data='%s']",
                startIndex, length, crcValid, getPayloadAsString());
    }
}
