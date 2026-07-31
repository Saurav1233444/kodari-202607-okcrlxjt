package com.kodari.instanceddungeons.lifecycle;

/**
 * A service whose resources are owned by the plugin lifecycle.
 */
public interface LifecycleComponent {
    /**
     * Starts the component.
     *
     * @throws Exception when startup cannot complete
     */
    void start() throws Exception;

    /**
     * Stops the component and releases its resources.
     */
    void stop();
}