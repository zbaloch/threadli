package com.threadli.threadli_web.controllers;

import java.security.Principal;
import java.util.List;
import java.util.Calendar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.services.EmailService;

@Controller
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    /**
     * Show members page (admin only)
     */
    @GetMapping("/settings/members")
    public String showMembers(Principal principal, Model model, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getName());

        // Admin check
        if (!user.getIsAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Access denied");
            return "redirect:/";
        }

        List<User> allUsers = userRepository.findAll();
        model.addAttribute("users", allUsers);
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", user.getIsAdmin());
        model.addAttribute("pageTitle", "Threadli | Settings & Members");

        return "settings/members";
    }

    /**
     * Invite user via magic link email (admin only)
     */
    @PostMapping("/settings/members")
    public String inviteUser(
            @RequestParam String email,
            Principal principal,
            Model model,
            RedirectAttributes redirectAttributes) {

        User currentUser = userRepository.findByEmail(principal.getName());

        // Admin check
        if (!currentUser.getIsAdmin()) {
            redirectAttributes.addFlashAttribute("error", "Access denied");
            return "redirect:/";
        }

        User existingUser = userRepository.findByEmail(email);

        String token = generateToken();

        if (existingUser != null) {
            // User already exists, send login magic link
            existingUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            existingUser.setTokenExpirationDate(expirationDate);
            existingUser.setTokenUsedDate(null);
            userRepository.save(existingUser);
            emailService.sendEmail(existingUser);
            log.info("Sent login magic link to existing user: {}", email);
            redirectAttributes.addFlashAttribute("successMessage", "Magic link sent to " + email);
        } else {
            // New user, send signup magic link
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            newUser.setTokenExpirationDate(expirationDate);
            newUser.setTokenUsedDate(null);
            userRepository.save(newUser);
            emailService.sendEmail(newUser);
            log.info("Sent signup magic link to new user: {}", email);
            redirectAttributes.addFlashAttribute("successMessage", "Invitation sent to " + email);
        }

        return "redirect:/settings/members";
    }

    private String generateToken() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return String.valueOf(org.springframework.security.crypto.codec.Hex.encode(bytes));
    }
}
