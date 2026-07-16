package org.example.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.transport.serial.BaudRatesList;
import org.example.services.transport.serial.DataBitsList;
import org.example.services.transport.serial.StopBitsList;
import org.example.utilites.properties.MyProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionSettingsService {

    private final MainLeftPanelStateCollection stateCollection;
    private final MyProperties properties;

    // === State creation ===

    public int ensureClientId(int tabNumber) {
        int clientId = stateCollection.findClientIdByTabNumber(tabNumber);
        if (clientId == -1) {
            clientId = stateCollection.getNewRandomId();
            stateCollection.addPairClientIdTabNumber(clientId, tabNumber);
            MainLeftPanelState state = new MainLeftPanelState();
            state.setTabNumber(tabNumber);
            state.setClientId(clientId);
            stateCollection.addOrUpdateIdState(clientId, state);
            log.info("ensureClientId: новая вкладка tabNumber={} → clientId={}", tabNumber, clientId);
        }
        return clientId;
    }

    // === Connection settings ===

    public void updateBaudRate(int clientId, int baudRateIndex) {
        stateCollection.setBaudRate(clientId, baudRateIndex);
        stateCollection.setBaudRateValue(clientId, BaudRatesList.getNameLikeArray(baudRateIndex));
    }

    public void updateParity(int clientId, int parityIndex) {
        stateCollection.setParityBits(clientId, parityIndex);
        stateCollection.setParityBitsValue(clientId, parityIndex);
    }

    public void updateStopBits(int clientId, int stopBitsIndex) {
        stateCollection.setStopBits(clientId, stopBitsIndex);
        stateCollection.setStopBitsValue(clientId, StopBitsList.getNameLikeArray(stopBitsIndex));
    }

    public void updateDataBits(int clientId, int dataBitsIndex) {
        stateCollection.setDataBits(clientId, dataBitsIndex);
        stateCollection.setDataBitsValue(clientId, DataBitsList.getNameLikeArray(dataBitsIndex));
    }

    public void updateProtocol(int clientId, int protocolIndex) {
        stateCollection.setProtocol(clientId, protocolIndex);
    }

    // === Device name ===

    public void setDeviceName(int clientId, String name) {
        stateCollection.setDevName(clientId, name);
    }

    public String getDeviceName(int clientId) {
        return stateCollection.getDevName(clientId);
    }

    // === State queries ===

    public int getBaudRate(int clientId) {
        return stateCollection.getBaudRate(clientId);
    }

    public int getParity(int clientId) {
        return stateCollection.getParityBits(clientId);
    }

    public int getStopBits(int clientId) {
        return stateCollection.getStopBits(clientId);
    }

    public int getDataBits(int clientId) {
        return stateCollection.getDataBits(clientId);
    }

    public int getProtocol(int clientId) {
        return stateCollection.getProtocol(clientId);
    }

    public int getClientIdByTab(int tabNumber) {
        return stateCollection.getClientIdByTabNumber(tabNumber);
    }

    public int getComPortComboNumber(int clientId) {
        return stateCollection.getComPortComboNumber(clientId);
    }

    public void setComPortComboNumber(int clientId, int portIndex) {
        stateCollection.setComPortComboNumber(clientId, portIndex);
    }

    public String getCommand(int clientId) {
        return stateCollection.getCommand(clientId);
    }

    public String getPrefix(int clientId) {
        return stateCollection.getPrefix(clientId);
    }

    public void setCommand(int clientId, String command) {
        stateCollection.setCommandToSend(clientId, command);
    }

    public void setPrefix(int clientId, String prefix) {
        stateCollection.setPrefixToSend(clientId, prefix);
    }

    // === Persistence ===

    public void saveState() {
        properties.setLastLeftPanel(stateCollection);
    }

    public MainLeftPanelStateCollection getRestoredState() {
        MainLeftPanelStateCollection restored = properties.getLeftPanelStateCollection();
        if (restored == null) {
            log.warn("В настройках нет состояний. Создаю с нуля");
            return null;
        }
        return restored;
    }
}
