package com.kodari.instanceddungeons.editor;

import com.kodari.instanceddungeons.dungeon.definition.DungeonDefinitionModel;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EditorSessionManager {
    private final Map<UUID, DungeonDefinitionModel> activeSessions = new ConcurrentHashMap<>();

    public void startSession(UUID playerId, DungeonDefinitionModel model) {
        activeSessions.put(playerId, model);
    }

    public Optional<DungeonDefinitionModel> getSession(UUID playerId) {
        return Optional.ofNullable(activeSessions.get(playerId));
    }

    public void endSession(UUID playerId) {
        activeSessions.remove(playerId);
    }

    public boolean hasActiveSession(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }
}
