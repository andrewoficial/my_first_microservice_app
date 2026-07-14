package org.example.utilites;

import com.fazecast.jSerialComm.SerialPort;
import org.example.device.*;
import org.example.device.lora.rui420.igm.mesh.Igm10LoraMesh;
import org.example.device.mgu.usbadcten.MskMguUsbAdc10;
import org.example.device.protArdBadVlt.ARD_BAD_VOLTMETER;
import org.example.device.protArdCurrLoopMeter.ARD_CUR_LOOP_METER;
import org.example.device.protArdFeeBrdMeter.ARD_FEE_BRD_METER;
import org.example.device.protArdTerm.ARD_TERM;
import org.example.device.protBelead.BeLead;
import org.example.device.protCubic.Cubic;
import org.example.device.protDemo.DEMO_PROTOCOL;
import org.example.device.protDvk4rd.DVK_4RD;
import org.example.device.protDynament.Dynament;
import org.example.device.protEctTc290.ECT_TC290;
import org.example.device.protEdwardsD397.EDWARDS_D397_00_000;
import org.example.device.protErstevakMtp4d.ERSTEVAK_MTP4D;
import org.example.device.protFnirsiDps150.FNIRSI_DPS150;
import org.example.device.protGpsTest.GPS_Test;
import org.example.device.protIgm10.ascii.Igm10Ascii;
import org.example.device.protIgm10.modbus.Igm10Modbus;
import org.example.device.protIgm11.modbus.Igm11Modbus;
import org.example.device.protIgm12.modbus.Igm12Modbus;
import org.example.device.protMipex2.Mipex2;
import org.example.device.protOwonSpe3051.OWON_SPE3051;
import org.example.device.protQdl80a.Qdl80aDevice;
import org.example.device.protSimpleHex.SimpleHexDevice;
import org.example.device.protTt5166.TT5166;
import org.example.device.spbstu.mcps.SPbSTuMcps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;

public class MyUtilities {
    private static final Logger log = LoggerFactory.getLogger(MyUtilities.class);

