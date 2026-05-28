package org.akazukin.loader.api.event.events;

import org.akazukin.event.event.ICancellableEvent;
import org.akazukin.loader.api.context.IPluginContext;

public interface IPreBatchPluginLoadEvent extends IBatchPluginLifecycleEvent, ICancellableEvent {
    IPluginContext[] getPluginContexts();
}
