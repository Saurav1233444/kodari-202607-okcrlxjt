package com.kodari.instanceddungeons.lifecycle;

/**
 * A component that can apply its current configuration without restarting the plugin.
 */
public interface Reloadable {
    /**
     * Reloads the component.
     *
     * @throws Exception when the new configuration is invalid
     */
    void reload() throws Exception;
}