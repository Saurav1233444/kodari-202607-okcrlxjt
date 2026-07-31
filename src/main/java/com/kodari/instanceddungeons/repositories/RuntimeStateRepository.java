package com.kodari.instanceddungeons.repositories;

import com.kodari.instanceddungeons.domain.RuntimeState;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface RuntimeStateRepository {
    CompletableFuture<RuntimeState> find(UUID instanceId);
    CompletableFuture<Void> save(RuntimeState state);
    CompletableFuture<Void> delete(UUID instanceId);
}