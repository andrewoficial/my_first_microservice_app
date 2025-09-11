package org.example.device.protMipex2.parsers;

import org.example.services.AnswerValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class ZERO2Parser {
    private static final Logger log = LoggerFactory.getLogger(ZERO2Parser.class);

    public AnswerValues parseZero2Response(byte[] response) {
        try {
            String responseString = new String(response, "ASCII").trim();

            // Проверяем, является ли строка "ZERO2 OK"
            if ("ZERO2 OK".equals(responseString)) {
                log.info("ZERO2 OK parsing: Successfully parsed ZERO2 OK response");
                return createAnswerValues(1.0);
            } else {
                log.warn("ZERO2 OK parsing: Unexpected response: {}", responseString);
                return createAnswerValues(-1.0);
            }
        } catch (Exception e) {
            log.warn("ZERO2 OK parsing: Failed to convert response to string: {}", Arrays.toString(response));
            return createAnswerValues(-1.0);
        }
    }

    private AnswerValues createAnswerValues(double value) {
        AnswerValues answerValues = new AnswerValues(1);
        answerValues.addValue(value, "Result");
        return answerValues;
    }
}