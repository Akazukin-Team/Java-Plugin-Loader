package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPostBatchPluginUnregisterEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Getter
public final class PostBatchPluginUnregisterEvent implements IPostBatchPluginUnregisterEvent {
    String[] pluginIds;
    IPluginContext[] pluginContexts;

    public PostBatchPluginUnregisterEvent(final @NotNull IPluginContext[] pluginContexts) {
        this.pluginIds = Arrays.stream(pluginContexts).map(ctx -> ctx.getMetadata().getId()).toArray(String[]::new);
        this.pluginContexts = pluginContexts;
    }

    @Override
    public String toString() {
        return "PostBatchPluginUnregisterEvent{"
                + "pluginIds='" + Arrays.toString(this.pluginIds) + '\''
                + '}';
    }
}
