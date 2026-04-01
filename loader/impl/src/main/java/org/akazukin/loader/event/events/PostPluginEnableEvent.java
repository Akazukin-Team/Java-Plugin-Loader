package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPostPluginEnableEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public class PostPluginEnableEvent implements IPostPluginEnableEvent {
    String pluginId;
    IPluginContext pluginContext;

    public PostPluginEnableEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }

    @Override
    public String toString() {
        return "PostPluginEnableEvent{"
                + "pluginId='" + this.pluginId + '\''
                + '}';
    }
}
