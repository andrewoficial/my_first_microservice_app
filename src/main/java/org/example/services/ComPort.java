/*
Файл содержит активное соединение по ком-порту

Переделать в коллекцию асинхронную (volatite) (зочем?)
 */
package org.example.services;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;
import lombok.Getter;
import org.example.utilites.MyUtilities;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class ComPort {
    public  SerialPort activePort;
    private SerialPort[] ports = SerialPort.getCommPorts();



    @Getter
    private int comNumber = 0;



    public void showAllPort() {
        int i = 0;
        for(SerialPort port : ports) {
            System.out.print(i + ". " + port.getDescriptivePortName() + " ");
            System.out.println(port.getPortDescription());
            i++;
        }
    }

    public ArrayList<SerialPort> getAllPorts(){
        ArrayList <SerialPort> forReturn = new ArrayList<SerialPort>();
        forReturn.addAll(Arrays.asList(ports));
        return forReturn;
    }
    public void setPort(int portIndex) {
        activePort = ports[portIndex];
        comNumber = portIndex;
        /* Не нужно открывать что попало
            if (activePort.openPort())
                System.out.println(activePort.getPortDescription() + " порт открыт");
            else
                System.out.println("Cant open");
        */

    }

    public void updatePorts(){
        /*
        System.out.println(SerialPort.getVersion());
        System.out.println(System.getProperty("java.version"));
        System.out.println(System.getProperty("java.vm.name"));
        System.out.println(System.getProperty("sun.arch.data.model"));
        ports = SerialPort.getCommPorts();
        ports = SerialPort.getCommPorts();
        System.out.println("All OK");
        //System.out.println(SerialPort.getVersion());
         */

        Arrays.fill(ports, null);
        ports = SerialPort.getCommPorts();
    }

    public String getCurrentComName(){
        return MyUtilities.removeComWord(activePort.getSystemPortName());
    }

}
