/*
Тут будет обработка запросов
/state/devices - вывод списка подключенных устройств
/state/stage - вывод мнемосхемы с параметрами
/state/pool - опрашиваемая на большой скорости (аяксом) информация (текущие показания) (опрос 10 раз в сек)

 */

package org.example.web.controller;


import org.example.web.entity.MyUser;
import org.example.web.service.StateMeasureService;
import org.example.web.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated // в классе будет использоваться валидация по аннотациям
@RestController
@RequestMapping("/api/v1/apps") // обработка запросов, начинающихся с ./genre
public class StateMeasureController {
    private final StateMeasureService stateMeasureService;
    private final UserService userService;

    public StateMeasureController(StateMeasureService StateMeasureService, UserService userService) {
        this.stateMeasureService = StateMeasureService;
        this.userService = userService;
    }

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
        userService.AddUser(user);
        return user.getName()+" is saved";
    }


}
