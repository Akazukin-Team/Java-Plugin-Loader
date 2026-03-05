package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.IPluginContext;
import org.akazukin.loader.api.event.events.IPrePluginUnloadEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class PrePluginUnloadEvent implements IPrePluginUnloadEvent {
    final String pluginId;
    final IPluginContext pluginContext;
    boolean cancelled;

    public PrePluginUnloadEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }
}
