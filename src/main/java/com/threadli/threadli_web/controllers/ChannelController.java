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
import com.threadli.threadli_web.models.Workspace;
import com.threadli.threadli_web.models.WorkspaceRole;
import com.threadli.threadli_web.repositories.ChannelRepository;
import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;

@Controller
public class ChannelController {

    private static final Logger log = LoggerFactory.getLogger(ChannelController.class);

    @Autowired
    private ChannelRepository channelRepository;

    @Autowired
    private ThreadRepository threadRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    /**
     * Get all channels for a workspace as JSON (for Alpine.js typeahead)
     * Only returns channels where the user has at least one thread
     */
    @GetMapping("/api/w/{workspaceId}/channels")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> getWorkspaceChannels(@PathVariable Long workspaceId,
                                                                            Principal principal) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId)
                .orElse(null);

        if (workspace == null) {
            return ResponseEntity.notFound().build();
        }

        // Get user's threads in this workspace
        List<Thread> userThreads = threadRepository
                .findByWorkspaceIdAndMemberships_User_IdAndIsDeletedFalseOrderByUpdatedAtDesc(workspaceId, user.getId());

        // Get channel IDs where user has threads
        java.util.Set<Long> userChannelIds = userThreads.stream()
                .filter(thread -> thread.getChannel() != null)
                .map(thread -> thread.getChannel().getId())
                .collect(Collectors.toSet());

        List<Map<String, Object>> channels = workspace.getChannels().stream()
                .filter(channel -> userChannelIds.contains(channel.getId()))
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
    @GetMapping("/w/{workspaceId}/c/{channelId}")
    public String getChannel(@PathVariable Long workspaceId,
                            @PathVariable Long channelId,
                            Principal principal,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(principal.getName());
        Workspace workspace = workspaceRepository.findByMembershipsUserIdAndId(user.getId(), workspaceId)
                .orElse(null);

        if (workspace == null) {
            redirectAttributes.addFlashAttribute("error", "Workspace not found");
            return "redirect:/w";
        }

        Channel channel = channelRepository.findByWorkspaceIdAndId(workspaceId, channelId)
                .orElse(null);

        if (channel == null) {
            redirectAttributes.addFlashAttribute("error", "Channel not found");
            return "redirect:/w/" + workspaceId;
        }

        // Get all non-deleted threads in this channel where user is a member, ordered by most recent
        List<Thread> allChannelThreads = threadRepository
                .findByWorkspaceIdAndChannelIdAndIsDeletedFalseOrderByUpdatedAtDesc(workspaceId, channelId);
        List<Thread> threads = allChannelThreads.stream()
                .filter(thread -> thread.getMemberships().stream()
                        .anyMatch(membership -> membership.getUser().getId().equals(user.getId())))
                .collect(Collectors.toList());

        // Check if user is admin
        boolean isAdmin = workspace.getMemberships().stream()
                .anyMatch(membership -> membership.getUser().getId().equals(user.getId())
                        && membership.getRole() == WorkspaceRole.ADMIN);

        // Get all user's threads in workspace to filter channels
        List<Thread> userWorkspaceThreads = threadRepository
                .findByWorkspaceIdAndMemberships_User_IdAndIsDeletedFalseOrderByUpdatedAtDesc(workspaceId, user.getId());
        java.util.Set<Long> userChannelIds = userWorkspaceThreads.stream()
                .filter(thread -> thread.getChannel() != null)
                .map(thread -> thread.getChannel().getId())
                .collect(Collectors.toSet());
        List<Channel> userChannels = workspace.getChannels().stream()
                .filter(ch -> userChannelIds.contains(ch.getId()))
                .collect(Collectors.toList());

        model.addAttribute("workspace", workspace);
        model.addAttribute("channel", channel);
        model.addAttribute("threads", threads);
        model.addAttribute("userChannels", userChannels);
        model.addAttribute("user", user);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("view", "channel");
        model.addAttribute("pageTitle", channel.getName());

        log.info("Viewing channel: {} in workspace: {}", channel.getName(), workspaceId);

        return "workspace/channel/view";
    }
}
