package org.example.web.service;

/*
Что-то сервисное не до конца разобрался зачем оно нужно :(
Из репозитория (шаблоны запросов) получает данные и обрабатывает
 */

import org.example.web.entity.StateMeasure;
import org.example.web.exception.StateMesureExceprion;
import org.example.web.repository.StateMeasureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StateMeasureService {
    private final StateMeasureRepository stateMeasureRepository;

    @Autowired
    public StateMeasureService(StateMeasureRepository stateMeasureRepository) {
        this.stateMeasureRepository = stateMeasureRepository;
    }

    public String saveStateMeasure(StateMeasure stateMeasure) throws StateMesureExceprion {
        if (StateMeasureRepository.existsById(stateMeasure.getId())) {
            throw new StateMesureExceprion("Снимок имерения с url '" + stateMeasure.getId() + "' уже существует");
        }
        stateMeasure.setId(System.currentTimeMillis());
        return stateMeasureRepository.save(stateMeasure).toString();
    }


    public boolean isStateMeasureByUrl(String url) throws StateMesureExceprion {
        return StateMeasureRepository.existsById(Long.parseLong(url));
    }
}
