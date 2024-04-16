/*
Вспомогательные сложно классифицируемые утилиты
 */
package org.example.utilites;

import com.fazecast.jSerialComm.SerialPort;
import org.example.services.ComPort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;

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


    public static Date convertToLocalDateViaMilisecond(LocalDateTime dateToConvert) {
        return java.util.Date
                .from(dateToConvert.atZone(ZoneId.systemDefault())
                        .toInstant());
    }

    public static Date convertToDateViaSqlDate(LocalDate dateToConvert) {
        return java.sql.Date.valueOf(dateToConvert);
    }

    public static void restoreLastComPort(ComPort comPort, MyProperties properties){
        System.out.println("Restore " + properties.getLastComPort());
        comPort.updatePorts();
        int i = 0;
        for (SerialPort port : comPort.getAllPorts() ) {
            if(port.getSystemPortName().contains(properties.getLastComPort())){
                comPort.setPort(i);
                System.out.println("com found");
                break;
            }
            i++;
        }
        System.out.println("com doest exist");
    }
}
