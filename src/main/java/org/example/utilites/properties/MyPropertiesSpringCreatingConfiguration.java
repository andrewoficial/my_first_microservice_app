package org.example.utilites.properties;

import org.example.gui.MainLeftPanelStateCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyPropertiesSpringCreatingConfiguration {
    @Autowired
    private ApplicationContext applicationContext;

    @Bean
    public MyProperties myProperties() {
        // Получаем MainLeftPanelStateCollection из контекста
        MainLeftPanelStateCollection stateCollection =
                applicationContext.getBean(MainLeftPanelStateCollection.class);

        return new MyProperties(true, stateCollection);
    }
}

