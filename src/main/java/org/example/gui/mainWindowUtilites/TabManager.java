package org.example.gui.mainWindowUtilites;

import org.example.services.TabService;
import org.example.gui.MainLeftPanelStateCollection;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TabManager {
    private final JTabbedPane tabbedPane;
    private final MainLeftPanelStateCollection stateCollection;
    private final TabService tabService;
    private final Map<Integer, JTextPane> logPanels;
    private final Map<Integer, Integer> positions;
    private final AtomicInteger tabCounter = new AtomicInteger();
    private final JButton button_RemoveDev;

    public TabManager(JTabbedPane pane,
                     MainLeftPanelStateCollection stateCollection,
                     TabService tabService,
                     Map<Integer, JTextPane> logPanels,
                     Map<Integer, Integer> positions,
                      JButton button_RemoveDev) {
        this.tabbedPane = pane;
        this.stateCollection = stateCollection;
        this.tabService = tabService;
        this.logPanels = logPanels;
        this.positions = positions;
        this.button_RemoveDev = button_RemoveDev;
    }

    public void addTab() {
        int newTabIndex = tabbedPane.getTabCount();
        TabService.TabInfo info = tabService.addTab(newTabIndex);

        JTextPane logPanel = createLogPanel(info.getClientId());
        JPanel tabPanel = createTabPanel(logPanel);
        initializeMaps(info.getClientId(), logPanel);

        tabbedPane.addTab("dev" + (newTabIndex + 1), tabPanel);
        updateUIAfterAdd(newTabIndex);
    }

    public void removeTab() {
        int tabToRemove = tabbedPane.getSelectedIndex();
        if (tabToRemove < 0) return;

        Integer clientId = tabService.getClientIdByTab(tabToRemove);
        if (clientId == null) return;

        tabService.removeTab(tabToRemove);
        tabbedPane.removeTabAt(tabToRemove);

        // Обновление GUI-маппингов
        positions.remove(clientId);
        logPanels.remove(clientId);

        Map<Integer, Integer> newMappings = tabService.rebuildTabMappings(tabToRemove, tabbedPane.getTabCount());
        Map<Integer, JTextPane> updatedLogPanels = new HashMap<>();
        newMappings.forEach((cid, newTab) -> {
            JTextPane pane = logPanels.get(cid);
            if (pane != null) {
                updatedLogPanels.put(cid, pane);
            }
        });
        logPanels.clear();
        logPanels.putAll(updatedLogPanels);

        updateUIAfterRemove();
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
