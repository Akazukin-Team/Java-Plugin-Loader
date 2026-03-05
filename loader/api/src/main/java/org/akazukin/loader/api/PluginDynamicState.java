package org.akazukin.loader.api;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public enum PluginDynamicState {
    NONE("None"),
    LOADING("Loading"),
    UNLOADING("Unloading"),
    ENABLING("Enabling"),
    DISABLING("Disabling"),
    UNREGISTERING("Unregistering"),
    REGISTERING("Registering");

    String name;

    PluginDynamicState(final String name) {
        this.name = name;
    }
}
