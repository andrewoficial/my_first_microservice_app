package org.example.utilites;

import org.example.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringLoader {

    public static SpringApplication app = new SpringApplication(Main.class);
    //Нужно оставить Main.class как аргумент, что бы правильно сканировал классы конфигов
    public static ConfigurableApplicationContext ctx;


    public static void RunApp(){
        ctx = app.run();
    }


}