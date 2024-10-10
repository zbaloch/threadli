package com.async.async_web.controllers;

import java.security.Principal;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.async.async_web.models.User;
import com.async.async_web.models.Workspace;
import com.async.async_web.models.WorkspaceMembership;
import com.async.async_web.models.WorkspaceRole;
import com.async.async_web.repositories.UserRepository;
import com.async.async_web.repositories.WorkspaceMembershipRepository;
import com.async.async_web.repositories.WorkspaceRepository;
import com.async.async_web.services.EmailService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;

@Controller
public class UserController {

    private static Logger log = LoggerFactory.getLogger(UserController.class);

    private final SecureRandom random = new SecureRandom();
    private static final int TOKEN_BYTE_SIZE = 32;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;


    // private SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();

    private final SecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();

    @GetMapping("/signup")
    public String showSignUpForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("create-user")
    public String createUser(User user, 
        Model model,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        // this.securityContextRepository = securityContextRepository;
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(user.getPassword());
        user.setPassword(encodedPassword);
        userRepository.save(user);
        model.addAttribute("successTitle", "Yay! Your account has been created successfully! Log in to your account now.");
        model.addAttribute("successMessage", "Your account has been created successfully! Log in to your account now.");

        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        final Authentication authentication = UsernamePasswordAuthenticationToken.
                authenticated(user.getEmail(), null, null);
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        
        return "redirect:/";
    }

    
    @GetMapping("/verify-token-and-login")
    public String verifyTokenAndLogin(User user, 
        Model model,
        @Param(value = "token") String token,
        @Param(value = "email") String email,
        HttpServletRequest request,
        HttpServletResponse response,
        RedirectAttributes redirectAttrs
    ) {
        if(email.contains(" ")) {
            email = email.replaceAll(" ", "+");
        }
        log.info(email);
        log.info(token);
        User existingUser = userRepository.findByEmailAndToken(email, token);
        if(existingUser != null) {

             SecurityContext context = securityContextHolderStrategy.createEmptyContext();
            final Authentication authentication = UsernamePasswordAuthenticationToken.
                    authenticated(user.getEmail(), null, null);
            context.setAuthentication(authentication);
            securityContextHolderStrategy.setContext(context);
            securityContextRepository.saveContext(context, request, response);

            redirectAttrs.addFlashAttribute("successMessage", "Login successfull.");

            // Invalidate the token
            existingUser.setToken(null);
            existingUser.setTokenUsedDate(null);
            existingUser.setTokenExpirationDate(null);
            userRepository.save(existingUser);

            List<Workspace> workspaces = workspaceRepository.findByMembershipsUserId(existingUser.getId());

            if(workspaces.size() == 0) {
                createWorkspaceForUser(existingUser);
            }

            workspaces = workspaceRepository.findByMembershipsUserId(existingUser.getId());

            model.addAttribute("workspaces", workspaces);
            model.addAttribute("user", existingUser);

            return "redirect:/w/" + workspaces.iterator().next().getId();
        } else {
            model.addAttribute("errorTitle", "Login error");
            model.addAttribute("errorMessage", "No account for this email. Signup now to selling on Async.");

           return "login";
        }


    }
    

    @GetMapping("/login")
    public String showLoginForm(Model model, @RequestParam(name="error", required=false) String error) {
        log.info("error " + error);
        model.addAttribute("error", error);
        if(error != null) {
            model.addAttribute("errorTitle", "Login failed!");
            model.addAttribute("errorMessage", "Your email or password did not match our records. Please try again.");
        }
        model.addAttribute("user", new User());
        return "login";
    }

    @PostMapping("create-user-magic")
    public String createUserMagic(User user, 
        Model model,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        User existingUser = userRepository.findByEmail(user.getEmail());

        String token = generateToken();

        if(existingUser != null) {
            
            existingUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            existingUser.setTokenExpirationDate( expirationDate );
            existingUser.setTokenUsedDate(null);
            userRepository.save(existingUser);
            emailService.sendEmail(existingUser);

        } else {

            user.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            user.setTokenExpirationDate( expirationDate );
            user.setTokenUsedDate(null);
            userRepository.save(user);
            emailService.sendEmail(user);

        }

        log.info("token " + token);
        model.addAttribute("user", new User());
        

        model.addAttribute("successTitle", "Magic link sent to email");
        model.addAttribute("successMessage", "Check your email for the magic link to login to your account.");

        return "signup";
    }

    @PostMapping("login-magic")
    public String loginMagic(User user, 
        Model model,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        User existingUser = userRepository.findByEmail(user.getEmail());

        String token = generateToken();

        if(existingUser != null) {
            
            existingUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            existingUser.setTokenExpirationDate( expirationDate );
            existingUser.setTokenUsedDate(null);
            userRepository.save(existingUser);

            log.info("token " + token);
            model.addAttribute("user", new User());

            emailService.sendEmail(existingUser);

            List<Workspace> workspaces = workspaceRepository.findByMembershipsUserId(existingUser.getId());

            if(workspaces.size() == 0) {
                createWorkspaceForUser(existingUser);
            }

            model.addAttribute("successTitle", "Magic link sent to email");
            model.addAttribute("successMessage", "Check your email for the magic link to login to your account.");
            
            return "login";

        } else {

            log.info("token " + token);
            model.addAttribute("user", new User());

            model.addAttribute("errorTitle", "Login error");
            model.addAttribute("errorMessage", "No account for this email. Signup now to selling on Async.");

           return "signup";

        }
        
    }

    @GetMapping("/forgot-password")
    public String showForgotForm(Model model) {
        model.addAttribute("user", new User());
        return "forgot-password";
    }

    @GetMapping("/profile")
    public String profile(Principal principal, Model model) {
        User user = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", user);
        return "profile/index";
    }


    private void autoLoginAfterSignup(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder.getContextHolderStrategy();
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
        securityContextRepository.saveContext(context, request, response);
    }

    private String generateToken() {
        
        byte[] bytes = new byte[TOKEN_BYTE_SIZE];
        random.nextBytes(bytes);
        return String.valueOf(Hex.encode(bytes));
    }


    private void createWorkspaceForUser(User user) {
        // Create new workspace
        Workspace workspace = new Workspace();
        workspace.setName(user.getFirstName() + " " + user.getLastName() + "'s Workspace");
        workspace.setCreatedBy(user);
        workspaceRepository.save(workspace);

        // Add user to workspace
        WorkspaceMembership membership = new WorkspaceMembership();
        membership.setUser(user);
        membership.setWorkspace(workspace);
        membership.setRole(WorkspaceRole.ADMIN);
        membership.setDefaultWorkspace(true);
        workspaceMembershipRepository.save(membership);
    }

    @GetMapping("/profile/edit")
    public String editProfile(Principal principal, Model model) {
        User user = userRepository.findByEmail(principal.getName());
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/profile/edit")
    public String editProfile(
        @RequestParam("firstName") String firstName,
        @RequestParam("lastName") String lastName,
        Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName());
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
        model.addAttribute("user", user);
        model.addAttribute("success", "Profile updated");
        model.addAttribute("successMessage", "Your profile has been updated.");
        return "profile/edit";
    }

    
}