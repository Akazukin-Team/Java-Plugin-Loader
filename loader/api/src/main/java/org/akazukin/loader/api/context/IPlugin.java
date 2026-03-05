package org.akazukin.loader.api.context;

/**
 * Base plugin interface defining plugin lifecycle.
 */
public interface IPlugin {
    default void onLoad() {
    }

    default void onUnload() {
    }

    default void onEnable() {
    }

    default void onDisable() {
    }
}
