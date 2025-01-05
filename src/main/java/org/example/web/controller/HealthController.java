package org.example.web.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Profile({ "srv-offline", "srv-online" })
@Controller
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "health";
    }
}