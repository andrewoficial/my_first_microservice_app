package org.example.web.controller;

import org.example.web.config.MyUserDetails;
import org.example.web.entity.MyUser;
import org.example.web.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class MyUserDetailService implements UserDetailsService {
    @Autowired
    private UserRepository repository;
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional <MyUser> user = repository.findByName(username);
        return user.map(MyUserDetails::new)
                .orElseThrow();
    }
}
