package org.example.device.mgu.usbadcten;

import org.apache.log4j.Logger;
import org.example.device.DeviceCommandRegistry;
import org.example.device.command.SingleCommand;
import org.example.services.AnswerValues;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MguUsbAdc10CommandRegistry extends DeviceCommandRegistry {
    private static final Logger log = Logger.getLogger(MguUsbAdc10CommandRegistry.class);

    @Override
    protected void initCommands() {
        commandList.addCommand(createGinfCmd());
        commandList.addCommand(createGconCmd());
        commandList.addCommand(createGdtaCmd());
        // Добавление ошибочных команд
        commandList.addCommand(createErrvCmd());
        commandList.addCommand(createErrdCmd());
    }

    // ===== КОМАНДЫ УСТРОЙСТВА =====

    private SingleCommand createGinfCmd() {
        return new SingleCommand(
                "ginf",
                "ginf - Получить информацию об устройстве",
                this::parseGinfResponse,
                500  // 4 + 72 + 2
        );
    }

    private SingleCommand createGconCmd() {
        return new SingleCommand(
                "gcon",
                "gcon - Получить калиброванные данные АЦП",
                this::parseGconGdtaResponse,
                500  // 4 + 20 + 2
        );
    }

    private SingleCommand createGdtaCmd() {
        return new SingleCommand(
                "gdta",
                "gdta - Получить сырые данные АЦП",
                this::parseGconGdtaResponse,
                500  // 4 + 20 + 2
        );
    }

    // ===== СЛУЖЕБНЫЕ ОШИБКИ =====

    private SingleCommand createErrvCmd() {
        return new SingleCommand(
                "errv",
                "errv - Ошибка значения (неверный параметр)",
                bytes -> null,  // Нет данных, возвращаем null
                4
        );
    }

    private SingleCommand createErrdCmd() {
        return new SingleCommand(
                "errd",
                "errd - Ошибка устройства (внутренняя ошибка)",
                bytes -> null,  // Нет данных, возвращаем null
                4
        );
    }

    // ===== ПАРСЕРЫ ОТВЕТОВ =====

    private AnswerValues parseGinfResponse(byte[] response) {
        try {
            if (response.length != 78) {
                log.warn("Invalid ginf response length: " + response.length);
                return null;
            }

            String cid = new String(response, 0, 4, StandardCharsets.US_ASCII);
            if (!cid.equals("ginf")) {
                log.warn("CID mismatch in ginf response");
                return null;
            }

            byte[] data = Arrays.copyOfRange(response, 4, 76);
            ByteBuffer crcBuffer = ByteBuffer.wrap(response, 76, 2).order(ByteOrder.LITTLE_ENDIAN);
            short crcReceived = crcBuffer.getShort();

            MguUsbAdc10 device = new MguUsbAdc10(); // Assuming to access calculateCrc16, or make static
            short crcCalc = device.calculateCrc16(data);
            if (crcCalc != crcReceived) {
                log.warn("CRC mismatch in ginf response: calculated " + Integer.toHexString(crcCalc) + ", received " + Integer.toHexString(crcReceived));
                return null;
            }

            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

            byte[] manufacturerBytes = new byte[16];
            buffer.get(manufacturerBytes);
            String manufacturer = new String(manufacturerBytes, StandardCharsets.US_ASCII).trim();

            byte[] productNameBytes = new byte[16];
            buffer.get(productNameBytes);
            String productName = new String(productNameBytes, StandardCharsets.US_ASCII).trim();

            byte[] controllerNameBytes = new byte[16];
            buffer.get(controllerNameBytes);
            String controllerName = new String(controllerNameBytes, StandardCharsets.US_ASCII).trim();

            short hardwareMajor = (short) buffer.get();
            short hardwareMinor = (short) buffer.get();
            int hardwareBugfix = buffer.getShort() & 0xFFFF;

            short bootloaderMajor = (short) buffer.get();
            short bootloaderMinor = (short) buffer.get();
            int bootloaderBugfix = buffer.getShort() & 0xFFFF;

            short firmwareMajor = (short) buffer.get();
            short firmwareMinor = (short) buffer.get();
            int firmwareBugfix = buffer.getShort() & 0xFFFF;

            long serialNumber = buffer.getInt() & 0xFFFFFFFFL;

            byte[] reserved = new byte[8];
            buffer.get(reserved);

            AnswerValues answerValues = new AnswerValues(12);
            answerValues.addValue(1.0, manufacturer);
            answerValues.addValue(1.0, productName);
            answerValues.addValue(1.0, controllerName);
            answerValues.addValue(hardwareMajor, "HardwareMajor");
            answerValues.addValue(hardwareMinor, "HardwareMinor");
            answerValues.addValue(hardwareBugfix, "HardwareBugfix");
            answerValues.addValue(bootloaderMajor, "BootloaderMajor");
            answerValues.addValue(bootloaderMinor, "BootloaderMinor");
            answerValues.addValue(bootloaderBugfix, "BootloaderBugfix");
            answerValues.addValue(firmwareMajor, "FirmwareMajor");
            answerValues.addValue(firmwareMinor, "FirmwareMinor");
            answerValues.addValue(firmwareBugfix, "FirmwareBugfix");
            answerValues.addValue(serialNumber, "SerialNumber");

            return answerValues;
        } catch (Exception e) {
            log.error("Error parsing ginf response: " + e.getMessage());
            return null;
        }
    }

    private AnswerValues parseGconGdtaResponse(final byte[] response) {
        log.info("Run parse gcon/gdta response");
        log.info("Answer: " + MyUtilities.bytesToHex(response));
        log.info("AnswerLength: " + response.length);
        try {
            if (response.length != 26) {
                log.warn("Invalid gcon/gdta response length: " + response.length);
                return null;
            }

            String cid = new String(response, 0, 4, StandardCharsets.US_ASCII);
            if (!cid.equals("gcon") && !cid.equals("gdta")) {
                log.warn("CID mismatch in gcon/gdta response");
                return null;
            }

            byte[] data = Arrays.copyOfRange(response, 4, 24);
            ByteBuffer crcBuffer = ByteBuffer.wrap(response, 24, 2).order(ByteOrder.LITTLE_ENDIAN);
            short crcReceived = crcBuffer.getShort();

            MguUsbAdc10 device = new MguUsbAdc10(); // Assuming to access, or make static
            short crcCalc = device.calculateCrc16(data);
            if (crcCalc != crcReceived) {
                log.warn("CRC mismatch in gcon/gdta response: calculated " + Integer.toHexString(crcCalc) + ", received " + Integer.toHexString(crcReceived));
                return null;
            }

            ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
            AnswerValues answerValues = new AnswerValues(10);
            boolean isGcon = cid.equals("gcon");
            for (int i = 0; i < 10; i++) {
                int rawValue = buffer.getShort() & 0xFFFF;
                if (isGcon) {
                    double value = rawValue / 10.0; // since uint16 in 100 uV = value / 10 mV
                    answerValues.addValue(value, "mV");
                } else {
                    answerValues.addValue(rawValue, "raw");
                }
            }
            return answerValues;
        } catch (Exception e) {
            log.error("Error parsing gcon/gdta response: " + e.getMessage());
            return null;
        }
    }
}