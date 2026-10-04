package com.api.agenda_odontologica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping({"/", "/calendar", "/patients"})
    public String application() {
        return "forward:/index.html";
    }
}