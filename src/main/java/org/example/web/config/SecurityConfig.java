package org.example.web.config;


import org.example.web.repository.UserRepository;
import org.example.web.service.myUserDetailService.MyUserDetailServiceOffline;
import org.example.web.service.myUserDetailService.MyUserDetailServiceProduction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Profile({ "srv-offline", "srv-online" })
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    @ConditionalOnProperty(name = "server.enabled", havingValue = "true" )
    public SecurityConfig serverService() {
        return new SecurityConfig();
    }

    @Profile("srv-offline")
    @Bean
    public UserDetailsService userDetailsServiceOffline (){
        System.out.println("Try find MyUserDetailService in userDetailsServiceOffline");
        return new MyUserDetailServiceOffline();
    }

    @Profile("srv-online")
    @Bean
    public UserDetailsService userDetailsServiceProduction (UserRepository repository){
        System.out.println("Try find MyUserDetailService in userDetailsServiceOnline");
        return new MyUserDetailServiceProduction(repository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) {
        return http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry.requestMatchers("/api/v1/apps/welcome", "/api/v1/apps/new-user").permitAll())
                //.authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry.requestMatchers("/api/v1/apps/**").authenticated())
                .authorizeHttpRequests(authorizationManagerRequestMatcherRegistry -> authorizationManagerRequestMatcherRegistry.requestMatchers("/**").authenticated())
                .formLogin(AbstractAuthenticationFilterConfigurer::permitAll)
                .formLogin(formLogin -> formLogin.loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true))
                .build();
    }


    @Profile("srv-offline")
    @Bean
    public AuthenticationProvider authenticationProviderOffline(){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsServiceOffline());
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Profile("srv-online")
    @Bean
    public AuthenticationProvider authenticationProviderProduction(UserDetailsService userDetailsService){
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder(){
        return new BCryptPasswordEncoder();
    }



}
