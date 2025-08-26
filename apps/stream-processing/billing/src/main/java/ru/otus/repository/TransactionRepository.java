package ru.otus.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.otus.models.Transaction;

import java.util.UUID;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

}