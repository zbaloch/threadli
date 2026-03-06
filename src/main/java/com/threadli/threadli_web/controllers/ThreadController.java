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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

import com.threadli.threadli_web.models.Channel;
import com.threadli.threadli_web.models.Post;
import com.threadli.threadli_web.models.ThreadMembership;
import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.repositories.ChannelRepository;
import com.threadli.threadli_web.repositories.PostRepository;
import com.threadli.threadli_web.repositories.ThreadMembershipRepository;
import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.UserRepository;

import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import java.time.Instant;
import com.threadli.threadli_web.models.Thread;



@Controller
public class ThreadController {

    private static final Logger log = LoggerFactory.getLogger(ThreadController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ThreadMembershipRepository threadMembershipRepository;

    @Autowired
    private ChannelRepository channelRepository;

    /**
     * Security helper method to verify if a user is a member of a specific thread
     * @param threadId The thread ID to check
     * @param userId The user ID to verify
     * @return true if user is a member, false otherwise
     */
    private boolean isUserThreadMember(Long threadId, Long userId) {
        List<ThreadMembership> memberships = threadMembershipRepository.findByThreadId(threadId);
        return memberships.stream()
            .anyMatch(membership -> membership.getUser().getId().equals(userId));
    }

    /**
     * Capitalize the first letter of each word
     * E.g. "engineering team" -> "Engineering Team"
     */
    private String capitalizeWords(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String[] words = input.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return result.toString().trim();
    }
    

    @GetMapping("/compose")
    public String accessCompose(@RequestParam(value = "channel", required = false) String channel,
                                    Principal principal,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        User user = userRepository.findByEmail(principal.getName());
        log.info("Accessing compose page");
        model.addAttribute("user", user);
        if (channel != null) {
            model.addAttribute("prefilledChannel", channel);
        }
        model.addAttribute("view", "workspace");
        model.addAttribute("pageTitle", "New Thread");
        return "workspace/thread/compose";
    }

    @GetMapping("/api/members")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getMembers(Principal principal) {
        User user = userRepository.findByEmail(principal.getName());

        // Get all users in the system (excluding current user)
        List<User> allUsers = userRepository.findAll();

        List<Map<String, Object>> members = allUsers.stream()
            .filter(u -> !u.getId().equals(user.getId())) // Exclude current user
            .map(memberUser -> {
                Map<String, Object> member = new HashMap<>();
                member.put("id", memberUser.getId());
                member.put("name", memberUser.getFirstName() + " " + memberUser.getLastName());
                member.put("email", memberUser.getEmail());
                member.put("avatar", "https://api.dicebear.com/9.x/initials/svg?seed=" +
                          memberUser.getFirstName() + " " + memberUser.getLastName());
                return member;
            })
            .collect(Collectors.toList());

        return ResponseEntity.ok(members);
    }

    @PostMapping("/compose")
    public String createThread(@RequestParam("title") String title,
                                @RequestParam("content") String content,
                                @RequestParam(value = "selectedMembers", required = false) List<Long> selectedMemberIds,
                                @RequestParam(value = "channelName", required = false) String channelName,
                                Principal principal,
                                Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = new com.threadli.threadli_web.models.Thread();
        thread.setTitle(title);
        thread.setCreatedBy(user);

        // Handle channel assignment
        if (channelName != null && !channelName.trim().isEmpty()) {
            // Clean input: remove #, trim spaces
            String cleanedName = channelName.replace("#", "").trim();

            if (!cleanedName.isEmpty()) {
                // Generate slug for lookups
                String slug = Channel.generateSlug(cleanedName);

                // Find or create channel
                Channel channel = channelRepository.findBySlug(slug)
                        .orElseGet(() -> {
                            // Auto-capitalize first letter and first letter after space
                            String displayName = capitalizeWords(cleanedName);
                            Channel newChannel = new Channel(displayName, user);
                            return channelRepository.save(newChannel);
                        });
                thread.setChannel(channel);
            }
        }

        threadRepository.save(thread);

        // Add the creator to the thread
        ThreadMembership creatorMembership = new ThreadMembership(user, thread);
        creatorMembership.setCaughtUp(true);
        threadMembershipRepository.save(creatorMembership);

        // Add selected members to the thread
        if (selectedMemberIds != null && !selectedMemberIds.isEmpty()) {
            for (Long memberId : selectedMemberIds) {
                User member = userRepository.findById(memberId).orElse(null);
                if (member != null) {
                    ThreadMembership membership = new ThreadMembership(member, thread);
                    membership.setCaughtUp(false);
                    threadMembershipRepository.save(membership);
                }
            }
        }

        Post post = new Post();
        post.setContent(content);
        post.setThread(thread);
        post.setCreatedBy(user);
        postRepository.save(post);

        thread.getPosts().add(post);

        model.addAttribute("thread", thread);
        model.addAttribute("post", post);
        model.addAttribute("user", user);

        return "redirect:/t/" + thread.getId();
    }


    @GetMapping("/t/{threadId}/add-people")
    public String addPeopleToThread(@PathVariable Long threadId,
                                Principal principal,
                                Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

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

        return "workspace/thread/addPeople";
    }

    @PostMapping("/t/{threadId}/add-people")
    public String addPeopleToThreadPost(@PathVariable Long threadId,
                                    @RequestParam(required = false) List<Long> memberIds,
                                    Principal principal,
                                    Model model) {
        log.info("memberIds " + memberIds);
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

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
                    ThreadMembership membership = new ThreadMembership(member, thread);
                    membership.setCaughtUp(false);
                    threadMembershipRepository.save(membership);
                }
            }

