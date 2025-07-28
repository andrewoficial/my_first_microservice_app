package org.example.gui.curve;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.utilites.MyUtilities;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.IntConsumer;

public class CurveDeviceCommander {
    private SerialPort comPort;
    private Logger log = Logger.getLogger(CurveDeviceCommander.class);
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

        byte[] testCommand = MyUtilities.strToByte("*IDN?", '\r');
        try {
            comPort.writeBytes(testCommand, testCommand.length);
        } catch (Exception ex) {
            return "Ошибка 002. Команда не отправлена." + ex.getMessage();
        }

        byte[] response = null;
        log.info("Начинаю считывание ответа *IDN?");
        try {
            response = readAnswer();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return "Ошибка 003. Ответ не прочитан." + e.getMessage();
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

    public ArrayList<CurveMetaData> getListOfCurvesInDevice(IntConsumer progressConsumer){

        if(!isPortConsistent()) return null;
        ArrayList<CurveMetaData> list = new ArrayList<>();
        //[CRVHDR? 22] //
        int READ_LIMIT = 65;//Limit for curve list reading. Ограничение на размер считываемых кривых.
        int percentOfAsking = 0;
        for (int i = 1; i < READ_LIMIT; i++) {
            String cmdToSend= "CRVHDR? " + i;
            byte[] curveInfoAsk = MyUtilities.strToByte("CRVHDR? " + i, '\r');
            try {
                comPort.writeBytes(curveInfoAsk, curveInfoAsk.length);
            } catch (Exception ex) {
                return null;
            }

            byte[] response = null;
            log.info("Начинаю считывание ответа " + cmdToSend);
            try {
                response = readAnswer();
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
                return null;
            }
            //[CRVHDR? 21] -> [CurName,SerNum,2,800.0000,1]
            /*
            CurName - Name of Curve
            SerNum - Serila number of sensor
            2 - Curve Format
            {
                Curve Formats (based on CurveHandler Ver 3.3) (at 18.07.2025 on live Device)
                1 - million-Volts vs Kelvin
                2 - Volts vs Kelvin
                3 - Ohms vs Kelvin
                4 - Log Ohms vs Kelvin
                5 - Volts vs Kelvin (spline)
                6 - Ohms vs Kelvin (spline)
            }
            800.0000 - Max Kelvin Temperature (SetPointsLimit)
            1 - unknown parameter
             */

            StringBuilder sb = new StringBuilder();
            for (byte b : response) {
                if (b > 32 && b < 127) {
                    sb.append((char) b);
                }
            }
            if (sb.isEmpty()) {
                return null;
            }
            String responseStr = sb.toString().trim();

            // Разбиваем ответ по запятым и проверяем структуру
            String[] parts = responseStr.split(",", -1); // -1 сохраняет пустые значения

            // Проверяем что получили ровно 5 частей
            if(parts.length < 5) {
                log.warn("Неверный формат ответа для кривой " + i + ": " + responseStr);
                continue;
            }

            // Извлекаем и чистим компоненты
            String sensorModel = parts[0].trim();
            String serialNumber = parts[1].trim();
            String dataFormatStr = parts[2].trim();
            String setPointStr = parts[3].trim().replace("+", ""); // Удаляем возможный плюс
            String tempCoeff = parts[4].trim();

            // Проверяем что кривая не пустая (серийник должен быть заполнен)
            if(serialNumber.isEmpty()) {
                continue; // Пропускаем пустые кривые
            }

            // Парсим числовые параметры с обработкой ошибок
            try {
                CurveMetaData curveMetaData = new CurveMetaData();

                curveMetaData.setSensorModel(sensorModel);
                curveMetaData.setSerialNumber(serialNumber);

                // Преобразуем формат данных в enum
                int formatCode = Integer.parseInt(dataFormatStr);
                curveMetaData.setDataFormat(CurveDataTypes.getByValue(formatCode));

                // Преобразуем температуру (дробь -> целое)
                double setPointValue = Double.parseDouble(setPointStr);
                curveMetaData.setSetPointLimit((int) setPointValue);

                curveMetaData.setTemperatureCoefficient(tempCoeff);

                curveMetaData.setNumberInDeviceMemory(i);
                if(i > 20){
                    curveMetaData.setIsUserCurve(true);
                }else{
                    curveMetaData.setIsUserCurve(false);
                }
                list.add(curveMetaData);
                percentOfAsking = (int) ((i + 1) * 100.0 / READ_LIMIT);
                progressConsumer.accept(percentOfAsking);
            } catch (IllegalArgumentException e) {
                log.warn("Ошибка парсинга кривой " + i + ": " + e.getMessage() + "\nОтвет: " + responseStr);
            }
        }
        log.info("Возвращаю список кривых: " + list.size());
        return list;
    }

    public List<Map.Entry<Double, Double>> readCurveFromDevice(int curveAddress, IntConsumer progressConsumer) throws Exception {
        if (!isPortConsistent()) {
            throw new Exception("Порт не инициализирован");
        }
        List<Map.Entry<Double, Double>> points = new ArrayList<>();
        final int MAX_POINTS = 9995; // Максимальное количество точек для чтения
        int pointsRead = 0;
        try {
            // Начинаем чтение точек кривой
            for (int pointIndex = 1; pointIndex <= MAX_POINTS; pointIndex++) {
                // Формируем команду для запроса точки
                String cmd = "CRVPT? " + curveAddress + "," + pointIndex;
                byte[] commandBytes = MyUtilities.strToByte(cmd, '\r');

                // Отправка команды
                comPort.writeBytes(commandBytes, commandBytes.length);

                // Чтение ответа
                byte[] response = readAnswer();
                if (response == null || response.length == 0) {
                    log.warn("Пустой ответ для точки " + pointIndex + " кривой " + curveAddress);
                    break;
                }

                // Преобразуем ответ в строку и очищаем
                StringBuilder sb = new StringBuilder();
                for (byte b : response) {
                    if (b > 32 && b < 127) {
                        sb.append((char) b);
                    }
                }
                String responseStr = sb.toString().trim();

                // Проверяем на признак конца кривой (0.000000,0.000000)
                if ("0.000000,0.000000".equals(responseStr)) {
                    log.info("Обнаружен конец кривой (точка 0.000000,0.000000)" + pointIndex);
                    break;
                }

                // Парсим точку
                String[] coords = responseStr.split(",");
                if (coords.length != 2) {
                    log.warn("Некорректный формат точки: " + responseStr);
                    continue;
                }

                try {
                    double x = Double.parseDouble(coords[0]);
                    double y = Double.parseDouble(coords[1]);
                    points.add(new AbstractMap.SimpleEntry<>(x, y));
                    pointsRead++;

                    // Обновляем прогресс
                    int progress = (int) ((pointIndex * 100.0) / 350);
                    progressConsumer.accept(progress);

                    // Дополнительная проверка на конец кривой
                    if (x == 0.0 && y == 0.0) {
                        log.info("Обнаружена нулевая точка, завершение чтения");
                        break;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Ошибка парсинга точки: " + responseStr, e);
                }
                safetySleep(25);
            }

            log.info("Прочитано точек для кривой {"+curveAddress+"}: {"+pointsRead+"}");
            return points;
        } catch (Exception e) {
            log.error("Ошибка чтения кривой: " + curveAddress, e);
            throw new Exception("Ошибка чтения кривой: " + e.getMessage());
        }
    }

    public void writeCurveToDevice(CurveData data, int address, IntConsumer progressCallback) throws Exception {
        if (!isPortConsistent()) {
            throw new Exception("Порт не инициализирован");
        }

        inSendingProcess = true;
        try {


            // Отправка точек кривой
            List<Map.Entry<Double, Double>> points = data.getCurvePoints();
            points.add(new AbstractMap.SimpleEntry<>(0.0, 0.0)); // В конце кривой добавляется точка 0.00000 <==> 0.0000
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
                percent = (int) ((i + 1) * 99.0 / totalPoints);
                progressCallback.accept(percent);
            }
            // Подтсверждение завершения отправки
            CurveMetaData metaData = data.getCurveMetaData();
            String headerCommand = String.format("CRVHDR %d,%s,%s,%d,%.3f,%d\n",
                    address,
                    metaData.getSensorModel(),
                    metaData.getSerialNumber(),
                    metaData.getDataFormat().getValue(), // Предполагается метод getValue()
                    (double) metaData.getSetPointLimit(),
                    1); // Коэффициент температуры

            sendCommand(headerCommand);
            progressCallback.accept(100);
        } finally {
            inSendingProcess = false;
        }
    }

    public void sendWakeUp() throws Exception {
        sendCommand("WAKEUP\n");
    }

    public void sendSleep() throws Exception {
        sendCommand("SLEEP\n");
    }

    public StateWords getState() throws Exception {
        sendCommand("STATE?\n");
        safetySleep(20);

        byte[] response = null;
        log.info("Начинаю считывание ответа getState");
        response = readAnswer();


        StringBuilder sb = new StringBuilder();
        for (byte b : response) {
            if (b > 32 && b < 127) {
                sb.append((char) b);
            }
        }

        int state = Integer.parseInt(sb.toString());
        return StateWords.getByValue(state);
    }

    private void sendCommand(String command) throws Exception {
        byte[] bytes = command.getBytes(StandardCharsets.US_ASCII);
        int bytesWritten = comPort.writeBytes(bytes, bytes.length);

        if (bytesWritten != bytes.length) {
            throw new Exception("Ошибка отправки команды: " + command);
        }

        // Небольшая задержка для обработки прибором
        Thread.sleep(10);
    }

    private byte[] readAnswer() throws Exception {
        ArrayList <Byte> response = new ArrayList<>();
        byte[] buffer = new byte[600];
        int available = comPort.bytesAvailable();
        //log.info("Готово для считывания данных: " + available);
        int repeatLimit = 5;
        int repeatCount = 0;
        while(available == 0 && repeatCount < repeatLimit){
            safetySleep(10);
            available = comPort.bytesAvailable();
            repeatCount++;
            //log.info("После ожидания ответа " + repeatCount + " раз данных получено:" + available);
        }
        while(available > 0){
            for (int i = 0; i < available; i++) {
                comPort.readBytes(buffer, available);
            }
            for (int i = 0; i < available; i++) {
                response.add(buffer[i]);
            }
            Arrays.fill(buffer, (byte) 0);
            safetySleep(10);
            available = comPort.bytesAvailable();
        }
        byte[] forReturn = new byte[response.size()];
        for (int i = 0; i < response.size(); i++) {
            forReturn[i] = response.get(i);
        }
        log.info("Данные считаны: " + response.size() + " количество попыток:" + repeatCount);
        return forReturn;
    }

    private void safetySleep(long time){
        try {
            Thread.sleep(time);
        } catch (InterruptedException ex) {
            System.out.println(ex.getMessage());
        }
    }

}
