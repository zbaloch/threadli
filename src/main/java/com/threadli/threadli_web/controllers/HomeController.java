package com.threadli.threadli_web.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.repositories.WorkspaceRepository;
import com.threadli.threadli_web.services.UserService;
import com.threadli.threadli_web.services.WorkspaceService;

import java.security.Principal;



@Controller
public class HomeController {

    // Add logging
    private static final Logger log = LoggerFactory.getLogger(HomeController.class);

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public String home(
        Principal principal,
        RedirectAttributes redirectAttributes
        ) {
            if(principal == null) {
                return "index";
            }
         log.info("User: " + principal.getName() + " accessed home page");
         // Find user by email and get workspace memberships
         User user = userService.findByEmail(principal.getName());

         log.info("User: {}, Memberships: {}", user.getEmail(), user.getWorkspaceMemberships().iterator().next().getWorkspace().getId());

         // TODO: Redirect to the default workspace

        return "redirect:/w/" + user.getWorkspaceMemberships().iterator().next().getWorkspace().getId();

    }
}
