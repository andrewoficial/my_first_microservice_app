/*
Файл содержит активное соединение по ком-порту

Переделать в коллекцию асинхронную (volatite) (зочем?)
 */
package org.example.services.comPort;

import com.fazecast.jSerialComm.SerialPort;
import lombok.Getter;
import org.example.utilites.MyUtilities;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.Arrays;



@Component
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


    /**
     *Текущий массив портов преобразует в список и возвращает
     */
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
