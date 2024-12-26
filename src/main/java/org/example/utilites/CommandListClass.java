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

        if(name.length() == 13 && name.contains("V0091")){
            name = name.substring(6);
            //System.out.println("Remove prefix and got " + name);
        }
        return commandPool.get(name);
    }

    public boolean isKnownCommand(String name){

        if(name == null){
            return false;
        }
        if(getCommand(name) == null){
            return false;
        }

        return true;
    }

    public int getExpectedBytes(String name){

        if(getCommand(name) == null){
            return 500000;
        }
        return getCommand(name).getExpectedBytes();
    }

    public HashMap  <String, SingleCommand> getCommandPool(){
        return commandPool;
    }
}
