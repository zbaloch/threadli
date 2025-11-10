package com.threadli.threadli_web.services;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.threadli.threadli_web.models.ThreadMembership;
import com.threadli.threadli_web.models.User;

@Service
public class EmailService {
     @Value("${passwordless.email.from}")
    private String from;

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${host.url}")
    String url;

    private static Logger log = LoggerFactory.getLogger(EmailService.class);

    @Async
    public void sendEmail(User user) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(587);
        
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        SimpleMailMessage message = new SimpleMailMessage(); 
        message.setFrom(from);
        
        message.setTo(user.getEmail()); 
        message.setSubject("Threadli login magic link"); 
        message.setText("Please use this link to login: \n\n" + url + "/verify-token-and-login?email=" + user.getEmail()+"&token=" + user.getToken());
        log.info("Please use this link to login: \n\n" + url + "/verify-token-and-login?email=" + user.getEmail()+"&token=" + user.getToken());
        mailSender.send(message);

        // return true;


    }

    @Async
    public void sendUncaughtUpThreadsEmail(User user) {
        String to = user.getEmail();
        String subject = "Time to catch up ⚡";
        String body = "Hey " + user.getFirstName() + ","
            + "\n\nLooks like there’s been some action while you were away! Jump back in and catch up at " + url + "."
            + "\n\nCheers,\nThreadli";
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(587);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        mailSender.send(message);
        log.info("Custom email sent to: " + to);
    }
}