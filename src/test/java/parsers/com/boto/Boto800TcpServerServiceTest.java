package parsers.com.boto;

import org.example.device.protBoto.BotoModbusUtil;
import org.example.gui.devices.boto800.tcp.emulation.Boto800TcpEmulator;
import org.example.gui.devices.boto800.tcp.emulation.Boto800TcpServerService;
import org.example.gui.devices.emulation.EmulatorTrace;
import org.example.gui.devices.emulation.ModbusFrameGuesser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("BOTO-800 TCP/IP — Modbus TCP сниффер")
class Boto800TcpServerServiceTest {

    private static final int READ_TIMEOUT_MS = 2000;

    private Boto800TcpEmulator emulator;
    private Boto800TcpServerService server;
    private Socket client;

    @BeforeEach
    void setUp() throws IOException {
        emulator = new Boto800TcpEmulator();
        emulator.setRange(-40.0, 400.0);
        server = new Boto800TcpServerService(emulator);
        // тесты не должны сыпать мусор в консоль и в logs/
        server.trace().setTerminalEcho(false).setFileEnabled(false);
        assertThat(server.start(0)).isTrue();
        client = new Socket();
        client.connect(new InetSocketAddress("127.0.0.1", server.boundPort()), READ_TIMEOUT_MS);
        client.setSoTimeout(READ_TIMEOUT_MS);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (client != null) {
            client.close();
        }
        server.stop();
        server.trace().close();
    }

    @Test
    @DisplayName("Чтение блока 10..49 возвращает 40 регистров с корректным byteCount")
    void readsRealtimeBlock() throws IOException {
        emulator.setSetpoint(25.0);
        byte[] reply = exchange(BotoModbusUtil.buildReadHoldingRequest(1, 10, 40));

        // slave + fn + byteCount + 80 байт данных + CRC
        assertThat(reply).hasSize(85);
        assertThat(reply[1] & 0xFF).isEqualTo(0x03);
        assertThat(reply[2] & 0xFF).isEqualTo(80);
        assertThat(BotoModbusUtil.validateCrc(reply)).isTrue();
        assertThat(reg(reply, 0)).isEqualTo(250);   // рег 10 = PV ×10
        assertThat(reg(reply, 1)).isEqualTo(250);   // рег 11 = зеркало уставки ×10
    }

    @Test
    @DisplayName("Modbus/TCP: transactionId и unitId эхо-ятся, длина MBAP корректна")
    void answersModbusTcp() throws IOException {
        byte[] pdu = BotoModbusUtil.buildReadHoldingRequest(1, 10, 1);
        byte[] reply = exchange(mbap(0x1234, 1, pdu));

        // ответ: MBAP(6) + unitId(1) + PDU(func + byteCount + 2 байта данных = 4) = 11
        assertThat(reply.length).isEqualTo(11);
        assertThat(reply[0] & 0xFF).isEqualTo(0x12);
        assertThat(reply[1] & 0xFF).isEqualTo(0x34);
        assertThat(reply[2] & 0xFF).isEqualTo(0x00);
        assertThat(reply[3] & 0xFF).isEqualTo(0x00);
        assertThat(((reply[4] & 0xFF) << 8) | (reply[5] & 0xFF)).isEqualTo(1 + 4);  // unitId + PDU
        assertThat(reply[6] & 0xFF).isEqualTo(1);                                  // unitId
        assertThat(reply[7] & 0xFF).isEqualTo(0x03);
        assertThat(reply[8] & 0xFF).isEqualTo(0x02);
    }

    @Test
    @DisplayName("Незнакомая функция: ответ-исключение, соединение НЕ рвётся")
    void unknownFunctionKeepsConnectionAlive() throws IOException {
        byte[] unknown = BotoModbusUtil.appendCrc(new byte[]{0x01, 0x1F, 0x00, 0x00});
        byte[] reply = exchange(unknown);

        assertThat(reply).hasSize(5);                     // slave + fn|0x80 + code + CRC
        assertThat(reply[0]).isEqualTo((byte) 0x01);
        assertThat(reply[1] & 0xFF).isEqualTo(0x9F);      // 0x1F | 0x80
        assertThat(reply[2] & 0xFF).isEqualTo(0x01);      // IllegalFunction
        assertThat(BotoModbusUtil.validateCrc(reply)).isTrue();

        // соединение живо: следующий запрос обрабатывается штатно
        byte[] next = exchange(BotoModbusUtil.buildReadHoldingRequest(1, 10, 1));
        assertThat(next[1] & 0xFF).isEqualTo(0x03);
    }

