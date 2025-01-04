package org.example.web.controller;

import org.example.gui.MainWindow;
import org.example.web.service.FilePathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.IOException;
import java.net.URISyntaxException;

@ConditionalOnProperty(name = "server.enabled", havingValue = "true")
@Controller
public class TerminalController {

    @Autowired
    private FilePathService filePathService;

    @GetMapping("/terminal")
    public String myPage(Model model) {
        // Получаем количество вкладок из статического метода MainWindow
        int tabCount = MainWindow.getCurrTabCount();
        model.addAttribute("tabCount", tabCount);
        return "terminal";
    }
}