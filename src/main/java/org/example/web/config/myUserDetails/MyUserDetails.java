package org.example.web.config.myUserDetails;

import org.example.web.entity.MyUser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Представляет пользователя в системе безопасности Spring.
 * Этот класс реализует интерфейс UserDetails и предоставляет информацию
 * о пользователе, необходимую для аутентификации и авторизации.
 *
 * @author [Kantser Andrey]
 * @version 1.0
 */


//@ConditionalOnProperty(name = "server.enabled", havingValue = "true")
@Profile("srv-online")
public class MyUserDetails implements UserDetails {
    private MyUser user;
    public MyUserDetails(MyUser user){
        System.out.println("MyUserDetails");
        this.user = user;
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Arrays.stream(user.getRoles().split(", "))
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return this.user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getName();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