    @Test
    @DisplayName("Запись рег 63 включает камеру, 60 меняет уставку, новый адрес — в зеркало")
    void writesUpdateModel() throws IOException {
        exchange(BotoModbusUtil.buildWriteSingleRequest(1, 63, 1));
        assertThat(emulator.isPowerOn()).isTrue();

        exchange(BotoModbusUtil.buildWriteSingleRequest(1, 60, 440));
        assertThat(emulator.getSetpoint()).isEqualTo(44.0);

        exchange(BotoModbusUtil.buildWriteSingleRequest(1, 7777, 5));
        assertThat(emulator.writtenRegisters()).containsEntry(7777, 5);
    }

    @Test
    @DisplayName("Запись 0x10 пары 60+61 меняет обе уставки (штатный сценарий программы)")
    void writesSetpointPairViaFunction16() throws IOException {
        byte[] body = new byte[]{0x01, 0x10, 0x00, 0x3C, 0x00, 0x02, 0x04,
                0x01, (byte) 0xF4, 0x02, 0x58};   // 500 = 50.0 °C, 600 = 60.0 %
        byte[] ack = exchange(BotoModbusUtil.appendCrc(body));

        assertThat(ack[1] & 0xFF).isEqualTo(0x10);
        assertThat(((ack[2] & 0xFF) << 8) | (ack[3] & 0xFF)).isEqualTo(0x3C);   // echo startReg
        assertThat(((ack[4] & 0xFF) << 8) | (ack[5] & 0xFF)).isEqualTo(0x02);   // echo quantity
        assertThat(BotoModbusUtil.validateCrc(ack)).isTrue();
        assertThat(emulator.getSetpoint()).isEqualTo(50.0);
        assertThat(emulator.getHumiditySetpoint()).isEqualTo(60.0);
    }

    @Test
    @DisplayName("Служебные блоки 8108/8900/8964 отдают нули, слово 8900 настраивается")
    void serviceBlocksAreQuiet() throws IOException {
        emulator.setStatusWord(0x0200);
        assertThat(emulator.readRegister(0x1FAC)).isZero();
        assertThat(emulator.readRegister(0x22C4)).isEqualTo(0x0200);
        assertThat(emulator.readRegister(0x22C4 + 10)).isZero();
        assertThat(emulator.readRegister(0x2304)).isZero();
        assertThat(emulator.readRegister(0x157E)).isZero();

        byte[] reply = exchange(BotoModbusUtil.buildReadHoldingRequest(1, 0x22C4, 3));
        assertThat(reg(reply, 0)).isEqualTo(0x0200);
        assertThat(reg(reply, 1)).isZero();
        assertThat(reg(reply, 2)).isZero();
    }

    @Test
    @DisplayName("Пределы 39/41 отдаются реальные — иначе штатная программа не даст ввести уставку")
    void limitsAreSane() throws IOException {
        assertThat(emulator.readRegister(39)).isEqualTo(900);   // 90.0 °C
        assertThat(emulator.readRegister(41)).isEqualTo(980);   // 98.0 %

        byte[] reply = exchange(BotoModbusUtil.buildReadHoldingRequest(1, 38, 4));
        assertThat(reg(reply, 0)).isEqualTo((-400) & 0xFFFF);  // -40.0 °C, знаковый
        assertThat(reg(reply, 1)).isEqualTo(900);
        assertThat(reg(reply, 2)).isZero();                    // 0.0 %
        assertThat(reg(reply, 3)).isEqualTo(980);
    }

