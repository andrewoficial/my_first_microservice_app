package org.example.services.comPool;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.device.ProtocolsList;
import org.example.services.AnswerSaverLogger;
import org.example.services.AnswerStorage;
import org.example.services.comPort.ComPort;
import org.example.utilites.properties.MyProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;



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

    public void createOrUpdateComDataCollector(int tab, int selectedComPort, int selectedProtocol, boolean pool, boolean isBtn, int poolDelay, String[] prefixAndCmd) {
        ComDataCollector psSearch = findComDataCollector(tab, selectedComPort);
        if(prefixAndCmd == null){
            prefixAndCmd = new String[2];
            prefixAndCmd [0] = "";
            prefixAndCmd [1] = "";
        }

        if (psSearch != null) {
            log.info("Изменение существующего потока");
            processExistingComDataCollector(psSearch, tab, prefixAndCmd, pool, isBtn, poolDelay);
        } else {
            log.info("Создание нового потока");
            createNewComDataCollector(tab, pool, isBtn, poolDelay, prefixAndCmd, selectedComPort, selectedProtocol);
        }
    }

    private void handleTabInExistingCollector(ComDataCollector psSearch, int tab, String [] prefixAndCmd, boolean pool, boolean isBtn, int poolDelay) {
        if (pool || isBtn) {
            if (isBtn) {
                log.info("Разовая отправка");
                psSearch.sendOnce(prefixAndCmd[0], prefixAndCmd[1], tab, false);
            } else {
                log.info("Команда к запуску");
                psSearch.setNeedPool(tab, true);
                psSearch.setPoolDelay(poolDelay);
                psSearch.setTextToSendString(prefixAndCmd[0], prefixAndCmd[1], tab);
                sleepFor(60);
            }
        } else {
            log.info("Команда к остановке опроса");
            psSearch.setNeedPool(tab, false);
            if (psSearch.isRootTab(tab)) {
                log.info("Текущий поток является корневым для других");
            } else {
                log.info("Вкладка одинока. Но поток не будет завершен. Ожидание входящих сообщений.");
            }
        }
    }


    private void processExistingComDataCollector(ComDataCollector psSearch, int tab, String [] prefixAndCmd, boolean pool, boolean isBtn, int poolDelay) {
        //log.info("Порт уже используется, проверка среди запущенных потоков");
        if (psSearch.containTabDev(tab)) {
            log.info("Клинет уже содержится в потоке");
            handleTabInExistingCollector(psSearch, tab, prefixAndCmd, pool, isBtn, poolDelay);
        } else {
            log.info("Клинет не содержится в потоке");
            addDeviceToCollector(psSearch, tab, prefixAndCmd, isBtn, pool);
        }
    }

    private void createNewComDataCollector(int tab, boolean pool, boolean isBtn, int poolDelay, String [] prefixAndCmd , int comPortNumber, int protocolIndex) {
        log.info("Порт не используется, создание нового потока");
        boolean forEvent = isBtn ? true : !pool;
        log.info("соединение будет обрабатывать event события => " + forEvent);

        ComPort avaComPorts = new ComPort();
        avaComPorts.setPort(comPortNumber);
        ProtocolsList protocol = ProtocolsList.getLikeArrayEnum(protocolIndex);
        ComDataCollector toAdd = new ComDataCollector(protocol,prefixAndCmd[0],prefixAndCmd[1],avaComPorts.activePort,poolDelay,false,forEvent,tab, this);

        this.addComDataCollector(toAdd);

        setupNewCollector(tab, pool, isBtn, prefixAndCmd);
    }

    private void setupNewCollector(int tab, boolean pool, boolean isBtn, String[] prefixAndCmd) {
        ComDataCollector lastAdded = getComDataCollectors().get(getComDataCollectors().size() - 1);

        if (isBtn) {
            lastAdded.setNeedPool(tab, false);
            lastAdded.sendOnce(prefixAndCmd[0], prefixAndCmd[1], tab, false);
            log.info("Поток создан и запущен один раз");
            sleepFor(60);
        } else if (pool) {
            lastAdded.setNeedPool(tab, true);
            log.info("Параметры добавленного потока установлены в режим с опросом");
        } else {
            lastAdded.setNeedPool(tab, false);
            log.info("Поток создан и запущен для Event (без опроса)");
        }
    }

    private void addDeviceToCollector(ComDataCollector psSearch, int tab, String[] prefixAndCmd, boolean isBtn, boolean pool) {
        if (!isBtn) {
            log.info("Для текущей вкладки устройство не существует в потоке опроса (по чек-боксу)");
            psSearch.addDeviceToService(tab, prefixAndCmd[0], prefixAndCmd [1], false, true);
        } else {
            log.info("Для текущей вкладки устройство не существует в потоке опроса (по кнопке)");
            psSearch.addDeviceToService(tab, prefixAndCmd[0], prefixAndCmd [1], false, false);
        }
    }



    public void addComDataCollector(ComDataCollector comPoolService) {
        comDataCollectors.add(comPoolService);
        thPool.submit(comPoolService);
        log.info("Количество потоков опроса теперь" + getCurrentComConnectionsQuantity());
        log.info("Количество клиентов опроса теперь" + getCurrentComClientsQuantity());
    }


    public ComDataCollector findComDataCollectorByTabNumber(int number) {
        for (ComDataCollector comDataCollector : comDataCollectors) {
            if (comDataCollector.containTabDev(number)) {
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
        ComDataCollector psSearch = this.findComDataCollectorByTabNumber(tab);
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


    public boolean isComDataCollectorByTabNumberActiveDataSurvey(int number) {
        ComDataCollector ps = this.findComDataCollectorByTabNumber(number);
        if (ps != null) {
            return ps.isNeedPool(number);
        }
        return false;
    }

    public boolean isComDataCollectorByTabNumberLogged(int number) {
        ComDataCollector ps = this.findComDataCollectorByTabNumber(number);
        if (ps != null) {
            return ps.isNeedLog(number);
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
        if(comPort.getAllPorts().size() > portNumber) {
            //log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " это порт:" + comPort.getAllPorts().get(portNumber).getSystemPortName());
        }else{
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " Не может быть проверен. Такого порта нет в системе.");
            return -1;
        }
        if(comPort.getAllPorts().size() > portNumber) { //Если порт закрыт, то  не ищу его вкладку
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " В системе порта нету ");
            return -1;
        }
        if(comPort.getAllPorts().get(portNumber).isOpen()) { //Если порт закрыт, то  не ищу его вкладку
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " В системе этот " + comPort.getAllPorts().get(portNumber).getSystemPortName() + " Порт закрыт.");
            return -1;
        }
        int forReturnFix = -1;
        for (int i = 0; i < getCurrentComClientsQuantity(); i++) { //Перебираю всех клиентов
            //log.info("Просматриваю клиента " + i);
            int j = 0;
             //Если среди всех поисков она была найдена только как клиент, то пусть будет управляющей чем никакой
            for (ComDataCollector comDataCollector : comDataCollectors) {
                //log.info("  Просматриваю позицию " + j + " в ArrayList comDataCollectors ");
                if (comDataCollector.getComPort() != null) {//Не виртуальный опрос
                    //log.info("      В просматриваемом comDataCollector существует ком-порт");
                    if (comDataCollector.getComPortForJCombo() == portNumber) {//Выбранный порт был выбран ранее
                        //log.info("          В просматриваемом comDataCollector ком-порт по номеру совпал с требуемым");
                        if (comDataCollector.isRootTab(i)) {//Просматриваемая вкладка корневая для опроса
                            //log.info("              В просматриваемом comDataCollector проверяемый клиент (вкладка) " + i + " является КОРНЕВОЙ");
                            //rootTab = i;
                            //log.info("Нашел корневую");
                            //rootTab = i;
                            return j;
                        } else if (comDataCollector.containTabDev(i)) {//просматриваемая вкладка виртуальная, но содержится
                            //log.info("              В просматриваемом comDataCollector проверяемый клиент (вкладка) " + i + " просто найдена");
                            forReturnFix = j;
                        }
                    }else{
                        //log.info("          В просматриваемом comDataCollector ком-порт по номеру не совпал с требуемым");
                    }
                }else{
                    //log.info("      В просматриваемом comDataCollector не существует ком-порт");
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

    public void shutDownComDataCollectorsThreadByTab(int number) {
        ComDataCollector ps = this.findComDataCollectorByTabNumber(number);
        if(ps != null) {
            ps.removeDeviceFromComDataCollector(number);
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
