package org.example.device.lora.rui420;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

import java.util.Optional;

public class Rui420Message {
    private static final Logger log = Logger.getLogger(Rui420Message.class);

    private final byte[] rawMessage;
    @Getter
    private String eventType;
    @Getter
    private int snr;
    @Getter
    private int dbi;
    @Getter
    private byte[] payload;

    private Rui420Message(byte[] rawMessage, String eventType, int snr, int dbi, byte[] payload) {
        this.rawMessage = rawMessage;
        this.eventType = eventType;
        this.snr = snr;
        this.dbi = dbi;
        this.payload = payload;
    }

    public static Optional<Rui420Message> parse(final byte[] msgBytes) {
        if (msgBytes == null) {
            log.error("Message is null");
            return Optional.empty();
        }

        if (msgBytes.length < 12) {
            log.error("Message too short");
            return Optional.empty();
        }

        String msg = new String(msgBytes);
        if (!msg.startsWith("+EVT:RXP2P:")) {
            log.error("Message does not start with +EVT:RXP2P:");
            return Optional.empty();
        }

        String content = msg.substring("+EVT:RXP2P:".length());
        String[] parts = content.split(":");

        if (parts.length != 3) {
            log.error("Unexpected number of parts: " + parts.length);
            return Optional.empty();
        }

        int snr;
        int dbi;
        try {
            snr = Integer.parseInt(parts[0]);
            dbi = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.error("Invalid numeric fields", e);
            return Optional.empty();
        }

        String hex = parts[2].trim();
        if (hex.length() % 2 != 0) {
            log.error("Payload hex string has odd length");
            return Optional.empty();
        }

        for (char c : hex.toCharArray()) {
            if (Character.digit(c, 16) == -1) {
                log.error("Payload contains non-hex characters");
                return Optional.empty();
            }
        }

        byte[] payload = MyUtilities.strToByte(hex, (char) 0);  // Assuming this converts hex to bytes; fallback to hexStringToByteArray if needed

        log.info("Parsed RUI420 message: SNR=" + snr + ", DBI=" + dbi + ", Payload length=" + payload.length);
        return Optional.of(new Rui420Message(msgBytes, "RXP2P", snr, dbi, payload));
    }

}