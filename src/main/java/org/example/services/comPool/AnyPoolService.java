package org.example.services.comPool;

import lombok.Getter;
import org.example.services.AnswerStorage;
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



    public void addComDataCollector(ComDataCollector comPoolService) {
        comDataCollectors.add(comPoolService);
        thPool.submit(comPoolService);
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
            if (comDataCollector.containTabDev(portNumber)) {
                return comDataCollector;
            }
        }
        return null;
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

    public void closeUnusedComConnection(int quantityTabs){

        //Если в ArrayList больше объектов, чем открыто вкладок - позакрывать лишнее
        for (int i = 0; i < comDataCollectors.size(); i++) {
            if (i > quantityTabs) {
                int pointer = comDataCollectors.size() - 1;
                comDataCollectors.get(pointer).getComPort().closePort();
                comDataCollectors.get(pointer).removeDeviceToService(pointer);
                AnswerStorage.removeAnswersForTab(pointer);
                comDataCollectors.remove(pointer);
            }
        }
    }


    public void shutDownComDataCollectorsThread(int number) {
        if(comDataCollectors.size() > number) {
            comDataCollectors.get(number).removeDeviceToService(number);
            //poolServices.get(number).setNeedPool(number, false);
            comDataCollectors.remove(number);
        }

        if(comDataCollectors.size() > number) {
            comDataCollectors.remove(number);
        }
    }

    public void shutDownComDataCollectorThreadPool() {
        thPool.shutdownNow();
    }



}
