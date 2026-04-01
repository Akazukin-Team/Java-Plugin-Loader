package org.akazukin.loader.event.events;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.akazukin.loader.api.context.IPluginContext;
import org.akazukin.loader.api.event.events.IPrePluginRegisterEvent;
import org.jetbrains.annotations.NotNull;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public final class PrePluginRegisterEvent implements IPrePluginRegisterEvent {
    final String pluginId;
    final IPluginContext pluginContext;
    boolean cancelled;

    public PrePluginRegisterEvent(final @NotNull IPluginContext pluginContext) {
        this.pluginId = pluginContext.getMetadata().getId();
        this.pluginContext = pluginContext;
    }

    @Override
    public String toString() {
        return "PrePluginRegisterEvent{"
                + "pluginId='" + this.pluginId + '\''
                + '}';
    }
}
