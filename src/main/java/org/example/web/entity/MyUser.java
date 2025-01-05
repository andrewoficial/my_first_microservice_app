package org.example.web.entity;

import jakarta.persistence.*;
import jdk.jfr.DataAmount;
import org.springframework.context.annotation.Profile;

@Profile({ "srv-offline", "srv-online" })
@Entity
@Table(name = "tb_users")
public class MyUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private  Long id;

    @Column(unique = true)
    private String name;

    private String password;

    private String role;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getRoles() {
        return this.role;
    }
}
