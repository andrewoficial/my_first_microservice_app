package org.example.web.service.myUserDetailService;

import org.springframework.context.annotation.Profile;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Profile({ "srv-offline", "srv-online" })
public class MyUserDetailServiceOffline implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Run MyUserDetailServiceOffline");
        //Environment env = SpringLoader.ctx.getEnvironment();
        //String[] activeProfiles = env.getActiveProfiles();
        //System.out.println("Active profiles: " + String.join(", ", activeProfiles));


        // Проверка захардкоженного пользователя
        if ("offlineUser".equals(username)) {
            return User.withUsername("offlineUser")
                    .password(new BCryptPasswordEncoder().encode("offlinePassword"))
                    .roles("USER")
                    .build();
        }else {
            throw new UsernameNotFoundException("В оффлайн режиме пользователь не найден");
        }

    }
}
