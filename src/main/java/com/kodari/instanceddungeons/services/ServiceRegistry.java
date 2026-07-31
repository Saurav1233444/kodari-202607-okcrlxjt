package com.kodari.instanceddungeons.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Dependency registry for services owned by one plugin instance.
 */
public final class ServiceRegistry {
    private final Map<Class<?>, Object> services = new LinkedHashMap<>();

    /**
     * Registers a service under its contract type.
     *
     * @param type contract type
     * @param service service implementation
     * @param <T> service type
     */
    public <T> void register(Class<T> type, T service) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(service, "service");

        if (services.putIfAbsent(type, service) != null) {
            throw new IllegalStateException("A service is already registered for " + type.getName());
        }
    }

    /**
     * Resolves a service or fails fast when the composition root is incomplete.
     *
     * @param type contract type
     * @param <T> service type
     * @return registered service
     */
    public <T> T require(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalStateException("No service registered for " + type.getName());
        }
        return type.cast(service);
    }

    /**
     * Checks whether a contract has been registered.
     *
     * @param type contract type
     * @return true when registered
     */
    public boolean contains(Class<?> type) {
        return services.containsKey(type);
    }
}