package ru.otus.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IdempotencyService {

    @Value("${idempotency.key.expiration:24}")
    int expirationHours;

    private final Map<UUID, IdempotencyRecord> cache = new ConcurrentHashMap<>();

    public boolean isDuplicateRequest(UUID idempotencyKey) {
        cleanExpiredKeys();
        return cache.containsKey(idempotencyKey);
    }

    public void storeRequestResult(UUID idempotencyKey, Object result) {
        IdempotencyRecord rec = new IdempotencyRecord(
                idempotencyKey,
                result,
                LocalDateTime.now().plusHours(expirationHours)
        );
        cache.put(idempotencyKey, rec);
    }

    public Object getCachedResult(UUID idempotencyKey) {
        IdempotencyRecord rec = cache.get(idempotencyKey);
        if (rec != null && rec.getExpiryTime().isAfter(LocalDateTime.now())) {
            return rec.getResult();
        }
        return null;
    }

    private void cleanExpiredKeys() {
        cache.entrySet().removeIf(entry ->
                entry.getValue().getExpiryTime().isBefore(LocalDateTime.now())
        );
    }

    @Data
    @AllArgsConstructor
    private static class IdempotencyRecord {
        private UUID key;
        private Object result;
        private LocalDateTime expiryTime;
    }
}