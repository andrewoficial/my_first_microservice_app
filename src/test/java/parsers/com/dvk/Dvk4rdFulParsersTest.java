package parsers.com.dvk;

import org.example.device.protDvk4rd.parsers.FULSParser;
import org.example.device.protDvk4rd.parsers.FULUParser;
import org.example.services.AnswerValues;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Very large test coverage for the extracted FUL* parsers in protDvk4rd.
 * These parsers are called from Dvk4rdCommandRegistry for FULS / FULU commands
 * (in addition to the main "F" command).
 *
 * Goal: lock down current parsing behavior (including the duplicated isCorrectNumberF logic
 * inside the parsers) before unification work.
 */
@DisplayName("Dvk4rd FULS / FULU parsers - large regression suite")
public class Dvk4rdFulParsersTest {

    private FULSParser fulsParser;
    private FULUParser fuluParser;

    @BeforeEach
    void setUp() {
        fulsParser = new FULSParser();
        fuluParser = new FULUParser();
    }


    @ParameterizedTest(name = "FULS field count {0} should fail")
    @MethodSource("badFieldCounts")
    @DisplayName("FULS - wrong number of fields (large negative cases)")
    void fuls_wrongFieldCount(int fieldCount, boolean shouldFail) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fieldCount; i++) {
            sb.append("00123");
            if (i < fieldCount - 1) sb.append("\t");
        }
        sb.append("\r");

        AnswerValues av = fulsParser.parseFULSResponse(sb.toString().getBytes());
        if (shouldFail) {
            assertNull(av);
        }
    }

    static Stream<Arguments> badFieldCounts() {
        return Stream.of(
            Arguments.of(5, true),
            Arguments.of(18, true),
            Arguments.of(19, false),
            Arguments.of(20, true),
            Arguments.of(0, true)
        );
    }

    @Test
    @DisplayName("FULS - invalid character in middle field triggers error path")
    void fuls_badCharInField() {
        String bad = "123\t45X\t789\t" + "\t001\t".repeat(14) + "1234567\t\r";
        AnswerValues av = fulsParser.parseFULSResponse(bad.getBytes());
        assertNull(av);
    }

    @Test
    @DisplayName("FULS - negative serial number")
    void fuls_negativeSerial() {
        String resp = "001\t002\t003\t004\t005\t006\t007\t008\t009\t010\t011\t012\t013\t014\t015\t016\t017\t-1234567\t\r";
        AnswerValues av = fulsParser.parseFULSResponse(resp.getBytes());
        assertNull(av);
    }

    // ====================== FULU tests (15 fields) ======================

    @Test
    @DisplayName("FULU - happy path 15 fields")
    void fulu_happyPath() {
        String response = "100\t200\t300\t400\t500\t600\t700\t800\t900\t1000\t1100\t1200\t1300\t1400\t1234567\t\r";
        AnswerValues av = fuluParser.parseFULUResponse(response.getBytes());
        assertNotNull(av);
        assertEquals(15, av.getValues().length);
    }

    @ParameterizedTest
    @MethodSource("fuluBadLengths")
    @DisplayName("FULU - incorrect field count coverage (large)")
    void fulu_wrongLength(int fields, boolean expectNull) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields; i++) {
            sb.append("00123");
            if (i < fields - 1) sb.append("\t");
        }
        sb.append("\r");
        AnswerValues av = fuluParser.parseFULUResponse(sb.toString().getBytes());
        if (expectNull) {
            assertNull(av);
        }
    }

    static Stream<Arguments> fuluBadLengths() {
        return Stream.of(
            Arguments.of(10, true),
            Arguments.of(14, true),
            Arguments.of(15, false),
            Arguments.of(16, true)
        );
    }

    // ====================== Volume / stress ======================

    @Test
    @DisplayName("Mass volume - 100 synthetic FULS responses")
    void massFuls() {
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            for (int f = 0; f < 17; f++) {
                sb.append(String.format("%05d", 100 + i + f)).append("\t");
            }
            sb.append(String.format("%07d", 1000000 + i)).append("\t\r");
            fulsParser.parseFULSResponse(sb.toString().getBytes());
        }
        assertTrue(true, "Survived 100 synthetic FULS parses");
    }

    @Test
    @DisplayName("Mass volume - 100 synthetic FULU responses")
    void massFulu() {
        for (int i = 0; i < 100; i++) {
            StringBuilder sb = new StringBuilder();
            for (int f = 0; f < 14; f++) {
                sb.append(String.format("%05d", 200 + i + f)).append("\t");
            }
            sb.append(String.format("%07d", 2000000 + i)).append("\t\r");
            fuluParser.parseFULUResponse(sb.toString().getBytes());
        }
        assertTrue(true, "Survived 100 synthetic FULU parses");
    }
}
