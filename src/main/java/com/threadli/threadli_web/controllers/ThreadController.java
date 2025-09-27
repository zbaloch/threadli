package com.threadli.threadli_web.controllers;

import java.security.Principal;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.threadli.threadli_web.models.Post;
import com.threadli.threadli_web.models.ThreadMembership;
import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.models.WorkspaceRole;
import com.threadli.threadli_web.repositories.PostRepository;
import com.threadli.threadli_web.repositories.ThreadMembershipRepository;
import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;
import com.threadli.threadli_web.services.WorkspaceService;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import java.time.Instant;
import com.threadli.threadli_web.models.Thread;



@Controller
public class ThreadController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ThreadMembershipRepository threadMembershipRepository;
    

    @GetMapping("/w/{workspaceId}/compose")
    public String accessWorkspace(@PathVariable Long workspaceId, 
                                    Principal principal,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        log.info("workspace " + workspaceId);
        model.addAttribute("user", user);
        model.addAttribute("workspace", workspace);
        model.addAttribute("workspaceMembers", workspace.getMemberships());
        model.addAttribute("view", "workspace");
        return "workspace/thread/compose";
    }

    @PostMapping("/w/{workspaceId}/compose")
    public String createThread(@PathVariable Long workspaceId, 
                                @RequestParam("title") String title,
                                @RequestParam("content") String content,
                                Principal principal,
                                Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = new com.threadli.threadli_web.models.Thread();
        thread.setTitle(title);
        thread.setCreatedBy(user);
        thread.setWorkspace(workspace);
        threadRepository.save(thread);
        Post post = new Post();
        post.setContent(content);
        post.setThread(thread);
        post.setCreatedBy(user);
        postRepository.save(post);

        thread.getPosts().add(post);
        // threadRepository.save(thread);

        model.addAttribute("workspace", workspace);
        model.addAttribute("thread", thread);
        model.addAttribute("post", post);
        model.addAttribute("user", user);
        
        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId() + "/add-people";

    }


    @GetMapping("/w/{workspaceId}/t/{threadId}/add-people")
    public String addPeopleToThread(@PathVariable Long workspaceId, 
                                    @PathVariable Long threadId,
                                Principal principal,
                                Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();

        model.addAttribute("workspace", workspace);
        model.addAttribute("thread", thread);
        model.addAttribute("user", user);

        // Check if the user is already a member of the thread
        boolean isMember = threadMembershipRepository.findByThreadId(thread.getId()).stream()
            .anyMatch(membership -> membership.getUser().getId().equals(user.getId()));
        if(!isMember) {
            ThreadMembership threadMembership = new ThreadMembership(user, thread);
            threadMembership.setCaughtUp(true);
            threadMembershipRepository.save(threadMembership);
        }

        if(workspace.getMemberships().size() == 1) {
            return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
        } else {
            return "workspace/thread/addPeople";
        }

    }

    @PostMapping("/w/{workspaceId}/t/{threadId}/add-people")
    public String addPeopleToThreadPost(@PathVariable Long workspaceId, 
                                    @PathVariable Long threadId,
                                    @RequestParam(required = false) List<Long> memberIds, // Accept member IDs
                                    Principal principal,
                                    Model model) {
                                        log.info("memberIds " + memberIds);
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
        
        if(memberIds != null && memberIds.size() > 0)  {
            
            // Logic to add members to the thread
            for (Long memberId : memberIds) {
                User member = userRepository.findById(memberId).orElse(null);
                if (member != null) {
                    // Is the member already part of the thread?
                    boolean alreadyMember = threadMembershipRepository.findByThreadId(thread.getId()).stream()
                        .anyMatch(membership -> membership.getUser().getId().equals(memberId));
                    if(alreadyMember) {
                        continue;
                    }
                    // Add logic to associate the member with the thread
                    // For example, you might have a method in your Thread model to add members
                    // thread.addMember(member); // Assuming you have this method
                    ThreadMembership membership = new ThreadMembership(user, thread);
                    membership.setThread(thread);
                    membership.setUser(member);
                    membership.setCaughtUp(false);
                    threadMembershipRepository.save(membership);
                }
            }
            
            threadRepository.save(thread); // Save the updated thread
        }

        model.addAttribute("workspace", workspace);
        model.addAttribute("thread", thread);
        model.addAttribute("user", user);

        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }

    @GetMapping("/w/{workspaceId}/t/{threadId}/p/{postId}/delete")
        public String deletePost(@PathVariable Long workspaceId, 
        @PathVariable Long channelId,
        @PathVariable Long threadId,
        @PathVariable Long postId,
        Principal principal,
        Model mode
        ) {
        
            User user = userRepository.findByEmail(principal.getName());
            Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
            com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
            Post post = postRepository.findByCreatedByAndId(user, postId).get();
            postRepository.delete(post);
            return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }
    


    @GetMapping("/w/{workspaceId}/t/{threadId}")
    public String getThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            Principal principal,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        Thread thread = threadRepository.findByWorkspaceIdAndId(workspaceId, threadId).get();
        List<ThreadMembership> memberships = threadMembershipRepository.findByThreadId(thread.getId());
        log.info("memberships.size() " + memberships.size());
        for(ThreadMembership membership : memberships) {
            log.info("" + membership.getUser().getId());
            log.info("" + membership.getThread().getId());
            if(membership.getUser().getId() == user.getId() && membership.getCaughtUp() == false) {
                membership.setCaughtUp(true);
                threadMembershipRepository.save(membership);
            }

        }

        // Mark the thread as caughtup for this user

        

        List<Post> posts = postRepository.findByThreadId(thread.getId());

        boolean isAdmin = workspace.getMemberships().stream().anyMatch(membership -> membership.getUser().getId() == user.getId() && membership.getRole() == WorkspaceRole.ADMIN);
        model.addAttribute("isAdmin", isAdmin);

        model.addAttribute("workspace", workspace);
        model.addAttribute("thread", thread);
        model.addAttribute("memberships",memberships);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("view", "threads");
        return "workspace/thread/view";
    }

    @PostMapping("/w/{workspaceId}/t/{threadId}")
    public String postThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            @RequestParam("content") String content,
                            Principal principal,
                            Model model) {
        log.info("posting thread " + content);
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();

        thread.getMemberships().forEach(membership -> {
            if(membership.getUser().getId() != user.getId()) {
                membership.setCaughtUp(false);
            }
        });

        Post post = new Post();
        post.setContent(content);
        post.setThread(thread);
        post.setCreatedBy(user);
        postRepository.save(post);

        thread.setUpdatedAt(Instant.now());

        threadRepository.save(thread);

        List<Post> posts = postRepository.findByThreadId(threadId);

        model.addAttribute("workspace", workspace);
        model.addAttribute("thread", thread);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("view", "workspace");
        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }



    @GetMapping("/w/{workspaceId}/t/{threadId}/delete")
    public String deleteThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
        threadRepository.delete(thread);
        
        return "redirect:/w/" + workspace.getId();
    }

    @GetMapping("/w/{workspaceId}/t/{threadId}/pin")
    public String pinThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();

        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
        thread.setPinned(true);
        threadRepository.save(thread);
        
        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }

    @GetMapping("/w/{workspaceId}/t/{threadId}/unpin")
    public String unpinThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
        thread.setPinned(false);
        threadRepository.save(thread);
        
        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }


    @GetMapping("/w/{workspaceId}/t/{threadId}/close")
    public String closeThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
        thread.setClosed(true);
        threadRepository.save(thread);
        
        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }

    @GetMapping("/w/{workspaceId}/t/{threadId}/open")
    public String openThread(@PathVariable Long workspaceId, 
                            @PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId).get();
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByWorkspaceIdAndId(workspace.getId(), threadId).get();
        thread.setClosed(false);
        threadRepository.save(thread);
        
        return "redirect:/w/" + workspace.getId() + "/t/" + thread.getId();
    }


}
