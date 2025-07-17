package org.example.gui.curve;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.example.utilites.MyUtilities;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;

public class CurveDeviceCommander {
    SerialPort comPort;
    @Getter
    private volatile boolean inSendingProcess = false;
    @Getter
    private volatile int percent = 0;

    CurveDeviceCommander (SerialPort port){
        comPort = port;
    }

    public boolean isPortConsistent(){
        return comPort != null && comPort.isOpen();
    }

    public String pingDevice(){
        if(!isPortConsistent()) return "Порт не инициализирован";

        byte[] testCommand = MyUtilities.strToByte("*IDN?", '\n');
        try {
            comPort.writeBytes(testCommand, testCommand.length);
        } catch (Exception ex) {
            return "Ошибка 002. Команда не отправлена." + ex.getMessage();
        }

        byte[] response = new byte[100];
        try {
            comPort.readBytes(response, response.length);
        } catch (Exception ex) {
            return "Ошибка 003. Ответ не прочитан." + ex.getMessage();
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            if (b > 32 && b < 127) {
                sb.append((char) b);
            }
        }
        if (sb.isEmpty()) {
            return "Нет ответа от прибора";
        }
        if ("ECT,MODEL290,SN2307,Ver1.01/ECT,MODEL2901,SN1234,Ver1.00".equalsIgnoreCase(sb.toString().trim())) {
            return "Прибор обнаружен";
        } else {
            System.out.println("Найденный ИД: [" + sb.toString().trim() + "]");
            return " Прибор не обнаружен (неверный ответ)";
        }
    }

    public void writeCurveToDevice(CurveData data, int address, IntConsumer progressCallback)
            throws Exception {

        if (!isPortConsistent()) {
            throw new Exception("Порт не инициализирован");
        }

        inSendingProcess = true;
        try {
            // Отправка заголовка кривой
            CurveMetaData metaData = data.getCurveMetaData();
            String headerCommand = String.format("CRVHDR %d,%s,%s,%d,%.3f,%d\n",
                    address,
                    metaData.getSensorModel(),
                    metaData.getSerialNumber(),
                    metaData.getDataFormat().getValue(), // Предполагается метод getValue()
                    (double) metaData.getSetPointLimit(),
                    1); // Коэффициент температуры

            sendCommand(headerCommand);

            // Отправка точек кривой
            List<Map.Entry<Double, Double>> points = data.getCurvePoints();
            int totalPoints = points.size();

            for (int i = 0; i < totalPoints; i++) {
                Map.Entry<Double, Double> point = points.get(i);
                String pointCommand = String.format(Locale.US, "CRVPT %d,%d,%.5f,%.4f\n",
                        address,
                        i + 1, // Номер точки начинается с 1
                        point.getKey(),   // Напряжение
                        point.getValue()  // Температура
                );

                sendCommand(pointCommand);

                // Обновление прогресса
                percent = (int) ((i + 1) * 100.0 / totalPoints);
                progressCallback.accept(percent);
            }
        } finally {
            inSendingProcess = false;
        }
    }

    private void sendCommand(String command) throws Exception {
        byte[] bytes = command.getBytes(StandardCharsets.US_ASCII);
        int bytesWritten = comPort.writeBytes(bytes, bytes.length);

        if (bytesWritten != bytes.length) {
            throw new Exception("Ошибка отправки команды: " + command);
        }

        // Небольшая задержка для обработки прибором
        Thread.sleep(50);
    }

}
