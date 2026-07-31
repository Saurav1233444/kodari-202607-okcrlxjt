package com.kodari.instanceddungeons.repositories;

import com.kodari.instanceddungeons.domain.RewardRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RewardRepository {
    CompletableFuture<List<RewardRecord>> findByPlayer(UUID playerId);
    CompletableFuture<Void> save(RewardRecord reward);
}