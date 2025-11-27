// org/example/gui/autoresponder/ConfigManager.java
package org.example.gui.autoresponder;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "./config/dps_emulator_config.properties";

    public static void loadConfig(DpsEmulatorWindow window) {
        Properties prop = new Properties();
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            try (InputStream input = new FileInputStream(file)) {
                prop.load(input);
                window.jtfModel.setText(prop.getProperty("model", "DPS-150"));
                window.jtfSwVersion.setText(prop.getProperty("swVersion", "V1.1"));
                window.jtfHwVersion.setText(prop.getProperty("hwVersion", "V1.0"));
                window.jtfVoltageSet.setText(prop.getProperty("voltageSet", "4.0"));
                window.jtfCurrentSet.setText(prop.getProperty("currentSet", "1.234"));
                window.jtfTemperature.setText(prop.getProperty("temperature", "24.0"));
                window.jtfStatus.setText(prop.getProperty("status", "1"));
                window.jtfBrightness.setText(prop.getProperty("brightness", "10"));
                LogUtil.logMessage(window.jtaLog, "Config loaded");
            } catch (IOException ex) {
                DpsEmulatorWindow.log.error("Error loading config", ex);
            }
        }
    }

    public static void saveConfig(DpsEmulatorWindow window) {
        Properties prop = new Properties();
        prop.setProperty("model", window.jtfModel.getText());
        prop.setProperty("swVersion", window.jtfSwVersion.getText());
        prop.setProperty("hwVersion", window.jtfHwVersion.getText());
        prop.setProperty("voltageSet", window.jtfVoltageSet.getText());
        prop.setProperty("currentSet", window.jtfCurrentSet.getText());
        prop.setProperty("temperature", window.jtfTemperature.getText());
        prop.setProperty("status", window.jtfStatus.getText());
        prop.setProperty("brightness", window.jtfBrightness.getText());

        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            prop.store(output, null);
            LogUtil.logMessage(window.jtaLog, "Config saved");
        } catch (IOException io) {
            DpsEmulatorWindow.log.error("Error saving config", io);
        }
    }
}