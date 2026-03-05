package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPrePluginDisableEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class PrePluginDisableEvent implements IPrePluginDisableEvent {
    final String pluginId;
    final IPluginContext pluginContext;
    boolean cancelled;

    public PrePluginDisableEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }
}
