package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPostBatchPluginLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public final class PostBatchPluginLoadEvent implements IPostBatchPluginLoadEvent {
    String[] pluginIds;
    IPluginContext[] pluginContexts;

    public PostBatchPluginLoadEvent(final @NotNull IPluginContext[] pluginContexts) {
        this.pluginIds = Arrays.stream(pluginContexts).map(ctx -> ctx.getMetadata().getId()).toArray(String[]::new);
        this.pluginContexts = pluginContexts;
    }

    @Override
    public String toString() {
        return "PostBatchPluginLoadEvent{"
                + "pluginId='" + Arrays.toString(this.pluginIds) + '\''
                + '}';
    }
}
