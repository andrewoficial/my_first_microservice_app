package org.example.services;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;


public class AnswerValues {
    private static final Logger log = Logger.getLogger(AnswerValues.class);

    @Getter
    private final double [] values;
    @Getter
    private final String [] units;

    private int counter = 0;

    @Getter @Setter
    private int direction = -1;

    public AnswerValues(int quantity) {
        values = new double[quantity];
        units = new String[quantity];
    }

    public void addValue(double val, String unit){
        if(values.length <= counter){
            log.error("Получено  слишком много значений одного измерения");
            return;
        }
        values[counter] = val;
        units[counter] = unit;
        counter++;
    }

    public int getCounter(){
        return values.length; //Потому что счётчик плюсуется лишний раз!
    }
}
