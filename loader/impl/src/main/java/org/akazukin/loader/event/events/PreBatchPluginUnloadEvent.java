package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPreBatchPluginUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public final class PreBatchPluginUnloadEvent implements IPreBatchPluginUnloadEvent {
    final String[] pluginIds;
    final IPluginContext[] pluginContexts;
    boolean cancelled;

    public PreBatchPluginUnloadEvent(final @NotNull IPluginContext[] pluginContexts) {
        this.pluginIds = Arrays.stream(pluginContexts).map(ctx -> ctx.getMetadata().getId()).toArray(String[]::new);
        this.pluginContexts = pluginContexts;
    }

    @Override
    public String toString() {
        return "PreBatchPluginUnloadEvent{"
                + "pluginIds='" + Arrays.toString(this.pluginIds) + '\''
                + '}';
    }
}
