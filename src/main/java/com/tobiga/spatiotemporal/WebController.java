package com.tobiga.spatiotemporal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("username", "Tobiga9"); // Example dynamic value
        return "index"; // Refers to src/main/resources/templates/index.html
    }
}
