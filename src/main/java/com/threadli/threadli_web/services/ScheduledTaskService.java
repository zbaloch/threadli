package com.threadli.threadli_web.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import com.threadli.threadli_web.repositories.ThreadRepository;
import com.threadli.threadli_web.repositories.ThreadMembershipRepository;
import com.threadli.threadli_web.repositories.UserRepository;
import com.threadli.threadli_web.repositories.WorkspaceRepository;
import com.threadli.threadli_web.models.User;
import com.threadli.threadli_web.models.ThreadMembership;

import java.util.List;
import org.springframework.scheduling.annotation.SchedulingConfigurer;

@Service
@EnableScheduling
public class ScheduledTaskService implements SchedulingConfigurer {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTaskService.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    @Value("${threadli.scheduler.email.cron}")
    private String emailSchedulerCron;

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

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            this::sendScheduledNotifications,
            new CronTrigger(emailSchedulerCron)
        );
    }

    /**
     * Send scheduled email notifications based on cron expression from THREADLI_SCHEDULER_EMAIL_CRON environment variable
     * Cron format: second minute hour day month weekday
     * Example: "0 0 9,15 * * *" runs at 9:00am and 3:00pm every day
     */
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