package org.example.services.comPool;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.ProtocolsList;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerSaverLogger;
import org.example.services.AnswerStorage;
import org.example.services.comPort.ComPort;
import org.example.utilites.properties.MyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;



/*
Отвечает за хранение, остановку, запуск и синхронизацию разных сервисов опроса.
Сейчас это сервисы опроса ComPoolService (старое название класса PoolService)
 */

@Service
public class AnyPoolService {
    private final ExecutorService thPool = Executors.newCachedThreadPool();

    @Getter
    private final ArrayList <ComDataCollector> comDataCollectors = new ArrayList<>();
    private final Logger log = Logger.getLogger(AnyPoolService.class);
    private final ComPort comPort;
    private MyProperties properties;
    @Getter
    private final AnswerSaverLogger answerSaverLogger;

    @Autowired
    public AnyPoolService(ComPort comPort, MyProperties properties1) {
        this.comPort = comPort;
        this.properties = properties1;
        answerSaverLogger = new AnswerSaverLogger(properties);
    }

    public String createOrUpdateComDataCollector(MainLeftPanelStateCollection state, int clientId, int selectedComPort, int selectedProtocol, boolean pool, boolean isBtn, int poolDelay) throws ConnectException {
        if(state.getTabNumberByClientId(clientId) == -1){
            log.error(" В AnyPoolService передан ИД клиента, для которого нет сохранённого состояния");
            return " В AnyPoolService передан ИД клиента, для которого нет сохранённого состояния";
        }
        ComDataCollector psSearch = findComDataCollector(clientId, selectedComPort);


        if (psSearch != null) {
            log.info("Изменение существующего потока. Отправка префикса "  + state.getPrefix(clientId) + " и команды " + state.getCommand(clientId));
            processExistingComDataCollector(state, psSearch, clientId, pool, isBtn, poolDelay);
            return " Добавление вкладки к существующему потоку ";

        } else {
            log.info("Создание нового потока");
            createNewComDataCollector(state, clientId, pool, isBtn, poolDelay, selectedComPort, selectedProtocol);
            return " Был создан новый поток.(Порт " + findComDataCollectorByClientId(clientId).getComPort().getSystemPortName() + " ранее был закрыт) ";
        }
    }

    private void handleTabInExistingCollector(ComDataCollector psSearch, int clientId, MainLeftPanelStateCollection state, boolean pool, boolean isBtn, int poolDelay) {
        if (pool || isBtn) {
            if (isBtn) {
                //log.info("Разовая отправка");
                psSearch.sendOnce(state.getPrefix(clientId), state.getCommand(clientId), clientId, false);
            } else {
                log.info("Команда к запуску");
                psSearch.setNeedPool(clientId, true);
                psSearch.setPoolDelay(poolDelay);
                psSearch.setTextToSendString(state.getPrefix(clientId), state.getCommand(clientId), clientId);
                //thPool.submit(psSearch);
                sleepFor(60);
            }
        } else {
            log.info("Команда к остановке опроса");
            psSearch.setNeedPool(clientId, false);
        }
    }


    private void processExistingComDataCollector(MainLeftPanelStateCollection state, ComDataCollector psSearch, int clientId, boolean pool, boolean isBtn, int poolDelay) {
        //log.info("Порт уже используется, проверка среди запущенных потоков");
        if (psSearch.containClientId(clientId)) {
            log.info("Клинет уже содержится в потоке отправка префикса и команды");
            handleTabInExistingCollector(psSearch, clientId, state, pool, isBtn, poolDelay);
        } else {
            log.info("Клинет не содержится в потоке");
            addDeviceToCollector(psSearch, clientId, state, isBtn, pool);
        }
    }

    private void createNewComDataCollector(MainLeftPanelStateCollection state, int clientId, boolean pool, boolean isBtn, int poolDelay, int comPortNumber, int protocolIndex) throws ConnectException {
        log.info("Порт не используется, создание нового потока");

        ComPort avaComPorts = new ComPort();
        avaComPorts.setPort(comPortNumber);
        ProtocolsList protocol = ProtocolsList.getLikeArrayEnum(protocolIndex);
        try {
            ComDataCollector toAdd = new ComDataCollector(state, protocol, state.getPrefix(clientId), state.getCommand(clientId), avaComPorts.activePort, poolDelay, false, clientId, this);
            this.addComDataCollector(toAdd);
            setupNewCollector(clientId, pool, isBtn, state);
        }catch (ConnectException exp){
            throw new ConnectException("Ошибка в AnyPoolService при создании ComDataCollector " + exp.getMessage());
        }

    }