    public static String separator = ";";
    public static String dotOrPoint = ",";
    public static final DateTimeFormatter CUSTOM_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss.SSS");
    public static final DateTimeFormatter CUSTOM_FORMATTER_FILES = DateTimeFormatter.ofPattern("yyyy.MM.dd HH.mm.ss.SSS");
    public static final DateTimeFormatter CUSTOM_FORMATTER_CSV = DateTimeFormatter.ofPattern("yyyy.MM.dd"+MyUtilities.separator+"HH:mm:ss.SSS");
    public static final int[] CRC16_TABLE = {
            0x0000, 0x1021, 0x2042, 0x3063, 0x4084, 0x50A5, 0x60C6, 0x70E7,
            0x8108, 0x9129, 0xA14A, 0xB16B, 0xC18C, 0xD1AD, 0xE1CE, 0xF1EF,
            0x1231, 0x0210, 0x3273, 0x2252, 0x52B5, 0x4294, 0x72F7, 0x62D6,
            0x9339, 0x8318, 0xB37B, 0xA35A, 0xD3BD, 0xC39C, 0xF3FF, 0xE3DE,
            0x2462, 0x3443, 0x0420, 0x1401, 0x64E6, 0x74C7, 0x44A4, 0x5485,
            0xA56A, 0xB54B, 0x8528, 0x9509, 0xE5EE, 0xF5CF, 0xC5AC, 0xD58D,
            0x3653, 0x2672, 0x1611, 0x0630, 0x76D7, 0x66F6, 0x5695, 0x46B4,
            0xB75B, 0xA77A, 0x9719, 0x8738, 0xF7DF, 0xE7FE, 0xD79D, 0xC7BC,
            0x48C4, 0x58E5, 0x6886, 0x78A7, 0x0840, 0x1861, 0x2802, 0x3823,
            0xC9CC, 0xD9ED, 0xE98E, 0xF9AF, 0x8948, 0x9969, 0xA90A, 0xB92B,
            0x5AF5, 0x4AD4, 0x7AB7, 0x6A96, 0x1A71, 0x0A50, 0x3A33, 0x2A12,
            0xDBFD, 0xCBDC, 0xFBBF, 0xEB9E, 0x9B79, 0x8B58, 0xBB3B, 0xAB1A,
            0x6CA6, 0x7C87, 0x4CE4, 0x5CC5, 0x2C22, 0x3C03, 0x0C60, 0x1C41,
            0xEDAE, 0xFD8F, 0xCDEC, 0xDDCD, 0xAD2A, 0xBD0B, 0x8D68, 0x9D49,
            0x7E97, 0x6EB6, 0x5ED5, 0x4EF4, 0x3E13, 0x2E32, 0x1E51, 0x0E70,
            0xFF9F, 0xEFBE, 0xDFDD, 0xCFFC, 0xBF1B, 0xAF3A, 0x9F59, 0x8F78,
            0x9188, 0x81A9, 0xB1CA, 0xA1EB, 0xD10C, 0xC12D, 0xF14E, 0xE16F,
            0x1080, 0x00A1, 0x30C2, 0x20E3, 0x5004, 0x4025, 0x7046, 0x6067,
            0x83B9, 0x9398, 0xA3FB, 0xB3DA, 0xC33D, 0xD31C, 0xE37F, 0xF35E,
            0x02B1, 0x1290, 0x22F3, 0x32D2, 0x4235, 0x5214, 0x6277, 0x7256,
            0xB5EA, 0xA5CB, 0x95A8, 0x8589, 0xF56E, 0xE54F, 0xD52C, 0xC50D,
            0x34E2, 0x24C3, 0x14A0, 0x0481, 0x7466, 0x6447, 0x5424, 0x4405,
            0xA7DB, 0xB7FA, 0x8799, 0x97B8, 0xE75F, 0xF77E, 0xC71D, 0xD73C,
            0x26D3, 0x36F2, 0x0691, 0x16B0, 0x6657, 0x7676, 0x4615, 0x5634,
            0xD94C, 0xC96D, 0xF90E, 0xE92F, 0x99C8, 0x89E9, 0xB98A, 0xA9AB,
            0x5844, 0x4865, 0x7806, 0x6827, 0x18C0, 0x08E1, 0x3882, 0x28A3,
            0xCB7D, 0xDB5C, 0xEB3F, 0xFB1E, 0x8BF9, 0x9BD8, 0xABBB, 0xBB9A,
            0x4A75, 0x5A54, 0x6A37, 0x7A16, 0x0AF1, 0x1AD0, 0x2AB3, 0x3A92,
            0xFD2E, 0xED0F, 0xDD6C, 0xCD4D, 0xBDAA, 0xAD8B, 0x9DE8, 0x8DC9,
            0x7C26, 0x6C07, 0x5C64, 0x4C45, 0x3CA2, 0x2C83, 0x1CE0, 0x0CC1,
            0xEF1F, 0xFF3E, 0xCF5D, 0xDF7C, 0xAF9B, 0xBFBA, 0x8FD9, 0x9FF8,
            0x6E17, 0x7E36, 0x4E55, 0x5E74, 0x2E93, 0x3EB2, 0x0ED1, 0x1EF0
    };

    // === VERY SIMILAR: Two createDeviceByProtocol methods ===
    public static SomeDevice createDeviceByProtocol(ProtocolsList protocol, SerialPort comPort){
        SomeDevice device;

        switch (protocol) {
            case IGM10ASCII -> device = new Igm10Ascii(comPort);
            case IGM10MODBUS -> device = new Igm10Modbus(comPort);
            case IGM11MODBUS -> device = new Igm11Modbus(comPort);
            case IGM12MODBUS -> device = new Igm12Modbus(comPort);
            case ARD_BAD_VOLTMETER -> device = new ARD_BAD_VOLTMETER(comPort);
            case ARD_FEE_BRD_METER -> device = new ARD_FEE_BRD_METER(comPort);
            case ARD_CUR_LOOP_METER -> device = new ARD_CUR_LOOP_METER(comPort);
            case ARD_TERM -> device = new ARD_TERM(comPort);
            case SPB_STU_MCPS -> device = new SPbSTuMcps(comPort);
            case MSK_MGU_UsbAdc10 -> device = new MskMguUsbAdc10(comPort);
            case ERSTEVAK_MTP4D -> device = new ERSTEVAK_MTP4D(comPort);
            case EDWARDS_D397_00_000 -> device = new EDWARDS_D397_00_000(comPort);
            case ECT_TC290 -> device = new ECT_TC290(comPort);
            case Sens_DVK_4RD -> device = new DVK_4RD(comPort);
            case IGM10LORA_MESH -> device = new Igm10LoraMesh(comPort);
            case DEMO_PROTOCOL -> device = new DEMO_PROTOCOL(comPort);
            case GPS_Test -> device = new GPS_Test(comPort);
            case OWON_SPE3051 -> device = new OWON_SPE3051(comPort);
            case Sens_Dynament -> device = new Dynament(comPort);
            case Sens_Cubic -> device = new Cubic(comPort);
            case Sens_Belead -> device = new BeLead(comPort);
            case Sens_Mipex2 -> device = new Mipex2(comPort);
            case TT5166 -> device = new TT5166(comPort);
            case DPS150 -> device = new FNIRSI_DPS150(comPort);
            case QDL80A -> device = new Qdl80aDevice(comPort);
            case SIMPLE_HEX -> device = new SimpleHexDevice(comPort);
            default -> device = new DEMO_PROTOCOL(comPort);
        }
        return device;
    }