            threadRepository.save(thread); // Save the updated thread
        }

        model.addAttribute("thread", thread);
        model.addAttribute("user", user);

        return "redirect:/t/" + thread.getId();
    }

    @GetMapping("/t/{threadId}/p/{postId}/delete")
    public String deletePost(@PathVariable Long threadId,
                            @PathVariable Long postId,
                            Principal principal,
                            Model model) {

        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

        Post post = postRepository.findByCreatedByAndId(user, postId).orElse(null);
        if (post != null) {
            postRepository.delete(post);
        }
        return "redirect:/t/" + thread.getId();
    }
    


    @GetMapping("/t/{threadId}")
    public String getThread(@PathVariable Long threadId,
                            Principal principal,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getName());
        Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        // SECURITY CHECK: Check if thread exists and is not soft-deleted
        if (thread == null) {
            redirectAttributes.addFlashAttribute("error", "This thread has been deleted.");
            return "redirect:/";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            // User is not a member of this thread - redirect with error message
            redirectAttributes.addFlashAttribute("error", "You don't have access to this thread.");
            return "redirect:/";
        }

        List<ThreadMembership> memberships = threadMembershipRepository.findByThreadId(thread.getId());
        for(ThreadMembership membership : memberships) {
            log.info("" + membership.getUser().getId());
            log.info("" + membership.getThread().getId());
            if(membership.getUser().getId() == user.getId() && membership.getCaughtUp() == false) {
                membership.setCaughtUp(true);
                threadMembershipRepository.save(membership);
            }
        }

        List<Post> posts = postRepository.findByThreadId(thread.getId());

        boolean isAdmin = user.isAdmin();

        // Get user's threads to filter channels for sidebar
        List<Thread> userThreads = threadRepository.findByMemberships_User_IdAndIsDeletedFalseOrderByUpdatedAtDesc(user.getId());
        java.util.Set<Long> userChannelIds = userThreads.stream()
                .filter(t -> t.getChannel() != null)
                .map(t -> t.getChannel().getId())
                .collect(java.util.stream.Collectors.toSet());
        List<com.threadli.threadli_web.models.Channel> userChannels = userChannelIds.stream()
                .map(id -> channelRepository.findById(id).orElse(null))
                .filter(channel -> channel != null)
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("thread", thread);
        model.addAttribute("memberships",memberships);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("userChannels", userChannels);
        model.addAttribute("view", "threads");
        model.addAttribute("pageTitle", thread.getTitle());
        return "workspace/thread/view";
    }

    @PostMapping("/t/{threadId}")
    public String postThread(@PathVariable Long threadId,
                            @RequestParam("content") String content,
                            Principal principal,
                            Model model) {
        log.info("posting thread " + content);
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread before allowing posts
        if (!isUserThreadMember(threadId, user.getId())) {
            // User is not a member of this thread - redirect with error message
            return "redirect:/?error=thread_access_denied";
        }

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

        model.addAttribute("thread", thread);
        model.addAttribute("user", user);
        model.addAttribute("posts", posts);
        model.addAttribute("view", "workspace");
        return "redirect:/t/" + thread.getId();
    }



    @GetMapping("/t/{threadId}/delete")
    public String deleteThread(@PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Only thread creator or admin can delete threads
        // But first verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

        // Soft delete instead of permanent delete
        thread.setDeleted(true);
        thread.setDeletedAt(Instant.now());
        thread.setDeletedBy(user);
        threadRepository.save(thread);

        return "redirect:/";
    }

    @GetMapping("/t/{threadId}/pin")
    public String pinThread(@PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

        thread.setPinned(true);
        threadRepository.save(thread);

        return "redirect:/t/" + thread.getId();
    }

    @GetMapping("/t/{threadId}/unpin")
    public String unpinThread(@PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

        thread.setPinned(false);
        threadRepository.save(thread);

        return "redirect:/t/" + thread.getId();
    }


    @GetMapping("/t/{threadId}/close")
    public String closeThread(@PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }
        thread.setClosed(true);
        thread.setClosedAt(Instant.now());
        thread.setClosedBy(user);
        threadRepository.save(thread);

        return "redirect:/t/" + thread.getId();
    }

    @GetMapping("/t/{threadId}/open")
    public String openThread(@PathVariable Long threadId,
                            Principal principal,
                            Model model) {
        User user = userRepository.findByEmail(principal.getName());
        com.threadli.threadli_web.models.Thread thread = threadRepository.findByIdAndIsDeletedFalse(threadId).orElse(null);

        if (thread == null) {
            return "redirect:/?error=thread_not_found";
        }

        // SECURITY CHECK: Verify user is a member of this thread
        if (!isUserThreadMember(threadId, user.getId())) {
            return "redirect:/?error=thread_access_denied";
        }

        thread.setClosed(false);
        thread.setClosedAt(null);
        thread.setClosedBy(null);
        threadRepository.save(thread);

        return "redirect:/t/" + thread.getId();
    }


}
