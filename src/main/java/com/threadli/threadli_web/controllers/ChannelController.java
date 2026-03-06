package com.threadli.threadli_web.controllers;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.threadli.threadli_web.models.Channel;
import com.threadli.threadli_web.models.Thread;
import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.repositories.ChannelRepository;
import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.UserRepository;

@Controller
public class ChannelController {

    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all channels as JSON (for Alpine.js typeahead)
     * Only returns channels where the user has at least one thread
     */
    @GetMapping("/api/channels")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getChannels(Principal principal) {
        User user = userRepository.findByEmail(principal.getName());

        // Get user's threads
        List<Thread> userThreads = threadRepository
                .findByMemberships_User_IdAndIsDeletedFalseOrderByUpdatedAtDesc(user.getId());

        // Get channel IDs where user has threads
        java.util.Set<Long> userChannelIds = userThreads.stream()
                .filter(thread -> thread.getChannel() != null)
                .map(thread -> thread.getChannel().getId())
                .collect(Collectors.toSet());

        // Get all channels and filter to only those where user has threads
        List<Map<String, Object>> channels = userChannelIds.stream()
                .map(channelId -> channelRepository.findById(channelId).orElse(null))
                .filter(channel -> channel != null)
                .map(channel -> {
                    Map<String, Object> ch = new HashMap<>();
                    ch.put("id", channel.getId());
                    ch.put("name", channel.getName()); // Display name for UI
                    ch.put("slug", channel.getSlug()); // Slug for identification
                    return ch;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(channels);
    }

    /**
     * View threads in a specific channel
     */
    @GetMapping("/c/{channelId}")
    public String getChannel(@PathVariable Long channelId,
                            Principal principal,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getName());

        Channel channel = channelRepository.findById(channelId)
                .orElse(null);

        if (channel == null) {
            redirectAttributes.addFlashAttribute("error", "Channel not found");
            return "redirect:/";
        }

        // Get all non-deleted threads in this channel where user is a member, ordered by most recent
        List<Thread> allChannelThreads = threadRepository
                .findByChannelIdAndIsDeletedFalseOrderByUpdatedAtDesc(channelId);
        List<Thread> threads = allChannelThreads.stream()
                .filter(thread -> thread.getMemberships().stream()
                        .anyMatch(membership -> membership.getUser().getId().equals(user.getId())))
                .collect(Collectors.toList());

        // Check if user is admin
        boolean isAdmin = user.isAdmin();

        // Get all user's threads to filter channels
        List<Thread> userThreads = threadRepository
                .findByMemberships_User_IdAndIsDeletedFalseOrderByUpdatedAtDesc(user.getId());
        java.util.Set<Long> userChannelIds = userThreads.stream()
                .filter(thread -> thread.getChannel() != null)
                .map(thread -> thread.getChannel().getId())
                .collect(Collectors.toSet());
        List<Channel> userChannels = userChannelIds.stream()
                .map(id -> channelRepository.findById(id).orElse(null))
                .filter(ch -> ch != null)
                .collect(Collectors.toList());

        model.addAttribute("channel", channel);
        model.addAttribute("threads", threads);
        model.addAttribute("userChannels", userChannels);
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("view", "channel");
        model.addAttribute("pageTitle", channel.getName());

        log.info("Viewing channel: {}", channel.getName());

        return "workspace/channel/view";
    }
}
