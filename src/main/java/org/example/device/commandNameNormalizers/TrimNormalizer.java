package org.example.device.commandNameNormalizers;

public class TrimNormalizer implements DeviceCommandNameNormalizer {

    @Override
    public String normalize(String commandName) {
        return commandName.trim();
    }
}
