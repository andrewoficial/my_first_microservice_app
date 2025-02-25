package org.example.gui.mainWindowUtilites;

import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.AnswerStorage;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TabManager {
    private final JTabbedPane tabbedPane;
    private final MainLeftPanelStateCollection stateCollection;
    private final Map<Integer, JTextPane> logPanels;
    private final Map<Integer, Integer> positions;
    private final AtomicInteger tabCounter = new AtomicInteger();
    private final JButton button_RemoveDev;

    public TabManager(JTabbedPane pane, 
                     MainLeftPanelStateCollection stateCollection,
                     Map<Integer, JTextPane> logPanels,
                     Map<Integer, Integer> positions,
                      JButton button_RemoveDev) {
        this.tabbedPane = pane;
        this.stateCollection = stateCollection;
        this.logPanels = logPanels;
        this.positions = positions;
        this.button_RemoveDev = button_RemoveDev;
    }

    public void addTab() {
        int newTabIndex = tabbedPane.getTabCount();
        int newClientId = stateCollection.getClientIdByTabNumber(newTabIndex);
        if (newClientId == -1) {
            newClientId = stateCollection.getNewRandomId();
            stateCollection.addPairClientIdTabNumber(newClientId, newTabIndex);
            MainLeftPanelState state = new MainLeftPanelState();
            state.setTabNumber(newTabIndex);
            state.setClientId(newClientId);
            stateCollection.addOrUpdateIdState(newClientId, state);
        }
        // Создание компонентов вкладки
        JTextPane logPanel = createLogPanel(newClientId);
        JPanel tabPanel = createTabPanel(logPanel);

        initializeMaps(newClientId, logPanel);

        // Добавление вкладки
        tabbedPane.addTab("dev" + (newTabIndex + 1), tabPanel);
        updateUIAfterAdd(newTabIndex);
    }

    public void removeTab() {
        int tabToRemove = tabbedPane.getSelectedIndex();
        if (tabToRemove < 0) return;

        Integer clientId = stateCollection.getClientIdByTabNumber(tabToRemove);
        if (clientId == null) return;

        // Удаление данных
        removeClientData(clientId);
        tabbedPane.removeTabAt(tabToRemove);

        // Перестройка маппингов
        rebuildMappings(tabToRemove);
        updateUIAfterRemove();
    }

    // вспомогательные для removeTab
    private void removeClientData(int clientId) {
        stateCollection.removeEntryByClientId(clientId);
        positions.remove(clientId);
        logPanels.remove(clientId);
        AnswerStorage.removeAnswersForTab(clientId);
    }


    private void rebuildMappings(int removedTab) {
        Map<Integer, Integer> clientIdToOldTab = new LinkedHashMap<>();
        Map<Integer, JTextPane> updatedLogPanelsMap = new HashMap<>();

        for (int i = 0; i < tabbedPane.getTabCount() + 1; i++) {
            Integer clientId = stateCollection.getClientIdByTabNumber(i);
            if (clientId != null && clientId != -1) {
                clientIdToOldTab.put(clientId, i);
            }
        }

        clientIdToOldTab.forEach((clientId, oldTab) -> {
            if (oldTab < removedTab) {
                stateCollection.addOrUpdateClientIdTabNumber(clientId, oldTab);
                updatedLogPanelsMap.put(clientId, logPanels.get(clientId));
            } else if (oldTab > removedTab) {
                stateCollection.addOrUpdateClientIdTabNumber(clientId, oldTab - 1);
                updatedLogPanelsMap.put(clientId, logPanels.get(clientId));
            }
        });

        logPanels.clear();
        logPanels.putAll(updatedLogPanelsMap);
    }

    private void updateUIAfterRemove() {
        tabCounter.set(tabbedPane.getTabCount());
        button_RemoveDev.setEnabled(tabCounter.get() > 0);
        updateTabTitles();
    }

    public void updateTabTitles() {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            tabbedPane.setTitleAt(i, "dev" + (i + 1));
        }
    }
    // вспомогательные для removeTab конец

    private void updateUIAfterAdd(int tabIndex) {
        tabCounter.set(tabbedPane.getTabCount());
        button_RemoveDev.setEnabled(true);
        tabbedPane.setSelectedIndex(tabIndex);
        updateTabTitles();
    }

    private JTextPane createLogPanel(int clientId) {
        JTextPane panel = new JTextPane();
        panel.setText("Лог для клиента " + clientId + "\n");
        return panel;
    }

    private JPanel createTabPanel(JTextPane logPanel) {
        JScrollPane scrollPane = new JScrollPane(logPanel);
        scrollPane.setPreferredSize(new Dimension(400, 400));

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void initializeMaps(int clientId, JTextPane panel) {
        positions.put(clientId, 0);
        logPanels.put(clientId, panel);
    }

}