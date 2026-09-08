package org.example.gui.devices.tt5166.emulation;

import lombok.extern.slf4j.Slf4j;
import org.example.gui.devices.emulation.EmulatorCommandLog;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Modbus RTU-отклик термокамеры TT5166 (38400 8E1).
 * <p>
 * Функции: 0x03 (чтение holding), 0x05 (force coil — старт/стоп), 0x06 (запись регистра).
 * Карта регистров соответствует {@code TT5166CommandRegistry}:
 * 0x0000 tempPV ×100, 0x0001 tempSV ×10, 0x0002 tempOut ×10,
 * 0x0003 humPV ×10, 0x0004 humSV ×10, 0x0005 humOut ×10,
 * 0x0006/0x0008 — время программы (ч/м), 0x0018 состояние + 0x0019 режим,
 * 0x001B код ошибки, 0x0024..0x0026 темп. доп. каналов ×10,
 * 0x0026/0x0027 уставки темп./влаги (запись), 0x0064/0x0065 градиенты ×10.
 */
@Slf4j
public class TT5166ModbusResponder {

    private final TT5166Emulator emulator;
    private final int slaveId;

    /** Ручные значения по адресам регистров (адрес → значение). */
    private final ConcurrentHashMap<Integer, Integer> manualRegisters = new ConcurrentHashMap<>();

    private volatile boolean addressMapping = false;

    private volatile EmulatorCommandLog commandLog;

    public TT5166ModbusResponder(TT5166Emulator emulator, int slaveId) {
        this.emulator = emulator;
        this.slaveId = slaveId;
    }

    /** Подключить лог команд виртуальной камеры (опционально). */
    public void setCommandLog(EmulatorCommandLog log) { this.commandLog = log; }

    public void setAddressMapping(boolean addressMapping) { this.addressMapping = addressMapping; }
    public boolean isAddressMapping() { return addressMapping; }

    public void setManualRegister(int addr, int value) { manualRegisters.put(addr, value); }
    public void removeManualRegister(int addr) { manualRegisters.remove(addr); }
    public java.util.Map<Integer, Integer> manualRegisters() { return new java.util.HashMap<>(manualRegisters); }

    public synchronized byte[] processRequest(byte[] request) {
        if (request == null || request.length < 4) {
            log.warn("TT5166Emu: слишком короткий кадр ({} байт): {}",
                    request == null ? 0 : request.length, TT5166ModbusUtil.bytesToHex(request));
            return null;
        }
        if (!TT5166ModbusUtil.validateCrc(request)) {
            log.warn("TT5166Emu: CRC ошибка: {}", TT5166ModbusUtil.bytesToHex(request));
            return null;
        }
        int addr = request[0] & 0xFF;
        if (addr != slaveId && addr != 0) return null;

        int function = request[1] & 0xFF;
        switch (function) {
            case 0x03: return handleRead(request);
            case 0x05: return handleWriteCoil(request);
            case 0x06: return handleWriteSingle(request);
            default:
                log.warn("TT5166Emu: неподдерживаемая функция 0x{}", String.format("%02X", function));
                return buildExceptionResponse(addr, function, 0x01);
        }
    }

    private byte[] handleRead(byte[] request) {
        int startReg = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int quantity = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
        if (quantity < 1 || quantity > 125) {
            return buildExceptionResponse(request[0], request[1], 0x03);
        }
        if (commandLog != null) commandLog.dataRequest(TT5166ModbusUtil.bytesToHex(request));

        ByteBuffer resp = ByteBuffer.allocate(3 + 2 * quantity);
        resp.order(ByteOrder.BIG_ENDIAN);
        resp.put((byte) slaveId);
        resp.put((byte) 0x03);
        resp.put((byte) (2 * quantity));

        for (int i = 0; i < quantity; i++) {
            resp.putShort((short) readRegister(startReg + i));
        }
        return TT5166ModbusUtil.appendCrc(resp.array());
    }

    private byte[] handleWriteCoil(byte[] request) {
        int coilAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int value = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);
        boolean onCoil = value == 0xFF00;

