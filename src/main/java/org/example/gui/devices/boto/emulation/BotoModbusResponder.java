package org.example.gui.devices.boto.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.device.protBoto.BotoModbusUtil;

/**
 * Modbus RTU-отклик термокамеры BOTO.
 * <p>
 * Поддерживает функции 0x03 (чтение holding) и 0x06 (запись одного регистра).
 * Регистры: tempReg (текущая T × scale), setTempReg (уставка × scale), modReg (вкл/выкл).
 */
@Slf4j
public class BotoModbusResponder {

    private final BotoEmulator emulator;
    private final int slaveId;
    private final int tempReg;
    private final int setTempReg;
    private final int modReg;
    private final int tempScale;

    /**
     * Ручные значения по адресам регистров (адрес → значение). Если адрес присутствует,
     * при чтении отдаётся это значение (в приоритете над авто-вычислением). Используется
     * для подбора карты регистров по штатной программе.
     */
    private final java.util.concurrent.ConcurrentHashMap<Integer, Integer> manualRegisters
            = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * Режим маппинга: при чтении незамапленных регистров возвращать их собственный адрес,
     * чтобы по штатной программе было видно, какой регистр соответствует полю.
     */
    private volatile boolean addressMapping = false;
    public BotoModbusResponder(BotoEmulator emulator, int slaveId,
                               int tempReg, int setTempReg, int modReg, int tempScale) {
        this.emulator = emulator;
        this.slaveId = slaveId;
        this.tempReg = tempReg;
        this.setTempReg = setTempReg;
        this.modReg = modReg;
        this.tempScale = tempScale;
    }

    public void setAddressMapping(boolean addressMapping) { this.addressMapping = addressMapping; }
    public boolean isAddressMapping() { return addressMapping; }

    /** Установить ручное значение регистра (перекрывает авто-чтение). */
    public void setManualRegister(int addr, int value) { manualRegisters.put(addr, value); }

    /** Убрать ручное значение регистра. */
    public void removeManualRegister(int addr) { manualRegisters.remove(addr); }

    /** Очистить все ручные значения. */
    public void clearManualRegisters() { manualRegisters.clear(); }

    /** Текущие ручные значения (копия). */
    public java.util.Map<Integer, Integer> manualRegisters() { return new java.util.HashMap<>(manualRegisters); }

    /**
     * Обработка входящего Modbus-запроса. Возвращает ответный кадр (с CRC) или null.
     */
    public synchronized byte[] processRequest(byte[] request) {
        if (request == null || request.length < 4) {
            log.warn("BotoEmu: слишком короткий кадр ({} байт): {}",
                    request == null ? 0 : request.length,
                    BotoModbusUtil.bytesToHex(request));
            return null;
        }
        if (!BotoModbusUtil.validateCrc(request)) {
            short recv = (short) ((request[request.length - 1] & 0xFF) << 8 | (request[request.length - 2] & 0xFF));
            short calc = BotoModbusUtil.calculateCrc16(
                    java.util.Arrays.copyOfRange(request, 0, request.length - 2));
            log.warn("BotoEmu: CRC ошибка. Принят: {} | CRC в кадре=0x{:04X} расчётный=0x{:04X}",
                    BotoModbusUtil.bytesToHex(request),
                    recv & 0xFFFF, calc & 0xFFFF);
            return null;
        }
        int addr = request[0] & 0xFF;
        if (addr != slaveId && addr != 0) return null;

        int function = request[1] & 0xFF;
        switch (function) {
            case 0x03: return handleRead(request);
            case 0x06: return handleWriteSingle(request);
            case 0x10: return handleWriteMultiple(request);
            default:
                log.warn("BotoEmu: неподдерживаемая функция 0x{}", String.format("%02X", function));
                return buildExceptionResponse(addr, function, 0x01);
        }
    }

    private byte[] handleRead(byte[] request) {
        int startReg = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
        if (quantity < 1 || quantity > 125) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }

        java.nio.ByteBuffer resp = java.nio.ByteBuffer.allocate(3 + 2 * quantity);
        resp.order(java.nio.ByteOrder.BIG_ENDIAN);
        resp.put((byte) slaveId);
        resp.put((byte) 0x03);
        resp.put((byte) (2 * quantity));

        for (int i = 0; i < quantity; i++) {
            int reg = startReg + i;
            int val = readRegister(reg);
            resp.putShort((short) val);
        }

