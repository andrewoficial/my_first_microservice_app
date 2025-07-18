package org.example.gui.curve;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CurveData {
    private Logger log = Logger.getLogger(CurveStorage .class);

    @Getter @Setter
    private CurveMetaData curveMetaData = new CurveMetaData();

    List<Map.Entry<Double, Double>> curvePoints = new ArrayList<>();



    public void addCurvePoint(Double measurement, Double temperature) {
        if(isTemperatureIncorrect(measurement, temperature)) return;
        if(isAddingDenied()) return;
        curvePoints.add(new LinkedHashMap.SimpleEntry<>(measurement, temperature));

    }

    public void addCurvePointFromString(String measurement, String temperature) {
        if(measurement == null || temperature == null){
            log.warn("Одна из строк null");
            return;
        }
        if(measurement.isEmpty() || temperature.isEmpty()){
            log.warn("Одна из строк пуста");
        }

        Double mes = null;
        Double temper = null;

        try{
            mes = Double.parseDouble(measurement);
            temper = Double.parseDouble(temperature);
        }catch (NumberFormatException ex){
            log.warn("Исключение во время получения числа из строки"+ ex.getMessage());
            return;
        }
        addCurvePoint(mes, temper);
    }

    public void insertCurvePoint(int index, Double measurement, Double temperature) {
        if(isTemperatureIncorrect(measurement, temperature)) return;
        if(isAddingDenied()) return;
        if(curvePoints.size() < index) return;
        if(index < 0) return;
        curvePoints.add(index, new LinkedHashMap.SimpleEntry<>(measurement, temperature));
    }


    public void insertCurvePointFromString(int position, String measurement, String temperature) {
        if(measurement == null || temperature == null){
            log.warn("Одна из строк null");
            return;
        }
        if(measurement.isEmpty() || temperature.isEmpty()){
            log.warn("Одна из строк пуста");
        }

        Double mes = null;
        Double temper = null;

        try{
            mes = Double.parseDouble(measurement);
            temper = Double.parseDouble(temperature);
        }catch (NumberFormatException ex){
            log.warn("Исключение во время получения числа из строки"+ ex.getMessage());
            return;
        }
        insertCurvePoint(position, mes, temper);
    }

    public List<Map.Entry<Double, Double>> getCurvePoints(){
        if(isConsistent()){
            return this.curvePoints;
        }
        log.warn("Кривая некорректна, возвращаю null");
        return null;
    }
    public boolean isTemperatureIncorrect(Double measurement, Double temperature){
        if(curveMetaData.getSetPointLimit() < temperature){
            log.warn("Переданное значение температуры выше допустимого" + curveMetaData.getSetPointLimit());
            log.error("ПРОВЕРКА ОТКЛЮЧЕНА НА УРОВНЕ ПРОГРАММЫ");
            return false;//ToDO обсудить с илъёй
        }
        return false;
    }

    public boolean isAddingDenied(){
        if(curveMetaData.getNumberOfBreakpoints() != null &&   curvePoints.size() > curveMetaData.getNumberOfBreakpoints()){
            log.error("Превышен лимит точек для кривой" + curveMetaData.getSensorModel() + " ограничение: " + curveMetaData.getNumberOfBreakpoints());
            return true;
        }
        return false;
    }

    public boolean isConsistent(){
        if(curveMetaData == null) return false;
        if(curveMetaData.getNumberOfBreakpoints() != null &&  curveMetaData.getNumberOfBreakpoints().equals(0)) return false;
        if(curveMetaData.getSetPointLimit().equals(0)) return false;
        if(curveMetaData.getDataFormat() == null) return false;
        if(curveMetaData.getSerialNumber() == null || curveMetaData.getSerialNumber().isEmpty()) return false;
        if(curvePoints == null || curvePoints.isEmpty()) return false;
        return true;
    }

    public void setCurvePoints(List<Map.Entry<Double, Double>> calculatedRawData) {
        if(calculatedRawData == null) return;
        if(calculatedRawData.isEmpty()) return;
        this.curvePoints = calculatedRawData;
    }
}
