package org.example.device.ethernet.binder;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Протокольная сессия Binder поверх установленного {@link Socket} (TCP, порт 10001).
 *
 * <p>Выполняет отдельные операции Modbus-кадра с проверкой CRC (см.
 * {@link BinderCommandRegistry}). Методы синхронные и бросают {@link IOException}
 * при сетевой ошибке / коротком ответе / неверной CRC — повтор/политику повторных
 * попыток и влажностную авто-логику реализует вызывающий слой
 * ({@code BinderCommunicationService} и далее — глобальный протокол {@code SomeDevice}).
 */
public class BinderModbus {

    private final Socket socket;
    private final int slaveId;
    private final InputStream in;
    private final OutputStream out;

    public BinderModbus(Socket socket, int slaveId) throws IOException {
        this.socket = socket;
        this.slaveId = slaveId;
        this.in = socket.getInputStream();
        this.out = socket.getOutputStream();
    }

    public int getSlaveId() {
        return slaveId;
    }

    /** SetT: задать уставку температуры, °C. Ожидает эхо-подтверждение 0x10. */
    public void setTemperature(float temperature) throws IOException {
        byte[] frame = BinderCommandRegistry.buildSetTemperatureFrame(slaveId, temperature);
        out.write(frame);
        out.flush();
        byte[] reply = readFully(8);
        if (!BinderCommandRegistry.isSetTemperatureAck(reply, slaveId)) {
            throw new IOException("SetT: bad ack");
        }
    }

    /** GetT: прочитать текущую температуру, °C. */
    public float readTemperature() throws IOException {
        byte[] frame = BinderCommandRegistry.buildGetTemperatureFrame(slaveId);
        out.write(frame);
        out.flush();
        byte[] reply = readFully(BinderCommandRegistry.getTemperatureReplyLength());
        Float t = BinderCommandRegistry.parseGetTemperature(reply, slaveId);
        if (t == null) {
            throw new IOException("GetT: bad reply");
        }
        return t;
    }

    /** SetHC: включить/выключить управление влажностью. Ожидает эхо 0x06. */
    public void setHumidity(boolean on) throws IOException {
        byte[] frame = BinderCommandRegistry.buildSetHumidityFrame(slaveId, on);
        out.write(frame);
        out.flush();
        byte[] reply = readFully(8);
        if (!BinderCommandRegistry.isSetHumidityAck(reply, slaveId)) {
            throw new IOException("SetHC: bad ack");
        }
    }

    private byte[] readFully(int n) throws IOException {
        byte[] buf = new byte[n];
        int off = 0;
        while (off < n) {
            int r = in.read(buf, off, n - off);
            if (r < 0) {
                throw new EOFException("Unexpected EOF, wanted " + n + " got " + off);
            }
            off += r;
        }
        return buf;
    }
}
