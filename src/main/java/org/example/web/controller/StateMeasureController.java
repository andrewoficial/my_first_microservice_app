/*
Тут будет обработка запросов
/state/devices - вывод списка подключенных устройств
/state/stage - вывод мнемосхемы с параметрами
/state/pool - опрашиваемая на большой скорости (аяксом) информация (текущие показания) (опрос 10 раз в сек)

 */

package org.example.web.controller;


import org.example.web.service.StateMeasureService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated // в классе будет использоваться валидация по аннотациям
@RestController
@RequestMapping("/measure_archive") // обработка запросов, начинающихся с ./genre
public class StateMeasureController {
    private final StateMeasureService stateMeasureService;

    public StateMeasureController(StateMeasureService StateMeasureService) {
        this.stateMeasureService = StateMeasureService;
    }


}
