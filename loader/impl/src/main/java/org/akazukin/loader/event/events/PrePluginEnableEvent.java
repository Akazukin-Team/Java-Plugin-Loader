package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.IPluginContext;
import org.akazukin.loader.api.event.events.IPrePluginEnableEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class PrePluginEnableEvent implements IPrePluginEnableEvent {
    final String pluginId;
    final IPluginContext pluginContext;
    boolean cancelled;

    public PrePluginEnableEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }
}
