package com.threadli.threadli_web.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.ThreadMembershipRepository;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;
import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.models.ThreadMembership;

import java.util.List;

@Service
public class ScheduledTaskService {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private WorkspaceRepository workspaceRepository;
    
    @Autowired
    private ThreadRepository threadRepository;
    
    @Autowired
    private ThreadMembershipRepository threadMembershipRepository;

    @Autowired
    private EmailService emailService;


    /**
     * Example: Send scheduled notifications
     */
    @Scheduled(cron = "0 0 */2 * * *")
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    private void sendScheduledNotifications() {
        log.info("Processing scheduled notifications...");

        List<User> usersWithUncaughtUpThreads = threadMembershipRepository.findUsersWithUncaughtUpThreads();
        for (User user : usersWithUncaughtUpThreads) {
            log.info("User {} {} has uncaught up threads. Sending notification...", user.getId(), user.getFirstName());
            emailService.sendUncaughtUpThreadsEmail(user);
            // Here you would implement the actual notification sending logic
        }

        // Add notification logic:
        // - Send digest emails
        // - Push notifications for missed messages
        // - Reminder notifications
        // etc.
    }

}