        if (coilAddr == 0x0000) {
            emulator.setOn(onCoil);
            log.info("TT5166Emu: старт → {}", onCoil ? "ВКЛ" : "ВЫКЛ");
            if (commandLog != null) commandLog.command(onCoil ? "старт" : "стоп");
        } else if (coilAddr == 0x0001) {
            emulator.setOn(!onCoil);
            log.info("TT5166Emu: стоп → {}", !onCoil ? "ВЫКЛ" : "ВКЛ");
            if (commandLog != null) commandLog.command(!onCoil ? "стоп" : "старт");
        } else {
            log.warn("TT5166Emu: запись неиспользуемой катушки {}", String.format("0x%04X", coilAddr));
        }

        ByteBuffer resp = ByteBuffer.allocate(6);
        resp.order(ByteOrder.BIG_ENDIAN);
        resp.put((byte) slaveId);
        resp.put((byte) 0x05);
        resp.putShort((short) coilAddr);
        resp.putShort((short) value);
        return TT5166ModbusUtil.appendCrc(resp.array());
    }

    private byte[] handleWriteSingle(byte[] request) {
        int regAddr = ((request[2] & 0xFF) << 8) | (request[3] & 0xFF);
        int value = ((request[4] & 0xFF) << 8) | (request[5] & 0xFF);

        applyWrite(regAddr, value);

        ByteBuffer resp = ByteBuffer.allocate(6);
        resp.order(ByteOrder.BIG_ENDIAN);
        resp.put((byte) slaveId);
        resp.put((byte) 0x06);
        resp.putShort((short) regAddr);
        resp.putShort((short) value);
        return TT5166ModbusUtil.appendCrc(resp.array());
    }

    private int readRegister(int reg) {
        Integer manual = manualRegisters.get(reg);
        if (manual != null) {
            return manual & 0xFFFF;
        }
        switch (reg) {
            case 0x0000: return (int) Math.round(emulator.getCurrentTempC() * 100);
            case 0x0001: return (int) Math.round(emulator.getSetpointC() * 10);
            case 0x0002: return (int) Math.round(emulator.getTempOutputPct() * 10);
            case 0x0003: return (int) Math.round(emulator.getCurrentHumidity() * 10);
            case 0x0004: return (int) Math.round(emulator.getHumiditySetpoint() * 10);
            case 0x0005: return (int) Math.round(emulator.getHumOutputPct() * 10);
            case 0x0006: return emulator.getProgramHours();
            case 0x0008: return emulator.getProgramMinutes();
            case 0x0018: return emulator.isOn() ? 1 : 0;
            case 0x0019: return emulator.isProgramMode() ? 1 : 0;
            case 0x001B: return emulator.getFaultCode();
            case 0x0024: return (int) Math.round(emulator.getChannelTempC(2) * 10);
            case 0x0025: return (int) Math.round(emulator.getChannelTempC(3) * 10);
            case 0x0026: return (int) Math.round(emulator.getChannelTempC(4) * 10);
            case 0x0064: return (int) Math.round(emulator.getTempGradientCPer10Min() * 10);
            case 0x0065: return (int) Math.round(emulator.getHumGradientPctPer10Min() * 10);
            default: return addressMapping ? reg : 0;
        }
    }

    private void applyWrite(int regAddr, int value) {
        switch (regAddr) {
            case 0x0026:
                emulator.setSetpointC(value / 10.0);
                if (commandLog != null) commandLog.command("установка температуры: "
                        + String.format("%.1f", value / 10.0) + " °C");
                break;
            case 0x0027:
                emulator.setHumiditySetpoint(value / 10.0);
                if (commandLog != null) commandLog.command("установка влажности: "
                        + String.format("%.1f", value / 10.0) + "%");
                break;
            case 0x0064: emulator.setTempGradientCPer10Min(value / 10.0); break;
            case 0x0065: emulator.setHumGradientPctPer10Min(value / 10.0); break;
            default:
                log.info("TT5166Emu: запись регистра {} = {}", String.format("0x%04X", regAddr), value);
        }
    }

    private byte[] buildExceptionResponse(int addr, int func, int code) {
        return TT5166ModbusUtil.appendCrc(new byte[]{(byte) addr, (byte) (func | 0x80), (byte) code});
    }
}