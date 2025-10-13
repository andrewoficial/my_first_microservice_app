// DeviceAnswerParser.java
package org.example.gui.mgstest.service;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.answer.*;
import org.example.gui.mgstest.parser.answer.AdvancedResponseParser;
import org.example.gui.mgstest.parser.answer.GetAllCoefficientsParser;
import org.example.gui.mgstest.parser.answer.GetAllSettingsParser;
import org.example.gui.mgstest.parser.answer.GetDeviceInfoParser;
import org.example.gui.mgstest.repository.DeviceState;
import org.example.gui.mgstest.repository.DeviceStateRepository;
import org.example.gui.mgstest.transport.CommandParameters;
import org.example.gui.mgstest.transport.CradleController;
import org.example.gui.mgstest.transport.HidCommandName;
import org.hid4java.HidDevice;

import java.util.List;

public class DeviceAnswerParser {
    private static final Logger log = Logger.getLogger(DeviceAnswerParser.class);
    
    private final CradleController cradleController;
    private final DeviceStateRepository stateRepository;
    
    public DeviceAnswerParser(CradleController cradleController, DeviceStateRepository stateRepository) {
        this.cradleController = cradleController;
        this.stateRepository = stateRepository;
    }

    //Пишу с нарушением  open/closed из-за нехватки времени
    public void parseByName(byte[] data, HidCommandName commandName, HidDevice device) throws Exception {
        if(commandName == null ){
            return;
        }

        if(HidCommandName.GET_COEFF == commandName){
            parseAllCoefficients(data, device);
        }else if(HidCommandName.GET_DEV_INFO == commandName){
            parseDeviceInfo(data, device);
        }else if(HidCommandName.SENT_URT == commandName || HidCommandName.SENT_EXTERNAL_URT == commandName || HidCommandName.SENT_SPI == commandName){
            parseUartAnswer(data, device);
        }else if(HidCommandName.SET_ALARM_STATE == commandName){
            stateRepository.get(device).getDeviceInfo().setAlarmEnabled(true);
            updateDeviceState(device, state -> state.setDeviceInfo(stateRepository.get(device).getDeviceInfo()));
        }else if(HidCommandName.GET_SETTINGS == commandName){
            parseDeviceSettings(data, device);
        }

    }

    public void parseUartAnswer(byte[] rawData, HidDevice device) throws Exception {
        isInputEmpty(rawData);
        AdvancedResponseParser parser = new AdvancedResponseParser();
        List<String> answers = parser.extractAllTextResponses(rawData);
        log.info("Найденные ответы:");
        for (String answer : answers) {
            System.out.println(answer);
        }
        String blocks = parser.parseMipexResponse(rawData);
        log.info("Строка, найденная кошерным методом:" + blocks);
        MipexResponseModel responseModel = new MipexResponseModel(System.currentTimeMillis(), blocks);
        stateRepository.get(device).setLastMipexResponse(responseModel);
        updateDeviceState(device, state -> state.setLastMipexResponse(responseModel));
    }
    public void parseDeviceSettings(byte[] deviceInfoRaw, HidDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device settings data");
        GetAllSettingsModel settings = GetAllSettingsParser.parse(deviceInfoRaw);
        stateRepository.get(device).setAllSettings(settings);
        updateDeviceState(device, state -> state.setAllSettings(settings));
    }

    public void parseDeviceInfo(byte[] deviceInfoRaw, HidDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device info data");
        GetDeviceInfoModel info = GetDeviceInfoParser.parse(deviceInfoRaw);
        stateRepository.get(device).setDeviceInfo(info);
        updateDeviceState(device, state -> state.setDeviceInfo(info));
    }

    public void parseAllCoefficients(byte[] deviceInfoRaw, HidDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing all coefficients data");
        GetAllCoefficientsModel info = GetAllCoefficientsParser.parseAllCoef(deviceInfoRaw);
        stateRepository.get(device).setAllCoefficients(info);
        updateDeviceState(device, state -> state.setAllCoefficients(info));
    }

    public void setCoefficientsO2(HidDevice device) throws Exception {
        log.info("Executing setCoefficientsO2 command");
        cradleController.setCoefficientsO2(device);
        log.info("Successfully set O2 coefficients");
    }
    
    public void setCoefficientsCO(HidDevice device) throws Exception {
        log.info("Executing setCoefficientsCO command");
        cradleController.setCoefficientsCO(device);
        log.info("Successfully set CO coefficients");
    }
    
    public void setSerialNumber(HidDevice device, long serialNumber) throws Exception {
        log.info("Executing setSerialNumber command: " + serialNumber);
        //cradleController.setSerialNumber(device, serialNumber);
        log.info("Successfully set serial number");
    }

    

    
    public long parseSerialNumber(String input) throws NumberFormatException {
        return Long.parseLong(input);
    }

    private boolean isInputEmpty(byte[] data) throws Exception {
        if (data == null) {
            throw new Exception("Failed to get device info - null response");
        }
        return false;
    }
    @FunctionalInterface
    private interface StateUpdater {
        void update(DeviceState state);
    }

    private void updateDeviceState(HidDevice device, StateUpdater updater) {
        DeviceState state = stateRepository.get(device);
        if (state == null) {
            state = new DeviceState();
            stateRepository.put(device, state);
            updater.update(state);
        }else{
            log.warn("Try update null device state");
        }

    }
    

}