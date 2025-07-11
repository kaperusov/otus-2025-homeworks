package ru.otus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@SpringBootApplication
public class NotificationApp {
    public static void main(String[] args) {
        SpringApplication.run(NotificationApp.class, args);
    }


    // SMTP configuration
    private final String smtpServer = System.getenv().getOrDefault("SMTP_SERVER", "smtp.example.com");
    private final int smtpPort = Integer.parseInt(System.getenv().getOrDefault("SMTP_PORT", "587"));
    private final String smtpUsername = System.getenv().getOrDefault("SMTP_USERNAME", "your_email@example.com");
    private final String smtpPassword = System.getenv().getOrDefault("SMTP_PASSWORD", "your_password");

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpServer);
        mailSender.setPort(smtpPort);

        mailSender.setUsername(smtpUsername);
        mailSender.setPassword(smtpPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.debug", "true");

        return mailSender;
    }
}