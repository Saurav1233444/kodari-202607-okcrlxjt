package com.kodari.instanceddungeons.repositories;

import com.kodari.instanceddungeons.domain.InstanceRecord;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface InstanceRepository {
    CompletableFuture<InstanceRecord> findById(UUID id);
    CompletableFuture<List<InstanceRecord>> findActive();
    CompletableFuture<Void> save(InstanceRecord instance);
    CompletableFuture<Void> delete(UUID id);
}