package ru.otus.models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceState {
    private String status;
    private String message;
}
