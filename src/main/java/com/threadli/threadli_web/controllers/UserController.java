package com.threadli.threadli_web.controllers;

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

import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.models.WorkspaceMembership;
import com.threadli.threadli_web.models.WorkspaceRole;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.repositories.WorkspaceMembershipRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;
import com.threadli.threadli_web.services.EmailService;

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
        // Since Sep 22, 2025: All users join the default workspace instead of creating individual ones
        
        // Get the default workspace (should exist due to startup initialization)
        List<Workspace> allWorkspaces = workspaceRepository.findAll();
        Workspace defaultWorkspace;
        
        if (allWorkspaces.isEmpty()) {
            // Fallback: Create default workspace if it somehow doesn't exist
            log.warn("No default workspace found during user registration. Creating one now.");
            defaultWorkspace = new Workspace();
            defaultWorkspace.setName("Default Workspace");
            defaultWorkspace.setCreatedBy(user); // This user becomes the creator
            workspaceRepository.save(defaultWorkspace);
        } else {
            // Use the first (and should be only) default workspace
            defaultWorkspace = allWorkspaces.get(0);
        }

        // Check if user is already a member of this workspace
        boolean alreadyMember = workspaceMembershipRepository
            .findByWorkspaceIdAndUserId(defaultWorkspace.getId(), user.getId())
            .isPresent();
            
        if (!alreadyMember) {
            // Add user to the default workspace
            WorkspaceMembership membership = new WorkspaceMembership();
            membership.setUser(user);
            membership.setWorkspace(defaultWorkspace);
            
            // First user becomes admin, others become members
            boolean isFirstUser = workspaceMembershipRepository.count() == 0;
            membership.setRole(isFirstUser ? WorkspaceRole.ADMIN : WorkspaceRole.MEMBER);
            membership.setDefaultWorkspace(true);
            
            workspaceMembershipRepository.save(membership);
            
            log.info("Added user {} to default workspace as {}", 
                    user.getEmail(), membership.getRole());
        }
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