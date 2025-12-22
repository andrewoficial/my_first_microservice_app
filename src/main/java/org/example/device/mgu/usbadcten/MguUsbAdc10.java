package org.example.device.mgu.usbadcten;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.DeviceCommandListClass;
import org.example.device.ProtocolComPort;
import org.example.device.SomeDevice;
import org.example.device.TemplatedAscii;
import org.example.device.command.SingleCommand;
import org.example.device.connectParameters.ComConnectParameters;
import org.example.services.AnswerValues;
import org.example.services.comPort.*;
import org.example.utilites.MyUtilities;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MguUsbAdc10 implements SomeDevice, ProtocolComPort, TemplatedAscii {
    private static final Logger log = Logger.getLogger(MguUsbAdc10.class);
    @Getter
    private final ComConnectParameters comParameters = new ComConnectParameters(); // Типовые параметры связи для прибора
    private final SerialPort comPort;
    @Getter
    private final DataBitsList defaultDataBit = DataBitsList.B8;
    @Getter
    private final ParityList defaultParity = ParityList.P_NO;
    @Getter
    private final BaudRatesList defaultBaudRate = BaudRatesList.B57600;
    @Getter
    private final StopBitsList defaultStopBit = StopBitsList.S1;

    private final DeviceCommandListClass commands;
    private final MguUsbAdc10CommandRegistry commandRegistry;

    private byte[] lastAnswerBytes = new byte[1];
    private StringBuilder lastAnswer = new StringBuilder();
    private AnswerValues answerValues = null;
    private int received = 0;

    private String cmdToSend;
    private int expectedBytes = 0;

    private String devIdent = "MguUsbAdc10";

    public MguUsbAdc10() {
        log.info("Создан объект протокола MguUsbAdc10 эмуляция");
        this.comPort = null;
        this.commandRegistry = new MguUsbAdc10CommandRegistry();
        this.commands = commandRegistry.getCommandList();
    }

    public MguUsbAdc10(SerialPort port) {
        log.info("Создан объект протокола MguUsbAdc10");
        this.comPort = port;
        this.commandRegistry = new MguUsbAdc10CommandRegistry();
        this.commands = commandRegistry.getCommandList();
        comParameters.setDataBits(defaultDataBit);
        comParameters.setParity(defaultParity);
        comParameters.setBaudRate(defaultBaudRate);
        comParameters.setStopBits(defaultStopBit);
        comParameters.setStringEndian(StringEndianList.CR); //CR
        comParameters.setMillisLimit(300); // Таймаут 2 сек
        comParameters.setRepeatWaitTime(50);
        this.enable();
    }

    @Override
    public DeviceCommandListClass getCommandListClass() {
        return this.commands;
    }

    @Override
    public void setCmdToSend(String str) {
        if (str == null || str.isEmpty()) {
            expectedBytes = 500;
            cmdToSend = null;
        } else {
            // Получает количество ожидаемых байт
            expectedBytes = commands.getExpectedBytes(str); // ToDo распространить на остальные девайсы
            cmdToSend = str;
        }
    }

    @Override
    public int getExpectedBytes() {
        return expectedBytes;
    }

    @Override
    public byte[] getStrEndian() {
        return new byte[0]; // Нет специального окончания
    }

    @Override
    public SerialPort getComPort() {
        return this.comPort;
    }

    @Override
    public boolean isKnownCommand() {
        return commands.isKnownCommand(cmdToSend);
    }

    @Override
    public int getReceivedCounter() {
        return received;
    }

    @Override
    public void setReceivedCounter(int cnt) {
        this.received = cnt;
    }

    public void setReceived(String answer) {
        lastAnswerBytes = answer.getBytes();
        this.received = lastAnswerBytes.length;
    }

    @Override
    public long getMillisLimit() {
        return comParameters.getMillisLimit();
    }

    @Override
    public int getMillisReadLimit() {
        return comParameters.getMillisReadLimit();
    }

    @Override
    public int getMillisWriteLimit() {
        return comParameters.getMillisWriteLimit();
    }

    @Override
    public long getRepeatWaitTime() {
        return comParameters.getRepeatWaitTime();
    }

    @Override
    public void setLastAnswer(byte[] sb) {
        lastAnswerBytes = sb;
    }

    public boolean enable() {
        return true;
    }

    @Override
    public void parseData() {
        if (lastAnswerBytes != null && lastAnswerBytes.length > 0) {
            // Игнорировать начальные нулевые байты
            int start = 0;
            while (start < lastAnswerBytes.length && lastAnswerBytes[start] == 0) {
                start++;
            }
            if (start > 0) {
                lastAnswerBytes = Arrays.copyOfRange(lastAnswerBytes, start, lastAnswerBytes.length);
            }

            log.info("Command: " + MyUtilities.bytesToHex(cmdToSend.getBytes()));
            log.info("Answer: " + MyUtilities.bytesToHex(lastAnswerBytes));
            log.info("AnswerLength: " + lastAnswerBytes.length);

            lastAnswer.setLength(0); // Очистка строкового представления ответа

            if (lastAnswerBytes.length < 4) {
                log.warn("Response too short");
                lastAnswer.append("Error: Response too short");
                return;
            }

            String cid;
            try {
                cid = new String(lastAnswerBytes, 0, 4, StandardCharsets.US_ASCII);
            } catch (Exception e) {
                log.warn("Invalid CID");
                lastAnswer.append("Error: Invalid CID");
                return;
            }

            if (!cid.equals(cmdToSend)) {
                if (cid.equals("errv") || cid.equals("errd")) {
                    log.warn("Error response: " + cid);
                    lastAnswer.append("Error: " + cid);
                } else {
                    log.warn("CID mismatch: expected " + cmdToSend + ", got " + cid);
                    performZeroSync();
                    lastAnswer.append("Error: CID mismatch");
                }
                answerValues = null;
                return;
            }

            if (commands.isKnownCommand(cmdToSend)) { // Проверка наличия команды в реестре команд
                SingleCommand getCommand = commands.getCommand(cmdToSend);
                answerValues = getCommand.getResult(lastAnswerBytes); // Получение значений в ответе
                if (answerValues != null) {
                    for (int i = 0; i < answerValues.getValues().length; i++) {
                        lastAnswer.append(answerValues.getValues()[i]);
                        lastAnswer.append(" ");
                        lastAnswer.append(answerValues.getUnits()[i]);
                        lastAnswer.append("  ");
                    }
                } else {
                    for (byte lastAnswerByte : lastAnswerBytes) {
                        lastAnswer.append((char) lastAnswerByte);
                    }
                    log.info("MguUsbAdc10 Cant create answers obj (error in answer)");
                }
            } else {
                for (byte lastAnswerByte : lastAnswerBytes) {
                    lastAnswer.append((char) lastAnswerByte);
                }
                log.info("MguUsbAdc10 Cant create answers obj (unknown command)");
            }
        } else {
            log.info("MguUsbAdc10 empty received");
        }
    }

    public String getAnswer() {
        if (hasAnswer()) {
            received = 0;
            lastAnswerBytes = null;
            return lastAnswer.toString();
        } else {
            return null;
        }
    }

    public boolean hasAnswer() {
        return lastAnswerBytes != null && lastAnswerBytes.length > 0;
    }

    @Override
    public boolean hasValue() {
        return answerValues != null;
    }

    public AnswerValues getValues() {
        return this.answerValues;
    }

    // Дополнительные методы для протокола uRPC

    /**
     * Вычисляет CRC-16 Modbus для данных
     */
    public short calculateCrc16(byte[] data) {
        int crc = 0xFFFF;
        for (byte b : data) {
            crc ^= (b & 0xFF);
            for (int i = 0; i < 8; i++) {
                if ((crc & 0x0001) != 0) {
                    crc = (crc >> 1) ^ 0xA001;
                } else {
                    crc >>= 1;
                }
            }
        }
        return (short) crc;
    }

    /**
     * Проверяет CRC в ответе
     */
    private boolean verifyCrc(byte[] data, short receivedCrc) {
        short calculated = calculateCrc16(data);
        return calculated == receivedCrc;
    }

    /**
     * Выполняет Zero Sync
     */
    public void performZeroSync() {
        log.info("Performing Zero Sync");
        byte[] zeros = new byte[100];
        Arrays.fill(zeros, (byte) 0x00);
        // Отправить 100 нулей
        comPort.writeBytes(zeros, 100);
        // Ожидать один ноль
        byte[] response = new byte[1];
        if (comPort.readBytes(response, 1) == 1 && response[0] == 0x00) {
            log.info("Zero Sync successful");
        } else {
            log.warn("Zero Sync failed");
        }
    }

    // Переопределить методы отправки/чтения если нужно, но предполагаем, что базовый ProtocolComPort обрабатывает
}