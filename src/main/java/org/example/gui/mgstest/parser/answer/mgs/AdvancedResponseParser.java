package org.example.gui.mgstest.parser.answer.mgs;

import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class AdvancedResponseParser {


    /**
     * Парсит ответ от MIPEX и извлекает строку ответа, проверяя CRC.
     * @param data Массив байт с сырым ответом (конкатенированный из блоков, начиная с arrRead[3] первого блока)
     * @return Строка ответа (например, "00101032")
     * @throws Exception Если ошибка валидации или парсинга
     */
    public String parseMipexResponse(byte[] data) throws Exception {
        if (data.length < 30) {
            throw new Exception("Данные слишком короткие");
        }

        // Извлечение nSize (arr[8], возможно с arr[9] если BITLEN16)
        int sizeIndex = 27; // Позиция arr[8] в data (если data начинается с arrRead[3] = data[0] = packet[0])
        int nSize = data[sizeIndex] & 0xFF;
        System.out.println("Предпологаю длинну посылки " + nSize);

        // Начало данных (arr[27] соответствует data[24], если нет padding)
        int dataStart = 27;
        int payloadLen = 2 + nSize;//[длинна_псылки  непонятное][посылка]
        if (payloadLen + 27 > data.length) {
            throw new Exception("Неверная длина пакета");
        }

        // Извлечение payload
        byte[] payload = new byte[payloadLen];
        System.arraycopy(data, 27, payload, 0, payloadLen);
        System.out.println(MyUtilities.bytesToHexString(payload));
        // Извлечение CRC
        byte[] crcBytes = new byte[4];
        System.arraycopy(data, dataStart + payloadLen, crcBytes, 0, 4);
        long receivedCrc = ByteBuffer.wrap(crcBytes).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xFFFFFFFFL;

        // Проверка FE
        if (data[dataStart + payloadLen + 4] != (byte) 0xFE) {
            //throw new Exception(" 0xFE");

        }

        // Вычисление CRC
        CRC32 crcObj = new CRC32();
        crcObj.update(payload);
        long calculatedCrc = crcObj.getValue();
        if (calculatedCrc != receivedCrc) {
            throw new Exception(String.format("Ошибка CRC: рассчитанный %08X, полученный %08X", calculatedCrc, receivedCrc));
        }

        // Извлечение под-длины, extra и строки
        if (payload.length < 3) {
            throw new Exception("Payload слишком короткий");
        }
        int subLen = payload[0] & 0xFF;
        if (subLen + 2 > payload.length) {
            throw new Exception("Неверная под-длина");
        }
        byte extra = payload[1];
        byte[] strBytes = new byte[subLen];
        System.arraycopy(payload, 2, strBytes, 0, subLen);

        // Преобразование в строку, удаление \r если нужно
        String response = new String(strBytes, "US-ASCII").replace("\r", "");

        return response;
    }




    /**
     * Старый метод для обратной совместимости и отладки
     */
    public List<String> extractAllTextResponses(byte[] data) {
        List<String> result = new ArrayList<>();
        StringBuilder currentString = new StringBuilder();
        boolean inString = false;

        for (int i = 0; i < data.length; i++) {
            byte b = data[i];

            if (b >= 0x20 && b <= 0x7E) {
                if (!inString) {
                    inString = true;
                }
                currentString.append((char) b);
            }
            else if (inString) {
                if (currentString.length() >= 3) {
                    result.add(currentString.toString());
                }
                currentString.setLength(0);
                inString = false;
            }
        }

        if (inString && currentString.length() >= 3) {
            result.add(currentString.toString());
        }

        return result;
    }
}