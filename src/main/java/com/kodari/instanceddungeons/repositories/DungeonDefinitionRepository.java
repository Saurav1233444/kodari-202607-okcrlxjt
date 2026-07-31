package com.kodari.instanceddungeons.repositories;

import com.kodari.instanceddungeons.domain.DungeonDefinition;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DungeonDefinitionRepository {
    CompletableFuture<DungeonDefinition> findById(UUID id);
    CompletableFuture<DungeonDefinition> findByKey(String key);
    CompletableFuture<Void> save(DungeonDefinition definition);
    CompletableFuture<Void> delete(UUID id);
}