        return BotoModbusUtil.appendCrc(resp.array());
    }

    private byte[] handleWriteSingle(byte[] request) {
        int regAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int value = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);

        applyWrite(regAddr, value);

        // Echo-ответ для 0x06
        java.nio.ByteBuffer resp = java.nio.ByteBuffer.allocate(6);
        resp.order(java.nio.ByteOrder.BIG_ENDIAN);
        resp.put((byte) slaveId);
        resp.put((byte) 0x06);
        resp.putShort((short) regAddr);
        resp.putShort((short) value);
        return BotoModbusUtil.appendCrc(resp.array());
    }
    private int readRegister(int reg) {
        // Блок реального времени. Базовый адрес TEMP_PV = tempReg.
        // По результатам маппинга (штатная программа) внутри блока:
        //   +1 = TEMP_SP (уставка темп.), +3 = MV темп.,
        //   +4 = HUMI_PV, +5 = HUMI_SP, +7 = MV влажности.
        // Рег +2 и +6 — не десятичные поля (не проявились в маппинге как 1.2/1.6),
        // предположительно бинарный статус/режим. Зеркалим состояние вкл/выкл.
        int base = tempReg;
        int run = emulator.isOn() ? 1 : 0;
        Integer manual = manualRegisters.get(reg);
        if (manual != null) {
            return manual & 0xFFFF;
        }
        if (reg == tempReg) {
            return (int) Math.round(emulator.getCurrentTempC() * tempScale);
        } else if (reg == base + 1 || reg == setTempReg) {
            return (int) Math.round(emulator.getSetpointC() * tempScale);
        } else if (reg == base + 2 || reg == base + 6) {
            return run;
        } else if (reg == base + 3) {
            return (int) Math.round(computeTempMv() * tempScale);
        } else if (reg == base + 4) {
            return (int) Math.round(emulator.getCurrentHumidity() * tempScale);
        } else if (reg == base + 5) {
            return (int) Math.round(emulator.getHumiditySetpoint() * tempScale);
        } else if (reg == base + 7) {
            return (int) Math.round(computeHumidityMv() * tempScale);
        } else if (reg == modReg) {
            return run;
        } else if (reg == 32 || reg == base + 22) {
            return emulator.getRunHours();
        } else if (reg == 33 || reg == base + 23) {
            return emulator.getRunMinutes();
        } else if (reg == 34 || reg == base + 24) {
            return emulator.getRunSeconds();
        } else if (reg == 39 || reg == base + 29) {
            return (int) Math.round(emulator.getTempMaxLimitC() * tempScale);
        } else if (reg == 38 || reg == base + 28) {
            return (short) Math.round(emulator.getTempMinLimitC() * tempScale);
        } else if (reg == 41 || reg == base + 31) {
            return (int) Math.round(emulator.getHumiMaxLimitPct() * tempScale);
        } else if (reg == 40 || reg == base + 30) {
            return (int) Math.round(emulator.getHumiMinLimitPct() * tempScale);
        } else if (reg == 31 || reg == base + 21) {
            return run;
        } else if (reg == 101) {
            return emulator.getTimeSetRaw();
        } else if (reg == 102) {
            return (int) Math.round(emulator.getTempRatePerMin() * tempScale);
        } else if (reg == 103) {
            return (int) Math.round(emulator.getHumiRatePerMin() * tempScale);
        } else if (reg >= 92 && reg <= 97) {
            return emulator.getSysTime()[reg - 92];
        } else if (reg >= 151 && reg <= 156) {
            return emulator.getReserveTime()[reg - 151];
        } else if (reg == 61) {
            return (int) Math.round(emulator.getHumiditySetpoint() * tempScale);
        } else if (reg == 62) {
            return emulator.getProgramNumber();
        } else if (reg == 70) {
            return emulator.getSegTotal();
        } else if (reg == 71) {
            return emulator.getSegCurrent();
        } else if (reg == 72) {
            return emulator.getCycleCurrent();
        } else if (reg == 73) {
            return emulator.getCycleTotal();
        } else if (reg == 74) {
            return emulator.getRmnTime()[0];
        } else if (reg == 75) {
            return emulator.getRmnTime()[1];
        } else if (reg == 76) {
            return emulator.getRmnTime()[2];
        } else if (reg == 111) {
            return (int) Math.round(emulator.getProgWaitTempC() * tempScale);
        } else if (reg == 112) {
            return (int) Math.round(emulator.getProgWaitHumPct() * tempScale);
        }
        // В режиме маппинга отдаём адрес регистра, чтобы узнать, какой регистр
        // соответствует полю в штатной программе.
        return addressMapping ? reg : 0;
    }

    /**
     * Степень коррекции температуры 0..100 (упрощённо, по рассогласованию).
     */
    private double computeTempMv() {
        if (!emulator.isOn()) return 0;
        double err = Math.abs(emulator.getSetpointC() - emulator.getCurrentTempC());
        return clampPct(err / 10.0 * 100.0);
    }

    /**
     * Степень коррекции влажности 0..100 (упрощённо).
     */
    private double computeHumidityMv() {
        if (!emulator.isOn()) return 0;
        double err = Math.abs(emulator.getHumiditySetpoint() - emulator.getCurrentHumidity());
        return clampPct(err / 10.0 * 100.0);
    }

    private static double clampPct(double v) {
        return Math.max(0, Math.min(100, v));
    }

    /**
     * Запись нескольких регистров (функция 0x10).
     * Применяет известные регистры, остальные принимает как успех (echo),
     * чтобы штатная программа не считала запись ошибкой.
     */
    private byte[] handleWriteMultiple(byte[] request) {
        if (request.length < 8) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }
        int startReg = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
        int byteCount = request[6] & 0xFF;

        if (quantity < 1 || quantity > 123 || byteCount != 2 * quantity
                || request.length < 7 + byteCount + 2) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }

        for (int i = 0; i < quantity; i++) {
            int regAddr = startReg + i;
            int value = ((request[7 + 2 * i] & 0xFF) << 8) | (request[8 + 2 * i] & 0xFF);
            applyWrite(regAddr, value);
        }

        // Echo-ответ для 0x10: slave + 0x10 + startReg(2) + quantity(2) + CRC
        java.nio.ByteBuffer resp = java.nio.ByteBuffer.allocate(8);
        resp.order(java.nio.ByteOrder.BIG_ENDIAN);
        resp.put((byte) slaveId);
        resp.put((byte) 0x10);
        resp.putShort((short) startReg);
        resp.putShort((short) quantity);
        return BotoModbusUtil.appendCrc(resp.array());
    }

    private void applyWrite(int regAddr, int value) {
        if (regAddr == setTempReg) {
            emulator.setSetpointC(value / (double) tempScale);
            log.info("BotoEmu: уставка темп → {} (raw {})", String.format("%.2f", value / (double) tempScale), value);
        } else if (regAddr == setTempReg + 1) {
            emulator.setHumiditySetpoint(value / (double) tempScale);
            log.info("BotoEmu: уставка влаги → {}% (raw {})", String.format("%.1f", value / (double) tempScale), value);
        } else if (regAddr == modReg) {
            emulator.setOn(value == 1);
            log.info("BotoEmu: вкл/выкл → {}", value == 1 ? "ВКЛ" : "ВЫКЛ");
        } else if (regAddr == tempReg) {
            emulator.setCurrentTempC(value / (double) tempScale);
        } else if (regAddr == 39) {
            emulator.setTempMaxLimitC(value / (double) tempScale);
        } else if (regAddr == 38) {
            emulator.setTempMinLimitC((short) value / (double) tempScale);
        } else if (regAddr == 41) {
            emulator.setHumiMaxLimitPct(value / (double) tempScale);
        } else if (regAddr == 40) {
            emulator.setHumiMinLimitPct(value / (double) tempScale);
        } else if (regAddr == 101) {
            emulator.setTimeSetRaw(value);
        } else if (regAddr == 102) {
            emulator.setTempRatePerMin(value / (double) tempScale);
        } else if (regAddr == 103) {
            emulator.setHumiRatePerMin(value / (double) tempScale);
        } else if (regAddr == 62) {
            emulator.setProgramNumber(value);
        } else if (regAddr == 70) {
            emulator.setSegTotal(value);
        } else if (regAddr == 71) {
            emulator.setSegCurrent(value);
        } else if (regAddr == 72) {
            emulator.setCycleCurrent(value);
        } else if (regAddr == 73) {
            emulator.setCycleTotal(value);
        } else if (regAddr >= 74 && regAddr <= 76) {
            int[] t = emulator.getRmnTime();
            if (regAddr == 74) emulator.setRmnTime(value, t[1], t[2]);
            else if (regAddr == 75) emulator.setRmnTime(t[0], value, t[2]);
            else emulator.setRmnTime(t[0], t[1], value);
        } else if (regAddr == 111) {
            emulator.setProgWaitTempC(value / (double) tempScale);
        } else if (regAddr == 112) {
            emulator.setProgWaitHumPct(value / (double) tempScale);
        }
    }

    private byte[] buildExceptionResponse(int addr, int func, int code) {
        byte[] data = new byte[3];
        data[0] = (byte) addr;
        data[1] = (byte) (func | 0x80);
        data[2] = (byte) code;
        return BotoModbusUtil.appendCrc(data);
    }
}
