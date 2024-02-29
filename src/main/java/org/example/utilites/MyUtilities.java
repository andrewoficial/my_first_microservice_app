package org.example.utilites;

import java.util.ArrayList;

public class MyUtilities {
    public static String removeComWord(String arg){
        if(arg == null || arg.length() < 1){
            return " ";
        }
        if(arg.indexOf("(CO") > 0){
            return arg.substring(0, arg.indexOf("(CO"));
        }else{
            return arg;
        }

    }

    public static boolean containThreadByName(ArrayList<Thread> threadArrList, String name){
        if(threadArrList.isEmpty() || name == null)
            return false;

        if(name.isEmpty())
            return false;

        for (Thread thread : threadArrList) {
            if(thread.getName().equalsIgnoreCase(name))
                return true;
        }

        return false;
    }

    public static Thread getThreadByName(ArrayList<Thread> threadArrList, String name){
        if(threadArrList.isEmpty() || name == null)
            return null;

        if(name.isEmpty())
            return null;

        for (Thread thread : threadArrList) {
            if(thread.getName().equalsIgnoreCase(name))
                return thread;
        }

        return null;
    }
}
