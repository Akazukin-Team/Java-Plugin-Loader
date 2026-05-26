package org.akazukin.loader.api.event.events;

import org.akazukin.loader.api.context.IPluginContext;

public interface IPostBatchPluginDisableEvent extends IBatchPluginLifecycleEvent {
    IPluginContext[] getPluginContexts();
}
