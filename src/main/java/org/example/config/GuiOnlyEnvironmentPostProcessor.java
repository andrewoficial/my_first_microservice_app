package org.example.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In gui-only profile, excludes JPA/DataSource/Security/Web MVC auto-configurations
 * before AutoConfigurationImportSelector runs.
 * <p>
 * Spring Boot 4 moved these classes out of {@code org.springframework.boot.autoconfigure.*};
 * excludes must use the new packages or they are silently ignored / rejected.
 */
public class GuiOnlyEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String PROPERTY_SOURCE_NAME = "guiOnlyAutoConfigurationExcludes";

    /**
     * Boot 4 auto-configuration class names (not the Boot 2/3 packages).
     */
    private static final String GUI_ONLY_EXCLUDES = String.join(",",
            // JPA / Hibernate / JDBC — main ~5s cost
            "org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration",
            "org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceTransactionManagerAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.DataSourceInitializationAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration",
            "org.springframework.boot.jdbc.autoconfigure.JdbcClientAutoConfiguration",
            // Validation (Hibernate Validator)
            "org.springframework.boot.validation.autoconfigure.ValidationAutoConfiguration",
            // Web stack not needed without server
            "org.springframework.boot.webmvc.autoconfigure.WebMvcAutoConfiguration",
            "org.springframework.boot.webmvc.autoconfigure.DispatcherServletAutoConfiguration",
            "org.springframework.boot.webmvc.autoconfigure.error.ErrorMvcAutoConfiguration",
            "org.springframework.boot.thymeleaf.autoconfigure.ThymeleafAutoConfiguration",
            // Security not needed in pure GUI mode
            "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration",
            "org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration"
    );

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (!isGuiOnly(environment)) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("spring.autoconfigure.exclude", GUI_ONLY_EXCLUDES);
        map.put("spring.sql.init.mode", "never");
        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
        // Early bootstrap: logger may not be ready yet
        System.out.println("[GuiOnlyEnvironmentPostProcessor] gui-only: excluded JPA/DataSource/Validation/WebMvc/Security auto-config");
    }

    private static boolean isGuiOnly(ConfigurableEnvironment environment) {
        return Arrays.asList(environment.getActiveProfiles()).contains("gui-only")
                || (environment.getActiveProfiles().length == 0
                && Arrays.asList(environment.getDefaultProfiles()).contains("gui-only"));
    }

    @Override
    public int getOrder() {
        // After config data is loaded so active profiles are reliable; still before context refresh.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
