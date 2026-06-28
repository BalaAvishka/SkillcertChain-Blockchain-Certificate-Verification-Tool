package com.skincertchain.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serves the single-page web UI.
 * All UI logic lives in src/main/resources/static/index.html
 * using vanilla JS calling the REST API.
 */
@Controller
public class WebController {

    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }
}
