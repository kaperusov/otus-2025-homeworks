package ru.otus.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class Notification {
    private int id;
    private String email;
    private String subject;
    private String message;
    private LocalDateTime sentAt;
}
