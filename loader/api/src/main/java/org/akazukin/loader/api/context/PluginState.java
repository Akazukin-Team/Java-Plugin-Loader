package org.akazukin.loader.api.context;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public enum PluginState {
    NONE("None", 0, false, false, false),
    ENTRY("Entry", 1, true, false, false),
    LOADED("Loaded", 2, true, true, false),
    ENABLED("Enabled", 3, true, true, true),
    TERMINATED("Terminated", -1, false, false, false);

    String name;
    int order;
    boolean registered;
    boolean loaded;
    boolean enabled;

    PluginState(final String name, final int order, final boolean registered, final boolean loaded, final boolean enabled) {
        this.name = name;
        this.order = order;
        this.registered = registered;
        this.loaded = loaded;
        this.enabled = enabled;
    }
}
