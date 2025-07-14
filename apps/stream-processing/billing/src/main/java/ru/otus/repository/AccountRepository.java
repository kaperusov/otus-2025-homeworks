package ru.otus.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.models.Account;

import java.util.Optional;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByUserId(UUID userId);
    boolean existsByUserId(UUID userId);
}