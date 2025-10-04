// RepositoryFactory.java или в вашем основном классе приложения
package org.example.gui.mgstest.repository;

import lombok.Setter;

public class RepositoryFactory {
    // Для тестирования - возможность подменить реализацию
    @Setter
    private static DeviceRepository instance;
    
    public static DeviceRepository getDeviceRepository() {
        if (instance == null) {
            instance = new DeviceStateRepository();
        }
        return instance;
    }

}