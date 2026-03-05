package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.IPluginContext;
import org.akazukin.loader.api.event.events.IPostPluginUnloadEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PostPluginUnloadEvent implements IPostPluginUnloadEvent {
    String pluginId;
    IPluginContext pluginContext;

    public PostPluginUnloadEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }
}
