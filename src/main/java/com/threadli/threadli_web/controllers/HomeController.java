package com.threadli.threadli_web.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.threadli.threadli_web.services.UserService;

import java.security.Principal;

@Controller
public class HomeController {

    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(Principal principal) {
        if (principal == null) {
            return "index";
        }
        log.info("User: " + principal.getName() + " accessed home page");
        return "forward:/t/list";
    }
}
