package org.example.device;

import lombok.Getter;
import org.example.device.commandNameNormalizers.DeviceCommandNameNormalizer;
import org.example.device.commandNameNormalizers.PrefixCutNormalizer;
import org.example.device.commandNameNormalizers.TrimNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceCommandListClass {
    @Getter
    private final HashMap <String, SingleCommand> commandPool = new HashMap<>();
    private final List<DeviceCommandNameNormalizer> normalizers = new ArrayList<>();

    public DeviceCommandListClass() {
        addDefaultNormalizers();
    }

    private void addDefaultNormalizers() {
        // Добавляем стандартные нормализаторы
        normalizers.add(new TrimNormalizer());
        normalizers.add(new PrefixCutNormalizer("CRDG", 6));
        normalizers.add(new PrefixCutNormalizer("M^", 3));
        normalizers.add(new PrefixCutNormalizer("V0091", 6));
    }



    public SingleCommand getCommand(String originalName) {
        String normalized = originalName;
        for (DeviceCommandNameNormalizer normalizer : normalizers) {
            normalized = normalizer.normalize(normalized);
        }
        return commandPool.get(normalized);
    }

    public void addCommand(SingleCommand command){
        commandPool.put(command.getName(), command);
    }



    public boolean isKnownCommand(String name){

        if(name == null){
            return false;
        }
        return getCommand(name) != null;
    }

    public int getExpectedBytes(String name){

        if(getCommand(name) == null){
            return 500000;
        }
        return getCommand(name).getExpectedBytes();
    }

}
