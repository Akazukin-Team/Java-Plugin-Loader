package org.akazukin.loader.api.event.events;

import org.akazukin.loader.api.context.IPluginContext;

public interface IPostBatchPluginRegisterEvent extends IBatchPluginLifecycleEvent {
    IPluginContext[] getPluginContexts();
}
