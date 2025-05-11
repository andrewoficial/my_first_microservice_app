package org.example.web.controller;

import org.example.services.connectionPool.AnyPoolService;
import org.example.utilites.properties.MyProperties;
import org.example.web.service.FilePathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Profile({ "srv-offline", "srv-online" })
@Controller
public class TerminalController {

    @Autowired
    private FilePathService filePathService;

    @Autowired
    private AnyPoolService anyPoolService;

    @Autowired
    private MyProperties myProperties;

    @GetMapping("/terminal")
    public String myPage(Model model) {
        int tabCount = myProperties.getLeftPanelStateCollection().getSize();
        model.addAttribute("tabCount", tabCount);
        return "terminal";
    }
}