    @Test
    @DisplayName("Чтение катушек (0x01) отвечает битовой картой")
    void readsCoils() throws IOException {
        emulator.setPowerOn(true);
        byte[] coilReq = BotoModbusUtil.appendCrc(new byte[]{0x01, 0x01, 0x00, 0x00, 0x00, 0x08});
        byte[] reply = exchange(coilReq);

        assertThat(reply[0]).isEqualTo((byte) 0x01);
        assertThat(reply[1] & 0xFF).isEqualTo(0x01);     // функция
        assertThat(reply[2] & 0xFF).isEqualTo(1);        // 8 бит = 1 байт
        assertThat(reply[3] & 0xFF).isNotEqualTo(0);     // бит 0 включён
        assertThat(BotoModbusUtil.validateCrc(reply)).isTrue();
    }

    @Test
    @DisplayName("Режим CORE: функции вне 0x01..0x10 не получают ответа")
    void coreModeIsSilentOutsideCore() throws IOException {
        server.setAnswerMode(Boto800TcpServerService.AnswerMode.CORE);
        InputStream in = client.getInputStream();

        // 0x11 (Report Slave ID) — вне ядра: тишина
        client.getOutputStream().write(BotoModbusUtil.appendCrc(new byte[]{0x01, 0x11, 0x00}));
        client.getOutputStream().flush();
        assertThat(in.available()).isZero();

        // 0x03 — внутри ядра: обычный ответ
        byte[] reply = exchange(BotoModbusUtil.buildReadHoldingRequest(1, 10, 1));
        assertThat(reply[1] & 0xFF).isEqualTo(0x03);
    }

    @Test
    @DisplayName("Граница кадра находится и для нестандартного кода функции (по CRC)")
    void detectsFrameByCrcForUnknownFunction() {
        byte[] frame = BotoModbusUtil.appendCrc(new byte[]{0x01, 0x2A, 0x11, 0x22, 0x33, 0x44});
        assertThat(ModbusFrameGuesser.frameLength(frame, 0, frame.length)).isEqualTo(frame.length);

        byte[] two = new byte[frame.length * 2];
        System.arraycopy(frame, 0, two, 0, frame.length);
        System.arraycopy(frame, 0, two, frame.length, frame.length);
        assertThat(ModbusFrameGuesser.frameLength(two, 0, two.length)).isEqualTo(frame.length);
        assertThat(ModbusFrameGuesser.frameLength(two, frame.length, frame.length)).isEqualTo(frame.length);
    }

    @Test
    @DisplayName("Обрезанный кадр не «съедается»: длина больше доступных данных")
    void waitsForIncompleteFrame() {
        byte[] frame = BotoModbusUtil.buildReadHoldingRequest(1, 10, 1);
        byte[] half = Arrays.copyOf(frame, frame.length - 1);
        assertThat(ModbusFrameGuesser.frameLength(half, 0, half.length)).isGreaterThan(half.length);
    }

    @Test
    @DisplayName("MBAP-кадр опознаётся по заголовку, даже если код функции неизвестен")
    void detectsMbapHeader() {
        byte[] adu = mbapFromPdu(0x0001, 1, new byte[]{0x1F, 0x00});
        assertThat(ModbusFrameGuesser.isMbap(adu)).isTrue();
        assertThat(ModbusFrameGuesser.frameLength(adu, 0, adu.length)).isEqualTo(adu.length);
    }

    @Test
    @DisplayName("Трассировка пишет в слушателей и ведёт счётчики")
    void traceFeedsListeners() {
        EmulatorTrace trace = new EmulatorTrace("test");
        StringBuilder sink = new StringBuilder();
        trace.setTerminalEcho(false).setLogEnabled(false).setFileEnabled(false);
        trace.addListener(sink::append);
        trace.rx(new byte[]{0x01, 0x03, 0x00, 0x0A, 0x00, 0x01, 0x44, 0x09}, 0, 8, "#1 RTU");
        trace.tx(new byte[]{0x01, 0x03, 0x02, 0x06, 0x46, 0x01, 0x02, 0x03});

        assertThat(trace.rxBytes()).isEqualTo(8);
        assertThat(trace.txBytes()).isEqualTo(8);
        assertThat(trace.rxPackets()).isEqualTo(1);
        assertThat(trace.txPackets()).isEqualTo(1);
        assertThat(sink.toString()).contains("01 03 00 0A 00 01 44 09");
        trace.close();
    }

