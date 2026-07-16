package org.example.device.protArdFeeBrdMeter;

import lombok.extern.slf4j.Slf4j;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.SpringContextHolder;

import static org.example.utilites.MyUtilities.*;

@Slf4j
public class ArdFeeBrdMeterCommandRegistry extends DeviceCommandRegistry {

    private static final int F_RESPONSE_MIN_LENGTH = 71;

    private static final int[][] F_FIELDS = {
            {1,  5, 100},   // TermBM    °C
            {7,  5,  10},   // PresBM    mmHg
            {13, 5,   1},   // hydmBM    %
            {19, 5, 100},   // thre_V    V (pwr)
            {25, 5, 1000},  // cur_One   A
            {31, 5, 1000},  // cur_Two   A
            {37, 5, 1000},  // c_One_Poly A
            {43, 5, 1000},  // c_Two_Poly A
            {49, 5, 1000},  // currResF  A
            {55, 5,    1},  // stat      st
            {61, 8,    1},  // serialNumber SN
    };

    private static final String[] F_UNITS = {
            " °C", " mmHg", " %", " V (pwr)", " A", " A", " A", " A", " A", " st", " SN"
    };

    private static final int SERIAL_NUMBER_INDEX = 10;

    @Override
    protected void initCommands() {
        commandList.addCommand(createFCommand());
    }

    private SingleCommand createFCommand() {
        return new SingleCommand(
                "F",
                "F -> TermBM PresBM HydmBM Thre_V Cur_One Cur_Two C_One_Poly C_Two_Poly CurrResF Stat SerialNumber CRC",
                this::parseFResponse,
                72
        );
    }

    private AnswerValues parseFResponse(byte[] response) {
        if (response.length < F_RESPONSE_MIN_LENGTH) {
            log.warn("F response too short: {} bytes (expected >= {})", response.length, F_RESPONSE_MIN_LENGTH);
            return null;
        }

        byte expectedCrc = calculateCRCforF(response);
        if (response[70] != expectedCrc) {
            log.warn("CRC mismatch for F: expected 0x{} got 0x{}", String.format("%02X", expectedCrc), String.format("%02X", response[70]));
            return null;
        }

        AnswerValues answerValues = new AnswerValues(F_FIELDS.length);

        for (int i = 0; i < F_FIELDS.length; i++) {
            int offset = F_FIELDS[i][0];
            int length = F_FIELDS[i][1];
            double divisor = F_FIELDS[i][2];
            String unit = F_UNITS[i];

            Double val = parseAsciiFieldChecked(response, offset, length, divisor);
            if (val == null) {
                log.warn("Invalid F field at offset {}: {}", offset, new String(response, offset, length));
                return null;
            }
            answerValues.addValue(val, unit);
        }

        // Serial number → direction lookup
        double serialNumber = answerValues.getValues()[SERIAL_NUMBER_INDEX];
        String snStr = String.valueOf((long) serialNumber);
        AnswerStorage as = SpringContextHolder.getBean(AnswerStorage.class);
        Integer tab = as != null ? as.getTabByIdent(snStr) : null;
        if (tab != null) {
            answerValues.setDirection(tab);
        }

        return answerValues;
    }
}