    public static SomeDevice createDeviceByProtocol(ProtocolsList protocol){
        SomeDevice device;
        if(protocol == null)
            return new DEMO_PROTOCOL();

        switch (protocol) {
            case IGM10ASCII -> device = new Igm10Ascii();
            case IGM10MODBUS -> device = new Igm10Modbus();
            case IGM11MODBUS -> device = new Igm11Modbus();
            case IGM12MODBUS -> device = new Igm12Modbus();
            case ARD_BAD_VOLTMETER -> device = new ARD_BAD_VOLTMETER();
            case ARD_FEE_BRD_METER -> device = new ARD_FEE_BRD_METER();
            case ARD_CUR_LOOP_METER -> device = new ARD_CUR_LOOP_METER();
            case ARD_TERM -> device = new ARD_TERM();
            case SPB_STU_MCPS -> device = new SPbSTuMcps();
            case MSK_MGU_UsbAdc10 -> device = new MskMguUsbAdc10();
            case ERSTEVAK_MTP4D -> device = new ERSTEVAK_MTP4D();
            case EDWARDS_D397_00_000 -> device = new EDWARDS_D397_00_000();
            case ECT_TC290 -> device = new ECT_TC290();
            case IGM10LORA_MESH -> device = new Igm10LoraMesh();
            case DEMO_PROTOCOL -> device = new DEMO_PROTOCOL();
            case OWON_SPE3051 -> device = new OWON_SPE3051();
            case GPS_Test -> device = new GPS_Test();
            case Sens_DVK_4RD -> device = new DVK_4RD();
            case Sens_Dynament -> device = new Dynament();
            case Sens_Cubic -> device = new Cubic();
            case Sens_Belead -> device = new BeLead();
            case Sens_Mipex2 -> device = new Mipex2();
            case TT5166 -> device = new TT5166();
            case DPS150 -> device = new FNIRSI_DPS150();
            case QDL80A -> device = new Qdl80aDevice();
            case SIMPLE_HEX -> device = new SimpleHexDevice();
            default -> device = new DEMO_PROTOCOL();
        }
        return device;
    }

    // === BYTE COMPARISON ===
    public static boolean compare(byte[] reference, byte[] inCommand, int startPosition, boolean exactMatch) {
        if (reference == null || inCommand == null) {
            log.warn("Reference or inCommand is null");
            return false;
        }

        if (startPosition < 0 || startPosition >= inCommand.length || startPosition >= reference.length) {
            log.warn("Start position is out of range");
            return false;
        }

        if (exactMatch && reference.length != inCommand.length) {
            log.warn("Exact match required, but lengths do not match");
            return false;
        }

        if (reference.length > inCommand.length - startPosition) {
            log.warn("Reference is longer than available data in inCommand");
            return false;
        }

        for (int i = 0; i < reference.length; i++) {
            if (reference[i] != inCommand[startPosition + i]) {
                return false;
            }
        }

        return true;
    }

    // === VERY SIMILAR: CRC calculation methods ===
    public static int calculateCRC16_GPS(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            int index = (crc >> 8) ^ (b & 0xFF);
            crc = (crc << 8) ^ MyUtilities.CRC16_TABLE[index];
            crc &= 0xFFFF;
        }

