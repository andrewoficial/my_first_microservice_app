package org.example.utilites;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.example.gui.ServerSettingsWindow;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import javax.swing.plaf.PanelUI;

@Getter  //Lombok
@Configuration
public class SpringConfig {
    @Setter
    public static String dbUrl;
    public static String username;
    public static String password;
    public static String driver = "org.postgresql.Driver";

    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.username(MyProperties.usr);
        dataSourceBuilder.password(MyProperties.pwd);
        dataSourceBuilder.url(MyProperties.url);
        dataSourceBuilder.driverClassName(MyProperties.driver);
        return dataSourceBuilder.build();
    }
}