package org.example.gui.mgstest.gui.names;

import java.util.ArrayList;

public class CoefficientToolTipsText {

    public final ArrayList <String> oxygen;
    public final ArrayList <String> coDt;
    public final ArrayList <String> h2s;

    public CoefficientToolTipsText(){
        oxygen = new ArrayList<>();
        oxygen.add("Заводской сдвиг нуля");
        oxygen.add("Заводская калибровка");
        oxygen.add("Пользовательский сдвиг нуля");
        oxygen.add("Пользовательская калибровка");
        oxygen.add("Дельта температуры");
        oxygen.add("Коэффициент нулевой степени полинома зависимости по концентрации");
        oxygen.add("Коэффициент первой степени полинома зависимости по концентрации");
        oxygen.add("Коэффициент второй степени полинома зависимости по концентрации");
        oxygen.add("Коэффициент третьей степени полинома зависимости по концентрации");
        oxygen.add("Коэффициент нулевой степени полинома зависимости по температуре");
        oxygen.add("Коэффициент первой степени полинома зависимости по температуре");
        oxygen.add("Коэффициент второй степени полинома зависимости по температуре");
        oxygen.add("Коэффициент третьей степени полинома зависимости по температуре");
        oxygen.add("Дельта давления");
        oxygen.add("Коэффициент нулевой степени полинома зависимости нулевого уровня");
        oxygen.add("Коэффициент первой степени полинома зависимости нулевого уровня");
        oxygen.add("Коэффициент второй степени полинома зависимости нулевого уровня");
        oxygen.add("Коэффициент третьей степени полинома зависимости нулевого уровня");
        oxygen.add("Коэффициент четвёртой степени полинома зависимости нулевого уровня");

        coDt = new ArrayList<>();
        coDt.add("initOfs");
        coDt.add("initAmp");
        coDt.add("tarOfs");
        coDt.add("tarAmp");
        coDt.add("dT");
        coDt.add("D0");
        coDt.add("D1");;
        coDt.add("T0");
        coDt.add("T1");
        coDt.add("T2");
        coDt.add("T3");
        coDt.add("B0");
        coDt.add("B1");
        coDt.add("B2");

        h2s = new ArrayList<>();
        h2s.add("initOfs");
        h2s.add("initAmp");
        h2s.add("tarOfs");
        h2s.add("tarAmp");
        h2s.add("dT");
        h2s.add("D0");
        h2s.add("D1");;
        h2s.add("T0");
        h2s.add("T1");
        h2s.add("T2");
        h2s.add("T3");
        h2s.add("B0");
        h2s.add("B1");
        h2s.add("B2");
    }
}
