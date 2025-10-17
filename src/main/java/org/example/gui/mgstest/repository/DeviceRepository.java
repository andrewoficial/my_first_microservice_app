// DeviceManager.java
package org.example.gui.mgstest.repository;

import lombok.Getter;
import org.apache.log4j.Logger;
import org.example.gui.mgstest.model.HidSupportedDevice;
import org.example.utilites.Constants;
import org.hid4java.HidDevice;
import org.hid4java.HidServices;
import org.hid4java.HidManager;

import java.util.*;

import static org.example.utilites.Constants.HidCommunication.MIKROSENSE_TARGET_PRODUCT_ID;
import static org.example.utilites.Constants.HidCommunication.MULTIGASSENSE_TARGET_PRODUCT_ID;

public class DeviceRepository {

    private final HidServices hidServices;
    private final DeviceRepositoryInterface stateRepository;
    private final Logger log = Logger.getLogger(DeviceRepository.class);
    @Getter
    private HashSet<HidSupportedDevice> deviceList = new HashSet<>(3);


    public DeviceRepository(DeviceRepositoryInterface stateRepository) {
        this.hidServices = HidManager.getHidServices();
        this.stateRepository = stateRepository;
    }

    public void updateDeviceList() {
        //deviceList.clear();(ломает ссылки)
        for (HidSupportedDevice supportedDevice : deviceList) {
            supportedDevice.setAlive(false);
        }
        List<HidDevice> devices = hidServices.getAttachedHidDevices();
        hidServices.stop();

        for (HidDevice device : devices) {
            HidSupportedDevice supportedDeviceForAdd = new HidSupportedDevice(device, device.getPath(),  Constants.SupportedHidDeviceType.UNKNOWN);
            if (device != null && device.getProductId() == MULTIGASSENSE_TARGET_PRODUCT_ID) {
                supportedDeviceForAdd.setDeviceType(Constants.SupportedHidDeviceType.MULTIGASSENSE);
            } else if (device != null && device.getProductId() == MIKROSENSE_TARGET_PRODUCT_ID) {
                supportedDeviceForAdd.setDeviceType(Constants.SupportedHidDeviceType.MIKROSENSE);
            }
            supportedDeviceForAdd.setAlive(true);
            if( ! deviceList.contains(supportedDeviceForAdd)){
                if(supportedDeviceForAdd.getDeviceType() == Constants.SupportedHidDeviceType.UNKNOWN) {
                    log.debug("Неизвестное устройство " + supportedDeviceForAdd.getHidDevice().getSerialNumber() + " " + supportedDeviceForAdd.getHidDevice().getPath());
                }else{
                    log.info("Добавляю прибор " + supportedDeviceForAdd.getHidDevice());
                    deviceList.add(supportedDeviceForAdd);
                }

            }else{
                //log.info("Сбрасываю флаг неактивности " + supportedDeviceForAdd.getHidDevice().getSerialNumber());
                getDeviceByHidObject(supportedDeviceForAdd.getHidDevice()).setAlive(true);
            }

        }

        for (HidSupportedDevice supportedDevice : deviceList) {
            if(!supportedDevice.isAlive()){
                supportedDevice.setDisplayName(" [*] " + createName(supportedDevice));
            }else{
                supportedDevice.setDisplayName(createName(supportedDevice));
            }
        }
    }

    private String createName(HidSupportedDevice dev) {
        if(!stateRepository.contains(dev)){
            log.debug("Для прибора не найдено состояние");
            return dev.getHidDevice().getSerialNumber();
        }else if( stateRepository.get(dev) == null){
            log.debug("Для прибора найдено состояние null");
            return dev.getHidDevice().getSerialNumber();
        }else if( stateRepository.get(dev).getDeviceInfo() == null){
            log.debug("Для прибора getDeviceInfo null");
            return dev.getHidDevice().getSerialNumber();
        }else if( stateRepository.get(dev).getDeviceInfo() == null){
            log.debug("Для прибора getDeviceInfo null");
            return dev.getHidDevice().getSerialNumber();
        }else if(!stateRepository.get(dev).getDeviceInfo().isLoaded()){
            log.debug("Для прибора isLoaded false");
            return dev.getHidDevice().getSerialNumber();
        }else if(stateRepository.get(dev).getDeviceInfo().getSerialNumber() == 0){
            log.debug("Для прибора getSerialNumber == 0");
            return dev.getHidDevice().getSerialNumber();
        }else{
            log.debug("Нашел имя для устновки");
            return String.valueOf(stateRepository.get(dev).getDeviceInfo().getSerialNumber());
        }

    }
    public HidSupportedDevice getDeviceBySerialNumber(String displayName) {
        for (HidSupportedDevice supportedDevice : deviceList) {
            if(supportedDevice.getDisplayName().equalsIgnoreCase(displayName)) {
                return supportedDevice;
            }
        }
        throw new IllegalStateException("Device with serial number " + displayName + " not found");
    }

    public HidSupportedDevice getDeviceByHidObject(HidDevice hidDevice) {
        for (HidSupportedDevice supportedDevice : deviceList) {
            if(supportedDevice.getHidDevice().getPath().equals(hidDevice.getPath())) {
                return supportedDevice;
            }
        }
        throw new IllegalStateException("Device specified by HID object " + hidDevice.getSerialNumber() + " not found");
    }

 }