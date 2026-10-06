package org.example.web.controller;

import lombok.RequiredArgsConstructor;
import org.example.services.TabService;
import org.example.utilites.properties.MyProperties;
import org.example.web.service.FilePathService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Profile({ "srv-offline", "srv-online" })
@Controller
@RequiredArgsConstructor
public class TerminalController {

    private final FilePathService filePathService;
    private final TabService tabService;
    private final MyProperties myProperties;

    @GetMapping("/terminal")
    public String myPage(Model model) {
        int tabCount = tabService.getStateSize();
        model.addAttribute("tabCount", tabCount);
        model.addAttribute("canSend", currentUserCanSend());
        return "terminal";
    }

    @GetMapping("/graphs")
    public String graphs(Model model) {
        model.addAttribute("tabCount", tabService.getStateSize());
        return "graph";
    }

    static boolean currentUserCanSend() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}