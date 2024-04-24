package org.example.services;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.gui.ChartWindow;

public class AnswerValues {
    private static final Logger log = Logger.getLogger(ChartWindow.class);
    private int quantity = 1;
    @Getter
    private double [] values;
    @Getter
    private String [] units;

    @Getter
    private int counter = 0;

    public AnswerValues(int quantity) {
        this.quantity = quantity;
        values = new double[quantity];
        units = new String[quantity];
    }

    public void addValue(double val, String unit){
        if(values.length < counter){
            log.error("Получено  слишком много значений одного измерения");
            return;
        }
        //System.out.println("add " + val + " units " + unit);
        values[counter] = val;
        units[counter] = unit;
        counter++;
    }
}
