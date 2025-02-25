package org.example.gui.mainWindowUtilites;

import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.comPool.AnyPoolService;
import org.example.services.comPool.ComDataCollector;
import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.ParityList;
import org.example.services.comPort.StopBitsList;
import org.example.utilites.properties.MyProperties;

import java.net.ConnectException;

public class PortManager {
    private final AnyPoolService anyPoolService;
    private final MyProperties properties;
    private final GuiStateManager guiStateManager;
    private final MainLeftPanelStateCollection leftPanelStateCollection;

    public PortManager(AnyPoolService anyPoolService,
                       MyProperties properties,
                       GuiStateManager guiStateManager, MainLeftPanelStateCollection leftPanelStateCollection) {
        this.anyPoolService = anyPoolService;
        this.properties = properties;
        this.guiStateManager = guiStateManager;
        this.leftPanelStateCollection = leftPanelStateCollection;
    }

    public String openPort(int clientId, int comIndex, int protocolIndex) {
        ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(clientId);
        String resultCreating = "";
        if (ps == null) {
            try {
                resultCreating = anyPoolService.createOrUpdateComDataCollector(leftPanelStateCollection, clientId, comIndex, protocolIndex, false, false, 3000);
            } catch (ConnectException exc) {
                resultCreating = "Ошибка при открытии порта для клиента " + clientId + ". Поток не создан. Проброс сообщения: " + exc;
            }
        } else {
            resultCreating = ps.reopenPort(clientId, leftPanelStateCollection);
        }
        return resultCreating;
    }

    public String closePort(int clientId) {
        ComDataCollector ps = anyPoolService.findComDataCollectorByClientId(clientId);
        String resultCreating = "";
        if (ps != null) {
            try {
                resultCreating = ps.closePort(clientId);
            } catch (ConnectException exception) {
                resultCreating = exception.getMessage();
            }
            anyPoolService.shutDownComDataCollectorsThreadByClientId(clientId);
        } else {
            resultCreating = "Попытка закрыть ком-порт у несуществующего потока опроса";
        }
        return resultCreating;
    }
}