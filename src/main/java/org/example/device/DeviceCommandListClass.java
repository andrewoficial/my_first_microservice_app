package org.example.device;

import lombok.Getter;
import org.example.device.command.SingleCommand;
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
        if (originalName == null) {
            return null;
        }
        SingleCommand command = commandPool.get(normalize(originalName));
        if (command != null) {
            return command;
        }
        // Многодроповые ASCII-приборы шлют "<адрес><команда>", а команды в реестре хранятся
        // без адреса. Если по полному имени не нашли — пробуем отбросить ведущий адрес:
        // "001M^" (3 цифры, напр. Erstevak/Thyracont) или "@01CRDG? 1" ('@' + цифры, напр. ECT_TC290).
        String trimmed = originalName.trim();
        if (trimmed.matches("\\d{3}.+")) {
            command = commandPool.get(normalize(trimmed.substring(3)));
        }
        if (command == null && trimmed.matches("@\\d{1,3}.+")) {
            int i = 1;
            while (i < trimmed.length() && Character.isDigit(trimmed.charAt(i))) {
                i++;
            }
            command = commandPool.get(normalize(trimmed.substring(i)));
        }
        return command;
    }

    private String normalize(String commandName) {
        String normalized = commandName;
        for (DeviceCommandNameNormalizer normalizer : normalizers) {
            if (normalized == null) {
                return null;
            }
            normalized = normalizer.normalize(normalized);
        }
        return normalized;
    }

    public void addCommand(SingleCommand command){
        commandPool.put(command.getMapKey(), command);
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
