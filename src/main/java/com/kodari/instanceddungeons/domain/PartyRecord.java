package com.kodari.instanceddungeons.domain;

import java.util.List;
import java.util.UUID;

public record PartyRecord(UUID id, UUID leaderId, String state, List<UUID> members, long createdAt) {
    public PartyRecord {
        if (id == null || leaderId == null || state == null || state.isBlank() || members == null) {
            throw new IllegalArgumentException("Party record contains invalid values.");
        }
        members = List.copyOf(members);
    }
}