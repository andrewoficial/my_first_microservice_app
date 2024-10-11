package org.example.web.service.myUserDetailService;

import org.example.utilites.SpringLoader;
import org.example.web.config.myUserDetails.MyUserDetails;
import org.example.web.entity.MyUser;
import org.example.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Profile("production")
@Service
public class MyUserDetailServiceProduction implements UserDetailsService {

    @Autowired
    private UserRepository repository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        System.out.println("Run MyUserDetailServiceProduction");
        Environment env = SpringLoader.ctx.getEnvironment();
        String[] activeProfiles = env.getActiveProfiles();
        System.out.println("Active profiles: " + String.join(", ", activeProfiles));


        Optional <MyUser> user = repository.findByName(username);


        return user.map(MyUserDetails::new)
                .orElseThrow();
    }
}
