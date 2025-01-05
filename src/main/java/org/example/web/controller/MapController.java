package org.example.web.controller;

import org.example.web.service.FilePathService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.io.IOException;
import java.net.URISyntaxException;

@Profile({ "srv-offline", "srv-online" })
@Controller
public class MapController {

    @Autowired
    private FilePathService filePathService;

    @GetMapping("/signalMap")
    public String myPage(Model model) {
        //GPS_66_EBYTE.js
        //GPS_66_RAK.js
        String fileContentRak = "var data_66_RAK = [[0, 0, \"0\"]];";
        String fileContentEbyte = "var data_66_EBYTE = [[0, 0, \"0\"]];";
        try {
            fileContentRak = filePathService.getFileContent("GPS_66_RAK.js");
            model.addAttribute("fileContentRak", fileContentRak);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            fileContentEbyte = filePathService.getFileContent("GPS_66_EBYTE.js");
            model.addAttribute("fileContentEbyte", fileContentEbyte);
        } catch (IOException e) {
            e.printStackTrace();
        }
        model.addAttribute("fileContentRak", fileContentRak);
        model.addAttribute("fileContentEbyte", fileContentEbyte);

        return "signalMap";
    }
}