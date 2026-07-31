package com.kodari.instanceddungeons.repositories;

import com.kodari.instanceddungeons.domain.PartyRecord;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PartyRepository {
    CompletableFuture<PartyRecord> findById(UUID id);
    CompletableFuture<Void> save(PartyRecord party);
    CompletableFuture<Void> delete(UUID id);
}