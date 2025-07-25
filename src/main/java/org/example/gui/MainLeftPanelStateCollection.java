package org.example.gui;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MainLeftPanelStateCollection {
    private final ConcurrentHashMap<Integer, Integer> clientIdTab = new ConcurrentHashMap<>(); // <RandomID, TabNumber>
    private final ConcurrentHashMap<Integer, MainLeftPanelState> clientIdTabState = new ConcurrentHashMap<>();


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
        if (clientId != null && clientId >= 0 && getTabNumberByClientId(clientId) != -1) {
            clientIdTab.remove(clientId);
            clientIdTabState.remove(clientId);
            return true;
        }
        return false;
    }
    public boolean addOrUpdateIdState(Integer clientId, MainLeftPanelState paneState){
        if (clientId != null && paneState != null && clientId >= 0) {
            clientIdTabState.put(clientId, paneState);
            System.out.println(" Выполнил clientIdTabState.put(clientId, paneState)" + clientId + " paneState tab " + paneState.getTabNumber() + " paneState id " + paneState.getClientId());
            return true;
        }
        return false;
    }
    public Integer getTabNumberByClientId(Integer clientId){
        if (clientId != null && clientId >= 0 && !clientIdTab.isEmpty()) {
            for (Map.Entry<Integer, Integer> integerIntegerEntry : clientIdTab.entrySet()) {
                if (Objects.equals(integerIntegerEntry.getKey(), clientId)) {
                    return integerIntegerEntry.getKey();
                }
            }
        }
        return -1;
    }

    public Integer getClientIdByTabNumber(Integer tabNumber){
        if (tabNumber != null && tabNumber >= 0 && !clientIdTab.isEmpty()) {
            for (Map.Entry<Integer, Integer> integerIntegerEntry : clientIdTab.entrySet()) {
                if (Objects.equals(integerIntegerEntry.getValue(), tabNumber)) {
                    return integerIntegerEntry.getKey();
                }
            }
        }
        System.out.println("Не найдена связка вкладка/клиент для tabNumber [" + tabNumber + "] clientIdTab.isEmpty()" + clientIdTab.isEmpty() + " clientIdTab" + clientIdTab.size());
        return -1;
    }

    public boolean containClientId(Integer clientId){
        if (clientId != null && clientId >= 0 && !clientIdTab.isEmpty()) {
            return clientIdTab.containsKey(clientId);
        }
        return false;
    }


    public boolean addPairClientIdTabNumber(Integer clientId, Integer tabNumber){
        if (clientId != null && tabNumber != null && clientId >= 0 && tabNumber >= 0) {
            if(getTabNumberByClientId(clientId) == -1 && getClientIdByTabNumber(tabNumber) == -1){
                clientIdTab.put(clientId, tabNumber);
                System.out.println("Выполнил  clientIdTab.put(clientId, tabNumber) ");
                return true;
            }else{
                System.out.println("Уже существует ключ или вкладка");
            }
        }
        return false;
    }

    public boolean addOrUpdateClientIdTabNumber(Integer clientId, Integer tabNumber){
        if (clientId != null && tabNumber != null && clientId >= 0 && tabNumber >= 0) {
            if(getTabNumberByClientId(clientId) == -1 && getClientIdByTabNumber(tabNumber) == -1){
                clientIdTab.put(clientId, tabNumber);
                clientIdTabState.putIfAbsent(clientId, new MainLeftPanelState());
                return true;
            }else{
                if(getClientIdByTabNumber(tabNumber) != -1){
                    clientIdTab.remove(getClientIdByTabNumber(tabNumber));
                }
                if(getTabNumberByClientId(clientId) != -1){
                    clientIdTab.remove(getTabNumberByClientId(clientId));
                }
                clientIdTab.put(clientId, tabNumber);
                clientIdTabState.putIfAbsent(clientId, new MainLeftPanelState());
                return true;
            }
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
    public void setProtocol(int clientId, int state) {
        MainLeftPanelState stateObj = clientIdTabState.getOrDefault(clientId, null);
        if (stateObj == null) {
            throw new IndexOutOfBoundsException("Для клиента " + clientId + " не найдено состояние панели");
        }
        stateObj.setProtocol(state);
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


}
