package com.kodari.instanceddungeons.lifecycle;

import com.kodari.instanceddungeons.logging.LoggingService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Starts and stops plugin services in dependency order.
 */
public final class LifecycleManager {
    private final LoggingService logging;
    private final List<LifecycleComponent> components = new ArrayList<>();
    private final List<Reloadable> reloadables = new ArrayList<>();
    private final AtomicBoolean started = new AtomicBoolean();

    public LifecycleManager(LoggingService logging) {
        this.logging = logging;
    }

    /**
     * Registers a component. Components start in registration order and stop in reverse order.
     *
     * @param component component to register
     */
    public void register(LifecycleComponent component) {
        components.add(component);
        if (component instanceof Reloadable reloadable) {
            reloadables.add(reloadable);
        }
    }

    /**
     * Starts all registered components.
     *
     * @throws Exception when a component cannot start
     */
    public void start() throws Exception {
        if (!started.compareAndSet(false, true)) {
            return;
        }

        int startedComponents = 0;
        try {
            for (LifecycleComponent component : components) {
                component.start();
                startedComponents++;
            }
        } catch (Exception exception) {
            for (int index = startedComponents - 1; index >= 0; index--) {
                components.get(index).stop();
            }
            started.set(false);
            throw exception;
        }
    }

    /**
     * Reloads all reloadable components in dependency order.
     *
     * @throws Exception when a component rejects the new configuration
     */
    public void reload() throws Exception {
        if (!started.get()) {
            throw new IllegalStateException("Cannot reload services before startup.");
        }

        for (Reloadable reloadable : reloadables) {
            reloadable.reload();
        }
    }

    /**
     * Stops all registered components in reverse dependency order.
     */
    public void stop() {
        if (!started.compareAndSet(true, false)) {
            return;
        }

        for (int index = components.size() - 1; index >= 0; index--) {
            try {
                components.get(index).stop();
            } catch (RuntimeException exception) {
                logging.error("Failed to stop " + components.get(index).getClass().getSimpleName() + ".", exception);
            }
        }
    }
}