    private void setupNewCollector(int clientId, boolean pool, boolean isBtn, MainLeftPanelStateCollection state) {
        ComDataCollector lastAdded = getComDataCollectors().get(getComDataCollectors().size() - 1);

        if (isBtn) {
            lastAdded.setNeedPool(clientId, false);
            lastAdded.sendOnce(state.getPrefix(clientId), state.getCommand(clientId), clientId, false);
            log.info("Поток создан и запущен один раз");
            sleepFor(60);
        } else if (pool) {
            lastAdded.setNeedPool(clientId, true);
            log.info("Параметры добавленного потока установлены в режим с опросом");
        } else {
            lastAdded.setNeedPool(clientId, false);
            log.info("Поток создан и запущен для Event (без опроса)");
        }
    }

    private void addDeviceToCollector(ComDataCollector psSearch, int clientId, MainLeftPanelStateCollection state, boolean isBtn, boolean pool) {
        if (!isBtn) {
            log.info("Для текущей вкладки устройство не существует в потоке опроса (по чек-боксу)");
            psSearch.addDeviceToService(clientId, state.getPrefix(clientId), state.getCommand(clientId), false, true);
        } else {
            log.info("Для текущей вкладки устройство не существует в потоке опроса (по кнопке)");
            psSearch.addDeviceToService(clientId, state.getPrefix(clientId), state.getCommand(clientId), false, false);
        }
    }



    public void addComDataCollector(ComDataCollector comPoolService) {
        comDataCollectors.add(comPoolService);
        thPool.submit(comPoolService);
        log.info("Количество потоков опроса теперь" + getCurrentComConnectionsQuantity());
        log.info("Количество клиентов опроса теперь" + getCurrentComClientsQuantity());
    }


    public ComDataCollector findComDataCollectorByClientId(int clientId) {
        for (ComDataCollector comDataCollector : comDataCollectors) {
            if (comDataCollector.containClientId(clientId)) {
                return comDataCollector;
            }
        }
        return null;
    }

    public ComDataCollector findComDataCollectorByOpenedPort(int portNumber) {
        for (ComDataCollector comDataCollector : comDataCollectors) {
            if (comDataCollector.getComPortForJCombo() == portNumber) {
                return comDataCollector;
            }
        }
        return null;
    }

    public ComDataCollector findComDataCollector(int tab, int comNumber) {
        ComDataCollector psSearch = this.findComDataCollectorByClientId(tab);
        if (psSearch == null) {
            psSearch = findComDataCollectorByOpenedPort(comNumber);
            if(psSearch != null) {
                log.info("Найден сервис опроса по КОМ-ПОРТУ");
            }else{
                log.info("Сервис опроса не найден ни для вкладки, ни для ком-порта");
            }
        }else {
            //log.info("Найден сервис опроса по ВКЛАДКЕ");
        }
        return psSearch;
    }


    public boolean isComDataCollectorByClientIdActiveDataSurvey(int clientId) {
        ComDataCollector ps = this.findComDataCollectorByClientId(clientId);
        if (ps != null) {
            return ps.isNeedPool(clientId);
        }
        return false;
    }

    public boolean isComDataCollectorByClientIdLogged(int clientId) {
        ComDataCollector ps = this.findComDataCollectorByClientId(clientId);
        if (ps != null) {
            return ps.isNeedLog(clientId);
        }
        return false;
    }

    public int getCurrentComClientsQuantity(){
        int counter = 0;
        for (ComDataCollector comDataCollector : comDataCollectors) {
            counter = counter + comDataCollector.getClientsCount();
        }
        return counter;
    }

    public int getCurrentComConnectionsQuantity(){
        return comDataCollectors.size();
    }


    public boolean isComPortInUse(int portNumber) {
        for (int j = 0; j < this.getComDataCollectors().size(); j++) {
            if (this.getComDataCollectors().get(j) != null &&
                    this.getComDataCollectors().get(j).getComPort() != null &&
                    this.getComDataCollectors().get(j).getComPort().isOpen() &&
                    this.getComDataCollectors().get(j).getComPortForJCombo() == portNumber) {
                return true;
            }
        }
        return false;
    }

