package org.example.utilites;

import lombok.Getter;
import org.example.Main;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

@Getter  //Lombok
@Configuration
public class SpringConfig {

    @Profile("production")
    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        //dataSourceBuilder.username(Main.prop.getUsr());
        //dataSourceBuilder.password(Main.prop.getPwd());
        //dataSourceBuilder.url(Main.prop.getUrl());
        //dataSourceBuilder.driverClassName(Main.prop.getDrv());
        return dataSourceBuilder.build();
    }

    @Profile("offline")
    @Bean
    public DataSource getOfflineDataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}