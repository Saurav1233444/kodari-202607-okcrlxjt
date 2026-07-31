package com.kodari.instanceddungeons.repositories;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface StatisticsRepository {
    CompletableFuture<Long> get(UUID playerId, String key);
    CompletableFuture<Void> set(UUID playerId, String key, long value);
    CompletableFuture<Void> increment(UUID playerId, String key, long amount);
}