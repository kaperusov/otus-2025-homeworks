package ru.otus.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import ru.otus.models.Notification;
import ru.otus.models.NotificationRequest;
import ru.otus.models.ServiceState;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final JavaMailSender emailSender;

    // In-memory "database"
    private final List<Notification> notificationsDb = new ArrayList<>();
    private final AtomicInteger nextId = new AtomicInteger(1);

    // SMTP configuration
    private final String mockMailSender = System.getenv().getOrDefault("MOCK_MAIL_SENDER", "true");

    @PostMapping("/notifications")
    public ResponseEntity<Object> sendNotification(@RequestBody NotificationRequest request) {
        // If not mock, try to send real email
        if (!"true".equals(mockMailSender)) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("noreplay@otus.ru");
                message.setTo(request.getEmail());
                message.setSubject(request.getSubject());
                message.setText(request.getMessage());
                emailSender.send(message);

            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("detail", "Email sending failed: " + e.getMessage()));
            }
        }

        // Save notification to "database"
        Notification notification = new Notification(
                nextId.getAndIncrement(),
                request.getEmail(),
                request.getSubject(),
                request.getMessage(),
                LocalDateTime.now()
        );
        notificationsDb.add(notification);

        return ResponseEntity.ok(notification);
    }

    @GetMapping("/notifications")
    public List<Notification> getNotifications() {
        return notificationsDb;
    }

    @GetMapping("/health")
    public ServiceState healthCheck() {
        return new ServiceState("OK", "Application is healthy");
    }

    @GetMapping("/ready")
    public ServiceState readinessCheck() {
        return new ServiceState("OK", "Application is ready to handle requests");
    }
}
