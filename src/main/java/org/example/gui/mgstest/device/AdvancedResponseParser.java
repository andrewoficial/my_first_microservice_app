package org.example.gui.mgstest.device;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.transport.CradleController;
import org.example.utilites.LittleEndianUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

public class AdvancedResponseParser {

    private static final byte START_MARKER = 0x0A;    // Признак начала блока
    private static final byte END_MARKER = (byte)0xFE; // Признак конца блока

    /**
     * Основной метод парсинга: ищет структурированные блоки и проверяет CRC
     */
    public static List<ParsedBlock> parseStructuredResponse(byte[] data) {
        final Logger log = Logger.getLogger(AdvancedResponseParser.class);
        List<ParsedBlock> result = new ArrayList<>();
        int index = 0;
        log.info("Начинаю разбор посылки");
        while (index < data.length - 7) { // Минимум нужно 7 байт: 0x0A + 2(длина) + 1(данные) + 4(CRC)

            // Начало блока (0x0A)
            if (data[index] == START_MARKER) {
                try {
                    log.info("Начало найдено");
                    ParsedBlock block = parseBlock(data, index);
                    result.add(block);
                    log.info("Блок текста " + block.getPayloadAsString());
                    index = block.getNextIndex(); // Переходим к следующему блоку
                } catch (InvalidBlockException e) {
                    // Если блок некорректен, продолжаем поиск со следующего байта
                    index++;
                }
            } else {
                index++;
            }
        }

        return result;
    }

    /**
     * Парсит один блок данных начиная с указанной позиции
     */
    private static ParsedBlock parseBlock(byte[] data, int startIndex) throws InvalidBlockException {
        if (startIndex + 7 >= data.length) {
            throw new InvalidBlockException("Недостаточно данных для парсинга блока");
        }

        // Читаем длину данных (2 байта, little-endian)
        int length = ((data[startIndex + 1] & 0xFF) << 8) | (data[startIndex + 2] & 0xFF);

        // Проверяем, что данных достаточно
        if (startIndex + 3 + length + 4 >= data.length) {
            throw new InvalidBlockException("Недостаточно данных для указанной длины: " + length);
        }

        // Извлекаем данные
        int dataStart = startIndex + 3;
        int dataEnd = dataStart + length;
        byte[] payload = new byte[length];
        System.arraycopy(data, dataStart, payload, 0, length);

        // Извлекаем CRC (4 байта, little-endian)
        byte[] receivedCrc = new byte[4];
        System.arraycopy(data, dataEnd, receivedCrc, 0, 4);

        // Проверяем маркер конца
        if (data[dataEnd + 4] != END_MARKER) {
            throw new InvalidBlockException("Отсутствует маркер конца блока 0xFE");
        }

        // Вычисляем CRC для проверки
        long calculatedCrc = calculateCrc32(payload);
        long receivedCrcValue = bytesToLongLE(receivedCrc, 0);

        return new ParsedBlock(startIndex, length, payload, receivedCrcValue,
                calculatedCrc, dataEnd + 5);
    }

    /**
     * Вычисляет CRC32 для данных
     */
    public static long calculateCrc32(byte[] data) {
        return LittleEndianUtils.calculateCrc32(data);
    }

    /**
     * Преобразует массив байт (little-endian) в long
     */
    public static long bytesToLongLE(byte[] bytes, int offset) {
        return ByteBuffer.wrap(bytes, offset, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .getInt() & 0xFFFFFFFFL;
    }

    /**
     * Преобразует long в массив байт (little-endian)
     */
    public static byte[] longToBytesLE(long value) {
        return ByteBuffer.allocate(4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) value)
                .array();
    }

    /**
     * Старый метод для обратной совместимости и отладки
     */
    public static List<String> extractAllTextResponses(byte[] data) {
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

    /**
     * Класс для хранения распарсенного блока
     */
    public static class ParsedBlock {
        private final int startIndex;
        private final int length;
        private final byte[] payload;
        private final long receivedCrc;
        private final long calculatedCrc;
        private final int nextIndex;
        private final boolean crcValid;

        public ParsedBlock(int startIndex, int length, byte[] payload,
                           long receivedCrc, long calculatedCrc, int nextIndex) {
            this.startIndex = startIndex;
            this.length = length;
            this.payload = payload;
            this.receivedCrc = receivedCrc;
            this.calculatedCrc = calculatedCrc;
            this.nextIndex = nextIndex;
            this.crcValid = (receivedCrc == calculatedCrc);
        }

        // Getters
        public int getStartIndex() { return startIndex; }
        public int getLength() { return length; }
        public byte[] getPayload() { return payload; }
        public long getReceivedCrc() { return receivedCrc; }
        public long getCalculatedCrc() { return calculatedCrc; }
        public int getNextIndex() { return nextIndex; }
        public boolean isCrcValid() { return crcValid; }

        public String getPayloadAsString() {
            return new String(payload, java.nio.charset.StandardCharsets.US_ASCII);
        }

        @Override
        public String toString() {
            return String.format("Block[start=%d, length=%d, crcValid=%b, data='%s']",
                    startIndex, length, crcValid, getPayloadAsString());
        }
    }

    /**
     * Исключение для некорректных блоков
     */
    private static class InvalidBlockException extends Exception {
        public InvalidBlockException(String message) {
            super(message);
        }
    }
}