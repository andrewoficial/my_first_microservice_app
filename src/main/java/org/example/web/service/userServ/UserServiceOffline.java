package org.example.web.service.userServ;

import org.example.web.entity.MyUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Profile("offline")
public class UserServiceOffline implements UserService {
    private final PasswordEncoder passwordEncoder;
    private final Map<String, MyUser> offlineUsers = new HashMap<>();  // Простое хранилище для оффлайн пользователей

    @Autowired
    public UserServiceOffline(PasswordEncoder passwordEncoder) {
        System.out.println("UserServiceOffline");
        this.passwordEncoder = passwordEncoder;

        // Добавляем оффлайн пользователя вручную
        MyUser offlineUser = new MyUser();
        offlineUser.setName("offlineUser");
        offlineUser.setPassword(passwordEncoder.encode("offlinePassword"));
        offlineUsers.put(offlineUser.getName(), offlineUser);
    }

    // Метод для добавления пользователей в оффлайн режиме
    public void addUser(MyUser user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        offlineUsers.put(user.getName(), user);
        System.out.println("Оффлайн пользователь добавлен: " + user.getName());
    }

}