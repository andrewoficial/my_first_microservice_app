package org.example.gui.mgstest.transport;

import org.apache.log4j.Logger;
import org.hid4java.HidDevice;

public class CradleController {
    SomeHidController hidController = new SomeHidController();
    private final Logger log = Logger.getLogger(CradleController.class);

    // Вкл NFC
    public void cradleSwitchOn(HidDevice device) {
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x01, (byte) 0x0D});
        safetySleep(400);
    }

    // Выкл NFC
    public void cradleSwitchOff(HidDevice device) {
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x02, (byte) 0x02, (byte) 0x00, (byte) 0x00});
        safetySleep(400);
    }

    // Запись блока (4 байта data)
    public void cradleWriteBlock(HidDevice device, byte blockId, byte[] data) {
        if (data.length != 4) {
            throw new IllegalArgumentException("Data must be 4 bytes");
        }
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, blockId, data[0], data[1], data[2], data[3]});
        safetySleep(200);
    }

    // Отправка счётчика (overwrite блока 0x03)
    public void cradleSendCount(HidDevice device, int count) {
        byte low = (byte) (count & 0xFF);
        byte high = (byte) ((count >> 8) & 0xFF);
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, low, high, (byte) 0x00});
        safetySleep(200);
    }

    // Активация передачи в прибор
    public void cradleActivateTransmit(HidDevice device) {
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x00, (byte) 0xE1, (byte) 0x40, (byte) 0xFF, (byte) 0x01});
        safetySleep(200);
    }

    // Чтение ответа (по offset, size обычно 0x07)
    public byte[]  cradleReadResponse(HidDevice device, byte offset, byte size) {
        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x04, (byte) 0x02, (byte) 0x23, offset, size});
        safetySleep(1000);  // Увеличено для стабильности

        byte[] buffer = new byte[64];
        int bytesRead = device.read(buffer, 1000);
        if (bytesRead > 0) {
            log.info("Прочитано в ответ: " + bytesRead);
            hidController.printArrayLikeDeviceMonitor(buffer);  // Предполагаю, commander — твой логгер массива
        } else {
            log.error("Нет ответа");
        }
        return buffer;  // Возвращаем для парсинга
    }

    // Получение данных о приборе (0x2E)
    public byte[] getDeviceInfo(HidDevice device) throws Exception {
        log.info("Run get device info");

        cradleSwitchOn(device); //01 02 02 01 0D
        safetySleep(100);
        readResponse(device);   // ожидаю 07 00 00 00 dc
        safetySleep(100);

        hidController.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, 0x00});//Команда из дампа
        safetySleep(100);
        readResponse(device);   // ожидаю 07 80 04 00 78 f0
        safetySleep(100);

        hidController.simpleSend(device, new byte[]{0x01, 0x04, 0x07, 0x02, 0x21, 0x01, 0x03, 0x11, (byte) 0xD1, 0x01});//Команда из дампа
        safetySleep(100);
        readResponse(device);   // ожидаю 07 80 04 00 78 f0 00 00 00 00 54
        safetySleep(100);


        cradleWriteBlock(device, (byte) 0x02, new byte[]{0x0D, 0x54, 0x02, 0x65});
        safetySleep(100);
        readResponse(device);// ожидаю 07 80 04 00 78 f0 00 00 00 00 54
        safetySleep(100);

        cradleWriteBlock(device, (byte) 0x03, new byte[]{0x6E, 0x00, 0x00, 0x00});
        safetySleep(100);
        readResponse(device);
        safetySleep(100);// ожидаю 07 80 04 00 78 f0 00 00 00 00 54

        cradleWriteBlock(device, (byte) 0x04, new byte[]{0x00, 0x2E, 0x00, 0x01}); // Команда 0x2E
        safetySleep(100);
        readResponse(device);
        safetySleep(100);// ожидаю 07 80 04 00 78 f0 00 00 00 00 54

        cradleWriteBlock(device, (byte) 0x05, new byte[]{0x00, 0x00, 0x00, (byte) 0xFE});
        safetySleep(100);
        readResponse(device);
        safetySleep(100);// ожидаю 07 80 04 00 78 f0 00 00 00 00 54

        cradleSendCount(device, 6); // Отправка счётчика (проверено еще раз)
        safetySleep(100);
        readResponse(device);
        safetySleep(100);//ожидаю 07 80 04 00 78 f0 00 00 00 00 54

        hidController.simpleSend(device, new byte[]{(byte) 0x01, (byte) 0x04, (byte) 0x07, (byte) 0x02, (byte) 0x21, (byte) 0x03, (byte) 0x6E, (byte) 0x06, (byte) 0x00});
        safetySleep(100);
        readResponse(device);
        safetySleep(100);//ожидаю 07 80 04 00 78 f0 00 00 00 00 54

        cradleActivateTransmit(device); // Активация передачи//01 04 07 02 21 03 6e 06 должно быть отправлено
        safetySleep(500);
        readResponse(device);//ожидаю 07 80 04 00 78 f0 00 00 00 00 54
        safetySleep(500); // Увеличена задержка после активации

        cradleSwitchOff(device);//01 04 07 02 21 00 e1 40 ff 01 должно быть отправлено
        safetySleep(100);
        readResponse(device);//ожидаю 07 80 04 00 78 f0 00 00 00 00 54
        safetySleep(100);

        // Чтение ответа (4 блока по 32 байта, как в дампе)
        cradleSwitchOn(device);
        readResponse(device);
        safetySleep(500); // Большая задержка перед чтением данных

        byte[] block0 = cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        safetySleep(50);
        byte[] block1 = cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        safetySleep(50);
        byte[] block2 = cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        safetySleep(50);
        byte[] block3 = cradleReadResponse(device, (byte) 0x18, (byte) 0x07); // Добавлен 4-й блок
        safetySleep(50);

        cradleSwitchOff(device);
        readResponse(device);
        safetySleep(100);

        // Объединение всех 4 блоков
        byte[] result = new byte[128];
        System.arraycopy(block0, 0, result, 0, 32);
        System.arraycopy(block1, 0, result, 32, 32);
        System.arraycopy(block2, 0, result, 64, 32);
        System.arraycopy(block3, 0, result, 96, 32);

        return result;
    }

    private byte[] readResponse(HidDevice device) {
        byte[] buffer = new byte[64];
        int bytesRead = device.read(buffer, 1000);
        if (bytesRead > 0) {
            log.info("Ответ на команду: ");
            hidController.printArrayLikeDeviceMonitor(buffer);
        } else {
            log.error("Нет ответа на команду");
        }
        return buffer;
    }

    public String readerSequence3(HidDevice device) {
        cradleSwitchOn(device);
        byte[] response = cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleSwitchOff(device);

        // Проверка сигнатуры ответа
        if (response[11] == (byte) 0xE0 && response[10] == 0x02 && response[0] == (byte) 0x80) {
            StringBuilder sensorId = new StringBuilder();
            for (int i = 4; i <= 11; i++) {
                sensorId.append(String.format("%02X", response[i]));
            }
            return sensorId.reverse().toString(); // CPUId в обратном порядке
        }
        return null;
    }

    // Выключение звука (SwitchBeepByte, command 0x22, X=0x01)
    public void soundOff(HidDevice device) {
        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x1B, (byte) 0xDF, (byte) 0x05, (byte) 0xA5});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x0B);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        cradleSwitchOff(device);
    }

    // Включение звука (SwitchBeepByte, command 0x22, X=0x00)
    public void soundOn(HidDevice device) {
        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x16, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x12, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x22, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x8D, (byte) 0xEF, (byte) 0x02, (byte) 0xD2});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x0B);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        cradleSwitchOff(device);
    }

    // Тест мигания (BlinkByte, command 0x27)
    public void blinkTest(HidDevice device) {
        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x11, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x0D, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x27, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xFE});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x06);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        cradleSwitchOff(device);
    }

    // Тест звукового сигнала (OneBeepByte, command 0x2F)
    public void beepTest(HidDevice device) {
        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x11, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x0D, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x2F, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xFE});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0xBD});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x06);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        cradleSwitchOff(device);
    }

    // Перезагрузка устройства (RebootByte, command 0x17)
    public void rebootCmd(HidDevice device) {
        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x17, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x13, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x17, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xB9});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0xA9, (byte) 0x42, (byte) 0x1C, (byte) 0xD4});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xDB, (byte) 0xFE, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x0C);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        cradleSwitchOff(device);
    }

    // Сброс счётчика батареи (ReplButtByte, command 0x46)
    public void resetBatteryCounter(HidDevice device) {
        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x11, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x0D, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x46, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0xFE});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{(byte) 0xFE, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x06);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleReadResponse(device, (byte) 0x00, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x08, (byte) 0x07);
        cradleReadResponse(device, (byte) 0x10, (byte) 0x07);
        cradleSwitchOff(device);
    }

    // Установка серийного номера (SetSerialNumberByte, command 0x40)
    public void setSerialNumber(HidDevice device, long serialNumber) {
        if (serialNumber < 0 || serialNumber > 0xFFFFFFFFL) {
            throw new IllegalArgumentException("Serial number must be between 0 and 4294967295");
        }

        // Разбиваем serialNumber на байты (little-endian)
        byte byte0 = (byte) (serialNumber & 0xFF);
        byte byte1 = (byte) ((serialNumber >> 8) & 0xFF);
        byte byte2 = (byte) ((serialNumber >> 16) & 0xFF);
        byte byte3 = (byte) ((serialNumber >> 24) & 0xFF);

        // Данные для CRC (только серийный номер, без 01 00 00)
        byte[] crcData = new byte[]{byte0, byte1, byte2, byte3};
        int crc = calculateCRC(crcData);
        byte[] crcBytes = new byte[]{
                (byte) (crc & 0xFF),
                (byte) ((crc >> 8) & 0xFF),
                (byte) ((crc >> 16) & 0xFF),
                (byte) ((crc >> 24) & 0xFF)
        };

        cradleSwitchOn(device);

        cradleWriteBlock(device, (byte) 0x00, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x01, new byte[]{(byte) 0x03, (byte) 0x19, (byte) 0xD1, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x02, new byte[]{(byte) 0x15, (byte) 0x54, (byte) 0x02, (byte) 0x65});
        cradleWriteBlock(device, (byte) 0x03, new byte[]{(byte) 0x6E, (byte) 0x00, (byte) 0x00, (byte) 0x00});
        cradleWriteBlock(device, (byte) 0x04, new byte[]{(byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x01});
        cradleWriteBlock(device, (byte) 0x05, new byte[]{(byte) 0x01, (byte) 0x00, (byte) 0x00, byte0});
        cradleWriteBlock(device, (byte) 0x06, new byte[]{byte1, byte2, byte3, crcBytes[0]});
        cradleWriteBlock(device, (byte) 0x07, new byte[]{crcBytes[1], crcBytes[2], crcBytes[3], (byte) 0xFE});
        cradleWriteBlock(device, (byte) 0x08, new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00});

        cradleSendCount(device, 0x0E);

        cradleActivateTransmit(device);

        cradleSwitchOff(device);

        cradleSwitchOn(device);
        cradleSwitchOff(device);

    }

    // Пример CRC-вычисления (адаптировано из C++: CRC-32 reversed, poly 0xEDB88320)
    // Вычисляет над data (без header/FE)
    public int calculateCRC(byte[] data) {
        int crc = 0xFFFFFFFF;
        int poly = 0xEDB88320;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 1) != 0) {
                    crc = (crc >>> 1) ^ poly;
                } else {
                    crc = (crc >>> 1);
                }
            }
        }
        return ~crc;
    }

    private void safetySleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ex) {
            log.error("Sleep error " + ex.getMessage());
        }
    }
}