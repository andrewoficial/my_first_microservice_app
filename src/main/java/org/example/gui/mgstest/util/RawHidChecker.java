package org.example.gui.mgstest.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class RawHidChecker {
    
    public static void checkHidRawDevices() {
        File devDir = new File("/dev");
        File[] files = devDir.listFiles((dir, name) -> name.startsWith("hidraw"));
        
        if (files == null || files.length == 0) {
            System.out.println("No hidraw devices found in /dev/");
            return;
        }
        
        for (File device : files) {
            System.out.println("\nChecking: " + device.getAbsolutePath());
            System.out.println("Permissions: " + 
                (device.canRead() ? "R" : "-") + 
                (device.canWrite() ? "W" : "-"));
            
            try {
                // Попытка прочитать первые байты
                byte[] buffer = new byte[64];
                try (FileInputStream fis = new FileInputStream(device)) {
                    int bytesRead = fis.read(buffer);
                    System.out.println("Raw read: " + bytesRead + " bytes");
                    if (bytesRead > 0) {
                        System.out.print("Data: ");
                        for (int i = 0; i < bytesRead; i++) {
                            System.out.print(String.format("%02X ", buffer[i]));
                        }
                        System.out.println();
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading: " + e.getMessage());
            }
        }
    }

}