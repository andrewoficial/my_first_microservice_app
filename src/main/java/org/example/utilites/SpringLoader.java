package org.example.utilites;

import org.example.Main;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class SpringLoader {
    public static ConfigurableApplicationContext ctx;
    public static SpringApplication app = new SpringApplication(Main.class);

    public static void RunApp(){
    }


}
