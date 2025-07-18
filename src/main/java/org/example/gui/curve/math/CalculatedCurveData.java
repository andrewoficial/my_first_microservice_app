package org.example.gui.curve.math;

import lombok.Getter;
import lombok.Setter;
import org.example.gui.curve.CurveData;

public class CalculatedCurveData {
    @Getter @Setter
    CurveData curveData;
    @Getter @Setter
    Double nearestPoint;
    @Getter @Setter
    Double shiftSize;

}
