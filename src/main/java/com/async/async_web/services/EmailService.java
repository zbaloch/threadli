package com.async.async_web.services;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.async.async_web.models.User;

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
        message.setSubject("Async login magic link"); 
        message.setText("Please use this link to login: \n\n" + url + "/verify-token-and-login?email=" + user.getEmail()+"&token=" + user.getToken());
        log.info("Please use this link to login: \n\n" + url + "/verify-token-and-login?email=" + user.getEmail()+"&token=" + user.getToken());
        mailSender.send(message);

        // return true;


    }
}