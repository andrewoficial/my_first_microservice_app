package org.example.gui;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.example.services.connection.ConnectionType;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class MainLeftPanelStateCollection {
    private static volatile MainLeftPanelStateCollection instance = null;
    private final ConcurrentHashMap<Integer, Integer> clientIdTab = new ConcurrentHashMap<>(); // <RandomID, TabNumber>
    @Getter
    private final ConcurrentHashMap<Integer, MainLeftPanelState> clientIdTabState = new ConcurrentHashMap<>();

    private MainLeftPanelStateCollection() {
        // Защита от создания через Reflection
        if (instance != null) {
            throw new IllegalStateException("Instance already created");
        }
    }
    public static MainLeftPanelStateCollection getInstance() {
        MainLeftPanelStateCollection localInstance = instance;
        if (localInstance == null) {
            synchronized (MainLeftPanelStateCollection.class) {
                localInstance = instance;
                if (localInstance == null) {
                    instance = localInstance = new MainLeftPanelStateCollection();
                }
            }
        }
        return localInstance;
    }

    public static void renewInstance(MainLeftPanelStateCollection leftPanState) {
        MainLeftPanelStateCollection.instance = leftPanState;
    }

    public boolean isCollectionEmpty(){
        return clientIdTab.isEmpty() || clientIdTabState.isEmpty();
    }

    public int getSize(){
        return Math.max(clientIdTab.size(), clientIdTabState.size());
    }

    public void clearCollection(){
        clientIdTabState.clear();
        clientIdTab.clear();
    }

    public ArrayList <MainLeftPanelState> getIdTabStateAsList(){
        ArrayList<MainLeftPanelState> forReturn = new ArrayList<>();
        for (Map.Entry<Integer, MainLeftPanelState> entry : clientIdTabState.entrySet()) {
            entry.getValue().setClientId(entry.getKey());
            entry.getValue().setTabNumber(entry.getValue().getTabNumber());
            forReturn.add(entry.getValue());
        }
        return forReturn;
    }
    public boolean removeEntryByClientId(Integer clientId){
        if (clientId != null && clientId >= 0 && findTabNumberByClientId(clientId) != -1) {
            clientIdTab.remove(clientId);
            clientIdTabState.remove(clientId);
            return true;
        }
        return false;
    }
    public boolean addOrUpdateIdState(Integer clientId, MainLeftPanelState paneState){
        if (clientId != null && paneState != null && clientId >= 0) {
            clientIdTabState.put(clientId, paneState);
            log.debug("Обновлён state clientId={} tabNumber={}", clientId, paneState.getTabNumber());
            return true;
        }
        return false;
    }

    /**
     * Silent clientId → tabNumber. Returns -1 if missing.
     * Prefer this for existence checks (no WARN/noise).
     */
    public int findTabNumberByClientId(Integer clientId) {
        if (clientId == null || clientId < 0) {
            return -1;
        }
        Integer tab = clientIdTab.get(clientId);
        return tab != null ? tab : -1;
    }

    /**
     * Silent tabNumber → clientId. Returns -1 if missing.
     * Prefer this for existence checks during load/create (no WARN/noise).
     */
    public int findClientIdByTabNumber(Integer tabNumber) {
        if (tabNumber == null || tabNumber < 0 || clientIdTab.isEmpty()) {
            return -1;
        }
        for (Map.Entry<Integer, Integer> entry : clientIdTab.entrySet()) {
            if (Objects.equals(entry.getValue(), tabNumber)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * clientId → tabNumber. Miss is often normal (new tab); logged at DEBUG only.
     * For probes use {@link #findTabNumberByClientId(Integer)}.
     */
    public Integer getTabNumberByClientId(Integer clientId) {
        int tab = findTabNumberByClientId(clientId);
        if (tab == -1) {
            log.debug("Связка client→tab не найдена (часто ок при создании): clientId={}, mapSize={}",
                    clientId, clientIdTab.size());
        }
        return tab;
    }

    /**
     * tabNumber → clientId. Miss is often normal (new tab); logged at DEBUG only.
     * For probes use {@link #findClientIdByTabNumber(Integer)}.
     * <p>
     * Note: callers that previously saw two WARNs on one new tab were doing
     * getClientIdByTabNumber (miss) + addPair (miss check again). addPair now uses find*.
     */
    public Integer getClientIdByTabNumber(Integer tabNumber) {
        int clientId = findClientIdByTabNumber(tabNumber);
        if (clientId == -1) {
            log.debug("Связка tab→client не найдена (часто ок при создании): tabNumber={}, mapSize={}",
                    tabNumber, clientIdTab.size());
        }
        return clientId;
    }

    public boolean containClientId(Integer clientId) {
        return clientId != null && clientId >= 0 && clientIdTab.containsKey(clientId);
    }

    public boolean containTabNumber(Integer tabNumber) {
        return findClientIdByTabNumber(tabNumber) != -1;
    }

    /**
     * Registers a new clientId ↔ tabNumber pair. Existence checks are silent (find*);
     * one INFO line is logged only when the pair is actually created.
     */
    public boolean addPairClientIdTabNumber(Integer clientId, Integer tabNumber) {
        if (clientId != null && tabNumber != null && clientId >= 0 && tabNumber >= 0) {
            int existingTab = findTabNumberByClientId(clientId);
            int existingClient = findClientIdByTabNumber(tabNumber);
            if (existingTab == -1 && existingClient == -1) {
                clientIdTab.put(clientId, tabNumber);
                log.info("Создана связка clientId={} ↔ tabNumber={} (mapSize={})",
                        clientId, tabNumber, clientIdTab.size());
                return true;
            }
            log.warn("Связка уже есть: clientId={}→tab={}, tabNumber={}→client={} (запрос clientId={}, tabNumber={})",
                    clientId, existingTab, tabNumber, existingClient, clientId, tabNumber);
        }
        return false;
    }

    public boolean addOrUpdateClientIdTabNumber(Integer clientId, Integer tabNumber) {
        if (clientId != null && tabNumber != null && clientId >= 0 && tabNumber >= 0) {
            if (findTabNumberByClientId(clientId) == -1 && findClientIdByTabNumber(tabNumber) == -1) {
                clientIdTab.put(clientId, tabNumber);
                clientIdTabState.putIfAbsent(clientId, new MainLeftPanelState());
                log.info("Создана связка clientId={} ↔ tabNumber={} (mapSize={})",
                        clientId, tabNumber, clientIdTab.size());
                return true;
            }
            int ownerOfTab = findClientIdByTabNumber(tabNumber);
            if (ownerOfTab != -1) {
                clientIdTab.remove(ownerOfTab);
            }
            if (findTabNumberByClientId(clientId) != -1) {
                clientIdTab.remove(clientId);
            }
            clientIdTab.put(clientId, tabNumber);
            clientIdTabState.putIfAbsent(clientId, new MainLeftPanelState());
            log.debug("Обновлена связка clientId={} ↔ tabNumber={}", clientId, tabNumber);
            return true;
        }
        return false;
    }
    public void setDataBits(int clientId, int state){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        //System.out.println("Для клиента " + clientId + " был вызван setDataBits " + state);
        stateObj.setDataBits(state);
    }

    public void setComPortComboNumber(int clientId, int state){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        //System.out.println("Для клиента " + clientId + " был вызван setDataBits " + state);
        stateObj.setComPortComboNumber(state);
    }

    public void setDataBitsValue(int clientId, int state){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        //System.out.println("Для клиента " + clientId + " был вызван setDataBits " + state);
        stateObj.setDataBitsValue(state);
    }

    public void setClientId(int clientId, int clientIdForSet){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setClientId(clientIdForSet);
    }

    public void setTabNumber(int clientId, int tabNumber){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setClientId(tabNumber);
    }

    public void setParityBits(int clientId, int state){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setParityBit(state);
    }

    public void setParityBitsValue(int clientId, int state){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setParityBitValue(state);
    }

    public void setStopBits(int clientId, int state) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setStopBits(state);
    }

    public void setStopBitsValue(int clientId, int state) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setStopBitsValue(state);
    }



    public void setBaudRate(int clientId, int state) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setBaudRate(state);
    }

    public void setBaudRateValue(int clientId, int state) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setBaudRateValue(state);
    }

    public void setCommandToSend(int clientId, String command) {
        //System.out.println(" Вызвано сохранение строки для отправки " +  clientId + " строка " + command);
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        if(command == null || command.isEmpty()){
            //System.out.println("В сохранение состояния передана пустая команда");
            command = "";
        }
        stateObj.setCommand(command);
    }

    public void setPrefixToSend(int clientId, String prefix) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        if(prefix == null ){
            System.out.println("В сохранение состояния передан пустой префикс");
            prefix = "";
        }
        stateObj.setPrefix(prefix);
    }
    public void setVisibleName(int clientId, String visibleName) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }

        if(visibleName == null ){
            System.out.println("В сохранение состояния передан пустой visibleName");
            visibleName = "";
        }
        stateObj.setVisibleName(visibleName);
    }

    public void setProtocol(int clientId, int state) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setProtocol(state);
    }

    public void setRawCommand(int clientId, byte [] cmd){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setRawCommand(cmd);
    }

    public void setDevName(int clientId, String name){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        if(name == null ){
            System.out.println("В сохранение состояния передано пустое имя клиента");
            name = "";
        }
        stateObj.setVisibleName(name);
    }

    public int getNewRandomId(){
        int candidate = (int) (500000 + Math.random() * 10000 + Math.random() * 10);
        while(clientIdTab.containsKey(candidate)){
            candidate = (int) (500000 + Math.random() * 10000 + Math.random() * 10);
        }
        return candidate;
    }

    public int getParityBits(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getParityBit();
    }

    public int getParityBitsValue(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getParityBitValue();
    }

    public int getDataBits(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        //System.out.println("По запрсу с ИД " + clientId + " внутри найденого объекта данные getClientId " + stateObj.getClientId() + " getTabNumber " + stateObj.getTabNumber() + " getDataBits " + stateObj.getDataBits());
        return stateObj.getDataBits();
    }

    public int getDataBitsValue(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        //System.out.println("По запрсу с ИД " + clientId + " внутри найденого объекта данные getClientId " + stateObj.getClientId() + " getTabNumber " + stateObj.getTabNumber() + " getDataBits " + stateObj.getDataBits());
        return stateObj.getDataBitsValue();
    }

    public int getStopBits(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getStopBits();
    }

    public int getComPortComboNumber(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getComPortComboNumber();
    }

    public int getStopBitsValue(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getStopBitsValue();
    }

    public int getBaudRate(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getBaudRate();
    }

    public int getBaudRateValue(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getBaudRateValue();
    }

    public int getProtocol(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getProtocol();
    }

    public void setConnectionType(int clientId, ConnectionType connectionType) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setConnectionType(connectionType != null ? connectionType : ConnectionType.COM);
    }

    public ConnectionType getConnectionType(int clientId) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        ConnectionType t = stateObj.getConnectionType();
        return t != null ? t : ConnectionType.COM;
    }

    public String getCommand(int clientId){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getCommand();
    }

    public String getPrefix(int clientId){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getPrefix();
    }

    public byte [] getRawCommand(int clientId){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getRawCommand();
    }

    public String getDevName(int clientId){
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        return stateObj.getVisibleName();
    }


}
