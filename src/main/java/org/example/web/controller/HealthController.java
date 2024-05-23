package org.example.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "health";
    }
}