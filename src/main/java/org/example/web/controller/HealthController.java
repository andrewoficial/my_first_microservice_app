package org.example.web.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@ConditionalOnProperty(name = "server.enabled", havingValue = "true")
@Controller
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "health";
    }
}