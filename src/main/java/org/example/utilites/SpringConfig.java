package org.example.utilites;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Getter  //Lombok
@Configuration
public class SpringConfig {
    @Setter
    private String dbUrl = "jdbc:postgresql://floppy.db.elephantsql.com:5432/zhsiszsk";


    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.username("zhsiszsk");
        dataSourceBuilder.password("EcrvEk0pw2UaY6jdKY16R3RGiBrefui1");
        dataSourceBuilder.url(dbUrl);
        dataSourceBuilder.driverClassName("org.postgresql.Driver");
        return dataSourceBuilder.build();
    }
}
