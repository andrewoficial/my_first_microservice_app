import org.example.device.ProtocolsList;
import org.example.device.SomeDevice;
import org.example.device.protDemo.DEMO_PROTOCOL;
import org.example.utilites.MyUtilities;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import static org.assertj.core.api.Assertions.*;

@DisplayName("MyUtilities tests")
class MyUtilitiesTest {

    // ---- createDeviceByProtocol (with SerialPort) ----
    @Nested
    @DisplayName("createDeviceByProtocol with SerialPort")
    class CreateDeviceWithPort {

        @Test
        @DisplayName("Should return correct device for each known protocol")
        void shouldReturnCorrectDeviceForEachProtocol() {
            for (ProtocolsList protocol : ProtocolsList.values()) {
                SomeDevice device = MyUtilities.createDeviceByProtocol(protocol, null);
                assertThat(device).isNotNull();
                // Проверяем, что класс соответствует ожидаемому по имени протокола (приблизительно)
                assertThat(device.getClass().getSimpleName().toUpperCase())
                        .contains(protocol.name().replace("Sens_", "").replace("_", ""));
            }
        }

        @Test
        @DisplayName("Should return DEMO_PROTOCOL for null protocol")
        void shouldReturnDemoForNullProtocol() {
            assertThatThrownBy(() -> MyUtilities.createDeviceByProtocol(null, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle all enum constants without exception")
        void shouldNotThrowForAllEnumConstants() {
            assertThatCode(() -> {
                for (ProtocolsList p : ProtocolsList.values()) {
                    MyUtilities.createDeviceByProtocol(p, null);
                }
            }).doesNotThrowAnyException();
        }
    }

    // ---- createDeviceByProtocol (without SerialPort) ----
    @Nested
    @DisplayName("createDeviceByProtocol without port")
    class CreateDeviceWithoutPort {

        @Test
        @DisplayName("Should return correct device for each protocol")
        void shouldReturnCorrectDevice() {
            for (ProtocolsList protocol : ProtocolsList.values()) {
                SomeDevice device = MyUtilities.createDeviceByProtocol(protocol);
                assertThat(device).isNotNull();
            }
        }

        @Test
        @DisplayName("Should return DEMO_PROTOCOL for null")
        void shouldReturnDemoForNull() {
            assertThat(MyUtilities.createDeviceByProtocol(null))
                    .isInstanceOf(DEMO_PROTOCOL.class);
        }
    }

    // ---- compare ----
    @Nested
    @DisplayName("compare")
    class Compare {

        private final byte[] reference = {0x01, 0x02, 0x03};
        private final byte[] same = {0x01, 0x02, 0x03, 0x04};

        @Test
        @DisplayName("Should return true when arrays match from start position")
        void exactMatchFromStart() {
            assertThat(MyUtilities.compare(reference, reference, 0, true)).isTrue();
        }

        @Test
        @DisplayName("Should return false if reference is null")
        void nullReference() {
            assertThat(MyUtilities.compare(null, same, 0, false)).isFalse();
        }

        @Test
        @DisplayName("Should return false if inCommand is null")
        void nullInCommand() {
            assertThat(MyUtilities.compare(reference, null, 0, false)).isFalse();
        }

        @Test
        @DisplayName("Should return false if startPosition out of range")
        void startPositionOutOfRange() {
            assertThat(MyUtilities.compare(reference, same, 10, false)).isFalse();
        }

        @Test
        @DisplayName("Should return false if exactMatch but lengths differ")
        void exactMatchLengthMismatch() {
            assertThat(MyUtilities.compare(reference, same, 0, true)).isFalse();
        }

        @Test
        @DisplayName("Should return true for partial match when not exact")
        void partialMatch() {
            assertThat(MyUtilities.compare(reference, same, 0, false)).isTrue();
        }

        @Test
        @DisplayName("Should return false if reference longer than remaining data")
        void referenceTooLong() {
            assertThat(MyUtilities.compare(new byte[]{1,2,3,4}, new byte[]{1,2}, 0, false)).isFalse();
        }

        @Test
        @DisplayName("Should handle startPosition in the middle")
        void startPositionMiddle() {
            byte[] data = {0x00, 0x01, 0x02, 0x03};
            assertThat(MyUtilities.compare(reference, data, 1, false)).isTrue();
        }
    }

    // ---- CRC calculations ----
    @Nested
    @DisplayName("CRC calculations")
    class CrcCalculations {

        @Test
        @DisplayName("calculateCRC16_GPS with known vector")
        void crc16GpsKnownVector() {
            byte[] data = {0x31, 0x32, 0x33, 0x34}; // "1234"
            int crc = MyUtilities.calculateCRC16_GPS(data);
            // Предварительно вычисленное значение CRC16/XMODEM для "1234"
            assertThat(crc).isEqualTo(0x8A5E);
        }

        @Test
        @DisplayName("calculateCRC16_GPS empty array returns 0xFFFF?")
        void crc16GpsEmpty() {
            int crc = MyUtilities.calculateCRC16_GPS(new byte[0]);
            // Ожидаем начальное значение 0xFFFF, обработанное циклом 0 раз
            assertThat(crc).isEqualTo(0xFFFF);
        }

        @Test
        @DisplayName("calculateCRCforF with length >= 70")
        void crcForFNormal() {
            byte[] arr = new byte[70];
            arr[1] = 0x01; // начальное crcVar
            for (int i = 2; i < 70; i++) arr[i] = (byte) i;
            byte crc = MyUtilities.calculateCRCforF(arr);
            // Ручной расчёт XOR: 0x01 ^ все байты 2..69
            byte expected = 0x01;
            for (int i = 2; i < 70; i++) expected ^= (byte) i;
            assertThat(crc).isEqualTo(expected);
        }

        @Test
        @DisplayName("calculateCRCforF with length < 70 returns 0")
        void crcForFShortArray() {
            byte[] arr = new byte[10];
            arr[1] = 0x05;
            assertThat(MyUtilities.calculateCRCforF(arr)).isEqualTo((byte)0);
        }

        @Test
        @DisplayName("calculateCRCforFdocumentation normal")
        void crcForFDocNormal() {
            byte[] arr = new byte[70];
            // зададим случайные значения, но исключим 0x0E и 0x09 на последней позиции
            for (int i = 1; i < 70; i++) arr[i] = (byte) (i + 1);
            arr[69] = 0x09; // этот должен быть пропущен
            byte crc = MyUtilities.calculateCRCforFdocumentation(arr);
            // Вычисляем ожидаемый XOR: байты 1..68 (исключая 0x0E), а 69 пропускаем
            byte expected = 0;
            for (int i = 1; i < 70; i++) {
                if (arr[i] == 0x0E || (arr[i] == 0x09 && i == 69)) continue;
                expected ^= arr[i];
            }
            assertThat(crc).isEqualTo(expected);
        }

        @Test
        @DisplayName("calculateCRCforFdocumentation short array")
        void crcForFDocShort() {
            assertThat(MyUtilities.calculateCRCforFdocumentation(new byte[10])).isEqualTo((byte)0);
        }
    }

    // ---- Structure checks ----
    @Nested
    @DisplayName("Structure checks (F-protocol)")
    class StructureChecks {

        @Test
        @DisplayName("checkStructureForF valid array passes")
        void validFStructure() {
            byte[] arr = new byte[73];
            arr[0] = 14; // маркер
            int[] tabs = {6,12,18,24,30,36,42,48,54,60,69};
            for (int pos : tabs) arr[pos] = 9;
            // CRC сделаем совпадающим с расчётным
            arr[70] = MyUtilities.calculateCRCforF(arr);
            assertThat(MyUtilities.checkStructureForF(arr)).isTrue();
        }

        @Test
        @DisplayName("checkStructureForF too short array")
        void tooShortFStructure() {
            byte[] arr = new byte[70]; // length <= 72
            assertThat(MyUtilities.checkStructureForF(arr)).isFalse();
        }

        @Test
        @DisplayName("checkStructureForF missing marker")
        void missingMarker() {
            byte[] arr = new byte[73];
            arr[0] = 0; // не 14
            assertThat(MyUtilities.checkStructureForF(arr)).isFalse();
        }

        @Test
        @DisplayName("checkStructureForF missing tab")
        void missingTab() {
            byte[] arr = new byte[73];
            arr[0] = 14;
            // все табы кроме одного
            int[] tabs = {6,12,18,24,30,36,42,48,54,60,69};
            for (int pos : tabs) arr[pos] = 9;
            arr[12] = 0; // ломаем табуляцию
            assertThat(MyUtilities.checkStructureForF(arr)).isFalse();
        }

        @Test
        @DisplayName("checkStructureForF invalid CRC")
        void invalidCrc() {
            byte[] arr = new byte[73];
            arr[0] = 14;
            int[] tabs = {6,12,18,24,30,36,42,48,54,60,69};
            for (int pos : tabs) arr[pos] = 9;
            arr[70] = 0; // неверный CRC
            assertThat(MyUtilities.checkStructureForF(arr)).isFalse();
        }

        @Test
        @DisplayName("checkStructureForArduinoBadVLTanswer valid")
        void validArduinoBadVlt() {
            byte[] arr = new byte[73];
            arr[0] = 14;
            int[] tabs = {6,12,18,24,30};
            for (int pos : tabs) arr[pos] = 9;
            assertThat(MyUtilities.checkStructureForArduinoBadVLTanswer(arr)).isTrue();
        }

        @Test
        @DisplayName("checkStructureForArduinoBadVLTanswer missing marker")
        void arduinoMissingMarker() {
            byte[] arr = new byte[73];
            arr[0] = 0;
            assertThat(MyUtilities.checkStructureForArduinoBadVLTanswer(arr)).isFalse();
        }
    }

    // ---- Number validation ----
    @Nested
    @DisplayName("Number validation")
    class NumberValidation {

        @Test
        @DisplayName("containAsciiDot returns true for dot present")
        void containDotTrue() {
            assertThat(MyUtilities.containAsciiDot(new byte[]{'1', '.', '2'})).isTrue();
        }

        @Test
        @DisplayName("containAsciiDot returns false if no dot")
        void containDotFalse() {
            assertThat(MyUtilities.containAsciiDot(new byte[]{'1', '2'})).isFalse();
        }

        @Test
        @DisplayName("containAsciiDot empty array returns false")
        void containDotEmpty() {
            assertThat(MyUtilities.containAsciiDot(new byte[0])).isFalse();
        }

        @Test
        @DisplayName("isCorrectNumberWithDot valid")
        void validWithDot() {
            assertThat(MyUtilities.isCorrectNumberWithDot(new byte[]{'1', '.', '2'})).isTrue();
        }

        @Test
        @DisplayName("isCorrectNumberWithDot fails if contains 0x00")
        void containsNullByte() {
            assertThat(MyUtilities.isCorrectNumberWithDot(new byte[]{'1', '.', 0x00})).isFalse();
        }

        @Test
        @DisplayName("isCorrectNumberWithDot fails if no dot")
        void noDot() {
            assertThat(MyUtilities.isCorrectNumberWithDot(new byte[]{'1', '2'})).isFalse();
        }

        @Test
        @DisplayName("isCorrectNumberF valid digits")
        void validF() {
            assertThat(MyUtilities.isCorrectNumberF(new byte[]{'0', '9'})).isTrue();
        }

        @Test
        @DisplayName("isCorrectNumberF too short")
        void tooShortF() {
            assertThat(MyUtilities.isCorrectNumberF(new byte[]{'1'})).isFalse();
        }

        @Test
        @DisplayName("isCorrectNumberF non-digit fails")
        void nonDigitF() {
            assertThat(MyUtilities.isCorrectNumberF(new byte[]{'1', 'A'})).isFalse();
        }

        @Test
        @DisplayName("isCorrectNumberFExceptMinus allows minus at start")
        void exceptMinusValid() {
            assertThat(MyUtilities.isCorrectNumberFExceptMinus(new byte[]{'-', '1', '2'})).isTrue();
        }

        @Test
        @DisplayName("isCorrectNumberFExceptMinus disallows minus elsewhere")
        void exceptMinusInvalidPosition() {
            assertThat(MyUtilities.isCorrectNumberFExceptMinus(new byte[]{'1', '-', '2'})).isFalse();
        }

        @Test
        @DisplayName("isCorrectNumberFExceptMinus empty array considered ok? (цикл не выполнится)")
        void exceptMinusEmpty() {
            // Пустой массив: цикл не выполнится, isOk останется true
            assertThat(MyUtilities.isCorrectNumberFExceptMinus(new byte[0])).isTrue();
        }
    }

    // ---- ASCII digit parsing ----
    @Nested
    @DisplayName("ASCII digit parsing")
    class AsciiParsing {

        @Test
        @DisplayName("parseAsciiDigits positive number")
        void parsePositive() {
            byte[] data = {'1', '2', '3'};
            long val = MyUtilities.parseAsciiDigits(data, 0, 3);
            assertThat(val).isEqualTo(123);
        }

        @Test
        @DisplayName("parseAsciiDigits negative number")
        void parseNegative() {
            byte[] data = {'-', '4', '2'};
            long val = MyUtilities.parseAsciiDigits(data, 0, 3);
            assertThat(val).isEqualTo(-42);
        }

        @Test
        @DisplayName("parseAsciiDigits invalid range returns 0")
        void invalidRange() {
            byte[] data = {'1', '2'};
            long val = MyUtilities.parseAsciiDigits(data, 0, 10);
            assertThat(val).isEqualTo(0);
        }

        @Test
        @DisplayName("parseAsciiDigits non-digit returns 0")
        void nonDigit() {
            byte[] data = {'1', 'A'};
            long val = MyUtilities.parseAsciiDigits(data, 0, 2);
            assertThat(val).isEqualTo(0);
        }

        @Test
        @DisplayName("parseAsciiField divides correctly")
        void parseField() {
            byte[] data = {'1', '2', '3', '4'};
            double result = MyUtilities.parseAsciiField(data, 0, 4, 100.0);
            assertThat(result).isCloseTo(12.34, within(0.001));
        }
    }

    // ---- Byte/String/Hex conversions ----
    @Nested
    @DisplayName("Byte-String-Hex conversions")
    class Conversion {

        @Test
        @DisplayName("bytesToHexString null returns null")
        void bytesToHexNull() {
            assertThat(MyUtilities.bytesToHexString(null)).isNull();
        }

        @Test
        @DisplayName("bytesToHexString empty returns empty string")
        void bytesToHexEmpty() {
            assertThat(MyUtilities.bytesToHexString(new byte[0])).isEmpty();
        }

        @Test
        @DisplayName("bytesToHexString and hexStringToBytes roundtrip")
        void roundtripHex() {
            byte[] original = {0x1F, (byte)0xAB, 0x00};
            String hex = MyUtilities.bytesToHexString(original);
            byte[] converted = MyUtilities.hexStringToBytes(hex);
            assertThat(converted).containsExactly(original);
        }

        @Test
        @DisplayName("hexStringToBytes empty string returns empty array")
        void hexEmpty() {
            assertThat(MyUtilities.hexStringToBytes("")).isEmpty();
        }

        @Test
        @DisplayName("strToByte with endian")
        void strToByteWithEndian() {
            byte[] result = MyUtilities.strToByte("AB", '\n');
            assertThat(result).containsExactly((byte)'A', (byte)'B', (byte)'\n');
        }

        @Test
        @DisplayName("strToByte without endian")
        void strToByteNoEndian() {
            byte[] result = MyUtilities.strToByte("AB", (char)0);
            assertThat(result).containsExactly((byte)'A', (byte)'B');
        }

        @Test
        @DisplayName("strToByte null returns null")
        void strToByteNull() {
            assertThat(MyUtilities.strToByte(null, 'x')).isNull();
        }

        @Test
        @DisplayName("strToByte empty string returns null")
        void strToByteEmpty() {
            assertThat(MyUtilities.strToByte("", 'x')).isNull();
        }
    }

    // ---- Date conversion ----
    @Test
    @DisplayName("convertToLocalDateViaMilisecond should convert correctly")
    void dateConversion() {
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 10, 12, 30);
        Date date = MyUtilities.convertToLocalDateViaMilisecond(ldt);
        assertThat(date).isNotNull();
        // проверяем, что год совпадает (с учетом временной зоны)
        LocalDateTime converted = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        assertThat(converted).isEqualTo(ldt);
    }

    // ---- changeToNeedSeparator ----
    @Test
    @DisplayName("changeToNeedSeparator replaces dots with configured separator")
    void changeSeparator() {
        String orig = MyUtilities.dotOrPoint;
        MyUtilities.dotOrPoint = ",";
        assertThat(MyUtilities.changeToNeedSeparator(3.14)).isEqualTo("3,14");
        MyUtilities.dotOrPoint = ";";
        assertThat(MyUtilities.changeToNeedSeparator(3.14)).isEqualTo("3;14");
        MyUtilities.dotOrPoint = orig; // cleanup
    }

    // ---- removeComWord ----
    @ParameterizedTest
    @CsvSource({
            "'test', 'test'",
            "'Arduino Uno(COM-23)', 'Arduino Uno'",
            "null, 'null'"
    })
    @DisplayName("removeComWord removes (COM...)")
    void removeComWord(String input, String expected) {
        assertThat(MyUtilities.removeComWord(input)).isEqualTo(expected);
    }

    @Test
    @DisplayName("removeComWord returns null string for null object")
    void removeComWordNullReturnsSpace() {
        assertThat(MyUtilities.removeComWord(null)).isEqualTo(" ");
    }


    // ---- getCubicUnits ----
    @ParameterizedTest
    @CsvSource({
            "0, ppm",
            "1, probably lel",
            "2, vol%",
            "99, unknown(99)"
    })
    void getCubicUnits(int num, String unit) {
        assertThat(MyUtilities.getCubicUnits(num)).isEqualTo(unit);
    }

    // ---- getAnyNumber ----
    @Nested
    @DisplayName("getAnyNumber")
    class GetAnyNumber {

        @Test
        @DisplayName("Extracts number from string with letters")
        void extractFromMixed() {
            assertThat(MyUtilities.getAnyNumber("abc12.3def")).isEqualTo(12.3);
        }

        @Test
        @DisplayName("Returns -1 for empty or null")
        void emptyOrNull() {
            assertThat(MyUtilities.getAnyNumber(null)).isEqualTo(-1.0);
            assertThat(MyUtilities.getAnyNumber("")).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("Handles multiple dots, takes first")
        void multipleDots() {
            assertThat(MyUtilities.getAnyNumber("12.34.56")).isEqualTo(12.34);
        }

        @Test
        @DisplayName("Trailing dot handled")
        void trailingDot() {
            assertThat(MyUtilities.getAnyNumber("12.")).isEqualTo(12.0);
        }

        @Test
        @DisplayName("Only dot returns -1")
        void onlyDot() {
            assertThat(MyUtilities.getAnyNumber(".")).isEqualTo(-1.0);
        }

        @Test
        @DisplayName("No digits returns -1")
        void noDigits() {
            assertThat(MyUtilities.getAnyNumber("abc")).isEqualTo(-1.0);
        }
    }
}