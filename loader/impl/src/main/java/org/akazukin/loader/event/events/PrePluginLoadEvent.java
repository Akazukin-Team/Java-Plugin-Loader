package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPrePluginLoadEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public final class PrePluginLoadEvent implements IPrePluginLoadEvent {
    final String pluginId;
    final IPluginContext pluginContext;
    boolean cancelled;

    public PrePluginLoadEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }

    @Override
    public String toString() {
        return "PrePluginLoadEvent{"
                + "pluginId='" + this.pluginId + '\''
                + '}';
    }
}