    public int getRootTabForComConnection(int portNumber) {
        if(portNumber < 0){
            log.info("В поиск корневой вкладки был передан неправильный номер порта ");
            return -1;
        }
        if(comPort.getAllPorts().size() < portNumber) { //Номер больше чем количество портов
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " В системе порта нету ");
            return -1;
        }
        if(comPort.getAllPorts().get(portNumber).isOpen()) { //Если порт закрыт, то  не ищу его вкладку
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " В системе этот " + comPort.getAllPorts().get(portNumber).getSystemPortName() + " Порт закрыт.");
            return -1;
        }


        int forReturnFix = -1;
        for (int i = 0; i < getCurrentComClientsQuantity(); i++) { //Перебираю всех клиентов
            int j = 0;
             //Если среди всех поисков она была найдена только как клиент, то пусть будет управляющей чем никакой
            for (ComDataCollector comDataCollector : comDataCollectors) {
                //log.info("  Просматриваю позицию " + j + " в ArrayList comDataCollectors ");
                if (comDataCollector.getComPort() != null) {//Не виртуальный опрос
                    //log.info("      В просматриваемом comDataCollector существует ком-порт");
                    if (comDataCollector.getComPortForJCombo() == portNumber) {//Выбранный порт был выбран ранее
                        //log.info("          В просматриваемом comDataCollector ком-порт по номеру совпал с требуемым");

                        if (comDataCollector.isRootThread( i)) {//Просматриваемая вкладка корневая для опроса
                            //log.info("              В просматриваемом comDataCollector проверяемый клиент (вкладка) " + i + " является КОРНЕВОЙ");
                            //rootTab = i;
                            //log.info("Нашел корневую");
                            //rootTab = i;
                            return j;
                        } else if (comDataCollector.containClientId(i)) {//просматриваемая вкладка виртуальная, но содержится
                            //log.info("              В просматриваемом comDataCollector проверяемый клиент (вкладка) " + i + " просто найдена");
                            forReturnFix = j;
                        }
                    }else{
                        //log.info("          В просматриваемом comDataCollector ком-порт по номеру не совпал с требуемым");
                    }
                }
                j++;
            }
            if(forReturnFix != -1){
                return forReturnFix;
            }
            //log.info("  Завершен просмотр всех comDataCollector  в ArrayList");
        }
        return forReturnFix;
    }

    public int searchComPortNumberByName(String name) {
        return IntStream.range(0, comPort.getAllPorts().size())
                .filter(i -> comPort.getAllPorts().get(i).getSystemPortName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(-1);
    }

    public void updateComPortList(){
        comPort.updatePorts();

    }
    public void closeUnusedComConnection(int quantityTabs){

        //Если в ArrayList больше объектов, чем открыто вкладок - позакрывать лишнее
        for (int i = 0; i < comDataCollectors.size(); i++) {
            if (i > quantityTabs) {
                int pointer = comDataCollectors.size() - 1;
                comDataCollectors.get(pointer).getComPort().closePort();
                comDataCollectors.get(pointer).removeDeviceFromComDataCollector(pointer);
                AnswerStorage.removeAnswersForTab(pointer);
                comDataCollectors.remove(pointer);
            }
        }
    }

    public void shutdownEmptyComDataCollectorThreads() {
        for (ComDataCollector comDataCollector : comDataCollectors) {
            if(comDataCollector.isEmpty()){
                comDataCollector.shutdown();
            }
        }
    }

    public void shutDownComDataCollectorsThreadByClientId(int clientId) {
        ComDataCollector ps = this.findComDataCollectorByClientId(clientId);
        if(ps != null) {
            ps.removeDeviceFromComDataCollector(clientId);
            ps.shutdown();
            for (int i = 0; i < comDataCollectors.size(); i++) {
                if(comDataCollectors.get(i) != null && comDataCollectors.get(i).isAlive() == false){
                    comDataCollectors.remove(i);
                    i--;
                }
            }
        }else{
            log.error("Попытка остановки потока с неассоциированного клиента (вкладки)");
        }

    }

    public void shutDownComDataCollectorThreadPool() {
        thPool.shutdownNow();
    }

    private void sleepFor(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            //Thread.currentThread().interrupt();
            log.warn("Проблема при засыпании после создания потока опроса", e);
        }
    }

}
