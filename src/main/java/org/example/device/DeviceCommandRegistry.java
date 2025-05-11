package org.example.device;

public abstract class DeviceCommandRegistry {
    protected final DeviceCommandListClass commandList = new DeviceCommandListClass();
    
    public DeviceCommandListClass getCommandList() {
        return commandList;
    }
    
    protected abstract void initCommands();
    
    public DeviceCommandRegistry() {
        initCommands();
    }
}