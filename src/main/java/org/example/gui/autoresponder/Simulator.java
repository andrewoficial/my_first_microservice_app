// org/example/gui/autoresponder/Simulator.java
package org.example.gui.autoresponder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Simulator {

    public static void simulateAll(DpsEmulatorWindow window) {
        try {
            List<byte[]> packets = new ArrayList<>();

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xE1,
                    new byte[]{(byte) Integer.parseInt(window.jtfStatus.getText())}));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xDE,
                    window.jtfModel.getText().getBytes()));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xE0,
                    window.jtfSwVersion.getText().getBytes()));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xDF,
                    window.jtfHwVersion.getText().getBytes()));

            //F1 B1 C2 04 6F 12 03 3B 85
            packets.add(window.buildResponse((byte) 0xB1, (byte) 0xC2,
                    ByteUtils.floatToBytes(Float.parseFloat(window.jtfCurrentSet.getText()))));

            packets.add(window.buildResponse((byte) 0xB1, (byte) 0xC1,
                    ByteUtils.floatToBytes(Float.parseFloat(window.jtfVoltageSet.getText()))));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xE2,
                    ByteUtils.floatToBytes(Float.parseFloat(window.jtfUpperLimitVoltage.getText()))));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xE3,
                    ByteUtils.floatToBytes(Float.parseFloat(window.jtfUpperLimitCurrent.getText()))));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xFF,
                    DumpGenerator.generateDump(window)));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xC0,
                    ByteUtils.floatToBytes(Float.parseFloat(window.jtfVin.getText()))));

            packets.add(window.buildResponse((byte)0xA1, (byte)0xC3,
                    ByteBuffer.allocate(12)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putFloat(Float.parseFloat(window.jtfMeasVoltOut.getText()))
                            .putFloat(Float.parseFloat(window.jtfMeasCurrentOut.getText()))
                            .putFloat(Float.parseFloat(window.jtfMeasWattOut.getText()))
                            .array()
            ));

            packets.add(window.buildResponse((byte) 0xA1, (byte) 0xC4,
                    ByteUtils.floatToBytes(Float.parseFloat(window.jtfTemperature.getText()))));


            // отправляем последовательно
            for (byte[] p : packets) {
                window.comPort.writeBytes(p, p.length);
                LogUtil.logSent(window.jtaLog, p);
                Thread.sleep(30);
            }

            LogUtil.logMessage(window.jtaLog, "Simulated responses sent.");

        } catch (Exception ex) {
            DpsEmulatorWindow.log.error("simulateAll() error", ex);
        }
    }

    public static void simulateForGraph(DpsEmulatorWindow window) {
        new Thread(() -> {
            try {
                AtomicBoolean simulating = new AtomicBoolean(true);
                // To stop, perhaps add a button, but for now, simulate 100 points
                int numPoints = 100;
                float baseVoltOut = Float.parseFloat(window.jtfMeasVoltOut.getText());
                float baseCurrentOut = Float.parseFloat(window.jtfMeasCurrentOut.getText());
                float baseWattOut = Float.parseFloat(window.jtfMeasWattOut.getText());
                float vin = Float.parseFloat(window.jtfVin.getText());
                float upperLimitVoltage = Float.parseFloat(window.jtfUpperLimitVoltage.getText());
                float upperLimitCurrent = Float.parseFloat(window.jtfUpperLimitCurrent.getText());
                float temperature = Float.parseFloat(window.jtfTemperature.getText());

                for (int i = 0; i < numPoints && simulating.get(); i++) {
                    // Варьируем значения для имитации изменения данных
                    float voltOut = baseVoltOut + (float) Math.sin(i * 0.1) * 0.5f;
                    float currentOut = baseCurrentOut + (float) Math.cos(i * 0.1) * 0.2f;
                    float wattOut = voltOut * currentOut; // Вычисляем мощность

                    // c3: Meas Out (V, I, W)
                    byte[] c3Data = ByteBuffer.allocate(12)
                            .order(ByteOrder.LITTLE_ENDIAN)
                            .putFloat(voltOut)
                            .putFloat(currentOut)
                            .putFloat(wattOut)
                            .array();
                    byte[] packet = window.buildResponse((byte) 0xA1, (byte) 0xC3, c3Data);
                    window.comPort.writeBytes(packet, packet.length);
                    LogUtil.logSent(window.jtaLog, packet);

                    // c0: Vin (с небольшим случайным вариациями)
                    packet = window.buildResponse((byte) 0xA1, (byte) 0xC0,
                            ByteUtils.floatToBytes(vin + (float) Math.random() * 0.1f - 0.05f));
                    window.comPort.writeBytes(packet, packet.length);
                    LogUtil.logSent(window.jtaLog, packet);

                    // e2: Upper Limit Voltage (с небольшим вариациями)
                    packet = window.buildResponse((byte) 0xA1, (byte) 0xE2,
                            ByteUtils.floatToBytes(upperLimitVoltage + (float) Math.random() * 0.1f - 0.05f));
                    window.comPort.writeBytes(packet, packet.length);
                    LogUtil.logSent(window.jtaLog, packet);

                    // e3: Upper Limit Current (фиксировано)
                    packet = window.buildResponse((byte) 0xA1, (byte) 0xE3,
                            ByteUtils.floatToBytes(upperLimitCurrent));
                    window.comPort.writeBytes(packet, packet.length);
                    LogUtil.logSent(window.jtaLog, packet);

                    // c4: Temperature (с небольшим случайным вариациями)
                    packet = window.buildResponse((byte) 0xA1, (byte) 0xC4,
                            ByteUtils.floatToBytes(temperature + (float) Math.random() * 2f - 1f));
                    window.comPort.writeBytes(packet, packet.length);
                    LogUtil.logSent(window.jtaLog, packet);

                    Thread.sleep(500); // Задержка 500 мс между наборами пакетов для обновления графика
                }

                LogUtil.logMessage(window.jtaLog, "Simulated graph sent.");

            } catch (Exception ex) {
                DpsEmulatorWindow.log.error("simulateForGraph() error", ex);
            }
        }).start();
    }
}