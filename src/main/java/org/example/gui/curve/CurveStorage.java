package org.example.gui.curve;

import org.apache.log4j.Logger;

import java.util.HashMap;

public class CurveStorage {
    private Logger log = Logger.getLogger(CurveStorage .class);
    private HashMap <String, CurveData> curves = new HashMap<>();

    public boolean isEmpty(){
        return curves.isEmpty();
    }

    public void addOrUpdateCurve(String name, CurveData curve){
        if(name == null || name.isEmpty()){
            log.warn("Пустое имя кривой");
            return;
        }
        if(curve == null || (! curve.isConsistent())){
            log.warn("Попытка добавить некорректную кривую");
            return;
        }
        if(curves.containsKey(name)){
            log.warn("Кривая с таким именем уже существует");
            curves.remove(name);
        }
        curves.put(name, curve);
    }

    public CurveData getCurve(String name){
        if(name == null || name.isEmpty()){
            log.warn("Попытка получить кривую с пустым именем");
            return null;
        }
        if(! curves.containsKey(name)){
            log.warn("Кривая с таким именем не существует");
            return null;
        }
        return curves.get(name);
    }

    public void removeCurve(String name){
        if(name == null || name.isEmpty()){
            log.warn("Попытка удалить кривую с пустым именем");
            return;
        }
        if(! curves.containsKey(name)){
            log.warn("Кривая с таким именем не существует");
            return;
        }
        curves.remove(name);
    }
    public boolean isContains(String name){
        if(name == null || name.isEmpty()){
            log.warn("Попытка получить кривую с пустым именем");
            return false;
        }
        return curves.containsKey(name);
    }


}
