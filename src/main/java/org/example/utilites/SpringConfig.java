package org.example.utilites;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.example.gui.ServerSettingsWindow;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
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

    @Profile("production")  // Этот бин будет создаваться только в production режиме
    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
        dataSourceBuilder.username(MyProperties.usr);
        dataSourceBuilder.password(MyProperties.pwd);
        dataSourceBuilder.url(MyProperties.url);
        dataSourceBuilder.driverClassName(MyProperties.driver);
        return dataSourceBuilder.build();
    }

    @Profile("offline")
    @Bean
    public DataSource getOfflineDataSource() {
        // Возвращайте или создавайте здесь другой DataSource, если нужно, например H2 in-memory database.
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
}