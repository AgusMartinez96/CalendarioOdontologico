package com.api.agenda_odontologica.controller;

import org.springframework.stereotype.Controller;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("legacy-ui")
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login"; // Nombre del archivo login.html en templates
    }
}
