package com.threadli.threadli_web.controllers;

import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.models.WorkspaceMembership;
import com.threadli.threadli_web.models.WorkspaceRole;
import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.repositories.WorkspaceMembershipRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;
import com.threadli.threadli_web.services.EmailService;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import java.util.Calendar;
import java.util.List;
import java.security.SecureRandom;
import com.threadli.threadli_web.models.Thread;





@Controller
public class WorkspaceController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceMembershipRepository workspaceMembershipRepository;

    @Autowired
   private ThreadRepository threadRepository;

    @Autowired
    private EmailService emailService;
    
    @GetMapping("/w")
    public String workspaces(Model model, Principal principal) {
        User user = userRepository.findByEmail(principal.getName());
        List<Workspace> workspaces = workspaceRepository.findByMembershipsUserId(user.getId());
        model.addAttribute("workspaces", workspaces);
        model.addAttribute("user", user);
        return "workspace/index";
    }

    @GetMapping("/w/{workspaceId}")
    public String accessWorkspace(@PathVariable Long workspaceId, 
                                    Principal principal,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        boolean isAdmin = workspace.getMemberships().stream().anyMatch(membership -> membership.getUser().getId() == user.getId() && membership.getRole() == WorkspaceRole.ADMIN);
       
        // List<Thread> userThreads = threadRepository.findByWorkspaceIdAndMemberships_User_Id(workspaceId, user.getId());
        List<Thread> userThreads = threadRepository.findByWorkspaceIdAndMemberships_User_IdOrderByUpdatedAtDesc(workspaceId, user.getId());
        
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("user", user);
        model.addAttribute("workspace", workspace);
        model.addAttribute("userThreads", userThreads);
        model.addAttribute("view", "threads");
        
        // If the user can access the workspace, continue with the workspace vieww
        // Redirect the user to the first channel in this workspace
        return "workspace/view";
    }

    @GetMapping("/w/{workspaceId}/settings/general")
    public String workspaceSettings(@PathVariable Long workspaceId, 
                                    Principal principal,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        boolean isAdmin = workspace.getMemberships().stream().anyMatch(membership -> membership.getUser().getId() == user.getId() && membership.getRole() == WorkspaceRole.ADMIN);
        model.addAttribute("isAdmin", isAdmin);
        if(!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to access this workspace settings");
            return "redirect:/w/{workspaceId}";
        }
        model.addAttribute("user", user);
        model.addAttribute("workspace", workspace);
        // If the user can access the workspace, continue with the workspace vieww
        return "workspace/settings/general";
    }

    @PostMapping("/w/{workspaceId}/settings/general")
    public String saveWorkspaceSettings(@PathVariable Long workspaceId, 
                                    Principal principal,
                                  RedirectAttributes redirectAttributes,
                                  @RequestParam("name") String name,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        boolean isAdmin = workspace.getMemberships().stream().anyMatch(membership -> membership.getUser().getId() == user.getId() && membership.getRole() == WorkspaceRole.ADMIN);
        model.addAttribute("isAdmin", isAdmin);
        if(!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to access this workspace settings");
            return "redirect:/w/{workspaceId}";
        }
        workspace.setName(name);
        workspaceRepository.save(workspace);
        model.addAttribute("user", user);
        model.addAttribute("workspace", workspace);
        // If the user can access the workspace, continue with the workspace vieww
        return "workspace/settings/general";
    }

    @GetMapping("/w/{workspaceId}/settings/members")
    public String workspaceMembers(@PathVariable Long workspaceId, 
                                    Principal principal,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        boolean isAdmin = workspace.getMemberships().stream().anyMatch(membership -> membership.getUser().getId() == user.getId() && membership.getRole() == WorkspaceRole.ADMIN);
        model.addAttribute("isAdmin", isAdmin);
        if(!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to access this workspace members");
            return "redirect:/w/{workspaceId}";
        } else {
            model.addAttribute("isAdmin", true);
        }
            
        model.addAttribute("user", user);
        model.addAttribute("workspace", workspace);
        // If the user can access the workspace, continue with the workspace vieww
        return "workspace/settings/members";
    }


    @PostMapping("/w/{workspaceId}/settings/members")
    public String addWorkspaceMembers(@PathVariable Long workspaceId, 
                                    Principal principal,
                                    @RequestParam("email") String email,
                                    @RequestParam("firstName") String firstName,
                                    @RequestParam("lastName") String lastName,

                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        boolean isAdmin = workspace.getMemberships().stream().anyMatch(membership -> membership.getUser().getId() == user.getId() && membership.getRole() == WorkspaceRole.ADMIN);
        model.addAttribute("isAdmin", isAdmin);
        if(!isAdmin) {
            redirectAttributes.addFlashAttribute("error", "You are not authorized to access this workspace members");
            return "redirect:/w/{workspaceId}";
        }
        User newMember = userRepository.findByEmail(email);

        String token = generateToken();

        if(newMember != null) {
            // Get workspace by userid and workspace id
            boolean membershipExists = workspaceMembershipRepository.findByWorkspaceIdAndUserId(workspaceId, newMember.getId()).isPresent();    

            if(membershipExists) {
                redirectAttributes.addFlashAttribute("error", "Member already in workspace");
                // model.addAttribute("error", "Member already in workspace");
                return "redirect:/w/{workspaceId}/settings/members";
            }
            
            // Add this member to this workspace
            WorkspaceMembership membership = new WorkspaceMembership();
            membership.setWorkspace(workspace);
            membership.setUser(newMember);
            membership.setRole(WorkspaceRole.MEMBER);
            membership.setDefaultWorkspace(false);
            workspaceMembershipRepository.save(membership);
            workspace.getMemberships().add(membership);
            workspaceRepository.save(workspace);

            // TODO: Notify the user that they have been added to a workspace
            
        } else {
            // Create a new user, add them to the workspace, and send them an email to join the workspace
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setToken(token);
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.HOUR, 1);
            newUser.setTokenExpirationDate( expirationDate );
            newUser.setTokenUsedDate(null);
            userRepository.save(newUser);

            WorkspaceMembership membership = new WorkspaceMembership();
            membership.setWorkspace(workspace);
            membership.setUser(newUser);
            membership.setRole(WorkspaceRole.MEMBER);
            membership.setDefaultWorkspace(false);
            workspaceMembershipRepository.save(membership);
            workspace.getMemberships().add(membership);
            workspaceRepository.save(workspace);

            // TODO: Notify the user that they have been added to a workspace
            emailService.sendEmail(newUser);
        }


        redirectAttributes.addFlashAttribute("success", "Member added successfully");
        model.addAttribute("user", user);
        model.addAttribute("workspace", workspace);
        // If the user can access the workspace, continue with the workspace vieww

        return "redirect:/w/{workspaceId}/settings/members";
    }


    
    private final SecureRandom random = new SecureRandom();
    private static final int TOKEN_BYTE_SIZE = 32;

     private String generateToken() {
        
        byte[] bytes = new byte[TOKEN_BYTE_SIZE];
        random.nextBytes(bytes);
        return String.valueOf(Hex.encode(bytes));
    }



}
