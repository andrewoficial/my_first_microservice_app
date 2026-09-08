package org.example.gui.devices.boto120.emulation;

import com.fazecast.jSerialComm.SerialPort;
import lombok.extern.slf4j.Slf4j;
import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.devices.emulation.EmulatorCommandLog;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;

/**
 * Мини-эмулятор BOTO-120 (China Modbus, Modbus RTU 9600 8N1).
 * Подтверждённые регистры: 12 = текущая температура ×100 (PV дрейфует к уставке),
 * 100 = уставка ×100, 105 = вкл/выкл (1/0). Остальные адреса читаются нулями.
 * Все запросы логируются.
 */
@Slf4j
public class Boto120EmulationPanel extends JPanel {

    private static final String PREFS_NODE = "org/example/gui/devices/boto120/emulation";
    private static final String PREFS_KEY_PORT = "lastEmuPort";

    private static final int BAUD = 9600;
    private static final int READ_TIMEOUT_MS = 50;
    private static final long FRAME_TIMEOUT_MS = 120;

    private SerialPort port;
    private volatile boolean running = false;
    private Thread readerThread;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    private final AtomicLong lastAdvance = new AtomicLong(System.currentTimeMillis());
    private volatile int pv = 1605;        // текущая температура ×100 (≈16.05 °C)
    private volatile int setpoint = 2500;  // уставка ×100 (25.00 °C)
    private volatile int power = 0;        // 1 = вкл, 0 = выкл

    private final JComboBox<String> portCombo = new JComboBox<>();
    private final JLabel statusLabel = new JLabel("Остановлено");
    private final JTextArea logArea = new JTextArea();
    private final EmulatorCommandLog commandLog = new EmulatorCommandLog("BOTO 120");

    private final Preferences prefs = Preferences.userRoot().node(PREFS_NODE);

    public Boto120EmulationPanel() {
        super(new BorderLayout(8, 8));
        setBorder(new TitledBorder("BOTO-120 — мини-эмулятор (China Modbus: 12/100/105)"));
        buildUi();
        refreshPorts();
        String last = prefs.get(PREFS_KEY_PORT, "");
        if (!last.isEmpty()) portCombo.setSelectedItem(last);
    }

