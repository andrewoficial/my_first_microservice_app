package org.example.device.commandNameNormalizers;

public class PrefixCutNormalizer implements DeviceCommandNameNormalizer {
    private final String prefix;
    private final int keepLength;
    
    public PrefixCutNormalizer(String prefix, int keepLength) {
        this.prefix = prefix;
        this.keepLength = keepLength;
    }
    
    @Override
    public String normalize(String commandName) {

        if(commandName == null){
            return null;
        }


        if(commandName.length() > 5 && commandName.contains("CRDG")){
            commandName = commandName.substring(0, 6);
        }else{
            commandName = commandName.trim();
        }


        if(commandName.length() == 5 && commandName.contains("M^")){
            commandName = commandName.substring(3);
        }

        if(commandName.length() == 13 && commandName.contains("V0091")){
            commandName = commandName.substring(6);
            //System.out.println("Remove prefix and got " + name);
        }

        if (prefix != null && !prefix.isEmpty() && commandName.contains(prefix)) {
            //return commandName.substring(0, keepLength);
            return commandName.trim();
        }
        return commandName;
    }
}