        return crc;
    }

    public static byte calculateCRCforF(byte[] responseArray) {
        byte crcVar = responseArray[1];
        if(responseArray.length < 70) return 0;
        for (int i = 2; i < 70; i++) {
            crcVar ^= responseArray[i];
        }
        return crcVar;
    }

    public static byte calculateCRCforFdocumentation(byte[] responseArray) {
        if (responseArray.length < 70) return 0;

        byte crcVar = 0;

        for (int i = 1; i < 70; i++) {
            if (responseArray[i] == 0x0E || responseArray[i] == 0x09 && i == 69) {
                continue;
            }
            crcVar ^= responseArray[i];
        }

        return crcVar;
    }

    // === VERY SIMILAR: F structure check methods ===
    public static boolean checkStructureForF(byte[] responseArray) {
        int limit = 72;
        if (responseArray.length <= limit) {
            System.out.println("Ошибка: Минимальная длина " + limit + " символов, получено: " + responseArray.length);
            return false;
        }

        if (responseArray[0] != 14) {
            System.out.println("Ошибка: Ожидаемый маркер (14) отсутствует в начале посылки.");
            return false;
        }

        int[] tabIndices = {6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 69};
        for (int index : tabIndices) {
            if (responseArray[index] != 9) {
                System.out.println("Ошибка: Отсутствует ожидаемая табуляция (9) в позиции " + index);
                return false;
            }
        }

        byte calculatedCRC = calculateCRCforF(responseArray);
        byte calculatedCRCdocumentation = calculateCRCforFdocumentation(responseArray);
        boolean isOkCRC = calculatedCRC == responseArray[70];
        boolean isOkCRCdocumentation = calculatedCRCdocumentation == responseArray[70];

        if (!isOkCRC && !isOkCRCdocumentation) {
            System.out.println("Ошибка: CRC не совпадает. "
                    + "Рассчитанный CRC: " + calculatedCRC
                    + ", Рассчитанный CRC по документации: " + calculatedCRCdocumentation
                    + ", Принятый CRC: " + responseArray[70]);
            return false;
        }

        return true;
    }

    public static boolean checkStructureForArduinoBadVLTanswer(byte[] responseArray) {
        int limit = 72;
        if (responseArray.length <= limit) {
            System.out.println("Ошибка: Минимальная длина " + limit + " символов, получено: " + responseArray.length);
            return false;
        }

        if (responseArray[0] != 14) {
            System.out.println("Ошибка: Ожидаемый маркер (14) отсутствует в начале посылки.");
            return false;
        }

        int[] tabIndices = {6, 12, 18, 24, 30};
        for (int index : tabIndices) {
            if (responseArray[index] != 9) {
                System.out.println("Ошибка: Отсутствует ожидаемая табуляция (9) в позиции " + index);
                return false;
            }
        }

        return true;
    }

    public static String changeToNeedSeparator(double value){
        String str = Double.toString(value);
        return str.replaceAll("[.,]", MyUtilities.dotOrPoint);
    }

    public static String removeComWord(String arg){
        if(arg == null || arg.length() < 1){
            return "null";
        }
        if(arg.indexOf("(CO") > 0){
            return arg.substring(0, arg.indexOf("(CO"));
        }else{
            return arg;
        }

    }

    // === DATE UTILITIES ===
    public static Date convertToLocalDateViaMilisecond(LocalDateTime dateToConvert) {
        return java.util.Date
                .from(dateToConvert.atZone(ZoneId.systemDefault())
                        .toInstant());
    }

    // === VERY SIMILAR: Number validation methods ===
    public static boolean containAsciiDot(byte[] stringArray){
        for (byte b : stringArray) {
            if(b == '.'){
                return true;
            }
        }
        return false;
    }
    public static boolean isCorrectNumberWithDot(byte[] stringArray){
        boolean isOk = containAsciiDot(stringArray);

        if(isOk){
            for (byte b : stringArray) {
                if(b == 0){
                    log.warn("Error in isCorrectNumberWithDot. Found 0x00");
                    isOk = false;
                }
            }
        }else {
            log.warn("Error in isCorrectNumberWithDot. Dot not found.");
        }
        return isOk;
    }

    public static boolean isCorrectNumberF(byte[] stringArray){
        boolean isOk = true;
        if(stringArray.length < 2){
            isOk = false;
            log.warn("In isCorrectNumberF received too short array");
        }
        if(isOk){
            for (byte b : stringArray) {
                if(b < 47 || b > 57){
                    //log.warn("Error in isCorrectNumberF. b < 47 || b > 57");
                    isOk = false;
                    break;
                }
            }
        }
        return isOk;
    }

    public static boolean isCorrectNumberFExceptMinus(byte[] stringArray){
        boolean isOk = true;

        if(isOk){
            for (int i = 0; i < stringArray.length; i++) {
                if (i == 0) {
                    if(!(stringArray[i] >= '0' &&
                            stringArray[i] <= '9') &&
                            stringArray[i] != '-'){
                        System.out.println("Error in isCorrectNumberFExceptMinus. b < 47 || b > 57");
                        isOk = false;
                        break;
                    }
                }else{
                    if(!(stringArray[i] >= '0' &&
                            stringArray[i] <= '9')){
                        System.out.println("Error in isCorrectNumberFExceptMinus. b < 47 || b > 57");
                        isOk = false;
                        break;
                    }
                }

            }
            for (byte b : stringArray) {

            }
        }
        return isOk;
    }

    // === VERY SIMILAR: ASCII digit parsing ===
    public static long parseAsciiDigits(byte[] response, int start, int length) {
        if (response == null || start + length > response.length) {
            log.warn("parseAsciiDigits: invalid range");
            return 0;
        }
        byte[] sub = Arrays.copyOfRange(response, start, start + length);
        boolean negative = false;
        long value = 0;

        for (int i = 0; i < sub.length; i++) {
            byte b = sub[i];
            if (b == '-' && i == 0) {
                negative = true;
                continue;
            }
            if (b >= '0' && b <= '9') {
                value = value * 10 + (b - '0');
            } else {
                log.warn("parseAsciiDigits: non-digit at pos " + (start + i));
                return 0;
            }
        }
        return negative ? -value : value;
    }

    public static double parseAsciiField(byte[] response, int start, int length, double divisor) {
        long raw = parseAsciiDigits(response, start, length);
        return raw / divisor;
    }

    // === VERY SIMILAR: Byte / String / Hex conversion helpers ===
    public static String bytesToHexString(byte[] bytes) {
        if(bytes == null) return  null;
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));

        }
        return sb.toString().trim();
    }

    public static byte[] hexStringToBytes(String hexString) {
        hexString = hexString.trim();
        if (hexString.isEmpty()) {
            return new byte[0];
        }
        String[] hexValues = hexString.split(" ");
        byte[] result = new byte[hexValues.length];
        for (int i = 0; i < hexValues.length; i++) {
            result[i] = (byte) Integer.parseInt(hexValues[i], 16);
        }
        return result;
    }

    public static byte[] strToByte(String str, char endian){
        if(str == null || str.isEmpty())
            return null;
        byte[] result = null;
        if(endian != 0){
            result = new byte[str.length()+1];
        }else{
            result = new byte[str.length()];
        }

        for (int i = 0; i < str.length(); i++) {
            result[i] = (byte) str.charAt(i);
        }
        if(endian != 0){
            result[result.length-1] = (byte) endian;
        }
        return result;
    }

    // === MISC ===
    public static String getCubicUnits(int num){
        String unitStr;
        switch (num) {
            case 0:
                unitStr = "ppm";
                break;
            case 1:
                unitStr = "probably lel";
                break;
            case 2:
                unitStr = "vol%";
                break;
            default:
                unitStr = "unknown(" + num + ")";
                break;
        }
        return unitStr;
    }

    public static Double getAnyNumber(String str) {
        if (str == null || str.isEmpty()) {
            return -1.0;
        }

        boolean pointFound = false;
        StringBuilder numberBuilder = new StringBuilder();

        for (char c : str.toCharArray()) {
            if (Character.isDigit(c)) {
                numberBuilder.append(c);
            } else if (c == '.' && !pointFound) {
                pointFound = true;
                numberBuilder.append(c);
            } else if (pointFound) {
                // После первой точки любой нецифровой символ (включая вторую точку) — конец числа
                break;
            }
            // нецифровые символы до первой точки просто игнорируются (как и раньше)
        }

        if (numberBuilder.length() == 0) {
            return -1.0;
        }

        String result = numberBuilder.toString();
        if (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }

        if (result.isEmpty() || result.equals(".")) {
            return -1.0;
        }

        try {
            return Double.parseDouble(result);
        } catch (NumberFormatException e) {
            return -1.0;
        }
    }
}
