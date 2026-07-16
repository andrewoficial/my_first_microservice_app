package org.example.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.connectionPool.AnyPoolService;
import org.example.services.connectionPool.ComDataCollector;
import org.example.utilites.properties.MyProperties;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PollingService {

    private final AnyPoolService anyPoolService;
    private final MyProperties properties;
    private final MainLeftPanelStateCollection stateCollection;

    public static class StartSendResult {
        private final String errorMessage;

        public StartSendResult(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return errorMessage == null;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    public void updatePoolDelay(int clientId, int poolDelay) {
        ComDataCollector collector = anyPoolService.findComDataCollectorByClientId(clientId);
        if (collector != null) {
            collector.setPoolDelay(poolDelay);
            log.info("Период опроса обновлён для клиента {}: {} мс", clientId, poolDelay);
        } else {
            log.info("Для клиента {} потока опроса не существует", clientId);
        }
    }

    public void toggleLog(int clientId, boolean needLog) {
        ComDataCollector collector = anyPoolService.findComDataCollectorByClientId(clientId);
        if (collector != null) {
            collector.setNeedLog(needLog, clientId);
            log.info("Лог для клиента {} установлен: {}", clientId, needLog);
        } else {
            log.info("Для клиента {} потока опроса не существует", clientId);
        }
    }

    public void updateCommand(int clientId, String prefix, String command) {
        ComDataCollector collector = anyPoolService.findComDataCollectorByClientId(clientId);
        if (collector != null) {
            collector.setTextToSendString(prefix, command, clientId);
        }
    }

    public StartSendResult startPolling(int clientId, int comPortIndex, int protocolIndex,
                                         boolean needPool, boolean isButton, int poolDelay) {
        try {
            anyPoolService.createOrUpdateComDataCollector(
                    stateCollection, clientId, comPortIndex, protocolIndex,
                    needPool, isButton, poolDelay);
            return new StartSendResult(null);
        } catch (ConnectException e) {
            log.error("Ошибка начала опроса для клиента {}: {}", clientId, e.getMessage());
            return new StartSendResult("Ошибка начала отправки " + e.getMessage());
        }
    }

    public ComDataCollector getCollector(int tabNumber, int totalTabs) {
        List<ComDataCollector> collectors = anyPoolService.getComDataCollectors();
        return collectors.size() > tabNumber ? collectors.get(tabNumber) : null;
    }

    public ComDataCollector getCollectorByClientId(int clientId) {
        return anyPoolService.findComDataCollectorByClientId(clientId);
    }

    public boolean isCollectorActive(int clientId) {
        return anyPoolService.isComDataCollectorByClientIdActiveDataSurvey(clientId);
    }

    public boolean isCollectorLogged(int clientId) {
        return anyPoolService.isComDataCollectorByClientIdLogged(clientId);
    }

    public void sendOnce(int clientId, String prefix, String command) {
        ComDataCollector collector = anyPoolService.findComDataCollectorByClientId(clientId);
        if (collector == null) {
            log.warn("Поток опроса для клиента {} не найден, отправка невозможна", clientId);
            return;
        }
        collector.sendOnce(prefix, command, clientId, false);
    }
}
