// DeviceAnswerParser.java
package org.example.gui.mgstest.service;

import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.gui.mgstest.model.answer.*;
import org.example.gui.mgstest.model.DeviceState;
import org.example.gui.mgstest.parser.answer.mgs.*;
import org.example.gui.mgstest.parser.answer.mkrs.AdvancedResponseParserMKRS;
import org.example.gui.mgstest.parser.answer.mkrs.GetDeviceInfoParserMkrs;
import org.example.gui.mgstest.repository.DeviceStateRepository;
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
    public void parseByName(byte[] data, HidCommandName commandName, HidSupportedDevice device) throws Exception {
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
        }else if(HidCommandName.GET_V_RANGE == commandName){
            parseVRange(data, device);
        }else if(HidCommandName.GET_ALARMS == commandName){
            parseAlarms(data, device);
        }else if(HidCommandName.GET_GAS_RANGE == commandName){
            parseGasRange(data, device);
        }else if(HidCommandName.GET_SENS_STATUS == commandName){
            parseSensStatus(data, device);
        }else if(HidCommandName.MKRS_GET_INFO == commandName){
            parseGetDevInfoMkrs(data, device);
        }else if(HidCommandName.MKRS_SEND_UART == commandName){
            parseUartAnswerMKRS(data, device);
        }

    }
    public void parseUartAnswerMKRS(byte[] rawData, HidSupportedDevice device) throws Exception {
        isInputEmpty(rawData);

        AdvancedResponseParserMKRS parser = new AdvancedResponseParserMKRS();
        String answers = parser.parseMkrsResponse(rawData);

        MipexResponseModel responseModel = new MipexResponseModel(System.currentTimeMillis(), answers);
        stateRepository.get(device).setLastMipexResponse(responseModel);
        updateDeviceState(device, state -> state.setLastMipexResponse(responseModel));
    }
    public void parseUartAnswer(byte[] rawData, HidSupportedDevice device) throws Exception {
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
    public void parseSensStatus(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device sens status");
        GetSensStatusModel status = GetSensStatusParser.parse(deviceInfoRaw);
        stateRepository.get(device).setSensStatusModel(status);
        updateDeviceState(device, state -> state.setSensStatusModel(status));
    }
    public void parseGasRange(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device gas range");
        GetGasRangeModel settings = GetGasRangeParser.parse(deviceInfoRaw);
        stateRepository.get(device).setGasRangeModel(settings);
        updateDeviceState(device, state -> state.setGasRangeModel(settings));
    }
    public void parseDeviceSettings(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device settings data");
        GetAllSettingsModel settings = GetAllSettingsParser.parse(deviceInfoRaw);
        stateRepository.get(device).setAllSettings(settings);
        updateDeviceState(device, state -> state.setAllSettings(settings));
    }
    public void parseVRange(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing vRange");
        GetVRangeModel vRangeModel = GetVRangeParser.parse(deviceInfoRaw);
        if(vRangeModel == null){
            log.error("vRange is null after parsing");
        }
        stateRepository.get(device).setVRangeModel(vRangeModel);
        updateDeviceState(device, state -> state.setVRangeModel(vRangeModel));
    }

    public void parseAlarms(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing alarms");
        GetAlarmsModel vRangeModel = GetAlarmsParser.parse(deviceInfoRaw);

        stateRepository.get(device).setAlarmsModel(vRangeModel);
        updateDeviceState(device, state -> state.setAlarmsModel(vRangeModel));
    }

    public void parseDeviceInfo(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device info data");
        GetDeviceInfoModel info = GetDeviceInfoParser.parse(deviceInfoRaw);
        stateRepository.get(device).setDeviceInfo(info);
        updateDeviceState(device, state -> state.setDeviceInfo(info));
    }

    public void parseGetDevInfoMkrs(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing device info data");
        GetDeviceInfoModel info = GetDeviceInfoParserMkrs.parse(deviceInfoRaw);
        stateRepository.get(device).setDeviceInfo(info);
        updateDeviceState(device, state -> state.setDeviceInfo(info));
    }

    public void parseAllCoefficients(byte[] deviceInfoRaw, HidSupportedDevice device) throws Exception {
        isInputEmpty(deviceInfoRaw);
        log.info("Started parsing all coefficients data");
        GetAllCoefficientsModel info = GetAllCoefficientsParser.parseAllCoef(deviceInfoRaw);
        stateRepository.get(device).setAllCoefficients(info);
        updateDeviceState(device, state -> state.setAllCoefficients(info));
    }

    public void setCoefficientsO2(HidSupportedDevice device) throws Exception {
        log.info("Executing setCoefficientsO2 command");
        cradleController.setCoefficientsO2(device);
        log.info("Successfully set O2 coefficients");
    }
    
    public void setCoefficientsCO(HidSupportedDevice device) throws Exception {
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

    private void isInputEmpty(byte[] data) throws Exception {
        if (data == null) {
            throw new Exception("Failed to get device info - null response");
        }
    }
    @FunctionalInterface
    private interface StateUpdater {
        void update(DeviceState state);
    }

    private void updateDeviceState(HidSupportedDevice device, StateUpdater updater) {
        DeviceState state = stateRepository.get(device);
        if (state == null) {
            state = new DeviceState();
            stateRepository.put(device, state);
            updater.update(state);
            log.info("Try update NULL device state");
        }else{
            stateRepository.put(device, state);
            updater.update(state);
            log.info("Try update not null device state");
        }

    }
    

}