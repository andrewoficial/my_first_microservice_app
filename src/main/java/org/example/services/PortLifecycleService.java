package org.example.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.connectionPool.AnyPoolService;
import org.example.services.connectionPool.ComDataCollector;
import org.example.utilites.properties.MyProperties;
import org.springframework.stereotype.Service;

import java.net.ConnectException;

@Service
@Slf4j
@RequiredArgsConstructor
public class PortLifecycleService {

    private final AnyPoolService anyPoolService;
    private final MyProperties properties;
    private final ConnectionSettingsService connectionSettingsService;
    private final MainLeftPanelStateCollection stateCollection;

    @Data
    public static class PortOpenResult {
        private final String message;
        private final boolean success;
        private final String portSystemName;
    }

    @Data
    public static class PortStatus {
        private final boolean portInUse;
        private final int owningTab;
        private final boolean ownedByCurrentTab;
        private final String warningMessage;
    }

    public PortOpenResult openPort(int clientId, int comIndex, int protocolIndex) {
        connectionSettingsService.setComPortComboNumber(clientId, comIndex);

        ComDataCollector collector = anyPoolService.findComDataCollectorByClientId(clientId);
        String message;
        if (collector == null) {
            try {
                message = anyPoolService.createOrUpdateComDataCollector(
                        stateCollection, clientId, comIndex, protocolIndex, false, false, 3000);
            } catch (ConnectException e) {
                message = "Ошибка при открытии порта для клиента " + clientId + ". " + e.getMessage();
                return new PortOpenResult(message, false, null);
            }
        } else {
            message = collector.reopenPort(clientId, stateCollection);
        }

        ComDataCollector updated = anyPoolService.findComDataCollectorByClientId(clientId);
        String portName = updated != null ? updated.getComPort().getSystemPortName() : null;

        log.info("Port opened for client {}: {}", clientId, message);
        return new PortOpenResult(message, true, portName);
    }

    public String closePort(int clientId) {
        ComDataCollector collector = anyPoolService.findComDataCollectorByClientId(clientId);
        if (collector == null) {
            return "Попытка закрыть ком-порт у несуществующего потока опроса";
        }
        String message;
        try {
            message = collector.closePort(clientId);
        } catch (ConnectException e) {
            message = e.getMessage();
        }
        anyPoolService.shutDownComDataCollectorsThreadByClientId(clientId);
        log.info("Port closed for client {}: {}", clientId, message);
        return message;
    }

    public PortStatus checkPortStatus(int comPortIndex, int currentTab) {
        anyPoolService.closeUnusedComConnection(stateCollection.getSize());
        boolean inUse = anyPoolService.isComPortInUse(comPortIndex);
        int rootTab = anyPoolService.getRootTabForComConnection(comPortIndex);

        boolean ownedByCurrentTab = rootTab == currentTab;
        String warning = null;
        if (inUse && !ownedByCurrentTab && rootTab >= 0) {
            warning = "Управление выбранным ком-портом возможно на вкладке 'dev" + (rootTab + 1) + "'";
            log.info(warning + " Просматриваемая вкладка " + currentTab);
        }

        return new PortStatus(inUse, rootTab, ownedByCurrentTab, warning);
    }

    public void savePortForTab(String portSystemName, int tabNumber) {
        properties.setPortForTab(portSystemName, tabNumber);
    }
}