    // ─── официальный документ «680通信协议V1.0» ───────────────────────────

    @Test
    @DisplayName("D0012/D0016 — температура и влажность, ×10")
    void setpointsAreReportedInTenths() {
        emulator.setSetpoint(25.0);
        emulator.setHumiditySetpoint(60.0);
        assertThat(emulator.readRegister(10)).isEqualTo(250);   // 温度测试值 = PV
        assertThat(emulator.readRegister(11)).isEqualTo(250);   // 温度设定值 NSP
        assertThat(emulator.readRegister(12)).isEqualTo(250);   // 温度目标设定值 TSP
        assertThat(emulator.readRegister(14)).isEqualTo(500);   // 湿度测试值 = PV, не уставка
        assertThat(emulator.readRegister(15)).isEqualTo(600);   // 湿度设定值 NSP
        assertThat(emulator.readRegister(16)).isEqualTo(600);   // 湿度目标设定值 TSP
    }

    @Test
    @DisplayName("D0018..D0020 — 湿球温度 (влажножарная температура), а не «признак влаги»")
    void wetBulbRegistersAreTemperatures() {
        assertThat(emulator.readRegister(18)).isEqualTo(200);   // ×10 → 20.0 °C
        emulator.setWetBulb(12.5, 13.5, 14.5);
        assertThat(emulator.readRegister(18)).isEqualTo(125);
        assertThat(emulator.readRegister(19)).isEqualTo(135);
        assertThat(emulator.readRegister(20)).isEqualTo(145);
    }

    @Test
    @DisplayName("D0043 — 0 = 温湿度 (T+RH), 1 = 单温度 (только T)")
    void singleTemperatureFlagIsInvertedHumidityFlag() {
        assertThat(emulator.readRegister(43)).isZero();         // по умолчанию T+RH
        emulator.setHumidityEnabled(false);
        assertThat(emulator.readRegister(43)).isEqualTo(1);
        emulator.setHumidityEnabled(true);
        assertThat(emulator.readRegister(43)).isZero();
    }

    @Test
    @DisplayName("D0042/D0045 — DI-состояние: бит 0 = DI1, бит 0 = DI17")
    void diStateIsBitField() {
        assertThat(emulator.readRegister(42)).isZero();
        assertThat(emulator.readRegister(45)).isZero();
        emulator.setDiState(0xE800, 0x0005);
        assertThat(emulator.readRegister(42)).isEqualTo(0xE800);   // DI13, DI15, DI16
        assertThat(emulator.readRegister(45)).isEqualTo(0x0005);   // DI19, DI21
    }

    @Test
    @DisplayName("D0030 — 运行模式: 0 = 程式 (программа), 1 = 定值 (фикс. значение)")
    void operatingModeIsProgramOrFixed() {
        assertThat(emulator.readRegister(30)).isEqualTo(1);    // по умолчанию фикс. режим
        emulator.writeRegister(30, 0);
        assertThat(emulator.readRegister(30)).isZero();
    }

    @Test
    @DisplayName("D0077 — 保持状态 On Hold, 0/1, R/W")
    void onHoldIsReadableFlag() {
        assertThat(emulator.readRegister(77)).isZero();
        emulator.writeRegister(77, 1);
        assertThat(emulator.readRegister(77)).isEqualTo(1);
        emulator.writeRegister(77, 0);
        assertThat(emulator.readRegister(77)).isZero();
    }

