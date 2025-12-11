package org.example.gui.mainWindowUtilites;

import org.apache.log4j.Logger;
import org.example.device.ProtocolsList;
import org.example.gui.MainLeftPanelState;
import org.example.gui.MainLeftPanelStateCollection;
import org.example.services.comPort.BaudRatesList;
import org.example.services.comPort.DataBitsList;
import org.example.services.comPort.StopBitsList;
import org.example.utilites.properties.MyPropertiesSettingsLoader;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GuiStateManager {
    private static final Logger log = Logger.getLogger(GuiStateManager.class);
    private final MainLeftPanelStateCollection state;
    private final AtomicInteger currentTab;

    public GuiStateManager(MainLeftPanelStateCollection state,
                           AtomicInteger currentTab) {
        this.state = state;
        this.currentTab = currentTab;
    }

    public void updateGuiFromModel(JComboBox<Integer> dataBits,
                                   JComboBox<String> parity,
                                   JComboBox<Integer> stopBit,
                                   JComboBox<String> baudRate,
                                   JComboBox<String> protocol,
                                   JTextField command,
                                   JTextField prefix,
                                   JTextField devName) {
        int clientId = ensureClientId();
        dataBits.setSelectedIndex(state.getDataBits(clientId));
        parity.setSelectedIndex(state.getParityBits(clientId));
        stopBit.setSelectedIndex(state.getStopBits(clientId));
        baudRate.setSelectedIndex(state.getBaudRate(clientId));
        protocol.setSelectedIndex(state.getProtocol(clientId));
        command.setText(state.getCommand(clientId));
        prefix.setText(state.getPrefix(clientId));
        devName.setText(state.getDevName(clientId));
    }

    public void updateModelFromGui(JComboBox<String> parity,
                                   JComboBox<Integer> dataBits,
                                   JComboBox<Integer> stopBit,
                                   JComboBox<String> baudRate,
                                   JComboBox<String> protocol,
                                   JTextField command,
                                   JTextField prefix,
                                   JTextField devName) {
        int clientId = ensureClientId();
        state.setParityBits(clientId, parity.getSelectedIndex());
        state.setParityBitsValue(clientId, parity.getSelectedIndex());

        state.setDataBits(clientId, dataBits.getSelectedIndex());
        state.setDataBitsValue(clientId, DataBitsList.getNameLikeArray(dataBits.getSelectedIndex()));

        state.setStopBits(clientId, stopBit.getSelectedIndex());
        state.setStopBitsValue(clientId, StopBitsList.getNameLikeArray(stopBit.getSelectedIndex()));

        state.setBaudRate(clientId, baudRate.getSelectedIndex());
        state.setBaudRateValue(clientId, BaudRatesList.getNameLikeArray(baudRate.getSelectedIndex()));

        state.setProtocol(clientId, protocol.getSelectedIndex());

        state.setCommandToSend(clientId, command.getText());

        state.setPrefixToSend(clientId, prefix.getText());

        state.setDevName(clientId, devName.getText());
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
