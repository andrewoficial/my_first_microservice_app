/*
Тут будет обработка запросов
/state/devices - вывод списка подключенных устройств
/state/stage - вывод мнемосхемы с параметрами
/state/pool - опрашиваемая на большой скорости (аяксом) информация (текущие показания) (опрос 10 раз в сек)

 */

package org.example.web.controller;


import lombok.RequiredArgsConstructor;
import org.example.services.AnswerStorage;
import org.example.services.AnswerValues;
import org.example.services.DeviceAnswer;
import org.example.services.GraphDataRepository;
import org.example.services.GraphSample;
import org.example.services.TabAnswerPart;
import org.example.services.TabService;
import org.example.services.PollingService;
import org.example.web.entity.MyUser;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile({ "srv-offline", "srv-online" })
@Validated
@RestController
@RequestMapping("/api/v1/apps")
@RequiredArgsConstructor
public class StateMeasureController {

    private final TabService tabService;
    private final PollingService pollingService;
    private final AnswerStorage answerStorage;
    private final GraphDataRepository graphDataRepository;

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
        TabAnswerPart tabAnswerPart = tabService.getTabData(tabNumber, lastPosition, true);

        Map<String, Object> response = new HashMap<>();
        response.put("answerPart", tabAnswerPart.getAnswerPart());
        response.put("newLastPosition", tabAnswerPart.getPosition());

        return response;
    }

    /**
     * Те же точки, что читает ChartWindow из {@link GraphDataRepository}.
     * {@code since} — миллисекунды последней уже показанной точки, 0 для первой загрузки.
     */
    @GetMapping("/state/graph/{tabNumber}")
    public Map<String, Object> graphPoints(@PathVariable Integer tabNumber,
                                            @RequestParam(defaultValue = "0") long since) {
        Map<String, Object> response = new HashMap<>();
        Integer clientId = tabService.getClientIdByTab(tabNumber);
        if (clientId == null || clientId < 0) {
            response.put("command", "");
            response.put("units", List.of());
            response.put("points", List.of());
            response.put("latest", since);
            return response;
        }
        String command = answerStorage.getStableCommand(clientId);
        List<String> units = unitsOf(clientId);
        List<GraphSample> points = command == null
                ? List.of()
                : graphDataRepository.pointsAfter(clientId, command, since, 500);
        long latest = since;
        for (GraphSample point : points) {
            latest = Math.max(latest, point.epochMilli());
        }
        response.put("command", command == null ? "" : command);
        response.put("units", units);
        response.put("points", points);
        response.put("latest", latest);
        return response;
    }

    private List<String> unitsOf(int clientId) {
        DeviceAnswer last = answerStorage.getLastAnswerForTab(clientId);
        if (last == null || last.getAnswerReceivedValues() == null) {
            return List.of();
        }
        AnswerValues values = last.getAnswerReceivedValues();
        if (values.getUnits() == null) {
            return List.of();
        }
        return new ArrayList<>(Arrays.asList(values.getUnits()));
    }

    @PostMapping("/state/send/{tabNumber}/{command}")
    public String sendCommand(@PathVariable Integer tabNumber, @PathVariable String command) {
        if (!TerminalController.currentUserCanSend()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Недостаточно прав для отправки команды");
        }
        Integer clientId = tabService.getClientIdByTab(tabNumber);
        if (clientId == null || clientId == -1) {
            return "Tab not found";
        }
        pollingService.sendOnce(clientId, "", command);
        return "OK";
    }
}
