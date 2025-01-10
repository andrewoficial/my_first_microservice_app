package org.example.utilites.properties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyPropertiesSpringCreatingConfiguration {
    @Bean
    public MyProperties myProperties() {
        return new MyProperties(true);
    }
}

