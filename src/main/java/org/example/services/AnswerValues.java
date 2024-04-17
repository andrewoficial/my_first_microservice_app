package org.example.services;

import lombok.Getter;

public class AnswerValues {

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
            System.out.println("Слишком много значений одного измерения");
            return;
        }
        //System.out.println("add " + val + " units " + unit);
        values[counter] = val;
        units[counter] = unit;
        counter++;
    }
}
