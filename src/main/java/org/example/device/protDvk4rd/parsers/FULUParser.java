package org.example.device.protDvk4rd.parsers;

import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class FULUParser {
    private static final Logger log = LoggerFactory.getLogger(FULUParser.class);

    public AnswerValues parseFULUResponse(byte[] response) {
        AnswerValues answerValues = null;
        String responseString;

        try {
            // Convert byte array to string, assuming ASCII encoding
            responseString = new String(response, "ASCII").trim();
            // Remove trailing <0D> or any non-field characters
            if (responseString.endsWith("\r") || responseString.endsWith("<0D>")) {
                responseString = responseString.substring(0, responseString.length() - 1);
                if (responseString.endsWith("<")) {
                    responseString = responseString.substring(0, responseString.length() - 1);
                }
            }
        } catch (Exception e) {
            log.warn("FULU parsing: Failed to convert response to string: {}", Arrays.toString(response));
            return null;
        }

        // Split by tab delimiter
        String[] fields = responseString.split("\t");
        if (fields.length != 15) { // 14 fields + 1 serial number
            log.warn("FULU parsing: Wrong number of fields, expected 15, got {}: {}", fields.length, responseString);
            return null;
        }

        answerValues = new AnswerValues(15); // 14 values + serial number
        double value = 0.0;
        double serialNumber = 0.0;

        // Parse each field (0 to 13)
        for (int field = 0; field < 14; field++) {
            String fieldValue = fields[field];
            if (isCorrectNumberF(fieldValue.getBytes())) {
                boolean success = true;
                value = 0.0;

                for (int i = 0; i < fieldValue.length(); i++) {
                    char c = fieldValue.charAt(i);
                    if (c >= '0' && c <= '9') {
                        value = value * 10 + (c - '0');
                    } else {
                        success = false;
                        break;
                    }
                }

                if (success) {
                    answerValues.addValue(value, " Units");
                } else {
                    log.warn("FULU parsing: Invalid number format in position ({}): {}", field, fieldValue);
                    answerValues.addValue(-88.88, " " + field + "(ERR)");
                    return null;
                }
            } else {
                log.warn("FULU parsing: Wrong number format in position ({}): {}", field, fieldValue);
                answerValues.addValue(-99.99, " " + field + "(ERR)");
                return null;
            }
        }

        // Parse serial number (field 14)
        String serialField = fields[14];
        if (isCorrectNumberF(serialField.getBytes())) {
            boolean success = true;
            boolean isNegative = false;

            for (int i = 0; i < serialField.length(); i++) {
                char c = serialField.charAt(i);
                if (c == '-' && i == 0) {
                    isNegative = true;
                    continue;
                }
                if (c >= '0' && c <= '9') {
                    serialNumber = serialNumber * 10 + (c - '0');
                } else {
                    success = false;
                    break;
                }
            }

            if (success) {
                serialNumber = isNegative ? -serialNumber : serialNumber;
                answerValues.addValue(serialNumber, " SN");
                if (AnswerStorage.getTabByIdent(String.valueOf(serialNumber)) != null) {
                    answerValues.setDirection(AnswerStorage.getTabByIdent(String.valueOf(serialNumber)));
                }
            } else {
                log.warn("FULU parsing: Invalid serial number format: {}", serialField);
                answerValues.addValue(-88.88, " SN (ERR)");
                return null;
            }
        } else {
            log.warn("FULU parsing: Wrong serial number format: {}", serialField);
            answerValues.addValue(-99.99, " SN (ERR)");
            return null;
        }

        log.info("FULU parsing: Successfully parsed response: {}", responseString);
        return answerValues;
    }

    // Assuming isCorrectNumberF is defined elsewhere, reused as in original code
    private boolean isCorrectNumberF(byte[] data) {
        // Placeholder for the original isCorrectNumberF logic
        for (byte b : data) {
            if (b != '-' && (b < '0' || b > '9')) {
                return false;
            }
        }
        return true;
    }
}