    @Test
    @DisplayName("D0250+ — сегменты программы, шаг 5 (T, RH, время, TS1, TS2)")
    void segmentsAreFiveRegistersWithStride() {
        emulator.setSetpoint(25.0);
        assertThat(emulator.readRegister(250)).isEqualTo(260);   // сегмент 1: 25 + 1 °C
        assertThat(emulator.readRegister(255)).isEqualTo(270);   // сегмент 2: 25 + 2 °C
        assertThat(emulator.readRegister(251)).isZero();         // влажность
        assertThat(emulator.readRegister(252)).isZero();         // время
        assertThat(emulator.readRegister(253)).isZero();         // TS1
        assertThat(emulator.readRegister(254)).isZero();         // TS2
    }

    @Test
    @DisplayName("D1000 — 设置程式号: запись номера программы перед сегментами")
    void programSelectorIsWrittenFirst() throws IOException {
        emulator.writeRegister(1000, 4);
        assertThat(emulator.readRegister(1000)).isEqualTo(4);
        assertThat(emulator.writtenRegisters()).containsEntry(1000, 4);
    }

    @Test
    @DisplayName("D0050..D0059 — каналы 3..12 отключены: −300 минус номер канала")
    void unusedChannelsReportDisconnected() {
        assertThat(emulator.readRegister(50)).isEqualTo((-300) & 0xFFFF);
        assertThat(emulator.readRegister(51)).isEqualTo((-301) & 0xFFFF);
        assertThat(emulator.readRegister(59)).isEqualTo((-309) & 0xFFFF);
        assertThat(emulator.isKnownRegister(50)).isTrue();
    }

    // ─── helpers ──────────────────────────────────────────────────────────

    /** Значение i-го регистра из ответа 0x03. */
    private static int reg(byte[] reply, int i) {
        int off = 3 + 2 * i;
        return ((reply[off] & 0xFF) << 8) | (reply[off + 1] & 0xFF);
    }

    private byte[] exchange(byte[] request) throws IOException {
        OutputStream out = client.getOutputStream();
        out.write(request);
        out.flush();
        return readAdu(client.getInputStream());
    }

    /** Читает ровно один ADU (MBAP или RTU) из потока. */
    private static byte[] readAdu(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] chunk = new byte[256];
        long deadline = System.currentTimeMillis() + READ_TIMEOUT_MS;
        while (System.currentTimeMillis() < deadline) {
            if (in.available() > 0) {
                int r = in.read(chunk);
                if (r < 0) {
                    break;
                }
                buf.write(chunk, 0, r);
                if (isComplete(buf.toByteArray())) {
                    break;
                }
            } else {
                Thread.onSpinWait();
            }
        }
        return buf.toByteArray();
    }

    private static boolean isComplete(byte[] d) {
        int mbap = ModbusFrameGuesser.mbapLength(d, 0, d.length);
        if (mbap > 0 && mbap == d.length) {
            return true;
        }
        if (d.length < 3) {
            return false;
        }
        int func = d[1] & 0xFF;
        int expected = switch (func & 0x7F) {
            case 0x01, 0x02, 0x03, 0x04 -> 3 + (d[2] & 0xFF) + 2;
            default -> (func & 0x80) != 0 ? 5 : ModbusFrameGuesser.rtuLength(d, 0, d.length);
        };
        return expected > 0 && expected == d.length;
    }

    /** Оборачивает RTU-кадр (с CRC) в Modbus/TCP ADU, выбрасывая CRC. */
    private static byte[] mbap(int txId, int unitId, byte[] rtuFrame) {
        return mbapFromPdu(txId, unitId, Arrays.copyOfRange(rtuFrame, 1, rtuFrame.length - 2));
    }

    /** Собирает Modbus/TCP ADU из готового PDU. */
    private static byte[] mbapFromPdu(int txId, int unitId, byte[] pdu) {
        int length = 1 + pdu.length;
        byte[] adu = new byte[6 + length];
        adu[0] = (byte) ((txId >> 8) & 0xFF);
        adu[1] = (byte) (txId & 0xFF);
        adu[2] = 0x00;
        adu[3] = 0x00;
        adu[4] = (byte) ((length >> 8) & 0xFF);
        adu[5] = (byte) (length & 0xFF);
        adu[6] = (byte) unitId;
        System.arraycopy(pdu, 0, adu, 7, pdu.length);
        return adu;
    }
}
