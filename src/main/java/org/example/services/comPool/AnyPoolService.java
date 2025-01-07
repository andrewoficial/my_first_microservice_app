package org.example.services.comPool;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.services.AnswerStorage;
import org.example.services.comPort.ComPort;
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
    //ToDo подумать: когда сервис будет передаваться спрингом, есть вероятность что будет создаваться новый класс. Нужно что бы этого не происходило. Каждый раз передавать существующий
    @Getter
    private final ArrayList <ComDataCollector> comDataCollectors = new ArrayList<>();
    private final Logger log = Logger.getLogger(AnyPoolService.class);
    private final ComPort comPort;

    public AnyPoolService(ComPort comPort) {
        this.comPort = comPort;
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
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " это порт:" + comPort.getAllPorts().get(portNumber).getSystemPortName());
        }else{
            log.info("Начинаю поиск корневой вкладки для порта номер: " + portNumber + " Не может быть проверен. Такого порта нет в системе.");
            return -1;
        }
        int forReturnFix = -1;
        for (int i = 0; i < getCurrentComClientsQuantity(); i++) { //Перебираю всех клиентов
            log.info("Просматриваю клиента " + i);
            int j = 0;
             //Если среди всех поисков она была найдена только как клиент, то пусть будет управляющей чем никакой
            for (ComDataCollector comDataCollector : comDataCollectors) {
                log.info("  Просматриваю позицию " + j + " в ArrayList comDataCollectors ");
                if (comDataCollector.getComPort() != null) {//Не виртуальный опрос
                    log.info("      В просматриваемом comDataCollector существует ком-порт");
                    if (comDataCollector.getComPortForJCombo() == portNumber) {//Выбранный порт был выбран ранее
                        log.info("          В просматриваемом comDataCollector ком-порт по номеру совпал с требуемым");
                        if (comDataCollector.isRootTab(i)) {//Просматриваемая вкладка корневая для опроса
                            log.info("              В просматриваемом comDataCollector проверяемый клиент (вкладка) " + i + " является КОРНЕВОЙ");
                            //rootTab = i;
                            //log.info("Нашел корневую");
                            //rootTab = i;
                            return j;
                        } else if (comDataCollector.containTabDev(i)) {//просматриваемая вкладка виртуальная, но содержится
                            log.info("              В просматриваемом comDataCollector проверяемый клиент (вкладка) " + i + " просто найдена");
                            forReturnFix = j;
                        }
                    }else{
                        log.info("          В просматриваемом comDataCollector ком-порт по номеру не совпал с требуемым");
                    }
                }else{
                    log.info("      В просматриваемом comDataCollector не существует ком-порт");
                }
                j++;
            }
            if(forReturnFix != -1){
                return forReturnFix;
            }
            log.info("  Завершен просмотр всех comDataCollector  в ArrayList");
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
        }else{
            log.error("Попытка остановки потока с неассоциированного клиента (вкладки)");
        }

    }

    public void shutDownComDataCollectorThreadPool() {
        thPool.shutdownNow();
    }



}
