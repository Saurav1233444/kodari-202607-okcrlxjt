package com.kodari.instanceddungeons.repositories;

import com.kodari.instanceddungeons.domain.PlayerProgress;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PlayerProgressRepository {
    CompletableFuture<PlayerProgress> find(UUID playerId, UUID instanceId);
    CompletableFuture<Void> save(PlayerProgress progress);
    CompletableFuture<Void> delete(UUID playerId, UUID instanceId);
}