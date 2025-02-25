package org.example.gui.mainWindowUtilites;

import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiStateManager {
    private final MainLeftPanelStateCollection state;
    private final AtomicInteger currentClientId;
    private final AtomicInteger currentTab;

    public GuiStateManager(MainLeftPanelStateCollection state,
                           AtomicInteger currentClientId,
                           AtomicInteger currentTab) {
        this.state = state;
        this.currentClientId = currentClientId;
        this.currentTab = currentTab;
    }

    public void updateGuiFromModel(JComboBox<Integer> dataBits,
                                   JComboBox<String> parity,
                                   JComboBox<Integer> stopBit,
                                   JComboBox<String> baudRate,
                                   JComboBox<String> protocol,
                                   JTextField command,
                                   JTextField prefix) {
        int clientId = ensureClientId();
        dataBits.setSelectedIndex(state.getDataBits(clientId));
        parity.setSelectedIndex(state.getParityBits(clientId));
        stopBit.setSelectedIndex(state.getStopBits(clientId));
        baudRate.setSelectedIndex(state.getBaudRate(clientId));
        protocol.setSelectedIndex(state.getProtocol(clientId));
        command.setText(state.getCommand(clientId));
        prefix.setText(state.getPrefix(clientId));
    }

    public void updateModelFromGui(JComboBox<String> parity,
                                   JComboBox<Integer> dataBits,
                                   JComboBox<Integer> stopBit,
                                   JComboBox<String> baudRate,
                                   JComboBox<String> protocol,
                                   JTextField command,
                                   JTextField prefix) {
        int clientId = ensureClientId();
        state.setParityBits(clientId, parity.getSelectedIndex());
        state.setDataBits(clientId, dataBits.getSelectedIndex());
        state.setStopBits(clientId, stopBit.getSelectedIndex());
        state.setBaudRate(clientId, baudRate.getSelectedIndex());
        state.setProtocol(clientId, protocol.getSelectedIndex());
        state.setCommandToSend(clientId, command.getText());
        state.setPrefixToSend(clientId, prefix.getText());
    }

    private int ensureClientId() {
        int clientId = state.getClientIdByTabNumber(currentTab.get());
        if (clientId == -1) {
            clientId = state.getNewRandomId();
            state.addPairClientIdTabNumber(clientId, currentTab.get());
            state.addOrUpdateIdState(clientId, new MainLeftPanelState());
        }
        return clientId;
    }
}
