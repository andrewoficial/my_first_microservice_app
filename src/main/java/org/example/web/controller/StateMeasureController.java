/*
Тут будет обработка запросов
/state/devices - вывод списка подключенных устройств
/state/stage - вывод мнемосхемы с параметрами
/state/pool - опрашиваемая на большой скорости (аяксом) информация (текущие показания) (опрос 10 раз в сек)

 */

package org.example.web.controller;


import org.example.gui.MainLeftPanelStateCollection;
import org.example.gui.MainWindow;
import org.example.services.AnswerStorage;
import org.example.services.TabAnswerPart;
import org.example.web.entity.MyUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Profile({ "srv-offline", "srv-online" })
@Validated // в классе будет использоваться валидация по аннотациям
@RestController
@RequestMapping("/api/v1/apps") // обработка запросов, начинающихся с ./genre
public class StateMeasureController {

    @Autowired
    private MainLeftPanelStateCollection leftPanelStateCollection;


    @GetMapping("/welcome")
    public String welcome(){
        return "Welcome";
    }

    @GetMapping("/sys-setting")
    public String setting(){
        return  "Setting";
    }

    @PostMapping("/new-user")
    public String addUser(@RequestBody MyUser user){

        return user.getName()+" is saved";
    }

    @GetMapping("/state/pool/{tabNumber}")
    public Map<String, Object> getCurrentData(@PathVariable Integer tabNumber, @RequestParam Integer lastPosition) {
        int clientId = 0;
        if(leftPanelStateCollection != null){
            clientId = leftPanelStateCollection.getClientIdByTabNumber(tabNumber);
        }
        TabAnswerPart tabAnswerPart = AnswerStorage.getAnswersQueForWeb(lastPosition, clientId, true);

        Map<String, Object> response = new HashMap<>();
        response.put("answerPart", tabAnswerPart.getAnswerPart());
        response.put("newLastPosition", tabAnswerPart.getPosition()); // Передаем обновленную последнюю позицию

        return response;
    }

    @PostMapping("/state/send/{tabNumber}/{command}")
    public String sendCommand(@PathVariable Integer tabNumber, @PathVariable String command) {
        System.out.println("Try send " + command);
        // Выполняем отправку команды
        //MainWindow.webSend(tabNumber, command);
        return "OK";
    }
}
