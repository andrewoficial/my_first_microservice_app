package org.example.services;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TabService {

    private final MainLeftPanelStateCollection stateCollection;
    private final AnswerStorage answerStorage;

    @Data
    public static class TabInfo {
        private final int clientId;
        private final int tabNumber;
    }

    public TabInfo addTab(int tabNumber) {
        // find* — silent probe; miss means new tab (not an error)
        int clientId = stateCollection.findClientIdByTabNumber(tabNumber);
        if (clientId == -1) {
            clientId = stateCollection.getNewRandomId();
            stateCollection.addPairClientIdTabNumber(clientId, tabNumber);
            MainLeftPanelState state = new MainLeftPanelState();
            state.setTabNumber(tabNumber);
            state.setClientId(clientId);
            stateCollection.addOrUpdateIdState(clientId, state);
        }
        answerStorage.addInTabsList(clientId);
        log.info("Вкладка добавлена: tabNumber={}, clientId={}", tabNumber, clientId);
        return new TabInfo(clientId, tabNumber);
    }

    public Integer getClientIdByTab(int tabNumber) {
        return stateCollection.getClientIdByTabNumber(tabNumber);
    }

    public void removeTab(int tabNumber) {
        Integer clientId = stateCollection.getClientIdByTabNumber(tabNumber);
        if (clientId == null) return;

        stateCollection.removeEntryByClientId(clientId);
        answerStorage.removeInTabsList(clientId);
        answerStorage.removeAnswersForTab(clientId);
        log.info("Вкладка удалена: tabNumber={}, clientId={}", tabNumber, clientId);
    }

    public Map<Integer, Integer> rebuildTabMappings(int removedTab, int totalTabsAfterRemove) {
        Map<Integer, Integer> oldToNew = new LinkedHashMap<>();

        for (int i = 0; i < totalTabsAfterRemove + 1; i++) {
            Integer clientId = stateCollection.getClientIdByTabNumber(i);
            if (clientId != null && clientId != -1) {
                int newTab = (i < removedTab) ? i : i - 1;
                if (newTab >= 0) {
                    stateCollection.addOrUpdateClientIdTabNumber(clientId, newTab);
                    oldToNew.put(clientId, newTab);
                }
            }
        }
        return oldToNew;
    }

    public int getStateSize() {
        return stateCollection.getSize();
    }

    public TabAnswerPart getTabData(int tabNumber, int lastPosition, boolean showCommands) {
        Integer clientId = stateCollection.getClientIdByTabNumber(tabNumber);
        if (clientId == null || clientId == -1) {
            return new TabAnswerPart("", lastPosition);
        }
        return answerStorage.getAnswersQueForWeb(lastPosition, clientId, showCommands);
    }
}