    private void buildUi() {
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));

        JPanel portRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        portRow.add(new JLabel("COM:"));
        portCombo.setPreferredSize(new Dimension(160, 26));
        portRow.add(portCombo);
        JButton refreshBtn = new JButton("Обновить");
        refreshBtn.addActionListener(e -> refreshPorts());
        portRow.add(refreshBtn);
        JButton openBtn = new JButton("Открыть");
        openBtn.addActionListener(e -> openPort());
        portRow.add(openBtn);
        JButton closeBtn = new JButton("Закрыть");
        closeBtn.addActionListener(e -> closePort());
        portRow.add(closeBtn);
        top.add(portRow);

        JPanel cmdRow = EmulatorCommandLog.createControls(commandLog);
        cmdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        top.add(cmdRow);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(logArea);

        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void refreshPorts() {
        String prev = (String) portCombo.getSelectedItem();
        portCombo.removeAllItems();
        SerialPort[] ports = SerialPort.getCommPorts();
        for (SerialPort p : ports) portCombo.addItem(p.getSystemPortName());
        if (portCombo.getItemCount() == 0) portCombo.addItem("— нет COM-портов —");
        else if (prev != null) portCombo.setSelectedItem(prev);
    }

    private void openPort() {
        String portName = (String) portCombo.getSelectedItem();
        if (portName == null || portName.startsWith("—")) return;
        if (port != null && port.isOpen()) closePort();
        port = SerialPort.getCommPort(portName);
        port.setBaudRate(BAUD);
        port.setNumDataBits(8);
        port.setNumStopBits(SerialPort.ONE_STOP_BIT);
        port.setParity(SerialPort.NO_PARITY);
        port.setFlowControl(SerialPort.FLOW_CONTROL_DISABLED);
        port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, READ_TIMEOUT_MS, 0);
        if (!port.openPort()) {
            appendLog("Не удалось открыть " + portName);
            port = null;
            return;
        }
        prefs.put(PREFS_KEY_PORT, portName);
        running = true;
        startReader();
        statusLabel.setText("Эмуляция: " + portName + " @ 9600 8N1");
        appendLog("Порт открыт: " + portName + " @ 9600 8N1");
    }

    public boolean isRunning() { return running && port != null && port.isOpen(); }

    private void closePort() {
        running = false;
        readerThread = null;
        if (port != null && port.isOpen()) port.closePort();
        port = null;
        synchronized (buffer) { buffer.reset(); }
        statusLabel.setText("Остановлено");
    }

    public void shutdown() { closePort(); }

    private void startReader() {
        readerThread = new Thread(() -> {
            byte[] single = new byte[1];
            long lastByteAt = 0;
            boolean collecting = false;
            while (running && port != null && port.isOpen()) {
                try {
                    int r = port.readBytes(single, 1);
                    long now = System.currentTimeMillis();
                    if (r > 0) {
                        if (!collecting) {
                            collecting = true;
                            synchronized (buffer) { buffer.reset(); }
                        }
                        synchronized (buffer) { buffer.write(single[0]); }
                        lastByteAt = now;
                    } else if (collecting && (now - lastByteAt > FRAME_TIMEOUT_MS)) {
                        processBuffer();
                        collecting = false;
                    }
                } catch (Exception e) {
                    if (running) log.error("Boto120Emu read error", e);
                }
            }
        }, "Boto120Emu-Reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void processBuffer() {
        byte[] data;
        synchronized (buffer) {
            data = buffer.toByteArray();
        }
        if (data.length < 1) return;

        int frameLen = BotoModbusUtil.expectedRequestFrameLength(data);
        if (frameLen > 0 && data.length < frameLen) return;   // ждём остаток кадра, буфер не чистим

        synchronized (buffer) { buffer.reset(); }

        if (frameLen <= 0) {
            // Нераспознанный поток (возможно, не Modbus) — не теряем его, показываем как есть.
            if (data.length > 64) data = Arrays.copyOf(data, 64);
            appendLog("RX << " + BotoModbusUtil.bytesToHex(data) + "   // не распознан как Modbus-кадр, ответа нет");
            return;
        }

        byte[] frame = Arrays.copyOf(data, frameLen);

        appendLog("RX << " + BotoModbusUtil.bytesToHex(frame));
        if (!BotoModbusUtil.validateCrc(frame)) {
            appendLog("     CRC ошибка, ответ не отправлен");
            return;
        }
        byte[] response = buildResponse(frame);
        if (response != null && response.length > 0) {
            port.writeBytes(response, response.length);
            appendLog("TX >> " + BotoModbusUtil.bytesToHex(response));
        } else {
            appendLog("TX >> (нет ответа)");
        }
    }

    /**
     * Ответ China Modbus: чтение → PV/уставка/состояние (остальное нули),
     * запись 0x06 → подтверждение эхом + обновление модели.
     */
    private byte[] buildResponse(byte[] req) {
        int slave = req[0] & 0xFF;
        int func = req[1] & 0xFF;
        if (func == 0x03 || func == 0x04) {
            commandLog.dataRequest(BotoModbusUtil.bytesToHex(req));
            int start = ((req[2] & 0xFF) << 8) | (req[3] & 0xFF);
            int quantity = ((req[4] & 0xFF) << 8) | (req[5] & 0xFF);
            if (quantity > 125) quantity = 125;
            advanceTick();
            int[] values = new int[quantity];
            for (int i = 0; i < quantity; i++) {
                int reg = start + i;
                if (reg == 12) values[i] = pv;
                else if (reg == 100) values[i] = setpoint;
                else if (reg == 105) values[i] = power;
            }
            int byteCount = quantity * 2;
            ByteBuffer buf = ByteBuffer.allocate(3 + byteCount);
            buf.put((byte) slave).put((byte) func).put((byte) byteCount);
            for (int value : values) buf.putShort((short) value);
            return BotoModbusUtil.appendCrc(buf.array());
        }
        if (func == 0x06) {
            int reg = ((req[2] & 0xFF) << 8) | (req[3] & 0xFF);
            int value = ((req[4] & 0xFF) << 8) | (req[5] & 0xFF);
            if (reg == 100) {
                setpoint = value;
                appendLog(String.format("     уставка → %.2f °C", value / 100.0));
                commandLog.command("установка температуры: " + String.format("%.2f", value / 100.0) + " °C");
            } else if (reg == 105) {
                power = value;
                appendLog("     камера → " + (value == 1 ? "ВКЛ" : "ВЫКЛ"));
                commandLog.command(value == 1 ? "старт" : "стоп");
            }
            return req;   // эхо запроса — стандартное подтверждение 0x06
        }
        if (func == 0x10) {
            ByteBuffer buf = ByteBuffer.allocate(6);
            buf.put((byte) slave).put((byte) 0x10);
            buf.put(req, 2, 4);   // адрес + количество
            return BotoModbusUtil.appendCrc(buf.array());
        }
        return BotoModbusUtil.appendCrc(new byte[]{(byte) slave, (byte) (func | 0x80), 0x01});
    }

    /** Движение температуры: при «вкл» ползёт к уставке, при «выкл» — к комнатной (~16 °C). */
    private void advanceTick() {
        long now = System.currentTimeMillis();
        long dtMs = now - lastAdvance.get();
        if (dtMs <= 0 || dtMs > 5000) {
            lastAdvance.set(now);
            return;
        }
        lastAdvance.set(now);
        double steps = dtMs / 1000.0;
        int target = power == 1 ? setpoint : 1600;
        if (pv < target) pv = (int) Math.min(target, Math.round(pv + 5 * steps));
        else if (pv > target) pv = (int) Math.max(target, Math.round(pv - 3 * steps));
    }

    private void appendLog(String line) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(line + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
}