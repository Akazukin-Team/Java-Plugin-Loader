package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPostPluginDisableEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PostPluginDisableEvent implements IPostPluginDisableEvent {
    String pluginId;
    IPluginContext pluginContext;

    public PostPluginDisableEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }
}
