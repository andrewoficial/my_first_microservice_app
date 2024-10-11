package org.example.web.repository;

import org.example.web.entity.MyUser;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
@Profile("production")
public interface UserRepository extends JpaRepository<MyUser, Long> {
    Optional<MyUser> findByName(String username);
}
