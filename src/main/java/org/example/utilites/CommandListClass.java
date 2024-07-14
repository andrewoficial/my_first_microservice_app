package org.example.utilites;

import java.util.HashMap;

public class CommandListClass {
    HashMap <String, SingleCommand> commandPool;
    public CommandListClass(){
        this.commandPool = new HashMap<>();
    }

    public void addCommand(SingleCommand command){
        commandPool.put(command.getName(), command);
    }

    public SingleCommand getCommand(String name){
        if(name == null){
            return null;
        }

        if(name.length() > 5 && name.contains("CRDG")){
            name = name.substring(0, 6);
        }else{
            name = name.trim();
        }


        if(name.length() == 5 && name.contains("M^")){
            name = name.substring(3);
        }
        return commandPool.get(name);
    }

    public boolean isKnownCommand(String name){
        System.out.println(name);
        if(name == null){
            System.out.println("Пустая команда");
            return false;
        }

        if(name.length() > 5 && name.contains("CRDG")){
            name = name.substring(0, 6);
        }else{
            name = name.trim();
        }

        if(name.length() == 5 && name.contains("M^")){
            name = name.substring(3);
        }
        System.out.println(name);
        return commandPool.containsKey(name);
    }

    public HashMap  <String, SingleCommand> getCommandPool(){
        return commandPool;
    }
}
