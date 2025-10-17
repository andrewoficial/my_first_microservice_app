package org.example.gui.mgstest.parser.answer.mkrs;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class AdvancedResponseParserMKRS {

    /**
     * Парсит ответ от устройства MKRS
     * Структура: [Header] [TotalPages] [CurrPage] [SizePayloadFromZero] [FF 55 00 13 B3 A1] [02 18 00 02] [PAYLOAD] [CRC]
     */
    public String parseMkrsResponse(byte[] data) throws Exception {
        if (data.length < 15) {
            throw new Exception("Данные слишком короткие");
        }

        // Проверяем заголовок (первые 4 байта)
        byte header1 = data[0];
        byte header2 = data[1];
        byte header3 = data[2];

        // TotalPages и CurrPage (байты 1 и 2)
        int totalPages = header2 & 0xFF;
        int currPage = header3 & 0xFF;

        // SizePayloadFromZero (байт 3) - размер от начала маркера FF 55 до конца PAYLOAD
        int sizePayloadFromZero = data[3] & 0xFF;
        System.out.println("SizePayloadFromTotalPages: " + sizePayloadFromZero);

        // Проверяем маркер FF 55
        if (data[4] != (byte) 0xFF || data[5] != 0x55) {
            throw new Exception("Неверный маркер FF 55");
        }

        // Фиксированная часть после FF 55: 00 13 B3 A1 02 18 00 02
        byte[] fixedPart = new byte[8];
        System.arraycopy(data, 6, fixedPart, 0, 8);

        // Начало PAYLOAD (после фиксированной части)
        int payloadStart = 14; // 4(заголовок) + 1(FF) + 1(55) + 8(фикс.часть)

        // Длина PAYLOAD = SizePayloadFromZero - (длина от FF 55 до начала PAYLOAD) - 2(CRC)
        // От FF 55 до начала PAYLOAD: 1(FF) + 1(55) + 8(фикс.часть) = 10 байт
        int payloadLength = sizePayloadFromZero - 10 - 2;

        if (payloadStart + payloadLength + 2 > data.length) {
            throw new Exception("Неверная длина PAYLOAD");
        }

        // Извлекаем PAYLOAD
        byte[] payload = new byte[payloadLength];
        System.arraycopy(data, payloadStart, payload, 0, payloadLength);

        // Извлекаем CRC (2 байта после PAYLOAD)
        int crcStart = payloadStart + payloadLength;
        short receivedCrc = (short) ((data[crcStart] & 0xFF) | ((data[crcStart + 1] & 0xFF) << 8));

        // Данные для расчета CRC: от байта 0x55 до конца PAYLOAD
        int crcDataStart = 5; // позиция 0x55
        int crcDataLength = (payloadStart + payloadLength) - crcDataStart;
        byte[] crcData = new byte[crcDataLength];
        System.arraycopy(data, crcDataStart, crcData, 0, crcDataLength);

        // Вычисляем CRC
        short calculatedCrc = computeCRC16(crcData);

        System.out.println("Полученный CRC: " + String.format("%04X", receivedCrc));
        System.out.println("Вычисленный CRC: " + String.format("%04X", calculatedCrc));

        if (calculatedCrc != receivedCrc) {
            throw new Exception(String.format("Ошибка CRC: рассчитанный %04X, полученный %04X",
                    calculatedCrc, receivedCrc));
        }

        // Преобразуем PAYLOAD в строку
        String response = new String(payload, "US-ASCII").trim();

        System.out.println("TotalPages: " + totalPages);
        System.out.println("CurrPage: " + currPage);
        System.out.println("Response: " + response);

        return response;
    }

    /**
     * Вычисляет CRC16 (MODBUS)
     */
    private static short computeCRC16(byte[] data) {
        int crc = 0xFFFF; // начальное значение

        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >>> 1) ^ 0xA001; // полином CRC16-MODBUS (обратный 0x8005)
                } else {
                    crc = crc >>> 1;
                }
            }
        }

        return (short) (crc & 0xFFFF);
    }

}