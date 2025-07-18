package org.example.gui.curve.math;

import org.example.gui.curve.CurveData;
import org.example.gui.curve.CurveMetaData;

import java.awt.event.ActionEvent;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Calculator {


    public CalculatedCurveData calculateActionHandlerNew(String kelvinString, String voltsString, CurveData selectedCurve) throws CurveCalculationException{
        //1. Серия проверок
        if(kelvinString == null || kelvinString.isEmpty()) throw new CurveCalculationException("Пустое значение температуры");
        if(voltsString == null || voltsString.isEmpty()) throw new CurveCalculationException("Пустое значение напряжения");
        if(selectedCurve == null || selectedCurve.getCurvePoints() == null || selectedCurve.getCurvePoints().isEmpty()) throw new CurveCalculationException("Пустой набор данных кривой");

        kelvinString = kelvinString.trim();
        kelvinString = kelvinString.replaceAll(",", ".");
        voltsString = voltsString.trim();
        voltsString = voltsString.replaceAll(",", ".");
        if(kelvinString.isEmpty()) throw new CurveCalculationException("Пустое значение температуры после очистки строки");
        if(voltsString.isEmpty()) throw new CurveCalculationException("Пустое значение напряжения после очистки строки");
        double measuredKelvin;
        double measuredVolts;
        try{
            measuredKelvin = Double.parseDouble(kelvinString);
        }catch (NumberFormatException ex){
            throw new CurveCalculationException("Неверное значение температуры" + ex.getMessage());
        }
        try{
            measuredVolts = Double.parseDouble(voltsString);
        }catch (NumberFormatException ex){
            throw new CurveCalculationException("Неверное значение напряжения" + ex.getMessage());
        }

        // 2.Создание заготовки кривой на снове переаднной (deep copy)
        CurveData calculatedCurve = new CurveData();
        CurveMetaData curveMetaData = selectedCurve.getCurveMetaData().clone();
        calculatedCurve.setCurveMetaData(curveMetaData);
        List<Map.Entry<Double, Double>> selectedPoints = new ArrayList<>();
        for (Map.Entry<Double, Double> curvePoint : selectedCurve.getCurvePoints()) {
            Map.Entry<Double, Double> selectedPoint = new AbstractMap.SimpleEntry<>(curvePoint.getKey(), curvePoint.getValue());
            selectedPoints.add(selectedPoint);
        }

        // 3. Поиск ближайшей точки по температуре
        double minDiff = Double.MAX_VALUE; //ToDo Ограничение на отклонение
        Map.Entry<Double, Double> nearestPoint = null;

        for (Map.Entry<Double, Double> point : selectedPoints) {
            double tempDiff = Math.abs(point.getValue() - measuredKelvin);
            if (tempDiff < minDiff) {
                minDiff = tempDiff;
                nearestPoint = point;
            }
        }

        // 4. Проверка найденной точки
        if (nearestPoint == null) {
            throw new CurveCalculationException("Не удалось найти ближайшую точку в открытой кривой");
        }

        // 6. Расчет разницы напряжений
        double voltageDiff = measuredVolts - nearestPoint.getKey();

        // 7. Обновление меток
        //jlbNearestPoint.setText(String.format("Ближ.т.: %.2fK", nearestPoint.getValue()));
        //jlbAddingVolts.setText(String.format("Точки смещены на: %.4fV", voltageDiff));

        // 8. Применение смещения ко всем точкам
        List<Map.Entry<Double, Double>> editedPoints = new ArrayList<>();
        for (Map.Entry<Double, Double> point : selectedPoints) {
            // Создаем новую точку со смещенным напряжением
            double newVoltage = point.getKey() + voltageDiff;
            editedPoints.add(new AbstractMap.SimpleEntry<>(newVoltage, point.getValue()));
        }
        calculatedCurve.setCurvePoints(editedPoints);
        CalculatedCurveData calculatedCurveData = new CalculatedCurveData();
        calculatedCurveData.setCurveData(calculatedCurve);
        calculatedCurveData.setNearestPoint(nearestPoint.getValue());
        calculatedCurveData.setShiftSize(voltageDiff);


        return calculatedCurveData;
    }
}
