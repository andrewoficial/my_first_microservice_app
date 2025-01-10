package org.example.utilites.properties;

import lombok.Getter;
import lombok.Setter;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class MyPropertiesFileHandler {
    private static Logger log = null;



    @Setter
    @Getter
    private File settingFile = null;

    public MyPropertiesFileHandler(){
        //Thread.currentThread().setName("MyProperties");
        log = Logger.getLogger(MyPropertiesFileHandler.class);
        log.debug("Загружаю файл configAccess.properties");
        try {
            Path configDir = Paths.get("config");
            Files.createDirectories(configDir);
            Path configFile = configDir.resolve("configAccess.properties");
            if (Files.exists(configFile)) {
                log.debug("Файл с настройками найден" + configFile.toAbsolutePath());
            } else {
                Files.createFile(configFile);
                log.warn("Создан новый файл с настройками" + configFile.toAbsolutePath());
            }
            this.settingFile = configFile.toFile();
        } catch (IOException e) {
            log.warn("Ошибка при работе с файлом настроек " + e.getMessage());
        }
    }

    public FileInputStream getFileInputStream(){
        try {
            return new FileInputStream(settingFile.getAbsolutePath());
        } catch (FileNotFoundException e) {
            log.warn("Файл с настройками не был найден " + e.getMessage());
            //throw new RuntimeException(e);
            return null;
        }
    }

    public void closeFileInputStream(FileInputStream fileInputStream){
        try {
            assert fileInputStream != null;
            fileInputStream.close();
        } catch (IOException e) {
            log.warn("Ошибка при закрытии файла с настройками (при чтении) " + e.getMessage());
            //throw new RuntimeException(e);
        }
    }

    public void closeFileOutputStream(FileOutputStream fileOutputStream){
        try {
            assert fileOutputStream != null;
            fileOutputStream.close();
        } catch (IOException e) {
            log.warn("Ошибка при закрытии файла с настройками (при записи) " + e.getMessage());
            //throw new RuntimeException(e);
        }
    }

    public Properties loadPropertiesFromFile(){
        java.util.Properties propertiesForReturn = null;
        FileInputStream in = getFileInputStream();
        try {
            propertiesForReturn = new java.util.Properties();
            propertiesForReturn.load(in);
        } catch (IOException e) {
            log.warn("Ошибка при загрузке файла с настройками " + e.getMessage());
        } finally {
            closeFileInputStream(in);
        }
        return propertiesForReturn;
    }

    public void updateFileFromProperties(Properties properties){
        OutputStream file = null;
        try {
            file = new FileOutputStream(this.settingFile.getAbsoluteFile());
        }catch (IOException e) {
            log.error("Ошибка открытия файла для сохранения " + e.getMessage());
        }
            try{
                properties.store(file, "General Settings");
            } catch (IOException e) {
                log.error("Ошибка обновления файла настроек " + e.getMessage());
            }finally {
                try {
                    file.close();
                } catch (IOException e) {
                    log.error("Ошибка закрытия файлового потока (при записи файла настроек) " + e.getMessage());
                }

            }

    }
}
