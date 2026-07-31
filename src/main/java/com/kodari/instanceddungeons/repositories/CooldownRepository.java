package com.kodari.instanceddungeons.repositories;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface CooldownRepository {
    CompletableFuture<Long> findExpiry(UUID playerId, String key);
    CompletableFuture<Void> save(UUID playerId, String key, long expiresAt);
    CompletableFuture<Void> delete(UUID playerId, String key);
}